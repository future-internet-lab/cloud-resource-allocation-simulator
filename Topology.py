import networkx as nx

class Topology():
    def __init__(self) -> None:
        pass

    def mk_topo(self,pods):
        num_hosts         = int((pods ** 3)//4)
        num_agg_switches  = int(pods * pods)
        num_core_switches = int((pods * pods)//4)

        hosts = [(i, {'type':'host'})for i in range (1, num_hosts + 1)]
        core_switches = [(i+num_hosts, {'type':'switch'})for i in range(1,num_core_switches + 1)]
        agg_switches = [(i+num_core_switches+num_hosts, {'type':'switch'})for i in range(num_core_switches + 1,num_core_switches + num_agg_switches+ 1)]
        server = [(0, {'type':'host_user'})]

        self.g = nx.Graph()
        self.g.add_nodes_from(hosts)
        self.g.add_nodes_from(core_switches)
        self.g.add_nodes_from(agg_switches)
        self.g.add_nodes_from(server)

        host_offset = 0
        for pod in range(pods):
            core_offset = 0
            for sw in range(pods//2):
                switch = agg_switches[(pod*pods) + sw][0]
                # Connect to core switches
                for port in range(int(pods//2)):
                    core_switch = core_switches[core_offset][0]
                    self.g.add_edge(switch,core_switch)
                    self.g[switch][core_switch]['bw'] = 0
                    core_offset += 1

                # Connect to aggregate switches in same pod
                for port in range(pods//2,pods):
                    lower_switch = agg_switches[(pod*pods) + port][0]
                    self.g.add_edge(switch,lower_switch)
                    self.g[switch][lower_switch]['bw'] = 0

            for sw in range(pods//2,pods):
                switch = agg_switches[(pod*pods) + sw][0]
                # Connect to hosts
                for port in range(pods//2,pods): # First k/2 pods connect to upper layer
                    host = hosts[host_offset][0]
                    # All hosts connect on port 0
                    self.g.add_edge(switch,host)
                    self.g[switch][host]['bw'] = 0
                    host_offset += 1

        for i in range(17,21):
            self.g.add_edge(0,i)
            self.g[0][i]['bw'] = 0
        
        return self.g

    def number_of_host(self):
        count = 0
        for i in self.g.nodes:
            if self.g.nodes[i]['type'] == 'host': count += 1
        return count

    def number_of_switch(self):
        count = 0
        for i in self.g.nodes:
            if self.g.nodes[i]['type'] == 'switch': count += 1
        return count