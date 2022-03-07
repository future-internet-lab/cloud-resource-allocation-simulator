/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.REAP;


import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import fil.resource.substrate.*;
import fil.resource.virtual.*;

public class LinkMapping {

	private int numLinkSuccess;
	private double powerConsumed;
	
	public LinkMapping() {
	//	listVirLink = new LinkedList<>();
		powerConsumed = 0;
		numLinkSuccess= 0;
	}

	public LinkedList<SFC> linkMapExternal(Topology topo, LinkedList<SFC> listSFC) {
		
		LinkedList<SFC> listFailedSFC = new LinkedList<>();
		
		LinkedList<SubstrateLink> listPhyLinks = topo.getlistPhyLinks();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAggMap = topo.getListCoreConnectAgg();	
		
		for(SFC sfc : listSFC) { // get edge switch connect to server
			
			SubstrateSwitch substrateAggr = null;
			SubstrateSwitch substrateCore = null;
			SubstrateSwitch substrateEdge = null;
			
			SubstrateLink edgeToAggr = null;
			SubstrateLink aggrToEdge = null;
			SubstrateLink coreToAggr = null;
			SubstrateLink aggrToCore = null;
			SubstrateLink edgeToServer = null;
			SubstrateLink serverToEdge = null;

			boolean checkEdge = false;
			boolean checkAgg = false;
			boolean checkCore = false;
			
			Service service = sfc.getFirstServiceCloud();
			PhysicalServer server = service.getBelongToServer();
			double bwDemand = sfc.getLastServiceEdge().getBandwidth();
			int countLink = 0;

//			for(SubstrateLink linkEdge : listPhyLinks) {
//				if(linkEdge.getStart().equals(server) && linkEdge.getRemainBW() >= bwDemand) { 
//					serverToEdge = linkEdge;
//					substrateEdge = (SubstrateSwitch) linkEdge.getEnd();
//					countLink ++;
//				} else if(linkEdge.getEnd().equals(server) && linkEdge.getRemainBW() >= bwDemand) {
//					edgeToServer = linkEdge;
//					substrateEdge = (SubstrateSwitch) linkEdge.getStart();
//					countLink ++;
//				} else {
//					;
//				}
//				
//				if(countLink == 2) {
//					checkEdge = true;
//					break;
//				}
//				if(linkEdge.equals(listPhyLinks.getLast()) && countLink != 2) {
//					checkEdge = false;
//					break;
//				}
//			}
			
			for(SubstrateLink linkEdge : listPhyLinks) {
				if(linkEdge.getStart().equals(server)) { 
					serverToEdge = linkEdge;
					substrateEdge = (SubstrateSwitch) linkEdge.getEnd();
					if(linkEdge.getRemainBW() >= bwDemand)
						countLink ++;
					else
						System.out.println("");
				} else if(linkEdge.getEnd().equals(server)) {
					edgeToServer = linkEdge;
					substrateEdge = (SubstrateSwitch) linkEdge.getStart();
					if(linkEdge.getRemainBW() >= bwDemand)
						countLink ++;
					else
						System.out.println("");
				} else {
					;
				}
				
				if(countLink == 2) {
					checkEdge = true;
					break;
				}
				if(linkEdge.equals(listPhyLinks.getLast()) && countLink != 2) {
					checkEdge = false;
					break;
				}
			}
			
			if(!checkEdge) {
				if(serverToEdge == null || edgeToServer == null)
					throw new java.lang.Error();
				System.out.println("Ext Link failed at PhyEdge.");
				listFailedSFC.add(sfc);
				continue;
			}
			
			//find aggregation switch
			LinkedList<SubstrateSwitch> listAgg = sortListSwitch(listAggConnectEdge.get(substrateEdge));
			
			FIND_AGG_SW:
			for(SubstrateSwitch aggSwitch : listAgg) { // find in MST first
				countLink = 0;
				for(SubstrateLink link : listPhyLinks) {
					if(link.getStart().equals(aggSwitch) && link.getEnd().equals(substrateEdge) && link.getRemainBW() >= bwDemand) {
						substrateAggr = aggSwitch;
						aggrToEdge = link;
						countLink ++;
					} else if(link.getStart().equals(substrateEdge) && link.getEnd().equals(aggSwitch) && link.getRemainBW() >= bwDemand) {
						substrateAggr = aggSwitch;
						edgeToAggr = link;
						countLink ++;
					}
					if(countLink == 2) {
						checkAgg = true;
						break;
					}
					if(link.equals(listPhyLinks.getLast()) && countLink != 2) {
						checkAgg = false;
						break;
					}
				}
				
				if(checkAgg == true) {
					LinkedList<SubstrateSwitch> listCore = sortListSwitch(listCoreConnectAggMap.get(substrateAggr));
					
					for(SubstrateSwitch coreSwitch : listCore) { //error here, find out edge switch again
//						if(coreSwitch.equals(substrateEdge))
//							continue;
						countLink = 0;
						checkCore = false;
						for(SubstrateLink link : listPhyLinks) {
							if(link.getStart().equals(substrateAggr) && link.getEnd().equals(coreSwitch)) {
								if(link.getRemainBW() >= bwDemand) {
									substrateCore = coreSwitch;
									aggrToCore = link;
									countLink ++;
								}
							} 
							
							if(link.getStart().equals(coreSwitch) && link.getEnd().equals(substrateAggr)) {
								if(link.getRemainBW() >= bwDemand) {
									substrateCore = coreSwitch;
									coreToAggr = link;
									countLink ++;
								}
							}
							if(countLink == 2) {
								checkCore = true;
								break;
							}
							if(link.equals(listPhyLinks.getLast()) && countLink != 2) {
								checkCore = false;
								break;
							}
						}
						
						if(checkCore == true) {
							VirtualLink vLink = new VirtualLink();
							edgeToServer.setUsedBW(bwDemand);
							serverToEdge.setUsedBW(bwDemand);
							edgeToAggr.setUsedBW(bwDemand);
							aggrToEdge.setUsedBW(bwDemand);
							aggrToCore.setUsedBW(bwDemand);
							coreToAggr.setUsedBW(bwDemand);
							substrateAggr.setPort(substrateEdge, bwDemand);
							substrateEdge.setPort(substrateAggr, bwDemand);
							substrateAggr.setPort(substrateCore, bwDemand);
							substrateCore.setPort(substrateAggr, bwDemand);
							vLink.getLinkSubstrate().add(serverToEdge);
							vLink.getLinkSubstrate().add(edgeToServer);
							vLink.getLinkSubstrate().add(edgeToAggr);
							vLink.getLinkSubstrate().add(aggrToEdge);
							vLink.getLinkSubstrate().add(aggrToCore);
							vLink.getLinkSubstrate().add(coreToAggr);
							vLink.setBWRequest(bwDemand);
							sfc.getvLink().add(vLink);
							break FIND_AGG_SW;
						}
						else { // checkCore fails
							if(aggSwitch.equals(listAgg.getLast())) {
								System.out.println("Ext Link failed at COre.");
								listFailedSFC.add(sfc);
								break FIND_AGG_SW;
							}
							else
								;
						}
					} // core loop
					
				} 
				else { // checkAggr fails
					if(aggSwitch.equals(listAgg.getLast())) {
						System.out.println("Ext Link failed at Agg.");
						listFailedSFC.add(sfc);
						break FIND_AGG_SW;
					}
					else
						continue;
				}
				
			}// agg loop
			

		} // end for loop SFC
		return listFailedSFC;
		
	}
	
