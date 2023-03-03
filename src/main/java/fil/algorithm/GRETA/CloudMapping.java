/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.GRETA;

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
	
	public boolean createVNF (int numberVNF, int type, Topology topo, boolean order) {
		
		boolean notOverLoad = true;
		LinkedList<Service> result = new LinkedList<>();
		LinkedList<PhysicalServer> listPhysical = topo.getListPhyServers();

		if(type == 2) {
			result = firstfit(listPhysical, type, numberVNF, false); // false means it can map in any server
			
		}
		else if(type == 3 || type == 4) {
			result = firstfit(listPhysical, type, numberVNF, order); // return number of success mapping
		}
		else {
			throw new java.lang.Error("Type is wrong - ServiceMapping");
		}
		
		if (result.size() < numberVNF)
			notOverLoad = false;
		
		if(result.size() > 0) {
			for(Service service : result) {
				service.setStatus("unassigned");
			}
			switch (result.getFirst().getServiceType()) {
				case "decode": this.poolDecode.addAll(result); break;
				case "density": this.poolDensity.addAll(result); break;
				case "receive": this.poolReceive.addAll(result); break;
			}
		}
		
		return notOverLoad;
	}

public LinkedList<SFC> assignVNF(Topology topo, LinkedList<SFC> listSFC, LinkedList<SFC> listTotalSFC, LinkedList<SFC> listSFCOnRpi){
		
		for(SFC sfc : listSFC) {
			for(Service service : sfc.getListService()) {
				 LinkedList<Service> poolService = this.getPoolService();
				if(service.isBelongToEdge()) {
					// in case VNF from Cloud is redirected to Edge
					// then this VNF must be deleted
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
		
		for(SFC sfc : listSFC) {
			for(Service service : sfc.getListService()) {
				if(!service.isBelongToEdge() && service.getStatus() == "unassigned") {
					LinkedList<Service> listUnSer = new LinkedList<>();
					LinkedList<Service> pool = new LinkedList<>();

					// assign VNF in VNF pool here
					switch(service.getServiceType()) {
						case "receive": pool = poolReceive; break;
						case "density": pool = poolDensity; break;
						case "decode": pool = poolDecode; break;
						default: throw new java.lang.Error();
					}
					
					for(Service ser : pool) {
						if(ser.getStatus() == "unassigned") {
							listUnSer.add(ser);
						}
					}
					// assign to an "unassigned" instance that is nearest to other instances within an SFC
					if(!listUnSer.isEmpty()) {
						Service chosenSer = new Service();
						// please uncomment the following code to use the VNF-nearest-neighbor function
						if(sfc.getNextService(service) != null && sfc.getNextService(service).getBelongToServer() != null)
							chosenSer = calDisSer(sfc.getNextService(service), listUnSer, topo);
						else if(sfc.getPreService(service) != null && sfc.getPreService(service).getBelongToServer() != null)
							chosenSer = calDisSer(sfc.getPreService(service), listUnSer, topo);
						else
							chosenSer = calBWSer(listUnSer, topo);
//						chosenSer = listUnSer.getFirst(); // comment this to use the VNF-nearest-neighbor function
						chosenSer.setStatus("assigned");
						chosenSer.setSfcID(sfc.getSfcID());
						if(listSFCOnRpi.contains(sfc)) // old service but now is being pushed to Cloud 
							this.setVNFmigration(1); 
						sfc.setService(chosenSer);
						if(chosenSer.getBelongToServer() == null)
							throw new java.lang.Error();
					}else {
						System.out.println("Pool doesn't have unassigned VNF for this type");
					}
				} // end if
			} // end for service
		} // end for sfc
					
		return listSFC;
	}
		
	public Service calDisSer(Service ser, LinkedList<Service> listSer, Topology topo){
		// nearest PM is considered in terms near - middle - far

		LinkedList<Service> listSerSort = new LinkedList<>();
		Map<Service, Integer> mapSort = new HashMap<>();
		
		int k = 10; // k = 10
		int curPM = Integer.parseInt(ser.getBelongToServer().getName());
		int curPMPod = (int) Math.ceil(curPM*1.0/(k^2/4)); // k^2/4 = number of server in a pod
		int curPMEdge_temp = (int) curPMPod % (k^2/4); // get remainder 
		int curPMEdge = (int) Math.ceil(curPMEdge_temp*1.0/(k/2)); // get edge cluster
		
		for(Service sv : listSer) {
			int nextPM = Integer.parseInt(sv.getBelongToServer().getName());
			int nextPMPod = (int) Math.ceil(nextPM*1.0/(k^2/4)); // k^2/4 = number of server in a pod
			int nextPMEdge_temp = (int) nextPMPod % (k^2/4); // get remainder 
			int nextPMEdge = (int) Math.ceil(nextPMEdge_temp*1.0/(k/2)); // get edge cluster
			// calculate distance
			if(nextPMPod == curPMPod) {
				if(nextPMEdge == curPMEdge) { // near
					if(nextPM == curPM) {
						mapSort.put(sv, 0);
					}else {
						mapSort.put(sv, 1);
					}
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
		
		for(Entry<Service, Integer> entry : mapSort.entrySet()) {
			listSerSort.add(entry.getKey());
		}
		return listSerSort.getFirst();
	}
	public Service calBWSer(LinkedList<Service> listSer, Topology topo) {
		LinkedList<PhysicalServer> listServer_temp = new LinkedList<>();
		for(Service service : listSer) {
			listServer_temp.add(service.getBelongToServer());
		}
		PhysicalServer chosen = topo.benchmarkServer(listServer_temp).getFirst();

		for(Service service : listSer) {
			if(service.getBelongToServer().equals(chosen))
				return service;
		}

		return null;
	}

	
	public void leave(LinkedList<Integer> resultPre, LinkedList<SFC> listSFCLeave , LinkedList<SFC> listTotalSFC, Topology topo) {
		// For prediction that:
		// case 1: next VNF > current VNF then no deleting, turn all leave to unassinged
		// case 2: next VNF < current VNF then delete (next - current) then turn all leave to unassigned
		// this process contains two steps:
		// step 1: delete required VNF at low utilized server
		// step 2: replace deleted VNFs with VNFs that truly need to leave
		this.setVNFmigration(0);
		
		for(int i = 0; i < resultPre.size(); i++) {
			
			int type = i + 2;			

			
			LinkedList<Service> listSer = new LinkedList<>();
			LinkedList<Service> listSerDelete = new LinkedList<>();
			
			for(SFC sfc : listSFCLeave) { // get list service out of SFC
				if(!sfc.getService(type).isBelongToEdge()) {
					listSer.add(sfc.getService(type));
				}
			}
			
			// if result of prediction is 0 then no VNF will be deleted
//			if(resultPre.get(i) != null) {
				for( int j = 0; j < resultPre.get(i) && j < listSer.size(); j ++) {
					listSerDelete.add(listSer.get(j));
					resultPre.set(i, resultPre.get(i) - 1);
				}

			// now turn all VNF in listSer to "unassigned" status
			for(Service ser : listSer) {
				ser.setStatus("unassigned");

			}
						
			LinkedList<PhysicalServer> listServer = topo.getListPhyServers();  //get list physical server
			
			// Get list servers that contains the service type
			if(type != 2)
				listServer = this.getListServerContainService(listServer, type);
			// Sort in terms of increasing utilization
			listServer = this.sortServerResource(listServer, false);
//			int c = 0;
			for(Service service : listSerDelete) {
				int serMig = 0; // service that is moved to other container is also considered migration
				if(service.getBelongToServer() == null)
					continue;
				Service RepService = null;
				SERVER:
				for(PhysicalServer server : listServer) {
					// search for the service in server
					for(Service sv : server.getListService()) {
						if(sv.getServiceType() == service.getServiceType()) {
							// prevent it chooses the service that is also in listSerDelete
							// and it does not choose service belongs to server that is 
							// at higher utilization 
							if(!listSerDelete.contains(sv) && server.getUsedCPU() < service.getBelongToServer().getUsedCPU()) {
								RepService = sv;
								break SERVER;
							}else {
								RepService = null;
							}
							
						}
					}
				} // loop server
				for(PhysicalServer server : listServer) {
					if(server.getListService().contains(service))
						break;
					if(!server.getListService().contains(service) && server.equals(listServer.getLast()))
						System.out.println("abc");
				} // loop server
				
				if(listSerDelete.contains(RepService))
					throw new java.lang.Error("abc");
				
				if(RepService != null){
					if(RepService.getStatus() == "assigned") {
						SFC sfcA = null;
						SFC sfcB = null;
						for(SFC sfc : listTotalSFC) {
							if(sfc.getListService().contains(RepService)) 
								sfcA = sfc;
							if(sfc.getListService().contains(service))
								sfcB = sfc;
						}
						if(sfcA != null && sfcB != null) {
							sfcA.setService(service);
							sfcB.setService(RepService);
						}else {
							throw new java.lang.Error();
						}
						int a = service.getSfcID();
						int b = RepService.getSfcID();
						service.setSfcID(b);
						service.setStatus("assigned");
						RepService.setSfcID(a);
					}

					this.deleteVNF(RepService);
					RepService.setReplicate("YesA");
					service.setReplicate("yes");
					serMig ++; // increase virtual service migration
					
				}else {
					service.setReplicate("yes");
					service.setStatus("assigned");
					this.deleteVNF(service);

				}
				
				if(service.equals(listSerDelete.getLast()))
						this.setVNFmigration(serMig);
			} // loop service to delete
			
			// change all service to "unassigned"
		} // loop each service
		
	}
	
	public int leaveRedundant(int numLeave, int type, Topology topo) {
		int numSer = 0;
		LinkedList<Service> removedVNF = new LinkedList<>();
		if(type == 3) {
			for(Service ser : this.poolDensity) {
				if(ser.getStatus() == "unassigned") {
					removedVNF.add(ser);
					numSer ++;
					if (numSer == numLeave) {
						break;
					}
				}
			}
			for(Service ser : removedVNF) { //actions
				PhysicalServer server = ser.getBelongToServer();
				server.setUsedCPU(-(ser.getCpu_server()));
				server.getListService().remove(ser);
				ser.setBelongToServer(null);
				if(this.poolDensity.contains(ser))
					this.poolDensity.remove(ser);
			}
		}else if(type == 4) {
			for(Service ser : this.poolReceive) {
				if(ser.getStatus() == "unassigned") {
					removedVNF.add(ser);
					numSer ++;
					if (numSer == numLeave) {
						break;
					}
				}
			}
			for(Service ser : removedVNF) { //actions
				PhysicalServer server = ser.getBelongToServer();
				server.setUsedCPU(-(ser.getCpu_server()));
				server.getListService().remove(ser);
				ser.setBelongToServer(null);
				if(this.poolReceive.contains(ser))
					this.poolReceive.remove(ser);
			}
		}else {
			throw new java.lang.Error("No service found!");
		}
		removedVNF.clear();
		// return number of leaving VNF for energy calculation
		// leave all "unsigned" VNFs that are still stay inside the system
		// decode - density - receive
//		LinkedList<Service> removedVNF = new LinkedList<>();
//		for(Service ser : this.poolDecode) {
//			if(ser.getStatus() == "unassigned") {
//				removedVNF.add(ser);
//				PhysicalServer server = ser.getBelongToServer();
//				server.setUsedCPU(-(ser.getCpu_server()));
//				server.getListService().remove(ser);
//			}
//		}
//		for(Service ser : removedVNF) {
//			ser.setBelongToServer(null);
//			if(this.poolDecode.contains(ser))
//				this.poolDecode.remove(ser);
//		}
//		numSer += removedVNF.size();
//		removedVNF.clear();
//		
//		// density
//		for(Service ser : this.poolDensity) {
//			if(ser.getStatus() == "unassigned") {
//				removedVNF.add(ser);
//				PhysicalServer server = ser.getBelongToServer();
//				server.setUsedCPU(-(ser.getCpu_server()));
//				server.getListService().remove(ser);
//			}
//		}
//		for(Service ser : removedVNF) {
//			ser.setBelongToServer(null);
//			if(this.poolDensity.contains(ser))
//				this.poolDensity.remove(ser);
//		}
//		numSer += removedVNF.size();
//		removedVNF.clear();
//		// receive
//		for(Service ser : this.poolReceive) {
//			if(ser.getStatus() == "unassigned") {
//				removedVNF.add(ser);
//				PhysicalServer server = ser.getBelongToServer();
//				server.setUsedCPU(-(ser.getCpu_server()));
//				server.getListService().remove(ser);
//			}
//		}
//		for(Service ser : removedVNF) {
//			ser.setBelongToServer(null);
//			if(this.poolReceive.contains(ser))
//				this.poolReceive.remove(ser);
//		}
//		numSer += removedVNF.size();
//		
		return numSer;
	}
	
	public LinkedList<Service> firstfit(LinkedList<PhysicalServer> listPhysical, int type, int numberVNF, boolean order) {
		
		LinkedList<PhysicalServer> listServer = new LinkedList<>();
		LinkedList<Service> result = new LinkedList<>();
		//<----Check reserved server condition
		listServer = sortServerResource(listPhysical, true);	
		//<-----Start mapping using first fit algorithm
		for(int i = 0; i < numberVNF; i ++) {
			
			Service service = new Service();
			switch(type) {
				case 2: service = new Decode(); break;
				case 3: service = new Density(); break;
				case 4: service = new Receive(); break;
				default: throw new java.lang.Error("Service unknown ");
			}
			
			for(PhysicalServer server : listServer) {
				double cpuDemand = service.getCpu_server();
				if(cpuDemand <= server.getRemainCPU()) {
					
					service.setBelongToServer(server);
					server.getListService().add(service);
					server.setUsedCPU(cpuDemand);
					result.add(service);
					break;
				}else {
					if(server.equals(listServer.getLast()))
						break;
//						throw new java.lang.Error("Please check this step");
					continue; // jump to next server
				}
			}
			
		}
		
		return result;
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

	
	public SubstrateSwitch getSwitchFromID(LinkedList<SubstrateSwitch> listSwitch, String id) {
		SubstrateSwitch s= new SubstrateSwitch();
		for(SubstrateSwitch sw: listSwitch)
			if(sw.getNameSubstrateSwitch().equals(id))
			{
				s= sw;
				break;
			}
		return s;
	}
	
	
//	public void resetRpiSFC(LinkedList<SFC> listSFC, Topology topo) {
//				
//		for(SFC sfc : listSFC) {
//			
//			double cpuSFC = 0;
//			PhysicalServer server = null;
//			boolean isSeparateSFC = sfc.isSeparateService();
//			
//			for(int i = 4; i >= 2; i--) {
//				if(!sfc.getService(i).getBelongToEdge()) {
//					//===return CPU===============================================//
//					cpuSFC = sfc.getService(i).getCpu_server();
//					server = sfc.getService(i).getBelongToServer();
//					server.setUsedCPU(-cpuSFC); // return CPU	
//					if(server.getListSFCInServer().contains(sfc))
//						server.getListSFCInServer().remove(sfc);
//					sfc.getService(i).setBelongToServer(null);// Kien fix 12052020
//				}
//			}
//			
//			//===remove sfc index inside all kind of list=========================//
//			if(isSeparateSFC == false) {
//				if(server.getListIndependRev().contains(sfc)) { // delete independent receive (if any)
//					server.getListIndependRev().remove(sfc);
//				}
//			}
//			
//			
//			//===return bandwidth for substratelink===============================//
//			LinkedList<VirtualLink> listVirLink = sfc.getvLink();
//			for(VirtualLink vLink : listVirLink) {
//				double bandwidth = vLink.getBandwidthRequest();
//				LinkedList<LinkPhyEdge> listPhyEdge = vLink.getLinkPhyEdge();
//				for(LinkPhyEdge linkEdge : listPhyEdge) {
//					linkEdge.setBandwidth(linkEdge.getBandwidth() + bandwidth);
//					linkEdge.getEdgeSwitch().setPort(linkEdge.getEdgeSwitch(), (-1.0)*bandwidth);
//				}
//				LinkedList<SubstrateLink> listSubstrate = vLink.getLinkSubstrate();
//				for(SubstrateLink linkSubstrate : listSubstrate) {
//					linkSubstrate.setBandwidth(linkSubstrate.getBandwidth() + bandwidth);
//					linkSubstrate.getStartSwitch().setPort(linkSubstrate.getEndSwitch(), (-1.0)*bandwidth);
//				}
//			}
//			listVirLink.clear(); 
//		}
//	}
//	
	public boolean remappingAggrFarGroup(VirtualLink vLink, Topology topo) {
		
		boolean isSuccess = false;
		//===remapping if 2 service connect through aggr/core switch=========//
		Service sService = vLink.getsService();
		Service dService = vLink.getdService();
		
		PhysicalServer phyA = sService.getBelongToServer();
		PhysicalServer phyB = dService.getBelongToServer();
				
		LinkedList<SFC> listSFCA = phyA.getListSFCInServer();
		LinkedList<SFC> listSFCB = phyB.getListSFCInServer();
						
		SFC sfcA = null; // sfc contains source service
		SFC sfcB = null; // sfc contains destination service
		
		for(SFC sfc: listSFCA) {
			if(sfc.getSfcID() == sService.getSfcID()) {
				sfcA = sfc;
			}
		}
		for(SFC sfc: listSFCB) {
			if(sfc.getSfcID() == sService.getSfcID()) {
				sfcB = sfc;
			}
		}
		
		
		//===get all the independent receive in server A & B ===================//
		int numReceiveA = 0;		
		numReceiveA = phyA.getListIndependRev().size();	

		//=======================================================================//
		
		//===get CPU demand for migrating dService to sService===================//
		
		double cpuDemand = dService.getCpu_server();
		double cpuReceive = 5.0;
		
		if((numReceiveA)*cpuReceive >= cpuDemand) { //neu demand nho hon so receive doc lap thi tien hanh chuyen
			double numReceiveEvacuate = Math.ceil(cpuDemand/cpuReceive);

			LinkedList<SFC> sfcEvacuate = new LinkedList<>();
			int sizeOfSFCEva = sfcEvacuate.size();
			
			for(int index = 0; index < numReceiveEvacuate; index++) {
				SFC sfc = phyA.getListIndependRev().getFirst();
				sfc.getService(4).setBelongToServer(null);
				sfcEvacuate.add(sfc);
				listSFCA.remove(sfc);
				phyA.getListIndependRev().remove(sfc);
			}
			
			LinkedList<SFC> listSFCMap = firstfit(sfcEvacuate, topo);
			// link wrong here
			if(listSFCMap.size() < sizeOfSFCEva) { // return everything
				isSuccess = false;
				returnSFC(listSFCMap);
			}
			else {
				this.setVNFmigration((int)numReceiveEvacuate + 1);
				phyA.setUsedCPU(-(numReceiveEvacuate)*cpuReceive + cpuDemand);
				phyB.setUsedCPU(-cpuDemand);
				dService.setBelongToServer(sService.getBelongToServer());
				if(sfcA.allServiceInSameServer()) {
					sfcA.setSeparateService(false);
					
				}
				if(!sfcB.existServiceInServer(phyB))
					listSFCB.remove(sfcB);
				
				isSuccess = true;
			}
			
		}
		else {
			isSuccess = false;
		}
		return isSuccess;
	}

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
	
	public LinkedList<PhysicalServer> sortServerResource(LinkedList<PhysicalServer> listServer, boolean order){
		//order = true: low to high, false means high to low
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
	public double getCPUserverNonZero(Topology topo) {
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
	
	public double getCPUServerUtil(Topology topo) {
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();
		double cpu = 0.0;
		double numSV = 250;
		for(PhysicalServer sv : listServer) {
			if(sv.getUsedCPU() > 0) {
				cpu += sv.getUsedCPU();
//				svRun ++;
			}
		}
		return cpu*1.0/numSV;
	}
	
	public LinkedList<PhysicalServer> getListReservedServer(LinkedList<PhysicalServer> listPhysical, int serviceType) {
			
		LinkedList<PhysicalServer> temp = new LinkedList<>();
		for(PhysicalServer a : listPhysical) {
			int name = Integer.parseInt(a.getName());
			if(name % 2 == 0 && serviceType == 4) { // even number
				temp.add(a);
			}
			if(name % 2 != 0 && serviceType == 3) { // even number
				temp.add(a);
			}
		}
		return temp;
	}
	
	public LinkedList<PhysicalServer> getListServerContainService(LinkedList<PhysicalServer> listPhysical, int serviceType) {
		
		Service service = new Service();
		switch(serviceType) {
			case 2: service = new Decode(); break;
			case 3: service = new Density(); break;
			case 4: service = new Receive(); break;
			default: throw new java.lang.Error("Service unknown - ServiceMapping");
		}
		
		LinkedList<PhysicalServer> temp = new LinkedList<>();
		for(PhysicalServer server : listPhysical) {
			for(Service ser : server.getListService()) {
				if(ser.getServiceType() == service.getServiceType()) {
					temp.add(server); break;
				}
			}
		}
		return temp;
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
				if(check == true) {
					power += phy.warmPowerCal();
				}else {
					phy.setPowerServer();
					power += phy.getPowerServer();
				}
				
			}
			else
				continue;
		}
		return power;
	}

	public double getMigEnergy(Topology topo) {
		double energy = 0;
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers(); 
		for(PhysicalServer phy : listServer) {
			if(phy.getState() == 1) {
				energy += phy.getMigEnergy();
			}
		}
		return energy;
	}
	
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
	
	
	public int getVNFmigration() {
		return VNFmigration;
	}

	public void setVNFmigration(int vNFmigration) {
		VNFmigration += vNFmigration;
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

	public LinkedList<Service> getPoolUnDensity() {
		LinkedList<Service> list = new LinkedList<>();
		for(Service ser : poolDensity) {
			if(ser.getStatus() == "unassigned")
				list.add(ser);
		}
		return list;
	}
	
	public void setPoolDensity(LinkedList<Service> poolDensity) {
		this.poolDensity = poolDensity;
	}

	public LinkedList<Service> getPoolReceive() {
		return poolReceive;
	}
	
	public LinkedList<Service> getPoolUnReceive() {
		LinkedList<Service> list = new LinkedList<>();
		for(Service ser : poolReceive) {
			if(ser.getStatus() == "unassigned")
				list.add(ser);
		}
		return list;
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
