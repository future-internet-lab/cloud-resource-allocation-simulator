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
    COLUMN_UTIL = 9
    COLUMN_PPERSFC = 13

    def power_per_sfc(data, marker, color, label):
        power = [0]*10
        count = [0]*10
        for row in data:
            if row[COLUMN_ACTION] == 'create':
                i = int(float(row[COLUMN_UTIL])/10)
                if i >= 10: i = 9
                power[i] = (count[i]*power[i] + float(row[COLUMN_PPERSFC]))/(count[i]+1)
                count[i] += 1
        while len(power)>0:
            if power[-1] == 0: power = power[:-1]
            else: break
        plt.plot([(i+1)*10 for i in range(len(power))], power, marker=marker, color=color, label=label)
    
    fig, ax = plt.subplots()
    
    power_per_sfc(cent1_wax, marker="^", color="c", label='HRE-SFC cent')
    power_per_sfc(dist1_wax, marker="o", color="orange", label='HRE-SFC dist')
    power_per_sfc(cent2_wax, marker="s", color="yellow", label='HRE-SFC cent + remap')
    power_per_sfc(dist2_wax, marker="x", color="black", label='HRE-SFC dist + remap')
    # power_per_sfc(cent_inf_wax, marker=">", color="brown", label='HRE-SFC cent + bw=inf')
    # power_per_sfc(dist_inf_wax, marker="<", color="gray", label='HRE-SFC dist + bw=inf')
    # power_per_sfc(cent_vnfg, marker="*", color="r", label='VNFG cent')
    # power_per_sfc(dist_vnfg, marker="+", color="b", label='VNFG dist')

    plt.xlabel("Utilization (%)")
    plt.ylabel("Power Per SFC (W)")
    ax.set_title("Average consumed power per serving SFC")
    ax.legend()
    plt.show()

    plt.show()
    
    f.close()
