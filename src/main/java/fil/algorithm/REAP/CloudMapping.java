/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.REAP;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import fil.resource.substrate.*;
import fil.resource.virtual.*;


public class CloudMapping {
	private int VNFmigration;
	private LinkedList<Service> poolDecode;
	private LinkedList<Service> poolDensity;
	private LinkedList<Service> poolReceive;
	
	public CloudMapping() {
//		this.setVNFmigration(0);
		this.poolDecode = new LinkedList<Service>();
		this.poolDensity = new LinkedList<Service>();
		this.poolReceive = new LinkedList<Service>();
	}
	
	public boolean scaleOut (SFC sfc, Service ser, Topology topo) {
		// scale out to nearest PM of the previous VNF, if no previous VNF exists, then scale
		// out following first-fit 
		// check if previous VNF exists
		boolean notOverLoad = true;
		boolean result = false;
		Service preSer = sfc.getPreService(ser);
		// check if previous ser exists and assigned
		if(preSer != null && !preSer.isBelongToEdge() && preSer.getStatus() != "unassigned") {
			// nearest PM mapping
			result = mapping(ser, preSer, topo, "reap");
		}else {
			// first fit mapping
			result = mapping(ser, preSer, topo, "firstfit");
		}
		
		
		if (!result) {
			notOverLoad = false;
		}else{
			ser.setStatus("unassigned");
			switch (ser.getServiceType()) {
				case "decode": this.poolDecode.add(ser); break;
				case "density": this.poolDensity.add(ser); break;
				case "receive": this.poolReceive.add(ser); break;
			}
		}
		
		return notOverLoad;
	}

	public LinkedList<SFC> redirect(LinkedList<SFC> listSFC, Topology topo, LinkedList<SFC> listTotalSFC, LinkedList<SFC> listSFCOnRpi){
		// block for checking VNF's status
		for(SFC sfc : listSFC) {
			for(Service service : sfc.getListService()) {
				 LinkedList<Service> poolService = this.getPoolService();
				if(service.isBelongToEdge()) {
					// incase VNF from Cloud is pushed back to Edge
					// then this VNF must deleted
					if(poolService.contains(service)) {
						this.deleteVNF(service);
						this.setVNFmigration(1); 
					}
				}else {
//					// in case some VNFs was changed status after remapping
//					// even its position is not changed
					// then it must be recovered to its initial status

					if(poolService.contains(service)) {
						service.setStatus("assigned");
						
					}
					
				}
			}
		}
		
		// block for assigning VNF
		for(SFC sfc : listSFC) {
			for(Service service : sfc.getListServiceCloud()) {
				if(service.getStatus() == "unassigned") {
					// get listServer has chosen type of service
					// sort listServer following a specific strategy
					Map<PhysicalServer, Integer> listServer = this.getListServerHasFreeSer(topo.getListPhyServers(), service.getServiceType());
					if(listServer.size() == 0)
						break; 
					// get one by one server that satisfies the delay constraint --> TBD
					// first one of the above map
					Map.Entry<PhysicalServer, Integer> entry = listServer.entrySet().iterator().next();
					PhysicalServer chosenSer = entry.getKey();
					for(Service ser : chosenSer.getListService()) {
						if(ser.getStatus() == "unassigned" && ser.getServiceType() == service.getServiceType()) {
							ser.setStatus("assigned");
							ser.setSfcID(sfc.getSfcID());
							sfc.setService(ser);
							if(listSFCOnRpi.contains(sfc)) // old service but now is being pushed to Cloud 
								this.setVNFmigration(1); 
							break;
						}
					}
				}
			}
			// check if all VNF on cloud has been assigned
			
		}
		
		
		return listSFC;
	}
	
	public void leave(LinkedList<SFC> listSFCLeave) {
		LinkedList<Service> listSer = new LinkedList<>();
		
		for(SFC sfc : listSFCLeave) { 
			listSer.addAll(sfc.getListServiceCloud());
		}
		
		for(Service ser : listSer) { // keep VNF alive but service will be removed
			ser.setStatus("unassigned");
			ser.setSfcID(-1);
		}
	}
	
