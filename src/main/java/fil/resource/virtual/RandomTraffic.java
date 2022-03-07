package fil.resource.virtual;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class RandomTraffic {
	private static PoissonDistribution poisson;
	private static ArrayList<Integer> listTraffic = new ArrayList<>();

	
	public static void write_integer (String filename, ArrayList<Integer> x) throws IOException{ //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
 		for (int i = 0; i < x.size(); i++) {
			// Maybe:
			//outputWriter.write(x.get(i));
			// Or:
			outputWriter.write(Integer.toString(x.get(i)));
			outputWriter.newLine();
 		}
		outputWriter.flush();  
		outputWriter.close();  
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		poisson = new PoissonDistribution();
		File file = new File("traffic.txt");
		BufferedReader br = new BufferedReader(new FileReader(file));
		  
		  String st;
		  while ((st = br.readLine()) != null) {
			  
			  Double traffic;
			  traffic = Double.parseDouble(st);
			  System.out.println(traffic);
			  int trafficSample = poisson.sample(traffic);
			  listTraffic.add(trafficSample);
		  }
		  
		  write_integer("VelocitySample.txt", listTraffic);
	}
}
