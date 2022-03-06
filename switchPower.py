import networkx as nx
from yafs.topology import Topology
import json

def mk_fattree_topo(pods):
    """ pods: int number
    """
    topology_json = {}
    topology_json["entity"] = []
    topology_json["link"] = []

    hosts = []
    agg_switches = []
    core_switches = []

    num_hosts         = int((pods ** 3)/4)
    num_agg_switches  = int(pods * pods)
    num_core_switches = int((pods * pods)/4)

    temp = {'id': 0, 'model':'user','IPT': 5000 * 10 ** 6, 'RAM': 40000,'COST': 3,'WATT':20.0}
    topology_json["entity"].append(temp)

    for i in range (1, num_hosts + 1):
        temp = {'id': i, 'model':'host','IPT': 5000 * 10 ** 6, 'RAM': 40000,'COST': 3,'WATT':20.0}
        hosts.append(i)
        topology_json["entity"].append(temp)

    for i in range(1,num_core_switches + 1):
        temp = {'id': i + num_hosts,'model':'switch','tag':'core'}
        core_switches.append(i + num_hosts)
        topology_json["entity"].append(temp)
        link = {'s': 0, 'd': i + num_hosts, 'BW': 1, 'PR': 1}
        topology_json["link"].append(link)

    for i in range(1, num_agg_switches + 1):
        temp = {'id': i + num_core_switches + num_hosts,'model':'switch','tag':'aggregation'}
        agg_switches.append(i + num_core_switches + num_hosts)
        topology_json["entity"].append(temp)

    host_offset = 0
    for pod in range(pods):
        core_offset = 0
        for sw in range(pods//2):
            switch = agg_switches[(pod*pods) + sw]
            # Connect to core switches
            for port in range(pods//2):
                core_switch = core_switches[core_offset]
                link = {'s': switch, 'd': core_switch, 'BW': 1, 'PR': 1}
                topology_json["link"].append(link)
                core_offset += 1

            # Connect to aggregate switches in same pod
            for port in range(pods//2,pods):
                lower_switch = agg_switches[(pod*pods) + port]
                link = {'s': switch, 'd': lower_switch, 'BW': 1, 'PR': 1}
                topology_json["link"].append(link)

        for sw in range(pods//2,pods):
            switch = agg_switches[(pod*pods) + sw]
            # Connect to hosts
            for port in range(pods//2,pods): # First k/2 pods connect to upper layer
                host = hosts[host_offset]
                # All hosts connect on port 0
                link = {'s': switch, 'd': host, 'BW': 1, 'PR': 1}
                topology_json["link"].append(link)
                host_offset += 1

    return topology_json

def sfc_track(t, sfc):
    """
    t: Topology
    sfc: NFC string ([1,5,7,9,...])
    return: the track through the nodes
    """
    src = 0
    track = [0]
    for vnf in sfc:
        track = track + nx.shortest_path(t.G, src, vnf)[1:]
        src = vnf
    track = track + nx.shortest_path(t.G, src, 0)[1:]
    return track

def create_switch_record(t):
    """
    bandwidth = 0 per port for all switches
    return a json
    """
    switch_port = {}
    for i in t.G.nodes:
        if t.get_nodes_att()[i]['model'] == 'switch':
            switch_port[i] = []
            for j in t.G.neighbors(i):
                temp = {'port': j, 'BW':0}
                switch_port[i].append(temp)
    return switch_port

def bandwidth_record(t, sfc, bandwidth):
    """
    return the record of all switches: bandwidth
    """
    track = sfc_track(t, sfc)
    # print("track = ", track)
    record = create_switch_record(t)
    # print("switch_record = ", json.dumps(record))
    for i in range(len(track)):
        if t.get_nodes_att()[track[i]]['model'] == 'switch':
            for j in range(len(record[track[i]])):
                if record[track[i]][j]['port'] == track[i-1] or record[track[i]][j]['port'] == track[i+1]: 
                    record[track[i]][j]['BW'] += float(bandwidth)
    return record

def switch_power(record):
    """power for all switches"""
    power = 0
    for i in record:
        pow = 39.0
        status = False
        for j in range(len(record[i])):
            bandwidth_temp = record[i][j]['BW']
            pw = 0.0
            if bandwidth_temp > 0: status = True
            if bandwidth_temp > 10 and bandwidth_temp <= 100:
                pw = 0.42
            elif bandwidth_temp > 100 and bandwidth_temp <= 1000:
                pw = 0.48
            elif bandwidth_temp > 1000:     # need to change
                pw = 0.9
            pow += pw
        if status == False: pow = 0.0
        power += pow

    return power


if __name__ == '__main__':
    t = Topology()
    t_json = mk_fattree_topo(4)
    t.load(t_json)

    # range of virtual machine id: from 1 to 16 if 4-fattree
    sfc = [1,3,5,7,11,15]

    # run simulator
    bandwidth = 200 #Mbps

    record = bandwidth_record(t, sfc, bandwidth)
    print(json.dumps(record))

    start_time = 1
    end_time = 100

    power = switch_power(record)
    
    print(power)
