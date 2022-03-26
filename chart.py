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


    def plottable(data, marker, color, label):
        N_vnf = [0]
        drop = [0]
        deploy = [0]
        c = 0
        for row in data:
            if row[COLUMN_TIME] != 'time':
                t = int(row[COLUMN_TIME])//60
                if t > c:
                    N_vnf.append(0)
                    drop.append(0)
                    deploy.append(0)
                    c += 1
            if row[COLUMN_ACTION] == 'create':
                N_vnf[t] += int(row[COLUMN_NVNFS])
            if row[COLUMN_ACTION] == 'deploy':
                deploy[t] += 1
            if row[COLUMN_ACTION] == 'drop':
                drop[t] += 1
        load = np.array(N_vnf)/np.array([10]*len(N_vnf))
        acceptance = (np.array(deploy))*100/(np.array(deploy)+np.array(drop))
        plt.plot(load[np.argsort(load)], acceptance[np.argsort(load)], marker=marker, color=color, label=label)
    
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
    plt.ylabel("Acceptance ratio (%)")
    ax.set_title("Acceptance Ratio")
    ax.legend()
    plt.show()
    
    f.close()