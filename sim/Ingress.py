import json
import copy



class Ingress():
    def __init__(self, id, apps):
        self.id = id
        self.apps = apps



    def generator(self, sim):
        for app in self.apps:
            sim.env.process(self.generate_SFC(sim, app))



    def generate_SFC(self, sim, app):
        while True:
            duration = app.distribution.next()
            yield sim.env.timeout(duration)

            sim.SFCcounter += 1
            sfcID = sim.SFCcounter
            sfcCreated = app.create_SFC(sfcID)
            sfc = {
                "id": sfcID,
                "app": app,
                "outlink": sfcCreated[1],
                "Ingress": self.id,
                "DataCentre": False,
                "struct": sfcCreated[0],
                "TTL": sfcCreated[2],
                "remain": sfcCreated[2],
                "outroute": []
            }
            sim.SFCs.append(sfc)
            sim.VNFs[0] += len(sfc["struct"].nodes)

            # change sfc from python dictionary to json format to logging to .csv
            struct = {"id": sfc["id"], "vnf": [], "vlink": []}
            for vnf in list(sfc["struct"].nodes.data()):
                struct["vnf"].append({"id": vnf[0], **vnf[1]})
            for vlink in list(sfc["struct"].edges.data()):
                struct["vlink"].append({"s": vlink[0], "d": vlink[1], **vlink[2]})
            struct = json.dumps(struct)

            sim.logger.log_event(sim, sim.logger.CREATE, SFC=sfc, topo=struct)
            # print(f"{sim.time()}: Ingress-{self.id} create SFC-{sfc['id']} with {len(sfc['struct'].nodes)} VNFs TTL = {sfc['TTL']}")
            sim.reqQueue.put(sfc)

            
