/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.agorithm.SFCPlanner;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import fil.resource.substrate.Node;
import fil.resource.substrate.PhysicalServer;
import fil.resource.substrate.Rpi;
import fil.resource.substrate.SubstrateLink;
import fil.resource.virtual.Event;
import fil.resource.virtual.FatTree;
import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;
import fil.resource.virtual.Topology;
import fil.resource.virtual.VirtualLink;



public class Scheduler_SFCPlanner_depricated {
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int MAX_SFC_PI = 7;
	final static int HOUR = 3600;
	final static double THOUS = 1000.0;
	final static double MIL = 1000000.0;
	final static double INIENERGY = 15.68; // calculate by power*time/number of pod
	final static double DELENERGY = 14.39; // calculate by power*time/number of pod	
	private double totalEnergy;
	private double iniEnergy;
	private double delEnergy;
	private int error;
	private int sfcID;
	private Topology topo;
	private FatTree fatTree;
	private boolean isSuccess;
	private LinkedList<SFC> listSFCTotal;
	private LinkedList<Integer> edgePosition;

	private HashMap<Integer, LinkedList<Double>> feedDataChecker;
	private HashMap<Integer, LinkedList<SFC>> listSFCAllRpi;
	
	public Scheduler_SFCPlanner_depricated()  {
		edgePosition = new LinkedList<>();
		edgePosition.add(10);
		edgePosition.add(5);
		edgePosition.add(13);
		edgePosition.add(14);
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH, NUM_PI, edgePosition);
		listSFCTotal = new LinkedList<>();
		listSFCAllRpi = new HashMap<>();
		for(int i = 0; i < NUM_PI; i ++) {
			listSFCAllRpi.put(i, new LinkedList<SFC>());
		}
		totalEnergy = 0.0;
		iniEnergy = 0.0;
		delEnergy = 0.0;
		sfcID = 0;
		isSuccess = false;
	}
	
	//<-----Prediction will be fetched every time this function is invoked
	public void run(LinkedList<LinkedList<Event>> listTotalEvent, String type) {
		
	
		LinkedList<Integer> listReqTW = new LinkedList<>();
		LinkedList<Integer> listReqLvTW = new LinkedList<>();
		LinkedList<Integer> listSFCActive = new LinkedList<>();
		LinkedList<Integer> listError = new LinkedList<>();
		LinkedList<Integer> listAcceptTW = new LinkedList<>();
		LinkedList<Double> listAveSerUtil = new LinkedList<>();
		LinkedList<Double> listTotalPower = new LinkedList<>();
		LinkedList<Double> listAcceptance = new LinkedList<>();
		LinkedList<Double> listEnergy = new LinkedList<>();
		LinkedList<Double> listDownTime = new LinkedList<>();
		LinkedList<Double> listDownTimePerSFC = new LinkedList<>();

		double time4Energy = 0.0;
		
		for(int eventInTW = 0; eventInTW < listTotalEvent.size(); eventInTW ++) {
			LinkedList<Event> listEvent = listTotalEvent.get(eventInTW);
			
			// declare variables that use for each TW
			int totalReqTW = 0;
			int totalReqLvTW = 0;
			int totalSFCacceptTW = 0;
			double aveSerUtil = 0.0;
			double power1h = 0.0;
			double downtime1h = 0.0;

			// reset global variables
			this.setDelEnergy(0.0);
			this.setIniEnergy(0.0);
			
			for(int eventIn = 0; eventIn < listEvent.size(); eventIn ++) {
				Event event = listEvent.get(eventIn);
				double insPower = 0.0;

				if(event.getType() == "join") {
					System.out.println("Event join " + event.getTime() +" at TW " + eventInTW);
					totalReqTW ++;
				}else {
					// case event leave sfc that is not in the system
					if(!this.listSFCTotal.contains(event.getSfc()))
						continue;
					System.out.println("Event leave " + event.getTime() +" at TW " + eventInTW);
				}
		
				// run scheduling the event
				boolean result = false;
				result = this.runEvent(event, eventInTW);
				
				if(result) {
					aveSerUtil = topo.aveSerUtil();
					insPower = topo.getPowerServer("SFCP") + topo.getNetworkPower() + topo.getPowerEdge(this.listSFCTotal);
					if(event.getType() == "join") {
						totalSFCacceptTW ++;
						this.listSFCTotal.add(event.getSfc());	
					}else {
						totalReqLvTW ++;
						this.listSFCTotal.remove(event.getSfc());
					}
					// calculate downtime
//					downtime1h += (this.getDowntime(event.getSfc()));

					downtime1h += (this.getDowntime(event.getSfc())/this.listSFCTotal.size());
					
					// calculate energy
					this.addTotalEnergy((insPower*(event.getTime() - time4Energy)*HOUR));
					power1h += (insPower*(event.getTime() - time4Energy));
					time4Energy = event.getTime();
				}else {
					if(event.getType() == "leave") {
						throw new java.lang.Error("Error occurs at leaving process.");
					}else {
						System.out.println("Resources are not enough.");
					}
				}
			} // loop each event
			// consolidation
//			if(totalReqTW < 1050) { // condition low load
			Map<SFC, LinkedList<Service>> migSFC = this.consolidation();
			downtime1h += (this.getDowntime(migSFC)/this.listSFCTotal.size());
//			}
			// update variables after 1 hour
			power1h += ((this.getIniEnergy() + this.getDelEnergy())/HOUR);
			listEnergy.add(this.getTotalEnergy());
			listTotalPower.add(power1h/THOUS);
			
			listDownTime.add(downtime1h);
			listError.add(this.error);
			// store log values
			listReqTW.add(totalReqTW);
			listReqLvTW.add(totalReqLvTW);
			// get and store number of active SFC
			listSFCActive.add(this.listSFCTotal.size()); 
			// get serverutilization
			listAveSerUtil.add(aveSerUtil);
			// get number of service
			// store list acceptance
			listAcceptance.add(totalSFCacceptTW*1.0/totalReqTW);
			listAcceptTW.add(totalSFCacceptTW);
		} // TW loop
		
		// print log values to txt or excel file
		try {
//			Path path = Paths.get("./PlotSFCPlanner");
//		    Files.createDirectories(path);
//		    System.out.println("Directory is created!");
			String path = "./PlotSFCPlanner/" + type;

			write_integer(path + "/AveReqTWSFCP.txt",listReqTW);
			write_integer(path + "/AveReqLvTWSFCP.txt",listReqLvTW);
			write_integer(path + "/AveReqActiveSFCP.txt",listSFCActive);
			write_integer(path + "/AveAcceptTWSFCP.txt",listAcceptTW);
			write_integer(path + "/TotalRelocateSFCP.txt",listError);
			write_double(path + "/AveServerUtilSFCP.txt",listAveSerUtil);
			write_double(path + "/AvePowerSFCP.txt",listTotalPower);
			write_double(path + "/TotalEnergySFCP.txt",listEnergy);
			write_double(path + "/AveAcceptanceSFCP.txt",listAcceptance);
			write_double(path + "/AveDownTimeSFCP.txt",listDownTime);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	

	
	public Map<SFC, LinkedList<Service>> consolidation() {
		Map<SFC, LinkedList<Service>> migSFC = new  HashMap<>();
		Map<PhysicalServer, Double> listNsServer = this.cal_ns(); 
//		// ascending order'
//		Map<PhysicalServer, Double> sortedAsc = this.sortByValue(listNsServer, "ascend"); // low utilized --> high utilized
		boolean flag = false;
		while(!flag) {
			int length1 = listNsServer.size();
			Map<PhysicalServer, Double> sortedAsc = this.sortByValue(listNsServer, "ascend"); // low utilized --> high utilized
			Map<PhysicalServer, Double> sortedDes = this.sortByValue(listNsServer, "descend");  // high utilized --> low utilized
			PhysicalServer migSv = sortedAsc.entrySet().iterator().next().getKey();
			sortedDes.remove(migSv); 
			for(Entry<PhysicalServer, Double> entry : sortedDes.entrySet()) {
				double cpuMig = migSv.getUsedCPU();
				PhysicalServer candSv = entry.getKey();
				if(candSv.getRemainCPU() >= cpuMig) {
					// action for migration
					candSv.setUsedCPU(cpuMig);
					migSv.resetCPU();
					for(Service sv : migSv.getListService()) {
						sv.setNode(candSv);
						// change position of service
						this.error ++;
						this.addIniEnergy(INIENERGY);
						this.addDelEnergy(DELENERGY);
						for(SFC sfc : this.listSFCTotal) {
							if(sfc.getListServiceCloud().contains(sv)) {
								// add to list SFC migration
								if(!migSFC.containsKey(sfc)) {
									migSFC.put(sfc, new LinkedList<>());
									migSFC.get(sfc).add(sv);
								}else {
									migSFC.get(sfc).add(sv);
								}
								// delete virtual links
								topo.returnBW(sfc);
								sfc.setBwReturn(true);
								break;
							}
						}
					}
					candSv.getListService().addAll(migSv.getListService());
					migSv.getListService().clear();
					// end action for migration
					break;
				} // cpu comparation
			} // loop descend 
			listNsServer.clear();
			listNsServer = this.cal_ns();
			int length2 = listNsServer.size();
			if (length1 == length2)
				flag = true;
//			if(listNsServer.containsKey(migSv))
//				listNsServer.remove(migSv);
		}
		
		return migSFC;
	}
		
		
		
//		public Map<SFC, LinkedList<Service>> consolidation() {
//			Map<SFC, LinkedList<Service>> migSFC = new  HashMap<>();
//			Map<PhysicalServer, Double> listNsServer = this.cal_ns(); 
////			// ascending order'
////			Map<PhysicalServer, Double> sortedAsc = this.sortByValue(listNsServer, "ascend"); // low utilized --> high utilized
//			while(!listNsServer.isEmpty()) {
//				Map<PhysicalServer, Double> sortedAsc = this.sortByValue(listNsServer, "ascend"); // low utilized --> high utilized
//				Map<PhysicalServer, Double> sortedDes = this.sortByValue(listNsServer, "descend");  // high utilized --> low utilized
//				PhysicalServer migSv = sortedAsc.entrySet().iterator().next().getKey();
//				sortedDes.remove(migSv); 
//				for(Entry<PhysicalServer, Double> entry : sortedDes.entrySet()) {
//					double cpuMig = migSv.getUsedCPU();
//					PhysicalServer candSv = entry.getKey();
//					if(candSv.getRemainCPU() >= cpuMig) {
//						candSv.setUsedCPU(cpuMig);
//						migSv.resetCPU();
//						for(Service sv : migSv.getListService()) {
//							sv.setNode(candSv);
//							// change position of service
//							this.error ++;
//							this.addIniEnergy(INIENERGY);
//							this.addDelEnergy(INIENERGY);
//							for(SFC sfc : this.listSFCTotal) {
//								if(sfc.getListServiceCloud().contains(sv)) {
//									// add to list SFC migration
//									if(!migSFC.containsKey(sfc)) {
//										migSFC.put(sfc, new LinkedList<>());
//										migSFC.get(sfc).add(sv);
//									}else {
//										migSFC.get(sfc).add(sv);
//									}
//									// delete virtual links
//									topo.returnBW(sfc);
//									sfc.setBwReturn(true);
//									break;
//								}
//							}
//						}
//						candSv.getListService().addAll(migSv.getListService());
//						migSv.getListService().clear();
//						break;
//					} // cpu comparation
//				} // loop descend 
//				listNsServer.clear();
//				listNsServer = this.cal_ns();
////				if(listNsServer.containsKey(migSv))
////					listNsServer.remove(migSv);
//			}
			
			
			
			
			
			
			
//		Map<PhysicalServer, Double> sortedDes = this.sortByValue(listNsServer, "descend");  // high utilized --> low utilized
//
//		for(Entry<PhysicalServer, Double> entry : sortedAsc.entrySet()) {
////			System.out.println(entry);
//			PhysicalServer migSv = entry.getKey();
//			// calculate CPU need to be migrated
//			double cpuMig = migSv.getUsedCPU();
//			// sort in N_s descending order
//			Map<PhysicalServer, Double> sortedDes = this.sortByValue(listNsServer, "descend");  // high utilized --> low utilized
//			// pick one by one server in the list and try to migrate
//			for(Entry<PhysicalServer, Double> entry1 : sortedDes.entrySet()) {
//				PhysicalServer candSv = entry1.getKey();
//				if(candSv.getRemainCPU() >= cpuMig) {
//					candSv.setUsedCPU(cpuMig);
//					migSv.resetCPU();
//					for(Service sv : migSv.getListService()) {
//						sv.setNode(candSv);
//						// change position of service
//						this.error ++;
//						this.addIniEnergy(INIENERGY);
//						this.addDelEnergy(INIENERGY);
//						for(SFC sfc : this.listSFCTotal) {
//							if(sfc.getListServiceCloud().contains(sv)) {
//								// add to list SFC migration
//								if(!migSFC.containsKey(sfc)) {
//									migSFC.put(sfc, new LinkedList<>());
//									migSFC.get(sfc).add(sv);
//								}else {
//									migSFC.get(sfc).add(sv);
//								}
//								// delete virtual links
//								topo.returnBW(sfc);
//								sfc.setBwReturn(true);
//								break;
//							}
//						}
//					}
//					candSv.getListService().addAll(migSv.getListService());
//					migSv.getListService().clear();
//					break;
//				}
//			}
//		}
		
	
	public double getDowntime(SFC sfc) {
		// calculate downtime based on number of services of a SFC that must be 
		// initiated in a server
		double downtime = 0.0;
		Map<PhysicalServer, Integer> listServer = new HashMap<>();
		LinkedList<Service> listSer = sfc.getListServiceCloud();
		LinkedList<Double> listDT = new LinkedList<>();
		for(Service ser : listSer) {
			PhysicalServer server = ser.getBelongToServer();
			if(!listServer.containsKey(server)) {
				listServer.put(server, 1);
			}else {
				Integer numScale = listServer.get(server);
				listServer.put(server, numScale + 1);
			}
		}
		// start calculating downtime causing by scaling out
		for(Entry<PhysicalServer, Integer> entry : listServer.entrySet()) {
			int numScale = entry.getValue();
			// equation
			if(numScale != 0)
				downtime += (4.24 + 0.7*numScale);
//			listDT.add((2.913 + 0.346*numScale - 0.001*numScale*numScale));
		}
//		Collections.sort(listDT);
		// return longest downtime caused by services of SFC 
//		double downtime = 0.0;
//		if(listDT.size() != 0)
//			downtime = listDT.getLast();
		return downtime;
	}
	
	public double getDowntime(Map<SFC, LinkedList<Service>> migSFC) {
//		LinkedList<Double> listDT = new LinkedList<>();
		// calculate downtime based on number of services of a SFC that must be 
		// initiated in a server
		double downtime = 0.0;
		Map<PhysicalServer, Integer> listServer = new HashMap<>();
		for(Entry<SFC, LinkedList<Service>> entry : migSFC.entrySet()) {
			LinkedList<Service> listSer = entry.getValue();
			for(Service ser : listSer) {
				PhysicalServer server = ser.getBelongToServer();
				if(!listServer.containsKey(server)) {
					listServer.put(server, 1);
				}else {
					Integer numScale = listServer.get(server);
					listServer.put(server, numScale + 1);
				}
			}
		}
		// start calculating downtime causing by scaling out
		for(Entry<PhysicalServer, Integer> entry : listServer.entrySet()) {
			int numScale = entry.getValue();
			// equation
			if(numScale != 0)
				downtime += (4.24 + 0.7*numScale);//			listDT.add((2.913 + 0.346*numScale - 0.001*numScale*numScale));
		}
		// these servers perform migration in parallel
//		Collections.sort(listDT);
		// return longest downtime caused by services of SFC 
//		if(migSFC.size() == 0)
//			return downtime;
		return downtime;
	}
	
	
	public <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, String order) {
		List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
       
		if(order == "ascend")
			list.sort(Entry.comparingByValue());
		else
			list.sort(Collections.reverseOrder(Entry.comparingByValue()));
			
        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
	
	public Map<PhysicalServer, Double> cal_ns() {
		Map<PhysicalServer, Double> listNsServer = new HashMap<>();
		for(PhysicalServer sv : topo.getListPhyServers()) {
			sv.setPowerServerNom();
			Double power = sv.getPowerServer();
			Double load = sv.getUsedCPU();
			if(load != 0 && sv.getState() == 1) {
			listNsServer.put(sv, load*1.0*100/power);
			}
		}
		return listNsServer;
	}
	
	public double cal_PF(LinkedList<LinkedList<SubstrateLink>> allPaths, Node node, double maxValueNode , double maxValueLink) { // calculate potential factor
		double smallPosNode = 1.0;
		double smallPosLink = 1.0;
		double d_t_p = 0.0;
		double min = Integer.MAX_VALUE;

		for(LinkedList<SubstrateLink> path : allPaths) {
			d_t_p = 0.0;
			if(path.isEmpty()) {
				d_t_p = 0.0;
			}else {
				for(SubstrateLink link : path) {
					d_t_p += (1/(maxValueLink + smallPosLink - link.getUsedBW())); // define later
				}
			}
			if(d_t_p < min) {
				min = d_t_p;
			} // if d_t_p == min ? currently ignore
		}
	
		
		return d_t_p/(maxValueNode + smallPosNode - node.getUsedCPU()); 
	}
	
	public double cal_PF(Node node, double maxValueNode , double maxValueLink) { // calculate potential factor
		double smallPosNode = 1.0;
		double smallNumerator = 1.0;
		
		return smallNumerator/(maxValueNode + smallPosNode - node.getUsedCPU());
		
	}
	
	
	public boolean runEvent(Event event, int TW) {
		
		boolean result = false;
		
		SFC sfc = event.getSfc();
		Rpi pi = topo.getListRPi().get(event.getPiID());
		String eventType = event.getType();
		
		boolean failedVNF = false;
		
		Map<Node, Double> listTempCPU = new HashMap<>();
		Map<SubstrateLink, Double> listTempBW = new HashMap<>();
		
		if(eventType == "join") { // event join -----------------------------------
			// map each VNF
			VNF_LOOP:
			for(int i = 0; i < sfc.getListService().size(); i++) {
				
				Service curSer = sfc.getListService().get(i);
				
				// check Capture resource
				if(curSer.getServiceType() == "capture") { // mapping at the "access node"
					if(pi.getRemainCPU() < curSer.getCpu_pi()) {
						failedVNF = true;
						break VNF_LOOP;
					}
					else {
						pi.setUsedCPU(curSer.getCpu_pi());
						curSer.setNode(pi);
						continue;
					}
				}
				
				Service preSer = sfc.getListService().get(i - 1);
				
				// calculate potential factor
				Map<Node, Double> listNodePF = new HashMap<>();
				Map<Node, LinkedList<SubstrateLink>> listPathOfNodes = new HashMap<>(); // store shortest path for a candidate node
				LinkedList<Node> listAllNode = new LinkedList<>();
				
				if(curSer.getServiceType() == "receive"  || preSer.getNode().getNodeType() == "server"){
					listAllNode.addAll(topo.getListServerNode());
				}else {
					listAllNode.add(pi);
					listAllNode.addAll(topo.getListServerNode());
				}
				for(Node node : listAllNode) {
					Node start = preSer.getNode();
					Node end = node;
					double pfValue  = 0;
					double maxValueLink = topo.getMaxLinkValue();
					double maxValueNode = topo.getMaxNodeValue();
					// how about start node = end node
					if(start.equals(end)) {
						pfValue = this.cal_PF(node, maxValueNode, maxValueLink);
						listNodePF.put(node, pfValue); // store PF value 
						LinkedList<SubstrateLink> empty = new LinkedList<>();
						listPathOfNodes.put(node, empty);

					}else { // start node is not equal end node
						BFS bfs = new BFS(start, end, preSer.getBandwidth());
						bfs.run(topo); // get all paths that are available
						LinkedList<LinkedList<SubstrateLink>> allPaths = bfs.getAvailablePaths();	
						// check if node is the last node
						if(allPaths.isEmpty() && node.equals(topo.getListSubNodes().getLast())) {
							failedVNF = true;
							break VNF_LOOP;
						}
						// check if no path is found
						if(allPaths.isEmpty())
							continue;	
						// all conditions passed, calculate pfvalue
						pfValue = this.cal_PF(allPaths, node, maxValueNode, maxValueLink);
						listNodePF.put(node, pfValue); // store PF value 
						listPathOfNodes.put(node, bfs.getShortestPath());
					}
					// find all path for this node
					
				}
				// sort PF value in ascending order & check node's resources
				Map<Node, Double> sortedNodes = this.sortByValue(listNodePF, "ascend");

//				LinkedList<SubstrateNode> listFinalNodes = new LinkedList<>(); // store nodes that satisfies CPU demand
				Node chosenNode = null;
				for(Node node : sortedNodes.keySet()) {
					if(node.getNodeType() == "pi") {
						if(node.getRemainCPU() >= curSer.getCpu_pi()) { // fix CPU type
							chosenNode = node;
							break;
						}
					}else {
						if(node.getRemainCPU() >= curSer.getCpu_server()) { // fix CPU type
							chosenNode = node;
							break;
						}
					}
				}
				if(chosenNode == null) {
					failedVNF = true;
					break VNF_LOOP;
				}
				// map VNF to chosen node & map link
				double cpuNode = 0.0; // for temporary list 
				if(chosenNode.getNodeType() == "pi") {
					cpuNode = curSer.getCpu_pi();
					curSer.setBelongToEdge(true);
					curSer.setNode(chosenNode);
					pi.setUsedCPU(cpuNode);
				}else {
					cpuNode = curSer.getCpu_server();
					curSer.setBelongToEdge(false);
					curSer.setBelongToServer((PhysicalServer) chosenNode);
					curSer.setNode(chosenNode);
					chosenNode.setUsedCPU(cpuNode);
					PhysicalServer sv = (PhysicalServer) chosenNode;
					sv.getListService().add(curSer);
				}

				LinkedList<SubstrateLink> chosenLink = listPathOfNodes.get(chosenNode);
				if(!chosenLink.isEmpty()) {
					topo.updateLink(chosenLink, preSer.getBandwidth());
					// store links in list virtuallink
					sfc.getvLink().add(new VirtualLink(preSer,curSer, chosenLink, preSer.getBandwidth()));
				}
				
			} // VNF loop
			
			// check condition that all VNFs are mapped successfully
			if(failedVNF) { // one VNF had failed
				topo.returnCPU(sfc);
				topo.returnBW(sfc);
				result = false;
			}else {
				this.addIniEnergy(INIENERGY*sfc.getListServiceCloud().size());
				result = true;
			}
		}else { // leaving event
			for(Service service : sfc.getListService()) {
				if(service.getNode().getNodeType() == "pi")
					listTempCPU.put(service.getNode(), service.getCpu_pi());
				else
					listTempCPU.put(service.getNode(), service.getCpu_server());	
			}
			for(VirtualLink link : sfc.getvLink()) {
				Double bw = link.getBWRequest();
				for(SubstrateLink linkSub : link.getLinkSubstrate()) {
					listTempBW.put(linkSub, bw);
				}
			}
			topo.returnCPU(sfc);
			topo.returnBW(sfc);
			sfc.setBwReturn(true);
			this.addDelEnergy(DELENERGY*sfc.getListServiceCloud().size());
			result = true;
		} // end of event ------------------------------------------------------
		
		return result;
	}
	
	public double cpuEdgeAverage() {
		if(this.getListSFCTotal().size() == 0)
			return 0.0;
		
		double result = 0;
		for(SFC sfc : this.listSFCTotal) {
			result += sfc.cpuEdgeUsage();
		}
		result = result/(1.0*this.listSFCTotal.size());
		
		return result;
	}
	
	public Double bwEdgeAverage() {
		if(this.getListSFCTotal().size() == 0)
			return 0.0;
		
		Double bw = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			bw += sfc.bandwidthUsageOutDC();
		}
		bw = bw/(1.0*this.listSFCTotal.size());
		return bw;
	}
	
	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
	
	public Topology getTopo() {
		return topo;
	}

	public void setTopo(Topology topo) {
		this.topo = topo;
	}

	public HashMap<Integer, LinkedList<Double>> getPrediction() {
		return feedDataChecker;
	}

	public void setPrediction(HashMap<Integer, LinkedList<Double>> prediction) {
		this.feedDataChecker = prediction;
	}

	public static void write_integer (String filename, LinkedList<Integer> x) throws IOException{ //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
 		for (int i = 0; i < x.size(); i++) {
			outputWriter.write(Integer.toString(x.get(i)));
			outputWriter.newLine();
 		}
		outputWriter.flush();  
		outputWriter.close();  
	}
	
	
	public static void write_integer (String filename, int [] x) throws IOException{ //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
			//outputWriter.write(x.get(i));
			// Or:
			outputWriter.write(Integer.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush();  
		outputWriter.close();  
	}

	public static void write_double (String filename, LinkedList<Double> x) throws IOException { //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
 		for (int i = 0; i < x.size(); i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x.get(i)));
			outputWriter.newLine();
 		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public static void write_double (String filename, double [] x) throws IOException { //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public Double PowerEdgeUsage() { //for checking
		Double power = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			power += sfc.powerEdgeUsage();
		}
		return power;
	}
	public Double cpuEdgeAllSFC() {
		Double cpu = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpu += sfc.cpuEdgeUsage();
		}
		return cpu;
	}
	public Double cpuServerAllSFC() {
		Double cpu = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpu += sfc.cpuServerUsage();
		}
		return cpu;
	}
	public Double cpuEdgePerSFC() {
		Double cpuRatio = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpuRatio += sfc.cpuEdgeUsage();
		}
		cpuRatio = cpuRatio/(1.0*this.listSFCTotal.size());
		return cpuRatio*1.0/100;
	}
	public Double cpuServerPerSFC() {
		Double cpuRatio = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpuRatio += sfc.cpuServerUsage();
		}
		cpuRatio = cpuRatio/(1.0*this.listSFCTotal.size());
		return cpuRatio*1.0/100;
	}
	public Double linkUsagePerSFC() {
		Double linkRatio = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			linkRatio += sfc.bandwidthUsageInDC() + sfc.bandwidthUsageOutDC();
		}
		return linkRatio/(1.0*this.listSFCTotal.size());
	}
	
	public LinkedList<SFC> getListSFCTotal() {
		return listSFCTotal;
	}

	public void setListSFCTotal(LinkedList<SFC> listSFCTotal) {
		this.listSFCTotal = listSFCTotal;
	}

	public int getSfcID() {
		return sfcID;
	}

	public void setSfcID(int sfcID) {
		this.sfcID = sfcID;
	}
	
	public double getIniEnergy() {
		return iniEnergy;
	}

	public void addIniEnergy(double iniEnergy) {
		this.iniEnergy += iniEnergy;
	}

	public void setIniEnergy(double iniEnergy) {
		this.iniEnergy = iniEnergy;
	}
	
	public double getDelEnergy() {
		return delEnergy;
	}

	public void setDelEnergy(double delEnergy) {
		this.delEnergy = delEnergy;
	}
	
	public void addDelEnergy(double delEnergy) {
		this.delEnergy += delEnergy;
	}


	public double getTotalEnergy() {
		return totalEnergy;
	}

	public void setTotalEnergy(double totalEnergy) {
		this.totalEnergy = totalEnergy;
	}
	
	public void addTotalEnergy(double totalEnergy) {
		this.totalEnergy += totalEnergy;
	}


}
