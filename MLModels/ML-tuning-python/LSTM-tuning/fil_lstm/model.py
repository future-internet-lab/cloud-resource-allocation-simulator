from sklearn.feature_selection import SelectFromModel
from sklearn.metrics import r2_score
import torch
from torch import nn, optim
from torch.utils.data import DataLoader
import numpy as np
from torch.optim.lr_scheduler import ExponentialLR, LinearLR, LambdaLR

class Model(nn.Module):
    def __init__(
        self, 
        input_size=12,
        hidden_size=100,
        num_layers=10,
        dropout=0.2,
        sequence_length=4,
    ):
        super(Model, self).__init__()
        self.input_size = input_size
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.dropout = dropout
        self.sequence_length = sequence_length
        self.first_train = True

        # LSTM setting
        self.lstm = nn.LSTM(
            input_size=self.input_size,
            hidden_size=self.hidden_size,
            num_layers=num_layers,
            dropout=self.dropout,
        )

        # Compute output
        self.fc1 = nn.Linear(hidden_size, hidden_size)
        self.relu1 = nn.ReLU()
        self.fc = nn.Linear(hidden_size, 1)
        self.relu = nn.ReLU()

    def forward(self, x, prev_state):

        output, state = self.lstm(x, prev_state)
        output = self.fc1(output)
        output = self.relu1(output)
        output = self.fc(output)
        output = self.relu(output)
        return output[:,-1,:], state

    def init_state(self):
        return (torch.zeros(self.num_layers, 
                    self.sequence_length, self.hidden_size),
                torch.zeros(self.num_layers, 
                    self.sequence_length, self.hidden_size))

    def train(self, train_dataset, test_dataset, epochs=5, batch_size=5,
        lr=0.0001):
        '''
        Train the model
        '''
        dataloader = DataLoader(train_dataset, batch_size=batch_size)
        criterion = nn.MSELoss()
        if self.first_train:
            self.optimizer = optim.Adam(self.parameters(), lr=lr)
            self.losses = []
        scheduler = ExponentialLR(self.optimizer, gamma=0.9)
        # scheduler = LinearLR(optimizer, start_factor=0.5, total_iters=4)

        for epoch in range(epochs):
            state_h, state_c = self.init_state()

            for batch, (x,y) in enumerate(dataloader):
                self.optimizer.zero_grad()

                out, (state_h, state_c) = self.forward(x, (state_h, state_c))
                # print('X:', x.size())
                # print('y:', y.size())
                # print('out:', out.size())
                loss = criterion(out, y[:,-1,:])


                state_h = state_h.detach()
                state_c = state_c.detach()

                loss.backward()
                self.optimizer.step()

            scheduler.step()
            if epoch%5==0:
                train_loss = self.evaluate(
                    train_dataset, batch_size=len(train_dataset))

                test_loss = self.evaluate(
                    test_dataset, batch_size=len(test_dataset))
                
                print('Epoch:', epoch, '\t Train loss: ', 
                    train_loss, '\t', 'Test loss: ', test_loss)
                # print({ 'epoch': epoch, 'RMSEloss': np.sqrt(loss.item()) })
                self.losses.append([train_loss[0], test_loss[0]])

        self.first_train = False

    def evaluate(self, dataset, batch_size=5):
        dataloader = DataLoader(dataset, batch_size=batch_size)
        state_h, state_c = self.init_state()
        res = 0
        with torch.no_grad():
            for batch, (x,y) in enumerate(dataloader):
                out, (state_h, state_c) = self.forward(x, (state_h, state_c))
                mse = nn.MSELoss()
                loss = mse(y[:,-1,:],out).detach()
                r2 = r2_score(y[:,-1,:].numpy(), out.numpy())
                res += loss
        return np.sqrt(res.item()), r2

        


