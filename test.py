from http import server
from library import *

import networkx as nx
import numpy as np
import simpy
import copy
import matplotlib.pyplot as plt
import sys
import os
import csv

import json
import random

f = open('log.txt', 'w+', newline="\n")

diction = {
    "name": "John",
    "age": 34,
    "marks": [9, 8, 10]
}

diction_2 = [
    {
        "name": "John",
        "age": 12
    },
    {
        "name": "Anna",
        "age": 10
    },
    {
        "name": "Petre",
        "age": 13
    },
    {
        "name": "Linda",
        "age": 9
    },
]
def age(diction): return diction["age"]


switchSpecs = {
    "basePower": 39,
    "portPower": [0.42, 0.48, 0.9]
}

serverSpecs = {
    "RAM": [4, 0],
    # "RAM": {"cap": 4, "usage": 0},
    "power": [205.1, 232.9, 260.7, 288.6, 316.4]
}

G = fat_tree(4, switchSpecs, serverSpecs)
for i in range(21, 25):
    print(G.nodes[i]["RAM"], end=" ")

for i in range(21, 25):
    G.nodes[i]["RAM"] = [
        G.nodes[i]["RAM"][0],
        G.nodes[i]["RAM"][1] + 1
    ]
    
print()
for i in range(21, 25):
    print(G.nodes[i]["RAM"], end=" ")


# for i in range (21, 37):
#     print(G.nodes[i]["RAM"][1])



# time = [1, 2, 3, 4, 5, 6, 7, 8]
# power = [8, 7, 6, 5, 4, 3, 2, 1]
# u = [4, 4, 4, 4, 4, 4, 4, 4, 4]

# with open("results/result_event.csv", "r") as f:
#     data = csv.reader(f)
#     for row in data:
#         if row[2] == "deployed" or row[2] == "remove":
#             time.append(int(row[1]))
#             power.append(float(row[9]))

# plt.plot(time, power, u)
# plt.show()


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



# def set_node_attr(id, attributes):
#     for attr in attributes.items():
#         G.nodes[id][attr[0]] = attr[1]



