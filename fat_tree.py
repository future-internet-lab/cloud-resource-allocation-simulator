from typing import *
from networkx.readwrite import json_graph

import os
import networkx as nx
import matplotlib.pyplot as plt
import json

def create_fat_tree(k, bw, pr, specs):
    """
    k = 4
    bw = {"L1": 3, "L2": 2, "L3": 1}
    pr = {"L1": 10, "L2": 5, "L3": 2}
    specs = {
        "IPT": [10000000000, 5000000000, 300000000, 200000000],
        "RAM": [40000, 20000, 10000, 8000],
        "COST": [10, 5, 3, 2],
        "WATT": [20.0, 10.0, 8.0, 15.0]
        "...": [..., ..., ..., ...]
    }
    """

    def add_node_spec(specName, valueName):
        nodeSpec = {} 
        for i in range(lastCore): nodeSpec[i+1] = valueName[0]
        for i in range(lastCore, lastAggre): nodeSpec[i+1] = valueName[1]
        for i in range(lastAggre, lastEdge): nodeSpec[i+1] = valueName[2]
        for i in range(lastEdge, lastServer): nodeSpec[i+1] = valueName[3]
        nx.set_node_attributes(G, values=nodeSpec, name=specName)

    lastCore = int((k/2)**2)
    lastAggre = int(lastCore + k**2 / 2)
    lastEdge = int(lastAggre  + k**2 / 2)
    lastServer = int(lastEdge + k**3 / 4)

    G = nx.Graph()

    for i in range(lastServer): G.add_node(i + 1)

    for pod in range(k): # create all links
        for aggre in range(int(k / 2)):
            for i in range(int(k / 2)):
                G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1),
                    BW=bw["L1"], PR=pr["L1"])
                G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1),
                    BW=bw["L2"], PR=pr["L2"])
                G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1),
                    BW=bw["L3"], PR=pr["L3"])

    # add mandatory spec of node: id
    for i in range(lastServer): G.nodes[i+1]["id"] = i + 1

    # add mandatory spec of node: type (rule in fat tree topology)
    add_node_spec("type", ["CoreSwitch", "AggregationSwitch", "EdgeSwitch", "Server"])

    # add other specs
    for idx in specs:
        add_node_spec(idx, specs[idx])

    # standardize on YAFS topology json form
    data = {}
    data["entity"] = []
    data["link"] = []
    for i in range(lastServer): data["entity"].append(dict(G.nodes[i+1]))
    for i in range(G.number_of_edges()):
        temp = {}
        temp["s"] = list(G.edges.data())[i][0]
        temp["d"] = list(G.edges.data())[i][1]
        temp["BW"] = list(G.edges.data())[i][2]["BW"]
        temp["PR"] = list(G.edges.data())[i][2]["PR"]
        data["link"].append(temp)

    return data


def fat_tree(k, switchSpec, serverSpec):
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

    for type,specs in switchSpec.items():
        if(type == "core"): rangeSwitch = [1, lastCore + 1]
        if(type == "aggregation"): rangeSwitch = [lastCore + 1, lastAggre + 1]
        if(type == "edge"): rangeSwitch = [lastAggre + 1, lastEdge + 1]

        for id in range(*rangeSwitch):
            G.nodes[id]['model'] = 'switch'
            G.nodes[id]['tag'] = type
            for index in specs:
                G.nodes[id][index] = specs[index]

    for id in range(lastEdge + 1, lastServer + 1):
        G.nodes[id]['model'] = 'server'
        for index in serverSpec:
            G.nodes[id][index] = serverSpec[index]

    return G


def create_json_topology():
    """
       TOPOLOGY DEFINITION

       Some attributes of fog entities (nodes) are approximate
       """

    ## MANDATORY FIELDS
    topology_json = {}
    topology_json["entity"] = []
    topology_json["link"] = []

    cloud_dev    = {"id": 0, "model": "cloud","mytag":"cloud", "IPT": 5000 * 10 ** 6, "RAM": 40000,"COST": 3,"WATT":20.0}
    sensor_dev   = {"id": 1, "model": "sensor-device", "IPT": 100* 10 ** 6, "RAM": 4000,"COST": 3,"WATT":40.0}
    actuator_dev = {"id": 2, "model": "actuator-device", "IPT": 100 * 10 ** 6, "RAM": 4000,"COST": 3, "WATT": 40.0}

    link1 = {"s": 0, "d": 1, "BW": 1, "PR": 10}
    link2 = {"s": 0, "d": 2, "BW": 1, "PR": 1}

    topology_json["entity"].append(cloud_dev)
    topology_json["entity"].append(sensor_dev)
    topology_json["entity"].append(actuator_dev)
    topology_json["link"].append(link1)
    topology_json["link"].append(link2)

    return topology_json

