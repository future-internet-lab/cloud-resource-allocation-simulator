from sim.Simulator import *
from sim.Application import *
from sim.SFC import *
from sim.Distribution import *
from sim.Selector import *
from sim.DataCentre import *
from sim.Ingress import *
from library import *

from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt
import sys
import json



def main():
    np.random.seed(2405)
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
    selector = SimpleSelector()
    avg_TTL = 120 # average time_to_live of SFC, exponential distribution
    n_VNFs_range = [4, 20] # number of VNFs per SFC, uniform distribution
    bw_range = [10, 90] # bw for each virtual link, uniform distribution
    arg = [avg_TTL, n_VNFs_range, bw_range]
    app = SimpleApplication("SimpleApp", dist, selector, *arg)
    apps = [app]

    # big topo
    topology = DistributedTopo()
    DCs = []
    DCs.append(DataCentre(2, fat_tree(10, switchSpecs, serverSpecs)))
    Ingresses = []
    Ingresses.append(Ingress(5, apps))
    Ingresses.append(Ingress(7, apps))
    Ingresses.append(Ingress(9, apps))
    Ingresses.append(Ingress(10, apps))

    sim = Simulator(topology, DCs, Ingresses, 1, folder_log)
    sim.run(120) # runtime = 120 minutes

    # draw graph, un completed
    time = []
    n_SFCs = []
    running = []
    failed = []
    acceptance = []
    power = []
    with open("results/result_event.csv", "r") as f:
        data = csv.reader(f)
        for row in data:
            if row[2] == "deployed" or row[2] == "remove":
                time.append(int(row[1]))
                n_SFCs.append(int(row[9]))
                running.append(int(row[10]))
                failed.append(int(row[11]))
                acceptance.append(round((int(row[9]) - int(row[11])) / int(row[9]) * 100))
                power.append(float(row[12]))
    plt.plot(time, power)
    plt.show()
    # plt.plot(time, acceptance)
    # plt.show()

if __name__ == "__main__":
    print("-----START SIMULATION-----")
    main()