from abc import abstractclassmethod, abstractmethod
from sim.DataCentre import *
from sim.Ingress import *

import networkx as nx
import copy




class Substrate():
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs):
        self.name = self.__class__.__name__
        self.linkCap = linkCap
        self.topology = self.substrate_topo(DCPos, IngressPos)

        self.DCs = []
        self.init_DCs(DCPos, DCArgs)

        self.Ingresses = []
        self.init_Ingresses(IngressPos, IngressArgs)


    def init_DCs(self, DCPos, DCArgs):
        for i in range(len(DCPos)):
            self.DCs.append(DataCentre(DCPos[i], self.DC_topo(DCArgs[i])))

    def init_Ingresses(self, IngressPos, IngressArgs):
        for i in range(len(IngressPos)):
            self.Ingresses.append(Ingress(IngressPos[i], IngressArgs[i]))

    @abstractmethod
    def substrate_topo(self): pass

    @abstractmethod
    def DC_topo(self, args): pass




class Abilene(Substrate):
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs):
        super().__init__(DCPos, IngressPos, linkCap, DCArgs, IngressArgs)
        


    def substrate_topo(self, DCPos, IngressPos):
        G = nx.Graph()

        G.add_edge(1, 2, capacity=self.linkCap, usage=0)
        G.add_edge(1, 12, capacity=self.linkCap, usage=0)
        G.add_edge(2, 3, capacity=self.linkCap, usage=0)
        G.add_edge(2, 12, capacity=self.linkCap, usage=0)
        G.add_edge(3, 5, capacity=self.linkCap, usage=0)
        G.add_edge(3, 10, capacity=self.linkCap, usage=0)
        G.add_edge(4, 5, capacity=self.linkCap, usage=0)
        G.add_edge(4, 6, capacity=self.linkCap, usage=0)
        G.add_edge(5, 9, capacity=self.linkCap, usage=0)
        G.add_edge(6, 7, capacity=self.linkCap, usage=0)
        G.add_edge(7, 9, capacity=self.linkCap, usage=0)
        G.add_edge(8, 9, capacity=self.linkCap, usage=0)
        G.add_edge(9, 10, capacity=self.linkCap, usage=0)
        G.add_edge(10, 11, capacity=self.linkCap, usage=0)
        G.add_edge(11, 12, capacity=self.linkCap, usage=0)

        for i in range(1, 13):
            if(i in DCPos):
                G.nodes[i]["role"] = "DataCentre"
            elif(i in IngressPos):
                G.nodes[i]["role"] = "Ingress"
            else:
                G.nodes[i]["role"] = "Switch"

        return copy.deepcopy(G)



    def DC_topo(self, *arg):
        switchSpecs = {
            "basePower": 39,
            "portPower": [0.42, 0.48, 0.9]
        }
        serverSpecs = {
            "capacity": 100,
            "usage": 0
        }
        k = arg[0]
        lastCore = int((k/2)**2)
        lastAggre = int(lastCore + k**2 / 2)
        lastEdge = int(lastAggre  + k**2 / 2)
        lastServer = int(lastEdge + k**3 / 4)
        bwCapacity = 500

        G = nx.Graph()

        for i in range(lastServer): G.add_node(i + 1)

        for pod in range(k): # create all links
            for aggre in range(int(k / 2)):
                for i in range(int(k / 2)):
                    G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1), capacity=bwCapacity, usage=0)
                    G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1), capacity=bwCapacity, usage=0)
                    G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1), capacity=bwCapacity, usage=0)


        for type in ["core", "aggregation", "edge"]:
            if(type == "core"): rangeSwitch = [1, lastCore + 1]
            if(type == "aggregation"): rangeSwitch = [lastCore + 1, lastAggre + 1]
            if(type == "edge"): rangeSwitch = [lastAggre + 1, lastEdge + 1]

            for switchID in range(*rangeSwitch):
                G.nodes[switchID]['model'] = 'switch'
                G.nodes[switchID]['tag'] = type
                G.nodes[switchID]['state'] = False
                for key in switchSpecs:
                    G.nodes[switchID][key] = switchSpecs[key]

        for serverID in range(lastEdge + 1, lastServer + 1):
            G.nodes[serverID]['model'] = 'server'
            G.nodes[serverID]['deployed'] = []
            G.nodes[serverID]['state'] = False
            for key in serverSpecs:
                G.nodes[serverID][key] = serverSpecs[key]

        return copy.deepcopy(G)
