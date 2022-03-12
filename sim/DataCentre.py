from ctypes import sizeof
import simpy
import copy
import json



class DataCentre():
    def __init__(self, id, topo):
        self.id = id
        self.topo = topo
        self.power = 0



    def create_pipe(self, sim):
        self.considerPipe = simpy.Store(sim.env)



    def consider(self, sim):
        while True:
            sfc = yield self.considerPipe.get()
            deploy = sfc["app"].selector.analyse(self, sfc)
            if(deploy):
                topo = self.install(deploy)
                power = self.energy(topo)
                # print(f"DC-{self.id} deploy SFC-{deploy['sfc']['id']} delta_power = {power - self.power}")
                considerRes = {
                    "DCid": self.id,
                    "deploy": deploy,
                    "deltaPower": round(power - self.power, 1)
                }
            else:
                considerRes = {
                    "DCid": self.id,
                    "deploy": 0
                }
                # print(f"DC-{self.id}: drop")
            sim.considerResults.put(considerRes)



    def deployer(self, deploy, sim):
        self.topo = self.install(deploy["deploy"])
        self.power = self.energy(self.topo)
        route = deploy["route"]
        for i in range(len(route) - 1):
            # sim.topology.edges[route[i], route[i+1]]["bw"][1] += deploy["deploy"]["sfc"]["outlink"]
            sim.topology.edges[route[i], route[i+1]]["bw"] = [
                sim.topology.edges[route[i], route[i+1]]["bw"][0],
                sim.topology.edges[route[i], route[i+1]]["bw"][1] + deploy["deploy"]["sfc"]["outlink"]
            ]
        
        sim.SFCs.append(deploy["deploy"]["sfc"])
        
        # change topo from python dictionary to json format to logging to .csv
        topo = self.topo_status_json()
        
        # print(f"now system has {len(sim.SFCs)} SFCs------------------")
        sim.logger.log_event(sim.time(), sim.logger.DEPLOYED,
            DC=deploy["DCid"], appname=deploy["deploy"]["sfc"]["app"].name,
            SFC=deploy["deploy"]["sfc"], power=self.power,
            n_SFCs=sim.SFCcounter, running=len(sim.SFCs),
            failed=sim.SFCfailed, topo=topo)
        
        sim.env.process(self.release(deploy, sim))



    def release(self, deploy, sim):
        # print(f"{sim.time()}: SFC-{deploy['deploy']['sfc']['id']} is living")
        yield sim.env.timeout(deploy["deploy"]["sfc"]["TTL"])

        sfc = deploy["deploy"]["sfc"]
        for req in deploy["deploy"]['node']:
            self.topo.nodes[req[1]]["RAM"] = [
                self.topo.nodes[req[1]]["RAM"][0],
                self.topo.nodes[req[1]]["RAM"][1] - sfc["struct"].nodes[req[0]]["RAM"]
            ]
            self.topo.nodes[req[1]]['deployed'].remove([sfc["id"], req[0]])

        # change topo from python dictionary to json format to logging to .csv
        for linkreq in deploy["deploy"]['link']:
            for i in range(len(linkreq['route']) - 1):
                # self.topo.edges[linkreq['route'][i], linkreq['route'][i+1]]["bw"][1] -= linkreq["bw"]
                self.topo.edges[linkreq['route'][i], linkreq['route'][i+1]]["bw"] = [
                    self.topo.edges[linkreq['route'][i], linkreq['route'][i+1]]["bw"][0],
                    self.topo.edges[linkreq['route'][i], linkreq['route'][i+1]]["bw"][1] - linkreq["bw"]
                ]

                # consider to turn off switch
                nodeID = linkreq['route'][i]
                if(self.topo.nodes[nodeID]["model"] == "switch"):
                    for neighborLink in self.topo.adj[nodeID].items():
                        if(neighborLink[1]["bw"][1] > 0):
                            self.topo.nodes[nodeID]["status"] = True
                        else:
                            self.topo.nodes[nodeID]["status"] = False

        for i in range(len(deploy["route"]) - 1):
            # sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][1] -= deploy["deploy"]["sfc"]["outlink"]
            sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"] = [
                sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][0],
                sim.topology.edges[deploy["route"][i], deploy["route"][i+1]]["bw"][1] - deploy["deploy"]["sfc"]["outlink"]
            ]

        self.power = self.energy(self.topo)
        
        sim.SFCs.remove([sfc for sfc in sim.SFCs if sfc["id"] == deploy["deploy"]["sfc"]["id"]][0])

        topo = self.topo_status_json()

        sim.logger.log_event(sim.time(), sim.logger.REMOVE,
            DC=deploy["DCid"], appname=deploy["deploy"]["sfc"]["app"].name,
            SFC=deploy["deploy"]["sfc"], power=self.power,
            n_SFCs=sim.SFCcounter, running=len(sim.SFCs),
            failed=sim.SFCfailed, topo=topo)

        # print(f"{sim.time()}: DC-{self.id} removed SFC-{sfc['id']}")
        print(f"now system has {len(sim.SFCs)} SFCs------------------")



    def install(self, deploy):
        topo = copy.deepcopy(self.topo)
        # topo.nodes[255]["RAM"][1] += 1
        # print(topo.nodes[255]["RAM"])
        # exit()

        sfc = deploy["sfc"]
        for req in deploy['node']:
            topo.nodes[req[1]]["RAM"] = [
                topo.nodes[req[1]]["RAM"][0],
                topo.nodes[req[1]]["RAM"][1] + sfc["struct"].nodes[req[0]]["RAM"]
            ]
            topo.nodes[req[1]]['deployed'].append([sfc["id"], req[0]])

        for linkreq in deploy['link']:
            for i in range(len(linkreq['route']) - 1):
                # topo.edges[linkreq['route'][i], linkreq['route'][i+1]]['bw'][1] += linkreq["bw"]
                topo.edges[linkreq['route'][i], linkreq['route'][i+1]]['bw'] = [
                    topo.edges[linkreq['route'][i], linkreq['route'][i+1]]['bw'][0],
                    topo.edges[linkreq['route'][i], linkreq['route'][i+1]]['bw'][1] + linkreq["bw"]
                ]
                
                # turn on switch
                if(topo.nodes[linkreq['route'][i]]["model"] == "switch"):
                    topo.nodes[linkreq['route'][i]]["status"] = True

        return topo

        # change topo from python dictionary to json format to logging to .csv
        # pTopo = {"node": [], "link": []}
        # for node in list(self.topology.nodes.data()):
        #     pTopo["node"].append({"id": node[0], **node[1]})
        # for plink in list(self.topology.edges.data()):
        #     pTopo["link"].append({"s": plink[0], "d": plink[1], **plink[2]})



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
                n_VNFs = len(node[1]["deployed"])
                if(n_VNFs > 0):
                    serverPower += node[1]["power"][n_VNFs]
            
            if(node[1]["model"] == "switch"):
                if(node[1]["status"] == True):
                    switchPower += node[1]["basePower"]
                    for neighborLink in topo.adj[node[0]].items():
                        usage = neighborLink[1]["bw"][1]
                        if(usage <= 10): switchPower += node[1]["portPower"][0]
                        if(usage > 10 and usage <= 100): switchPower += node[1]["portPower"][1]
                        if(usage > 100 and usage <= 1000): switchPower += node[1]["portPower"][2]
        
        return round(serverPower + switchPower, 1)
