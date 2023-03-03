package fil.agorithm.SFCPlanner;

import java.util.HashMap;
import java.util.LinkedList;

import fil.resource.substrate.Node;
import fil.resource.substrate.SubstrateLink;
import fil.resource.substrate.SubstrateSwitch;
import fil.resource.virtual.Topology;
import fil.resource.virtual.modelHP;


/**
 * This class helps to find all paths exit between source node and destination
 * node
 * 
 * @author Van Huynh Nguyen
 * 
 * 
 */
public class BFS {

	private Node startNode;
	private Node endNode;
	private double bwDemand;
	private LinkedList<LinkedList<Node>> allPaths;
	private LinkedList<SubstrateLink> shortestPath;
	private LinkedList<LinkedList<SubstrateLink>> listAvailablePath;
	/**
	 * The initial class
	 */
	
	public BFS(Node start, Node end, double bw) {
		startNode = start;
		endNode = end;
		bwDemand = bw;
		allPaths = new LinkedList<>();
		listAvailablePath = new LinkedList<>();
	}

	/**
	 * Initial some parameters and run main function
	 * 
	 * @param topo
	 *            This is the topology object, which contains all informations
	 *            of Substrate Network
	 */
//	public LinkedList<SubstrateSwitch> run(Topology topo, LinkedList<SubstrateSwitch> path) {
//	
////		mypath.clear();
//		LinkedList<Node> visited = new LinkedList<>();
//		visited.add(startNode);
//		breadthFirst(topo, visited); // start finding path from the startNode and topology
////		getAvailablePath(topo);
//		path = getShortestPath();
////		System.out.println("Path goes through " + path.size() + " switches");
////		System.out.println("Done finding path! \n");
//		return path;
//	}
//	
	public void run(Topology topo) {
		
		LinkedList<Node> visited = new LinkedList<>();
		HashMap<Node, Boolean> status = new HashMap<>();
		visited.add(startNode);
		breadthFirst(startNode, endNode, topo, visited, status); // start finding path from the startNode and topology
//		depthFirst(topo, visited);
//		for(LinkedList<Node> path : allPaths) {
//			System.out.print("Path travel through: ");
//			for(Node node : path) {
//				System.out.print(" " + node.getNodeType());
//			}
//			System.out.println();
//		}
//		System.out.println("all path: " + allPaths);
		findAvailablePath(topo);
		findShortestPath();
	}
	/**
	 * This is the main function, which is used to find all paths exit between
	 * startNode and endNode
	 * 
	 * @param topo
	 *            This is the topology object, which contains all informations
	 *            of Substrate Network
	 * @param visited
	 *            This is the list of nodes, which are visited by algorithm
	 */
	
//	public void breadthFirst(Topology topo, LinkedList<Node> visited) {
//		LinkedList<Node> nodes = topo.adjacentNodes(visited.getLast());
//		for (Node node : nodes) {
//
//			if (visited.contains(node)) {
//				continue;
//			}
//			// if found the end node, update path to mypath list
//			if (node.equals(endNode)) {
//				visited.add(node);
//				printPath(visited);
//				visited.removeLast();
//				break;
//			}
//		}
//
//		for (Node node : nodes) {
//
//			if (visited.contains(node) || node.equals(endNode)) {
//				continue;
//			}
//			visited.addLast(node);
//			breadthFirst(topo, visited);
//			visited.removeLast();
//		}
//
//	}

	public void breadthFirst(Node a, Node b, Topology topo, LinkedList<Node> visited, HashMap<Node, Boolean> status) {
//		System.out.println("Node A: " + a.getNodeType() + "   " + a);
//		System.out.println("Node B: " + b.getNodeType() + "   " + b);

		if(a.equals(b)) {
//			System.out.println("path: " + visited);
			LinkedList<Node> path = new LinkedList<>();
			path.addAll(visited);
			allPaths.add(path);
			return;
		}
		
		status.put(a, true);
		
		if(!this.checkLevel(visited)) { // constraint to limit the tree search
			return;
		}
		
		LinkedList<Node> nodes = topo.adjacentNodes(visited.getLast());
		
		for (Node node : nodes) {
			if(!status.containsKey(node) || !status.get(node)) {
				visited.add(node);
				breadthFirst(node, b, topo, visited, status);
				visited.remove(node);
			}
		}
		status.put(a, false);
	}
	
