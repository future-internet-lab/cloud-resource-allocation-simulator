from abc import ABC, abstractmethod

import numpy as np
import networkx as nx
import random

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
                serverCap.append(node[1]["capacity"] - node[1]["usage"])
        vnfCap = []
        for vnf in list(sfc["struct"].nodes.data()):
            vnfCap.append(vnf[1]["demand"])
        
        alloc = randPlacement(serverCap, vnfCap)

        if(alloc):
            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                vnf[1]["server"] = c_server

            for vlink in list(sfc["struct"].edges.data()):
                s = sfc["struct"].nodes[vlink[0]]["server"]
                d = sfc["struct"].nodes[vlink[1]]["server"]

                _topo = copy.deepcopy(topo)
                
                v_link = sfc["struct"].edges[vlink[0], vlink[1]]
                for p_link in list(_topo.edges.data()):
                    if(p_link[2]["capacity"] - p_link[2]['usage'] < v_link["demand"]):
                        _topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(_topo, s, d)
                    for i in range(len(route) - 1):
                        topo.edges[route[i], route[i+1]]['usage'] + v_link["demand"]
                    sfc["struct"].edges[vlink[0], vlink[1]]["route"] = route
                except:
                    print(f"cannot routing from {s} to {d}, bw = {v_link['demand']} ---------")
                    sfc["struct"].edges[vlink[0], vlink[1]]["route"] = []
                    return False
            sfc["DataCentre"] = DC.id
            return copy.deepcopy(sfc)
        else:
            print("cannot alloc")
            return False



