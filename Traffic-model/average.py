# importing module
from pandas import *
import math
import csv 

# reading CSV file
data = read_csv("2018-weekdays-quarter.csv")

# average traffic
traffic = data["traffic"]
ave_traffic = []
day = len(traffic)/96
# print()
for i in range(0, 96, 1):
    #ave_traffic.append(0.0)
    # print(ave_traffic[i])
    temp = 0
    for j in range(i, len(traffic), 96):
        temp += traffic[j]
        # print("temp value: %f", temp)
        # print("Traffic {} value: {} ".format(j, traffic[j]))
        # print(j)
        # print(ave_traffic[i])
    ave_traffic.append(temp)
# for x in ave_traffic:

#     print(math.ceil(x/day))

with open('temp.csv', 'w', newline='') as f:
    writer = csv.writer(f)
    for x in ave_traffic:
        writer.writerow([math.ceil(x/day)])
print("Finish")