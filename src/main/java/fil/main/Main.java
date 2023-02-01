/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.main;

import java.io.IOException;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import fil.algorithm.REAP.Scheduler_REAP;
import fil.resource.virtual.GenRequest;
import fil.agorithm.SFCPlanner.Scheduler_SFCPlanner;
//import fil.algorithm.CR.copy.Scheduler_CR_new;
import fil.algorithm.CR.Orchestrator_CR;
import fil.algorithm.FullRemap.Scheduler_FR;
import fil.algorithm.FullRemap.Scheduler_FR_Lack_Power;

//import fil.algorithm.CR.Scheduler_CR_new;

//import fil.algorithm.FullRemapRealTime.Scheduler_FRRT;
//import fil.algorithm.FullRemapTW.Scheduler_FRTW;

public class Main {
	final static String PATH = System.getProperty("user.dir");

	public static void main(String[] args) throws IOException, SAXException, JAXBException {
		// TODO Auto-generated method stub
//		String path = "C:\\Users\\kienkk\\OneDrive - Hanoi University of Science and Technology\\Simulators-n-tools\\low-complexity\\20211204-sync-simulator\\lowcomplexity\\Traffic-model\\";
		String traffic_all = PATH + "\\Traffic-model\\2018-weekdays-quarter.xlsx";
		String traffic_test = PATH + "\\Traffic-model\\temp.xlsx";
		/*
		 * There are two version of GenRequest based on the constructor, so be careful.
		 */
		int days = 1; // simulation in "days" , maximum in data is 178
		String type = "24h"; // il: increase load, 24h: 24 hour simulation
		boolean ML = true; // true: turn on prediction, false: turn off
//		double TW = 0.5;
		for(int i = 0; i < days; i ++) {
			System.out.println("Day: " + i);
			Orchestrator_CR CR = new Orchestrator_CR();
			Scheduler_FR FR = new Scheduler_FR();
			Scheduler_FR_Lack_Power FR_LP = new Scheduler_FR_Lack_Power();
			Scheduler_REAP reap = new Scheduler_REAP();
			Scheduler_SFCPlanner sfcp = new Scheduler_SFCPlanner();
			
			
//			GenRequest eventGen = new GenRequest();
//			eventGen.generate(type);
//			GenRequest eventGen = new GenRequest(traffic_all);
			GenRequest eventGen = new GenRequest(traffic_test);
			eventGen.generate(i, 0.5);
			
			
//			Scheduler_FRTW frtw = new Scheduler_FRTW();
//			System.out.println("Size of 0.5 TW: " + eventGen.sortInTW(0.5).size());
//			CR.run(eventGen.sortInTW(0.5), 0.5, type, ML);
//			eventGen.resetListEvent();
//			CR = new Orchestrator_CR();
//			try {
//				CR.run(eventGen.sortInTW(0.5), 0.5, type, ML, "GB");
//				eventGen.resetListEvent();
//				CR = new Orchestrator_CR();
//				CR.run(eventGen.sortInTW(1.0), 1.0, type, ML, "GB");
//				eventGen.resetListEvent();
//				CR = new Orchestrator_CR();
//				CR.run(eventGen.sortInTW(1.0), 2.0, type, ML, "GB");	
//				eventGen.resetListEvent();
//				CR = new Orchestrator_CR();
//				CR.run(eventGen.sortInTW(1.0), 2.0, type, ML, "LSTM");	
//				
//				eventGen.resetListEvent();
//				FR.run(eventGen.sortInTW(1.0), type);
//				eventGen.resetListEvent();
//				FR_LP.run(eventGen.sortInTW(1.0), type);
				eventGen.resetListEvent();
				reap.run(eventGen.sortInTW(1.0), type);
//				eventGen.resetListEvent();
//				sfcp.run(eventGen.sortInTW(1.0), type);
				
//			} catch (Exception e) {
//				System.out.println("Error happens");
//				System.out.println(e);
////				i --; // rerun the round
//			}
			
//			
////
//			eventGen.resetListEvent();
//			FR.run(eventGen.sortInTW(1.0), type);
//			
//			eventGen.resetListEvent();
//			reap.run(eventGen.sortInTW(1.0), type);
//
//			eventGen.resetListEvent();
//			sfcp.run(eventGen.sortInTW(1.0), type);

//			reap.run(eventGen.getListEvent());
		}

	}
}
