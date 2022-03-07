import numpy as np
import math as m
import matplotlib.pyplot as plt
from sklearn.neural_network import MLPRegressor 
from sklearn.tree import DecisionTreeRegressor
from sklearn.ensemble import RandomForestRegressor
from sklearn.ensemble import GradientBoostingRegressor
# from keras.models import Sequential
# from keras.layers import Dense, Activation, Dropout
from xgboost import XGBRegressor
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_squared_error
from sklearn.metrics import explained_variance_score
from sklearn.model_selection import learning_curve
from sklearn.model_selection import ShuffleSplit
from sklearn.linear_model import LinearRegression
from sklearn import linear_model

def HataModel(carrier_freq, BS_height, MS_height, distance):
    # Apply Hata formular for the input parameters
    a_hr = 0
    if carrier_freq <= 300:
        a_hr = 8.29*(m.log10(1.54*MS_height))**2 - 1.1
    else:
        a_hr = 3.2*(m.log10(11.75*MS_height))**2 - 4.97
    
    loss = 69.55 + 26.16*m.log10(carrier_freq) - 13.82*m.log10(BS_height) - a_hr + (44.9 - 6.55*m.log10(BS_height))*m.log10(distance)
    return loss

def plot_learning_curve(estimator, X, Y, ylim=None, n_jobs=None, train_sizes=np.linspace(.1, 1.0, 5)):
    
    cv = ShuffleSplit(n_splits=20, test_size=0.2, random_state=0) #default split = 10, test_size = 0.2
    fig, ax = plt.subplots()
    ax.set_title("XGBoost Learning Curves")
    if ylim is not None:
        ax.set_ylim(*ylim)
    ax.set_xlabel("Training examples")
    ax.set_ylabel("Score")

    train_sizes, train_scores, test_scores, fit_times, score_times = learning_curve(estimator, X, Y, cv=cv, n_jobs=n_jobs,train_sizes=train_sizes, return_times=True)
    train_scores_mean = np.mean(train_scores, axis=1)
    train_scores_std = np.std(train_scores, axis=1)
    test_scores_mean = np.mean(test_scores, axis=1)
    test_scores_std = np.std(test_scores, axis=1)
    # fit_times_mean = np.mean(fit_times, axis=1)
    # fit_times_std = np.std(fit_times, axis=1)

    # Plot learning curve
    ax.grid()
    ax.fill_between(train_sizes, train_scores_mean - train_scores_std,
                         train_scores_mean + train_scores_std, alpha=0.1,
                         color="r")
    ax.fill_between(train_sizes, test_scores_mean - test_scores_std,
                         test_scores_mean + test_scores_std, alpha=0.1,
                         color="g")
    ax.plot(train_sizes, train_scores_mean, 'o-', color="r",
                 label="Training score")
    ax.plot(train_sizes, test_scores_mean, 'o-', color="g",
                 label="Cross-validation score")
    ax.legend(loc="best")
    plt.show()
    # return plt

def plot_error(estimator, X, Y):
    row, collumn = X.shape
    n_sample_max = collumn
    for i in range(100, n_sample_max,100):
        train_number = int(n_sample_max*0.8)
        X_total_sample = X_total[:,:n_sample_max]
        Y_total_sample = Y_total[:,:n_sample_max]
        X_train, Y_train = X_total_sample[:,:train_number],  Y_total_sample[:,:train_number]
        X_test, Y_test = X_total_sample[:,train_number::], Y_total_sample[:,train_number::]
        X_train, X_test, Y_train, Y_test = X_train.T, X_test.T, Y_train.T, Y_test.T
        estimator = MLPRegressor(hidden_layer_sizes=(10,),  activation='relu', solver='sgd',batch_size='auto',
            learning_rate='adaptive', power_t=0.5, max_iter=i, shuffle=True,
            random_state=None, tol=0.0001, verbose=False, warm_start=False, momentum=0.9,
            nesterovs_momentum=True, early_stopping=False, validation_fraction=0.1, beta_1=0.9, beta_2=0.999)
        # estimator = DecisionTreeRegressor()
        # estimator = LinearRegression()
        # estimator = linear_model.Lasso(alpha=0.1)
        estimator.fit(X_train, Y_train)
        Y_predict = estimator.predict(X_test)

        # mean square error, lower means better
        EV_score = explained_variance_score(Y_test, Y_predict)
        score_ev.append(EV_score)
        R2_score = estimator.score(X_test, Y_test)
        score_r2.append(R2_score)
