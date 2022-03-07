package fil.resource.virtual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import fil.resource.substrate.LinkPhyEdge;
import fil.resource.substrate.Node;
import fil.resource.substrate.PhysicalServer;
import fil.resource.substrate.Rpi;
import fil.resource.substrate.SubstrateLink;
import fil.resource.substrate.SubstrateSwitch;


/**
 * This class helps user to build new substrate network
 * 
 * @author Van Huynh Nguyen
 *
 */
public class Topology  {
	private Map<Node, LinkedList<Node>> map;
	private LinkedList<SubstrateLink> listPhyLinks; // bandwidth of all links
	private LinkedList<PhysicalServer> listPhyServers;
	private LinkedList<Node> listSubNodes;
	private LinkedList<Node> listComputeNodes;

//	private LinkedList<LinkPhyEdge> listLinkPhyEdge; // bandwidth of all Phy->Edge switch link
	private LinkedList<SubstrateSwitch> listSwitch;
	private LinkedList<SubstrateSwitch> listSwitchUsed;
	private LinkedList<Rpi> listRpi;
	
	
	//for link mapping
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge; // list agg switch connect to edge switch
	
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAgg; // list core switch connect to agg switch
	
	private Map<Integer, LinkedList<SubstrateSwitch>> listEdgeSwitchInPod; // list edge switch in pod
	private Map<Integer, LinkedList<SubstrateSwitch>> listAggSwitchInPod; // list agg switch in pod
	private LinkedList<SubstrateSwitch> listPhySwitch; // gia physical server la mot switch
	private Map<Integer, SubstrateSwitch> listAggSwitch;
	private Map<Integer, SubstrateSwitch> listCoreSwitch;
	private Map<Integer, SubstrateSwitch> listGateway;
	private Map<Integer, SubstrateSwitch> listEdgeSwitch;
	
	/**
	 * Constructs new topology
	 */
	public Topology() {
		map = new HashMap<>();
		listPhyServers = new LinkedList<>();
		listSubNodes = new LinkedList<>();
		listComputeNodes = new LinkedList<>();
		listPhyLinks = new LinkedList<>();
//		listLinkPhyEdge = new LinkedList<>();
		listSwitch = new LinkedList<>();
		listAggConnectEdge = new HashMap<>();
		listCoreConnectAgg = new HashMap<>();
		listEdgeSwitchInPod = new HashMap<>();
		listAggSwitchInPod = new HashMap<>();
		listPhySwitch = new LinkedList<>();
		listAggSwitch = new HashMap<>();
		listCoreSwitch = new HashMap<>();
		listGateway = new HashMap<>();
		listEdgeSwitch = new HashMap<>();
		listSwitchUsed = new LinkedList<>();
		listRpi = new LinkedList<>();
	}

	public void addEdge(Node node1, Node node2, double bandwidth) {
		listPhyLinks.add(new SubstrateLink(node1, node2, bandwidth));
		LinkedList<Node> neighbor = map.get(node1);
		if (neighbor == null) {
			neighbor = new LinkedList<>();
			map.put(node1, neighbor);
		}
		neighbor.add(node2);
		if(node1.getNodeType() == "switch" && !listSwitch.contains(node1))
		{
			listSwitch.add((SubstrateSwitch) node1);	
		}
		if(node2.getNodeType() == "switch" && !listSwitch.contains(node2))
		{
			listSwitch.add((SubstrateSwitch) node2);
		}
	}

	public LinkedList<Node> adjacentNodes(Node node) {
		LinkedList<Node> adjacent = map.get(node);
		if (adjacent == null) {
			return new LinkedList<Node>();
		}
		return new LinkedList<Node>(adjacent);
	}


	public void addListEdge(int index, SubstrateSwitch edge) {
		
		this.listEdgeSwitch.put(index, edge);
	}
	
	public void addListAgg(int index, SubstrateSwitch agg) {
		this.listAggSwitch.put(index, agg);
	}
	
	public void addListCore(int index, SubstrateSwitch core) {
		this.listCoreSwitch.put(index, core);
	}
	
	public void addListGW(int index, SubstrateSwitch gw) {
		this.listGateway.put(index, gw);
	}
	
