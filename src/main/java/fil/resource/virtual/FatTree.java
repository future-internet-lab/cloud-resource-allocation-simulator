package fil.resource.virtual;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fil.resource.substrate.Node;
import fil.resource.substrate.PhysicalServer;
import fil.resource.substrate.Rpi;
import fil.resource.substrate.SubstrateSwitch;

/**
 * @author Van Huynh Nguyen
 *
 */
public class FatTree {
	private int k;
	final private double BANDWIDTH = 1*1024; // default bandwidth = 1000;
	final private double BANDWIDTH_GW = 7*1024; // default bandwidth = 1000;
	final private double BANDWIDTH_EDGE = 100;
	
	private int totalGW = 0;
	private int totalCore = 0;
	private int totalAgg = 0;
	private int totalEdge = 0;
	private int totalHost = 0;
	private int totalEdgeHost = 0;

	private Map<Integer, Rpi> listRpi;
	private LinkedList<Node> listNode;
	private LinkedList<Node> listComputeNode;
	private Map<Integer, SubstrateSwitch> listGateway;
	private Map<Integer, SubstrateSwitch> listEdgeSwitch;
	private Map<Integer, Integer> listPhyConnectEdge;
	private Map<Integer, SubstrateSwitch> listAggSwitch;
	private Map<Integer, SubstrateSwitch> listCoreSwitch;
	private Map<Integer, PhysicalServer> listPhysicalServer;
	private Map<Integer, LinkedList<SubstrateSwitch>> listPod; // list edge
																// switch in pod
	private Map<Integer, LinkedList<SubstrateSwitch>> listPodAgg; // list agg
																	// switch in
																	// pod
	// for link mapping
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge; // list
																					// agg
																					// switch
																					// connect
																					// to
																					// edge
																					// switch

	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAgg; // list
																					// core
																					// switch
																					// connect
																					// to
																					// agg
																					// switch
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectGW; // list
	
	public FatTree() {
		this.k = 0;
		listRpi = new HashMap<>();
		listNode = new LinkedList<>();
		listComputeNode = new LinkedList<>();
		listGateway = new HashMap<>();
		listEdgeSwitch = new HashMap<>();
		listAggSwitch = new HashMap<>();
		listCoreSwitch = new HashMap<>();
		listPhysicalServer = new HashMap<>();
		listPod = new HashMap<>();
		listAggConnectEdge = new HashMap<>();
		listCoreConnectAgg = new HashMap<>();
		listPodAgg = new HashMap<>();
		listPhyConnectEdge = new HashMap<>();
	}

	public FatTree(Map<Integer, SubstrateSwitch> listEdgeSwitch, Map<Integer, SubstrateSwitch> listAggSwitch,
			Map<Integer, SubstrateSwitch> listCoreSwitch, Map<Integer, PhysicalServer> listPhysicalServer,
			Map<Integer, LinkedList<SubstrateSwitch>> listPod, Map<Integer, LinkedList<SubstrateSwitch>> listPodAgg,
			int k, int totalCore, int totalAgg, int totalEdge, int totalHost,
			Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge,
			Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAgg) {
		this.k = k;
		this.listAggConnectEdge = listAggConnectEdge;
		this.listAggSwitch = listAggSwitch;
		this.listCoreConnectAgg = listCoreConnectAgg;
		this.listPhysicalServer = listPhysicalServer;
		this.listPod = listPod;
		this.listPodAgg = listPodAgg;
		this.listAggConnectEdge = listAggConnectEdge;
		this.listCoreConnectAgg = listCoreConnectAgg;
		this.totalAgg = totalAgg;
		this.totalCore = totalCore;
		this.totalEdge = totalEdge;
		this.totalHost = totalHost;
		this.listEdgeSwitch = listEdgeSwitch;
		this.listCoreSwitch = listCoreSwitch;
	}

