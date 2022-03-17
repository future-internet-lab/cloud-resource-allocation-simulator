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
                



class WaxmanSelector_0(Selector):
    """
    selector algorithm for analysing SFC waxman random topo
    """
    def __init__(self):
        pass


    def analyse(self, DC, sfc):
        topo = copy.deepcopy(DC.topo)

        def randPlacement(bin, package):
            # print(bin)
            # print(package)
            n_bin = len(bin)
            n_package = len(package)
            # print("n_bin = ", n_bin)
            # print("n_package = ", n_package)
            arg = round(5 / 4 * pow(4 * n_bin, 2/3) + 1)
            alloc = [-1] * n_package
            for i in range(n_package):
                for _ in range(0, 10):
                    idx = np.random.randint(0, n_bin)
                    # print(i, idx, bin[idx])
                    if(bin[idx] >= package[i]):
                        bin[idx] -= package[i]
                        alloc[i] = idx + arg
                        break
                    if(_ == 9):
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
        # print(serverCap)
        # print(vnfCap)
        # print("alloc =",alloc)

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
                _topo = copy.deepcopy(topo)
                v_link = sfc["struct"].edges[i, j]
                # print()
                # print(sfc["struct"].edges[i, j]["bw"], v_link["bw"])
                for p_link in list(topo.edges.data()):
                    # print(p_link[2]["bw"][1], p_link[2]['bw'][0])
                    if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < v_link["bw"]):
                        _topo.remove_edge(p_link[0], p_link[1])
                # if(len(topo.edges) != len(_topo.edges)):
                #     print(len(topo.edges), len(_topo.edges))
                try:
                    route = nx.shortest_path(_topo, s, d)
                    # print(route)
                    for i in range(len(route) - 1):
                        topo.edges[route[i], route[i+1]]['bw'] = [
                            topo.edges[route[i], route[i+1]]['bw'][0],
                            topo.edges[route[i], route[i+1]]['bw'][1] + v_link["bw"]
                        ]
                    # print(sfc["struct"].edges[i, j]["bw"], v_link["bw"])
                    sfc["struct"].edges[i, j]["bw"] = v_link["bw"]
                    sfc["struct"].edges[i, j]["route"] = route
                except:
                    print(f"cannot routing from {s} to {d}")
                    return False
                # anaRes["link"].append({
                #     "bw": v_link["bw"],
                #     "route": route
                # })
                
            
            # anaRes["sfc"] = sfc
            sfc["DataCentre"] = DC.id

            return copy.deepcopy(sfc)

        else:
            return False


class WaxmanSelector(Selector):
    """
    selector algorithm for analysing SFC waxman random topo
    """
    def __init__(self):
        pass


    def analyse(self, DC, sfc):
        topo = copy.deepcopy(DC.topo)

        def fat_tree(k):
            lastCore = int((k/2)**2)
            lastAggre = int(lastCore + k**2 / 2)
            lastEdge = int(lastAggre  + k**2 / 2)
            lastServer = int(lastEdge + k**3 / 4)
            G = nx.Graph()
            for i in range(lastServer): G.add_node(i + 1)
            for pod in range(k): # create all links
                for aggre in range(int(k / 2)):
                    for i in range(int(k / 2)):
                        G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1))
                        G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1))
                        G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1))
            return G

        # nx.draw(topo, with_labels=True, font_weight='bold')
        # plt.show()

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
            # deploy = {"node": [], "link": []}
            """temp = list(nx.connected_components(sfc["struct"]))
            print(temp)
            for i in temp:
                print(i)"""

            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                # deploy["node"].append([vnf[0], c_server])
                vnf[1]["server"] = c_server

            for (i, j) in sfc["struct"].edges:
                s = sfc["struct"].nodes[i]["server"]
                d = sfc["struct"].nodes[j]["server"]
                if s == d: continue
                # print('s:',s,'d:',d)
                _topo = fat_tree(round((len(serverCap)*4)**(1/3)))
                v_link = sfc["struct"].edges[i, j]
                for p_link in list(topo.edges.data()):
                    if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < v_link["bw"]):
                        _topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(_topo, s, d)
                    # print(route)
                    for a in range(len(route)-1):
                        topo.edges[route[a],route[a+1]]['bw'] = [
                            topo.edges[route[a],route[a+1]]['bw'][0],
                            topo.edges[route[a],route[a+1]]['bw'][1] + v_link["bw"]
                        ]
                    sfc["struct"].edges[i, j]["bw"] = v_link["bw"]
                    sfc["struct"].edges[i, j]["route"] = route
                except:
                    return False
            
            sfc["DataCentre"] = DC.id

            return copy.deepcopy(sfc)

        else: return False
