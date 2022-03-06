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
                G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1), bw=1000, usage=0)
                G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1), bw=1000, usage=0)
                G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1), bw=1000, usage=0)


    for type in ["core", "aggregation", "edge"]:
        if(type == "core"): rangeSwitch = [1, lastCore + 1]
        if(type == "aggregation"): rangeSwitch = [lastCore + 1, lastAggre + 1]
        if(type == "edge"): rangeSwitch = [lastAggre + 1, lastEdge + 1]

        for switchID in range(*rangeSwitch):
            G.nodes[switchID]['model'] = 'switch'
            G.nodes[switchID]['tag'] = type
            G.nodes[switchID]['status'] = "off"
            for index in switchSpecs:
                G.nodes[switchID][index] = switchSpecs[index]

        for serverID in range(lastEdge + 1, lastServer + 1):
            G.nodes[serverID]['model'] = 'server'
            G.nodes[serverID]['deployed'] = []
            for index in serverSpecs:
                G.nodes[serverID][index] = serverSpecs[index]

    return G