	public LinkedList<SFC> linkMapInternal(Topology topo, LinkedList<SFC> listSFC) {

		Service serviceA = new Service();
		Service serviceB = new Service();
		
		LinkedList<SFC> listFailedSFC = new LinkedList<>();

		
		// block below examines bandwidth, servers between two services
		for(SFC sfc : listSFC) {
			
			Service service = sfc.getFirstServiceCloud();
			double bandwidth = 0;
			boolean subResult = false;
			
			if(service.getServiceType() == "density") {
				if(!sfc.getService(3).getBelongToServer().equals(sfc.getService(4).getBelongToServer())) {
					bandwidth = sfc.getService(3).getBandwidth();
					serviceA = sfc.getService(3);
					serviceB = sfc.getService(4);
					VirtualLink vLink = new VirtualLink();
					vLink = new VirtualLink(serviceA, serviceB, bandwidth);
					System.out.println("Internal density block");
					subResult = linkMappingSeparate(topo, vLink);
					if (subResult == true)
						sfc.getvLink().add(vLink);
					else
						listFailedSFC.add(sfc);
				}else {
					subResult = true;
				}
			} else if(service.getServiceType() == "decode") {
				
				boolean subResult1 = false;
				boolean subResult2 = false;
				VirtualLink vLink1 = null;
				VirtualLink vLink2 = null;
				System.out.println("Internal decode block");
				if(!sfc.getService(2).getBelongToServer().equals(sfc.getService(3).getBelongToServer())) {
					bandwidth = sfc.getService(2).getBandwidth();
					serviceA = sfc.getService(2);
					serviceB = sfc.getService(3);	
					vLink1 = new VirtualLink(serviceA, serviceB, bandwidth);
					subResult1 = linkMappingSeparate(topo, vLink1);
				}else
					subResult1 = true;
				
				if(!sfc.getService(3).getBelongToServer().equals(sfc.getService(4).getBelongToServer())) {
					bandwidth = sfc.getService(3).getBandwidth();
					serviceA = sfc.getService(3);
					serviceB = sfc.getService(4);
					vLink2 = new VirtualLink(serviceA, serviceB, bandwidth);
					subResult2 = linkMappingSeparate(topo, vLink2);
				}else
					subResult2 = true;
				
				subResult = subResult1 & subResult2;
				if(subResult == true) {
					if(vLink1 != null)
						sfc.getvLink().add(vLink1);
					if(vLink2 != null)
						sfc.getvLink().add(vLink2);
				}
				else
					listFailedSFC.add(sfc);
			} else {
				continue;
			}
		}
		return listFailedSFC;
	}
	
