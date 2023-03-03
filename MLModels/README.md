## where to look for files
* The series of folder `0.5 1.0 2.0` contains `jpmml` files that can be used directly in Eclipse with the help of [JPMML library](https://github.com/jpmml). `LSTM` models are compressed as `.zip` files in `2.0` folder, they are torch_script which can be read in Eclipse by [Deep Java Library](https://djl.ai/).
* `Datasets` folder contains three excel files that are the recording of system's metrics when real traffic is fed as input in 178 days. The first 11 columns represents X, the last column represents y for supervised learning.
* Fine tunning parameters for non-Deep Learning algorithms can be found in the two `.txt` files.
* Fine tunning of non-DL algorithms were extracted from GridSearch. GridSearch files can be found in directory `ML-tyning-python/Others`.
* Fine tunning of DL algorithm can be found in directory  `ML-tyning-python/LSTM`
