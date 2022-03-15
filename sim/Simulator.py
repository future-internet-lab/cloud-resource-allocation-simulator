from sqlite3 import DatabaseError
from sim.SFC import *
from sim.Logger import *

import copy
import simpy
import networkx as nx



class Simulator():
    def __init__(self, topology, DCs, Ingresses, strategy, folder_log):
        self.env = simpy.Environment()
        self.logger = Logger(folder_log)
        self.strategy = strategy

        self.topology = topology
        self.DataCentres = DCs
        self.Ingresses = Ingresses

        self.SFCs = []

        self.reqQueue = simpy.Store(self.env)

        self.considerPipes = []
        for DC in self.DataCentres:
            DC.create_pipe(self)
            self.considerPipes.append(DC.considerPipe)
        self.considerResults = simpy.Store(self.env)

        # self.SFCs = []

        self.SFCs = {
            "all": [],
            "running": [],
            "accepted": [],
            "failed": []
        }

        self.SFCcounter = 0



    def handler(self): # consider if it's possible to implement sfc
        while True:
            sfc = yield self.reqQueue.get()
            # print(f"{self.time()}: SFC-{sfc['id']} is retrieved")

            for pipe in self.considerPipes:
                pipe.put(sfc.copy())

            received = 0
            failed = 0
            power = 0
            while received < len(self.DataCentres):
                result = yield self.considerResults.get()
                received += 1
                if(result): # result["deploy"] is sfc after analysing
                    topo = copy.deepcopy(self.topology)
                    sfc = result["sfc"]
                    for p_link in list(topo.edges.data()):
                        if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < sfc["outlink"]):
                            topo.remove_edge(p_link[0], p_link[1])
                    try:
                        route = nx.shortest_path(topo, sfc["Ingress"], sfc["DataCentre"])
                    except:
                        failed += 1
                    if(len(route) > 0): # exist route
                        if(power == 0):
                            power = result["deltaPower"]
                            step = len(route)
                            deploy = {**result, "route": route}
                        elif((result["deltaPower"] < power and len(route) > 0)
                            or (result["deltaPower"] == power and len(route) < step)):
                            power = result["deltaPower"]
                            step = len(route)
                            deploy = {**result, "route": route}
                    else: failed += 1
                else: failed += 1
            if(failed == len(self.DataCentres)):
                self.SFCs["failed"].append(sfc["id"])
                self.logger.log_event(self.time(), self.logger.DROP, SFC=sfc, sim=self)
                # print(f"{self.time()}: SFC-{sfc['id']} has been dropped")
            else:
                self.SFCs["accepted"].append(sfc["id"])
                print(f"{self.time()}: SFC-{deploy['sfc']['id']} has been deployed in DC-{deploy['sfc']['DataCentre']} and use deltaPower = {power}")
                [DC for DC in self.DataCentres if DC.id == deploy['sfc']['DataCentre']][0].deployer(deploy, self)



    def time(self): # return time
        return self.env.now # minutes
        return f"{self.env.now//60}h{self.env.now%60:2d}m:" # format 2h43m
        


    def run(self, runtime):
        for ingress in self.Ingresses:
            ingress.generator(self)
        for DC in self.DataCentres:
            self.env.process(DC.consider(self))
        self.env.process(self.handler())

        self.env.run(until=runtime)
        self.logger.close()

        print()
        total = len(self.SFCs['all'])
        failed = len(self.SFCs['failed'])
        print(f"total: {total} SFCs")
        print(f"failed: {failed} SFCs")
        print(f"acceptance ratio: {round((total - failed) / total * 100, 1)}%")