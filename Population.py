import random

class SFC_request:
    """VNFs_random_for_SFC: 2-element array, include number of VNFs per SFC, random uniformly distributed
    vnfs: [0,63]"""
    def __init__(self, VNFs_random_for_SFC, vnfs):
        self.VNFs_random_for_SFC = VNFs_random_for_SFC
        self.vnfs = vnfs

    def new_SFC_request(self):
        result = []
        val = random.randint(self.VNFs_random_for_SFC[0], self.VNFs_random_for_SFC[1])
        itr = 0
        while itr < val:
            number = random.randint(self.vnfs[0],self.vnfs[1])
            if number not in result:
                result.append(number)
                itr += 1
        result.sort()
        return result

from scipy.stats import poisson
from numpy import random

class SFC_population:
    def __init__(self, simulate_time, k, time):
        """simulate_time: Set the time to run the simulation. The unit is seconds"""
        self.simulate_time = simulate_time
        self.k = k
        self.time = time
    
    def poisson_distibuted(self):
        """return SFC per hour"""
        return poisson.rvs(mu=self.k, size=self.simulate_time//3600+1)

    def duration_time(self):
        """time: Average duration time, exponentially distributed
        
        2*60*60"""
        return int(random.exponential(scale=self.time))