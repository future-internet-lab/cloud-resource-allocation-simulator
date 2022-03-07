/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.FullRemap;

import java.util.LinkedList;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.*;


public class EdgeMapping  {
	

	private LinkedList<Rpi> listRpi;
	private LinkedList<Integer> edgePosition;

	public EdgeMapping() { // default constructor
		
		

	}
		
	
	
	public boolean join (SFC sfc, LinkedList<SFC> listSFCOnRpi, Rpi pi){
		
		boolean result = false;
		LinkedList<SFC> listSFCOnRpi_temp = new LinkedList<>();
		
		if(listSFCOnRpi.size() == 0) {
			return true;
		}
		
//		sfc.setServicePosition("capture", true);
//		sfc.setServicePosition("receive", false);
		
			
		listSFCOnRpi_temp.clear();
		listSFCOnRpi_temp.addAll(listSFCOnRpi);
//		listSFCOnRpi_temp.add(sfc);
//		pi.reset(); //reset RPI
		
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
//				System.out.println("bwdemand sfc " + sfc + " is " + bwDemand);
//				System.out.println("bw current in pi is " + pi.getUsedBandwidth());
				double cpuDemand = numSFC*sfc.getCapture().getCpu_pi() + 
						(numSFC - offDecode)*sfc.getDecode().getCpu_pi() + 
						(numSFC - offDensity)*sfc.getDensity().getCpu_pi();
//				System.out.println("cpudemand sfc " + sfc + " is " + cpuDemand);
//				System.out.println("cpu current in pi is " + pi.getUsedCPU());
				if(bwDemand > pi.getRemainBandwidth()) {
					result = false;
					break OFFDECODE;
					
				}else if(cpuDemand > pi.getRemainCPU()){
						continue; // try to offload service
				}
				else {
					// set cpu, bw for Rpi
					pi.setUsedCPU(cpuDemand); // change CPU pi-server
					pi.setUsedBandwidth(bwDemand); //change Bandwidth used by Pi
					result = true;
					break OFFDECODE;
//						if(result.size() >=2 && result.get(0).equals(result.get(1)))
//							System.out.println("abcdef");
				}
			} // density
		} // decode
			

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
			}
		}

		topology.getListRPi().get(piID).setUsedCPU(-cpuSFC);
		topology.getListRPi().get(piID).setUsedBandwidth(-bwSFC);		
				
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