	private boolean linkMappingSeparate(Topology topo, VirtualLink vLink) {
		
		boolean result = false;
		
		LinkedList<SubstrateLink> listPhyEdge = topo.getlistPhyLinks();

		SubstrateSwitch edgeSwitch1 = new SubstrateSwitch();
		SubstrateSwitch edgeSwitch2 = new SubstrateSwitch();
		
		PhysicalServer phy1 = vLink.getsService().getBelongToServer();
		PhysicalServer phy2 = vLink.getdService().getBelongToServer();
		
		SubstrateLink phy2Edge1 = null, phy2Edge2 = null;
		SubstrateLink edge2Phy1 = null, edge2Phy2 = null;

		
		int countP2E = 0;
		double bandwidth = vLink.getBWRequest();
		
		for(SubstrateLink linkPhyEdge : listPhyEdge) {
			if(linkPhyEdge.getStart().equals(phy1)) {
				edgeSwitch1 = (SubstrateSwitch) linkPhyEdge.getEnd();
				phy2Edge1 = linkPhyEdge;
				countP2E++;
			} else if(linkPhyEdge.getEnd().equals(phy1)) {
				edgeSwitch1 = (SubstrateSwitch) linkPhyEdge.getStart();
				edge2Phy1 = linkPhyEdge;
				countP2E++;
			} else if (linkPhyEdge.getStart().equals(phy2)) {
				edgeSwitch2 = (SubstrateSwitch) linkPhyEdge.getEnd();
				phy2Edge2 = linkPhyEdge;
				countP2E++;
			} else if (linkPhyEdge.getEnd().equals(phy2)) {
				edgeSwitch2 = (SubstrateSwitch) linkPhyEdge.getStart();
				edge2Phy2 = linkPhyEdge;
				countP2E++;
			} else {
				;
			}

			if(countP2E == 4) {
				break;
			}
		}
		
		if (countP2E != 4) { // for testing only
			throw new java.lang.Error();
		}
		
		if(phy2Edge1.getRemainBW() <= bandwidth || phy2Edge2.getRemainBW() <= bandwidth) {
				System.out.println("Int Link failed at Edge.");
				return false;	
		}
		
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		LinkedList<SubstrateSwitch> listAggConnectStartEdge = new LinkedList<>();
		LinkedList<SubstrateSwitch> listAggConnectEndEdge = new LinkedList<>();
		
		// near groups
		if (edgeSwitch1.equals(edgeSwitch2)) {
			///////////////////////////////////////////////////////
//			LinkedList<SubstrateLink> linkEdge = new LinkedList<>();
			phy2Edge1.setUsedBW(bandwidth);
			edge2Phy1.setUsedBW(bandwidth);
			phy2Edge2.setUsedBW(bandwidth);
			edge2Phy2.setUsedBW(bandwidth);
			vLink.getLinkSubstrate().add(phy2Edge1);
			vLink.getLinkSubstrate().add(edge2Phy1);
			vLink.getLinkSubstrate().add(phy2Edge2);
			vLink.getLinkSubstrate().add(edge2Phy2);
//			vLink.getLinkSubstrate().addAll(linkEdge); // Kien add 1912
			//edgeSwitch1.setPort(edgeSwitch1, bandwidth);
			//edgeSwitch2.setPort(edgeSwitch2, bandwidth);
			result = true;
		} else {
			//check if aggregation or core
			int count = 0;
			for (Entry<SubstrateSwitch, LinkedList<SubstrateSwitch>> entry : listAggConnectEdge.entrySet()) {
				if (entry.getKey().equals(edgeSwitch1)) {
					listAggConnectStartEdge = entry.getValue();
					count++;
				}
					
				if (entry.getKey().equals(edgeSwitch2)) {
					listAggConnectEndEdge = entry.getValue();
					count++;
				}
				
				if(count == 2) {
					break;
				}
			}
			// sort list Agg
			listAggConnectStartEdge = sortListSwitch(listAggConnectStartEdge);
			listAggConnectEndEdge = sortListSwitch(listAggConnectEndEdge);

			// check middle groups
			if (listAggConnectStartEdge.equals(listAggConnectEndEdge)) {
					result = linkMappingAggSeparate(vLink, edgeSwitch1, edgeSwitch2, listAggConnectStartEdge, topo);

			} else {
					result = linkMappingCoreSeparate(vLink, edgeSwitch1, edgeSwitch2, topo);
			}
		} // end creating link between two server - Kien 17/12/2019
		return result;
	}
	
