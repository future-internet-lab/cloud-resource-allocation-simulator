from re import A
from library import *

import networkx as nx
import numpy as np
import simpy
import matplotlib.pyplot as plt

import json
import random



# def fat_tree(k, switchSpecs, serverSpecs):
#     lastCore = int((k/2)**2)
#     lastAggre = int(lastCore + k**2 / 2)
#     lastEdge = int(lastAggre  + k**2 / 2)
#     lastServer = int(lastEdge + k**3 / 4)

#     G = nx.Graph()

#     for i in range(lastServer): G.add_node(i + 1)

#     for pod in range(k): # create all links
#         for aggre in range(int(k / 2)):
#             for i in range(int(k / 2)):
#                 G.add_edge(int(lastCore+pod*k/2+aggre+1), int(2*lastCore/k*aggre+i+1), bw=1000)
#                 G.add_edge(int(lastCore+pod*k/2+aggre+1), int(lastAggre+pod*k/2+i+1), bw=1000)
#                 G.add_edge(int(lastAggre+pod*k/2+i+1), int(lastEdge+pod*k**2/4+k/2*i+aggre+1), bw=1000)


#     for type in ["core", "aggregation", "edge"]:
#         if(type == "core"): rangeSwitch = [1, lastCore + 1]
#         if(type == "aggregation"): rangeSwitch = [lastCore + 1, lastAggre + 1]
#         if(type == "edge"): rangeSwitch = [lastAggre + 1, lastEdge + 1]

#         for switchID in range(*rangeSwitch):
#             G.nodes[switchID]['model'] = 'switch'
#             G.nodes[switchID]['tag'] = type
#             for index in switchSpecs:
#                 G.nodes[switchID][index] = switchSpecs[index]

#         for serverID in range(lastEdge + 1, lastServer + 1):
#             G.nodes[serverID]['model'] = 'server'
#             for index in serverSpecs:
#                 G.nodes[serverID][index] = serverSpecs[index]

#     return G

def func():
    return [1, 2]

switchSpecs = {
    "basePower": 39,
    "portPower": [0.42, 0.48, 0.9]
}

serverSpecs = {
    "RAM": 4,
    "power": [205.1, 232.9, 260.7, 288.6, 316.4]
}
G = fat_tree(4, switchSpecs, serverSpecs)

for i in range(0):
    print("a")


# try:
#     route = nx.shortest_path(G, 21, 34)
# except:
#     route = False
# print("route =", route)
# print("G =", G)

# G.edges[13, 5]["bw"] = 500
# G.edges[7, 15]["bw"] = 500

# for link in list(tempG.edges.data()):
#     if(link[2]["bw"] < 1000): tempG.remove_edge(link[0], link[1])

# route = nx.shortest_path(G, 21, 25)
# print("route =", route)
# print("G =", G)


# servers = []
# for node in list(G.nodes.data()):
#     if(node[1]["model"] == "server"):
#         servers.append(node[1]["RAM"])
# print("servers =", servers)


# route = nx.shortest_path(tempG, 21, 25)
# print("route =", route)
# print("G =", G)
# print("tempG =", tempG)

