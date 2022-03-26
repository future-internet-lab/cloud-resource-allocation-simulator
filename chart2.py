import matplotlib.pyplot as plt
import numpy as np
import csv

if __name__ == "__main__":
    folder_name = 'edgecloud-simulator-daohiep/results/4_2_120_420_1090_600/'

    f = open(folder_name+"limited/cent_1_event.csv")
    cent1_wax = csv.reader(f)
    f = open(folder_name+"limited/dist_1_event.csv")
    dist1_wax = csv.reader(f)
    f = open(folder_name+"limited/cent_2d_event.csv")
    cent2_wax = csv.reader(f)
    f = open(folder_name+"limited/dist_2d_event.csv")
    dist2_wax = csv.reader(f)
    f = open(folder_name+"inf/cent_1_event.csv")
    cent_inf_wax = csv.reader(f)
    f = open(folder_name+"inf/dist_1_event.csv")
    dist_inf_wax = csv.reader(f)
    """f = open(folder_name+"VNFGApp/cent_1_event.csv")
    cent_vnfg = csv.reader(f)
    f = open(folder_name+"VNFGApp/dist_1_event.csv")
    dist_vnfg = csv.reader(f)"""

    COLUMN_TIME = 1
    COLUMN_ACTION = 2
    COLUMN_NVNFS = 5
    COLUMN_UTIL = 9

    def plottable(data, marker, color, label):
        N_vnf = [0]
        util = [0]
        n_util = [0]
        c = 0
        for row in data:
            if row[COLUMN_TIME] != 'time':
                t = int(row[COLUMN_TIME])//60
                if t > c:
                    N_vnf.append(0)
                    util.append(0)
                    n_util.append(0)
                    c += 1
            if row[COLUMN_ACTION] == 'create':
                N_vnf[t] += int(row[COLUMN_NVNFS])
            if row[COLUMN_ACTION] == 'deploy':
                util[t] += float(row[COLUMN_UTIL])
                n_util[t] += 1
        load = np.array(N_vnf)/np.array([10]*len(N_vnf))
        utilization = np.array(util)/np.array(n_util)
        plt.plot(load[np.argsort(load)], utilization[np.argsort(load)], marker=marker, color=color, label=label)
            
    fig, ax = plt.subplots()

    plottable(cent1_wax, marker="^", color="c", label='HRE-SFC cent')
    plottable(dist1_wax, marker="o", color="orange", label='HRE-SFC dist')
    plottable(cent2_wax, marker="s", color="yellow", label='HRE-SFC cent + remap')
    plottable(dist2_wax, marker="x", color="black", label='HRE-SFC dist + remap')
    plottable(cent_inf_wax, marker=">", color="brown", label='HRE-SFC cent + bw=inf')
    plottable(dist_inf_wax, marker="<", color="gray", label='HRE-SFC dist + bw=inf')
    # plottable(cent_vnfg, marker="*", color="r", label='VNFG cent')
    # plottable(dist_vnfg, marker="+", color="b", label='VNFG dist')

    plt.xlabel("Load (%)")
    plt.ylabel("Utilization (%)")
    ax.set_title("System utilization")
    ax.legend()
    plt.show()
    
    f.close()