if __name__ == "__main__":
    os.system("cls")


    k = 4

    switchSpec = {
        "core": {
            "port": [
                    {
                        "name": "10Mbps",
                        "speed": 10,
                        "quantity": 10,
                        "power": 0.42
                    },
                    {
                        "name": "100Mbps",
                        "speed": 100,
                        "quantity": 10,
                        "power": 0.48
                    },
                    {
                        "name": "1Gbps",
                        "speed": 1024,
                        "quantity": 10,
                        "power": 0.9
                    }
                ],
            "basePower": 39
        },
        "aggregation": {
            "port": [
                    {
                        "name": "10Mbps",
                        "speed": 10,
                        "quantity": 10,
                        "power": 0.42
                    },
                    {
                        "name": "100Mbps",
                        "speed": 100,
                        "quantity": 10,
                        "power": 0.48
                    },
                    {
                        "name": "1Gbps",
                        "speed": 1024,
                        "quantity": 10,
                        "power": 0.9
                    }
                ],
            "basePower": 39
        },
        "edge": {
            "port": [
                    {
                        "name": "10Mbps",
                        "speed": 10,
                        "quantity": 10,
                        "power": 0.42
                    },
                    {
                        "name": "100Mbps",
                        "speed": 100,
                        "quantity": 10,
                        "power": 0.48
                    },
                    {
                        "name": "1Gbps",
                        "speed": 1024,
                        "quantity": 10,
                        "power": 0.9
                    }
                ],
            "basePower": 39
        }
    }

    serverSpec = {
        "port": [
                    {
                        "name": "100Mbps",
                        "speed": 100,
                        "quantity": 1
                    },
                ],
        "VMs": 4,
        "power": [205.1, 232.9, 260.7, 288.6, 316.4]
    }


    k = 4
    bw = {"L1": 3, "L2": 2, "L3": 1}
    pr = {"L1": 10, "L2": 5, "L3": 2}
    specs = {
        "IPT": [10000000000, 5000000000, 300000000, 200000000],
        "RAM": [40000, 20000, 10000, 8000],
        "COST": [10, 5, 3, 2],
        "WATT": [20.0, 10.0, 8.0, 15.0]
    }

    data = create_fat_tree(k, bw, pr, specs)
    print(data)

    # result = json.dumps(data)
    # f = open("log.txt", mode="a")
    # f.write(result)
    # f.close()

    # d = create_json_topology()
    # print(d)





    # L1_BW = 3
    # L1_PR = 10
    # L2_BW = 2
    # L2_PR = 5
    # L3_BW = 1
    # L3_PR = 2

    # lastCore = int((k/2)**2)
    # lastAggre = int(lastCore + k**2 / 2)
    # lastEdge = int(lastAggre  + k**2 / 2)
    # lastServer = int(lastEdge + k**3 / 4)

    # G = nx.Graph()

    # for i in range(lastServer): G.add_node(i + 1)

    # for pod in range(k): # loop through each pod
    #     for aggre in range(int(k / 2)):
    #         for i in range(int(k / 2)):
    #             G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1), BW=L1_BW, PR=L1_PR)
    #             G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1), BW=L2_BW, PR=L2_PR)
    #             G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1), BW=L3_BW, PR=L3_PR)
    #             print(int(lastCore+pod*k/2+aggre+1), end=' ')
    #             print(int(2*lastCore/k*aggre+i+1), end=' ')
    #             print(int(lastAggre+pod*k/2+i+1), end=' ')
    #             print(int(lastEdge+pod*k**2/4+k/2*i+aggre+1), end=' ')
    #             print()

    # for i in range(lastServer): G.nodes[i+1]["id"] = i + 1
    # add_node_spec("type", ["CoreSwitch", "AggregationSwitch", "EdgeSwitch", "Server"])
    # add_node_spec("IPT", [10000000000, 5000000000, 300000000, 200000000])
    # add_node_spec("RAM", [40000, 20000, 10000, 8000])
    # add_node_spec("COST", [10, 5, 3, 2])
    # add_node_spec("WATT", [20.0, 10.0, 8.0, 15.0])


    # data = {}
    # data["entity"] = []
    # data["link"] = []
    # for i in range(lastServer): data["entity"].append(dict(G.nodes[i+1]))

    # for i in range(G.number_of_edges()):
    #     temp = {}
    #     temp["s"] = list(G.edges.data())[i][0]
    #     temp["d"] = list(G.edges.data())[i][1]
    #     temp["BW"] = list(G.edges.data())[i][2]["BW"]
    #     temp["PR"] = list(G.edges.data())[i][2]["PR"]
    #     data["link"].append(temp)
