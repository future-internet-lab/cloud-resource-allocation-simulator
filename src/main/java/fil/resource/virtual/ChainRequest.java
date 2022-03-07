package fil.resource.virtual;


import java.util.Random;
import java.util.ArrayList;

public class ChainRequest {
	
	private int requestID;
	private int randomNum;
	private ArrayList<Integer> numChain;
	private int totalChain;

	/**
	 * Constructs new Chain Request
	 */
	public ChainRequest(int min, int max) {
		int inChain = 0;
		numChain = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			inChain = this.getRamdom(min, max);
			numChain.add(inChain);
			this.totalChain += inChain;
		}
	}

	public int getRamdom(int min, int max) {
		Random rand = new Random();
	    this.randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}

	public int getRequestID() {
		return requestID;
	}


	public void setRequestID(int requestID) {
		this.requestID = requestID;
	}



	public ArrayList<Integer> getNumChain() {
		return numChain;
	}


	public int getTotalChain() {
		return totalChain;
	}


	
}