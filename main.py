from sim.Simulator import *
from sim.Application import *
from sim.SFC import *
from sim.Distribution import *
from sim.Selector import *
from library import *

from pathlib import Path
import sys
import json



def main():
    # define log file
    folder_log = Path("results")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/result"

    # create physical topology
    switchSpecs = {
        "basePower": 39,
        "portPower": [0.42, 0.48, 0.9]
    }
    serverSpecs = {
        "RAM": 4,
        "power": [205.1, 232.9, 260.7, 288.6, 316.4]
    }
    t = fat_tree(4, switchSpecs, serverSpecs)

    distribution = Poisson(lamda=8)
    selector = SimpleSelector()

    # create one app
    avg_TTL = 120 # average time_to_live of SFC, exponential distribution
    n_VNFs_range = [4, 20] # number of VNFs per SFC, uniform distribution
    bw_range = [10, 90] # bw for each virtual link, uniform distribution
    arg = [avg_TTL, n_VNFs_range, bw_range]
    app = SimpleApplication("SimpleApp", distribution, selector, *arg)
    
    # create a list of apps to put into simulator
    apps = [app]

    sim = Simulator(t, apps, folder_log)
    sim.run(120) # runtime = 120 minutes



if __name__ == "__main__":
    print("-----START SIMULATION-----")
    main()