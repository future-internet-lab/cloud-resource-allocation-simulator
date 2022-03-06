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


    def analyse(self, topo, sfc):
        _topo = copy.deepcopy(topo)
        def randPlacement(bin, package):
            n_bin = len(bin)
            n_package = len(package)
            alloc = [-1] * n_package
            for i in range(n_package):
                for _ in range(1, 20):
                    idx = np.random.randint(0, n_bin)
                    if(bin[idx] > package[i]):
                        bin[idx] -= package[i]
                        alloc[i] = idx + 21
                        break
                    return -1

            return alloc

        # alloc vnf to server
        serverCap = []
        for node in list(_topo.nodes.data()):
            if(node[1]["model"] == "server"): serverCap.append(node[1]["RAM"])
        vnfCap = []
        for vnf in list(sfc["structure"].nodes.data()):
            vnfCap.append(vnf[1]["RAM"])
        # print(serverCap)
        alloc = randPlacement(serverCap, vnfCap)
        # print(alloc)

        # print("n_vnf =", len(sfc.structure.nodes))
        if(alloc != -1):
            deploy = {"sfc": sfc["id"], "node": [], "link": []}

            for vnf in list(sfc["structure"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                deploy["node"].append([vnf[0], c_server])
                vnf[1]["onServer"] = c_server
                # if(not str(sfc.id) in _topo.nodes[c_server]["deployed"].keys()): 
                #     _topo.nodes[c_server]['deployed'][str(sfc.id)] = []
                # _topo.nodes[c_server]['deployed'][str(sfc.id)].append(vnf[0])
                _topo.nodes[c_server]['deployed'].append([sfc["id"], vnf[0]])


            for vnf in range(len(sfc["structure"].nodes) - 1):
                s = sfc["structure"].nodes[vnf]["onServer"]
                d = sfc["structure"].nodes[vnf + 1]["onServer"]
                v_link = sfc["structure"].edges[vnf, vnf + 1]
                for p_link in list(_topo.edges.data()):
                    if(p_link[2]["bw"] - p_link[2]['usage'] < v_link["bw"]): _topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(_topo, s, d)
                except:
                    return False
                deploy["link"].append({
                    "bw": v_link["bw"],
                    "route": route
                })

            # print(json.dumps(deploy))
            return deploy
            # deploy has format like deploy in formdata.py     

        else: return False
                
        # print("serverCap =", serverCap)
        # print("vnfCap =", vnfCap)
        # print("alloc =", alloc)
        # print(sfc.structure.nodes.data())