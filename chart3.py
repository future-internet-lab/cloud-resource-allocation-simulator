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

    def power_con(data, marker, color, label):
        power = [0]*12
        count = [0]*12
        for row in data:
            if row[9] != '-' and row[9] != 'util':
                i = round(float(row[9])/10)
                power[i] = (count[i]*power[i] + float(row[12]))/(count[i]+1)
                count[i] += 1
        while len(power)>0:
            if power[-1] == 0: power = power[:-1]
            else: break
        plt.plot([i*10 for i in range(len(power))], power, marker=marker, color=color, label=label)
    
    fig, ax = plt.subplots()
    
    power_con(cent1_wax, marker="^", color="c", label='HRE-SFC cent')
    power_con(dist1_wax, marker="o", color="orange", label='HRE-SFC dist')
    # power_con(cent2_wax, marker="s", color="yellow", label='HRE-SFC cent + remap')
    # power_con(dist2_wax, marker="x", color="black", label='HRE-SFC dist + remap')
    power_con(cent_inf_wax, marker=">", color="brown", label='HRE-SFC cent + bw=inf')
    power_con(dist_inf_wax, marker="<", color="gray", label='HRE-SFC dist + bw=inf')
    # power_con(cent_vnfg, marker="*", color="r", label='VNFG cent')
    # power_con(dist_vnfg, marker="+", color="b", label='VNFG dist')

    # plt.plot(power_con(cent1_wax)[0], power_con(cent1_wax)[1], marker="^", color="c", label='HRE-SFC cent')
    # plt.plot(power_con(dist1_wax)[0], power_con(dist1_wax)[1], marker="o", color="orange", label='HRE-SFC dist')
    # plt.plot(power_con(cent2_wax)[0], power_con(cent2_wax)[1], marker="s", color="yellow", label='HRE-SFC cent + remap')
    # plt.plot(power_con(dist2_wax)[0], power_con(dist2_wax)[1], marker="x", color="black", label='HRE-SFC dist + remap')
    # plt.plot(power_con(cent_inf_wax)[0], power_con(cent_inf_wax)[1], marker=">", color="brown", label='HRE-SFC cent + bw=inf')
    # plt.plot(power_con(dist_inf_wax)[0], power_con(dist_inf_wax)[1], marker="<", color="gray", label='HRE-SFC dist + bw=inf')
    # plt.plot(power_con(cent_vnfg)[0], power_con(cent_vnfg)[1], marker="*", color="r", label='VNFG cent')
    # plt.plot(power_con(dist_vnfg)[1], power_con(dist_vnfg)[1], marker="+", color="b", label='VNFG dist')
    plt.xlabel("Utilization (%)")
    plt.ylabel("Power Consumption (W)")
    ax.set_title("Power consumption of the substrate network")
    ax.legend()
    plt.show()
    
    f.close()
