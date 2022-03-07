package fil.resource.substrate;

public class LinkPhyEdge {
	private PhysicalServer physicalServer;
	private SubstrateSwitch edgeSwitch;
	private double bandwidth;
	final private double BANDWIDTH = 1024; // default bandwidth = 1024;
//	private int type; //1: up, 0: down
	public LinkPhyEdge(PhysicalServer phy, SubstrateSwitch edge, double bandwidth)
	{
		this.physicalServer = phy;
		this.edgeSwitch = edge;
		this.bandwidth = bandwidth;
//		this.type= type;
	}
	public LinkPhyEdge() {
		physicalServer = new PhysicalServer();
		edgeSwitch = new SubstrateSwitch();
		bandwidth = 0;
		// TODO Auto-generated constructor stub
	}
	
	
	public PhysicalServer getPhysicalServer() {
		return physicalServer;
	}
	public void setPhysicalServer(PhysicalServer physicalServer) {
		this.physicalServer = physicalServer;
	}
	public void reset() {
		this.bandwidth = 1024;
	}
	public SubstrateSwitch getEdgeSwitch() {
		return edgeSwitch;
	}
	public void setEdgeSwitch(SubstrateSwitch edgeSwitch) {
		this.edgeSwitch = edgeSwitch;
	}
	public double getBandwidth() {
		return bandwidth;
	}
	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}
}