	public void leaveIdle(Topology topo) { // delete all idle VNFs
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();
		for(PhysicalServer server : listServer) {
			for(Service ser : server.getListService()) {
				if(ser.getStatus() == "unassigned") {
					if(ser.equals(server.getListService().getLast())) {
						// delete these VNF
						for(Service ser1 : server.getListService()) {
//							ser1.setBelongToServer(null);		
							switch (ser1.getServiceType()) {
							case "decode": this.poolDecode.remove(ser1); break;	
							case "density": this.poolDensity.remove(ser1); break;
							case "receive": this.poolReceive.remove(ser1); break;
							}
						}
						// reset server
						server.reset();
						break;
					}else {
						continue;
					}
				}else { // assigned VNF exists
					break;
				}
				
			}
		}
	}
	
	public void leaveRedundant(Topology topo) {
		// leave all "unsigned" VNFs that are still stay inside the system
		// decode - density - receive
		LinkedList<Service> removedVNF = new LinkedList<>();
		for(Service ser : this.poolDecode) {
			if(ser.getStatus() == "unassigned") {
				removedVNF.add(ser);
				PhysicalServer server = ser.getBelongToServer();
				server.setUsedCPU(-(ser.getCpu_server()));
				server.getListService().remove(ser);
			}
		}
		for(Service ser : removedVNF) {
			ser.setBelongToServer(null);
			if(this.poolDecode.contains(ser))
				this.poolDecode.remove(ser);
		}
		removedVNF.clear();
		
		// density
		for(Service ser : this.poolDensity) {
			if(ser.getStatus() == "unassigned") {
				removedVNF.add(ser);
				PhysicalServer server = ser.getBelongToServer();
				server.setUsedCPU(-(ser.getCpu_server()));
				server.getListService().remove(ser);
			}
		}
		for(Service ser : removedVNF) {
			ser.setBelongToServer(null);
			if(this.poolDensity.contains(ser))
				this.poolDensity.remove(ser);
		}
		removedVNF.clear();
		// receive
		for(Service ser : this.poolReceive) {
			if(ser.getStatus() == "unassigned") {
				removedVNF.add(ser);
				PhysicalServer server = ser.getBelongToServer();
				server.setUsedCPU(-(ser.getCpu_server()));
				server.getListService().remove(ser);
			}
		}
		for(Service ser : removedVNF) {
			ser.setBelongToServer(null);
			if(this.poolReceive.contains(ser))
				this.poolReceive.remove(ser);
		}
	}
	
	public boolean mapping(Service ser, Service preSer, Topology topo, String method) {
		
		LinkedList<PhysicalServer> listServer = new LinkedList<>();
		boolean result = false;
		//<----Check reserved server condition
//		if(!order) {
		if(method == "firstfit")
			listServer = sortServerResource(topo, true);
		else if(method == "reap") {	
			listServer = calDisSer(preSer, topo);
		}else
			throw new java.lang.Error();
		
		//<-----Start mapping using first fit algorithm
		
		for(PhysicalServer server : listServer) {
			double cpuDemand = ser.getCpu_server();
			if(cpuDemand <= server.getRemainCPU()) {
				
				ser.setBelongToServer(server);
				server.getListService().add(ser);
				server.setUsedCPU(cpuDemand);
				result = true;
				break;
			}else {
				if(server.equals(listServer.getLast()))
					break;
//						throw new java.lang.Error("Please check this step");
				continue; // jump to next server
			}
		}
					
		return result;
	}
	
	public void migration(Topology topo, LinkedList<SFC> listSFC, SFC sfc) {
		// REAP chooses either scaleOut or migration for VNF that fails to be natively placed
		// or redirected to existing VNF instance. This process migrates required VNF service
		// to a running VNF instance that will scale up to fit the required one. However, as
		// our scenario utilizes container as virtual unit, scaling up is feasible in theory
		// but inefficient and almost infeasible in practice, thus migration is NO-USE in our 
		// case.
	}
	
