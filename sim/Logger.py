import csv
import json
import os



class Logger():
    CREATE = "create"
    HANDLER_SUCCESS = "accept"
    DROP = "drop"
    DEPLOYED = "deployed"
    REMOVE = "remove"

    def __init__(self, folder_log=None):
        # definition of .csv fields
        eventFields = ["id", "time", "action", "ingress", "DC", "app", "SFC", "n_VNFs", "TTL", "n_SFCs", "running", "failed", "power"]
        energyFields = ["id", "time", "P_Servers", "P_Switches", "E_All"]

        path = "result"
        if(folder_log is not None):
            path = folder_log

        # .csv file name
        self.__fEvent = open(f"{path}_event.csv", mode="w", newline="")
        self.__wEvent = csv.writer(self.__fEvent)
        self.__wEvent.writerow(eventFields)

        self.__fEnergy = open(f"{path}_energy.csv", mode="w", newline="")
        self.__wEnergy = csv.writer(self.__fEnergy)
        self.__wEnergy.writerow(energyFields)

        self.__fTopo = open(f"{path}_topo.txt", mode="w")
        self.__fTopo.write("[")

        self.__countEvent = 0
        self.__countEnergy = 0

    
    def log_event(self, time, action, ingress="-", DC="-", appname="-", SFC=None, n_SFCs="-", running="-", failed="-", power="-", topo="-"):
        self.__countEvent += 1
        if(action == self.CREATE):
            print(f"{time}: Ingress-{ingress} create SFC-{SFC['id']} with {len(SFC['struct'].nodes)} VNFs, TTL = {SFC['TTL']}")
            # print(f"{time}: {action} SFC-{SFC}, {n_VNFs} VNFs, TTL = {TTL}")
        if(action == self.DROP):
            print(f"{time}: {action} SFC-{SFC['id']}")
        if(action == self.DEPLOYED):
            print(f"{time}: {action} SFC-{SFC['id']} on DC-{DC}")
        if(action == self.REMOVE):
            print(f"{time}: {action} SFC-{SFC['id']} on DC-{DC}")
        self.__wEvent.writerow([self.__countEvent, time, action, ingress, DC, appname, SFC['id'], len(SFC['struct'].nodes), SFC['TTL'], n_SFCs, running, failed, power])
        if(topo != "-"):
            topo = json.dumps({"event": self.__countEvent, **(json.loads(topo))})
            self.__fTopo.write(topo + ",")


    def log_energy(self, time, serverPower, switchPower):
        self.__countEnergy += 1
        total = serverPower + switchPower
        print(f"{time}: ENERGY: Servers = {serverPower:.2f}, Switches = {switchPower:.2f}, Total = {total:.2f}")
        self.__wEnergy.writerow([self.__countEnergy, time, serverPower, switchPower, total])


    def close(self):
        self.__fEvent.close()

        self.__fTopo.seek(0, 2)
        self.__fTopo.seek(self.__fTopo.tell() - 1)
        self.__fTopo.truncate()
        self.__fTopo.write("]")

        self.__fTopo.close()