	@SuppressWarnings("unused")
	private void depthFirst(Topology topo, LinkedList<Node> visited) {
        LinkedList<Node> nodes = topo.adjacentNodes(visited.getLast());
        // examine adjacent nodes
        System.out.println("size of allpaths is: " + allPaths.size());
        for (Node node : nodes) {
            if (visited.contains(node)) {
                continue;
            }
            if (node.equals(endNode)) {
                visited.add(node);
                allPaths.add(visited);
                visited.removeLast();
                break;
            }
        }
        for (Node node : nodes) {
            if (visited.contains(node) || node.equals(endNode) || !this.checkLevel(visited)) {
                continue;
            }
            visited.addLast(node);
            depthFirst(topo, visited);
            visited.removeLast();
        }
    }
	
	private boolean checkLevel(LinkedList<Node> visited) {
		boolean result = false;
		int limitE = 2; int limitA = 2; int limitC = 2;
		int countE = 0; int countA = 0; int countC = 0;

		for(Node node : visited) {
			switch(node.getNodeType()) {
			case "core": countC ++; break;
			case "agg": countA ++; break;
			case "edge": countE ++; break;
			default: break;
			}
		}
		if(countE > limitE || countA > limitA || countC > limitC)
			result = false;
		else
			result = true;
		return result;
	}
	public  void findShortestPath() {
		int numMin = Integer.MAX_VALUE;
		for(LinkedList<SubstrateLink> path : listAvailablePath)
		{
			if(path.size() < numMin)
			{
				numMin = path.size();
				setShortestPath(path);
			}

		}
	}
	
	public void findAvailablePath(Topology topo) {
		// sort all bandwidth-available paths
		for (int i = 0; i < allPaths.size(); i ++) { // check all path
			
			LinkedList<Node> path = allPaths.get(i); // get each specific path
			LinkedList<SubstrateLink> linkPath = new LinkedList<>();

			for (int j = 0; j < (path.size() - 1); j ++) {
				
				
				Node a = path.get(j);
				Node b = path.get(j + 1);

				SubstrateLink link = topo.getLinkOfNodes(a, b);
								
				if(bwDemand > (link.getRemainBW())) { // one route has failed
//					System.out.println("This path has no remained BW \n");
					break;
				}else if(j == (path.size() - 2)){
					this.listAvailablePath.add(linkPath);
				}else {
					linkPath.add(link);
				}
			}
		}
	}
//	
	public double getPower(Topology topo)
	{
		double power = 0;
		modelHP HP = new modelHP();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
//		for(SubstrateLink link: topo.getLinkBandwidth())
//		{
//			double bw = link.getBandwidth();
//			SubstrateSwitch s = link.getStartSwitch();
//			if(listSwitch.containsKey(s.getNameSubstrateSwitch()))
//			{
//				SubstrateSwitch sw = listSwitch.get(s.getNameSubstrateSwitch());
//				sw.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), sw);
//			}
//			else				
//			{
//				s.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), s);
//			}
//			
//		}
		for(SubstrateSwitch entry: listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}


	
	public LinkedList<LinkedList<SubstrateLink>> getAvailablePaths() {
		return this.listAvailablePath;
	}
	public static void main(String[] args) {
		
	}

	public LinkedList<SubstrateLink> getShortestPath() {
		return shortestPath;
	}

	public void setShortestPath(LinkedList<SubstrateLink> shortestPath) {
		this.shortestPath = shortestPath;
	}

	public LinkedList<LinkedList<Node>> getAllPaths() {
		return allPaths;
	}

	public void setAllPaths(LinkedList<LinkedList<Node>> allPaths) {
		this.allPaths = allPaths;
	}

}