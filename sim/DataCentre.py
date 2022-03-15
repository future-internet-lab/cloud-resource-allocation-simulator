from ctypes import sizeof
import simpy
import copy
import json



class DataCentre():
    def __init__(self, id, topo):
        self.id = id
        self.topo = topo

        self.activeServer = []
        self.SFCs = []

        self.power = 0



    def create_pipe(self, sim):
        self.considerPipe = simpy.Store(sim.env)



    def consider(self, sim):
        while True:
            sfc = yield self.considerPipe.get()
            anaRes = sfc["app"].selector.analyse(self, sfc) # analyse result
            if(anaRes):
                topo = self.install(anaRes)
                power = self.energy(topo)
                # print(f"DC-{self.id} deploy SFC-{deploy['sfc']['id']} delta_power = {power - self.power}")
                considerRes = {
                    "sfc": anaRes,
                    "deltaPower": round(power - self.power, 1)
                }
            else:
                considerRes = False
                # print(f"DC-{self.id}: drop")
            sim.considerResults.put(considerRes)



    def deployer(self, deploy, sim):
        self.topo = self.install(deploy["sfc"])
        self.power = self.energy(self.topo)
        route = deploy["route"]
        for i in range(len(route) - 1):
            sim.topology.edges[route[i], route[i+1]]["bw"] = [
                sim.topology.edges[route[i], route[i+1]]["bw"][0],
                sim.topology.edges[route[i], route[i+1]]["bw"][1] + deploy["sfc"]["outlink"]
            ]
        
        sim.SFCs["running"].append(deploy["sfc"]["id"])
        
        # change topo from python dictionary to json format to logging to .csv
        topo = self.topo_status_json()
        
        print(f"now system has {len(sim.SFCs['running'])} SFCs------------------")
        sim.logger.log_event(sim.time(), sim.logger.DEPLOYED,
            SFC=deploy["sfc"], sim=sim, DCpower=self.power, topo=topo)
        
        # sim.env.process(self.release(deploy, sim))



    def release(self, deploy, sim):
        # print(f"{sim.time()}: SFC-{deploy['deploy']['sfc']['id']} is living")
        yield sim.env.timeout(deploy["sfc"]["TTL"][0])

        sfc = deploy["sfc"]
        sfcTopo = sfc["struct"]
        for vNode in list(sfcTopo.nodes.data()):
            onServer = vNode[1]["server"]
            self.topo.nodes[onServer]["RAM"] = [
                self.topo.nodes[onServer]["RAM"][0],
                self.topo.nodes[onServer]["RAM"][1] - sfcTopo.nodes[vNode[0]]["RAM"]
            ]
            self.topo.nodes[onServer]['deployed'].remove([sfc["id"], vNode[0]])

        # change topo from python dictionary to json format to logging to .csv
        for vLink in list(sfcTopo.edges.data()):
            route = vLink[2]['route']
            for i in range(len(route) - 1):
                # self.topo.edges[linkreq['route'][i], linkreq['route'][i+1]]["bw"][1] -= linkreq["bw"]
                self.topo.edges[route[i], route[i+1]]["bw"] = [
                    self.topo.edges[route[i], route[i+1]]["bw"][0],
                    self.topo.edges[route[i], route[i+1]]["bw"][1] - vLink[2]["bw"]
                ]

                # consider to turn off switch
                nodeID = route[i]
                if(self.topo.nodes[nodeID]["model"] == "switch"):
                    for neighborLink in self.topo.adj[nodeID].items():
                        if(neighborLink[1]["bw"][1] > 0):
                            self.topo.nodes[nodeID]["state"] = True
                        else:
                            self.topo.nodes[nodeID]["state"] = False

        for i in range(len(deploy["route"]) - 1):
            # sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][1] -= deploy["deploy"]["sfc"]["outlink"]
            sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"] = [
                sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][0],
                sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][1] - deploy["sfc"]["outlink"]
            ]

        self.power = self.energy(self.topo)
        
        # sim.SFCs["all"].remove([sfc for sfc in sim.SFCs["all"] if sfc["id"] == deploy["sfc"]["id"]][0])
        sim.SFCs["running"].remove(deploy["sfc"]["id"])

        topo = self.topo_status_json()

        sim.logger.log_event(sim.time(), sim.logger.REMOVE,
            SFC=deploy["sfc"], sim=sim, DCpower=self.power, topo=topo)

        # print(f"{sim.time()}: DC-{self.id} removed SFC-{sfc['id']}")
        print(f"now system has {len(sim.SFCs['running'])} SFCs------------------")



    def install(self, sfc): # anaRes is sfc after analysing
        topo = copy.deepcopy(self.topo)

        sfcTopo = sfc["struct"]
        for vNode in list(sfcTopo.nodes.data()):
            onServer = vNode[1]["server"]
            topo.nodes[onServer]["RAM"] = [
                topo.nodes[onServer]["RAM"][0],
                topo.nodes[onServer]["RAM"][1] + sfcTopo.nodes[vNode[0]]["RAM"]
            ]
            topo.nodes[onServer]['deployed'].append([sfc["id"], vNode[0]])

        for vLink in list(sfcTopo.edges.data()):
            route = vLink[2]['route']
            for i in range(len(route) - 1):
                # topo.edges[linkreq['route'][i], linkreq['route'][i+1]]['bw'][1] += linkreq["bw"]
                topo.edges[route[i], route[i+1]]['bw'] = [
                    topo.edges[route[i], route[i+1]]['bw'][0],
                    topo.edges[route[i], route[i+1]]['bw'][1] + vLink[2]["bw"]
                ]
                
                # turn on switch
                if(topo.nodes[route[i]]["model"] == "switch"):
                    topo.nodes[route[i]]["status"] = True

        return topo



    def topo_status_json(self):
        topo = {"node": [], "link": []}
        for node in list(self.topo.nodes.data()):
            topo["node"].append({"id": node[0], **node[1]})
        for plink in list(self.topo.edges.data()):
            topo["link"].append({"s": plink[0], "d": plink[1], **plink[2]})
        topo = json.dumps(topo)
        return topo



    def energy(self, topo): # calculate energy
        serverPower = 0
        switchPower = 0

        for node in list(topo.nodes.data()):
            if(node[1]["model"] == "server"):
                if(node[1]["state"]): # server is online
                    n_VNFs = len(node[1]["deployed"])
                    if(n_VNFs > 0):
                        serverPower += node[1]["power"][n_VNFs]
                
            if(node[1]["model"] == "switch"):
                if(node[1]["state"]): # switch is online
                    switchPower += node[1]["basePower"]
                    for neighborLink in topo.adj[node[0]].items():
                        usage = neighborLink[1]["bw"][1]
                        if(usage <= 10):
                            switchPower += node[1]["portPower"][0]
                        if(usage > 10 and usage <= 100):
                            switchPower += node[1]["portPower"][1]
                        if(usage > 100 and usage <= 1000):
                            switchPower += node[1]["portPower"][2]
        
        return round(serverPower + switchPower, 1)
