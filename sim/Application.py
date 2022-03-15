from abc import ABC, abstractmethod

import numpy as np
import networkx as nx
import matplotlib.pyplot as plt



class Application(ABC):
    def __init__(self, name, distribution, selector, *arg):
        self.name = name
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
        n_VNFs = np.random.randint(self.arg[1][0], self.arg[1][1] + 1)
        G = nx.waxman_graph(n_VNFs, self.arg[3][0], self.arg[3][1])
        for (i, j) in G.edges:
            G[i][j]['bw'] = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)
            G[i][j]['route'] = []
        for i in range(n_VNFs):
            G.nodes[i]["SFC"] = id
            G.nodes[i]["RAM"] = 1
            G.nodes[i]["server"] = False
        out_link = np.random.randint(self.arg[2][0], self.arg[2][1] + 1)
        time_to_live = round(np.random.exponential(scale=self.arg[0]))

        # nx.draw(G, with_labels=True, font_weight='bold')
        # plt.show()
        # exit()

        return [G, out_link, time_to_live]