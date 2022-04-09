from sim.Simulator import *
from sim.Application import *
from sim.Distribution import *
from sim.Selector import *
from sim.DataCentre import *
from sim.Ingress import *
from sim.Substrate import *
from library import *
from threading import Thread
from pathlib import Path

import threading
import numpy as np
import matplotlib.pyplot as plt
import sys
import json
import random



def main_centralized(randomSeed, appArgs, runtime, argument):
    np.random.seed(randomSeed)
    random.seed(randomSeed)
    
    dist = Poisson(lamda=2)

    # selector = SimpleSelector()
    selector = WaxmanSelector()
    # selector = VNFG_node_splitting()
    app = WaxmanApp(dist, selector, *appArgs)

    apps = [app]

    substrate = Abilene(DCPos=[2], IngressPos=[5, 7, 9, 10], 
                        linkCap=100,
                        DCArgs=[10], IngressArgs=[apps, apps, apps, apps])

    # folder name
    spec = f"{n_VNFs[0]}{n_VNFs[1]}_{demand_VNF[0]}{demand_VNF[1]}_{bw[0]}{bw[1]}_{runtime}"
    folder_result = f"{selector.name}/{spec}_seed{randomSeed}"

    folder_log = Path(f"results/{folder_result}")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/cent_"
    if(len(argument) == 1):
        folder_log = str(folder_log) + f"{argument[0]}"
    if(len(argument) == 2):
        folder_log = str(folder_log) + f"{argument[0]}{argument[1]}"

    sim = Simulator(substrate, folder_log, *argument)
    sim.run(runtime)

    print("CENTRALIZED")
    print(f"L = {dist.lamda}, TTL = {avg_TTL}")
    print(f"nvnf = {n_VNFs}, bw = {bw}")
    print(f"runtime = {runtime}, strategy = {argument[0]}")
    if(len(argument) == 2):
        print(f"sortmode = {argument[1]}")



def main_distributed(randomSeed, appArgs, runtime, argument):
    np.random.seed(randomSeed)
    random.seed(randomSeed)
    
    dist = Poisson(lamda=2)

    # selector = SimpleSelector()
    selector = WaxmanSelector()
    # selector = VNFG_node_splitting()
    app = WaxmanApp(dist, selector, *appArgs)

    apps = [app]

    substrate = Abilene(DCPos=[1, 4, 6, 11], IngressPos=[5, 7, 9, 10], 
                        linkCap=100,
                        DCArgs=[4, 6, 6, 8], IngressArgs=[apps, apps, apps, apps])

    # folder name
    spec = f"{n_VNFs[0]}{n_VNFs[1]}_{demand_VNF[0]}{demand_VNF[1]}_{bw[0]}{bw[1]}_{runtime}"
    folder_result = f"{selector.name}/{spec}_seed{randomSeed}"

    folder_log = Path(f"results/{folder_result}")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/dist_"
    if(len(argument) == 1):
        folder_log = str(folder_log) + f"{argument[0]}"
    if(len(argument) == 2):
        folder_log = str(folder_log) + f"{argument[0]}{argument[1]}"

    sim = Simulator(substrate, folder_log, *argument)
    sim.run(runtime)

    print("DISTRIBUTED")
    print(f"L = {dist.lamda}, TTL = {avg_TTL}")
    print(f"nvnf = {n_VNFs}, bw = {bw}")
    print(f"runtime = {runtime}, strategy = {argument[0]}")
    if(len(argument) == 2):
        print(f"sortmode = {argument[1]}")



if __name__ == "__main__":
    print("-----START SIMULATION-----")
    # strategy = int(sys.argv[1])
    # if(len(sys.argv) == 3):
    #     sortmode = sys.argv[2]
    #     arg = [strategy, sortmode]
    # else:
    #     arg = [strategy]

    randomSeed = 2405

    dist = Poisson(lamda=2)
    avg_TTL = 120
    n_VNFs = [4, 20]
    demand_VNF = [10, 40]
    bw = [10, 20]
    runtime = 200
    appArgs = [avg_TTL, n_VNFs, demand_VNF, bw, [0.5, 0.5]]

    threads = []

    t1 = threading.Thread(target=main_centralized, args=(randomSeed, appArgs, runtime, [1],))
    threads.append(t1)
    t2 = threading.Thread(target=main_distributed, args=(randomSeed, appArgs, runtime, [1],))
    threads.append(t2)
    t3 = threading.Thread(target=main_centralized, args=(randomSeed, appArgs, runtime, [2, "d"],))
    threads.append(t3)
    t4 = threading.Thread(target=main_distributed, args=(randomSeed, appArgs, runtime, [2, "d"],))
    threads.append(t4)
    t5 = threading.Thread(target=main_centralized, args=(randomSeed, appArgs, runtime, [2, "n"],))
    threads.append(t5)
    t6 = threading.Thread(target=main_distributed, args=(randomSeed, appArgs, runtime, [2, "n"],))
    threads.append(t6)
    t7 = threading.Thread(target=main_centralized, args=(randomSeed, appArgs, runtime, [2, "i"],))
    threads.append(t7)
    t8 = threading.Thread(target=main_distributed, args=(randomSeed, appArgs, runtime, [2, "i"],))
    threads.append(t8)

    for thread in threads:
        thread.start()

    for thread in threads:
        thread.join()
    
    # main(arg)