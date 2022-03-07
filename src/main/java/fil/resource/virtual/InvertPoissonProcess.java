package fil.resource.virtual;

import java.util.Random;

public class InvertPoissonProcess { 
	private Random generator;
	
	public InvertPoissonProcess() {
		generator = new Random();
	}
	
	public double generate(int lambda) {
		
//		Random generator = new Random();
		double seed = generator.nextDouble();
		double interArrival = -Math.log(1.0 - seed)/lambda; // invert CDF of Poisson process
		return interArrival;
	}
}
