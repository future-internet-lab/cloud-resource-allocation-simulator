import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import xgboost as xgb
import math
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
from sklearn.preprocessing import PolynomialFeatures
from sklearn.model_selection import train_test_split
from sklearn.model_selection import KFold
from sklearn.model_selection import cross_val_score
from sklearn.model_selection import GridSearchCV
from sklearn.linear_model import Ridge
from sklearn.linear_model import LinearRegression
from sklearn.linear_model import Lasso
from sklearn.linear_model import ElasticNet
from sklearn.tree import DecisionTreeRegressor
from sklearn.neighbors import KNeighborsRegressor
from sklearn.svm import SVR
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestRegressor
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_squared_error
from sklearn.model_selection import RandomizedSearchCV
from sklearn.model_selection import GridSearchCV

# Loading the data from the file into mydata
# mydata = pd.read_excel('listStoreValue-RecInSys.xlsx', header=None).to_numpy()
# n = len(mydata)
# X = mydata[1:,0:5]
# y = mydata[1:,-1]
def plot_learning_curves(model, X, y):
  X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2)
  train_errors, val_errors = [], []
  for m in range(1, len(X_train), 10):
    model.fit(X_train[:m], y_train[:m])
    y_train_predict = model.predict(X_train[:m]).round()
    y_val_predict = model.predict(X_val).round()
    train_errors.append(mean_squared_error(y_train[:m], y_train_predict))
    val_errors.append(mean_squared_error(y_val, y_val_predict))
  plt.plot(range(1, len(X_train), 10), np.sqrt(train_errors), "r-+", linewidth=2, label="train")
  plt.plot(range(1, len(X_train), 10), np.sqrt(val_errors), "b-", linewidth=3, label="val")
  plt.xlabel("Training Examples")
  plt.ylabel("RMS Errors")
  plt.suptitle(str(model).split("(")[0] + " Learning Curves")
  plt.legend()
  plt.show()

mydata = pd.read_excel('listStoreValue-RecInSys.xlsx', header=None).to_numpy()
n = len(mydata)
X = mydata[1:,0:5]
y = mydata[1:,-1]


# X_train, X_test, y_train, y_test = train_test_split(X,y,train_size=0.9)

#Preprocessing data
scaler = StandardScaler()
scaler.fit(X)
X_scale = scaler.transform(X)
# XGB model
# data_dmatrix = xgb.DMatrix(data=X_scale, label=y)
# params = {"objective":"reg:squarederror",'colsample_bytree': 1,'learning_rate': 0.1,
# 'max_depth': 5, 'alpha': 10, 'lambda': 1, 'gamma': 5, 'tree_method': "approx"}
# cv_results = xgb.cv(dtrain=data_dmatrix, params=params, nfold=3,
# num_boost_round=50,early_stopping_rounds=10,metrics="rmse", as_pandas=True, seed=123)
# print((cv_results["test-rmse-mean"]).tail(1))
# xg_reg = xgb.train(params=params, dtrain=data_dmatrix, num_boost_round=10)
# xgb.plot_importance(xg_reg)
# plt.rcParams['figure.figsize'] = [5, 5]
# plt.show()

# GB model
# FinalModel = GradientBoostingRegressor()
# FinalModel = DecisionTreeRegressor()
# Parameters tuning for RandomForest
# Number of trees in random forest
n_estimators = [int(x) for x in np.linspace(start = 100, stop = 2000, num = 10)]
# Number of features to consider at every split
max_features = ['auto', 'sqrt']
# Maximum number of levels in tree
max_depth = [int(x) for x in np.linspace(10, 110, num = 11)]
max_depth.append(None)
# Minimum number of samples required to split a node
min_samples_split = [2, 5, 10]
# Minimum number of samples required at each leaf node
min_samples_leaf = [1, 2, 4]
# Method of selecting samples for training each tree
bootstrap = [True, False]
# Create the random grid
random_grid = {'n_estimators': n_estimators,
               'max_features': max_features,
               'max_depth': max_depth,
               'min_samples_split': min_samples_split,
               'min_samples_leaf': min_samples_leaf,
               'bootstrap': bootstrap}


# FinalModel = RandomForestRegressor(random_state = 42)
# ModelRandom = RandomizedSearchCV(estimator = FinalModel, scoring = 'neg_mean_squared_error', param_distributions = random_grid, n_iter = 100, cv = 3, verbose=2, random_state=42, n_jobs = -1)
# ModelRandom.fit(X, y)
# print(ModelRandom.best_params_)

# grid_para = {'n_estimators': 733, 'min_samples_split': 2, 'min_samples_leaf': 1, 'max_features': 'auto', 'max_depth': 100, 'bootstrap': True}

# grid_search = GridSearchCV(estimator = ModelRandom, param_grid = grid_para, 
#                           cv = 3, n_jobs = -1, verbose = 2, return_train_score=True)
# grid_search.fit(X_scale, y)
# print(grid_search.best_params_)



FinalModel = RandomForestRegressor(n_estimators = 944, min_samples_split = 2, min_samples_leaf= 1, max_features = 'sqrt', max_depth = 110, bootstrap = True)
# FinalModel = RandomForestRegressor(n_estimators = 2000, min_samples_split = 2, min_samples_leaf= 2, max_features = 'auto', max_depth = 90, bootstrap = True)
# FinalModel.fit(X, y)
plot_learning_curves(FinalModel, X, y)# FinalModel.fit(X, y)

# kfold = KFold(n_splits=10, random_state=7)
# cv_results = cross_val_score(FinalModel, X, y, cv=kfold, scoring="neg_mean_squared_error")
# print("\nFinal Model: "+str(FinalModel).split("(")[0] )
# print(" Cross-validation Error: {}, RMSE: {}".format(cv_results.mean(),np.sqrt(-cv_results.mean())))
# a = np.array([9.0, 1554.0, 13.67, 13.38, 1419.0])
# a = pd.DataFrame(a.reshape(1, -1))
# result = FinalModel.predict(a)

# dataTest = pd.read_excel('feedDataDen.xlsx', header=None).to_numpy()
# X_test = dataTest[10:20,0:5]
# result = FinalModel.predict(X_test)
# result = np.round(result)
# print(result)