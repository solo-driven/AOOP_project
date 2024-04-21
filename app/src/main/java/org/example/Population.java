package org.example;

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

    public void initialize() {
        for (int i = 0; i < this.size; i++) {
            Gene gene = generateSolution();
//            System.out.println(Arrays.toString(gene.getGene()));
//            System.out.println(gene.calculateFitness());
//            gene.showAssignment();
            this.population.add(gene);
        }
    }

    private Gene generateSolution() {
        Gene gene = new Gene(this.students, this.destinations);
        for (Student student : this.students) {
            int destinationIndex = gene.chooseDestinationFromPreference(student);
            int startIndex = destinationIndex * this.students.size();
            int endIndex = startIndex + this.students.size();
//          System.out.println(startIndex + ", " + endIndex);
            for (int i = startIndex; i < endIndex; i++) {
                int studentIndex = i % this.students.size(); // Get the corresponding student index
//              System.out.println(studentIndex);
                if (studentIndex == this.students.indexOf(student)) {
                    gene.getGene()[i] = 1;
                    break;
                }
            }
        }
//        System.out.println(Arrays.toString(gene.getGene()));
        return gene;
    }

    public Map<Gene, Integer> calculateFitness() {
        Map<Gene, Integer> fitnessMap = new HashMap<>();
//        IntStream.range(0, this.size).parallel().forEach(i -> {
        for (Gene gene : this.population) {
//            Gene gene = this.population.get(i);
            int fitness = gene.calculateFitness();
            fitnessMap.put(gene, fitness);
        }
//            System.out.println(fitnessMap);
//        });
        return fitnessMap;
    }

/*    public Gene select() {
        // Calculate total fitness of the population
        int totalFitness = 0;
        for (Gene gene : this.population) {
            totalFitness += gene.calculateFitness();
//            System.out.println(gene.calculateFitness());
        }
//        System.out.println("Total fitness: " + totalFitness);
        // Generate a random value between 0 and totalFitness
        Random random = new Random();
        int randomValue = random.nextInt(totalFitness);
//        System.out.println("Random value for roulette wheel: " + randomValue);
        // Accumulate fitness values and select the corresponding gene
        int accumulatedFitness = 0;
        for (Gene gene : this.population) {
            accumulatedFitness += gene.calculateFitness();
//            System.out.println("Acc fitness: " + accumulatedFitness);
            if (accumulatedFitness >= randomValue) {
                return gene; // Select this gene
            }
        }
        // This line should never be reached, but if it does, return null
        return null;
    }*/

    public Gene select() {
        // Calculate total fitness of the population
        Collections.shuffle(this.population);
        double totalInverseFitness = 0;
        for (Gene gene : this.population) {
            totalInverseFitness += ((double) 1 / gene.calculateFitness()); // Inverse of fitness
        }
        // Generate a random value between 0 and totalInverseFitness
        Random random = new Random();
        double randomValue = random.nextDouble() * totalInverseFitness;
        // Accumulate inverse fitness values and select the corresponding gene
        double accumulatedInverseFitness = 0;
        for (Gene gene : this.population) {
            accumulatedInverseFitness += ((double) 1 / gene.calculateFitness()); // Inverse of fitness
            if (accumulatedInverseFitness >= randomValue) {
                return gene; // Select this gene
            }
        }

        // This line should never be reached, but if it does, return null
        return null;
    }

    public Gene evolve() {
        Gene fittestGene = null;
        int generationsCounter = 1;
        int noImprovementCount = 0;
        int previousFitness = Integer.MAX_VALUE;
        int minFitness;

        // Continue evolving until a solution meeting the criteria is found
        while (generationsCounter <= this.maxGenerations && noImprovementCount < this.maxNoImprovementCount) {
            // System.out.println("Stuck in evolve");
            // Evolution steps
            List<Gene> newPopulation = new ArrayList<>();
            Map<Gene, Integer> fitnessMap = calculateFitness();
//            System.out.println("Fitness Map: " + fitnessMap);
            List<Map.Entry<Gene, Integer>> sortedFitness = new ArrayList<>(fitnessMap.entrySet());
            sortedFitness.sort(Map.Entry.comparingByValue());
//            System.out.println("Population size: " + this.size);
            int elitismCount = (int) Math.ceil(0.1 * this.size);
//            System.out.println("Elitism count: " + elitismCount);
            // Elitism: Select top individuals to pass unchanged to the next generation
            for (int i = 0; i < elitismCount; i++) {
//                System.out.println(sortedFitness);
                Gene eliteGene = sortedFitness.get(i).getKey();
                newPopulation.add(eliteGene);
            }

            // Crossover and mutation to fill the rest of the new population
            while (newPopulation.size() < this.size) {
                // System.out.println("Generation no:" + generationsCounter);
                // System.out.println("Stuck in fill Population");
                Gene parent1 = select();
                Gene parent2 = select();
                // System.out.println("this population size: " + this.size + " newPopulation size: " + newPopulation.size());
                // while (parent1 == parent2) {
                //     // System.out.println("parent 1: " + parent1 + " parent 2: " + parent2);
                //     System.out.println("Stuck in parent select");
                //     parent2 = select();
                // }
                if (this.crossoverProb > Math.random()) {
                    Gene child = parent1.crossover(parent2);
                    mutate(child);
                    newPopulation.add(child);
                } else {
                    mutate(parent1);
                    mutate(parent2);
                    newPopulation.add(parent1);
                    newPopulation.add(parent2);
                }
            }

            // Replace the current population with the new population
            this.population = newPopulation;

            fittestGene = sortedFitness.get(0).getKey();
            minFitness = sortedFitness.get(0).getValue();

            if (previousFitness == minFitness) {
                noImprovementCount++;
            }
            else {
                previousFitness = minFitness;
                noImprovementCount = 0;
            }
            // if (noImprovementCount >= 500 && noImprovementCount % 50 == 0)
                // System.out.println("No Improvement count: " + noImprovementCount);

//            System.out.println("Generation No: " + generationsCounter + " Gene: " + fittestGene.calculateFitness());
            // Increment generation counter
            generationsCounter++;
        }

        // Return the fittest gene found
        return fittestGene;
    }



    private Gene getFittestGene() {
        Map<Gene, Integer> fitnessMap = calculateFitness();
        Gene fittestGene = null;
        int minFitness = Integer.MAX_VALUE;
        for (Map.Entry<Gene, Integer> entry : fitnessMap.entrySet()) {
            if (entry.getValue() < minFitness) {
                minFitness = entry.getValue();
                fittestGene = entry.getKey();
            }
        }
        return fittestGene;
    }

    public void mutate(Gene gene) {
        gene.mutate(this.mutationProb);
    }

    public Gene crossover(Gene father, Gene mother) {
        Gene child = null;

        return child;
    }

    public Gene convertToGene() {
        Gene gene = new Gene(this.students, this.destinations);
        int index = 0;
        for (Destination destination : this.destinations) {
            for (Student student : this.students) {
                Map<Integer, Destination> preferences = student.getPreferences();
                if (preferences.containsValue(destination)) {
                    gene.getGene()[index] = 1;
                } else {
                    gene.getGene()[index] = 0;
                }
                index++;
            }
        }
        return gene;
    }
}