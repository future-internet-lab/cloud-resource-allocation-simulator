from sim.DataCentre import *
from sim.Ingress import *
from sklearn.cluster import KMeans
from pandas import DataFrame

import networkx as nx
import numpy as np
import copy
import itertools




class Substrate():
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters):
        self.name = self.__class__.__name__
        self.linkCap = linkCap

        self.topology = self.substrate_topo()

        self.n_clusters = n_clusters
        if(n_clusters != 0):
            self.DCPos, self.DCArgs = self.find_DC()
        else:
            self.DCPos = DCPos
            self.DCArgs = DCArgs
        # print(self.DCPos, self.DCArgs)
        self.IngressPos = IngressPos
        self.IngressArgs = IngressArgs

        

        self.DCs = []
        self.init_DCs()

        self.Ingresses = []
        self.init_Ingresses()

    def find_DC(self):
        INF = 999
        def floydWarshall():
            """
            shortest distance from a node to each others
            """
            G = self.topology
            n_node = len(G.nodes.data())
            graph = [[INF for col in range(n_node)] for row in range(n_node)]
            edges = [(edge[0], edge[1]) for edge in list(G.edges.data())]

            for edge in edges:
                i = edge[0] - 1
                j = edge[1] - 1
                graph[i][j] = 1
                graph[j][i] = 1
            for i in range(n_node):
                graph[i][i] = 0

            dist = list(map(lambda i: list(map(lambda j: j, i)), graph))
            for k in range(n_node):
                # pick all vertices as source one by one
                for i in range(n_node):
                    # Pick all vertices as destination for the
                    # above picked source
                    for j in range(n_node):
                        # If vertex k is on the shortest path from
                        # i to j, then update the value of dist[i][j]
                        dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])

            graphInfo = []
            for i in range(0, n_node):
                row_data = [i] * (n_node + 1)
                for j in range(1, n_node+1):
                    row_data[j] = dist[i][j-1]
                row_data.pop(0)
                graphInfo.append(row_data)

            return dist, graphInfo

        def find(kmeans, shortestMatrix):
            def find_center(cluster, shortestMatrix, center_coordinates):
                np_center_coordinates = np.array(center_coordinates)
                find_min_arr = []
                for i in cluster:
                    node_i_features = np.array(shortestMatrix[i])
                    euclid_dist = np.linalg.norm(np_center_coordinates - node_i_features)
                    find_min_arr.append(euclid_dist)
                temp = find_min_arr.index(min(find_min_arr))
                return cluster[temp]

            nodeLabels = kmeans.labels_
            centers = kmeans.cluster_centers_
            G = self.topology
            n_node = len(G.nodes.data())

            result = [[] for col in range(self.n_clusters)]
            for i in range(n_node):
                result[nodeLabels[i]].append(i)
            i = 0
            clusters = []
            DCs = []
            for cluster in result:
                center_of_cluster = find_center(cluster, shortestMatrix, centers[i])
                print(f"{cluster} -> {center_of_cluster}")
                i += 1
                clusters.append(cluster)
                DCs.append(center_of_cluster + 1)
            return clusters, DCs

        shortestMatrix, graphInfo = floydWarshall()
        kmeans = KMeans(n_clusters=self.n_clusters, n_init=20).fit(DataFrame(graphInfo))
        clusters, DCPos = find(kmeans, shortestMatrix)

        i = 1
        # for cluster in clusters:
        #     for node in cluster:
        #         print(self.topology)
        #         # self.topology.nodes[node][1]["clusterID"] = i
        #     i += 1

        k_list = [8, 6, 4]
        delta = 10 # percent, 0 ~ 100
        CENT_N_SERVER = 250
        solution = []
        best = CENT_N_SERVER
        for combine in itertools.combinations_with_replacement(k_list, self.n_clusters):
            n_server = [k**3//4 for k in combine]
            distance = abs(CENT_N_SERVER - sum(n_server))
            if(distance < CENT_N_SERVER * delta / 100 and distance < best):
                solution = combine
                best = distance
        DCArgs = list(solution)
        return DCPos, DCArgs


    def init_DCs(self):
        for i in range(len(self.DCPos)):
            self.DCs.append(DataCentre(self.DCPos[i], self.DC_topo(self.DCArgs[i])))


    def init_Ingresses(self):
        for i in range(len(self.IngressPos)):
            self.Ingresses.append(Ingress(self.IngressPos[i], self.IngressArgs[i]))


    def substrate_topo(self): pass


    def DC_topo(self, args): pass




class Abilene(Substrate):
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters):
        super().__init__(DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters)
        


    def substrate_topo(self):
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

        # for i in range(1, 13):
        #     if(i in self.DCPos):
        #         G.nodes[i]["role"] = "DataCentre"
        #     elif(i in self.IngressPos):
        #         G.nodes[i]["role"] = "Ingress"
        #     else:
        #         G.nodes[i]["role"] = "Switch"

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
        bwCapacity = 1000

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