	private boolean linkMappingAggSeparate(VirtualLink vLink, SubstrateSwitch edgeSwitch1, SubstrateSwitch edgeSwitch2, 
			LinkedList<SubstrateSwitch> listAggConnectStartEdge, Topology topo) {
		// TODO Auto-generated method stub
		boolean success = false;
		boolean checkAgg = false;
		boolean checkEdge = false;
		double bandwidth = vLink.getBWRequest();
		
		LinkedList<SubstrateLink> listPhyLinks = topo.getlistPhyLinks();
		
		PhysicalServer phy1 = vLink.getsService().getBelongToServer();
		PhysicalServer phy2 = vLink.getdService().getBelongToServer();
		
		SubstrateLink linkPhyEdge01 = null;
		SubstrateLink linkPhyEdge10 = null;
		SubstrateLink linkPhyEdge02 = null;
		SubstrateLink linkPhyEdge20 = null;
		
		SubstrateLink linkAggEdge01 = null;
		SubstrateLink linkAggEdge10 = null;
		SubstrateLink linkAggEdge02 = null;
		SubstrateLink linkAggEdge20 = null;
		
		SubstrateSwitch aggSW = new SubstrateSwitch();

		int count = 0; 
		
		AGG_LOOP:
		for(SubstrateSwitch sw : listAggConnectStartEdge) {
			count = 0;
			for(SubstrateLink link : listPhyLinks) {
				if(link.getStart().equals(sw) && link.getEnd().equals(edgeSwitch1) && link.getRemainBW() >= bandwidth) {
					count++;
					linkAggEdge01 = link;
				} else if(link.getStart().equals(edgeSwitch1) && link.getEnd().equals(sw) && link.getRemainBW() >= bandwidth) {
					count++;
					linkAggEdge10 = link;
				} else if(link.getStart().equals(sw) && link.getEnd().equals(edgeSwitch2) && link.getRemainBW() >= bandwidth) {
					count++;
					linkAggEdge02 = link;
				} else if(link.getStart().equals(edgeSwitch2) && link.getEnd().equals(sw) && link.getRemainBW() >= bandwidth) {
					count++;
					linkAggEdge20 = link;
				} else {
					;
				}
				if(count == 4) {
					aggSW = sw;
					checkAgg = true;
					break AGG_LOOP;
				} else {
					;
				}
			} // link loop
		} // agg switch loop
		
		count = 0;
		
		for(SubstrateLink link : listPhyLinks) {
			if(link.getStart().equals(edgeSwitch1) && link.getEnd().equals(phy1) && link.getRemainBW() >= bandwidth) {
				linkPhyEdge01 = link;
				count++;
			} else if(link.getEnd().equals(edgeSwitch1) && link.getStart().equals(phy1) && link.getRemainBW() >= bandwidth) {
				linkPhyEdge10 = link;
				count++;
			} else if(link.getStart().equals(edgeSwitch2) && link.getEnd().equals(phy2) && link.getRemainBW() >= bandwidth) {
				linkPhyEdge02 = link;
				count++;
			} else if(link.getEnd().equals(edgeSwitch2) && link.getStart().equals(phy2) && link.getRemainBW() >= bandwidth) {
				linkPhyEdge20 = link;
				count++;
			} else {
				;
			}
			
			if(count == 4) {
				checkEdge = true;
				break;
			}
		}
		
		if(checkAgg == true && checkEdge == true) {
			
			success = true;
			
			LinkedList<SubstrateSwitch> listSWUsed = topo.getListSwitchUsed();
			boolean checkContain = false;
			for(SubstrateSwitch sw : listSWUsed) {
				if(sw.getNameSubstrateSwitch().equals(aggSW.getNameSubstrateSwitch())) {
					checkContain = true;
					break;
				}
			}
			
			if(checkContain == false) {
				listSWUsed.add(aggSW);
				topo.setListSwitchUsed(listSWUsed);
			}
			
			linkAggEdge01.setUsedBW(bandwidth);
			linkAggEdge10.setUsedBW(bandwidth);
			linkAggEdge02.setUsedBW(bandwidth);
			linkAggEdge20.setUsedBW(bandwidth);
			vLink.getLinkSubstrate().add(linkAggEdge01);
			vLink.getLinkSubstrate().add(linkAggEdge10);
			vLink.getLinkSubstrate().add(linkAggEdge02);
			vLink.getLinkSubstrate().add(linkAggEdge20);
			aggSW.setPort(edgeSwitch1, bandwidth);
			aggSW.setPort(edgeSwitch2, bandwidth);
			edgeSwitch1.setPort(aggSW, bandwidth);
			edgeSwitch2.setPort(aggSW, bandwidth);		

			linkPhyEdge01.setUsedBW(bandwidth);
			linkPhyEdge10.setUsedBW(bandwidth);
			linkPhyEdge02.setUsedBW(bandwidth);
			linkPhyEdge20.setUsedBW(bandwidth);
			vLink.getLinkSubstrate().add(linkPhyEdge01);
			vLink.getLinkSubstrate().add(linkPhyEdge10);
			vLink.getLinkSubstrate().add(linkPhyEdge02);
			vLink.getLinkSubstrate().add(linkPhyEdge20);
			

		} else {
			System.out.println("Int Link failed at Agg.");
			success = false;
		}
		
		return success;
	}

