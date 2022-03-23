from unittest import result
import matplotlib.pyplot as plt
import numpy as np
import csv

if __name__ == "__main__":
    f = open("edgecloud-simulator-daohiep/results/dist_1_event.csv")
    data = csv.reader(f)

    power = np.array([0]*11)
    count = np.array([0]*11)

    for row in data:
        if row[9] != '-' and row[9] != 'util':
            i = round(float(row[9])/10)
            power[i] = (power[i]*count[i] + float(row[11]))/(count[i]+1)
            count[i] += 1
            
        

    plt.plot([i*10 for i in range(11)], power, c="r")
    plt.xlabel("Utilization (%)")
    plt.ylabel("Power Consumption (W)")

    plt.show()

    # print(power)
    
    f.close()