class BigAbilene(Substrate):
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters):
        super().__init__(DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters)
        


    def substrate_topo(self):
        G = nx.Graph()

        G.add_edge(1, 2, capacity=self.linkCap, usage=0)
        G.add_edge(1, 4, capacity=self.linkCap, usage=0)
        G.add_edge(2, 3, capacity=self.linkCap, usage=0)
        G.add_edge(3, 6, capacity=self.linkCap, usage=0)
        G.add_edge(3, 12, capacity=self.linkCap, usage=0)
        G.add_edge(4, 12, capacity=self.linkCap, usage=0)
        G.add_edge(4, 38, capacity=self.linkCap, usage=0)
        G.add_edge(5, 6, capacity=self.linkCap, usage=0)
        G.add_edge(5, 12, capacity=self.linkCap, usage=0)
        G.add_edge(6, 8, capacity=self.linkCap, usage=0)
        G.add_edge(7, 8, capacity=self.linkCap, usage=0)
        G.add_edge(7, 10, capacity=self.linkCap, usage=0)
        G.add_edge(7, 12, capacity=self.linkCap, usage=0)
        G.add_edge(8, 9, capacity=self.linkCap, usage=0)
        G.add_edge(9, 10, capacity=self.linkCap, usage=0)
        G.add_edge(10, 11, capacity=self.linkCap, usage=0)
        G.add_edge(11, 14, capacity=self.linkCap, usage=0)
        G.add_edge(11, 17, capacity=self.linkCap, usage=0)
        G.add_edge(12, 13, capacity=self.linkCap, usage=0)
        G.add_edge(13, 14, capacity=self.linkCap, usage=0)
        G.add_edge(13, 15, capacity=self.linkCap, usage=0)
        G.add_edge(14, 16, capacity=self.linkCap, usage=0)
        G.add_edge(14, 17, capacity=self.linkCap, usage=0)
        G.add_edge(14, 21, capacity=self.linkCap, usage=0)
        G.add_edge(15, 16, capacity=self.linkCap, usage=0)
        G.add_edge(15, 37, capacity=self.linkCap, usage=0)
        G.add_edge(15, 25, capacity=self.linkCap, usage=0)
        G.add_edge(17, 18, capacity=self.linkCap, usage=0)
        G.add_edge(18, 21, capacity=self.linkCap, usage=0)
        G.add_edge(18, 22, capacity=self.linkCap, usage=0)
        G.add_edge(18, 19, capacity=self.linkCap, usage=0)
        G.add_edge(19, 20, capacity=self.linkCap, usage=0)
        G.add_edge(20, 22, capacity=self.linkCap, usage=0)
        G.add_edge(20, 23, capacity=self.linkCap, usage=0)
        G.add_edge(21, 24, capacity=self.linkCap, usage=0)
        G.add_edge(21, 25, capacity=self.linkCap, usage=0)
        G.add_edge(22, 23, capacity=self.linkCap, usage=0)
        G.add_edge(22, 24, capacity=self.linkCap, usage=0)
        G.add_edge(23, 24, capacity=self.linkCap, usage=0)
        G.add_edge(23, 31, capacity=self.linkCap, usage=0)
        G.add_edge(24, 26, capacity=self.linkCap, usage=0)
        G.add_edge(25, 26, capacity=self.linkCap, usage=0)
        G.add_edge(25, 39, capacity=self.linkCap, usage=0)
        G.add_edge(26, 27, capacity=self.linkCap, usage=0)
        G.add_edge(26, 39, capacity=self.linkCap, usage=0)
        G.add_edge(27, 29, capacity=self.linkCap, usage=0)
        G.add_edge(28, 29, capacity=self.linkCap, usage=0)
        G.add_edge(29, 30, capacity=self.linkCap, usage=0)
        G.add_edge(29, 33, capacity=self.linkCap, usage=0)
        G.add_edge(30, 31, capacity=self.linkCap, usage=0)
        G.add_edge(31, 32, capacity=self.linkCap, usage=0)
        G.add_edge(32, 33, capacity=self.linkCap, usage=0)
        G.add_edge(33, 34, capacity=self.linkCap, usage=0)
        G.add_edge(33, 36, capacity=self.linkCap, usage=0)
        G.add_edge(34, 35, capacity=self.linkCap, usage=0)
        G.add_edge(35, 36, capacity=self.linkCap, usage=0)
        G.add_edge(37, 38, capacity=self.linkCap, usage=0)
        G.add_edge(37, 39, capacity=self.linkCap, usage=0)
        

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
        bwCapacity = 1000

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




