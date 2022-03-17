import simpy
import copy
import json



class DataCentre():
    def __init__(self, id, topo):
        self.id = id
        self.freeTopo = copy.deepcopy(topo)
        self.topo = topo

        self.activeServer = []
        self.SFCs = []

        self.power = 0

        self.SFCs = []



    def create_pipe(self, sim):
        self.considerPipe = simpy.Store(sim.env)



    def consider(self, sim, sfc):
        anaRes = sfc["app"].selector.analyse(self, sfc) # analyse result
        if(anaRes):
            topo = self.install(anaRes)
            power = self.energy(topo)
            considerRes = {
                "sfc": anaRes,
                "deltaPower": round(power - self.power, 1)
            }
        else:
            considerRes = False
            print(f"DC-{self.id} drop SFC-{sfc['id']}")
        sim.considerResults.put(considerRes)
        return considerRes



    def deployer(self, deploy, sim):
        self.topo = self.install(deploy["sfc"])
        self.power = self.energy(self.topo)
        route = deploy["route"]
        for i in range(len(route) - 1):
            sim.topology.edges[route[i], route[i+1]]["bw"] = [
                sim.topology.edges[route[i], route[i+1]]["bw"][0],
                sim.topology.edges[route[i], route[i+1]]["bw"][1] + deploy["sfc"]["outlink"]
            ]
        
        sim.SFCs["running"].append({
            "sfc": deploy["sfc"],
            "event": sim.env.process(self.release(deploy, sim))
        })
        
        topo = self.topo_status_json()
        sim.logger.log_event(sim.time(), sim.logger.DEPLOYED,
            SFC=deploy["sfc"], sim=sim, DCpower=self.power, topo=topo)
        


    def release(self, deploy, sim):
        try:
            start = sim.env.now
            yield sim.env.timeout(deploy["sfc"]["TTL"][1])
            deploy["sfc"]["TTL"] = [deploy["sfc"]["TTL"], 0]

            sfc = deploy["sfc"]
            sfcTopo = sfc["struct"]
            for vNode in list(sfcTopo.nodes.data()): # release RAM
                onServer = vNode[1]["server"]
                self.topo.nodes[onServer]["RAM"] = [
                    self.topo.nodes[onServer]["RAM"][0],
                    self.topo.nodes[onServer]["RAM"][1] - sfcTopo.nodes[vNode[0]]["RAM"]
                ]
                self.topo.nodes[onServer]['deployed'].remove([sfc["id"], vNode[0]])

            for vLink in list(sfcTopo.edges.data()): # release link
                route = vLink[2]['route']
                for i in range(len(route) - 1):
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

            for i in range(len(deploy["route"]) - 1): # release substrate link
                sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"] = [
                    sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][0],
                    sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][1] - deploy["sfc"]["outlink"]
                ]

            self.power = self.energy(self.topo)

            for e in sim.SFCs["running"]:
                if(e["sfc"]["id"] == deploy["sfc"]["id"]):
                    sim.SFCs["running"].remove(e)
                    break

            topo = self.topo_status_json()

            sim.logger.log_event(sim.time(), sim.logger.REMOVE,
                SFC=deploy["sfc"], sim=sim, DCpower=self.power, topo=topo)

        except simpy.Interrupt:
            deploy["sfc"]["TTL"] = [
                deploy["sfc"]["TTL"][0],
                deploy["sfc"]["TTL"][1] - sim.env.now + start
            ]
            print(f"{sim.time()}: SFC-{deploy['sfc']['id']} is interrupted! remain {deploy['sfc']['TTL'][1]} -------------->{deploy['sfc']['id']}")
            # if(sim.SFCs["running"]):
            #     print(sim.SFCs["running"][0]["sfc"]["TTL"])

        



    def reset(self):
        print(f"DC-{self.id} has been reset")
        self.topo = copy.deepcopy(self.freeTopo)
        self.power = 0



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
                    topo.nodes[route[i]]["state"] = True

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
