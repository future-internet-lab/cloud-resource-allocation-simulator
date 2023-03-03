/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.RESCE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import fil.resource.substrate.PhysicalServer;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.Event;
import fil.resource.virtual.FatTree;
import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;
import fil.resource.virtual.Topology;


public class Scheduler_RESCE {
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int MAX_SFC_PI = 7;
	final static int HOUR = 3600;
	final static double THOUS = 1000.0;
	final static double MIL = 1000000.0;
	final static double INIENERGY = 15.68; // calculate by power*time/number of pod
	final static double DELENERGY = 14.39; // calculate by power*time/number of pod
	final static List<Integer> edgePosition = 
			Collections.unmodifiableList(Arrays.asList(10, 5, 13, 14));

	private double totalEnergy;
	private double iniEnergy;
	private double delEnergy;
	private int sfcID;
	private int totalRelocate;
	private Topology topo;
	private FatTree fatTree;
	private EdgeMapping edgeMapping;
	private CloudMapping cloudMapping;
	private LinkMapping linkMapping;
	private boolean isSuccess;
	private LinkedList<SFC> listSFCTotal;

	private HashMap<Integer, LinkedList<Double>> feedDataChecker;
	private HashMap<Integer, LinkedList<SFC>> listSFCAllRpi;
//	private LinkedList<Service> poolDecode;
//	private LinkedList<Service> poolDensity;
//	private LinkedList<Service> poolReceive;
	
	//private Topology topo;
	
