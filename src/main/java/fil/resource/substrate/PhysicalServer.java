package fil.resource.substrate;

import java.util.LinkedList;

import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;

public class PhysicalServer extends Node {
	final double CPU = 100; //100%
	private String name;
	private double powerServer;
	private int state ; // state =0:  off, state=1:  on
	private LinkedList<SFC> listSFCInServer;
	private LinkedList<SFC> listIndependRev;
	private LinkedList<Service> listService;
	
	public PhysicalServer() {
		this.setNodeType("server");
		this.powerServer = 0;
		this.listSFCInServer = new LinkedList<>();
		this.listIndependRev = new LinkedList<>();
		this.listService = new LinkedList<>();
//		this.ram = RAM;
//		this.name=name;
		//this.listService = new LinkedList<>();
	}
	public PhysicalServer(String name) {
		this.setNodeType("server");
		this.powerServer = 0;
		this.listSFCInServer = new LinkedList<>();
		this.listIndependRev = new LinkedList<>();
		this.listService = new LinkedList<>();
		this.name = name;
	}

	
	public void setPowerServer() {
		this.powerServer = basePowerCal() + warmPowerCal(); 
	}
	
	
	public LinkedList<Service> numWarmService() {
		LinkedList<Service> listWarm = new LinkedList<>();
		for(Service ser : this.listService) {
			if(ser.getStatus() == "unassigned") {
				listWarm.add(ser);
			}
		}
		return listWarm;
	}
	
	public double basePowerCal() {
		double cpu = this.getUsedCPU();
		for(Service ser : this.numWarmService()) {
			cpu -= ser.getCpu_server();
		}
		if(cpu < -0.1)
			throw new java.lang.Error(); // checking CPU negative
		return 95*(cpu/100) + 110; // Pmax x (0.7 + 0.3xCPU) 
	}
	
	public double warmPowerCal() {
		return 5.01 * 0.01 * this.numWarmService().size(); 
	}
	
	public void setPowerServerNom() {
		this.powerServer = 95*(this.getUsedCPU()/100) + 110;
	}
	
	public int getState() {
//		if(this.getUsedCPU() <  0.5) {
		if(this.getListService().isEmpty()) {
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

	public double getCPU() {
		return CPU;
	}
	
	public void setPowerServer(double powerServer) {
		this.powerServer = powerServer;
	}
	
	public double getPowerServer() {
		return powerServer;
	}
	
	public double getMigEnergy() {
		double energy = 0;
		int numVNF = this.getListService().size();
		// regression equation
		energy = numVNF*19.0*2;
//		energy += -5.064*numVNF + 1.44*numVNF*numVNF + 16.044; //init
//		energy += 1.547*numVNF + 0.527*numVNF*numVNF - 1.274; //del
		return energy;
	}
	
	public void reset() {
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