	private boolean linkMappingCoreSeparate( VirtualLink vLink, SubstrateSwitch edgeSwitch1,
			SubstrateSwitch edgeSwitch2, Topology topo) {
		// TODO Auto-generated method stub
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAggMap = topo.getListCoreConnectAgg();	
		LinkedList<SubstrateLink> listPhyLinks = topo.getlistPhyLinks();
		LinkedList<SubstrateSwitch> listAggSort1 = new LinkedList<>();
		LinkedList<SubstrateSwitch> listAggSort2 = new LinkedList<>();
		LinkedList<SubstrateSwitch> listCoreSort1 = new LinkedList<>();
		LinkedList<SubstrateSwitch> listCoreSort2 = new LinkedList<>();
		
		PhysicalServer phy1 = vLink.getsService().getBelongToServer();
		PhysicalServer phy2 = vLink.getdService().getBelongToServer();
		
		SubstrateSwitch aggA = null, aggB = null, core = null;
		
		SubstrateLink linkPhyEdge1a = null, linkPhyEdge1b = null;
		SubstrateLink linkPhyEdge2a = null, linkPhyEdge2b = null;
		SubstrateLink linkAggEdge1a = null, linkAggEdge1b = null;
		SubstrateLink linkAggEdge2a = null, linkAggEdge2b = null;
		SubstrateLink linkCoreAgg1a = null, linkCoreAgg1b = null;
		SubstrateLink linkCoreAgg2a = null, linkCoreAgg2b = null;
		
		boolean checkEdge = false;
		double bwDemand = vLink.getBWRequest();
		int count = 0;
		//===get edge switch connect to server=====================================//
		for(SubstrateLink link : listPhyLinks) {
			if(link.getStart().equals(edgeSwitch1) && link.getEnd().equals(phy1) && link.getRemainBW() >= bwDemand) {
				linkPhyEdge1a = link;
				count++;
			} else if(link.getEnd().equals(edgeSwitch1) && link.getStart().equals(phy1) && link.getRemainBW() >= bwDemand) {
				linkPhyEdge1b = link;
				count++;
			} else if(link.getStart().equals(edgeSwitch2) && link.getEnd().equals(phy2) && link.getRemainBW() >= bwDemand) {
				linkPhyEdge2a = link;
				count++;
			} else if(link.getEnd().equals(edgeSwitch2) && link.getStart().equals(phy2) && link.getRemainBW() >= bwDemand) {
				linkPhyEdge2b = link;
				count++;
			} else {
				;
			}
			if(count == 4) {
				checkEdge = true;
				break;
			} 
		}
		
		if(!checkEdge) {
			System.out.println("Int Link failed at Core1.");
			return false; // link from server to edge switch is not enough
		}
		
		listAggSort1 = sortListSwitch(listAggConnectEdge.get(edgeSwitch1));
		listAggSort2 = sortListSwitch(listAggConnectEdge.get(edgeSwitch2));
	
		//=== find link connect Agg to Edge ======================================//
		AGG_EDGE_LOOP1:
		for(SubstrateSwitch agg1 : listAggSort1) {
			count = 0;
			for (SubstrateLink link : listPhyLinks) {
				if(link.getStart() == edgeSwitch1 && link.getEnd() == agg1) {
					if(link.getRemainBW() < bwDemand) {
						break;
					}else {
						linkAggEdge1a = link;
						count ++;
					}
				}
				if(link.getStart() == agg1 && link.getEnd() == edgeSwitch1) {
					if(link.getRemainBW() < bwDemand) {
						break;
					}else {
						linkAggEdge1b = link;
						count ++;
					}
				}
				if(count == 2) {
					aggA = agg1;
					break AGG_EDGE_LOOP1;
				}
			}
		} // end agg1 
		count = 0;
		AGG_EDGE_LOOP2:
		for(SubstrateSwitch agg2 : listAggSort2) {
			for (SubstrateLink link : listPhyLinks) {
				if(link.getStart() == edgeSwitch2 && link.getEnd() == agg2) {
					if(link.getRemainBW() < bwDemand) {
						break;
					}else {
						linkAggEdge2a = link;
						count ++;
					}
				}
				
				if(link.getStart() == agg2 && link.getEnd() == edgeSwitch2) {
					if(link.getRemainBW() < bwDemand) {
						break;
					}else {
						linkAggEdge2b = link;
						count ++;
					}
				}
				
				if(count == 2) {
					aggB = agg2;
					break AGG_EDGE_LOOP2;
				}
			}
		} // end agg2
		//=== find link connect Agg to Core ======================================//
		listCoreSort1 = sortListSwitch(listCoreConnectAggMap.get(aggA));
		listCoreSort2 = sortListSwitch(listCoreConnectAggMap.get(aggB));
		
		if(!listCoreSort1.equals(listCoreSort2)) {
			return false;
		}
		
		for(int index = 0; index < listCoreSort1.size(); index++) {
			core = listCoreSort1.get(index);
			for (SubstrateLink link : listPhyLinks) {
				if(link.getStart() == aggA && link.getEnd() == core) {
					if(link.getRemainBW() < bwDemand) {
						return false;
					}else {
						linkCoreAgg1a = link;
					}
				}
				else if(link.getStart() == core && link.getEnd() == aggA) {
					if(link.getRemainBW() < bwDemand) {
						return false;
					}else {
						linkCoreAgg1b = link;
					}
				}
				else if(link.getStart() == aggB && link.getEnd() == core) {
					if(link.getRemainBW() < bwDemand) {
						return false;
					}else {
						linkCoreAgg2a = link;
					}
				}
				else if(link.getStart() == core && link.getEnd() == aggB) {
					if(link.getRemainBW() < bwDemand) {
						return false;
					}else {
						linkCoreAgg2b = link;
					}
				}
			}
		}
		
		//===set up bandwidth for all found links above ========//
		linkPhyEdge1a.setUsedBW(bwDemand);
		linkPhyEdge1b.setUsedBW(bwDemand);
		linkPhyEdge2a.setUsedBW(bwDemand);
		linkPhyEdge2b.setUsedBW(bwDemand);
		linkAggEdge1a.setUsedBW(bwDemand);
		linkAggEdge1b.setUsedBW(bwDemand);
		linkAggEdge2a.setUsedBW(bwDemand);
		linkAggEdge2b.setUsedBW(bwDemand);
		linkCoreAgg1a.setUsedBW(bwDemand);
		linkCoreAgg1b.setUsedBW(bwDemand);
		linkCoreAgg2a.setUsedBW(bwDemand);
		linkCoreAgg2b.setUsedBW(bwDemand);
		edgeSwitch1.setPort(edgeSwitch1, bwDemand);
		edgeSwitch2.setPort(edgeSwitch1, bwDemand);
		aggA.setPort(edgeSwitch1, bwDemand);
		edgeSwitch1.setPort(aggA, bwDemand);
		aggB.setPort(edgeSwitch2, bwDemand);
		edgeSwitch2.setPort(aggB, bwDemand);
		core.setPort(aggA, bwDemand);
		aggA.setPort(core, bwDemand);
		core.setPort(aggB, bwDemand);
		aggB.setPort(core, bwDemand);
		vLink.getLinkSubstrate().add(linkPhyEdge1a);
		vLink.getLinkSubstrate().add(linkPhyEdge1b);
		vLink.getLinkSubstrate().add(linkPhyEdge2a);
		vLink.getLinkSubstrate().add(linkPhyEdge2b);
		vLink.getLinkSubstrate().add(linkAggEdge1a);
		vLink.getLinkSubstrate().add(linkAggEdge1b);
		vLink.getLinkSubstrate().add(linkAggEdge2a);
		vLink.getLinkSubstrate().add(linkAggEdge2b);
		vLink.getLinkSubstrate().add(linkCoreAgg1a);
		vLink.getLinkSubstrate().add(linkCoreAgg1b);
		vLink.getLinkSubstrate().add(linkCoreAgg2a);
		vLink.getLinkSubstrate().add(linkCoreAgg2b);
		return true;
	}

	
	// sort List switch in increasing order by ID
	public LinkedList<SubstrateSwitch> sortListSwitch(LinkedList<SubstrateSwitch> list) {
		Collections.sort(list, new Comparator<SubstrateSwitch>() {
			@Override
			public int compare(SubstrateSwitch o1, SubstrateSwitch o2) {
				if (Integer.parseInt(o1.getNameSubstrateSwitch()) < Integer.parseInt(o2.getNameSubstrateSwitch())) {
					return -1;
				}
				if (Integer.parseInt(o1.getNameSubstrateSwitch()) > Integer.parseInt(o2.getNameSubstrateSwitch())) {
					return 1;
				}
				return 0;
			}
		});
		return list;
	}