	public LinkedList<SubstrateSwitch> getMinimunSpanningTree() {
		
		LinkedList<SubstrateSwitch> listSwitchON = new LinkedList<SubstrateSwitch>();
		
		Map<Integer, SubstrateSwitch> listEdgeSwitchON = this.getListEdgeSwitch();
		Map<Integer, SubstrateSwitch> listAggSwitch = new HashMap<>();
		listAggSwitch = this.getListAggSwitch();
		Map<Integer, SubstrateSwitch> listCoreSwitch = this.getListCoreSwitch();
				
		
		if(!listEdgeSwitchON.isEmpty()) {
			Collection<SubstrateSwitch> listEdgeSwitch = listEdgeSwitchON.values();
			listSwitchON.addAll(listEdgeSwitch);
		} else {
			System.out.println("List edge switch get from topo is empty!");
		}
		
		Map<Integer, SubstrateSwitch> sortListAgg = listAggSwitch
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(
				Collectors.toMap(e -> e.getKey(), e -> e.getValue(),
				(e1, e2) -> e2, LinkedHashMap::new));
		
		int countAgg = 0;
		if(!listAggSwitch.isEmpty()) {
			for(Entry<Integer, SubstrateSwitch> substrateAgg : sortListAgg.entrySet()) {
				if(countAgg == 0) {
					countAgg = substrateAgg.getKey();
				}
				if(listAggSwitch.get(countAgg) == null) {
					break;
				}
				listSwitchON.add(listAggSwitch.get(countAgg));
				countAgg += 10;
			}
		} else {
			System.out.println("List aggregation switch get from topo is empty!");
		}
		
		Map<Integer, SubstrateSwitch> sortListCore = listCoreSwitch
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(
				Collectors.toMap(e -> e.getKey(), e -> e.getValue(),
				(e1, e2) -> e2, LinkedHashMap::new));
		
		if(!listCoreSwitch.isEmpty()) {
			for(Entry<Integer, SubstrateSwitch> substrateCore : sortListCore.entrySet()) {
				listSwitchON.add(substrateCore.getValue());
				break;
			}
		}
		return listSwitchON;
	}

	public void reset(FatTree fattree) {
		
		for(PhysicalServer sv : this.listPhyServers) {
			sv.reset();
		}
		// reset links
		for(SubstrateLink link : this.listPhyLinks) {
			link.reset();
		}

		
		for (SubstrateSwitch sw : this.listSwitch) {
			Map<SubstrateSwitch, Double> bwport = sw.getBandwidthPort();
			LinkedList<Node> neighbor_temp = this.adjacentNodes(sw);
			LinkedList<SubstrateSwitch> neighbor = new LinkedList<>();
			for(Node node : neighbor_temp) {
				if(node.getNodeType() == "switch")
					neighbor.add((SubstrateSwitch) node);
			}
			for (SubstrateSwitch s : neighbor) {
				bwport.put(s, (double) 0);
			}
		}
		for (SubstrateSwitch swPhy : this.listPhySwitch) {
			Integer idPhy = Integer.parseInt(swPhy.getNameSubstrateSwitch());
			Integer idEdge = fattree.getListPhyConnectEdge().get(idPhy);
			SubstrateSwitch swEdge = fattree.getListEdgeSwitch().get(idEdge);
			swEdge.getBandwidthPort().put(swPhy, (double) 0);
		}
	}
	
