import csv
import json
import logging



class Logger():
    CREATE = "create"
    DROP = "drop"
    DEPLOY = "deploy"
    REDEPLOY = "redeploy"
    REMOVE = "remove"
    INTERRUPT = "interrupt"
    REMAP_START = "remap"
    REMAP_FAIL = "remap_failed"
    REMAP_SUCCESS = "remap_successfully"

    def __init__(self, sim, folder_log=None, logCSV=True):
        self.sim = sim
        path = folder_log
        self.logCSV = logCSV

        eventFields = ["id", "time", "action", "Ingress/DC", "SFC", "n_VNFs",
            "demand", "TTL", "remain", "util", "power", "P_per_sfc", "active_server", "failDetail", "selectorLog"
        ]

        self.fEvent = open(f"{path}_event.csv", mode="w", newline="")
        self.wEvent = csv.writer(self.fEvent)
        self.wEvent.writerow(eventFields)

        # self.DCstatus = open(f"{path}_DCstatus.json", mode="w")
        # self.DCstatus.write("[")

        self.sfc = open(f"{path}_sfc.json", mode="w")
        self.sfc.write("[")

        self.stat = open(f"{path}_stat.json", mode="w")

        self.countEvent = 0

    

    def log_event(self, sim, action, SFC="-", topo="-"):
        self.countEvent += 1
        subtrateNode = "-"
        if SFC != "-" and SFC["failDetail"] != []:
            failDetail = SFC["failDetail"]
        else:
            failDetail = "-"
        load = 1
        util = round(sim.util / sim.capacity * 100, 1)

        try: acceptance = round(len(sim.stat["accepted"]) / (len(sim.stat["accepted"]) + len(sim.stat["failed"])) * 100, 1)
        except:  acceptance = 0.0

        power = sim.cal_power()

        try: power_per_sfc = round(power / len(sim.runningSFCs), 1)
        except: power_per_sfc = 0.0

        active_server = sim.cal_active_server()

        if(action == self.CREATE):
            logging.info(f"{sim.time()}: Ingress-{SFC['Ingress']} create SFC-{SFC['id']} with {len(SFC['struct'].nodes)} VNFs, TTL = {SFC['TTL']}")
            subtrateNode = SFC["Ingress"]
        if(action == self.DROP):
            logging.info(f"{sim.time()}: drop SFC-{SFC['id']}, remain {SFC['remain']}")
        if(action == self.DEPLOY):
            logging.info(f"{sim.time()}: deploy SFC-{SFC['id']}({len(SFC['struct'].nodes)} vnfs) on DC-{SFC['DataCentre']}, remain {SFC['remain']}")
            subtrateNode = SFC['DataCentre']
        if(action == self.REDEPLOY):
            logging.info(f"{sim.time()}: redeploy SFC-{SFC['id']}({len(SFC['struct'].nodes)} vnfs) on DC-{SFC['DataCentre']}, remain {SFC['remain']}")
            subtrateNode = SFC['DataCentre']
        if(action == self.REMOVE):
            logging.info(f"{sim.time()}: remove SFC-{SFC['id']} on DC-{SFC['DataCentre']}, remain {SFC['remain']}")
            subtrateNode = SFC['DataCentre']
        if(action == self.INTERRUPT):
            power = "-"
            power_per_sfc = "-"
            active_server = "-"
            logging.info(f"{sim.time()}: interrupt SFC-{SFC['id']} on DC-{SFC['DataCentre']}, remain {SFC['remain']} ----->{SFC['id']}")
        if(action == self.REMAP_START):
            logging.info(f"{sim.time()}: -----Start remapping-----")
        if(action == self.REMAP_FAIL):
            logging.info(f"{sim.time()}:-----Remap failed, turn back previous status-----")
        if(action == self.REMAP_SUCCESS):
            logging.info(f"{sim.time()}: -----Remap successfully-----")
        if self.logCSV:
            if(action == self.REMAP_START):
                self.wEvent.writerow([self.countEvent, sim.time(), action, "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-"])
            elif(action in [self.REMAP_FAIL, self.REMAP_SUCCESS]):
                self.wEvent.writerow([
                    self.countEvent, sim.time(), action, subtrateNode, "-", "-", "-", "-", "-",
                    util, power, power_per_sfc, active_server, failDetail
                ])
            elif(action == self.DEPLOY and "itr" in SFC.keys()):
                self.wEvent.writerow([
                    self.countEvent, sim.time(), action, subtrateNode, SFC["id"],
                    len(SFC['struct'].nodes), SFC["demand"], SFC["TTL"], SFC["remain"],
                    util, power, power_per_sfc, active_server, failDetail, SFC["itr"]
                ])
            else:
                self.wEvent.writerow([
                    self.countEvent, sim.time(), action, subtrateNode, SFC["id"],
                    len(SFC['struct'].nodes), SFC["demand"], SFC["TTL"], SFC["remain"],
                    util, power, power_per_sfc, active_server, failDetail
                ])

        # if(topo != "-"):
        #     if(action == self.CREATE):
        #         self.sfc.write(topo + ",")
            # else:
            #     topo = json.dumps({"event": self.countEvent, **(json.loads(topo))})
            #     self.DCstatus.write(topo + ",")



    def close(self):
        # self.end_of_json(self.DCstatus)
        self.end_of_json(self.sfc)
        self.stat.write(json.dumps(self.sim.stat))

        self.fEvent.close()
        # self.DCstatus.close()
        self.sfc.close()
        self.stat.close()



    def end_of_json(self, jsonFile):
        jsonFile.seek(0, 2)
        jsonFile.seek(jsonFile.tell() - 1)
        jsonFile.truncate()
        jsonFile.write("]")
