from abc import ABC, abstractmethod

import numpy as np



class Distribution(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def next(self): pass





class Poisson(Distribution):
    def __init__(self, lamda):
        super().__init__()
        self.lamda = lamda

    def next(self, time):
        # duration = round(60 * np.random.exponential(scale=1/self.lamda))
        lamda = self.lamda * (time // 60 + 1)
        duration = round(60 * np.random.exponential(scale=1/lamda))
        if duration == 0: duration = 1
        return duration