	public Scheduler_RESCE()  {
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH, NUM_PI, edgePosition);
		cloudMapping = new CloudMapping();
		edgeMapping = new EdgeMapping();
		linkMapping = new LinkMapping();
		listSFCTotal = new LinkedList<>();
		listSFCAllRpi = new HashMap<>();
		for(int i = 0; i < NUM_PI; i ++) {
			listSFCAllRpi.put(i, new LinkedList<SFC>());
		}
		totalEnergy = 0.0;
		iniEnergy = 0.0;
		delEnergy = 0.0;
		sfcID = 0;
		totalRelocate = 0;
		isSuccess = false;
	}
	
	//<-----Prediction will be fetched every time this function is invoked
	public void run(LinkedList<LinkedList<Event>> listTotalEvent, String type) {
		
		LinkedList<Integer> listUsedSer = new LinkedList<>();
		LinkedList<Integer> listReqTW = new LinkedList<>();
		LinkedList<Integer> listReqLvTW = new LinkedList<>();
//		LinkedList<Integer> listRecInSys = new LinkedList<>();
//		LinkedList<Integer> listDenInSys = new LinkedList<>();
		LinkedList<Integer> listSFCActive = new LinkedList<>();
		LinkedList<Integer> listError = new LinkedList<>();
		LinkedList<Integer> listRelocate = new LinkedList<>();
		LinkedList<Integer> listAcceptTW = new LinkedList<>();
		LinkedList<Double> listAveSerUtil = new LinkedList<>();
		LinkedList<Double> listPower = new LinkedList<>();
		LinkedList<Double> listPowerColdTerm = new LinkedList<>();
		LinkedList<Double> listPowerWarm = new LinkedList<>();
		LinkedList<Double> listAcceptance = new LinkedList<>();
		LinkedList<Double> listEnergy = new LinkedList<>();
		LinkedList<Double> listDownTime = new LinkedList<>();

		int accumError = 0;
		double time4Energy = 0.0;
		//<-----Generate request
//		GenRequest sample = new GenRequest();
//		sample.generator();
//		LinkedList<LinkedList<Event>> listTotalEvent = sample.getListEvent();
		//<-----Pass request to edge and start running mapping process
		
		for(int eventInTW = 0; eventInTW < listTotalEvent.size(); eventInTW ++) {
			LinkedList<Event> listEvent = listTotalEvent.get(eventInTW);
			
			// declare variables that use for each TW
//			int error1h = 0;
			int totalReqTW = 0;
			int totalReqLvTW = 0;
			int totalSFCacceptTW = 0;
//			double aveSerUtil = 0.0;
			double power1h = 0.0;
			double powerColdTerm1h = 0.0;
			double powerWarm1h = 0.0;
			double downtime1h = 0.0;

			// reset global variables
			this.setDelEnergy(0.0);
			this.setIniEnergy(0.0);
			
			for(int eventIn = 0; eventIn < listEvent.size(); eventIn ++) {
				double insPower = 0.0;
				Event event = listEvent.get(eventIn);
				
				if(event.getType() == "join") {
					System.out.println("Event join " + event.getTime() +" at TW " + eventInTW);
					totalReqTW ++;
				}else {
					// case event leave sfc that is not in the system
					if(!this.listSFCTotal.contains(event.getSfc()))
						continue;
					System.out.println("Event leave " + event.getTime() +" at TW " + eventInTW);
					totalReqLvTW ++;
				}
				// reset for the event
				this.reset(event.getPiID());
				
				// run scheduling the event
				boolean result = false;
				result = this.runEvent(event, eventInTW);
				
				// variable for storing migration energy
//				double energyMig = this.listSFCTotal.size()*4*19*2; //19J is energy required for init/del a VNF
				if(result) {
					accumError += this.listSFCTotal.size()*4;
					this.totalRelocate += (this.listSFCTotal.size() - 1)*4;
//					error1h += this.listSFCTotal.size()*4;
					if(event.getType() == "join") {
						totalSFCacceptTW ++;
						this.listSFCTotal.add(event.getSfc());
						this.listSFCAllRpi.get(event.getPiID()).add(event.getSfc());

						
					}else {
						this.listSFCTotal.remove(event.getSfc());
						this.listSFCAllRpi.get(event.getPiID()).remove(event.getSfc());
					}
					//calculate accumulated averaging downtime
//					downtime1h += this.getDowntime();
					downtime1h += (this.getDowntime()/this.listSFCTotal.size());
					// calculate energy
					insPower = cloudMapping.getPowerServer(topo) + linkMapping.getPower(topo) + edgeMapping.getPowerEdge(this.listSFCTotal);
					// calculate energy
					this.addTotalEnergy((insPower*(event.getTime() - time4Energy)*HOUR));
					power1h += (insPower*(event.getTime() - time4Energy));
//					powerWarm1h += (cloudMapping.getPowerWasted(topo)*(event.getTime() - time4Energy));

					time4Energy = event.getTime();
					System.out.println("Migration energy: " + cloudMapping.getMigEnergy(topo) + " (J)");
				}else {
					if(event.getType() == "leave") {
						throw new java.lang.Error("Error occurs at leaving process");
					}else {
						System.out.println("System is full");
					}
				}

			} // loop each event
			
			// update variables after 1 hour
			power1h += ((this.getIniEnergy() + this.getDelEnergy())/HOUR);
			powerColdTerm1h += ((this.getIniEnergy() + this.getDelEnergy())/HOUR);
			listDownTime.add(downtime1h);
			listUsedSer.add(cloudMapping.getUsedServer(topo));
			listEnergy.add(this.getTotalEnergy());
			listPower.add(power1h/THOUS);
			listPowerColdTerm.add(powerColdTerm1h/THOUS);
//			listPowerWarm.add(powerWarm1h/THOUS);
			listError.add(accumError);
			listRelocate.add(this.totalRelocate);
			// store log values
			listReqTW.add(totalReqTW);
			listReqLvTW.add(totalReqLvTW);
			// get and store number of active SFC
			listSFCActive.add(this.listSFCTotal.size()); 
			// get serverutilization
			listAveSerUtil.add(cloudMapping.aveSerUtil(topo));
			// get number of service
			// store list acceptance
			listAcceptance.add(totalSFCacceptTW*1.0/totalReqTW);
			listAcceptTW.add(totalSFCacceptTW);
		} // TW loop
		
		// print log values to txt or excel file
		try {
			String path = "./PlotRESCE/" + type;
			write_integer(path + "/UsedSerRESCE.txt",listUsedSer);
			write_integer(path + "/ReqTWRESCE.txt",listReqTW);
			write_integer(path + "/ReqLvTWRESCE.txt",listReqLvTW);
			write_integer(path + "/ReqActiveRESCE.txt",listSFCActive);
			write_integer(path + "/AcceptTWRESCE.txt",listAcceptTW);
			write_integer(path + "/ErrorRESCE.txt",listError);
			write_integer(path + "/RelocateRESCE.txt",listRelocate);
			write_double(path + "/ServerUtilRESCE.txt",listAveSerUtil);
			write_double(path + "/PowerRESCE.txt",listPower);
			write_double(path + "/PowerColdTermRESCE.txt",listPowerColdTerm);
			write_double(path + "/PowerWarmRESCE.txt",listPowerWarm);
			write_double(path + "/EnergyRESCE.txt",listEnergy);
			write_double(path + "/AveAcceptanceRESCE.txt",listAcceptance);
			write_double(path + "/AveDownTimeRESCE.txt",listDownTime);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void reset(int piID) {
		LinkedList<Rpi> listRpi = topo.getListRPi();
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH, NUM_PI, edgePosition);
		topo.setListRPi(listRpi);
//		topo.reset(fatTree);
//		LinkedList<Rpi> ab = topo.getListRPi();
		topo.getListRPi().get(piID).reset();
		cloudMapping = new CloudMapping();
		edgeMapping = new EdgeMapping();
		linkMapping = new LinkMapping();
		for(SFC sfc : listSFCTotal) {
			this.addDelEnergy(DELENERGY*sfc.getListServiceCloud().size());
			sfc.reset();
		}// reset fattree topology
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
	
	public boolean runEvent(Event event, int TW) {
		
		boolean result = false;
		int piID = event.getPiID();
		SFC sfc = event.getSfc();
		Rpi pi = topo.getListRPi().get(piID);
		String eventType = event.getType();
		
		boolean resultEdge = false;
		LinkedList<SFC> listSFCEdge = new LinkedList<>();
		LinkedList<SFC> listAllSFC = new LinkedList<>();
		
		listSFCEdge.addAll(this.listSFCAllRpi.get(piID));
		listAllSFC.addAll(listSFCTotal);
		
		if(eventType == "join") {
			listSFCEdge.add(sfc);
			listAllSFC.add(sfc);
		}else {
			listSFCEdge.remove(sfc);
			listAllSFC.remove(sfc);
		}
		
		if(listSFCEdge.size() <= MAX_SFC_PI) {
			resultEdge = edgeMapping.join(sfc, listSFCEdge, pi);
		}
		
		if(resultEdge) {
			
			boolean resultCloud = false;

			resultCloud = cloudMapping.firstfit(listAllSFC, topo);
				
			if(resultCloud) {
				
				// Link mapping block
				boolean resultEx = false;
				boolean resultIn = false;
				
				resultEx = linkMapping.linkMapExternal(topo, listAllSFC);
				
				resultIn = linkMapping.linkMapInternal(topo, listAllSFC);
				
				// set up CPU, BW for edge tier
				// cloud gets reseted every round so no need to set
				if(resultEx && resultIn) {
					result = true;
							
				}else { // result link mapping
					result = false;
				}
				
			} else{// result cloud
				result = false;
			}
		
		} else{//result edge
			result = false;
		}
		
		if(result) {
			// sum up init energy
			for(SFC sfc0 : listAllSFC) {
				this.addIniEnergy(INIENERGY*sfc0.getListServiceCloud().size());
			}
			// setup cpu, bw for pi
			double cpuEdge = 0;
			double bwEdge = 0;
			for(SFC sfc0 : listSFCEdge) {
				cpuEdge += sfc0.cpuEdgeUsage();
				bwEdge += sfc0.bandwidthUsageOutDC();
			}
			pi.setUsedCPU(cpuEdge); 
			pi.setUsedBandwidth(bwEdge); 
		}else {
			// restore pi to the previous state
			pi.restore();
		}
		return result;
	}
	
	public double getDowntime() {
//		LinkedList<Double> listDT = new LinkedList<>();
		// calculate downtime based on number of services of a SFC that must be 
		// initiated in a server
		double downtime = 0.0;
		Map<PhysicalServer, Integer> listServer = new HashMap<>();
		for(SFC sfc : this.listSFCTotal) {
			LinkedList<Service> listSer = sfc.getListServiceCloud();
			for(Service ser : listSer) {
				PhysicalServer server = ser.getBelongToServer();
				if(!listServer.containsKey(server)) {
					listServer.put(server, 1);
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
				downtime += (4.24 + 0.7*numScale);
//			listDT.add((2.913 + 0.346*numScale - 0.001*numScale*numScale));
		}
		// these servers perform migration in parallel
//		Collections.sort(listDT);
		// return longest downtime caused by services of SFC 
//		double downtime = 0.0;
//		int sizeListMig = this.listSFCTotal.size();
//		if(this.listSFCTotal.size() == 0)
//			return downtime;
	
//		return downtime*sizeListMig;
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
	
	public static void write_excel(String filename, Map<Integer,LinkedList<Integer>> map) throws IOException {
		//Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook();
	      
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.createSheet();

	      //Create row object
	      XSSFRow row;
	      
	      Set < Integer > keyid = map.keySet();
	      int rowid = 0;
	      
	      for (Integer key : keyid) {
	         row = spreadsheet.createRow(rowid++);
	         LinkedList<Integer> objectArr = map.get(key);
	         int cellid = 0;
	         
	         for (Object obj : objectArr){
	            Cell cell = row.createCell(cellid++);
	            cell.setCellValue(obj.toString());
	         }
	      }
	      //Write the workbook in file system
	      FileOutputStream out = new FileOutputStream(new File(filename));
	      workbook.write(out);
	      out.close();
	      workbook.close();
	}

	public static void write_excel_double(String filename, Map<Integer,LinkedList<Double>> map) throws IOException {
		//Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook();
	      
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.createSheet();

	      //Create row object
	      XSSFRow row;
	      
	      Set < Integer > keyid = map.keySet();
	      int rowid = 0;
	      
	      for (Integer key : keyid) {
	         row = spreadsheet.createRow(rowid++);
	         LinkedList<Double> objectArr = map.get(key);
	         int cellid = 0;
	         
	         for (Object obj : objectArr){
	            Cell cell = row.createCell(cellid++);
	            cell.setCellValue(obj.toString());
	         }
	      }
	      //Write the workbook in file system
	      FileOutputStream out = new FileOutputStream(new File(filename));
	      workbook.write(out);
	      out.close();
	      workbook.close();
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

	public int getTotalRelocate() {
		return totalRelocate;
	}

	public void setTotalRelocate(int totalRelocate) {
		this.totalRelocate = totalRelocate;
	}

}
