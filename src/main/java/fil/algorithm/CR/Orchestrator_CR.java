/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.CR;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.SAXException;

import fil.ml.*;
import fil.resource.substrate.PhysicalServer;
import fil.resource.substrate.SubstrateLink;
import fil.resource.virtual.Event;
import fil.resource.virtual.FatTree;
import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;
import fil.resource.virtual.Topology;

public class Orchestrator_CR {
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int HOUR = 3600;
	final static double THOUS = 1000.0;
	final static double MIL = 1000000.0;
	final static double INIENERGY = 15.68; // calculate by power*time/number of pod
	final static double DELENERGY = 14.39; // calculate by power*time/number of pod

	private double totalEnergy;
	private double iniEnergy;
	private double delEnergy;
	private double downtime;
	private int sfcID;
	private Double totalError;
	private int totalCold;
	private int totalInSuf;
	private Topology topo;
	private FatTree fatTree;
	private EdgeMapping edgeMapping;
	private CloudMapping cloudMapping;
	private LinkMapping linkMapping;
	private boolean isSuccess;
	private LinkedList<SFC> listSFCTotal;
	private LinkedList<Integer> edgePosition;
	private LinkedList<Integer> listMLResultRec;
	private LinkedList<Integer> listMLResultDen;
	private HashMap<Integer, LinkedList<Double>> feedDataChecker;
	private HashMap<Integer, LinkedList<SFC>> listSFCAllRpi;
	private Inference denModel;
	private Inference recModel;

		
	public Orchestrator_CR() throws IOException, SAXException, JAXBException  {
		edgePosition = new LinkedList<>();
		edgePosition.add(10);
		edgePosition.add(5);
		edgePosition.add(13);
		edgePosition.add(14);
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH, NUM_PI, edgePosition);
		cloudMapping = new CloudMapping();
		edgeMapping = new EdgeMapping();
		linkMapping = new LinkMapping();
		listSFCTotal = new LinkedList<>();
		listMLResultRec = new LinkedList<>();
		listMLResultDen = new LinkedList<>();

