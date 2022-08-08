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



def main(randomSeed, appArgs, runtime, argument, DCPos, IngressPos):
    np.random.seed(randomSeed)
    random.seed(randomSeed)

    selector = HRE_ver2() # thuật toán chọn server trong DC

    subSelector = ShortestPath() # thuật toán chọn DC

    app = WaxmanApp(dist, selector, subSelector, *appArgs) # cấu hình app

    apps = [app]

    substrate = Abilene(DCPos=DCPos, IngressPos=IngressPos, linkCap=100,
                        DCArgs=[4, 6, 6, 8], IngressArgs=[apps, apps, apps, apps], n_clusters=0)

    ######################################## folder name
    spec = f"{DCPos[0]}{DCPos[1]}{DCPos[2]}{DCPos[3]}/{IngressPos[0]}{IngressPos[1]}{IngressPos[2]}{IngressPos[3]}"
    folder_result = f"{selector.name}/dist/{spec}"
    ########################################

    folder_log = Path(f"results/{folder_result}")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/dist_"
    if(len(argument) == 1):
        folder_log = str(folder_log) + f"{argument[0]}"
    if(len(argument) == 2):
        folder_log = str(folder_log) + f"{argument[0]}{argument[1]}"

    sim = Simulator(substrate, folder_log, logging.DEBUG, True, *argument)
    sim.run(runtime)

    logging.warning("DISTRIBUTED")
    logging.warning(f"L = {dist.lamda}, TTL = {avg_TTL}")
    logging.warning(f"nvnf = {n_VNFs}, bw = {bw}")
    logging.warning(f"runtime = {runtime}, strategy = {argument[0]}")
    logging.warning(f"randomSeed = {randomSeed}")
    if(len(argument) == 2):
        logging.warning(f"sortmode = {argument[1]}")



if __name__ == "__main__":
    print("-----START SIMULATION-----")
    strategy = int(sys.argv[1])
    if(len(sys.argv) == 3):
        sortmode = sys.argv[2]
        arg = [strategy, sortmode]
    else:
        arg = [strategy]

    randomSeed = 1111

    dist = Poisson(lamda=2)
    avg_TTL = 120
    n_VNFs = [4, 10]
    demand_VNF = [15, 30]
    bw = [10, 90]
    runtime = 200
    appArgs = [avg_TTL, n_VNFs, demand_VNF, bw, [0.5, 0.5]]

    # chay 1 truong hop
    main(randomSeed, appArgs, runtime, arg, [1, 4, 6, 11], [5, 7, 9, 10])
