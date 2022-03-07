package fil.resource.substrate;


import java.util.LinkedList;
import fil.resource.virtual.*;
public class RpiNew extends SubstrateNode {

	final static double BASELINE = 1.28;
	final static double BW = 100.0;

	private int id;
	private double usedBandwidth;
	private double currentPower;
	private double cpuTH;
	private int cluster;
	private boolean overload;
	
	private double usedCPU_bu;
	private double usedBandwidth_bu;
	private double currentPower_bu;
	private boolean overload_bu;
	//private String name;
	private LinkedList<Service> listService;
	
	public RpiNew(int id) {
		this.setId(id);
	}
	public RpiNew(int id, int position) {
//		this.setRemainBandwidth();
		//this.setRemainCPU();
		setCluster(position);
		//this.listService = new LinkedList<>();
		this.setOverload(false);
		this.setId(id); 
		this.setUsedCPU(0);;
		this.usedBandwidth = 0;
		this.currentPower = BASELINE;
//		this.setUsedBandwidth(0);
//		this.setCurrentPower(1.28);
		//this.setName(name);
	}
	public RpiNew(double state, LinkedList<Service> listService) {
//		this.setRemainBandwidth();
//		this.setRemainCPU();
		this.setListService(listService);
	}
//	public String getName() {
//		return name;
//	}
//	public void setName(String name) {
//		this.name = name;
//	}
	
	public LinkedList<Service> getListService() {
		return listService;
	}
	public void setListService(LinkedList<Service> listService) {
		this.listService = listService;
	}
	public void addService(Service service) {
		this.listService.add(service);
	}
	
	public void removeService(Service service) {
		if(this.listService.contains(service))
			this.listService.remove(service);
	}
	

	public double getRemainBandwidth() {
		return (BW - this.getUsedBandwidth());
	}
//	public void setRemainBandwidth() {
//		this.remainBandwidth = BW - this.usedBandwidth;
//	}
	public double getCurrentPower() {
		return this.currentPower;
	}
	public void setCurrentPower(double currentPower) {
		this.currentPower += currentPower;
	}
	

	public void reset() {
		this.setUsedCPU(0);
		this.usedBandwidth = 0;
		this.currentPower = BASELINE;
		this.overload = false;
		this.backup();
	}
	
	public void backup() {
		this.usedCPU_bu = this.getUsedCPU();
		this.usedBandwidth_bu = this.usedBandwidth;
		this.currentPower_bu = this.currentPower;
		this.overload_bu = this.overload;
	}
	
	public void restore() {
		this.setUsedCPU(this.usedCPU_bu);
		this.usedBandwidth = this.usedBandwidth_bu;
		this.currentPower = this.currentPower_bu;
		this.overload = this.overload_bu;
	}
	public double getUsedBandwidth() {
		return usedBandwidth;
	}
	public void setUsedBandwidth(double usedBandwidth) {
		this.usedBandwidth += usedBandwidth;
	}
	public double getCpu_threshold() {
		return this.cpuTH;
	}
	public int getCluster() {
		return cluster;
	}
	public void setCluster(int cluster) {
		this.cluster = cluster;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean isOverload() {
		return overload;
	}
	public void setOverload(boolean overload) {
		this.overload = overload;
	}
}
