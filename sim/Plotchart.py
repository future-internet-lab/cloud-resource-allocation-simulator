import matplotlib.pyplot as plt
import numpy as np
import csv

COLUMN_TIME = 1
COLUMN_ACTION = 2
COLUMN_NVNFS = 5
COLUMN_DEMAND = 6
COLUMN_UTIL = 9
COLUMN_MIGRATION = 10
COLUMN_POWER = 12
COLUMN_PPERSFC = 13
COLUMN_ACTIVE_SERVER = 14

def round_load(load, ydata):
    new_data = [0]*10
    count = [0]*10
    for i in range(len(load)):
        if load[i] > 100: itr = 9
        else:
            itr = round(load[i]/10)-1
        count[itr] += 1
        new_data[itr] += ydata[i]
    result_load = []
    result_y = []
    for i in range(len(count)):
        if count[i] > 0:
            result_load.append((i+1)*10)
            result_y.append(new_data[i]/count[i])
    
    return result_load, result_y


def average(value, time, period, lastValue):
    sum = lastValue * (time[0] % period)
    for i in range(len(time)):
        if(i == len(time) - 1):
            time[i] = ((time[0] // period + 1) * period) - (time[i] % 60)
        else:
            time[i] = time[i+1] - time[i]
        sum += value[i] * time[i]
    avg = sum / period
    return avg

class Chart():
    def __init__(self, folder_name, dir, label, open_mode):
        self.folder_name = folder_name
        self.dir = dir
        self.label = label
        self.open_mode = open_mode
        self.colors = []
        for color in ["black","red","green","orange","cyan"]:
            for _ in range(self.open_mode):
                self.colors.append(color)

        self.markers = []
        for marker in ["o","<",">","*","^"]:
            for _ in range(self.open_mode):
                self.markers.append(marker)

        self.line_style = [None,'--','-','-.',':']

    def open_data(self):
        data = []
        label = []
        linestyle = []

        def foo(dir, note): # i dont know how to name this function @@
            for itr in range(self.open_mode):
                f = open(self.folder_name + dir[itr])
                data.append(csv.reader(f))
                label.append(note[itr])
                linestyle.append(self.line_style[itr])

        for itr in range(len(self.dir)):
            foo(self.dir[itr], self.label[itr])

        return data, label, linestyle

    def Acceptance(self, data_id=[]):
        """
        Acceptance ratio: Load (%) - Acceptance Ratio (%)
        """
        data, label, ls = self.open_data()
        def draw_each_line(data, marker, color, label, linestyle):
            demand_list = []
            demand_time = []
            deploy_time = []
            drop_time = []
            for row in data:
                if row[COLUMN_TIME] != "time":
                    if row[COLUMN_ACTION] == "create":
                        demand_list.append(int(row[COLUMN_DEMAND]))
                        demand_time.append(int(row[COLUMN_TIME]))
                    if row[COLUMN_ACTION] in ["deploy"]:
                        deploy_time.append(int(row[COLUMN_TIME]))
                    if row[COLUMN_ACTION] in ["drop"]:
                        drop_time.append(int(row[COLUMN_TIME]))
            runtime = max(demand_time)
            load = []
            acceptance = []
            for i in range(len(demand_time)):
                if demand_time[i] + 60 <= runtime:
                    start = demand_time[i]
                    _demand = 0
                    for j in range(i, len(demand_time)):
                        if demand_time[j] - start <= 60:
                            _demand += demand_list[j]
                        else:
                            # print(f"from {demand_time[i]}({demand_list[i]}) to {demand_time[j-1]}({demand_list[j-1]}): {_demand}")
                            _deploy = 0
                            _drop = 0
                            for k in range(len(deploy_time)):
                                if deploy_time[k] >= demand_time[i] and deploy_time[k] <= demand_time[j-1]:
                                    _deploy += 1
                            for k in range(len(drop_time)):
                                if drop_time[k] >= demand_time[i] and drop_time[k] <= demand_time[j-1]:
                                    _drop += 1
                            acceptance.append(_deploy / (_deploy + _drop) * 100)
                            load.append(_demand / 250)
                            break
                else:
                    break

            combine = []
            for i in range(len(load)):
                combine.append({
                    "acceptance": acceptance[i],
                    "load": load[i]
                })

            roundAcceptance = []
            roundLoad = [5 * i for i in range(1, 21)]
            for rload in roundLoad:
                combine.sort(key=lambda e : abs(e["load"] - rload))
                roundAcceptance.append((combine[0]["acceptance"] + combine[1]["acceptance"]) / 2)

            acceptance = np.array(roundAcceptance)
            load = np.array(roundLoad)

            plt.plot(load[np.argsort(load)], acceptance[np.argsort(load)], linestyle=linestyle, marker=marker, color=color, label=label)
            

        fig, ax = plt.subplots()

        if data_id == []:
            for itr in range(len(data)):
                draw_each_line(data[itr], self.markers[itr], color=self.colors[itr], label=label[itr], linestyle=ls[itr])
        else:
            for itr in data_id:
                draw_each_line(data[itr], self.markers[itr], color=self.colors[itr], label=label[itr], linestyle=ls[itr])

        plt.xlabel("Load (%)")
        plt.ylabel("Acceptance ratio (%)")
        ax.set_title("Acceptance Ratio")
        ax.legend()
        plt.show()



    def Utilization(self, data_id=[]):
        """
        System utilization: Load (%) - Utilization (%)
        """
        data, label, ls = self.open_data()
        def draw_each_line(data, marker, color, label, linestyle):
            demand_list = []
            demand_time = []
            util_list = []
            util_time = []
            for row in data:
                if row[COLUMN_TIME] != "time":
                    if row[COLUMN_ACTION] == "create":
                        demand_list.append(int(row[COLUMN_DEMAND]))
                        demand_time.append(int(row[COLUMN_TIME]))
                    if row[COLUMN_ACTION] in ["deploy", "drop"]:
                        util_list.append(float(row[COLUMN_UTIL]))
                        util_time.append(int(row[COLUMN_TIME]))
            runtime = max(demand_time)
            load = []
            util = []
            for i in range(len(demand_time)):
                if demand_time[i] + 60 <= runtime:
                    start = demand_time[i]
                    _demand = 0
                    _util = 0
                    n_util = 0
                    for j in range(i, len(demand_time)):
                        if demand_time[j] - start <= 60:
                            _demand += demand_list[j]
                            _util += util_list[j]
                            n_util += 1
                        else:
                            util.append(_util / n_util)
                            load.append(_demand / 250)
                            _demand = 0
                            _util = 0
                            n_util = 0
                            break
                else:
                    break
            
            combine = []
            for i in range(len(load)):
                combine.append({
                    "util": util[i],
                    "load": load[i]
                })

            roundUtil = []
            roundLoad = [5 * i for i in range(2, 21)]
            for rload in roundLoad:
                combine.sort(key=lambda e : abs(e["load"] - rload))
                roundUtil.append((combine[0]["util"] + combine[1]["util"]) / 2)

            util = np.array(roundUtil)
            load = np.array(roundLoad)

            plt.plot(load[np.argsort(load)], util[np.argsort(load)], linestyle=linestyle, marker=marker, color=color, label=label)

        
        fig, ax = plt.subplots()

        if(data_id == []):
            for itr in range(len(data)):
                draw_each_line(data[itr], marker=self.markers[itr], color=self.colors[itr], label=label[itr], linestyle=ls[itr])
        else:
            for itr in data_id:
                draw_each_line(data[itr], marker=self.markers[itr], color=self.colors[itr], label=label[itr], linestyle=ls[itr])

        plt.xlabel("Load (%)")
        plt.ylabel("Utilization (%)")
        ax.set_title("System utilization")
        ax.legend()
        plt.show()

    def Power(self, data_id=[]):
        """
        Power consumption: Utilization (%) - Power Consumption (W)
        """
        data, label, ls = self.open_data()

        def draw_each_line(data, marker, color, label, linestyle):
            util = []
            power = []
            for row in data:
                if row[COLUMN_ACTION] in ["deploy", "remove"]:
                    _util = float(row[COLUMN_UTIL])
                    _power = float(row[COLUMN_POWER])
                    if not _util in util:
                        util.append(_util)
                        power.append([_power])
                    else:
                        idx = util.index(_util)
                        power[idx].append(_power)
            power = [sum(arr) / len(arr) for arr in power]

            combine = []
            for i in range(len(util)):
                combine.append({
                    "power": power[i],
                    "util": util[i]
                })

            roundPower = []
            roundUtil = [5 * i for i in range(1, 21)]
            for rutil in roundUtil:
                combine.sort(key=lambda e : abs(e["util"] - rutil))
                # if abs(combine[0]["util"] - rutil) < 5:
                roundPower.append((combine[0]["power"] + combine[1]["power"]) / 2)
                # else:
                    # roundPower.append(0)

            util = np.array(util)
            power = np.array(power)

            plt.plot(util[np.argsort(util)], power[np.argsort(util)], marker=marker, color=color, label=label, linestyle=linestyle)

        fig, ax = plt.subplots()

        if(data_id == []):
            for itr in range(len(data)):
                draw_each_line(data[itr], marker=None, color=self.colors[itr], label=label[itr], linestyle=ls[itr])
        else:
            for itr in data_id:
                draw_each_line(data[itr], marker=None, color=self.colors[itr], label=label[itr], linestyle=ls[itr])

        plt.xlabel("Utilization (%)")
        plt.ylabel("Power Consumption (W)")
        ax.set_title("Power consumption of the substrate network")
        ax.legend()
        plt.show()


    def PowerPerSFC(self, data_id = None):
        """
        Average consumed power per serving SFC: Utilization (%) - Power Per SFC (W)
        """
        data, label, ls = self.open_data()
        def draw(data, reso, marker, color, label, linestyle):
            n_value = 100 // reso
            pps = [0] * n_value
            util = [0] * n_value
            count = [0] * n_value
            for row in data:
                if(row[COLUMN_ACTION] in ["deploy", "remove"]):
                    i = int(float(row[COLUMN_UTIL]) / reso)
                    if i >= n_value: i = n_value-1
                    pps[i] += float(row[COLUMN_PPERSFC])
                    util[i] += float(row[COLUMN_UTIL])
                    count[i] += 1
            _length = len(count)
            i = 0
            while i < _length:
                try:
                    if count[i] == 0:
                        pps.pop(i)
                        util.pop(i)
                        count.pop(i)
                        _length -= 1
                    else:
                        i += 1
                except:
                    break
            pps = np.array(pps) / np.array(count)
            util = np.array(util) / np.array(count)
            plt.plot(util, pps, marker=marker, color=color, label=label, linestyle=linestyle)
            
            
        fig, ax = plt.subplots()

        if data_id == None:
            for itr in range(len(data)):
                draw(data[itr], 5, marker=self.markers[itr], color=self.colors[itr], label=label[itr], linestyle=ls[itr])
        else:
            for itr in data_id:
                draw(data[itr], 5, marker=self.markers[itr], color=self.colors[itr], label=label[itr], linestyle=ls[itr])

        
        plt.xlabel("Utilization (%)")
        plt.ylabel("Power Per SFC (W)")
        ax.set_title("Average consumed power per serving SFC")
        ax.legend()
        plt.show()

    def ActiveServer(self, data_id = None):
        """
        Number of active servers: Utilization (%) - Number of Active Servers
        """
        data, name, ls = self.open_data()
        def draw(data, reso, marker, color, label, linestyle):
            n_value = 100 // reso
            aserver = [0] * n_value
            util = [0] * n_value
            count = [0] * n_value
            for row in data:
                if(row[COLUMN_ACTION] in ["deploy", "remove"]):
                    i = int(float(row[COLUMN_UTIL]) / reso)
                    if i >= n_value: i = n_value-1
                    aserver[i] += int(row[COLUMN_ACTIVE_SERVER])
                    util[i] += float(row[COLUMN_UTIL])
                    count[i] += 1

            _length = len(count)
            i = 0
            while i < _length:
                try:
                    if count[i] == 0:
                        aserver.pop(i)
                        util.pop(i)
                        count.pop(i)
                        _length -= 1
                    else:
                        i += 1
                except:
                    break

            aserver = np.array(aserver) // np.array(count)
            util = np.array(util) / np.array(count)
            plt.plot(util, aserver, marker=marker, color=color, label=label, linestyle=linestyle)
        
        fig, ax = plt.subplots()

        if data_id == None:
            for itr in range(len(data)):
                draw(data[itr], 5, marker=self.markers[itr], color=self.colors[itr], label=name[itr], linestyle=ls[itr])
        else:
            for itr in data_id:
                draw(data[itr], 5, marker=self.markers[itr], color=self.colors[itr], label=name[itr], linestyle=ls[itr])

        plt.xlabel("Utilization (%)")
        plt.ylabel("Number of Active Servers")
        ax.set_title("Number of active servers")
        ax.legend()
        plt.show()

    def Figure6(self, start_time,end_time,time_format='hour',data_id = None):
        """Fluctuation of system utilization:

        Time (hour) - Utilization (%)

        start_time, end_time: Thời gian bắt đầu và kết thúc

        time_format: Định dạng thời gian của start_time và end_time, có giá trị là 'hour' và 'minute'. Mặc định là 'hour'
        """
        data, name, ls = self.open_data()
        if time_format == 'hour':
            start_time = start_time*60
            end_time = end_time*60

        def plottable(data, marker, color, label, linestyle):
            time = []
            util = []
            for row in data:
                if row[1] == 'time' or int(row[COLUMN_TIME]) < start_time or int(row[COLUMN_TIME]) > end_time or row[COLUMN_ACTION] == 'create' or row[COLUMN_UTIL] == '-': continue
                else:
                    time.append(int(row[COLUMN_TIME]))
                    util.append(float(row[COLUMN_UTIL]))

            plt.plot(np.array(time)/60, np.array(util), marker=marker, color=color, label=label, linestyle=linestyle)
        
        fig, ax = plt.subplots()

        if data_id == None:
            for itr in range(len(data)):
                plottable(data[itr], marker=self.markers[itr], color=self.colors[itr], label=name[itr], linestyle=ls[itr])
        else:
            for itr in data_id:
                plottable(data[itr], marker=self.markers[itr], color=self.colors[itr], label=name[itr], linestyle=ls[itr])

        plt.xlabel("Time (hour)")
        plt.ylabel("Utilization (%)")
        ax.set_title("Fluctuation of system utilization")
        ax.legend()
        plt.show()