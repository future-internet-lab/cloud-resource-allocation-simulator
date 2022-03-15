from abc import ABC, abstractmethod

import numpy as np
import networkx as nx

import json
import copy



class Selector(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def analyse(self):
        pass





class SimpleSelector(Selector):
    """
    all of this class is used to test input and output of analyse()
    no useful algorithm is implemented
    """
    def __init__(self):
        pass


    def analyse(self, DC, sfc):
        topo = copy.deepcopy(DC.topo)

        def randPlacement(bin, package):
            n_bin = len(bin)
            n_package = len(package)
            arg = round(5 / 4 * pow(4 * n_bin, 2/3) + 1)
            alloc = [-1] * n_package
            for i in range(n_package):
                for _ in range(1, 20):
                    idx = np.random.randint(0, n_bin)
                    if(bin[idx] > package[i]):
                        bin[idx] -= package[i]
                        alloc[i] = idx + arg
                        break
                    return 0

            return alloc

        # alloc vnf to server
        serverCap = []
        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                serverCap.append(node[1]["RAM"][0] - node[1]["RAM"][1])
        vnfCap = []
        for vnf in list(sfc["struct"].nodes.data()):
            vnfCap.append(vnf[1]["RAM"])
        
        alloc = randPlacement(serverCap, vnfCap)
        print(alloc)

        if(alloc):
            deploy = {"node": [], "link": []}

            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                deploy["node"].append([vnf[0], c_server])
                vnf[1]["place"] = [DC.id, c_server]

            for vnf in range(len(sfc["struct"].nodes) - 1):
                s = sfc["struct"].nodes[vnf]["place"][1]
                d = sfc["struct"].nodes[vnf + 1]["place"][1]
                v_link = sfc["struct"].edges[vnf, vnf + 1]
                for p_link in list(topo.edges.data()):
                    if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < v_link["bw"]):
                        topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(topo, s, d)
                except:
                    return False
                deploy["link"].append({
                    "bw": v_link["bw"],
                    "route": route
                })

            deploy["sfc"] = sfc

            return deploy

        else: return False
                



class WaxmanSelector(Selector):
    """
    selector algorithm for analysing SFC waxman random topo
    """
    def __init__(self):
        pass


    def analyse(self, DC, sfc):
        topo = copy.deepcopy(DC.topo)

        def randPlacement(bin, package):
            n_bin = len(bin)
            n_package = len(package)
            arg = round(5 / 4 * pow(4 * n_bin, 2/3) + 1)
            alloc = [-1] * n_package
            for i in range(n_package):
                for _ in range(1, 20):
                    idx = np.random.randint(0, n_bin)
                    if(bin[idx] > package[i]):
                        bin[idx] -= package[i]
                        alloc[i] = idx + arg
                        break
                    return 0
            return alloc

        # alloc vnf to server
        serverCap = []
        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                serverCap.append(node[1]["RAM"][0] - node[1]["RAM"][1])
        vnfCap = []
        for vnf in list(sfc["struct"].nodes.data()):
            vnfCap.append(vnf[1]["RAM"])

        alloc = randPlacement(serverCap, vnfCap)

        if(alloc):
            # anaRes = {"node": [], "link": []}

            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                # deploy["node"].append([vnf[0], c_server])
                vnf[1]["server"] = c_server

            for (i, j) in sfc["struct"].edges:
                s = sfc["struct"].nodes[i]["server"]
                d = sfc["struct"].nodes[j]["server"]
                # print(s, d)
                v_link = sfc["struct"].edges[i, j]
                for p_link in list(topo.edges.data()):
                    # print(p_link[2]["bw"][1], p_link[2]['bw'][0])
                    if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < v_link["bw"]):
                        # print("remove")
                        topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(topo, s, d)
                except:
                    # print(f"cannot routing from {s} to {d}")
                    return False
                # anaRes["link"].append({
                #     "bw": v_link["bw"],
                #     "route": route
                # })
                sfc["struct"].edges[i, j]["bw"] = v_link["bw"]
                sfc["struct"].edges[i, j]["route"] = route
            
            # anaRes["sfc"] = sfc
            sfc["DataCentre"] = DC.id

            return copy.deepcopy(sfc)

        else: return False
