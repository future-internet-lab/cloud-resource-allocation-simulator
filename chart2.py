from unittest import result
import matplotlib.pyplot as plt
import numpy as np
import csv

if __name__ == "__main__":
    f = open("edgecloud-simulator-daohiep/results/dist_1_event.csv")
    data = csv.reader(f)

    util = np.array([0]*12)
    count = np.array([0]*12)

    for row in data:
        if row[9] != '-' and row[9] != 'util':
            util[round(float(row[8])/10)] += float(row[9])
            count[round(float(row[8])/10)] += 1
            
        

    plt.plot([i*10 for i in range(12)], util/count, c="r")
    plt.xlabel("Load (%)")
    plt.ylabel("Utilization (%)")

    plt.show()

    # print(power)
    
    f.close()