	public Topology genFatTree(int k, int edge, List<Integer> edgePosition) {
		int nodeID = 0;
		Topology topo = new Topology();
		this.k = k;
		totalGW = 1;
		totalCore = k * k / 4;
		totalAgg = k * k / 2;
		totalEdge = k * k / 2;
		totalHost = k * k * k / 4;
		totalEdgeHost = edge;
		// add list Physical Server
		for (int i = 1; i <= totalHost; i++) {
			PhysicalServer server = new PhysicalServer(String.valueOf(i));
			server.setNodeID(nodeID);
			nodeID ++;
			listPhysicalServer.put(i, server);
			listNode.add(server);
			listComputeNode.add(server);
			topo.getListPhyServers().add(server);
		}
		// add Edge host
		
		for(int i = 1; i <= totalEdgeHost; i ++) {
			Random rand = new Random ();
			int position = rand.nextInt(4);
			Rpi rpi = new Rpi(i, edgePosition.get(position));
			rpi.setNodeID(nodeID);
			nodeID ++;
			listRpi.put(i, rpi);
			listNode.add(rpi);
			listComputeNode.add(rpi);
			topo.getListRPi().add(rpi);

		}
		
		for (int i = 0; i < k; i++) {
			int aggSwitchIndexBegin = totalHost + totalCore + 1 + i * k;
			// add list agg switch
			for (int j = aggSwitchIndexBegin; j < aggSwitchIndexBegin + k / 2; j++) {
				SubstrateSwitch aggSwitch = new SubstrateSwitch(String.valueOf(j), 100, false);
				aggSwitch.setNodeType("agg");
				aggSwitch.setType(2);
				aggSwitch.setNodeID(nodeID);
				nodeID ++;
				listAggSwitch.put(j, aggSwitch);
				listNode.add(aggSwitch);
				topo.addListAgg(j, aggSwitch);
				topo.getListSwitch().add(aggSwitch);
			}
			// add list edge switch
			for (int j = aggSwitchIndexBegin + k / 2; j < aggSwitchIndexBegin + k; j++) {
				SubstrateSwitch edgeSwitch = new SubstrateSwitch(String.valueOf(j), 100, true);
				edgeSwitch.setNodeType("edge");
				edgeSwitch.setType(1);
				edgeSwitch.setNodeID(nodeID);
				nodeID ++;
				listEdgeSwitch.put(j, edgeSwitch);
				listNode.add(edgeSwitch);
				topo.addListEdge(j, edgeSwitch);
				topo.getListSwitch().add(edgeSwitch);
			}
		}

		// add list core switch
		for (int i = 1; i <= totalCore; i++) {
			SubstrateSwitch coreSwitch = new SubstrateSwitch(String.valueOf(i + totalHost), 100, false);
			coreSwitch.setNodeType("core");
			coreSwitch.setType(3);
			coreSwitch.setNodeID(nodeID);
			nodeID ++;
			listCoreSwitch.put(i + totalHost, coreSwitch);
			listNode.add(coreSwitch);
			topo.addListCore(i + totalHost, coreSwitch);
			topo.getListSwitch().add(coreSwitch);
		}
		// add gateway
		for(int i = 1; i <= totalGW; i ++) {
			SubstrateSwitch gateway = new SubstrateSwitch(String.valueOf(i + totalHost + totalCore), 100, false);
			gateway.setNodeType("gw");
			gateway.setType(4);
			gateway.setNodeID(nodeID);
			nodeID ++;
			listGateway.put(i + totalHost + totalCore, gateway);
			listNode.add(gateway);
			topo.addListGW(i + totalHost + totalCore, gateway);
			topo.getListSwitch().add(gateway);

		}
		/**************** Add links ****************/
		//add links: pi -> gateway
		for(int i = 1 + totalHost + totalCore; i <= totalGW + totalHost + totalCore; i++) {
			for(int j = 1; j <= totalEdgeHost; j ++) {
				// only allow forwarding link from pi --> gateway, no traverse backwards
//				topo.addEdge(listGateway.get(i), listRpi.get(j), BANDWIDTH_EDGE); 
				topo.addEdge(listRpi.get(j), listGateway.get(i), BANDWIDTH_EDGE);
			}
		}
		// add links: core -> gateway
		for(int i = 1 + totalHost + totalCore; i <= totalGW + totalHost + totalCore; i++) {
			int coreSwitchIndex = totalHost + 1;
			for(int j = coreSwitchIndex; j < coreSwitchIndex + totalCore; j++) {
				// only allow forwarding link from gateway --> core, no traverse backwards
				topo.addEdge(listGateway.get(i), listCoreSwitch.get(j), BANDWIDTH_GW);
//				topo.addEdge(listCoreSwitch.get(j), listGateway.get(i), BANDWIDTH_GW);
				
				// set list core switch connects to gateway -- later 
				
			}
		}
		// add links: physical -> edgeswitch -> aggswitch -> core > gateway
		// Iterator pod=1,2...k
		for (int i = 0; i < k; i++) {
			int aggSwitchIndexBegin = totalHost + totalCore + 1 + i * k;
			// add links from coreSwitch to aggSwitch
			int coreSwitchIndex = totalHost + 1;
			
			for (int j = aggSwitchIndexBegin; j < aggSwitchIndexBegin + k / 2; j++) {
				for (int l = coreSwitchIndex; l < coreSwitchIndex + k / 2; l++) {
					// aggSwitch j connect to coreSwitch j in each group

					topo.addEdge(listAggSwitch.get(j), listCoreSwitch.get(l), BANDWIDTH);
					topo.addEdge(listCoreSwitch.get(l), listAggSwitch.get(j), BANDWIDTH);

					// get list core switch connect to agg switch
					if (listCoreConnectAgg.containsKey(listAggSwitch.get(j))) {
						LinkedList<SubstrateSwitch> temp = new LinkedList<>();
						temp = listCoreConnectAgg.get(listAggSwitch.get(j));
						temp.add(listCoreSwitch.get(l));
						listCoreConnectAgg.put(listAggSwitch.get(j), temp);
					} else {

						LinkedList<SubstrateSwitch> temp = new LinkedList<>();
						temp.add(listCoreSwitch.get(l));
						listCoreConnectAgg.put(listAggSwitch.get(j), temp);
					}
					// stupid :))
					if (l == coreSwitchIndex + k / 2 - 1) {
						coreSwitchIndex = coreSwitchIndex + k / 2;
						break;
					}
				}
			}

			// add links from aggSwitch to edgeSwitch
			for (int j = aggSwitchIndexBegin; j < aggSwitchIndexBegin + k / 2; j++)
				for (int l = aggSwitchIndexBegin + k / 2; l < aggSwitchIndexBegin + k; l++) {
					// just one direction
					topo.addEdge(listAggSwitch.get(j), listEdgeSwitch.get(l), BANDWIDTH);
					topo.addEdge(listEdgeSwitch.get(l), listAggSwitch.get(j), BANDWIDTH);
					// get list agg switch connect to edge switch
					if (listAggConnectEdge.containsKey(listEdgeSwitch.get(l))) {
						LinkedList<SubstrateSwitch> temp = new LinkedList<>();
						temp = listAggConnectEdge.get(listEdgeSwitch.get(l));
						temp.add(listAggSwitch.get(j));
						listAggConnectEdge.put(listEdgeSwitch.get(l), temp);
					} else {

						LinkedList<SubstrateSwitch> temp = new LinkedList<>();
						temp.add(listAggSwitch.get(j));
						listAggConnectEdge.put(listEdgeSwitch.get(l), temp);
					}
				}

			// add links from edgeSwitch to Physical server
			int hostIndexBegin = k * k * i / 4 + 1;
			for (int j = aggSwitchIndexBegin + k / 2; j < aggSwitchIndexBegin + k; j++) {
				for (int l = 0; l < k / 2; l++) {
					// just one direction
					topo.addEdge(listPhysicalServer.get(hostIndexBegin), listEdgeSwitch.get(j), BANDWIDTH);
					topo.addEdge(listEdgeSwitch.get(j), listPhysicalServer.get(hostIndexBegin), BANDWIDTH);
					listPhyConnectEdge.put(hostIndexBegin, j);
					hostIndexBegin++;
				}

			}
			LinkedList<SubstrateSwitch> listEdgePod = new LinkedList<>();
			LinkedList<SubstrateSwitch> listAggPod = new LinkedList<>();
			for (int l = aggSwitchIndexBegin + k / 2; l < aggSwitchIndexBegin + k; l++) {
				listEdgePod.add(listEdgeSwitch.get(l));
			}
			for (int l = aggSwitchIndexBegin; l < aggSwitchIndexBegin + k / 2; l++) {
				listAggPod.add(listAggSwitch.get(l));
			}
			listPod.put(i, listEdgePod);
			listPodAgg.put(i, listAggPod);

		}
		for (SubstrateSwitch sw : topo.getListSwitch()) {
			Map<SubstrateSwitch, Double> bwport = sw.getBandwidthPort();
			LinkedList<Node> neighbor_temp = topo.adjacentNodes(sw);
			LinkedList<SubstrateSwitch> neighbor = new LinkedList<>();
			for(Node node : neighbor_temp) {
				if(node.getNodeType() == "switch")
					neighbor.add((SubstrateSwitch) node);
			}
			for (SubstrateSwitch s : neighbor) {
				bwport.put(s, (double) 0);
			}
		}
		LinkedList<SubstrateSwitch> listPhySw = topo.getListPhySwitch();
		for (SubstrateSwitch swPhy : listPhySw) {
			Integer idPhy = Integer.parseInt(swPhy.getNameSubstrateSwitch());
			Integer idEdge = listPhyConnectEdge.get(idPhy);
			SubstrateSwitch swEdge = listEdgeSwitch.get(idEdge);
			swEdge.getBandwidthPort().put(swPhy, (double) 0);
		}
		
		topo.setListComputeNodes(listComputeNode);
		topo.setListSubNodes(listNode);
		topo.setListAggConnectEdge(listAggConnectEdge);
		topo.setListCoreConnectAgg(listCoreConnectAgg);
		topo.setListEdgeSwitchInPod(listPod);
		topo.setListAggSwitchInPod(listPodAgg);
		
		return topo;
	}


	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public Map<Integer, LinkedList<SubstrateSwitch>> getListPod() {
		return listPod;
	}

