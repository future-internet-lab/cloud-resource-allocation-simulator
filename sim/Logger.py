import csv



class Logger():
    CREATE = "create"
    HANDLER_SUCCESS = "accept"
    HANDLER_FAIL = "drop"
    DEPLOYED = "deployed"
    REMOVE = "remove"

    def __init__(self, folder_log=None):
        # definition of .csv fields
        eventFields = ["id", "time", "action", "app", "SFC", "n_VNFs", "TTL", "topo"]
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

        self.__countEvent = 0
        self.__countEnergy = 0

    
    def log_event(self, time, action, app="-", SFC=None, n_VNFs="-", TTL="-", topo="-"):
        self.__countEvent += 1
        if(action == self.CREATE):
            print(f"{time}: {action} SFC-{SFC}, {n_VNFs} VNFs, TTL = {TTL}")
        else:
            print(f"{time}: {action} SFC-{SFC}")
        self.__wEvent.writerow([self.__countEvent, time, action, app, SFC, n_VNFs, TTL, topo])


    def log_energy(self, time, serverPower, switchPower):
        self.__countEnergy += 1
        total = serverPower + switchPower
        print(f"{time}: ENERGY: Servers = {serverPower:.2f}, Switches = {switchPower:.2f}, Total = {total:.2f}")
        self.__wEnergy.writerow([self.__countEnergy, time, serverPower, switchPower, total])


    def close(self):
        self.__fEvent.close()