	public boolean checkPhyEdge(PhysicalServer phy1, SubstrateSwitch edge1, PhysicalServer phy2,
			SubstrateSwitch edge2, double bandwidth, LinkedList<LinkPhyEdge> listPhyEdgeTemp) {
		boolean check = false;
		boolean Satisfied = false;
		int count=0;
		for (LinkPhyEdge link : listPhyEdgeTemp) {
			if ((link.getPhysicalServer().equals(phy1) && link.getEdgeSwitch().equals(edge1)) && link.getBandwidth() >= bandwidth) {
					Satisfied = true;
					count++;
			}
			if ((link.getPhysicalServer().equals(phy2) && link.getEdgeSwitch().equals(edge2)) && link.getBandwidth() >= bandwidth) {
					check = true;
					count++;
			}
			if(count == 2) {
				break;
			}
		}
		
		return (Satisfied&&check);
	}
	
	public void leave(LinkedList<SFC> listSFC, Topology topo) {
		
		for(SFC sfc : listSFC) {
			
			//===return bandwidth for substratelink===============================//
			LinkedList<VirtualLink> listVirLink = sfc.getvLink();
			for(VirtualLink vLink : listVirLink) {
				double bandwidth = vLink.getBWRequest();
				LinkedList<SubstrateLink> listSubstrate = vLink.getLinkSubstrate();
				for(SubstrateLink linkSubstrate : listSubstrate) {
					linkSubstrate.setUsedBW((-1.0)*bandwidth);
					if(!topo.getListComputeNodes().contains(linkSubstrate.getStart()) && !topo.getListComputeNodes().contains(linkSubstrate.getEnd())) {
						SubstrateSwitch start = (SubstrateSwitch) linkSubstrate.getStart();
						SubstrateSwitch end = (SubstrateSwitch) linkSubstrate.getEnd();
						start.setPort(end, (-1.0)*bandwidth);
						end.setPort(start, (-1.0)*bandwidth);
					}
				}
			}
			listVirLink.clear(); 
		}
	}

	
	
