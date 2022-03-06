from abc import ABC, abstractmethod

import numpy as np
import networkx as nx



# counter = 0
class SFC(ABC):
    def __init__(self):
        # global counter
        # counter += 1
        self.id = 0
        self.time_to_live = 0
        self.app = ""
        self.structure = self.initial()
        return self

    @abstractmethod
    def initial(self): pass






class SimpleSFC(SFC):
    def __init__(self):
        super().__init__()

    def initial(self):
        n_VNFs = np.random.randint(4, 21)
        G = nx.Graph()
        for i in range(n_VNFs - 1): 
            G.add_edge(i, i + 1, bw=np.random.randint(10, 91))
            G.nodes[i]["SFC"] = self.id
            G.nodes[i]["RAM"] = 1
        G.nodes[n_VNFs - 1]["SFC"] = self.id
        G.nodes[n_VNFs - 1]["RAM"] = 1
        self.time_to_live = round(np.random.exponential(scale=120))
        return G

