/**
* @author EdgeCloudTeam-HUST
*
* @date 
* 
* @title Reconfiguration Aware Orchestration for Network Function Virtualization With Time-Varied Workload in Virtualized Datacenters
*/
package fil.algorithm.REAP;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fil.resource.substrate.PhysicalServer;
import fil.resource.virtual.Event;
import fil.resource.virtual.FatTree;
import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;
import fil.resource.virtual.Topology;

public class Scheduler_REAP {
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int HOUR = 3600;
	final static double THOUS = 1000.0;
	final static double MIL = 1000000.0;
	final static double INIENERGY = 19.0;
	final static List<Integer> edgePosition = 
			Collections.unmodifiableList(Arrays.asList(10, 5, 13, 14));
	
	private double totalEnergy;
	private double iniEnergy;
	private double delEnergy;
	private double downtime;
	private int sfcID;
	private int totalError;
	private int totalInsuf;
	private Topology topo;
	private FatTree fatTree;
	private EdgeMapping edgeMapping;
	private CloudMapping cloudMapping;
	private LinkMapping linkMapping;
	private boolean isSuccess;
	private LinkedList<SFC> listSFCTotal;

	private HashMap<Integer, LinkedList<Double>> feedDataChecker;
	private HashMap<Integer, LinkedList<SFC>> listSFCAllRpi;

	
	//private Topology topo;
	
	public Scheduler_REAP() {
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH, NUM_PI, edgePosition);
		cloudMapping = new CloudMapping();
		edgeMapping = new EdgeMapping();
		linkMapping = new LinkMapping();
		listSFCTotal = new LinkedList<>();
		feedDataChecker = new HashMap<>();
		listSFCAllRpi = new HashMap<>();
		for(int i = 0; i < NUM_PI; i ++) {
			listSFCAllRpi.put(i, new LinkedList<SFC>());
		}