#main program
N = 10000 # Number of total data
F = 4 # Number of feature
carrier_freq = np.random.uniform(low=150, high=1500,size=(N,))
BS_height = np.random.uniform(low=30, high=200,size=(N,))
MS_height = np.random.uniform(low=1, high=10,size=(N,))
distance = np.random.uniform(low=1, high=20,size=(N,))
Y_total = np.zeros(N)
# X = X.T
for i in range(N):
    Y_total[i] = HataModel(carrier_freq[i], BS_height[i], MS_height[i], distance[i])

X_total = np.concatenate((carrier_freq, BS_height, MS_height, distance), axis = 0)
X_total = X_total.reshape(F,N)
Y_total = Y_total.reshape(1,N)
scaler = StandardScaler()
scaler.fit(X_total)
X_total = scaler.transform(X_total)

X_train = X_total[:,:8000] 
X_test = X_total[:,8000::]

Y_train = Y_total[:8000]
Y_test = Y_total[8000:]

X_train = X_train.T
X_test = X_test.T
Y_train = Y_train.T 
Y_test = Y_test.T

# print(Y_total)
print(X_train.shape)
print(Y_train.shape)


# array to store score value for each epoch
score_ev, score_r2 = [], []
# print(X_train)
n_epoch = 20
n_sample_max = 1000 

# declare estimator
# estimator = MLPRegressor(hidden_layer_sizes=(10,),  activation='relu', solver='sgd',batch_size='auto',
#         learning_rate='adaptive', power_t=0.5, max_iter= 200, shuffle=True,
#         random_state=None, tol=0.0001, verbose=False, warm_start=False, momentum=0.9,
#         nesterovs_momentum=True, early_stopping=False, validation_fraction=0.1, beta_1=0.9, beta_2=0.999)
# estimator = DecisionTreeRegressor()
estimator = GradientBoostingRegressor()
# estimator = XGBRegressor()
# estimator = RandomForestRegressor()
title = "ANN Learning Curves"
plot_learning_curve(estimator, X_total.T, Y_total.T, ylim=(0.0, 1.01), n_jobs=4)

# try ANN with kerras
# estimator = Sequential()
# estimator.add(Dropout(0.2))
# estimator.add(Dense(10))
# estimator.add(Activation('relu'))
# estimator.add(Dense(1))
# estimator.add(Activation('relu'))
# estimator.compile(optimizer='sgd', loss='mse', metrics=['mse'])
# history = estimator.fit(X_train, Y_train,epochs = 100, validation_data = (X_test, Y_test))
# for i in range(10, n_epoch, 10):
# for i in range(100, n_sample_max,100):
#     train_number = int(n_sample_max*0.8)
#     test_number = n_sample_max - train_number
#     X_total_sample = X_total[:,:n_sample_max]
#     Y_total_sample = Y_total[:,:n_sample_max]
#     X_train, Y_train = X_total_sample[:,:train_number],  Y_total_sample[:,:train_number]
#     X_test, Y_test = X_total_sample[:,train_number::], Y_total_sample[:,train_number::]
#     X_train, X_test, Y_train, Y_test = X_train.T, X_test.T, Y_train.T, Y_test.T
#     # estimator = MLPRegressor(hidden_layer_sizes=(10,),  activation='relu', solver='sgd',batch_size='auto',
#     #     learning_rate='adaptive', power_t=0.5, max_iter=i, shuffle=True,
#     #     random_state=None, tol=0.0001, verbose=False, warm_start=False, momentum=0.9,
#     #     nesterovs_momentum=True, early_stopping=False, validation_fraction=0.1, beta_1=0.9, beta_2=0.999)
#     estimator = DecisionTreeRegressor()
#     # estimator = LinearRegression()
#     # estimator = linear_model.Lasso(alpha=0.1)
#     estimator.fit(X_train, Y_train)
#     Y_predict = estimator.predict(X_test)

#     # mean square error, lower means better
#     EV_score = explained_variance_score(Y_test, Y_predict)
#     score_ev.append(EV_score)
#     R2_score = estimator.score(X_test, Y_test)
#     score_r2.append(R2_score)
    
# print('MSE_score:' + str(score_r2))
# # print('R2_score:' + str(R2_score))
# fig, ax = plt.subplots()
# ax.set_ylim([0,1])
# ax.plot(range(100, n_sample_max, 100), score_r2, label="Training error")
# ax.set_xlabel("Number of sample")
# ax.set_ylabel("Error")
# ax.legend()
# plt.show()