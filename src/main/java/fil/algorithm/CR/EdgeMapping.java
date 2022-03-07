/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.CR;

import java.util.LinkedList;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.*;


public class EdgeMapping  {
	

	private LinkedList<Rpi> listRpi;
	private LinkedList<Integer> edgePosition;

	public EdgeMapping() { // default constructor
		
		

	}
		
	
	
	public LinkedList<SFC> join (Topology topology, SFC sfc, LinkedList<SFC> listSFCOnRpi, LinkMapping linkMapping){
		
		int piID = sfc.getPiBelong();
		LinkedList<SFC> result = new LinkedList<>();
		LinkedList<SFC> listSFCOnRpi_temp = new LinkedList<>();
		Rpi pi = topology.getListRPi().get(piID);
		
		if(listSFCOnRpi.size() == 7) {
			System.out.println("Pi reaches maximum SFCs");
			return result;
		}
		
		sfc.setServicePosition("capture", true);
		sfc.setServicePosition("receive", false);
		
		boolean remap = false;
		int count = 0;
		WHILE:
		while(true) {
			count ++ ;
			if(remap == true) {
				listSFCOnRpi_temp.clear();
				listSFCOnRpi_temp.addAll(listSFCOnRpi);
				listSFCOnRpi_temp.add(sfc);
				pi.reset(); //reset RPI
				linkMapping.leave(listSFCOnRpi, topology);
				
			}else {
				listSFCOnRpi_temp.add(sfc);
			}
//			if(listSFCOnRpi_temp.size() == 2)
//				System.out.println("abcdef");
			
			int numSFC = listSFCOnRpi_temp.size();
			int offDecode = 0;
			int offDensity = 0;
			OFFDECODE:
			for (offDecode = 0; offDecode <= numSFC; offDecode++) {
				for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numSFC; offDensity++) {
					
					int numOffDecode = 0;
					int numOffDensity = 0;
					for(SFC sfc_temp : listSFCOnRpi_temp) {
						if(numOffDecode < offDecode) {
							sfc_temp.setServicePosition("decode", false);
						}
						else 
							sfc_temp.setServicePosition("decode", true);
						if(numOffDensity < offDensity) {
							sfc_temp.setServicePosition("density", false);
						}
						else 
							sfc_temp.setServicePosition("density", true);
		
						numOffDecode++;
						numOffDensity++;
						
					}
			
					double bwDemand = (offDecode)*sfc.getCapture().getBandwidth() + 
							(offDensity - offDecode)*sfc.getDecode().getBandwidth() + 
							(numSFC - offDensity)*sfc.getDensity().getBandwidth();
//					System.out.println("bwdemand sfc " + sfc + " is " + bwDemand);
//					System.out.println("bw current in pi is " + pi.getUsedBandwidth());
					double cpuDemand = numSFC*sfc.getCapture().getCpu_pi() + 
							(numSFC - offDecode)*sfc.getDecode().getCpu_pi() + 
							(numSFC - offDensity)*sfc.getDensity().getCpu_pi();
//					System.out.println("cpudemand sfc " + sfc + " is " + cpuDemand);
//					System.out.println("cpu current in pi is " + pi.getUsedCPU());
					if(bwDemand > pi.getRemainBandwidth()) {
						if(remap == true) {
							if(offDecode == offDensity && offDensity == numSFC)
								break WHILE; // break while loop
							else
								continue;
						} 
						else{
							remap = true; // conduct remapping
							break OFFDECODE;
						}
					}else if(cpuDemand > pi.getRemainCPU()){
						if (offDecode == numSFC && remap == false) {
							remap = true; //turn on remapping Pi
							break OFFDECODE;
						} else
							continue; // try to offload service
					}
					else {
						// set cpu, bw for Rpi
						pi.setUsedCPU(cpuDemand); // change CPU pi-server
						pi.setUsedBandwidth(bwDemand); //change Bandwidth used by Pi
						// set status for VNF at edge
						for(SFC sfc_t : listSFCOnRpi_temp) {
//							if(!sfc_t.getDecode().isBelongToEdge() && sfc_t.getDensity().isBelongToEdge())
//								System.out.println("abcdef");
							for(Service ser : sfc_t.getListService()) {
								if(ser.isBelongToEdge()) {
									ser.setStatus("assigned");
//									ser.setBelongToServer(null);
								}
								else {
									ser.setStatus("unassigned");
								}
							}
						}
						result.addAll(listSFCOnRpi_temp);
//						if(result.size() >=2 && result.get(0).equals(result.get(1)))
//							System.out.println("abcdef");
						break WHILE;
					}
				} // density
			} // decode
			
//			if(result.size() == 0 && remap != true)
//				remap = true;
//			else
//				break WHILE;
		} // while
		if(result.size() == 0)
			System.out.println("abcdef");
		return result;
	}
	
	
	public LinkedList<SFC> remap (Topology topology, SFC sfc, LinkedList<SFC> listSFCOnRpi, LinkMapping linkMapping){
		
		int piID = sfc.getPiBelong();
		LinkedList<SFC> result = new LinkedList<>();
		LinkedList<SFC> listSFCOnRpi_temp = new LinkedList<>();
		Rpi pi = topology.getListRPi().get(piID);
		
		if(listSFCOnRpi.size() == 7) {
			System.out.println("Pi reaches maximum SFCs");
			return result;
		}
		
		sfc.setServicePosition("capture", true);
		sfc.setServicePosition("receive", false);
		
		boolean remap = false;
		int count = 0;
		WHILE:
		while(true) {
			count ++ ;
			if(remap == true) {
				listSFCOnRpi_temp.clear();
				listSFCOnRpi_temp.addAll(listSFCOnRpi);
				listSFCOnRpi_temp.add(sfc);
				pi.reset(); //reset RPI
				linkMapping.leave(listSFCOnRpi, topology);
				
			}else {
				listSFCOnRpi_temp.add(sfc);
			}
//			if(listSFCOnRpi_temp.size() == 2)
//				System.out.println("abcdef");
			
			int numSFC = listSFCOnRpi_temp.size();
			int offDecode = 0;
			int offDensity = 0;
			OFFDECODE:
			for (offDecode = 0; offDecode <= numSFC; offDecode++) {
				for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numSFC; offDensity++) {
					
					int numOffDecode = 0;
					int numOffDensity = 0;
					for(SFC sfc_temp : listSFCOnRpi_temp) {
						if(numOffDecode < offDecode) {
							sfc_temp.setServicePosition("decode", false);
						}
						else 
							sfc_temp.setServicePosition("decode", true);
						if(numOffDensity < offDensity) {
							sfc_temp.setServicePosition("density", false);
						}
						else 
							sfc_temp.setServicePosition("density", true);
		
						numOffDecode++;
						numOffDensity++;
						
					}
			
					double bwDemand = (offDecode)*sfc.getCapture().getBandwidth() + 
							(offDensity - offDecode)*sfc.getDecode().getBandwidth() + 
							(numSFC - offDensity)*sfc.getDensity().getBandwidth();
					System.out.println("bwdemand sfc " + sfc + " is " + bwDemand);
					System.out.println("bw current in pi is " + pi.getUsedBandwidth());
					double cpuDemand = numSFC*sfc.getCapture().getCpu_pi() + 
							(numSFC - offDecode)*sfc.getDecode().getCpu_pi() + 
							(numSFC - offDensity)*sfc.getDensity().getCpu_pi();
					System.out.println("cpudemand sfc " + sfc + " is " + cpuDemand);
					System.out.println("cpu current in pi is " + pi.getUsedCPU());
					if(bwDemand > pi.getRemainBandwidth()) {
						if(remap == true) {
							if(offDecode == offDensity && offDensity == numSFC)
								break WHILE; // break while loop
							else
								continue;
						} 
						else{
							remap = true; // conduct remapping
							break OFFDECODE;
						}
					}else if(cpuDemand > pi.getRemainCPU()){
						if (offDecode == numSFC && remap == false) {
							remap = true; //turn on remapping Pi
							break OFFDECODE;
						} else
							continue; // try to offload service
					}
					else {
						// set cpu, bw for Rpi
						pi.setUsedCPU(cpuDemand); // change CPU pi-server
						pi.setUsedBandwidth(bwDemand); //change Bandwidth used by Pi
						// set status for VNF at edge
						for(SFC sfc_t : listSFCOnRpi_temp) {
//							if(!sfc_t.getDecode().isBelongToEdge() && sfc_t.getDensity().isBelongToEdge())
//								System.out.println("abcdef");
							for(Service ser : sfc_t.getListService()) {
								if(ser.isBelongToEdge()) {
									ser.setStatus("assigned");
//									ser.setBelongToServer(null);
								}
								else {
									ser.setStatus("unassigned");
								}
							}
						}
						result.addAll(listSFCOnRpi_temp);
//						if(result.size() >=2 && result.get(0).equals(result.get(1)))
//							System.out.println("abcdef");
						break WHILE;
					}
				} // density
			} // decode
			
//			if(result.size() == 0 && remap != true)
//				remap = true;
//			else
//				break WHILE;
		} // while
		if(result.size() == 0)
			System.out.println("abcdef");
		return result;
	}

	
	public void leave (Topology topology, int piID, SFC sfc) {
//		System.out.println("Start leaving process ....");
		
		// return CPU for RPi
		double cpuSFC = 0;
		double bwSFC = 0;
		for(int i = 1; i <= 4; i ++ ) {
			if(sfc.getService(i).isBelongToEdge()) {
				cpuSFC += sfc.getService(i).getCpu_pi();
			}else {
				bwSFC = sfc.getService(i - 1).getBandwidth();
				break;
			}
		}
		Rpi pi = topology.getListRPi().get(piID);
		pi.setUsedCPU(-cpuSFC);
		if(pi.getUsedCPU() > 100 || pi.getUsedBandwidth() > 100)
			throw new java.lang.Error();
		pi.setUsedBandwidth(-bwSFC);		
				
	}
	
	public double getCPUaverageAll(Topology topo) {
		double cpu = 0.0;
		LinkedList<Rpi> listRpi = topo.getListRPi();
		int size = listRpi.size();
		for(Rpi pi : listRpi) {
			cpu += pi.getUsedCPU();
		}
		if(size == 0)
			return 0.0;
		return cpu/size*1.0;
	}
	
	public double getCPUaverageNonZero(Topology topo) {
		double cpu = 0.0;
		int size = 0;

		LinkedList<Rpi> listRpi = topo.getListRPi();
		for(Rpi pi : listRpi) {
			if(pi.getUsedCPU() > 0) {
				cpu += pi.getUsedCPU();
				size ++;
			}
		}
		if(size == 0)
			return 0.0;
		return cpu/size*1.0;
	}
	
	public double getNonZeroDevice(Topology topo) {
		double count = 0;
		LinkedList<Rpi> listRpi = topo.getListRPi();
		for(Rpi pi : listRpi) {
			if(pi.getUsedCPU() > 0)
				count ++;
		}
		return count*1.0;
	}
	
	public double getZeroDevice(Topology topo) {
		double count = 0;
		LinkedList<Rpi> listRpi = topo.getListRPi();
		for(Rpi pi : listRpi) {
			if(pi.getUsedCPU() == 0)
				count ++;
		}
		return count*1.0;
	}
	
	public double getBWaverageAll(Topology topo) {
		double bw = 0.0;
		LinkedList<Rpi> listRpi = topo.getListRPi();
		int size = listRpi.size();
		for(Rpi pi : listRpi) {
			bw += pi.getUsedBandwidth() ;
		}
		if(size == 0)
			return 0.0;
		return bw/size*1.0;
	}
	
	public double getBWaverageNonZero(Topology topo) {
		double bw = 0.0;
		int size = 0;

		LinkedList<Rpi> listRpi = topo.getListRPi();
		for(Rpi pi : listRpi) {
			if(pi.getUsedBandwidth() > 0) {
				bw += pi.getUsedBandwidth();
				size ++;
			}
		}
		if(size == 0)
			return 0.0;
		return bw/size*1.0;
	}
	
	public double getPowerEdge(LinkedList<SFC> listSFCTotal) {
		double power = 0.0;
		double numPi = 300;
		for(SFC sfc : listSFCTotal) {
			power += sfc.powerEdgeUsage();
		}
		return power + numPi*1.28;
	}
	public LinkedList<Rpi> getListRpi() {
		return listRpi;
	}


	public void setListRpi(LinkedList<Rpi> listRpi) {
		this.listRpi = listRpi;
	}


	public LinkedList<Integer> getEdgePosition() {
		return edgePosition;
	}


	public void setEdgePosition(LinkedList<Integer> edgePosition) {
		this.edgePosition = edgePosition;
	}

	
}