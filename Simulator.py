import numpy as np
import random
import networkx as nx
from heapq import merge
import csv

class Simulator:
    def __init__(self, topo, placement, SFC_placement, population):
        self.topo = topo
        self.placement = placement
        self.SFC_placement = SFC_placement
        self.population = population

    def working_host(self,sfc_id):
        a = self.all_SFC[sfc_id]['vnf']
        result = []
        for i in a:
            if self.placement.vnf_record[i,1] not in result: result.append(self.placement.vnf_record[i,1])
        return result

    def accept_connect(self, sfc_id, road):
        topo_cal = self.topo.g.copy()
        bandwidth = random.randint(10,90)
        for i in range(len(road)-1):
            if topo_cal.nodes[road[i]]['type'] == 'host': bandwidth = random.randint(10,90)
            topo_cal.edges[road[i],road[i+1]]['bw'] += bandwidth   # Mbps
        self.all_SFC[sfc_id]['status'] = 1
        self.all_SFC[sfc_id]['topo'] = topo_cal

    def calculate_all_bandwidth(self):
        topo_cal = self.topo.g.copy()
        for i in range(len(self.all_SFC)):
            if self.all_SFC[i]['status'] == 1:      # start to calculating
                for (a,b) in topo_cal.edges:
                    topo_cal.edges[a,b]['bw'] = topo_cal.edges[a,b]['bw'] + self.all_SFC[i]['topo'].edges[a,b]['bw']
        return topo_cal

    def find_road(self, sfc_id, topo_cal):
        """Tìm đường đi cho SFC có id
        
        trả về mảng 1 chiều road"""
        src = 0
        road = [0]
        hosts = self.working_host(sfc_id) + [0]
        for host in hosts:
            allow_path = []
            for path in nx.all_simple_paths(topo_cal, src, host):
                for i in range(len(path)-1): # check
                    if topo_cal.edges[path[i],path[i+1]]['bw'] < 1024: allow_path.append(path)
            src = host
            if len(allow_path) == 0:    # không còn đường
                road = [0]
                break
            
            else: 
                index = []
                for i in range(len(allow_path)):
                    index.append(len(allow_path[i]))
                road = road + allow_path[np.argsort(np.array(index))[0]][1:]
                
        return road

    def run_simulate(self):
        # create all SFC
        num_sfc_array = self.population.poisson_distibuted()
        hour = 0
        self.all_SFC = []
        record = []
        ids = 0
        for num_sfc in num_sfc_array:
            for itr in range(num_sfc):
                start_time = random.randint(0,3599)+hour*60*60
                end_time = self.population.duration_time() + start_time

                self.all_SFC.append({'vnf':self.SFC_placement.new_SFC_request(),'status':0,'topo':None})

                record.append([ids,start_time,0])
                record.append([ids,end_time,1])
                
                ids += 1

            hour += 1
        
        self.record = np.array(record)
        c = np.argsort(self.record[:,1])
        self.record = self.record[c]
        # create csv
        self.create_csv()
        for i in range(len(self.record)):
            if self.record[i][1] > self.population.simulate_time: break
            print('----------process: {} % ----------------'.format((self.record[i][1]*100)/self.population.simulate_time))
            if self.record[i][2] == 0: # request to connect
                topo_cal = self.calculate_all_bandwidth()
                road_temp = self.find_road(self.record[i][0],topo_cal)
                
                if len(road_temp) == 1: #
                    print('id: {}, time: {}, từ chối kết nối'.format(self.record[i][0],self.record[i][1] ))
                    # huy k cho ket noi
                    self.log_event(self.record[i][1],self.record[i][0],'tu_choi_ket_noi','-')
                    
                else: 
                    self.accept_connect(self.record[i][0],road_temp)
                    print('id: {}, time: {}, kết nối'. format(self.record[i][0],self.record[i][1]))
                    print('switch power: {}'.format(self.switch_power(topo_cal)))
                    print('host power: {}'.format(self.host_power()))
                    self.log_event(self.record[i][1],self.record[i][0],'ket_noi',self.working_host(self.record[i][0]))
                    self.log_energy(self.record[i][1],self.record[i][0],self.switch_power(topo_cal),self.host_power(),self.switch_power(topo_cal)+self.host_power())

            elif self.record[i][2] == 1 and self.all_SFC[self.record[i][0]]['status'] == 1:   #terminal
                self.all_SFC[self.record[i][0]]['status'] = 0
                print('id: {}, time: {}, hủy kết nối'. format(self.record[i][0],self.record[i][1]) )
                print('switch_power: {}'.format(self.switch_power(self.calculate_all_bandwidth())))
                print('host power: {}'.format(self.host_power()))
                self.log_event(self.record[i][1],self.record[i][0],'huy_ket_noi','-')
                self.log_energy(self.record[i][1],self.record[i][0],self.switch_power(topo_cal),self.host_power(),self.switch_power(topo_cal)+self.host_power())


    def switch_power(self, topo_cal):
        def bw_power(bandwidth):
            if bandwidth >= 10 and bandwidth < 100: return 0.42
            elif bandwidth >= 100 and bandwidth < 1000: return 0.48
            elif bandwidth >= 1000: return 0.9
            else: return 0.0
        power = float(self.topo.number_of_switch() * 39)
        for (a,b) in topo_cal.edges:
            if topo_cal.nodes[a]['type'] == 'switch' and topo_cal.nodes[b]['type'] == 'switch':
                power = power + bw_power(topo_cal.edges[a,b]['bw'])*2
            else:
                power = power + bw_power(topo_cal.edges[a,b]['bw'])
        return power

    def host_power(self):
        temp = []
        for i in range(len(self.all_SFC)):
            if self.all_SFC[i]['status'] == 1:
                temp = list(dict.fromkeys( merge(temp, self.all_SFC[i]['vnf'])))
        score = [0]*self.topo.number_of_host()
        for i in temp:
            score[self.placement.vnf_record[i,1]-1] += 1
        result = 0.0
        for i in score:
            if i == 1: result += 232.9
            elif i == 2: result += 260.7
            elif i == 3: result += 288.6
            elif i == 4: result += 316.4
        return result

    def create_csv(self, folder_log=None):
        eventFields = ['time', 'id', 'action', 'SFCs']
        energyFields = ['time', 'id', 'switch_power', 'host_power', 'power']

        path = 'results'
        if(folder_log is not None):
            path = folder_log

        self.__fEvent = open(f"results/{path}_event.csv", mode="w", newline="")
        self.__wEvent = csv.writer(self.__fEvent)
        self.__wEvent.writerow(eventFields)

        self.__fEnergy = open(f"results/{path}_energy.csv", mode="w", newline="")
        self.__wEnergy = csv.writer(self.__fEnergy)
        self.__wEnergy.writerow(energyFields)

    
    def log_event(self, time, id, action, SFCs):
        self.__wEvent.writerow([time, id, action, SFCs])


    def log_energy(self, time, id, switch_power, host_power, power):
        self.__wEnergy.writerow([time, id, switch_power, host_power, power])
