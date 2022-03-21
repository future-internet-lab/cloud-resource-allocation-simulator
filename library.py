import networkx as nx



def fat_tree(k, switchSpecs, serverSpecs):
    """
    k = int
    switchSpecs = {
        "basePower": 39,
        "portPower": [0.42, 0.48, 0.9]
    }

    serverSpecs = {
        "RAM": 4,
        "power": [205.1, 232.9, 260.7, 288.6, 316.4]
    }
    """
    lastCore = int((k/2)**2)
    lastAggre = int(lastCore + k**2 / 2)
    lastEdge = int(lastAggre  + k**2 / 2)
    lastServer = int(lastEdge + k**3 / 4)

    G = nx.Graph()

    for i in range(lastServer): G.add_node(i + 1)

    for pod in range(k): # create all links
        for aggre in range(int(k / 2)):
            for i in range(int(k / 2)):
                G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1), capacity=1000, usage=0)
                G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1), capacity=1000, usage=0)
                G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1), capacity=1000, usage=0)


    for type in ["core", "aggregation", "edge"]:
        if(type == "core"): rangeSwitch = [1, lastCore + 1]
        if(type == "aggregation"): rangeSwitch = [lastCore + 1, lastAggre + 1]
        if(type == "edge"): rangeSwitch = [lastAggre + 1, lastEdge + 1]

        for switchID in range(*rangeSwitch):
            G.nodes[switchID]['model'] = 'switch'
            G.nodes[switchID]['tag'] = type
            G.nodes[switchID]['state'] = False
            for index in switchSpecs:
                G.nodes[switchID][index] = switchSpecs[index]

        for serverID in range(lastEdge + 1, lastServer + 1):
            G.nodes[serverID]['model'] = 'server'
            G.nodes[serverID]['deployed'] = []
            G.nodes[serverID]['state'] = True
            for index in serverSpecs:
                G.nodes[serverID][index] = serverSpecs[index]

    return G


def DistributedTopo():
    G = nx.Graph()

    BW = [2000, 0]

    G.add_edge(1, 2, capacity=BW[0], usage=BW[1])
    G.add_edge(1, 12, capacity=BW[0], usage=BW[1])
    G.add_edge(2, 3, capacity=BW[0], usage=BW[1])
    G.add_edge(2, 12, capacity=BW[0], usage=BW[1])
    G.add_edge(3, 5, capacity=BW[0], usage=BW[1])
    G.add_edge(3, 10, capacity=BW[0], usage=BW[1])
    G.add_edge(4, 5, capacity=BW[0], usage=BW[1])
    G.add_edge(4, 6, capacity=BW[0], usage=BW[1])
    G.add_edge(5, 9, capacity=BW[0], usage=BW[1])
    G.add_edge(6, 7, capacity=BW[0], usage=BW[1])
    G.add_edge(7, 9, capacity=BW[0], usage=BW[1])
    G.add_edge(8, 9, capacity=BW[0], usage=BW[1])
    G.add_edge(9, 10, capacity=BW[0], usage=BW[1])
    G.add_edge(10, 11, capacity=BW[0], usage=BW[1])
    G.add_edge(11, 12, capacity=BW[0], usage=BW[1])

    for i in range(1, 13):
        if(i in [1, 4, 6, 11]):
            G.nodes[i]["role"] = "DataCentre"
        elif(i in [5, 7, 9, 10]):
            G.nodes[i]["role"] = "Ingress"
        else:
            G.nodes[i]["role"] = "Switch"

    return G
