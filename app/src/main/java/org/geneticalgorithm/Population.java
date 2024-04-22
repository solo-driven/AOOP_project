package org.geneticalgorithm;

import java.util.*;

public class Population {
    private List<Gene> population;
    private List<Destination> destinations;
    private List<Student> students;
    private int size;
    private int maxGenerations;
    private int maxNoImprovementCount;
    private double mutationProb;
    private double crossoverProb;

    public Population(List<Destination> destinations, List<Student> students, int size, int maxGenerations, int maxNoImprovementCount, double mutationProb, double crossoverProb) {
        this.destinations = destinations;
        this.students = students;
        this.size = size;
        this.maxGenerations = maxGenerations;
        this.maxNoImprovementCount = maxNoImprovementCount;
        this.mutationProb = mutationProb;
        this.crossoverProb = crossoverProb;
        this.population = new ArrayList<>();
    }

    public List<Gene> getPopulation() {
        return this.population;
    }

    // Method to initialize the population with randomly generated genes
    public void initialize() {
        for (int i = 0; i < this.size; i++) {
            Gene gene = generateSolution();
            this.population.add(gene);
        }
    }

    // Method to generate a single solution (gene) for the population
    private Gene generateSolution() {
        Gene gene = new Gene(this.students, this.destinations);
        // Assign students to destinations based on preferences
        for (Student student : this.students) {
            int destinationIndex = gene.getHelperMethods().chooseDestinationFromPreference(student);
            int startIndex = destinationIndex * this.students.size();
            int endIndex = startIndex + this.students.size();
            for (int i = startIndex; i < endIndex; i++) {
                int studentIndex = i % this.students.size(); // Get the corresponding student index
                if (studentIndex == this.students.indexOf(student)) {
                    gene.getGene()[i] = 1; // Assign student to destination in gene
                    break;
                }
            }
        }
        return gene;
    }

    // Method to calculate the fitness of each gene in the population
    public Map<Gene, Integer> calculateFitness() {
        Map<Gene, Integer> fitnessMap = new HashMap<>();
        for (Gene gene : this.population) {
            int fitness = gene.calculateFitness(); // Calculate fitness of gene
            fitnessMap.put(gene, fitness); // Store gene and its fitness in map
        }
        return fitnessMap;
    }

    // Method to select a gene from the population based on fitness
    public Gene select() {
        // Calculate total inverse fitness of the population
        Collections.shuffle(this.population);
        double totalInverseFitness = 0;
        for (Gene gene : this.population) {
            totalInverseFitness += ((double) 1 / gene.calculateFitness()); // Calculate inverse fitness
        }
        // Generate a random value between 0 and totalInverseFitness
        Random random = new Random();
        double randomValue = random.nextDouble() * totalInverseFitness;
        // Accumulate inverse fitness values and select the corresponding gene
        double accumulatedInverseFitness = 0;
        for (Gene gene : this.population) {
            accumulatedInverseFitness += ((double) 1 / gene.calculateFitness()); // Calculate accumulated inverse fitness
            if (accumulatedInverseFitness >= randomValue) {
                return gene; // Select this gene
            }
        }

        // This line should never be reached, but if it does, return null
        return null;
    }

    // Method to evolve the population to find the fittest gene
    public Gene evolve() {
        Gene fittestGene = null;
        int generationsCounter = 1;
        int noImprovementCount = 0;
        int previousFitness = Integer.MAX_VALUE;
        int minFitness;

        // Continue evolving until a solution meeting the criteria is found
        while (generationsCounter <= this.maxGenerations && noImprovementCount < this.maxNoImprovementCount) {
            // Evolution steps
            List<Gene> newPopulation = new ArrayList<>();
            Map<Gene, Integer> fitnessMap = calculateFitness();
            List<Map.Entry<Gene, Integer>> sortedFitness = new ArrayList<>(fitnessMap.entrySet());
            sortedFitness.sort(Map.Entry.comparingByValue());
            int elitismCount = (int) Math.ceil(0.1 * this.size); // Number of top individuals to pass unchanged
            // Elitism: Select top individuals to pass unchanged to the next generation
            for (int i = 0; i < elitismCount; i++) {
                Gene eliteGene = sortedFitness.get(i).getKey();
                newPopulation.add(eliteGene);
            }

            // Crossover and mutation to fill the rest of the new population
            while (newPopulation.size() < this.size) {
                Gene parent1 = select();
                Gene parent2 = select();
                if (this.crossoverProb > Math.random()) {
                    Gene child = parent1.crossover(parent2); // Perform crossover
                    mutate(child); // Mutate child gene
                    newPopulation.add(child);
                } else {
                    mutate(parent1); // Mutate parent 1
                    mutate(parent2); // Mutate parent 2
                    newPopulation.add(parent1);
                    newPopulation.add(parent2);
                }
            }

            // Replace the current population with the new population
            this.population = newPopulation;

            // Update fittest gene and fitness
            fittestGene = sortedFitness.get(0).getKey();
            minFitness = sortedFitness.get(0).getValue();

            // Track no improvement count
            if (previousFitness == minFitness) {
                noImprovementCount++;
            } else {
                previousFitness = minFitness;
                noImprovementCount = 0;
            }
            // Increment generation counter
            generationsCounter++;
        }

        // Return the fittest gene found
        return fittestGene;
    }

    // Method to mutate a gene with given probability
    private void mutate(Gene gene) {
        gene.mutate(this.mutationProb);
    }
}