class WaxmanSelector_0(Selector):
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

        def usagebw(_topo):
            usage = 0
            for link in list(_topo.edges.data()):
                usage += link[2]["bw"][1]
            return usage

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
            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                vnf[1]["server"] = c_server

            for vlink in list(sfc["struct"].edges.data()):
                s = sfc["struct"].nodes[vlink[0]]["server"]
                d = sfc["struct"].nodes[vlink[1]]["server"]
                _topo = copy.deepcopy(topo)
                
                v_link = sfc["struct"].edges[vlink[0], vlink[1]]
                for p_link in list(_topo.edges.data()):
                    if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < v_link["bw"]):
                        _topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(_topo, s, d)
                    for i in range(len(route) - 1):
                        topo.edges[route[i], route[i+1]]['bw'] = [
                            topo.edges[route[i], route[i+1]]['bw'][0],
                            topo.edges[route[i], route[i+1]]['bw'][1] + v_link["bw"]
                        ]
                    sfc["struct"].edges[vlink[0], vlink[1]]["route"] = route
                except:
                    print(f"cannot routing from {s} to {d}")
                    return False
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

        def Placement(serverCap, package):
            arg = round(5 / 4 * pow(4 * len(serverCap), 2/3) + 1)
            k = round((len(serverCap)*4)**(1/3))
            a,b,a2,b2,onState = [],[],[],[],[]
            for i in serverCap:
                if i==100: onState.append(1)
                else: onState.append(0)
            for i in range(2*len(serverCap)//k):
                a.append(sum(onState[i*k//2:(i+1)*k//2]))
                a2.append(sum(serverCap[i*k//2:(i+1)*k//2]))
            for i in range(4*len(serverCap)//(k**2)):
                b.append(a[i*(k//2):(i+1)*k//2])
                b2.append(a2[i*(k//2):(i+1)*k//2])
            b = np.array(b)
                    
            def process():
                vnf_c = 0
                result = []
                for j in np.argsort(np.sum(b,axis=1))[::-1]:
                    # choose candidate groups with the least number of servers in ON State
                    for i in np.argsort(b[j])[::-1]:
                        addr = (k//2)*j+i
                        temp = np.array(serverCap[addr*(k//2):(addr+1)*(k//2)])
                        for l in np.argsort(temp):
                            while temp[l] >= package[vnf_c]:
                                temp[l] -= package[vnf_c]
                                result.append([addr*(k//2)+l,temp[l]])
                                vnf_c += 1
                                # print(temp)
                                if vnf_c >= len(package): return result
                return result

            result = process()
            # print("result =", result)
            # print("package =", package)
            if len(result) < len(package): return False
            # try:
            #     if len(result) < len(package): return False
            # except:
            #     print(result)
            #     print(package)
            #     exit()
            #     return False
            alloc = []
            for i in result:
                alloc += [i[0]+arg]
            # print(alloc)
            return alloc

        # alloc vnf to server
        serverCap = []
        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                serverCap.append(node[1]["capacity"] - node[1]["usage"])
        vnfCap = []
        for vnf in list(sfc["struct"].nodes.data()):
            vnfCap.append(vnf[1]["demand"])
        # print('serverCap',serverCap)
        alloc = Placement(serverCap, vnfCap)

        if(alloc):
            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                vnf[1]["server"] = c_server

            for vlink in list(sfc["struct"].edges.data()):
                s = sfc["struct"].nodes[vlink[0]]["server"]
                d = sfc["struct"].nodes[vlink[1]]["server"]

                _topo = copy.deepcopy(topo)
                
                v_link = sfc["struct"].edges[vlink[0], vlink[1]]
                for p_link in list(_topo.edges.data()):
                    if(p_link[2]["capacity"] - p_link[2]['usage'] < v_link["demand"]):
                        _topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(_topo, s, d)
                    for i in range(len(route) - 1):
                        topo.edges[route[i], route[i+1]]['usage'] + v_link["demand"]
                    sfc["struct"].edges[vlink[0], vlink[1]]["route"] = route
                except:
                    print(f"cannot routing from {s} to {d}, bw = {v_link['demand']} ---------")
                    sfc["struct"].edges[vlink[0], vlink[1]]["route"] = []
                    return False
            sfc["DataCentre"] = DC.id
            return copy.deepcopy(sfc)
        else:
            print("cannot alloc")
            return False




class VNFFG_node_splitting(Selector):
    """
    use with VNF-FG app
    """
    def __init__(self):
        pass

    def analyse(self, DC, sfc):
        topo = copy.deepcopy(DC.topo)

        def status(sc):
            result = []
            for i in range(len(sc)):
                if sc[i] != 0 : result.append(i)
            return result

        def Placement(serverCap, package):
            arg = round(5 / 4 * pow(4 * len(serverCap), 2/3) + 1)
            sfc_len = len(package)
            if sum(serverCap) < sfc_len: return False
            alloc = []
            for i in range(sfc_len):
                rand_id = random.randint(0,len(status(serverCap))-1)
                alloc.append(status(serverCap)[rand_id]+arg)
                serverCap[status(serverCap)[rand_id]] -= 1
            return alloc

        # alloc vnf to server
        serverCap = []
        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                serverCap.append(node[1]["capacity"] - node[1]["usage"])
        vnfCap = []
        for vnf in list(sfc["struct"].nodes.data()):
            vnfCap.append(vnf[1]["demand"])
        alloc = Placement(serverCap, vnfCap)

        def split_a_node(sfc_struct, node, vnf_splited, count):
            node_new = count
            count += 1
            vnf_splited.append(node)
            vnf_splited.append(node_new)
            attl = copy.deepcopy(sfc_struct.nodes[node])
            sfc_struct.add_nodes_from([(node_new, attl)])
            sfc_struct.nodes[node_new]['server'] = Placement(serverCap, [1])[0]     # them node split
            for (i,j) in sfc_struct.edges(node):
                sfc_struct.add_edge(j,node_new)     # them edge
                sfc_struct[j][node_new]['demand'] = sfc_struct[j][node]['demand'] / 2   # chia doi
                sfc_struct[j][node]['demand'] = sfc_struct[j][node]['demand'] / 2
                sfc_struct[j][node_new]['route'] = []
                sfc_struct[j][node]['route'] = []
                sfc_struct[j][node_new]['split'] = ''
                sfc_struct[j][node]['split'] = ''
        c = len(vnfCap)

        if(alloc):
            for vnf in list(sfc["struct"].nodes.data()):
                c_server = alloc[vnf[0]] # the choosen server
                vnf[1]["server"] = c_server

            vnf_splited = []
            for vlink in list(sfc["struct"].edges.data()):
                s = sfc["struct"].nodes[vlink[0]]["server"]
                d = sfc["struct"].nodes[vlink[1]]["server"]

                _topo = copy.deepcopy(topo)
                v_link = sfc["struct"].edges[vlink[0], vlink[1]]
                for p_link in list(_topo.edges.data()):
                    if (p_link[2]["capacity"] - p_link[2]['usage'] < v_link["demand"]):
                        _topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(_topo, s, d)
                    for i in range(len(route) - 1):
                        topo.edges[route[i], route[i+1]]['usage'] + v_link["demand"]
                    sfc["struct"].edges[vlink[0], vlink[1]]["route"] = route
                except:
                    if len(status(serverCap)) == 0 or vlink[0] in vnf_splited or vlink[1] in vnf_splited or sfc["struct"][vlink[0]][vlink[1]]['split'] == '':
                        sfc["struct"].edges[vlink[0], vlink[1]]["route"] = []
                        # print(f"cannot routing from {s} to {d}, bw = {v_link['demand']} ---------")
                        return False
                    else: # try to splitting
                        split_a_node(sfc["struct"], sfc["struct"][vlink[0]][vlink[1]]['split'], vnf_splited, c)
                        _topo = copy.deepcopy(topo)
                        v_link = sfc["struct"].edges[vlink[0], vlink[1]]
                        for p_link in list(_topo.edges.data()):
                            if (p_link[2]["capacity"] - p_link[2]['usage'] < v_link["demand"]):    # mot nua bw
                                _topo.remove_edge(p_link[0], p_link[1])
                        try:
                            route = nx.shortest_path(_topo, s, d)
                            for i in range(len(route) - 1):
                                topo.edges[route[i], route[i+1]]['usage'] + v_link["demand"]
                            sfc["struct"].edges[vlink[0], vlink[1]]["route"] = route
                        except:
                            # print(f"cannot routing from {s} to {d}, bw = {v_link['demand']} ---------")
                            sfc["struct"].edges[vlink[0], vlink[1]]["route"] = []
                            return False
            sfc["DataCentre"] = DC.id
            return copy.deepcopy(sfc)  
        else: return False
