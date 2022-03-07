/* Sim_poisson_obj.java */

package fil.resource.virtual;

import java.util.Random;

//import eduni.simjava.distributions.*;
//import eduni.simjava.*;

/**
 * A random number generator based on the Poisson distribution. <br>
 * NOTE: This is an updated version that fixes a bug in {@link #sample()}
 *       method. In the previous version {@link #sample()} always return zero
 *       value.
 *
 * @version     1.0, 14 May 2002
 * @author      Costas Simatos
 */

public class PoissonDistribution {
    
    private double lambda;  //lambda is taken to be the rate eg. counts/sec
    //private String name;
    private Random rng = new Random();
    
    
    public PoissonDistribution() {
 
//        this.lambda = l;
//        this.name = name;
    }
  
    public double sample(){    
        
        double randNum = rng.nextDouble();
        double probTerm = Math.exp(-lambda ); // probability of 0
        double probSum = probTerm;
        int sampleNum = 0;
        while (randNum >= probSum) {
            sampleNum++;
            // compute probability of n from prob. of n-1
            probTerm *= (double)lambda / sampleNum;
            // add to sum
            probSum += probTerm;
        }
        
        return sampleNum ;
        
    }

        public int sample(double l){    
        
        lambda = l;
        double randNum = rng.nextDouble();
        double probTerm = Math.exp(-lambda ); // probability of 0
        double probSum = probTerm;
        int sampleNum = 0;
        while (randNum >= probSum) {
            sampleNum++;
            // compute probability of n from prob. of n-1
            probTerm *= (double)lambda / sampleNum;
            // add to sum
            probSum += probTerm;
        }
        
        return sampleNum ;
        
    }
        
    public double sampleaverage(){
        
        //System.out.print("Trials: ");
        int trials = 100;
        
        
        int total = 0;
        for (int i=0; i < trials; i++) {
            double randNum = rng.nextDouble();
            double probTerm = Math.exp(-lambda ); // probability of 0
            double probSum = probTerm;
            int sampleNum = 0;
            while (randNum >= probSum) {
                sampleNum++;
                // compute probability of n from prob. of n-1
                probTerm *= (double)lambda / sampleNum;
                // add to sum
                probSum += probTerm;
            }
            //System.out.println(sampleNum);
            total += sampleNum;
        }
        double x = ((double)total / trials);
        System.out.println("Sample average: " + x );
        return x;
        
    }
    
        
    /**
     * Generate a new random number.
     * @return The next random number in the sequence
     */
    public double sample1() {
        long x = -1L;
        double m = Math.exp(-1 * lambda);
        double product = 1;
        do {
            x++;
            product *= Math.random();
        } while(m < product);
        return x;
    }
    
    /**
     * Get the random number generator's name.
     * @return The generator's name
     */
//    public String get_name() {
//        return name;
//    }

}
