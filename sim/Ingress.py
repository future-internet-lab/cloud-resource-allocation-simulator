import json



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
            sfc = {
                "id": sfcID,
                "ingress": self.id,
                "app": app,
                "outlink": app.create_SFC(sfcID)[1],
                "struct": app.create_SFC(sfcID)[0],
                "TTL": app.create_SFC(sfcID)[2]
            }

            # change sfc from python dictionary to json format to logging to .csv
            struct = {"vnf": [], "vlink": []}
            for vnf in list(sfc["struct"].nodes.data()):
                struct["vnf"].append({"id": vnf[0], **vnf[1]})
            for vlink in list(sfc["struct"].edges.data()):
                struct["vlink"].append({"s": vlink[0], "d": vlink[1], **vlink[2]})
            struct = json.dumps(struct)

            sim.logger.log_event(sim.time(), sim.logger.CREATE, ingress=self.id, appname=app.name, SFC=sfc, topo=struct)
            # print(f"{sim.time()}: Ingress-{self.id} create SFC-{sfc['id']} with {len(sfc['struct'].nodes)} VNFs TTL = {sfc['TTL']}")
            sim.reqQueue.put(sfc)

            
