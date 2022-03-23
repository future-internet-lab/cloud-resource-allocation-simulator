from unittest import result
import matplotlib.pyplot as plt
import numpy as np
import csv

if __name__ == "__main__":
    f = open("edgecloud-simulator-daohiep/4_20_80_1020_10150_120/VNFGApp/cent_1_event.csv")
    data = csv.reader(f)

    create = np.array([0]*12)
    drop = np.array([0]*12)

    for row in data:
        if row[2] == 'create':
            create[round(float(row[8])/10)] += 1
        if row[2] == 'drop':
            drop[round(float(row[8])/10)] += 1
        

    plt.plot([i*10 for i in range(12)], (1 - drop/create)*100, c="r")
    plt.xlabel("Load (%)")
    plt.ylabel("Acceptance ratio (%)")

    plt.show()

    # print(power)
    
    f.close()
