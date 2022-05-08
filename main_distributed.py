from sim.Simulator import *
from sim.Application import *
from sim.Distribution import *
from sim.Selector import *
from sim.DataCentre import *
from sim.Ingress import *
from sim.Substrate import *
from sim.SubstrateSelector import *
from library import *

from pathlib import Path
import sys
import json
import random
import logging

LAMDA = 5

def main_distributed(randomSeed, appArgs, runtime, argument, selector):
    np.random.seed(randomSeed)
    random.seed(randomSeed)

    subSelector = ShortestPath()
    # subSelector = AlphaSubsel()
    # subSelector = BetaSubsel()

    app = SequenceApp(dist, selector, subSelector, *appArgs)
    # app = WaxmanApp(dist, selector, subSelector, *appArgs)

    apps = [app]

    substrate = Abilene(DCPos=[1, 4, 6, 11], IngressPos=[5, 7, 9, 10], 
                        linkCap=1000,
                        DCArgs=[4, 6, 6, 8], IngressArgs=[apps, apps, apps, apps])

    ######################################## folder name
    spec = f"{n_VNFs[0]}{n_VNFs[1]}_{demand_VNF[0]}{demand_VNF[1]}_{bw[0]}{bw[1]}_{runtime}"
    folder_result = f"{selector.name}/{spec}_lamda{LAMDA}_seed{randomSeed}"
    ########################################

    folder_log = Path(f"results/{folder_result}")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/dist_"
    if(len(argument) == 1):
        folder_log = str(folder_log) + f"{argument[0]}"
    if(len(argument) == 2):
        folder_log = str(folder_log) + f"{argument[0]}{argument[1]}"

    sim = Simulator(substrate, folder_log, logging.DEBUG, *argument)
    sim.run(runtime)

    print("DISTRIBUTED")
    print(f"L = {dist.lamda}, TTL = {avg_TTL}")
    print(f"nvnf = {n_VNFs}, bw = {bw}")
    print(f"runtime = {runtime}, strategy = {argument[0]}")
    print("randomSeed =", randomSeed)
    if(len(argument) == 2):
        print(f"sortmode = {argument[1]}")



if __name__ == "__main__":
    print("-----START SIMULATION-----")
    """
    strategy = int(sys.argv[1])
    if(len(sys.argv) == 3):
        sortmode = sys.argv[2]
        arg = [strategy, sortmode]
    else:
        arg = [strategy]
    """

    # randomSeed = 3

    avg_TTL = 120
    n_VNFs = [4, 20]
    demand_VNF = [15, 30]
    bw = [10, 50]
    runtime = 600
    appArgs = [avg_TTL, n_VNFs, demand_VNF, bw, [0.5, 0.5]]

    for i in range(10):
        dist = Poisson(lamda=LAMDA)
        for randomSeed in range(1,5):
            main_distributed(randomSeed, appArgs, runtime, [1], WaxmanSelector())
            # main_distributed(randomSeed, appArgs, runtime, [2,"d"], WaxmanSelector())
            main_distributed(randomSeed, appArgs, runtime, [1], VNFG())
            main_distributed(randomSeed, appArgs, runtime, [1], ONP_SFO(30))
            main_distributed(randomSeed, appArgs, runtime, [1], ONP_SFO(50))
        LAMDA += 1
    