package fil.resource.virtual;

import fil.resource.substrate.Node;
import fil.resource.substrate.PhysicalServer;
/**
 * Builds Virtual Service
 * 
 * @author Van Huynh Nguyen
 *
 */
public class Service {

	private int piBelong;
	private Node node;
	private String serviceType;
	private double bandwidth;
	private double power;
	private int requestID;
	private int sfcID;
	private boolean belongToEdge;
	private PhysicalServer belongToServer;
	private double cpu_pi;
	private double cpu_server;
	private String status;
	private int serviceID;
	private String replicate;
	private int sfcIDInit;
	
	public Service() {
		this.setRequestID(0);
		this.setBelongToEdge(false);
		this.setBelongToServer(null);
		this.setNode(null);
		this.setStatus("unassigned");
		this.setReplicate("No");
	}
	
	public double getCpu_pi() {
		return cpu_pi;
	}

	public void setCpu_pi(double cpu_pi) {
		this.cpu_pi = cpu_pi;
	}

	public double getCpu_server() {
			return cpu_server;
	}

	public void setCpu_server(double cpu_server) {
		this.cpu_server = cpu_server;
	}

	public Service getServiceOfType(int type) {
		Service service = new Service();
		switch(type) {
			case 2: service = new Decode(); break;
			case 3: service = new Density(); break;
			case 4: service = new Receive(); break;
			default: throw new java.lang.Error("Service unknown - ServiceMapping");
		}
		return service;
	}
	public int getRequestID() {
		return requestID;
	}

	public void setRequestID(int requestID) {
		this.requestID = requestID;
	}

	public boolean isBelongToEdge() {
		return belongToEdge;
	}

	public void setBelongToEdge(boolean belongToEdge) {
		this.belongToEdge = belongToEdge;
	}
	
	public boolean getBelongToEdge() {
		return belongToEdge;
	}
	public double getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public int getSfcID() {
		return sfcID;
	}

	public void setSfcID(int sfcID) {
		this.sfcID = sfcID;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public PhysicalServer getBelongToServer() {
		return belongToServer;
	}

	public void setBelongToServer(PhysicalServer belongToServer) {
		this.belongToServer = belongToServer;
	}

	public int getPiBelong() {
		return piBelong;
	}

	public void setPiBelong(int piBelong) {
		this.piBelong = piBelong;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getServiceID() {
		return serviceID;
	}

	public void setServiceID(int serviceID) {
		this.serviceID = serviceID;
	}

	public String getReplicate() {
		return replicate;
	}

	public void setReplicate(String replicate) {
		this.replicate = replicate;
		this.sfcIDInit = this.sfcID;
	}

	public int getSfcIDInit() {
		return sfcIDInit;
	}

	public void setSfcIDInit(int sfcIDInit) {
		this.sfcIDInit = sfcIDInit;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

}