class Europe(Substrate):
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters):
        super().__init__(DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters)
        


    def substrate_topo(self):
        G = nx.Graph()

        G.add_edge(1, 2, capacity=self.linkCap, usage=0)
        G.add_edge(1, 3, capacity=self.linkCap, usage=0)
        G.add_edge(1, 4, capacity=self.linkCap, usage=0)
        G.add_edge(2, 5, capacity=self.linkCap, usage=0)
        G.add_edge(3, 5, capacity=self.linkCap, usage=0)
        G.add_edge(4, 5, capacity=self.linkCap, usage=0)
        G.add_edge(4, 11, capacity=self.linkCap, usage=0)
        G.add_edge(4, 21, capacity=self.linkCap, usage=0)
        G.add_edge(5, 6, capacity=self.linkCap, usage=0)
        G.add_edge(5, 14, capacity=self.linkCap, usage=0)
        G.add_edge(6, 7, capacity=self.linkCap, usage=0)
        G.add_edge(6, 8, capacity=self.linkCap, usage=0)
        G.add_edge(7, 10, capacity=self.linkCap, usage=0)
        G.add_edge(8, 9, capacity=self.linkCap, usage=0)
        G.add_edge(8, 10, capacity=self.linkCap, usage=0)
        G.add_edge(9, 14, capacity=self.linkCap, usage=0)
        G.add_edge(9, 16, capacity=self.linkCap, usage=0)
        G.add_edge(10, 16, capacity=self.linkCap, usage=0)
        G.add_edge(11, 12, capacity=self.linkCap, usage=0)
        G.add_edge(11, 14, capacity=self.linkCap, usage=0)
        G.add_edge(12, 13, capacity=self.linkCap, usage=0)
        G.add_edge(13, 17, capacity=self.linkCap, usage=0)
        G.add_edge(13, 21, capacity=self.linkCap, usage=0)
        G.add_edge(13, 20, capacity=self.linkCap, usage=0)
        G.add_edge(14, 15, capacity=self.linkCap, usage=0)
        G.add_edge(14, 17, capacity=self.linkCap, usage=0)
        G.add_edge(15, 16, capacity=self.linkCap, usage=0)
        G.add_edge(15, 18, capacity=self.linkCap, usage=0)
        G.add_edge(16, 29, capacity=self.linkCap, usage=0)
        G.add_edge(17, 18, capacity=self.linkCap, usage=0)
        G.add_edge(18, 19, capacity=self.linkCap, usage=0)
        G.add_edge(19, 20, capacity=self.linkCap, usage=0)
        G.add_edge(19, 29, capacity=self.linkCap, usage=0)
        G.add_edge(20, 22, capacity=self.linkCap, usage=0)
        G.add_edge(20, 24, capacity=self.linkCap, usage=0)
        G.add_edge(21, 22, capacity=self.linkCap, usage=0)
        G.add_edge(22, 23, capacity=self.linkCap, usage=0)
        G.add_edge(22, 34, capacity=self.linkCap, usage=0)
        G.add_edge(23, 24, capacity=self.linkCap, usage=0)
        G.add_edge(23, 26, capacity=self.linkCap, usage=0)
        G.add_edge(24, 25, capacity=self.linkCap, usage=0)
        G.add_edge(25, 27, capacity=self.linkCap, usage=0)
        G.add_edge(25, 31, capacity=self.linkCap, usage=0)
        G.add_edge(26, 32, capacity=self.linkCap, usage=0)
        G.add_edge(26, 27, capacity=self.linkCap, usage=0)
        G.add_edge(27, 28, capacity=self.linkCap, usage=0)
        G.add_edge(28, 31, capacity=self.linkCap, usage=0)
        G.add_edge(29, 30, capacity=self.linkCap, usage=0)
        G.add_edge(30, 31, capacity=self.linkCap, usage=0)
        G.add_edge(32, 33, capacity=self.linkCap, usage=0)
        G.add_edge(33, 37, capacity=self.linkCap, usage=0)
        G.add_edge(34, 35, capacity=self.linkCap, usage=0)
        G.add_edge(34, 36, capacity=self.linkCap, usage=0)
        G.add_edge(35, 37, capacity=self.linkCap, usage=0)
        G.add_edge(36, 37, capacity=self.linkCap, usage=0)

        # for i in range(1, 13):
        #     if(i in self.DCPos):
        #         G.nodes[i]["role"] = "DataCentre"
        #     elif(i in self.IngressPos):
        #         G.nodes[i]["role"] = "Ingress"
        #     else:
        #         G.nodes[i]["role"] = "Switch"

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
        bwCapacity = 1000

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




