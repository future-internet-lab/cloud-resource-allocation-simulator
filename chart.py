from sim.Plotchart import *
import sys

folder_name = 'results/'
bw_range = str(sys.argv[1])
dir = [["ONP_SFO/420_1040_"+bw_range+"_800_seed2405/cent_1_event.csv", "ONP_SFO/420_1040_"+bw_range+"_800_seed2405/dist_1_event.csv"],
        ["VNFG/420_1040_"+bw_range+"_800_seed2405/cent_1_event.csv", "VNFG/420_1040_"+bw_range+"_800_seed2405/dist_1_event.csv"],
        ["WaxmanSelector/420_1040_"+bw_range+"_800_seed2405/inf/cent_1_event.csv", "WaxmanSelector/420_1040_"+bw_range+"_800_seed2405/inf/dist_1_event.csv"],
        ["WaxmanSelector/420_1040_"+bw_range+"_800_seed2405/limited/cent_1_event.csv", "WaxmanSelector/420_1040_"+bw_range+"_800_seed2405/limited/dist_1_event.csv"],
        ["WaxmanSelector/420_1040_"+bw_range+"_800_seed2405/limited/cent_2d_event.csv", "WaxmanSelector/420_1040_"+bw_range+"_800_seed2405/limited/dist_2d_event.csv"]
]
label = [["ONP_SFO cent", "ONP_SFO dist"],
        ["VNFG cent", "VNFG dist"],
        ["HRE cent inf", "HRE dist inf"],
        ["HRE cent", "HRE dist"],
        ["HRE cent + remap", "HRE dist + remap"]
]

chart = Chart(folder_name,dir,label)
# chart.markers = ["o","o","<","<",">",">","*","*","^","^"]
chart.markers = [None]*10
paper_input = [0, 1, 2, 3]
all_input = [0, 1, 2, 3, 4, 5, 6, 7]

# input bw range: 1020, 1050, 1090

chart.Acceptance(round=False)
chart.Utilization(round=False)
chart.Power([0,1,2,3,6,7,8,9])
chart.PowerPerSFC([0,1,2,3,6,7,8,9])
chart.ActiveServer([0,1,2,3,6,7,8,9])