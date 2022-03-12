import json
import matplotlib as plt
import numpy as np



f = open("results/result_topo.txt", "r")
data = json.loads(f.read())

event = data[0]
listbw = []
for vl in event["vlink"]:
    listbw.append(vl["bw"])
print("list vlink bw = ", listbw)


event = data[1]
listServer = []
for node in event["node"]:
    if(node["model"] == "server"):
        if(node["deployed"]):
            listServer.append(node)
            # print(node["id"], node["deployed"], node["RAM"])

for i in range(len(listServer) - 1):
    s1 = listServer[i]['id']
    s2 = listServer[i + 1]['id']
    # print(s1, s2)
    # link = {}
    for _link in event['link']:
        # print(_link['s'], _link['d'])
        if((_link['s'] == s1 and _link['d'] == s2) or (_link['s'] == s2 and _link['d'] == s1)):
            print(_link['s'], _link['d'])
            # link = _link
            break
        # if((_link['s'] == 1 and _link['d'] == 26)):
        #     print(1)
    # print(f"link {s1} - {s2} : {link}")



f.close()
