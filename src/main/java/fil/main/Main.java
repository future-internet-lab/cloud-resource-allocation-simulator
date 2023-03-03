/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.main;

import java.io.IOException;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import fil.algorithm.GRETA.Orchestrator_GRETA;
import fil.algorithm.REAP.Scheduler_REAP;
import fil.algorithm.RESCE.Scheduler_RESCE;
import fil.algorithm.RESCE.Scheduler_RESCE_I;
import fil.resource.virtual.GenRequest;
import fil.agorithm.SFCPlanner.Scheduler_SFCPlanner;

public class Main {
	final static String PATH = System.getProperty("user.dir");

	public static void main(String[] args) throws IOException, SAXException, JAXBException {
		// TODO Auto-generated method stub
		String traffic_all = PATH + "\\Traffic-model\\2018-weekdays-quarter.xlsx"; // separated traffic of each day
		String traffic_test = PATH + "\\Traffic-model\\temp.xlsx";
		int days = 1; // simulation in "days" , maximum in data is 178 days
		String type = "24h"; // il: increase load data for benchmark only
		boolean ML = true; // true: turn on prediction, false: turn off
//		double TW = 0.5;
		
		for(int i = 0; i < days; i ++) { 
			System.out.println("Day number: " + i);
			Orchestrator_GRETA greta = new Orchestrator_GRETA();
			Scheduler_RESCE resce = new Scheduler_RESCE();
			Scheduler_RESCE_I resce_i = new Scheduler_RESCE_I();
			Scheduler_REAP reap = new Scheduler_REAP();
			Scheduler_SFCPlanner sfcp = new Scheduler_SFCPlanner();
			
//			GenRequest eventGen = new GenRequest(); // only this line and the below for benchmarking with IL traffic
//			eventGen.generate(type);
//			GenRequest eventGen = new GenRequest(traffic_all); // turn on to simulate all 178 traffic days
			GenRequest eventGen = new GenRequest(traffic_test);
			eventGen.generate(i, 0.5);
			
			try { // since the used ML libraries for Java is very primitive, it's recommend to surround the GRETA simulation with try and catch
				// current the TW is set to 1.0
				// the following block runs GRETA in different TW with different ML models
				// check Orchestrator_GRETA to know the passing arguments
				// after every algorithm, the function "resetListEvent()" must be run to reset the SFC event list
				
				greta.run(eventGen.sortInTW(0.5), 0.5, type, ML, "GB");
				eventGen.resetListEvent();
				greta = new Orchestrator_GRETA();
				greta.run(eventGen.sortInTW(1.0), 1.0, type, ML, "GB");
				eventGen.resetListEvent();
				greta = new Orchestrator_GRETA();
				greta.run(eventGen.sortInTW(1.0), 2.0, type, ML, "GB");	
				eventGen.resetListEvent();
				greta = new Orchestrator_GRETA();
				greta.run(eventGen.sortInTW(1.0), 2.0, type, ML, "LSTM");	
				
				// the following block consists of other algorithms, namely, RESCE, RESCE-ideal, REAP and SFCP
				// uncomment them with the function "resetListEvent()" to run
				
				eventGen.resetListEvent();
				resce.run(eventGen.sortInTW(1.0), type);
				eventGen.resetListEvent();
				resce_i.run(eventGen.sortInTW(1.0), type);
				eventGen.resetListEvent();
				reap.run(eventGen.sortInTW(1.0), type);
				eventGen.resetListEvent();
				sfcp.run(eventGen.sortInTW(1.0), type);
				
			} catch (Exception e) {
				System.out.println("Error happens");
				System.out.println(e);
//				i --; // rerun the round
			}
			
		}

	}
}
