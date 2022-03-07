import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import xgboost as xgb
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
mydata = pd.read_excel('listStoreValue-RecInSys.xlsx', header=None).to_numpy()
n = len(mydata)
X = mydata[:,0:5]
y = mydata[:,-1]

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

# Parameters tuning for GB
# Number of trees in GB

range_estimators = [int(x) for x in np.linspace(start = 10, stop = 1000, num = 20)]
range_loss = ['ls', 'lad', 'huber', 'quantile']
range_lr = [float(x) for x in np.linspace(0.01, 0.9, num = 50)]
range_subsample = [float(x) for x in np.linspace(0.0, 1.0, num = 10)]
# Maximum number of levels in tree
range_minsamsplit = [float(x) for x in np.linspace(0.5, 5.0, num = 5)]
range_minsamleaf = [float(x) for x in np.linspace(0.5, 5.0, num = 5)]
range_mwfl = [float(x) for x in np.linspace(0.0, 1.0, num = 5)]
range_maxdepth = [int(x) for x in np.linspace(start = 3, stop = 100, num = 30)]
range_maxfea = ['auto', 'sqrt', 'log2', None]

# Create the random grid
random_grid = {'n_estimators': range_estimators,
               'max_features': range_maxfea,
               'max_depth': range_maxdepth,
               'min_samples_split': range_minsamsplit,
               'min_samples_leaf': range_minsamleaf,
               'loss': range_loss,
               'subsample':range_subsample,
               'min_weight_fraction_leaf': range_mwfl,
               'learning_rate': range_lr}


FinalModel = GradientBoostingRegressor(random_state = 42)
ModelRandom = RandomizedSearchCV(estimator = FinalModel, scoring = 'neg_mean_squared_error', param_distributions = random_grid, n_iter = 100, cv = 10, verbose=2, random_state=42, n_jobs = -1)
ModelRandom.fit(X_scale, y)
print(ModelRandom.best_params_)

# grid_para = {'n_estimators': 733, 'min_samples_split': 2, 'min_samples_leaf': 1, 'max_features': 'auto', 'max_depth': 100, 'bootstrap': True}

# grid_search = GridSearchCV(estimator = ModelRandom, param_grid = grid_para, 
#                           cv = 3, n_jobs = -1, verbose = 2, return_train_score=True)
# grid_search.fit(X_scale, y)
# print(grid_search.best_params_)



# FinalModel = GradientBoostingRegressor(subsample = 0.8888888888888888, n_estimators = 114, min_weight_fraction_leaf = 0.0, min_samples_split = 0.5, min_samples_leaf = 0.5, max_features = None, max_depth = 73, loss = 'ls', learning_rate = 0.5185714285714286)

# kfold = KFold(n_splits=10, random_state=7)
# cv_results = cross_val_score(FinalModel, X_scale, y, cv=kfold, scoring="neg_mean_squared_error")
# print("\nFinal Model: "+str(FinalModel).split("(")[0] )
# print(" Cross-validation Error: {}, RMSE: {}".format(cv_results.mean(),np.sqrt(-cv_results.mean())))