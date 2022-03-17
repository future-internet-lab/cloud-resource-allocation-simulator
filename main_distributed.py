from sim.Simulator import *
from sim.Application import *
from sim.SFC import *
from sim.Distribution import *
from sim.Selector import *
from sim.DataCentre import *
from sim.Ingress import *
from library import *

from pathlib import Path
import sys
import json
import random



def main():
    np.random.seed(2405)
    random.seed(2405)
    # define log file
    folder_log = Path("results")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/result"

    switchSpecs = {
        "basePower": 39,
        "portPower": [0.42, 0.48, 0.9]
    }
    serverSpecs = {
        "RAM": [4, 0],
        "power": [205.1, 232.9, 260.7, 288.6, 316.4]
    }

    # create one app
    dist = Poisson(lamda=8)
    selector = WaxmanSelector_0()
    avg_TTL = 120 # average time_to_live of SFC, exponential distribution
    n_VNFs_range = [4, 20] # number of VNFs per SFC, uniform distribution
    bw_range = [10, 90] # bw for each virtual link, uniform distribution
    waxman = [0.5, 0.5]
    arg = [avg_TTL, n_VNFs_range, bw_range, waxman]
    app = WaxmanApp("SimpleApp", dist, selector, *arg)
    apps = [app]

    # big topo
    topology = DistributedTopo()
    DCs = []
    DCs.append(DataCentre(1, fat_tree(4, switchSpecs, serverSpecs)))
    DCs.append(DataCentre(4, fat_tree(6, switchSpecs, serverSpecs)))
    DCs.append(DataCentre(6, fat_tree(6, switchSpecs, serverSpecs)))
    DCs.append(DataCentre(11, fat_tree(8, switchSpecs, serverSpecs)))
    Ingresses = []
    Ingresses.append(Ingress(5, apps))
    Ingresses.append(Ingress(7, apps))
    Ingresses.append(Ingress(9, apps))
    Ingresses.append(Ingress(10, apps))

    sim = Simulator(topology, DCs, Ingresses, 1, folder_log)
    sim.run(120)
    # acceptance = sim.run(120) # runtime = 120 minutes
    # return acceptance



if __name__ == "__main__":
    print("-----START SIMULATION-----")
    # acceptance = []
    # for i in range(0, 50):
    #     act = main()
    #     acceptance.append(act)
    # print(f"acceptance ratio: {round(sum(acceptance) / len(acceptance), 1)}%")
    main()
    
    