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

        def Placement(serverCap, package):
            arg = round(5 / 4 * pow(4 * len(serverCap), 2/3) + 1)
            k = round((len(serverCap)*4)**(1/3))
            a,b,a2,b2,onState = [],[],[],[],[]
            for i in serverCap:
                if i==4: onState.append(1)
                else: onState.append(0)
            for i in range(2*len(serverCap)//k):
                a.append(sum(onState[i*k//2:(i+1)*k//2]))
                a2.append(sum(serverCap[i*k//2:(i+1)*k//2]))
            for i in range(4*len(serverCap)//(k**2)):
                b.append(a[i*(k//2):(i+1)*k//2])
                b2.append(a2[i*(k//2):(i+1)*k//2])
            sfc_len = len(package)
            if sfc_len > sum(serverCap): return False
            b = np.array(b)
            def smallfunction(addr, num_vnf, result):
                temp = np.array(serverCap[addr*(k//2):(addr+1)*(k//2)])
                for l in np.argsort(temp)[::-1]:
                    if num_vnf > temp[l]:
                        if temp[l] == 0: continue
                        num_vnf -= temp[l]
                        # print(addr*(k//2)+l,'--',temp[l])
                        result.append([addr*(k//2)+l,temp[l]])
                    else:
                        # print(addr*(k//2)+l,'--',num_vnf)
                        result.append([addr*(k//2)+l,num_vnf])
                        break
                    
            result = []
            for j in np.argsort(np.sum(b,axis=1)):
                if sfc_len == 0: break
                # print(b[j])
                temp_array = np.argsort(b[j])
                # print(temp_array)
                for i in temp_array:
                    if b2[j][i] >= sfc_len:
                        smallfunction((k//2)*j+i,sfc_len,result)
                        sfc_len = 0
                        break
                    else:
                        smallfunction((k//2)*j+i,b2[j][i],result)
                        sfc_len -= b2[j][i]

            alloc = []
            for i in result:
                alloc += [i[0]+arg]*i[1]
            return alloc

        # alloc vnf to server
        serverCap = []
        k = round((len(topo.nodes.data())*4)**(1/3))-1
        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                serverCap.append(node[1]["RAM"][0] - node[1]["RAM"][1])
        vnfCap = []
        for vnf in list(sfc["struct"].nodes.data()):
            vnfCap.append(vnf[1]["RAM"])

        # print(serverCap)
        alloc = Placement(serverCap, vnfCap)
        # print(alloc)

        if(alloc):
            deploy = {"node": [], "link": []}

            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                deploy["node"].append([vnf[0], c_server])
                vnf[1]["place"] = [DC.id, c_server]

            for (i, j) in sfc["struct"].edges:
                s = sfc["struct"].nodes[i]["place"][1]
                d = sfc["struct"].nodes[j]["place"][1]
                v_link = sfc["struct"].edges[i, j]
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
            # print(deploy["sfc"]["id"])
            # exit()

            return deploy
            # deploy has format like deploy in formdata.py     

        else: return False