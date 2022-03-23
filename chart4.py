import matplotlib.pyplot as plt
import numpy as np
import csv

if __name__ == "__main__":
    f = open("edgecloud-simulator-daohiep/4_20_80_1020_10150_120/VNFGApp/cent_1_event.csv")
    data = csv.reader(f)

    pff = [0]*11
    count = [0]*11

    for row in data:
        if row[10] != '-' and row[9] != 'util':
            i = round(float(row[9])/10)
            pff[i] = (pff[i]*count[i] + float(row[12]))/(count[i]+1)
            count[i] += 1
    
    while True:
        try:
            pff.remove(0)
        except:
            break

    plt.plot([i*10 for i in range(len(pff))], pff, c="r")
    plt.xlabel("Utilization (%)")
    plt.ylabel("Power Per SFC (W)")

    plt.show()
    
    f.close()