class France(Substrate):
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters):
        super().__init__(DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters)
        


    def substrate_topo(self):
        G = nx.Graph()

        G.add_edge(1, 2, capacity=self.linkCap, usage=0)
        G.add_edge(1, 25, capacity=self.linkCap, usage=0)
        G.add_edge(2, 25, capacity=self.linkCap, usage=0)
        G.add_edge(3, 7, capacity=self.linkCap, usage=0)
        G.add_edge(3, 9, capacity=self.linkCap, usage=0)
        G.add_edge(3, 10, capacity=self.linkCap, usage=0)
        G.add_edge(3, 25, capacity=self.linkCap, usage=0)
        G.add_edge(4, 5, capacity=self.linkCap, usage=0)
        G.add_edge(4, 6, capacity=self.linkCap, usage=0)
        G.add_edge(4, 25, capacity=self.linkCap, usage=0)
        G.add_edge(5, 6, capacity=self.linkCap, usage=0)
        G.add_edge(6, 7, capacity=self.linkCap, usage=0)
        G.add_edge(6, 8, capacity=self.linkCap, usage=0)
        G.add_edge(7, 8, capacity=self.linkCap, usage=0)
        G.add_edge(8, 9, capacity=self.linkCap, usage=0)
        G.add_edge(9, 25, capacity=self.linkCap, usage=0)
        G.add_edge(9, 16, capacity=self.linkCap, usage=0)
        G.add_edge(9, 15, capacity=self.linkCap, usage=0)
        G.add_edge(9, 22, capacity=self.linkCap, usage=0)
        G.add_edge(10, 11, capacity=self.linkCap, usage=0)
        G.add_edge(10, 12, capacity=self.linkCap, usage=0)
        G.add_edge(10, 16, capacity=self.linkCap, usage=0)
        G.add_edge(10, 17, capacity=self.linkCap, usage=0)
        G.add_edge(11, 12, capacity=self.linkCap, usage=0)
        G.add_edge(11, 15, capacity=self.linkCap, usage=0)
        G.add_edge(11, 16, capacity=self.linkCap, usage=0)
        G.add_edge(12, 16, capacity=self.linkCap, usage=0)
        G.add_edge(13, 14, capacity=self.linkCap, usage=0)
        G.add_edge(13, 15, capacity=self.linkCap, usage=0)
        G.add_edge(14, 15, capacity=self.linkCap, usage=0)
        G.add_edge(15, 16, capacity=self.linkCap, usage=0)
        G.add_edge(15, 18, capacity=self.linkCap, usage=0)
        G.add_edge(15, 20, capacity=self.linkCap, usage=0)
        G.add_edge(15, 21, capacity=self.linkCap, usage=0)
        G.add_edge(15, 22, capacity=self.linkCap, usage=0)
        G.add_edge(16, 17, capacity=self.linkCap, usage=0)
        G.add_edge(18, 19, capacity=self.linkCap, usage=0)
        G.add_edge(19, 20, capacity=self.linkCap, usage=0)
        G.add_edge(20, 21, capacity=self.linkCap, usage=0)
        G.add_edge(20, 22, capacity=self.linkCap, usage=0)
        G.add_edge(20, 24, capacity=self.linkCap, usage=0)
        G.add_edge(22, 23, capacity=self.linkCap, usage=0)
        G.add_edge(22, 24, capacity=self.linkCap, usage=0)
        G.add_edge(23, 24, capacity=self.linkCap, usage=0)

        # for i in range(1, 13):
        #     if(i in self.DCPos):
        #         G.nodes[i]["role"] = "DataCentre"
        #     elif(i in self.IngressPos):
        #         G.nodes[i]["role"] = "Ingress"
        #     else:
        #         G.nodes[i]["role"] = "Switch"

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
        bwCapacity = 1000

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




