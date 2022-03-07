from SFCSimulator.Placement import *
from SFCSimulator.Population import *
from SFCSimulator.Topology import *
from SFCSimulator.Simulator import *

from pathlib import Path

RANDOM_SEED = 1

def main():

    random.seed(RANDOM_SEED)
    np.random.seed(RANDOM_SEED)
    
    folder_log = Path("results")
    folder_log.mkdir(parents=True, exist_ok=True)
    folder_log = str(folder_log) + "/result"

    topo = Topology()
    topo.mk_topo(4)

    # Placement Algorithm
    placement = VNFs_placement(topo, 4)

    number_of_vnfs = placement.num_VNFs()   # 64
    vnfs = [0,number_of_vnfs-1]       # 0->63
    VNFs_random_for_SFC = [4,20]

    SFC_placement = SFC_request(VNFs_random_for_SFC, vnfs)

    # Population Algorithm
    simulate_time = 24*60*60     # second
    population = SFC_population(simulate_time, 8, 2*60*60)

    # Simulate Application
    simu = Simulator(topo, placement, SFC_placement, population)
    simu.run_simulate()

if __name__ == '__main__':
    main()