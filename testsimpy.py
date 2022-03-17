import simpy
import random
import copy



class Machine():
    def __init__(self, env):
        self.env = env
        self.event = self.env.process(self.working())
        self.env.process(self.breaking())
        self.done = 0
        self.broken = False


    def working(self):
        while True:
            done_in = random.randint(5, 10)
            print(f"{self.env.now}: task {self.done + 1}: need: {done_in} --------------")
            while done_in:
                try:
                    start = self.env.now
                    yield self.env.timeout(done_in)
                    done_in = 0
                except simpy.Interrupt:
                    self.broken = True
                    done_in -= self.env.now - start
                    print(f"{self.env.now}: task {self.done + 1}: remain: {done_in}")
                    self.broken = False
            self.done += 1


    def breaking(self):
        while True:
            time_to_failure = random.randint(3, 6)
            yield self.env.timeout(time_to_failure)
            print(f"{self.env.now}: fail {time_to_failure}")
            if not self.broken:
                
                self.event.interrupt()



env = simpy.Environment()
m1 = Machine(env)
env.run(until=30)
