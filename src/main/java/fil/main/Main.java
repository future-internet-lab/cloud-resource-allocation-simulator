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

//import fil.algorithm.CR.Scheduler_CR_new;

//import fil.algorithm.FullRemapRealTime.Scheduler_FRRT;
//import fil.algorithm.FullRemapTW.Scheduler_FRTW;

public class Main {
	
	public static void main(String[] args) throws IOException, SAXException, JAXBException {
		// TODO Auto-generated method stub
		String path = "C:\\Users\\kienkk\\Downloads\\2018-weekdays-quarter (2)\\2018-weekdays-quarter.xlsx";
		/*
		 * There are two version of GenRequest based on the constructor, so be careful.
		 */
//		System.out.println("Before run");
//		Scheduler_CRO CRO = new Scheduler_CRO(); // failed 
		int days = 178; // simulation in "days" 
		String type = "24h"; // il: increase load, 24h: 24 hour simulation
		boolean ML = false;
//		double TW = 0.5;
		for(int i = 60; i < days; i ++) {
			System.out.println("Day: " + i);
			Orchestrator_CR CR = new Orchestrator_CR();
//			Scheduler_FR FR = new Scheduler_FR();
//			Scheduler_REAP reap = new Scheduler_REAP();
//			Scheduler_SFCPlanner sfcp = new Scheduler_SFCPlanner();
			GenRequest eventGen = new GenRequest(path);
//			GenRequest eventGen = new GenRequest();

//			eventGen.generate(type);
			eventGen.generate(i, 1.0);
//			Scheduler_FRTW frtw = new Scheduler_FRTW();
//			System.out.println("Size of 0.5 TW: " + eventGen.sortInTW(0.5).size());
			CR.run(eventGen.sortInTW(1.0), 1.0, type, ML);
//			eventGen.resetListEvent();
//			CR = new Orchestrator_CR();
//			CR.run(eventGen.sortInTW(1.0), 1.0, type);
//			eventGen.resetListEvent();
//			CR = new Orchestrator_CR();
//			CR.run(eventGen.sortInTW(1.0), 2.0, type);
			
//
//			eventGen.resetListEvent();
//			FR.run(eventGen.sortInTW(1.0), type);
////			
//			eventGen.resetListEvent();
//			reap.run(eventGen.sortInTW(1.0), type);
////
//			eventGen.resetListEvent();
//			sfcp.run(eventGen.sortInTW(1.0), type);

//			reap.run(eventGen.getListEvent());
		}

	}
}
