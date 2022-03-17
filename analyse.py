import json
import matplotlib as plt
import numpy as np



f = open("results/result_test.json", "r")
data = json.loads(f.read())

for DC in data:
    servers = []
    RAM = 0
    for node in DC:
        if(node[1]["model"] == "server"):
            servers.append(node)
            RAM += node[1]["RAM"][0] - node[1]["RAM"][1]
    print(RAM)



f.close()