		feedDataChecker = new HashMap<>();
		listSFCAllRpi = new HashMap<>();
		for(int i = 0; i < NUM_PI; i ++) {
			listSFCAllRpi.put(i, new LinkedList<SFC>());
		}
		totalEnergy = 0.0;
		iniEnergy = 0.0;
		delEnergy = 0.0;
		sfcID = 0;
		totalError = 0.0;
		totalCold = 0;
		totalInSuf = 0;
		isSuccess = false;
	}
	

	
	public void run(LinkedList<LinkedList<Event>> listTotalEvent, double TW, String type, boolean ML, String MLtype) throws IOException, SAXException, JAXBException {
		
		/*
		 * The following ML model is chosen:
		 * - GB for 0.5 and 1.0 TW in both type of VNF
		 * - LSTM for 2.0 TW in both type of VNF
		 * Inputs:
		 * - GB: Whole set of features, name of output
		 * - LSTM:
		 * 		- Density: '# VNF Density current', seq_length = 12, num_layer = 2, batch_size = 1, hidden_size = 100
		 * 		- Receive: 'Time ','# VNF Receive current',  seq_length = 12, num_layer = 1, batch_size = 1, hidden_size = 500
		 */
		if(MLtype == "LSTM") {
			this.denModel = new LSTM(TW, "den", 12, 1, 1, 500);
			this.recModel = new LSTM(TW, "rec", 12, 2, 1, 100);
		}else {
			this.denModel = new Others(TW, "den", type, "# VNF Density future");
			this.recModel = new Others(TW, "rec", type, "# VNF Receive future");	
		}
		
		/* init list for logging data */
		LinkedList<Integer> listReceive = new LinkedList<>();
		LinkedList<Integer> listDensity = new LinkedList<>();
		LinkedList<Integer> listUsedSer = new LinkedList<>();
		LinkedList<Integer> listReqTW = new LinkedList<>();
		LinkedList<Integer> listReqLvTW = new LinkedList<>();
		LinkedList<Integer> listSFCActive = new LinkedList<>();
		LinkedList<Double> listError = new LinkedList<>();
		LinkedList<Integer> listRelocate = new LinkedList<>();
		LinkedList<Integer> listColdStart = new LinkedList<>();
		LinkedList<Integer> listInSuf = new LinkedList<>();
		LinkedList<Integer> listRedundant = new LinkedList<>();
		LinkedList<Integer> listRedirect = new LinkedList<>();
		LinkedList<Integer> listAcceptTW = new LinkedList<>();
		LinkedList<Double> listAveSerUtil = new LinkedList<>();
		LinkedList<Double> listTotalPower = new LinkedList<>();
		LinkedList<Double> listPowerColdTerm = new LinkedList<>();
		LinkedList<Double> listPowerWarm = new LinkedList<>();
		LinkedList<Double> listAcceptance = new LinkedList<>();
		LinkedList<Double> listEnergy = new LinkedList<>();
		LinkedList<Double> listDownTime = new LinkedList<>();
		List<Double> listLinkCore = new ArrayList<>();
		List<Double> listLinkAgg = new ArrayList<>();
		
		// list to store all MLdata of all TWs for excel file for training
		LinkedList<LinkedList<Double>> storeMLData = new LinkedList<>();
		LinkedList<Double> listMLDataTW = new LinkedList<>();
		LinkedList<Integer> resultPre = new LinkedList<>();
		
		// declare variables that use for each TW
		int totalRedun1h = 0;
		int totalReq1h = 0;
		int totalReqLv1h = 0;
		int totalSFCaccept1h = 0;
		double time4Energy = 0.0;
		double power1h = 0.0;
		double powerColdTerm1h = 0.0;
		double powerWarm1h = 0.0;
		double downtime1h = 0.0;
		// pass request to edge and start running mapping process
		for(int listEventIndex = 0; listEventIndex < listTotalEvent.size(); listEventIndex ++) {
			// get event
			LinkedList<Event> listEvent = listTotalEvent.get(listEventIndex);
			
			// declare time point
			double timeP = 0.0;
			if(TW == 0.5)
				timeP = listEventIndex*TW;
			else
				timeP = (double) listEventIndex;
			
			// reset power1h variable in 1hour period
			if(timeP == (int) (timeP)) {
				totalRedun1h = 0;
				totalReq1h = 0;
				totalReqLv1h = 0;
				totalSFCaccept1h = 0;
				power1h = 0.0;
				powerColdTerm1h = 0.0;
				powerWarm1h = 0.0;
				downtime1h = 0.0;
				// reset global variables
				this.setDelEnergy(0.0);
				this.setIniEnergy(0.0);
			}
			

			// running prediction and getting results
			// the if condition is tweaked for 2-hour TW, further coding is required for better generalization
			if((TW == 2.0 && timeP % 2.0 == 0.0) || TW != 2.0) {
				// below renew list to fix the mismatch between odd and event TW recording data
				listMLDataTW = new LinkedList<>();
				// declare ML list and data
//				LinkedList<Double> listMLDataTW = new LinkedList<>();
				listMLDataTW.add(timeP); //time
				listMLDataTW.add(edgeMapping.getCPUaverageAll(topo)); //cpu per all devices
				listMLDataTW.add(edgeMapping.getCPUaverageNonZero(topo)); //cpu per non-zero devices
				listMLDataTW.add(cloudMapping.getCPUServerUtil(topo)); //CPU server average
				listMLDataTW.add(edgeMapping.getBWaverageAll(topo)); //bw per all devices
				listMLDataTW.add(edgeMapping.getBWaverageNonZero(topo)); //bw per non zero
				listMLDataTW.add(topo.getLinkUtil("core", "agg")); //link util core-agg
				listMLDataTW.add(topo.getLinkUtil("agg", "edge")); //link util agg-edge
				listMLDataTW.add(edgeMapping.getNonZeroDevice(topo)); //number edge devices
				listMLDataTW.add(topo.getNonZeroLink("core", "agg")); //link util core-agg
				listMLDataTW.add(topo.getNonZeroLink("agg", "edge")); //link util agg-edge			listMLDataTW.add(cloudMapping.getPoolReceive().size()*1.0); // current
				listMLDataTW.add(cloudMapping.getPoolDensity().size()*1.0); // current
				listMLDataTW.add(cloudMapping.getPoolReceive().size()*1.0); // current	
//				this.setTotalInsuf(0);
				if(ML) { // if prediction is requested
					resultPre = this.mapPrediction((double)timeP);
				}else {
					resultPre.clear();
					for(int i = 0; i < 3; i ++) {
						resultPre.add(0); // no prediction result
					}
				}
			}
			
			// totalReqTW is not known at the beginning of TW -- fixed, excluded from ML dataset
			for(Event event : listEvent) {
				if(event.getType() == "join")
					totalReq1h ++;
			}
			
			// start looping each event
			for(int eventIn = 0; eventIn < listEvent.size(); eventIn ++) {
				this.setDowntime(0.0);
				double insPower = 0.0;
				// getting event
				Event event = listEvent.get(eventIn);
				if(event.getType() == "join") {
					System.out.println("Event join " + event.getTime() +" at TW " + timeP);
				}else {
					// case event leave sfc that is not in the system
					if(!this.listSFCTotal.contains(event.getSfc()))
						continue;
					System.out.println("Event leave " + event.getTime() +" at TW " + timeP);
				}

				// run scheduling the event
				boolean result = false;
				result = this.runEvent(event, resultPre);
				
				if(result) { // successful
					insPower = cloudMapping.getPowerServer(topo) + linkMapping.getPower(topo) + edgeMapping.getPowerEdge(this.listSFCTotal);
					if(event.getType() == "join") {
						totalSFCaccept1h ++;

					}else { // leave
						totalReqLv1h ++;
					}
					//calculate accumulated averaging downtime
//					downtime1h += (this.downtime);
					downtime1h += (this.downtime/this.listSFCTotal.size());

//					System.out.println("Total downtime this event: " + this.downtime);
					
					// calculate energy
					this.addTotalEnergy((insPower*(event.getTime() - time4Energy)*HOUR));
					power1h += (insPower*(event.getTime() - time4Energy));
					powerWarm1h += (cloudMapping.getPowerWasted(topo)*(event.getTime() - time4Energy));
					time4Energy = event.getTime();
				
				}else { // failed
					if(event.getType() == "leave") {
						throw new java.lang.Error("Error occurs at leaving process");
					}else {
						System.out.println("System is full");
					}
				}
				
			} // loop each event
			
			
			// removing all "unassigned" VNFs to purify the system
			// the folowing data must be put at the end of odd TW
			if((TW == 2.0 && timeP % 2.0 != 0.0) || TW != 2.0) {
				for(Service service : cloudMapping.getPoolService()) {
					if(service.getStatus() == "unassigned") {
						this.totalError ++;
						totalRedun1h ++;
					}
				}
				
//				int numLeave = cloudMapping.leaveRedundant(topo);
//				this.addDelEnergy(numLeave*DELENERGY);
				
				// store label for ML dataset
				listMLDataTW.add(cloudMapping.getPoolDensity().size()*1.0); // label
				listMLDataTW.add(cloudMapping.getPoolReceive().size()*1.0); // label
				storeMLData.add(listMLDataTW);
			}
			
			
			
			// store log values in 1 hour period
			if(TW == (int) TW || timeP != (int) (timeP)) {
				// update variables after 1 hour
				power1h += ((this.getIniEnergy() + this.getDelEnergy())/HOUR);
				powerColdTerm1h += ((this.getIniEnergy() + this.getDelEnergy())/HOUR);
				// store log files
				listUsedSer.add(cloudMapping.getUsedServer(topo));
				listReceive.add(cloudMapping.getPoolReceive().size());
				listDensity.add(cloudMapping.getPoolDensity().size());
				listEnergy.add(this.getTotalEnergy()/MIL);
				listTotalPower.add(power1h/THOUS);
				listPowerColdTerm.add(powerColdTerm1h/THOUS);
				listPowerWarm.add(powerWarm1h/THOUS);
//				listPowerTW.add(this.power);
				listError.add(this.totalError);
//				listRelocate.add(this.totalRelocate);
				listColdStart.add(this.totalCold);
				listInSuf.add(this.totalInSuf);
				listRedundant.add(totalRedun1h);
				listRedirect.add(cloudMapping.getVNFmigration());
				listReqTW.add(totalReq1h);
				listReqLvTW.add(totalReqLv1h);
				listDownTime.add(downtime1h);
				// get and store number of active SFC
				listSFCActive.add(this.listSFCTotal.size()); 
				// get serverutilization
//				listAveSerUtil.add(aveSerUtil);
				// get number of service
				// store list acceptance
				listAcceptance.add(totalSFCaccept1h*1.0/totalReq1h);
				listAcceptTW.add(totalSFCaccept1h);
				// store link usage
				listLinkCore = topo.getLinkType("core-agg").stream().map(SubstrateLink::getRemainBW).collect(Collectors.toList());
				listLinkAgg = topo.getLinkType("agg-edge").stream().map(SubstrateLink::getRemainBW).collect(Collectors.toList());
//				listLinkEdge = topo.getListLinkPhyEdge().stream().map(LinkPhyEdge::getBandwidth).collect(Collectors.toList());
			}
			
		} // TW loop
		
		// print log values to txt or excel file
		try {
			String path = null;
			if(MLtype == "LSTM")
				path = "./PlotCR/" + String.valueOf(TW) + "/" + type + "/" + MLtype;
			else
				path = "./PlotCR/" + String.valueOf(TW) + "/" + type;
			
			write_integer(path + "/UsedSerCR.txt",listUsedSer);
			write_integer(path + "/ReceiveCR.txt",listReceive);
			write_integer(path + "/DensityCR.txt",listDensity);
			write_integer(path + "/ReqTWCR.txt",listReqTW);
			write_integer(path + "/ReqLvTWCR.txt",listReqLvTW);
			write_integer(path + "/ReqActiveCR.txt",listSFCActive);
			write_integer(path + "/AcceptTWCR.txt",listAcceptTW);
			write_double(path + "/ErrorCR.txt",listError);
			write_integer(path + "/InSufCR.txt",listInSuf);
			write_integer(path + "/ColdStartCR.txt",listColdStart);
			write_integer(path + "/RedundantCR.txt",listRedundant);
			write_integer(path + "/RedirectCR.txt",listRedirect);
			write_double(path + "/ServerUtilCR.txt",listAveSerUtil);
			write_double(path + "/PowerCR.txt",listTotalPower);
			write_double(path + "/PowerColdTermCR.txt",listPowerColdTerm);
			write_double(path + "/PowerWarmCR.txt",listPowerWarm);
//			write_double(path + "/ListPowerEndTWCR.txt",listPowerTW);
			write_double(path + "/EnergyCR.txt",listEnergy);
			write_double(path + "/AveAcceptanceCR.txt",listAcceptance);
			write_double(path + "/AveLinkCoreCR.txt",listLinkCore);
			write_double(path + "/AveLinkAggCR.txt",listLinkAgg);
			write_double(path + "/AveDowntimeCR.txt",listDownTime);
//			write_double("./PlotCR/TotalLinkEdgeCR.txt",listLinkEdge);

//			write_excel_double("./PlotCR/DensityCR.xlsx",storeMLData);
//			write_excel_double_result(path + "/result.xlsx", "power", listTotalPower);
//			write_excel_double_result(path + "/result.xlsx", "error", listError);
//			write_excel_double_result(path + "/result.xlsx", "downtime", listDownTime);

//			write_excel_double(path + "/" + type + ".xlsx",storeMLData);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean runEvent(Event event, LinkedList<Integer> resultPre) {
			
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
	
					boolean assignedStatus = false;
					boolean stop = false;
					
					while(!assignedStatus) {
						
						assignedStatus = true;
		
						result = cloudMapping.assignVNF(topo, result, listSFCTotal, listSFCOnRpi);
						
						if(stop) // allow system to assignVNF one more time before stopping
							break;
						// check if all VNF has been assigned or not?
						Map <SFC, LinkedList<Service>> migSFC = new HashMap<>();
						for(SFC sfc0 : result) {
							for(Service service : sfc0.getListService()) {
								if(!service.getBelongToEdge() && service.getStatus() == "unassigned") {
									assignedStatus = false;
									this.totalError ++; // one VNF is not sufficiently
									this.totalCold ++;
									this.totalInSuf ++;
//									if(result.size() > 1) // VNF is relocated from edge --> cloud
//										this.totalRelocate ++;
									if(!migSFC.containsKey(sfc0))
										migSFC.put(sfc0, new LinkedList<>());
									// simplify problem by assuming resource on cloud is infinity
									boolean success = cloudMapping.createVNF(1, service.getServiceID(), topo, true);
									if(!success) {
										System.out.println("Cannot create more VNF.");
										this.totalError --;
										this.totalCold --;
										this.totalInSuf --;
//										if(result.size() > 1) 
//											this.totalRelocate --;
										resultFailed.add(sfc0);
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
	//				for(SFC sfcF : result) {
	//					for(Service sv : sfcF.getListService())
	//						if(!sv.isBelongToEdge() && sv.getBelongToServer() == null) {
	//							resultFailed.add(sfcF);
	//							break;
	//						}
	//				}
					
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
					for(SFC sfcF : result) {
						for(Service sv : sfcF.getListService())
							if(!sv.isBelongToEdge() && sv.getBelongToServer() == null)
								System.out.println();
					}
					
					// cmt: topo should include rpi - done
					if(result.size() > 0) {
						
						// if the SFC has been filled with enough VNF then it can be
						// add to the list of total SFC. Then, these VNFs need to
						// change their status to "assigned". Number of abundant/lack VNF
						// should be able to query to calculate error of prediction. Note
						// that this action will be conducted either when VNF pool is empty
						// but requirement is not filled or when requirement is filled but
						// VNF pool is still not empty
						// double check
						
						// Link mapping block
						LinkedList<SFC> SFCLinkFailed_t = new LinkedList<SFC>();
						SFCLinkFailed_t = linkMapping.linkMapExternal(topo, result);
						resultLinkFailed.addAll(SFCLinkFailed_t);
						for(SFC sfcL : SFCLinkFailed_t) { // remove failed links
	//						System.out.println("Ext Link failed.");
							if(result.contains(sfcL))
								result.remove(sfcL);
						}
						SFCLinkFailed_t.clear();
						SFCLinkFailed_t = linkMapping.linkMapInternal(topo, result);
						resultLinkFailed.addAll(SFCLinkFailed_t);
		//				totalRejectLink += resultLinkFailed.size();
						for(SFC sfcL : SFCLinkFailed_t) { // remove failed links
	//						System.out.println("Int Link failed.");
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
					System.out.println("Edge failed.");
				}
			}else { // leave process
				LinkedList<SFC> listSFCLeave = new LinkedList<>();
				listSFCLeave.add(sfc);
			
				edgeMapping.leave(topo,piID, sfc);
				linkMapping.leave(listSFCLeave, topo);
				cloudMapping.leave(resultPre, listSFCLeave, listSFCTotal, topo);
				
				LinkedList<SFC> listSFCRpi = listSFCAllRpi.get(piID);
				listSFCRpi.remove(sfc);
				listSFCTotal.remove(sfc);
				this.addDelEnergy(sfc.getListServiceCloud().size()*DELENERGY);
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
		}
		// these servers perform migration in parallel
//		Collections.sort(listDT);
		// return longest downtime caused by services of SFC 
//		if(listDT.size() != 0)
//			downtime = listDT.getLast();
		
		return downtime;
	}
	
	
	public double getRemovalEner() {
		int reduntTW = 0;
		for(Service service : cloudMapping.getPoolService()) {
			if(service.getStatus() == "unassigned") {
				reduntTW ++;
			}
		}
		double energy = reduntTW*DELENERGY;
		return energy;
	}
	
	public double getInitEner() {
		return this.totalCold*INIENERGY;
	}
	
	//<----This process is mapped following "Detail mapping process" 
	public LinkedList<Integer> mapPrediction(double time) {
				
		LinkedList<Integer> result = new LinkedList<>();
		
		// result equals 0 when join > leave
		// result equals leave - join
			//<----Loop through all type of VNF 
			//<----0 1:vnf2 join-leave 2 3:vnf3 4 5:vnf4
			//<-----Loop through result join - leave of each type of VNF 
		for(int i = 0; i < 3; i ++) {
			if(i == 0) {
				result.add(0);
				continue;
			}
			Map<String, Double> feedData = new HashMap<>();
//			LinkedList<Double> feedData_arr = new LinkedList<>();
			// feed data to MLModel
			feedData.put("Time ", time); //time "Time " for 24h
			feedData.put("CPU edge average (1)", edgeMapping.getCPUaverageAll(topo)); //CPU edge (1)
			feedData.put("CPU edge average (2)", edgeMapping.getCPUaverageNonZero(topo)); //CPU edge (2)
			feedData.put("CPU server average", cloudMapping.getCPUServerUtil(topo)); //CPU server
			feedData.put("BW edge (1)", edgeMapping.getBWaverageAll(topo)); //BW edge (1)
			feedData.put("BW edge (1).1", edgeMapping.getBWaverageNonZero(topo)); //BW edge (2) BW edge (1).1 for 24h
			feedData.put("BW server (1)", topo.getLinkUtil("core", "agg")); //BW server (1)
			feedData.put("BW server (2)", topo.getLinkUtil("agg", "edge")); //BW server (2)
			feedData.put("# edge device", edgeMapping.getNonZeroDevice(topo)); //#edge device
			feedData.put("# links (1)", topo.getNonZeroLink("core", "agg")); //#link (1)
			feedData.put("# links (2)",topo.getNonZeroLink("agg", "edge"));  //#link (2)
			

			if(i == 1) {
				double numDenCur = (double) this.cloudMapping.getPoolDensity().size();
				feedData.put("# VNF Density current", numDenCur); //#VNF current
			} else if(i == 2) {
				double numRecCur = (double) this.cloudMapping.getPoolReceive().size();
				feedData.put("# VNF Receive current", numRecCur); //#VNF current
			} else {
				throw new java.lang.Error("abc");
			}
			// feed data to MLmodel
			
			double presult = 0;
			int compare = 0;
			switch(i) {
			case 1:
				presult = this.denModel.predict(feedData);
				this.listMLResultRec.add((int) presult);
				compare = (int) presult - this.cloudMapping.getPoolDensity().size();
				break;
			case 2:
				presult = this.recModel.predict(feedData);
				this.listMLResultDen.add((int) presult);
				compare = (int) presult - this.cloudMapping.getPoolReceive().size();
				break;
			}
			if(compare < 0) {
				// if leave > join then: leave redundant CNF belongs to the previous TW, if it's still not enough then let CNF leave when they request
				int numLeave = cloudMapping.leaveRedundant(-compare, i+2, topo);
				this.addDelEnergy(numLeave*DELENERGY);
				result.add(numLeave - (-compare)); // turn on prediction // left over must-leave CNF
			}
			else if(compare > 0) {
				result.add(0);
				boolean mapResult = cloudMapping.createVNF((int) compare, i+2, topo, true); // true indicates mapping with reserved order
				if(mapResult) {
					this.addTotalCold(compare);
					this.addIniEnergy(compare*INIENERGY);
					this.addTotalEnergy(compare*INIENERGY);
				}
			}
			else {
				result.add(0);
				//<---Num join = num leave, temporarily do nothing 
			}
		}
			
		
		
		return result;
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

	public static void write_excel_double (String filename, LinkedList<LinkedList<Double>> data) throws IOException { //write result to file
		 File excelFile = new File(filename);
		  FileInputStream fis = new FileInputStream(excelFile);
		  //Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook(fis);
	      
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.getSheetAt(0);

	      //Create row object
	      XSSFRow row;
	      
	      int rowid = spreadsheet.getLastRowNum();
	      
	      for(LinkedList<Double> objectArr : data) {
	    	  row = spreadsheet.createRow(++rowid);
		         int cellid = 0;
		         for (Object obj : objectArr){
		            Cell cell = row.createCell(cellid++);
		            cell.setCellValue(obj.toString());
		         }
	      }
	      //Write the workbook in file system
	      fis.close();
		  FileOutputStream out = new FileOutputStream(new File(filename));
	      workbook.write(out);
	      out.close();
	      workbook.close();
	}
	
	public static void write_excel_double_result (String filename, String result, LinkedList<Double> data) throws IOException { //write result to file
		 File excelFile = new File(filename);
		  FileInputStream fis = new FileInputStream(excelFile);
		  //Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook(fis);
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.getSheet(result);
	      //Create row object
	      XSSFRow row; 
	      int rowid = spreadsheet.getLastRowNum();
    	  row = spreadsheet.createRow(++rowid);
	         int cellid = 0;
	         for (Object obj : data){
	            Cell cell = row.createCell(cellid++);
	            cell.setCellValue(obj.toString());
	         }
	      //Write the workbook in file system
	      fis.close();
		  FileOutputStream out = new FileOutputStream(new File(filename));
	      workbook.write(out);
	      out.close();
	      workbook.close();
	}
	
	public static void write_double (String filename, List<Double> x) throws IOException { //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
 		for (int i = 0; i < x.size(); i++) {
			outputWriter.write(Double.toString(x.get(i)));
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
		  File excelFile = new File(filename);
		  FileInputStream fis = new FileInputStream(excelFile);
		  //Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook(fis);
	      
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.getSheetAt(0);

	      //Create row object
	      XSSFRow row;
	      
	      Set <Integer> keyid = map.keySet();
	      int rowid = spreadsheet.getLastRowNum();
	      
	      for (Integer key : keyid) {
	         row = spreadsheet.createRow(++rowid);
	         LinkedList<Double> objectArr = map.get(key);
	         int cellid = 0;
	         
	         for (Object obj : objectArr){
	            Cell cell = row.createCell(cellid++);
	            cell.setCellValue(obj.toString());
	         }
	      }
	      //Write the workbook in file system
	      fis.close();
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

	public void setDenModel(Others denModel) {
		this.denModel = denModel;
	}

	public void setRecModel(Others recModel) {
		this.recModel = recModel;
	}



	public int getTotalCold() {
		return totalCold;
	}



	public void setTotalCold(int totalCold) {
		this.totalCold = totalCold;
	}

	public void addTotalCold(int totalCold) {
		this.totalCold += totalCold;
	}



	public double getDowntime() {
		return downtime;
	}



	public void setDowntime(double downtime) {
		this.downtime = downtime;
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

	public double getIniEnergy() {
		return iniEnergy;
	}



	public void setIniEnergy(double iniEnergy) {
		this.iniEnergy = iniEnergy;
	}
	
	public void addIniEnergy(double iniEnergy) {
		this.iniEnergy += iniEnergy;
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

}
