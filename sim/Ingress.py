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
            duration = app.distribution.next(sim.time())
            yield sim.env.timeout(duration)

            sim.SFCcounter += 1
            sfcID = sim.SFCcounter
            sfcInit = app.create_SFC(sfcID)
            sfc = {
                "id": sfcID,
                "app": app,
                "outlink": sfcInit[1],
                "Ingress": self.id,
                "DataCentre": False,
                "struct": sfcInit[0],
                "demand": sfcInit[2],
                "TTL": sfcInit[3],
                "remain": sfcInit[3],
                "outroute": [],
                "failDetail": [],
            }
            sim.SFCs.append(sfc)

            # change sfc from python dictionary to json format to logging to .csv
            sfc_log = {
                "id": sfc["id"],
                "outlink": sfc["outlink"],
                "Ingress": sfc["Ingress"],
                "demand": sfc["demand"],
                "TTL": sfc["TTL"],
                "vnf": [],
                "vlink": []
            }
            for vnf in list(sfc["struct"].nodes.data()):
                sfc_log["vnf"].append({"id": vnf[0], **vnf[1]})
            for vlink in list(sfc["struct"].edges.data()):
                sfc_log["vlink"].append({"s": vlink[0], "d": vlink[1], **vlink[2]})
            sfc_log = json.dumps(sfc_log)
            sim.logger.log_event(sim, sim.logger.CREATE, SFC=sfc, topo=sfc_log)

            sim.reqQueue.put(sfc)

            
