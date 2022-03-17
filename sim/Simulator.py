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



    def pickup(self): # get sfc from reqQueue
        while True:
            sfc = yield self.reqQueue.get()

            if(self.strategy == 1):
                self.handler(sfc)
            if(self.strategy == 2):
                for aliveSFC in self.SFCs["running"]:
                    aliveSFC["event"].interrupt()
                yield self.env.timeout(0)

                for DC in self.DataCentres:
                    DC.reset()
                    for link in list(DC.topo.edges.data()):
                        if(link[2]["bw"][1]):
                            print("DC reset error")
                            exit()
                            

                runningSFC = self.SFCs["running"]
                runningSFC.sort(reverse=True, key=lambda e : len(e["sfc"]["struct"].nodes))
                self.SFCs["running"] = []

                for e in runningSFC:
                    self.handler(e["sfc"])
                self.handler(sfc)



    def handler(self, sfcinput): # consider if it's possible to implement sfc
        failed = 0
        power = 0
        deploy = {}
        for DC in self.DataCentres:
            result = DC.consider(self, sfcinput)
            if(result): # result["deploy"] is sfc after analysing
                topo = copy.deepcopy(self.topology)
                sfc = result["sfc"]
                for p_link in list(topo.edges.data()):
                    if(p_link[2]["bw"][0] - p_link[2]['bw'][1] < sfc["outlink"]):
                        topo.remove_edge(p_link[0], p_link[1])
                try:
                    route = nx.shortest_path(topo, sfc["Ingress"], sfc["DataCentre"])
                except:
                    print(f"cannot routing to DC-{DC.id}")
                    failed += 1
                    continue
                if(len(route) > 0): # exist route
                    if(power == 0
                        or (result["deltaPower"] < power)
                        or (result["deltaPower"] == power and len(route) < step)):
                        power = result["deltaPower"]
                        step = len(route)
                        deploy = {**result, "route": route}
                else: failed += 1
            else:
                print(f"cannot deploy SFC-{sfcinput['id']} on DC-{DC.id}")
                failed += 1
        if(failed == len(self.DataCentres)):
            if((sfcinput["id"] in self.SFCs["accepted"])):
                # f = open("results/result_test_link.json", "w")
                # f.write("[")
                # for DC in self.DataCentres:
                #     f.write(json.dumps(list(DC.topo.edges.data())) + ",")
                # print(json.dumps(dict(sfcinput["struct"].nodes.data())))
                # f.seek(0, 2)
                # f.seek(f.tell() - 1)
                # f.truncate()
                # f.write("]")
                # f.close()
                # exit()
                print("-----------------------------------------------")
            if((not sfcinput["id"] in self.SFCs["failed"])):
                self.SFCs["failed"].append(sfcinput["id"])
            self.logger.log_event(self.time(), self.logger.DROP, SFC=sfcinput, sim=self)
            # print(f"{self.time()}: SFC-{sfc['id']} has been dropped")
        else:
            if(not sfcinput["id"] in self.SFCs["accepted"]):
                self.SFCs["accepted"].append(sfcinput["id"])
            # print(f"{self.time()}: SFC-{deploy['sfc']['id']} has been deployed in DC-{deploy['sfc']['DataCentre']} and use deltaPower = {power}")
            [DC for DC in self.DataCentres if DC.id == deploy['sfc']['DataCentre']][0].deployer(deploy, self)
        


    def time(self): # return time
        return self.env.now # minutes
        return f"{self.env.now//60}h{self.env.now%60:2d}m:" # format 2h43m
        


    def run(self, runtime):
        for ingress in self.Ingresses:
            ingress.generator(self)
        # for DC in self.DataCentres:
        #     self.env.process(DC.consider(self))
        self.env.process(self.pickup())

        self.env.run(until=runtime)
        self.logger.close()

        print()
        total = len(self.SFCs['all'])
        accepted = len(self.SFCs['accepted'])
        failed = len(self.SFCs['failed'])
        acceptance = round(accepted / total * 100, 1)
        print(f"total: {total} SFCs")
        print(f"accepted: {accepted} SFCs")
        print(f"failed: {failed} SFCs")
        print(f"acceptance ratio: {acceptance}%")
        # print(self.SFCs["accepted"])
        # print(self.SFCs["failed"])
        return acceptance