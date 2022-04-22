from abc import ABC, abstractmethod

import numpy as np
import networkx as nx
import matplotlib.pyplot as plt



class Application(ABC):
    def __init__(self, distribution, selector, *arg):
        self.name = self.__class__.__name__
        self.distribution = distribution
        self.selector = selector
        
        # custom argument for custom SFC
        self.arg = arg

        # list of SFCs of this app
        self.SFCs = []


    # create SFC by a pattern (SFC topo)
    @abstractmethod
    def create_SFC(self): pass




class SimpleApp(Application):
    """
    this app create SFC which has linear-topo
    """
    def __init__(self, name, distribution, selector, *arg):
        super().__init__(name, distribution, selector, *arg)


    def create_SFC(self, id):
        n_VNFs = np.random.randint(self.arg[1][0], self.arg[1][1] + 1)
        G = nx.Graph()
        for i in range(n_VNFs - 1):
            G.add_edge(i, i + 1, bw=np.random.randint(self.arg[2][0], self.arg[2][1] + 1))
            G.nodes[i]["SFC"] = id
            G.nodes[i]["RAM"] = 1
        G.nodes[n_VNFs - 1]["SFC"] = id
        G.nodes[n_VNFs - 1]["RAM"] = 1
        out_link = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)
        time_to_live = round(np.random.exponential(scale=self.arg[0]))
        return [G, out_link, time_to_live]




class WaxmanApp(Application):
    """
    create SFC which waxman random topo
    """
    def __init__(self, name, distribution, selector, *arg):
        super().__init__(name, distribution, selector, *arg)


    def create_SFC(self, id):
        # n_VNFs = np.random.randint(self.arg[1][0], self.arg[1][1] + 1)
        n_VNFs = 20 - id // 50
        leng = 0
        while leng == 0:
            G = nx.waxman_graph(n_VNFs, self.arg[4][0], self.arg[4][1])
            leng = len(G.edges.data())

        for (i, j) in G.edges:
            G[i][j]['demand'] = np.random.randint(self.arg[3][0], self.arg[3][1] + 1)
            G[i][j]['route'] = []
        demand = 0
        for i in range(n_VNFs):
            G.nodes[i]["SFC"] = id
            G.nodes[i]["demand"] = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)
            demand += G.nodes[i]["demand"]
            G.nodes[i]["server"] = False
            G.nodes[i].pop("pos", None)
        out_link = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)
        time_to_live = round(np.random.exponential(scale=self.arg[0]))
        if time_to_live == 0: time_to_live = 1

        return [G, out_link, demand, time_to_live]




class VNFGApp(Application):
    def __init__(self, name, distribution, selector, *arg):
        super().__init__(name, distribution, selector, *arg)

    def create_SFC(self, id):
        n_VNFs = np.random.randint(self.arg[1][0], self.arg[1][1] + 1)
        G = nx.Graph()
        G_result = nx.Graph()
        all_nodes = [i for i in range(n_VNFs)]
        G.add_nodes_from(all_nodes)
        G_result.add_nodes_from(all_nodes)

        for i in range(n_VNFs):
            for j in range(n_VNFs-i-1):
                G.add_edge(i,i+j+1)
                G[i][i+j+1]['demand'] = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)

        temp = all_nodes[0]
        all_nodes = all_nodes[1:]

        while len(all_nodes) > 0:
            bw_temp = []
            for i in all_nodes:
                G.edges[temp,i]['demand']
                bw_temp.append(G.edges[temp,i]['demand'])
            # print(min(bw_temp),np.argmin(bw_temp),all_nodes[np.argmin(bw_temp)])
            G_result.add_edge(temp, all_nodes[np.argmin(bw_temp)])
            G_result[temp][all_nodes[np.argmin(bw_temp)]]['demand'] = min(bw_temp)
            G_result[temp][all_nodes[np.argmin(bw_temp)]]['route'] = []
            if len(all_nodes) > 1:
                G_result[temp][all_nodes[np.argmin(bw_temp)]]['split'] = all_nodes[np.argmin(bw_temp)]
            else: 
                G_result[temp][all_nodes[np.argmin(bw_temp)]]['split'] = ''
            temp = all_nodes[np.argmin(bw_temp)]
            all_nodes.remove(all_nodes[np.argmin(bw_temp)])

        for i in range(n_VNFs):
            G_result.nodes[i]["SFC"] = id
            G_result.nodes[i]["demand"] = 1
            G_result.nodes[i]["server"] = False
            
        out_link = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)
        time_to_live = round(np.random.exponential(scale=self.arg[0]))
        if time_to_live == 0: time_to_live = 1
        
        return [G_result, out_link, time_to_live]
