package fil.resource.substrate;

import java.util.LinkedList;

import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;

public class PhysicalServerNew extends SubstrateNode {
	
	private String name;
	private double powerServer;
	private int state ; // state =0:  off, state=1:  on
	private LinkedList<SFC> listSFCInServer;
	private LinkedList<SFC> listIndependRev;
	private LinkedList<Service> listService;
	
	public PhysicalServerNew() {
		this.setUsedCPU(0);
		this.powerServer = 0;
		this.listSFCInServer = new LinkedList<>();
		this.listIndependRev = new LinkedList<>();
		this.listService = new LinkedList<>();
	}
	
	public PhysicalServerNew(String name) {
		this.setUsedCPU(0);
		this.setName(name);this.setPowerServer(0);
		this.listSFCInServer = new LinkedList<>();
		this.listIndependRev = new LinkedList<>();
		this.listService = new LinkedList<>();
	}

	
	public void setPowerServer() {
		this.powerServer = 95*(this.getUsedCPU()/100) + 221;
	}
	
	
	public int getState() {
		if(this.getUsedCPU() <  0.5) {
			this.state = 0;
		}
		else
			this.state = 1;
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
//	public void addService(Service service) {
//		this.listService.add(service);
//	}
//	
//	public void removeService(Service service) {
//		if(this.listService.contains(service))
//			this.listService.remove(service);
//	}
//	
//	public LinkedList<Service> getListService() {
//		return this.listService;
//	}

	
	public void setPowerServer(double powerServer) {
		this.powerServer = powerServer;
	}
	
	public double getPowerServer() {
		return powerServer;
	}

//	public double calCurrentPowerServer(double cpuServer) {
//		double cpuTotal = this.usedCPUServer + cpuServer;
//		System.out.println("Total CPU Server right now is " + cpuTotal + " ....... \n");
//		double numServer = Math.floor(cpuTotal/100);
//		double cpuFragment = cpuTotal - 100*numServer;
//		 return numServer*this.calculatePowerServer(100) +
//				this.calculatePowerServer(cpuFragment);

	
	public void reset() {
		this.setUsedCPU(0);;
		this.state = 0 ; // state =0:  off, state=1:  on
		this.listSFCInServer = new LinkedList<SFC>();
		this.listIndependRev  = new LinkedList<SFC>();
		this.listService  = new LinkedList<Service>();
	}
	
	public LinkedList<SFC> getListSFCInServer() {
		return listSFCInServer;
	}
	public void setListSFCInServer(LinkedList<SFC> listSFCInServer) {
		this.listSFCInServer = listSFCInServer;
	}
	public LinkedList<SFC> getListIndependRev() {
		return listIndependRev;
	}
	public void setListIndependRev(LinkedList<SFC> listIndependRev) {
		this.listIndependRev = listIndependRev;
	}
	public LinkedList<Service> getListService() {
		return listService;
	}
	public void setListService(LinkedList<Service> listService) {
		this.listService = listService;
	}
}