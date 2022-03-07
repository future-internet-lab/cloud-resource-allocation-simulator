package fil.resource.substrate;

public class Node {
	private String nodeType;
	private int nodeID;
	final static double CPU = 100; //100%
	private double usedCPU;
	
	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}
	
	public double getRemainCPU() {
		return (CPU - this.usedCPU);
	}

	public double getUsedCPU() {
		return usedCPU;
	}

//	public void setUsedCPU(double usedCPU) {
//		this.usedCPU = usedCPU;
//	}
	
	public void setUsedCPU(double usedCPU) {
		this.usedCPU += usedCPU;
	}
	
	public void resetCPU() {
		this.usedCPU = 0.0;
	}

	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}
	
}
