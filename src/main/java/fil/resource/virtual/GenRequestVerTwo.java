package fil.resource.virtual;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Random;

import fil.resource.substrate.Rpi;


public class GenRequestVerTwo {
	final static double LIVETIME = 2.0;
	final static double TIMEWINDOW = 1.0;
	final static int COF = 1; //6 for full 24h option //3 for not-full option
	final static int PINUMBER = 300;
	private LinkedList<HashMap<Rpi, Double>> listTotalRequest;
	private PoissonDistribution poisson;
//	private int [] LUT = {32,22,20,23,37,89,211,335,337,282,263,269,
//			276,287,313,350,375,304,269,151,110,80,30,20}; //full 24h
	private Double [] LUT = {0.025,0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0}; // not-full

	public GenRequestVerTwo() {
		this.listTotalRequest = new LinkedList<>();
		this.poisson = new PoissonDistribution();
	}
	
	public LinkedList<HashMap<Rpi, Double>> generator() {
		int numOfWD = LUT.length;
//		InvertPoissonProcess pp = new InvertPoissonProcess();
		Random generator = new Random();
//		Random generator_uni = new Random();
		
		for(int WD = 0; WD < numOfWD; WD ++) {
//			System.out.println("TimeWindow number: " + WD);
			HashMap<Rpi, Double> deviceEachTW = new HashMap<>(); // store request in 1 TW
//			int lambda = LUT[WD];
			double lambda = LUT[WD]*2100;
			System.out.println("lambda: " + lambda);

//			int totalRequest = this.poisson.sample(lambda);
			int totalRequest = (int) lambda;
			totalRequest *= COF;
			while(totalRequest > 0) {
				System.out.println("Total request: " + totalRequest);
//				double seed = generator_uni.nextDouble();
//				double interArrival = (StdRandom.exp(lambda));
//				timeArrival += interArrival; // arrival time of the next request
//				System.out.println("TimeArrival: " + timeArrival);
//				if(timeArrival < (WD + 1.0)) {
				Rpi pi = new Rpi(generator.nextInt(PINUMBER));
				double endTime = WD + StdRandom.exp(1.0/LIVETIME*1.0);
				deviceEachTW.put(pi, endTime);
				totalRequest --;
//				}
//				else {
//					this.listTotalRequest.add(deviceEachTW); // add to list all TW
//					break;
//				}
			}
			this.listTotalRequest.add(deviceEachTW); // add to list all TW
		}
		return this.listTotalRequest;
	}
	
	public static void main(String[] args) {
//		Random generator_uni = new Random();
//		double lambda = 1000;
//		for(int i = 0; i < 100; i++) {
////			double seed = generator_uni.nextDouble();
//			double interArrival = (StdRandom.exp(lambda));
//			System.out.println("interarrival time: " + interArrival);
//		}
//		double seed = generator_uni.nextDouble();
//		double interArrival = -Math.log(1.0 - seed)/lambda;
		GenRequestVerTwo sample = new GenRequestVerTwo();
		sample.generator();
		System.out.println("size of TW: " + sample.getListTotalRequest().size());
		
		for(int i = 0; i < sample.getListTotalRequest().size(); i++) {
			System.out.println("device: " + sample.getListTotalRequest().get(i).size());
		}
	}
	
	public LinkedList<HashMap<Rpi, Double>> getListTotalRequest() {
		return listTotalRequest;
	}

	public void setListTotalRequest(LinkedList<HashMap<Rpi, Double>> listTotalRequest) {
		this.listTotalRequest = listTotalRequest;
	}
	
}
