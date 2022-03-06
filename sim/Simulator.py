from http import server
from sim.SFC import *
from sim.Logger import *

import copy
import simpy
import sys
import json



class Simulator():
    def __init__(self, topology, apps, folder_log):
        self.env = simpy.Environment()
        self.topology = topology
        self.apps = apps
        self.requestsRoom = simpy.Store(self.env)
        self.readyRoom = simpy.Store(self.env)
        self.SFCs = 0
        self.logger = Logger(folder_log)


    def generator(self, app):
        while True:
            self.SFCs += 1
            sfc = {
                "id": self.SFCs,
                "app": app.name,
                "structure": app.generate_SFC(self.SFCs)[0], #SFC topo
                "TTL": app.generate_SFC(self.SFCs)[1]
            }
            app.SFCs.append(sfc)

            # change sfc from python dictionary to json format to logging to .csv
            structure = {"vnf": [], "vlink": []}
            for vnf in list(sfc["structure"].nodes.data()):
                structure["vnf"].append({"id": vnf[0], **vnf[1]})
            for vlink in list(sfc["structure"].edges.data()):
                structure["vlink"].append({"s": vlink[0], "d": vlink[1], **vlink[2]})

            # duration between 2 requests creation
            duration = app.distribution.next()
            self.logger.log_event(self.time(), self.logger.CREATE, app.name, sfc["id"], len(sfc["structure"].nodes), sfc["TTL"], json.dumps(structure))
            self.requestsRoom.put(sfc)
            yield self.env.timeout(duration)


    def handler(self, app): # consider if it's possible to implement sfc
        while True:
            sfc = yield self.requestsRoom.get()
            
            deploy = app.selector.analyse(self.topology, sfc)
            if(deploy): # accept
                self.deployer(app, deploy)
                self.logger.log_event(self.time(), self.logger.HANDLER_SUCCESS, SFC=sfc["id"])
                self.readyRoom.put(deploy)
            else: # drop
                self.logger.log_event(self.time(), self.logger.HANDLER_FAIL, SFC=sfc["id"])


    def deployer(self, app, deploy): # update physical topology's specs 
        # while True:
        #     deploy = yield self.readyRoom.get()
            sfcID = deploy['sfc']
            for req in deploy['node']:
                self.topology.nodes[req[1]]["RAM"] -= app.SFCs[sfcID-1]["structure"].nodes[req[0]]["RAM"]

                self.topology.nodes[req[1]]['deployed'].append([sfcID, req[0]])
                
                app.SFCs[sfcID-1]["structure"].nodes[req[0]]["onServer"] = req[1]

            for linkreq in deploy['link']:
                for i in range(len(linkreq['route']) - 1):
                    self.topology.edges[linkreq['route'][i], linkreq['route'][i+1]]['usage'] += linkreq["bw"]
                    
                    # turn on switch
                    if(self.topology.nodes[linkreq['route'][i]]["model"] == "switch"):
                        self.topology.nodes[linkreq['route'][i]]["status"] = "on"

            # change topo from python dictionary to json format to logging to .csv
            pTopo = {"node": [], "link": []}
            for node in list(self.topology.nodes.data()):
                pTopo["node"].append({"id": node[0], **node[1]})
            for plink in list(self.topology.edges.data()):
                pTopo["link"].append({"s": plink[0], "d": plink[1], **plink[2]})

            self.logger.log_event(self.time(), self.logger.DEPLOYED, SFC=sfcID, topo=json.dumps(pTopo))
            self.logger.log_energy(self.time(), *(self.energy()))

            self.env.process(self.remover(app, deploy))


    # def deployer(self, app): # update physical topology's specs 
    #     while True:
    #         deploy = yield self.readyRoom.get()
    #         sfcID = deploy['sfc']
    #         for req in deploy['node']:
    #             self.topology.nodes[req[1]]["RAM"] -= app.SFCs[sfcID-1]["structure"].nodes[req[0]]["RAM"]

    #             self.topology.nodes[req[1]]['deployed'].append([sfcID, req[0]])
                
    #             app.SFCs[sfcID-1]["structure"].nodes[req[0]]["onServer"] = req[1]

    #         for linkreq in deploy['link']:
    #             for i in range(len(linkreq['route']) - 1):
    #                 self.topology.edges[linkreq['route'][i], linkreq['route'][i+1]]['usage'] += linkreq["bw"]
                    
    #                 # turn on switch
    #                 if(self.topology.nodes[linkreq['route'][i]]["model"] == "switch"):
    #                     self.topology.nodes[linkreq['route'][i]]["status"] = "on"

    #         # change topo from python dictionary to json format to logging to .csv
    #         pTopo = {"node": [], "link": []}
    #         for node in list(self.topology.nodes.data()):
    #             pTopo["node"].append({"id": node[0], **node[1]})
    #         for plink in list(self.topology.edges.data()):
    #             pTopo["link"].append({"s": plink[0], "d": plink[1], **plink[2]})

    #         self.logger.log_event(self.time(), self.logger.DEPLOYED, SFC=sfcID, topo=json.dumps(pTopo))
    #         self.logger.log_energy(self.time(), *(self.energy()))

    #         self.env.process(self.remover(app, deploy))


    def remover(self, app, deploy): # free resources after TTL
        yield self.env.timeout(app.SFCs[deploy['sfc']-1]["TTL"])

        sfcID = deploy['sfc']
        for req in deploy['node']:
            self.topology.nodes[req[1]]["RAM"] += app.SFCs[sfcID-1]["structure"].nodes[req[0]]["RAM"]
            self.topology.nodes[req[1]]['deployed'].remove([sfcID, req[0]])
            app.SFCs[sfcID-1]["structure"].nodes[req[0]]["onServer"] = req[1]

        # change topo from python dictionary to json format to logging to .csv
        for linkreq in deploy['link']:
            for i in range(len(linkreq['route']) - 1):
                self.topology.edges[linkreq['route'][i], linkreq['route'][i+1]]['usage'] -= linkreq["bw"]

                # consider to turn off switch
                nodeID = linkreq['route'][i]
                if(self.topology.nodes[nodeID]["model"] == "switch"):
                    for neighborLink in self.topology.adj[nodeID].items():
                        if(neighborLink[1]['usage'] > 0):
                            self.topology.nodes[nodeID]["status"] = "on"
                            continue
                        self.topology.nodes[nodeID]["status"] = "off"

        pTopo = {"node": [], "link": []}
        for node in list(self.topology.nodes.data()):
            pTopo["node"].append({"id": node[0], **node[1]})
        for plink in list(self.topology.edges.data()):
            pTopo["link"].append({"s": plink[0], "d": plink[1], **plink[2]})

        self.logger.log_event(self.time(), self.logger.REMOVE, SFC=deploy['sfc'], topo=json.dumps(pTopo))
        self.logger.log_energy(self.time(), *(self.energy()))


    def energy(self): # calculate energy
        topo = copy.deepcopy(self.topology)
        
        serverPower = 0
        switchPower = 0

        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                n_VNFs = len(node[1]["deployed"])
                if(n_VNFs > 0):
                    if(n_VNFs > 4):
                        print(n_VNFs)
                        print(node[0], node[1])
                        exit()
                    serverPower += node[1]["power"][n_VNFs]
            
            if(node[1]["model"] == "switch"):
                if(node[1]["status"] == "on"):
                    switchPower += node[1]["basePower"]
                    for neighborLink in self.topology.adj[node[0]].items():
                        usage = neighborLink[1]['usage']
                        if(usage <= 10): switchPower += node[1]["portPower"][0]
                        if(usage > 10 and usage <= 100): switchPower += node[1]["portPower"][1]
                        if(usage > 100 and usage <= 1000): switchPower += node[1]["portPower"][2]
        
        return [serverPower, switchPower]


    def time(self): # return time
        return self.env.now # minutes
        return f"{self.env.now//60}h{self.env.now%60:2d}m:" # format 2h43m
        

    def run(self, runtime):
        for i in range(len(self.apps)):
            self.env.process(self.generator(self.apps[i]))
            self.env.process(self.handler(self.apps[i]))
            # self.env.process(self.deployer(self.apps[i]))
        self.env.run(until=runtime)