	public  boolean updateLink(LinkedList<SubstrateLink> listLink, double bw) {
		boolean result = false;
		
		for(SubstrateLink link : listLink) {
			Node a = link.getStart();
			Node b = link.getEnd();
			SubstrateLink forward = null;
			SubstrateLink backward = null;
			for(SubstrateLink find : this.getlistPhyLinks()) {
				if (find.getStart().equals(a) && find.getEnd().equals(b)) {
					if(!find.equals(link))
						throw new java.lang.Error();
					forward = find;
				}else if(find.getStart().equals(b) && find.getEnd().equals(a)) {
					backward = find;
				}else {
					continue;
				}
			}
			// condition specially for pi & gateway
			if(a.getNodeType() == "pi" || a.getNodeType() == "gw") {
				if(forward.getRemainBW() >= bw) {
					forward.setUsedBW(bw);
					result = true;
				}else {
					result = false;
				}
			}else { // normal switches
				if(forward != null && backward != null) {
					if(forward.getRemainBW() != backward.getRemainBW())
						throw new java.lang.Error();
					if(forward.getRemainBW() >= bw) {
						forward.setUsedBW(bw);
						backward.setUsedBW(bw);
						if(a.getNodeType() == "server") {
							SubstrateSwitch nodeB = (SubstrateSwitch) b;
							nodeB.setPort(nodeB, bw);
						}else if(b.getNodeType() == "server") {
							SubstrateSwitch nodeA = (SubstrateSwitch) a;
							nodeA.setPort(nodeA, bw);
						}else {
							SubstrateSwitch nodeA = (SubstrateSwitch) a;
							SubstrateSwitch nodeB = (SubstrateSwitch) b;
							nodeA.setPort(nodeB, bw);
							nodeB.setPort(nodeA, bw);
						}
						result = true;
					}else {
						result = false;
					}
				}else {
					throw new java.lang.Error();
				}
			}
			
		}
		return result;
	}
	
