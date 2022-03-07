import numpy as np

class Placement:
    def __init__(self) -> None:
        pass

class VNFs_placement(Placement):
    vnf_record = []
    
    def __init__(self, topo, max_vnfs_per_host):
        i = 0
        temp_record = []
        for j in topo.g.nodes:
            if topo.g.nodes[j]['type'] == 'host' and topo.g.nodes[j]['type'] != 'host_user':
                for itr in range(max_vnfs_per_host):
                    temp = [i,j]
                    temp_record.append(temp)
                    i += 1
        self.vnf_record = np.array(temp_record)

    def num_VNFs(self):
        return len(self.vnf_record)