import torch

class FILDataset(torch.utils.data.Dataset):
    def __init__(self, raw_data, raw_label, sequence_length=4):
        self.raw_data = raw_data
        self.raw_label = raw_label
        self.sequence_length = sequence_length
        self.clean()

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        x = torch.zeros((self.sequence_length, len(self.data.columns)))
        if idx < self.sequence_length - 1:
            start = 0
        else:
            start = idx - self.sequence_length + 1
        
        data_t = torch.Tensor(self.data.values)
        label_t = torch.Tensor(self.raw_label.values).reshape((-1,1))
        
        x = data_t[start:idx+1]
        y = label_t[start:idx+1]
        if idx < self.sequence_length - 1:
            padding_x = torch.zeros((self.sequence_length-idx-1, len(self.data.columns)))
            padding_y = torch.zeros((self.sequence_length-idx-1, 1))
            x = torch.cat((padding_x, x), dim=0)
            y = torch.cat((padding_y, y), dim=0)

        return x, y
        

    def clean(self):
        '''
        Clean data.
            Remove unnecessary columns
        '''
        self.data = self.raw_data.drop(
            columns=['# VNF Receive current','# VNF Receive future', '# VNF Density future'])
        