	public Object clone() {

		FatTree fat = new FatTree(listEdgeSwitch, listAggSwitch, listCoreSwitch, listPhysicalServer, listPod,
				listPodAgg, k, totalCore, totalAgg, totalEdge, totalHost, listAggConnectEdge, listCoreConnectAgg);
		return fat;
	}

	public Map<Integer, SubstrateSwitch> getListEdgeSwitch() {
		return listEdgeSwitch;
	}

	public void setListEdgeSwitch(Map<Integer, SubstrateSwitch> listEdgeSwitch) {
		this.listEdgeSwitch = listEdgeSwitch;
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

	public int getTotalEdgeHost() {
		return totalEdgeHost;
	}

	public void setTotalEdgeHost(int totalEdgeHost) {
		this.totalEdgeHost = totalEdgeHost;
	}

	public Map<Integer, Integer> getListPhyConnectEdge() {
		return listPhyConnectEdge;
	}

	public void setListPhyConnectEdge(Map<Integer, Integer> listPhyConnectEdge) {
		this.listPhyConnectEdge = listPhyConnectEdge;
	}


	public double getBANDWIDTH_GW() {
		return BANDWIDTH_GW;
	}

	public Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> getListCoreConnectGW() {
		return listCoreConnectGW;
	}

	public void setListCoreConnectGW(Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectGW) {
		this.listCoreConnectGW = listCoreConnectGW;
	}

	public LinkedList<Node> getListNode() {
		return listNode;
	}

	public void setListNode(LinkedList<Node> listNode) {
		this.listNode = listNode;
	}

	public LinkedList<Node> getListComputeNode() {
		return listComputeNode;
	}

	public void setListComputeNode(LinkedList<Node> listComputeNode) {
		this.listComputeNode = listComputeNode;
	}

}