	public LinkedList<PhysicalServer> benchmarkServer(LinkedList<PhysicalServer> listSer){
		LinkedList<PhysicalServer> result = new LinkedList<>();
		Map<PhysicalServer, Double> mapSort = new HashMap<>();
		for(PhysicalServer ser : listSer) {
			double totalCost = 0.0;
			Node edgeSwitch = null;
			Node aggSwitch = null;

			for(SubstrateLink link : this.listPhyLinks) {
				if(link.getEnd().equals(ser)) {
					totalCost += link.getRemainBW();
					edgeSwitch = link.getStart();
					break;
				}
			}
			for(SubstrateLink link : this.listPhyLinks) {
				if(link.getEnd().equals(edgeSwitch)) {
					totalCost += link.getRemainBW()*1/5;
					aggSwitch = link.getStart();
					for(SubstrateLink link1 : this.listPhyLinks) {
						if(link1.getEnd().equals(aggSwitch)) {
							totalCost += link.getRemainBW()*1/25;
						}
					}
				}
			} // loop link bandwidth
			mapSort.put(ser, totalCost);
			mapSort = mapSort.entrySet().stream() // sort low to high
					 .sorted(Map.Entry.comparingByValue())
					 .collect(Collectors.toMap(
					 Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
			
			for(Entry<PhysicalServer, Double> entry : mapSort.entrySet()) {
				result.add(entry.getKey());
			}
		} // loop server
		return result;
	}
	
	
	public LinkedList<PhysicalServer> getListPhyServers() {
		LinkedList<PhysicalServer> listServer = new LinkedList<>();
		for(Node node : this.listComputeNodes) {
			if(node.getNodeType() == "server")
				listServer.add((PhysicalServer) node);
		}
		return listServer;
	}
	
	public LinkedList<Node> getListServerNode() {
		LinkedList<Node> listServer = new LinkedList<>();
		for(Node node : this.listComputeNodes) {
			if(node.getNodeType() == "server")
				listServer.add(node);
		}
		return listServer;
	}
	
	public void setListPhyServers(LinkedList<PhysicalServer> listPhyServers) {
		this.listPhyServers = listPhyServers;
	}
	public LinkedList<SubstrateLink> getlistPhyLinks()
	
	{
		return listPhyLinks;
	}
	public void setlistPhyLinks(LinkedList<SubstrateLink> listPhyLinks)
	{
		this.listPhyLinks = listPhyLinks;
	}

	public LinkedList<SubstrateSwitch> getListSwitch() {
		return listSwitch;
	}

	public Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> getListAggConnectEdge() {
		return listAggConnectEdge;
	}

	public void setListAggConnectEdge(Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge) {
		this.listAggConnectEdge = listAggConnectEdge;
	}

	public Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> getListCoreConnectAgg() {
		return listCoreConnectAgg;
	}

	public void setListCoreConnectAgg(Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAgg) {
		this.listCoreConnectAgg = listCoreConnectAgg;
	}

	public Map<Integer, LinkedList<SubstrateSwitch>> getListEdgeSwitchInPod() {
		return listEdgeSwitchInPod;
	}

	public void setListEdgeSwitchInPod(Map<Integer, LinkedList<SubstrateSwitch>> listEdgeSwitchInPod) {
		this.listEdgeSwitchInPod = listEdgeSwitchInPod;
	}

	public Map<Integer, LinkedList<SubstrateSwitch>> getListAggSwitchInPod() {
		return listAggSwitchInPod;
	}

	public void setListAggSwitchInPod(Map<Integer, LinkedList<SubstrateSwitch>> listAggSwitchInPod) {
		this.listAggSwitchInPod = listAggSwitchInPod;
	}

	public void setListSwitch(LinkedList<SubstrateSwitch> listSwitch) {
		this.listSwitch = listSwitch;
	}

	public LinkedList<SubstrateSwitch> getListPhySwitch() {
		return listPhySwitch;
	}
	
	public double getlistPhyLinksTopo()
	{
		
		double totalBW =0;
		for(SubstrateLink link : listPhyLinks)
		{
			totalBW+= link.getUsedBW();
		}
		return totalBW;
	}
	
	public Map<Integer, SubstrateSwitch> getListAggSwitch() {
		return listAggSwitch;
	}
	public void setListAggSwitch(Map<Integer, SubstrateSwitch> listAggSwitch) {
		this.listAggSwitch = listAggSwitch;
	}
	public Map<Integer, SubstrateSwitch> getListCoreSwitch() {
		return listCoreSwitch;
	}
	public void setListCoreSwitch(Map<Integer, SubstrateSwitch> listCoreSwitch) {
		this.listCoreSwitch = listCoreSwitch;
	}
	public Map<Integer, SubstrateSwitch> getListEdgeSwitch() {
		return listEdgeSwitch;
	}
	public void setListEdgeSwitch(Map<Integer, SubstrateSwitch> listEdgeSwitch) {
		this.listEdgeSwitch = listEdgeSwitch;
	}
	public LinkedList<SubstrateSwitch> getListSwitchUsed() {
		return listSwitchUsed;
	}
	public void setListSwitchUsed(LinkedList<SubstrateSwitch> listSwitchUsed) {
		this.listSwitchUsed = listSwitchUsed;
	}

	public double getNetworkPower()
	{
		double power = 0;
		modelHP HP = new modelHP();
		LinkedList<SubstrateSwitch> listSwitch = this.getListSwitch();
		for(SubstrateSwitch entry: listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}
	
	public void returnCPU(SFC sfc) {
		for(Service sv : sfc.getListService()) {
			Node node = sv.getNode();
			if(node == null)
				continue;
			double cpu = 0.0;
			if(node.getNodeType() == "pi") {
				cpu = sv.getCpu_pi();
			}else {
				cpu = sv.getCpu_server();
			}
			node.setUsedCPU(-cpu);
//			if(node.getRemainCPU() > 100.1)
//				throw new java.lang.Error(); currently ignore this prob
		}	
	}
	
	public void returnBW(SFC sfc) {
		if(!sfc.isBwReturn()) {
			for(VirtualLink link : sfc.getvLink()) {
				LinkedList<SubstrateLink> subLink = link.getLinkSubstrate();
				this.updateLink(subLink, -(link.getBWRequest()));
			}
		}
	}

	public double getPowerServer(String algo) {
		double power = 0;
		LinkedList<PhysicalServer> listServer = this.getListPhyServers(); 
		for(PhysicalServer phy : listServer) {
			if(phy.getState() == 1) {
				if(algo.equals("SFCP"))
					phy.setPowerServerNom();
				else
					phy.setPowerServer();
				power += phy.getPowerServer();
			}
			else
				continue;
		}
		return power;
	}
	
	
	public double getPowerEdge(LinkedList<SFC> listSFCTotal) {
		double power = 0.0;
		double numPi = 300;
		for(SFC sfc : listSFCTotal) {
			power += sfc.powerEdgeUsage();
		}
		return power + numPi*1.28;
	}
	
	public double aveSerUtil() {
		LinkedList<PhysicalServer> listServer = this.getListPhyServers();
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
	
	public double getCPUServerUtilization() {
		double average = 0.0;
		for(PhysicalServer phy : this.listPhyServers) {
			//phy.resetCPU();
			average += phy.getUsedCPU();
		}
		return (average*1.0)/(listPhyServers.size()*1.0);
	}
	
	public int getServerUsed() {
		int serverUsed = 0;
		for(PhysicalServer phy : this.listPhyServers) {
			//phy.resetCPU();
			if(phy.getUsedCPU() != 0)
				serverUsed ++;
		}
		return serverUsed;
	}
	
	public double getMaxLinkValue() {
		double a = Integer.MIN_VALUE;
		for(SubstrateLink link :  this.listPhyLinks) {
			if(link.getUsedBW() > a)
				a = link.getUsedBW();
		}
		return a;
	}
	
	public double getMaxNodeValue() {
		double a = Integer.MIN_VALUE;
		for(Node node :  this.listSubNodes) {
			if(node.getUsedCPU() > a)
				a = node.getUsedCPU();
		}
		return a;
	}
	
	public double getLinkUtilization() {
		double totalLinkUsage = 0;
		int linkIndex = 0;
		for(SubstrateLink link :  this.listPhyLinks) {
			if(link.getUsedBW() < 0)
				throw new java.lang.Error("error link");
			totalLinkUsage += link.getUsedBW();
			linkIndex ++;
		}
		return totalLinkUsage/(1.0*linkIndex*1024);
	}
	
	public double getLinkUtil(String start, String stop) {
		double usage = 0.0;
		double numLink = 0.0;
		for(SubstrateLink link : this.listPhyLinks) {
			if(link.getStart().getNodeType() == start && link.getEnd().getNodeType() == stop) {
				if(link.getUsedBW() > 0.1) {
					usage += link.getUsedBW();
					numLink ++;
				}
			}
		}
		if(numLink == 0) return 0.0;
		return usage/numLink;
	}
	
	public double getNonZeroLink(String start, String stop) {
		double numLink = 0;
		for(SubstrateLink link : this.listPhyLinks) {
			if(link.getStart().getNodeType() == start && link.getEnd().getNodeType() == stop) {
				if(link.getUsedBW() > 0.1) {
					numLink ++;
				}
			}
		}
		return numLink;
	}
	
	public SubstrateLink getLinkOfNodes(Node a, Node b) {
		
		SubstrateLink link = new SubstrateLink();
		for(SubstrateLink l : this.listPhyLinks) {
			if(l.getStart().equals(a) && l.getEnd().equals(b)) {
				link = l;
				break;
			}else if(l.equals(this.listPhyLinks.getLast()))
				throw new java.lang.Error();
		}
		return link;
	}
	
	public ArrayList<SubstrateLink> getLinkType(String type) {
		ArrayList<SubstrateLink> listLink = new ArrayList<>();
		switch (type) {
		case "core-agg": for(SubstrateLink link : this.listPhyLinks) {
			if(link.getStart().getNodeType() == "core" && link.getEnd().getNodeType() == "agg")
				listLink.add(link);
		} break;
		case "agg-edge": for(SubstrateLink link : this.listPhyLinks) {
			if(link.getStart().getNodeType() == "agg" && link.getEnd().getNodeType() == "edge")
				listLink.add(link);
		} break;
		default: return null;
		
		}
		return listLink;
	}
	
	public LinkedList<Rpi> getListRPi() {
		return listRpi;
	}
	public void setListRPi(LinkedList<Rpi> listRPi) {
		this.listRpi = listRPi;
	}


	public LinkedList<Node> getListSubNodes() {
		return listSubNodes;
	}


	public void setListSubNodes(LinkedList<Node> listSubNodes) {
		this.listSubNodes = listSubNodes;
	}


	public Map<Integer, SubstrateSwitch> getListGateway() {
		return listGateway;
	}


	public void setListGateway(Map<Integer, SubstrateSwitch> listGateway) {
		this.listGateway = listGateway;
	}


	public LinkedList<Node> getListComputeNodes() {
		return listComputeNodes;
	}
	

	public void setListComputeNodes(LinkedList<Node> listComputeNodes) {
		this.listComputeNodes = listComputeNodes;
	}


}