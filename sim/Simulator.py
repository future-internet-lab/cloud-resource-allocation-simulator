from sqlite3 import DatabaseError
from turtle import back
from sim.SFC import *
from sim.Logger import *

import copy
import simpy
import networkx as nx
import random



class Simulator():
    def __init__(self, topology, DCs, Ingresses, folder_log, *arg):
        self.env = simpy.Environment()
        self.logger = Logger(self, folder_log)
        self.strategy = arg[0]

        if(len(arg) == 2): self.sortmode = arg[1]

        self.topology = topology
        self.DataCentres = DCs
        self.Ingresses = Ingresses

        self.capacity = 0
        for DC in self.DataCentres:
            for node in list(DC.topo.nodes.data()):
                if(node[1]["model"] == "server"):
                    self.capacity += node[1]["capacity"]

        self.reqQueue = simpy.Store(self.env)

        self.SFCcounter = 0 # for numbering SFC

        self.SFCs = []
        self.runningSFCs = []
        self.stat = {
            "accepted": [], # id
            "failed": [] # id
        }
        self.VNFs = [0, 0] # [load, util]
        self.power = 0
        self.activeServer = 0



    def pickup(self): # get sfc from reqQueue
        while True:
            sfc = yield self.reqQueue.get()
            print(f"get sfc-{sfc['id']}")

            if(self.strategy == 1):
                self.handler(sfc)
            if(self.strategy == 2):
                backupRunningSFCs = []
                for e in self.runningSFCs:
                    backupRunningSFCs.append(e["sfc"])
                self.logger.log_event(self, self.logger.REMAP_START)
                try:
                    for aliveSFC in self.runningSFCs:
                        aliveSFC["event"].interrupt()
                        yield self.env.timeout(0)

                    for DC in self.DataCentres:
                        DC.reset()
                    for outlink in list(self.topology.edges.data()):
                        outlink[2]["usage"] = 0

                    _runningSFCs = copy.deepcopy(backupRunningSFCs)
                    if(self.sortmode == 'd'):
                        _runningSFCs.sort(reverse=True, key=lambda e : len(e["struct"].nodes))
                    if(self.sortmode == 'i'):
                        _runningSFCs.sort(reverse=False, key=lambda e : len(e["struct"].nodes))
                    self.runningSFCs = []

                    for _sfc in _runningSFCs:
                        self.handler(_sfc)
                    self.logger.log_event(self, self.logger.REMAP_SUCCESS)
                    self.handler(sfc)
                except:
                    print("\n\n-----remap failed, turn back previous status-----\n\n")

                    for aliveSFC in self.runningSFCs:
                        try:
                            aliveSFC["event"].interrupt()
                        except:
                            pass
                        yield self.env.timeout(0)
                    
                    for DC in self.DataCentres:
                        DC.reset()
                    for outlink in list(self.topology.edges.data()):
                        outlink[2]["usage"] = 0

                    self.runningSFCs = []

                    for _sfc in backupRunningSFCs:
                        print(f"deploy SFC-{_sfc['id']} using backup")
                        [DC for DC in self.DataCentres if DC.id == _sfc["DataCentre"]][0].deployer(_sfc, self)
                    self.logger.log_event(self, self.logger.REMAP_FAIL)
                    self.handler(sfc)

                    

    def handler(self, sfc): # consider if it's possible to implement sfc
        failed = 0
        power = 0
        deploy = {"sfc": {}, "outroute": 0}
        for DC in self.DataCentres:
            result = DC.consider(self, sfc)
            if(result): # result["deploy"] is sfc after analysing
                topo = copy.deepcopy(self.topology)
                _sfc = result["sfc"]
                for out_link in list(topo.edges.data()):
                    if(out_link[2]["capacity"] - out_link[2]["usage"] < _sfc["outlink"]):
                        topo.remove_edge(out_link[0], out_link[1])
                try:
                    route = nx.shortest_path(topo, _sfc["Ingress"], _sfc["DataCentre"])
                except:
                    print(f"Cannot routing from Ingress-{_sfc['Ingress']} to DC-{DC.id}")
                    failed += 1
                    continue
                else: # exist route
                    if(power == 0
                        or (result["deltaPower"] < power)
                        or (result["deltaPower"] == power and len(route) < step)):
                        power = result["deltaPower"]
                        step = len(route)
                        result["sfc"]["outroute"] = route
                        deploy = result["sfc"]
                        # deploy = {**result["sfc"], "outroute": route}
            else:
                print(f"cannot deploy SFC-{sfc['id']} on DC-{DC.id}")
                failed += 1
        if(failed == len(self.DataCentres)):
            if((sfc["id"] in self.stat["accepted"])):
                exit()
                print("-----------------------------------------------")
            if((not sfc["id"] in self.stat["failed"])):
                self.stat["failed"].append(sfc["id"])
            self.logger.log_event(self, self.logger.DROP, sfc)
            self.VNFs[0] -= len(sfc["struct"].nodes)
            # print(f"{self.time()}: SFC-{sfc['id']} has been dropped")
        else:
            if(not sfc["id"] in self.stat["accepted"]):
                self.stat["accepted"].append(sfc["id"])
            # print(f"{self.time()}: SFC-{deploy['sfc']['id']} has been deployed in DC-{deploy['sfc']['DataCentre']} and use deltaPower = {power}")
            # print(f"deploy SFC-{_sfc['id']} first time")
            [DC for DC in self.DataCentres if DC.id == deploy['DataCentre']][0].deployer(deploy, self)
        


    def cal_power(self):
        self.power = 0
        for DC in self.DataCentres:
            self.power += DC.power
        return self.power



    def cal_active_server(self):
        self.activeServer = 0
        for DC in self.DataCentres:
            self.activeServer += len(DC.activeServer)
        return self.activeServer



    def time(self): # return time
        return self.env.now # minutes
        return f"{self.env.now//60}h{self.env.now%60:2d}m:" # format 2h43m
        


    def run(self, runtime):
        for ingress in self.Ingresses:
            ingress.generator(self)
        self.env.process(self.pickup())

        self.env.run(until=runtime)
        self.logger.close()

        print()
        total = len(self.SFCs)
        accepted = len(self.stat['accepted'])
        failed = len(self.stat['failed'])
        acceptance = round(accepted / total * 100, 1)
        print(f"accepted: {accepted} / {total} SFCs ({acceptance}%)")
        print(f"failed: {failed} SFCs")
        return acceptance