	public SubstrateSwitch getSwitchFromID(LinkedList<SubstrateSwitch> listSwitch, String id) {
		SubstrateSwitch s= new SubstrateSwitch();
		for(SubstrateSwitch sw: listSwitch)
			if(sw.getNameSubstrateSwitch().equals(id))
			{
				s= sw;
				break;
			}
		return s;
	}
	
	public double getPower(Topology topo)
	{
		double power = 0;
		modelHP HP = new modelHP();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
//		for(SubstrateLink link: topo.getLinkBandwidth())
//		{
//			double bw = link.getBandwidth();
//			SubstrateSwitch s = link.getStart();
//			if(listSwitch.containsKey(s.getNameSubstrateSwitch()))
//			{
//				SubstrateSwitch sw = listSwitch.get(s.getNameSubstrateSwitch());
//				sw.setPort(link.getEnd(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), sw);
//			}
//			else				
//			{
//				s.setPort(link.getEnd(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), s);
//			}
//			
//		}
		for(SubstrateSwitch entry: listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}
	
	public double getPower(LinkedList<SubstrateSwitch> listSwitch)
	{
		double power = 0;
		modelHP HP = new modelHP();
		for(SubstrateSwitch entry : listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}

	public int getNumLinkSuccess() {
		return numLinkSuccess;
	}

	public void setNumLinkSuccess(int numLinkSuccess) {
		this.numLinkSuccess = numLinkSuccess;
	}

	public double getPowerConsumed() {
		return powerConsumed;
	}

	public void setPowerConsumed(double powerConsumed) {
		this.powerConsumed = powerConsumed;
	}

}
