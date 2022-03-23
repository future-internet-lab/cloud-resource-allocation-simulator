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

    def utilization(data):
        util = np.array([0]*12)
        count = np.array([0]*12)
        for row in data:
            if row[9] != '-' and row[9] != 'util':
                util[round(float(row[8])/10)] += float(row[9])
                count[round(float(row[8])/10)] += 1
        return util/count
            
    fig, ax = plt.subplots()
    plt.plot([i*10 for i in range(12)], utilization(cent1_wax), marker="^", color="c", label='HRE-SFC cent')
    plt.plot([i*10 for i in range(12)], utilization(dist1_wax), marker="o", color="orange", label='HRE-SFC dist')
    # plt.plot([i*10 for i in range(12)], utilization(cent2_wax), marker="s", color="yellow", label='HRE-SFC cent + remap')
    # plt.plot([i*10 for i in range(12)], utilization(dist2_wax), marker="x", color="black", label='HRE-SFC dist + remap')
    plt.plot([i*10 for i in range(12)], utilization(cent_inf_wax), marker=">", color="brown", label='HRE-SFC cent + bw=inf')
    plt.plot([i*10 for i in range(12)], utilization(dist_inf_wax), marker="<", color="gray", label='HRE-SFC dist + bw=inf')
    # plt.plot([i*10 for i in range(12)], utilization(cent_vnfg), marker="*", color="r", label='VNFG cent')
    # plt.plot([i*10 for i in range(12)], utilization(dist_vnfg), marker="+", color="b", label='VNFG dist')
    plt.xlabel("Load (%)")
    plt.ylabel("Utilization (%)")
    ax.set_title("System utilization")
    ax.legend()
    plt.show()
    
    f.close()