	public LinkedList<PhysicalServer> calDisSer(Service ser, Topology topo){
		// nearest PM is considered in terms near - middle - far
		LinkedList<PhysicalServer> listSer = topo.getListPhyServers();
		LinkedList<PhysicalServer> listSerSort = new LinkedList<>();
		Map<PhysicalServer, Integer> mapSort = new HashMap<>();
		
		int k = 10; // k = 10
		int prePM = Integer.parseInt(ser.getBelongToServer().getName());
		int prePMPod = (int) Math.ceil(prePM*1.0/(k^2/4)); // k^2/4 = number of server in a pod
		int prePMEdge_temp = (int) prePMPod % (k^2/4); // get remainder 
		int prePMEdge = (int) Math.ceil(prePMEdge_temp*1.0/(k/2)); // get edge cluster
		
		for(PhysicalServer sv : listSer) {
			int curPM = Integer.parseInt(sv.getName());
			int curPMPod = (int) Math.ceil(curPM*1.0/(k^2/4)); // k^2/4 = number of server in a pod
			int curPMEdge_temp = (int) curPMPod % (k^2/4); // get remainder 
			int curPMEdge = (int) Math.ceil(curPMEdge_temp*1.0/(k/2)); // get edge cluster
			// calculate distance
			if(prePMPod == curPMPod) {
				if(prePMEdge == curPMEdge) { // near
					mapSort.put(sv, 1);
				}else { // middle
					mapSort.put(sv, 2); // increase distance
				}	
			}else { // far
				mapSort.put(sv, 3); // increase distance
			}
		}
		// sort map
		// sorted number of unassigned VNF decreasing , already tested
		mapSort = mapSort.entrySet().stream()
				 .sorted(Map.Entry.comparingByValue())
				 .collect(Collectors.toMap(
				 Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		for(Entry<PhysicalServer, Integer> entry : mapSort.entrySet())
			listSerSort.add(entry.getKey());
		return listSerSort;
	}
	
	public void deleteVNF(Service ser) {
		PhysicalServer server = ser.getBelongToServer();
		server.getListService().remove(ser);
		server.setUsedCPU(-(ser.getCpu_server())); // return CPU for this VNF
		if(server.getUsedCPU() > 100)
			throw new java.lang.Error("abc");
		ser.setBelongToServer(null);
		
		switch (ser.getServiceType()) {
		case "decode": this.poolDecode.remove(ser); break;	
		case "density": this.poolDensity.remove(ser); break;
		case "receive": this.poolReceive.remove(ser); break;
		}
	}
	
	public void deleteSFC(SFC sfc) {
		for(Service ser : sfc.getListService()) {
			if(!ser.isBelongToEdge() && ser.getBelongToServer() != null) {
				PhysicalServer server = ser.getBelongToServer();
				server.getListService().remove(ser);
				server.setUsedCPU(-(ser.getCpu_server())); // return CPU for this VNF
				ser.setBelongToServer(null);
				
				switch (ser.getServiceType()) {
				case "decode": this.poolDecode.remove(ser); break;	
				case "density": this.poolDensity.remove(ser); break;
				case "receive": this.poolReceive.remove(ser); break;
				}
			}
		}
	}
	
	public void consolidation (Topology topo) {
		//<-----Kien 26-10-2020
//		double threshold = 90.0;
//		LinkedList<PhysicalServer> listServerConso = new LinkedList<>();
//		for(PhysicalServer server : topo.getListPhyServers()) {
//			if(server.getUsedCPU() < threshold)
//				listServerConso.add(server);
//		}
//		listServerConso = sortServerResource(listServerConso);
		//<-----TBD
	}
	
	
 	public LinkedList<SFC> firstfit(LinkedList<SFC> listSFC, Topology topo) {
		
		LinkedList<SFC> listMappedSFC = new LinkedList<>(); 
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();

		//====Start First-Fit algorithm to map VNF into physical server ===========//
		sfc_loop:
		for(SFC sfc : listSFC) {
			server_loop:
			for(PhysicalServer server : listServer) {
				
				double cpuServer = 0;
				for(int i = 4; i >= 2; i--) { // only consider services on cloud
					if(!sfc.getService(i).getBelongToEdge()) {
						if(sfc.getService(i).getBelongToServer() == null) { // service hasnt been mapped to any server
							cpuServer += sfc.getService(i).getCpu_server();  //calculate all cpu used by chain
						}
					}
				}
				
				if(cpuServer <= server.getRemainCPU()) {
					// finish here
					// break loop server + condition to break server loop
					listMappedSFC.add(sfc);
					// set sfc position
					for(int i = 4; i >= 2; i--) {
						if(!sfc.getService(i).getBelongToEdge()) {
							if(sfc.getService(i).getBelongToServer() == null)
								sfc.getService(i).setBelongToServer(server);
						}
					}
					server.getListSFCInServer().add(sfc);
					server.setUsedCPU(cpuServer);
					// only Receive on cloud will be added to the below list 
					if(sfc.getFirstServiceCloud().getServiceType() == "receive") {
						server.getListIndependRev().add(sfc);
					}
					break server_loop;
				}
				else { // case cpu is not enough for the whole sfc
					
					if(listServer.indexOf(server) == (listServer.size() - 1)) {
						continue sfc_loop;
					}
					
					if(sfc.getService(3).getBelongToEdge()) {
						continue; // if only service 4 cannot map then it cannot be separated
					}
					for(int i = 2; i <= 4; i++) { // only consider services on cloud
						if(!sfc.getService(i).getBelongToEdge()) {
							if(sfc.getService(i).getBelongToServer() == null) { // service hasnt been mapped to any server
								double cpuRemain = sfc.getService(i).getCpu_server();
								if(cpuRemain <= server.getRemainCPU()) {
									// finish separated sfc here
									// do not break loop server
									server.setUsedCPU(cpuRemain);
									server.getListSFCInServer().add(sfc);
									sfc.getService(i).setBelongToServer(server);
									sfc.setSeparateService(true);
								}
								else {
									//continue server_loop;
								}
							} // what happen if belongtoserver != null??
						}
					} // end for i loop
					
				}
			} // end server_loop
		} // end sfc_loop
		
		return listMappedSFC;
	}

	
//	public SubstrateSwitch getSwitchFromID(LinkedList<SubstrateSwitch> listSwitch, String id) {
//		SubstrateSwitch s= new SubstrateSwitch();
//		for(SubstrateSwitch sw: listSwitch)
//			if(sw.getNameSubstrateSwitch().equals(id))
//			{
//				s= sw;
//				break;
//			}
//		return s;
//	}
	
//	public boolean remappingAggrFarGroup(VirtualLink vLink, Topology topo) {
//		
//		boolean isSuccess = false;
//		//===remapping if 2 service connect through aggr/core switch=========//
//		Service sService = vLink.getsService();
//		Service dService = vLink.getdService();
//		
//		PhysicalServer phyA = sService.getBelongToServer();
//		PhysicalServer phyB = dService.getBelongToServer();
//				
//		LinkedList<SFC> listSFCA = phyA.getListSFCInServer();
//		LinkedList<SFC> listSFCB = phyB.getListSFCInServer();
//						
//		SFC sfcA = null; // sfc contains source service
//		SFC sfcB = null; // sfc contains destination service
//		
//		for(SFC sfc: listSFCA) {
//			if(sfc.getSfcID() == sService.getSfcID()) {
//				sfcA = sfc;
//			}
//		}
//		for(SFC sfc: listSFCB) {
//			if(sfc.getSfcID() == sService.getSfcID()) {
//				sfcB = sfc;
//			}
//		}
//		
//		
//		//===get all the independent receive in server A & B ===================//
//		int numReceiveA = 0;		
//		numReceiveA = phyA.getListIndependRev().size();	
//
//		//=======================================================================//
//		
//		//===get CPU demand for migrating dService to sService===================//
//		
//		double cpuDemand = dService.getCpu_server();
//		double cpuReceive = 5.0;
//		
//		if((numReceiveA)*cpuReceive >= cpuDemand) { //neu demand nho hon so receive doc lap thi tien hanh chuyen
//			double numReceiveEvacuate = Math.ceil(cpuDemand/cpuReceive);
//
//			LinkedList<SFC> sfcEvacuate = new LinkedList<>();
//			int sizeOfSFCEva = sfcEvacuate.size();
//			
//			for(int index = 0; index < numReceiveEvacuate; index++) {
//				SFC sfc = phyA.getListIndependRev().getFirst();
//				sfc.getService(4).setBelongToServer(null);
//				sfcEvacuate.add(sfc);
//				listSFCA.remove(sfc);
//				phyA.getListIndependRev().remove(sfc);
//			}
//			
//			LinkedList<SFC> listSFCMap = firstfit(sfcEvacuate, topo);
//			// link wrong here
//			if(listSFCMap.size() < sizeOfSFCEva) { // return everything
//				isSuccess = false;
//				returnSFC(listSFCMap);
//			}
//			else {
//				this.setVNFmigration((int)numReceiveEvacuate + 1);
//				phyA.setUsedCPU(-(numReceiveEvacuate)*cpuReceive + cpuDemand);
//				phyB.setUsedCPU(-cpuDemand);
//				dService.setBelongToServer(sService.getBelongToServer());
//				if(sfcA.allServiceInSameServer()) {
//					sfcA.setSeparateService(false);
//					
//				}
//				if(!sfcB.existServiceInServer(phyB))
//					listSFCB.remove(sfcB);
//				
//				isSuccess = true;
//			}
//			
//		}
//		else {
//			isSuccess = false;
//		}
//		return isSuccess;
//	}

	public void returnSFC(LinkedList<SFC> listSFC) {
		for(SFC sfc : listSFC) {
			double cpu;
			PhysicalServer server = null;
			for(int i = 4; i >= 2; i --) {
				if(!sfc.getService(i).isBelongToEdge()) {
					cpu = sfc.getService(i).getCpu_server();
					server = sfc.getService(i).getBelongToServer();
					server.setUsedCPU(-cpu);
					if(server.getListSFCInServer().contains(sfc))
						server.getListSFCInServer().remove(sfc);
					if(server.getListIndependRev().contains(sfc))
						server.getListIndependRev().remove(sfc);
				}
				else
					break;
			}
			
		}
	}

//	public Map<Integer, PhysicalServer> sortListServer(LinkedList<PhysicalServer> listPhysical) {
//		
//		//create a list from elements of HashMap
//		LinkedList<Map.Entry<Integer, PhysicalServer> > listMap = new LinkedList<Map.Entry<Integer, PhysicalServer>>(listPhysical.entrySet());
//		
//		//sort the list
//		Collections.sort(listMap, new Comparator<Integer, PhysicalServer>() {
//			@Override
//			public int compare(Map.Entry<Integer, PhysicalServer> o1, Map.Entry<Integer, PhysicalServer> o2) {
//				int cpuCompare = 0;
//				if (o1.getValue().getUsedCPU() < o2.getValue().getUsedCPU()) {
//					cpuCompare = -1;
//				}
//				if (o1.getValue().getUsedCPU() > o2.getValue().getUsedCPU()) {
//					cpuCompare = 1;
//				}
//				if (o1.getValue().getUsedCPU() == o2.getValue().getUsedCPU()) {
//					cpuCompare = 0;
//				}
//				int nameCompare = o1.getValue().getName().compareTo(o2.getValue().getName()); 
//				  
//	            // 2-level comparison using if-else block 
//	            if (cpuCompare == 0) { 
//	                return nameCompare; 
//	            } else { 
//	                return cpuCompare; 
//	            } 
//			}
//		});
//		return listPhysical;
//	}
	
	public LinkedList<PhysicalServer> sortServerResource(Topology topo, boolean order){
		//order = true: low to high, false means high to low
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();
		Collections.sort(listServer, new Comparator<PhysicalServer>() {
			@Override
	        public int compare(PhysicalServer server1, PhysicalServer server2) { 
	            // for comparison
				int cpuCompare = 0;
				if(server1.getUsedCPU() < server2.getUsedCPU()) {
					if(order)
						cpuCompare = 1;
					else
						cpuCompare = -1;
				}
				else if(server1.getUsedCPU() > server2.getUsedCPU()) {
					if(order)
						cpuCompare = -1;
					else
						cpuCompare = 1;
				}
				else if(server1.getUsedCPU() == server2.getUsedCPU())
					cpuCompare = 0;
				
				int nameCompare =server1.getName().compareTo(server2.getName()); 
				  
	            // 2-level comparison using if-else block 
	            if (cpuCompare == 0) { 
	                return nameCompare; 
	            } else { 
	                return cpuCompare; 
	            } 
	        }
		});
		return listServer;
	}
	
//	public static void main(String args[]) {
//		int name = 0;
//		Random b = new Random();
//		LinkedList<PhysicalServer> list = new LinkedList<>();
//		for(int i = 0; i < 10; i ++) {
//			PhysicalServer a = new PhysicalServer(Integer.toString(name));
//			a.setUsedCPU(100*b.nextDouble());
//			list.add(a);
//		}
//		CloudMapping cloud = new CloudMapping();
//		list = cloud.sortServerResource(list, false);
//		for(int i = 0; i < list.size(); i ++) {
//			System.out.println("Cloud order: " + list.get(i).getUsedCPU());
//		}
//	}
	public double aveSerUtil(Topology topo) {
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();
		double cpu = 0.0;
		double svRun = 0.0;
		for(PhysicalServer sv : listServer) {
			if(sv.getUsedCPU() > 0) {
				cpu += sv.getUsedCPU();
				svRun ++;
			}
		}
		return cpu*1.0/svRun;
	}
//	public LinkedList<PhysicalServer> getListReservedServer(LinkedList<PhysicalServer> listPhysical, int serviceType) {
//			
//		LinkedList<PhysicalServer> temp = new LinkedList<>();
//		for(PhysicalServer a : listPhysical) {
//			int id = a.getID();
//			if(id % 2 == 0 && serviceType == 4) { // even number
//				temp.add(a);
//			}
//			if(id % 2 != 0 && serviceType == 3) { // even number
//				temp.add(a);
//			}
//		}
//		return temp;
//	}
//	
	public int getUsedServer(Topology topo) {
		int usedServer = 0;
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers(); 
		for(PhysicalServer phy : listServer) {
			if(phy.getState() == 1) {
				usedServer ++;
			}
		}
		return usedServer;
	}
	
	
	
	public Map<PhysicalServer, Integer> getListServerHasFreeSer(LinkedList<PhysicalServer> listPhysical, String serviceType) {
		
		Map<PhysicalServer, Integer> temp_map = new HashMap<>();
		for(PhysicalServer server : listPhysical) {
			// count number of unassigned VNF inside these PM
			int count_ser = 0;
			for(Service ser : server.getListService()) {
				if(ser.getServiceType() == serviceType && ser.getStatus() == "unassigned") {
					count_ser ++;
				}
			}
			if(count_ser != 0) {
				temp_map.put(server, count_ser);
			}
		}
		// sorted number of unassigned VNF decreasing 
		temp_map = temp_map.entrySet().stream()
			       .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			       .collect(Collectors.toMap(
			       Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		
		return temp_map;
	}
	
	public double getPowerServer(Topology topo) {
		double power = 0;
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers(); 
		for(PhysicalServer phy : listServer) {
			if(phy.getState() == 1) {
				phy.setPowerServer();
				power += phy.getPowerServer();
			}
			else
				continue;
		}
		return power;
	}
	
	public double getPowerWasted(Topology topo){ // = power warm + power idle device 
		double power = 0;
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers(); 
		for(PhysicalServer phy : listServer) {
			if(phy.getState() == 1) {
				boolean check = false;
				for(Service ser : phy.getListService()) {
					if(ser.getStatus() != "unassigned") {
						check = true;
						break;
					}
				}
				if(check == true) { // the base power is useful
					power += phy.warmPowerCal();
				}else { // base power is useless, hence consider it as wasted power
					phy.setPowerServer(); 
					power += phy.getPowerServer();
				}
				
			}
			else
				continue;
		}
		return power;
	}


	public int getVNFmigration() {
		return VNFmigration;
	}

	public void setVNFmigration(int vNFmigration) {
		VNFmigration = vNFmigration;
	}

	public LinkedList<Service> getPoolDecode() {
		return poolDecode;
	}

	public void setPoolDecode(LinkedList<Service> poolDecode) {
		this.poolDecode = poolDecode;
	}

	public LinkedList<Service> getPoolDensity() {
		return poolDensity;
	}

	public void setPoolDensity(LinkedList<Service> poolDensity) {
		this.poolDensity = poolDensity;
	}

	public LinkedList<Service> getPoolReceive() {
		return poolReceive;
	}

	public void setPoolReceive(LinkedList<Service> poolReceive) {
		this.poolReceive = poolReceive;
	}

	public LinkedList<Service> getPoolService() {
		 LinkedList<Service> poolService = new  LinkedList<Service>();
		poolService.addAll(this.poolDecode);
		poolService.addAll(this.poolDensity);
		poolService.addAll(this.poolReceive);
		return poolService;
	}

//	public void setPoolService(LinkedList<Service> poolService) {
//		this.poolService = poolService;
//	}

}