		totalEnergy = 0.0;
		iniEnergy = 0.0;
		delEnergy = 0.0;
		sfcID = 0;
		totalError = 0;
		isSuccess = false;
	}
	
	public void run(LinkedList<LinkedList<Event>> listTotalEvent, String type) {
		
		
		LinkedList<Integer> listReqTW = new LinkedList<>();
		LinkedList<Integer> listReqLvTW = new LinkedList<>();
		LinkedList<Integer> listSFCActive = new LinkedList<>();
		LinkedList<Integer> listError = new LinkedList<>();
		LinkedList<Integer> listAcceptTW = new LinkedList<>();
		LinkedList<Integer> listUsedSer = new LinkedList<>();
		LinkedList<Double> listAveSerUtil = new LinkedList<>();
		LinkedList<Double> listTotalPower = new LinkedList<>();
		LinkedList<Double> listAcceptance = new LinkedList<>();
		LinkedList<Double> listEnergy = new LinkedList<>();
		LinkedList<Double> listDownTime = new LinkedList<>();

		
		double time4Energy = 0.0;
		
		//<-----Pass request to edge and start running mapping process
		
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
			
			// totalReqTW is not known at the beginning of TW
			for(Event event : listEvent) {
				if(event.getType() == "join")
					totalReqTW ++;
			}
			// loop each event
			for(int eventIn = 0; eventIn < listEvent.size(); eventIn ++) {
				this.totalInsuf = 0;
				this.downtime = 0.0;
				double insPower = 0.0;
				
				Event event = listEvent.get(eventIn);
				
				if(event.getType() == "join") {
					System.out.println("Event join " + event.getTime() +" at TW " + eventInTW);
//					totalReqTW ++;
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
					aveSerUtil = cloudMapping.aveSerUtil(topo);
					insPower = cloudMapping.getPowerServer(topo) + linkMapping.getPower(topo) + edgeMapping.getPowerEdge(this.listSFCTotal);
					if(event.getType() == "join") {
						totalSFCacceptTW ++;	
					}else {
						totalReqLvTW ++;
					}
					//calculate accumulated averaging downtime
//					downtime1h += (this.downtime/this.listSFCTotal.size());
					downtime1h += (this.downtime);

					// calculate energy
					this.addTotalEnergy((insPower*(event.getTime() - time4Energy)*HOUR));
					power1h += (insPower*(event.getTime() - time4Energy));
					time4Energy = event.getTime();
				}else {
					if(event.getType() == "leave") {
						throw new java.lang.Error("Error occurs at leaving process");
					}else {
						System.out.println("System is full");
					}
				}
			} // loop each event
			
			for(Service service : cloudMapping.getPoolService()) {
				if(service.getStatus() == "unassigned")
					this.totalError ++;
			}
			
			// remove all idle PM to purify the system
//			cloudMapping.leaveIdle(topo);
			
			// update variables after 1 hour
			power1h += ((this.getIniEnergy() + this.getDelEnergy())/HOUR);
			
			listDownTime.add(downtime1h);
			listEnergy.add(this.getTotalEnergy());
			listTotalPower.add(power1h/1000.0);
			listError.add(this.totalError);
			// store log values
			listReqTW.add(totalReqTW);
			listReqLvTW.add(totalReqLvTW);
			// get and store number of active SFC
			listSFCActive.add(this.listSFCTotal.size()); 
			// get serverutilization
			listAveSerUtil.add(aveSerUtil);
			listUsedSer.add(cloudMapping.getUsedServer(topo));
			// get number of service
			// store list acceptance
			listAcceptance.add(totalSFCacceptTW*1.0/totalReqTW);
			listAcceptTW.add(totalSFCacceptTW);
		} // TW loop
		
		// print log values to txt or excel file
		try {
			String path = "./PlotREAP/" + type;
			write_integer(path + "/AveReqTWREAP.txt",listReqTW);
			write_integer(path + "/AveReqLvTWREAP.txt",listReqLvTW);
			write_integer(path + "/AveReqActiveREAP.txt",listSFCActive);
			write_integer(path + "/AveAcceptTWREAP.txt",listAcceptTW);
			write_integer(path + "/TotalErrorREAP.txt",listError);
			write_integer(path + "/AveUsedServerREAP.txt",listUsedSer);
			write_double(path + "/AveServerUtilREAP.txt",listAveSerUtil);
			write_double(path + "/AvePowerREAP.txt",listTotalPower);
			write_double(path + "/TotalEnergyREAP.txt",listEnergy);
			write_double(path + "/AveAcceptanceREAP.txt",listAcceptance);
			write_double(path + "/AveDowntimeREAP.txt",listDownTime);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

public boolean runEvent(Event event, int TW) {
		
		boolean resultAll = false;
		int piID = event.getPiID();
		SFC sfc = event.getSfc();
		String eventType = event.getType();
		
		if(eventType == "join") {
			LinkedList<SFC> result = new LinkedList<>();
			LinkedList<SFC> resultFailed = new LinkedList<>();
			LinkedList<SFC> resultLinkFailed = new LinkedList<SFC>();
			LinkedList<SFC> listSFCOnRpi = this.listSFCAllRpi.get(piID);
			
			result = edgeMapping.join(topo, sfc, listSFCOnRpi, linkMapping);
			// cloud join function should search in the VNF pool to fill the
			// SFC, it will have to initiate more VNF if the number is not
			// enough or release VNF if the number exceeds necessity
			if(result.size() > 0) {

				boolean redirect = false;
				boolean stop = false;
				
				while(!redirect) {
					
					redirect = true;
					
					// redirect first
					result = cloudMapping.redirect(result, topo, listSFCTotal);
					
					if(stop) // allow system to redirect one more time before stopping
						break;
					// check if all VNF has been assigned or not?
					Map <SFC, LinkedList<Service>> migSFC = new HashMap<>();
					for(SFC sfc0 : result) {
						for(Service service : sfc0.getListService()) {
							if(!service.getBelongToEdge() && service.getStatus() == "unassigned") {
								redirect = false;
								this.totalError ++; // one VNF is not sufficiently
								this.totalInsuf ++;
								if(!migSFC.containsKey(sfc0))
									migSFC.put(sfc0, new LinkedList<>());
								// redirect fails, process scaleOut
								boolean success = cloudMapping.scaleOut(sfc0, service, topo);
								if(!success) {
									this.totalError --;
									this.totalInsuf --;
									resultFailed.add(sfc0);
									System.out.println("No CPU left for mapping.");
									stop = true; // resource runs out, stop while loop
									break;
								}else {
									migSFC.get(sfc0).add(service);
									this.addIniEnergy(INIENERGY);
								}
							}
						} // service loop
					} // SFC loop
					// calculate downtime accumulation of this mapping process
					this.downtime += this.getDowntime(migSFC);
				} // while loop
				for(SFC sfcF : result) {
					for(Service sv : sfcF.getListService())
						if(!sv.isBelongToEdge() && sv.getBelongToServer() == null) {
							resultFailed.add(sfcF);
							break;
						}
				}
				
				for(SFC sfcF : resultFailed) {
					edgeMapping.leave(topo, piID, sfcF);
					cloudMapping.deleteSFC(sfcF);
					if(result.contains(sfcF))
						result.remove(sfcF);
					if(this.listSFCTotal.contains(sfcF))
						this.listSFCTotal.remove(sfcF);
					if(listSFCOnRpi.contains(sfcF))
						listSFCOnRpi.remove(sfcF);
					
				}
				
				if(result.size() > 0) {
					
					// if the SFC has been filled with enough VNF then it can be
					// add to the list of total SFC. Then, these VNFs need to
					// change their status to "assigned".
					
					// Link mapping block
					LinkedList<SFC> SFCLinkFailed_t = new LinkedList<SFC>();
					SFCLinkFailed_t = linkMapping.linkMapExternal(topo, result);
					resultLinkFailed.addAll(SFCLinkFailed_t);
					for(SFC sfcL : SFCLinkFailed_t) { // remove failed links
						if(result.contains(sfcL))
							result.remove(sfcL);
					}
					SFCLinkFailed_t.clear();
					SFCLinkFailed_t = linkMapping.linkMapInternal(topo, result);
					resultLinkFailed.addAll(SFCLinkFailed_t);
	//				totalRejectLink += resultLinkFailed.size();
					for(SFC sfcL : SFCLinkFailed_t) { // remove failed links
						if(result.contains(sfcL))
							result.remove(sfcL);
					}
					
					for(SFC sfcF : resultLinkFailed) {
						edgeMapping.leave(topo, piID, sfcF);
						cloudMapping.deleteSFC(sfcF);
						if(this.listSFCTotal.contains(sfcF))
							this.listSFCTotal.remove(sfcF);
						if(listSFCOnRpi.contains(sfcF))
							listSFCOnRpi.remove(sfcF);
					}
					
					// double checking and adding final result to listTotal
					for(SFC sfc1 : result) {
						for(int i = 4; i > 1; i --) {
							if(sfc1.getService(i).getStatus() == "unassigned")
								throw new java.lang.Error("Error happens in double-checking.");
						}
						if(!listSFCOnRpi.contains(sfc1))
							listSFCOnRpi.add(sfc1);
						
						if(!this.listSFCTotal.contains(sfc1))
							this.listSFCTotal.add(sfc1);
						
					}
					
					// store sfc that is accepted
					if(result.contains(sfc) && resultFailed.isEmpty() && resultLinkFailed.isEmpty()) {
						resultAll = true;
					}
				
				}else {
					resultAll = false;
					// cloud fails because of two reasons:
					// CPU shortage or link shortage
//					cloudMapping.consolidation(topo);
					//<---TBD
				}
			}else {
				resultAll = false;// edge failed then no way to map this SFC
				
			}
		}else { // leave process
			LinkedList<SFC> listSFCLeave = new LinkedList<>();
			listSFCLeave.add(sfc);
		
			edgeMapping.leave(topo,piID, sfc);
			LinkedList<SFC> listSFCRpi = listSFCAllRpi.get(piID);
			listSFCRpi.remove(sfc);
			listSFCTotal.remove(sfc);
			
			linkMapping.leave(listSFCLeave, topo);
			cloudMapping.leave(listSFCLeave);
			// sum up deleted energy required
			this.addDelEnergy(sfc.getListServiceCloud().size()*INIENERGY);
			resultAll = true;
		}
		return resultAll;
	}
	
		//<-----Get number of VNF Type "type" in Cloud at this time window
	public LinkedList<SFC> getNumberVNFTypePre(int type){
		LinkedList<SFC> listVNFTypeCurrent = new LinkedList<>();
		for(SFC sfc : this.listSFCTotal) {
			if(sfc.getService(type).getBelongToEdge() == false) {
				listVNFTypeCurrent.add(sfc);
			}
		}
		return listVNFTypeCurrent;
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
					listServer.put(server, 0);
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
				downtime += (2.913 + 0.346*numScale - 0.001*numScale*numScale);
//			listDT.add((2.913 + 0.346*numScale - 0.001*numScale*numScale));
		}
		// these servers perform migration in parallel
//		Collections.sort(listDT);
		// return longest downtime caused by services of SFC 
//		if(listDT.size() != 0)
//			downtime = listDT.getLast();
		return downtime;
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

	public LinkMapping getLinkMapping() {
		return linkMapping;
	}

	public void setLinkMapping(LinkMapping linkMapping) {
		this.linkMapping = linkMapping;
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

	public CloudMapping getServiceMapping() {
		return cloudMapping;
	}

	public void setServiceMapping(CloudMapping serviceMapping) {
		this.cloudMapping = serviceMapping;
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
	
	public int getNumVNFMigration() {
		int numVNF = this.cloudMapping.getVNFmigration();
		return numVNF;
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

	public int getTotalInsuf() {
		return totalInsuf;
	}

	public void setTotalInsuf(int totalInsuf) {
		this.totalInsuf = totalInsuf;
	}

	public double getDowntime() {
		return downtime;
	}

	public void setDowntime(double downtime) {
		this.downtime = downtime;
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
