import simpy

import random



carID = 0
cars = []
class Car():
    def __init__(self, env):
        global carID
        carID += 1
        self.id = carID
        self.env = env
        self.volume = random.randrange(0, 10)
        self.action = self.env.process(self.run())

    def run(self):
        while True:
            distance = random.randrange(1, 5)
            time = min(self.volume, distance)
            print(f"Driving, car-{self.id} has volume {self.volume}/10 distance = {distance} time = {time}")
            self.volume = max(self.volume - distance, 0)
            yield self.env.timeout(time)
            
            self.volume = min(self.volume + 4, 10)
            print(f"Charging, now car-{self.id} has volume {self.volume}/10")
            yield self.env.timeout(4)

            # yield self.env.process(self.drive(distance))
            # yield self.env.process(self.charge())

    def drive(self, distance):
        time = min(self.volume, distance)
        print(f"Driving, car-{self.id} has volume {self.volume}/10 distance = {distance} time = {time}")
        self.volume = max(self.volume - distance, 0)
        yield self.env.timeout(time)

    def charge(self):
        self.volume = min(self.volume + 4, 10)
        print(f"Charging, now car-{self.id} has volume {self.volume}/10")
        yield self.env.timeout(4)



def create_car(env):
    while True:
        global cars
        newcar = Car(env)
        cars.append(newcar)
        print(f"-car-{newcar.id} has been produced at {env.now} with volume = {newcar.volume}/10")
        yield env.timeout(random.randrange(5, 8))



RANDOM_SEED = 42
SIM_TIME = 100

class BroadcastPipe(object):
    def __init__(self, env, capacity=simpy.core.Infinity):
        self.env = env
        self.capacity = capacity
        self.pipes = []

    def put(self, value):
        if not self.pipes:
            raise RuntimeError('There are no output pipes.')
        events = [store.put(value) for store in self.pipes]
        return self.env.all_of(events)

    def get_output_conn(self):
        pipe = simpy.Store(self.env, capacity=self.capacity)
        self.pipes.append(pipe)
        return pipe


def message_generator(name, env, out_pipe):
    while True:
        yield env.timeout(random.randint(6, 10))
        msg = (env.now, '%s says hello at %d' % (name, env.now))
        out_pipe.put(msg)


def message_consumer(name, env, in_pipe):
    while True:
        msg = yield in_pipe.get()
        if msg[0] < env.now:
            print('LATE Getting Message: at time %d: %s received message: %s' %
                  (env.now, name, msg[1]))
        else:
            print('at time %d: %s received message: %s.' %
                  (env.now, name, msg[1]))
        yield env.timeout(random.randint(4, 8))

if __name__ == "__main__":
    # env = simpy.Environment()
    # env.process(create_car(env))
    # random.seed(1)
    # env.run(until=20)

    print('Process communication')
    random.seed(RANDOM_SEED)
    env = simpy.Environment()

    pipe = simpy.Store(env)
    env.process(message_generator('Generator A', env, pipe))
    env.process(message_consumer('Consumer A', env, pipe))

    print('\nOne-to-one pipe communication\n')
    env.run(until=SIM_TIME)

