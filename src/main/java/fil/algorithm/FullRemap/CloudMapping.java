/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.FullRemap;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;

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
			// sort server here
			if(order)
				listPhysical = getListReservedServer(listPhysical, type);
			
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

	public LinkedList<SFC> assignVNF(Topology topo, LinkedList<SFC> listSFC){
		
		for(SFC sfc : listSFC) {
			for(Service service : sfc.getListService()) {
				 LinkedList<Service> poolService = this.getPoolService();
				if(service.isBelongToEdge()) {
					// incase VNF from Cloud is pushed to Edge
					// then this VNF must deleted
					if(poolService.contains(service)) {
						this.deleteVNF(service);
					}
				}else {
//					// in case some VNFs was changed status after remapping
//					// even its position is not changed
					// then it must be recovered to its initial status

					if(poolService.contains(service)) {
						service.setStatus("assigned");
						continue;
					}
					// assign VNF in VNF pool here
					switch(service.getServiceType()) {
					case "receive": 
						for(Service ser : poolReceive) {
							if(ser.getStatus() == "unassigned") {
								ser.setStatus("assigned");
								ser.setSfcID(sfc.getSfcID());
								sfc.setService(ser);
								break;
							}
							// if pool doesn't have any unassigned VNF
							if(ser.equals(poolReceive.getLast())) {
								System.out.println("Pool doesn't have unassigned VNF for this type");
								break;
							}
						}; break;
					
					case "density": 
						for(Service ser : poolDensity) {
							if(ser.getStatus() == "unassigned") {
								ser.setStatus("assigned");
								ser.setSfcID(sfc.getSfcID());
								sfc.setService(ser);
								break;
							}
							// if pool doesn't have any unassigned VNF
							if(ser.equals(poolDensity.getLast())) {
								System.out.println("Pool doesn't have unassigned VNF for this type");
								break;
							}
						}; break;
					
					case "decode": 
						for(Service ser : poolDecode) {
							if(ser.getStatus() == "unassigned") {
								ser.setStatus("assigned");
								ser.setSfcID(sfc.getSfcID());
								sfc.setService(ser);
								break;
							}
							// if pool doesn't have any unassigned VNF
							if(ser.equals(poolDecode.getLast())) {
								System.out.println("Pool doesn't have unassigned VNF for this type");
								break;
							}
						}; break;
						
					}
				}
			}
			// check if all VNF on cloud has been assigned
			
		}
		
		
		return listSFC;
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
	
	public LinkedList<Service> firstfit(LinkedList<PhysicalServer> listPhysical, int type, int numberVNF, boolean order) {
		
		LinkedList<PhysicalServer> listServer = new LinkedList<>();
		LinkedList<Service> result = new LinkedList<>();
		//<----Check reserved server condition
//		if(!order) {
		listServer = sortServerResource(listPhysical, false);
		

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
		ser.setBelongToServer(null);
		
		switch (ser.getServiceType()) {
		case "decode": this.poolDecode.remove(ser); break;	
		case "density": this.poolDensity.remove(ser); break;
		case "receive": this.poolReceive.remove(ser); break;
		}
	}
	
	public void deleteSFC(SFC sfc) {
		for(Service ser : sfc.getListService()) {
			if(ser.getBelongToServer() != null) {
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
	
//	public void consolidation (Topology topo, LinkedList<SFC> listSFCTotal, LinkMapping linkMapping) {
//		// reset all servers
//		LinkedList<PhysicalServer> listServer = topo.getListPhyServers();
//		LinkedList<SFC> listMappedSFC = new LinkedList<SFC>();
//		
//		// reset server
//		for(PhysicalServer sv : listServer) {
//			sv.reset();
//		}
//		// reset links
//		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
//		for(SubstrateLink link : listLinkBandwidth) {
//			link.reset();
//		}
//		LinkedList<LinkPhyEdge> listLinkPhyEdge = topo.getListLinkPhyEdge();
//		for(LinkPhyEdge link : listLinkPhyEdge) {
//			link.reset();
//		}
//		
//		listMappedSFC = this.firstfit(listSFCTotal, topo);		
//		
//		if(listMappedSFC.size() != listSFCTotal.size())
//			throw new java.lang.Error("Problem");
//		
//		// link mapping 
//		LinkedList<SFC> SFCLinkFailed = new LinkedList<SFC>();
//
//		SFCLinkFailed = linkMapping.linkMapExternal(topo, listMappedSFC);
//	
//		SFCLinkFailed = linkMapping.linkMapInternal(topo, listMappedSFC);
//	
//	}
//	
	
 	public boolean firstfit(LinkedList<SFC> listSFC, Topology topo) {
		
 		boolean result = false;
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
							server.getListService().add(sfc.getService(i));
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
									server.getListService().add(sfc.getService(i));
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
		if(listMappedSFC.size() == listSFC.size()) {
			result = true;
		}
		else {
			result = false;
		}
		return result;
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
	

	public LinkedList<PhysicalServer> sortServerResource(LinkedList<PhysicalServer> listServer, boolean order){
		//order = true: low to high, false means high to low
		Collections.sort(listServer, new Comparator<PhysicalServer>() {
			@Override
	        public int compare(PhysicalServer server1, PhysicalServer server2) { 
	            // for comparison
				int cpuCompare = 0;
				if(server1.getUsedCPU() < server2.getUsedCPU()) {
					if(order)
						cpuCompare = -1;
					else
						cpuCompare = 1;
				}
				else if(server1.getUsedCPU() > server2.getUsedCPU()) {
					if(order)
						cpuCompare = 1;
					else
						cpuCompare = -1;
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
				phy.setPowerServerNom();
				power += phy.getPowerServer();
			}
			else
				continue;
		}
		return power;
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
	
	public double getMigEnergy(Topology topo) {
		double energy = 0;
		LinkedList<PhysicalServer> listServer = topo.getListPhyServers(); 
		for(PhysicalServer phy : listServer) {
			if(phy.getState() == 1) {
				energy += phy.getMigEnergy();
			}
			else
				continue;
		}
		return energy;
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