class Atlanta(Substrate):
    def __init__(self, DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters):
        super().__init__(DCPos, IngressPos, linkCap, DCArgs, IngressArgs, n_clusters)
        


    def substrate_topo(self):
        G = nx.Graph()

        G.add_edge(1, 6, capacity=self.linkCap, usage=0)
        G.add_edge(1, 7, capacity=self.linkCap, usage=0)
        G.add_edge(1, 8, capacity=self.linkCap, usage=0)
        G.add_edge(2, 3, capacity=self.linkCap, usage=0)
        G.add_edge(2, 5, capacity=self.linkCap, usage=0)
        G.add_edge(2, 6, capacity=self.linkCap, usage=0)
        G.add_edge(3, 5, capacity=self.linkCap, usage=0)
        G.add_edge(3, 8, capacity=self.linkCap, usage=0)
        G.add_edge(4, 5, capacity=self.linkCap, usage=0)
        G.add_edge(4, 6, capacity=self.linkCap, usage=0)
        G.add_edge(6, 13, capacity=self.linkCap, usage=0)
        G.add_edge(7, 10, capacity=self.linkCap, usage=0)
        G.add_edge(7, 14, capacity=self.linkCap, usage=0)
        G.add_edge(8, 9, capacity=self.linkCap, usage=0)
        G.add_edge(8, 15, capacity=self.linkCap, usage=0)
        G.add_edge(9, 10, capacity=self.linkCap, usage=0)
        G.add_edge(9, 12, capacity=self.linkCap, usage=0)
        G.add_edge(9, 15, capacity=self.linkCap, usage=0)
        G.add_edge(10, 12, capacity=self.linkCap, usage=0)
        G.add_edge(11, 13, capacity=self.linkCap, usage=0)
        G.add_edge(11, 14, capacity=self.linkCap, usage=0)
        G.add_edge(13, 14, capacity=self.linkCap, usage=0)

        # for i in range(1, 13):
        #     if(i in self.DCPos):
        #         G.nodes[i]["role"] = "DataCentre"
        #     elif(i in self.IngressPos):
        #         G.nodes[i]["role"] = "Ingress"
        #     else:
        #         G.nodes[i]["role"] = "Switch"

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
        bwCapacity = 1000

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
