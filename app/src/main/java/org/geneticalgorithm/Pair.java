package org.geneticalgorithm;

// Pair class used to return two values from a method call
public class Pair {
    int first, second;

    public Pair(int first, int second){
        this.first = first;
        this.second = second;
    }

    public int getFirst() {
      return this.first;
    }

    public int getSecond() {
      return this.second;
    }

    void print(){
        System.out.println("(" + this.first + ", " + this.second + ")" );
    }
}