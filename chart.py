import matplotlib.pyplot as plt
import numpy as np
import csv

if __name__ == "__main__":
    folder_name = 'edgecloud-simulator-daohiep/results/'

    f = open(folder_name+"limited/cent_1_event.csv")
    cent1_wax = csv.reader(f)
    f = open(folder_name+"limited/dist_1_event.csv")
    dist1_wax = csv.reader(f)
    f = open(folder_name+"limited/cent_2d_event.csv")
    cent2_wax = csv.reader(f)
    f = open(folder_name+"limited/dist_2d_event.csv")
    dist2_wax = csv.reader(f)
    f = open(folder_name+"infinitybw/cent_1_event.csv")
    cent_inf_wax = csv.reader(f)
    f = open(folder_name+"infinitybw/dist_1_event.csv")
    dist_inf_wax = csv.reader(f)
    """f = open(folder_name+"VNFGApp/cent_1_event.csv")
    cent_vnfg = csv.reader(f)
    f = open(folder_name+"VNFGApp/dist_1_event.csv")
    dist_vnfg = csv.reader(f)"""

    def plottable(data):
        create = np.array([0]*12)
        drop = np.array([0]*12)
        for row in data:
            if row[2] == 'create':
                create[round(float(row[8])/10)] += 1
            if row[2] == 'drop':
                drop[round(float(row[8])/10)] += 1
        return (1 - drop/create)*100
        
    fig, ax = plt.subplots()
    plt.plot([i*10 for i in range(12)], plottable(cent1_wax), marker="^", color="c", label='HRE-SFC cent')
    plt.plot([i*10 for i in range(12)], plottable(dist1_wax), marker="o", color="orange", label='HRE-SFC dist')
    plt.plot([i*10 for i in range(12)], plottable(cent2_wax), marker="s", color="yellow", label='HRE-SFC cent + remap')
    plt.plot([i*10 for i in range(12)], plottable(dist2_wax), marker="x", color="black", label='HRE-SFC dist + remap')
    plt.plot([i*10 for i in range(12)], plottable(cent_inf_wax), marker=">", color="brown", label='HRE-SFC cent + bw=inf')
    plt.plot([i*10 for i in range(12)], plottable(dist_inf_wax), marker="<", color="gray", label='HRE-SFC dist + bw=inf')
    # plt.plot([i*10 for i in range(12)], plottable(cent_vnfg), marker="*", color="r", label='VNFG cent')
    # plt.plot([i*10 for i in range(12)], plottable(dist_vnfg), marker="+", color="b", label='VNFG dist')
    plt.xlabel("Load (%)")
    plt.ylabel("Acceptance ratio (%)")
    ax.set_title("Acceptance Ratio")
    ax.legend()
    plt.show()
    
    f.close()
