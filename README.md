# Instruction  
Quick instruction for the event-based edge-cloud simulator  

## Notice  
New simulation is recommended to use to [Python-based version](https://github.com/future-internet-lab/edgecloud-simulator)
Reproduction of the paper ... can be done with the Java version. Please use the **Maven project** to automatically install all the dependencies.

## Dataset for prediction model  
The dataset of traffic from `Nistelrodeseweg road, Netherlands, 2018` can be found in [Traffic-model/2018-weekdays-quarter.csv](https://github.com/future-internet-lab/edgecloud-simulator/blob/Java/Traffic-model/2018-weekdays-quarter.csv). Both `csv` and `excel` files represent the same dataset.  
The overview of the traffic: 
![alt text](https://github.com/future-internet-lab/edgecloud-simulator/blob/Java/traffic.PNG "Traffic overview")
The system metric dataset based on the traffic dataset can be found in [/ML-data-models-2022](https://github.com/future-internet-lab/edgecloud-simulator/tree/Java/ML-data-models-2022) under file name `24h-05`, `24h-10` and `24h-20`, which represent the dataset recorded in `30 mins timewindow`, `1h timewindow` and `2h timewindow`, each.  

Latest tunning paprameter for non-LSTM algorithms can be found at [ML-data-models-2022/fine-tune-parameters.txt](https://github.com/future-internet-lab/edgecloud-simulator/blob/Java/ML-data-models-2022/fine-tune-parameters.txt)  

## How to run  
### Via main file  
The boolean value `ML` indicates the usage of prediction model. Specify `True` to turn it on and `False` to turn it off. According to the simulation results, turning off the prediction model will result in enormous number of migration, which consumes huge energy.  
  
To benchmark the system, there is an option called `IL` that can be set for the variable `type`. `IL` provides an linearly increasing traffic load that does not follow the real traffic dataset. The reason of `IL` type is to benchmark the system with increasing load to maximum.  
  
### Via .jar file
TBD