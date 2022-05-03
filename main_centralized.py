from sim.Simulator import *
from sim.Application import *
from sim.Distribution import *
from sim.Selector import *
from sim.DataCentre import *
from sim.Ingress import *
from sim.Substrate import *
from library import *

from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt
import sys
import json
import random



def main_centralized(randomSeed, appArgs, runtime, argument):
    np.random.seed(randomSeed)
    random.seed(randomSeed)

    selector = WaxmanSelector()
    # selector = VNFG()
    # selector = ONP_SFO(15)

    subSelector = ShortestPath()

    # app = SimpleApp(dist, selector, *appArgs)
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

    sim = Simulator(substrate, folder_log, logging.DEBUG, *argument)
    sim.run(runtime)

    print("CENTRALIZED")
    print(f"L = {dist.lamda}, TTL = {avg_TTL}")
    print(f"nvnf = {n_VNFs}, bw = {bw}")
    print(f"runtime = {runtime}, strategy = {argument[0]}")
    print("randomSeed =", randomSeed)
    if(len(argument) == 2):
        print(f"sortmode = {argument[1]}")


if __name__ == "__main__":
    print("-----START SIMULATION-----")
    strategy = int(sys.argv[1])
    if(len(sys.argv) == 3):
        sortmode = sys.argv[2]
        arg = [strategy, sortmode]
    else:
        arg = [strategy]

    randomSeed = 2405

    dist = Poisson(lamda=2)
    avg_TTL = 120
    n_VNFs = [12, 12]
    demand_VNF = [25, 25]
    bw = [50, 50]
    runtime = 800
    appArgs = [avg_TTL, n_VNFs, demand_VNF, bw, [0.5, 0.5]]

    main_centralized(randomSeed, appArgs, runtime, arg)
