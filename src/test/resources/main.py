from lightgbm import LGBMClassifier, LGBMRegressor
from pandas import DataFrame

import lightgbm
import numpy
import pandas
import re

def load_csv(name, categorical_columns = []):
	df = pandas.read_csv("csv/" + name, na_values = ["N/A", "NA"])
	for categorical_column in categorical_columns:
		df[categorical_column] = df[categorical_column].astype("category")
	return df

def store_csv(df, name):
	df.to_csv("csv/" + name, index = False)

def store_lgbm(lgbm, name):
	lgbm.booster_.save_model("lgbm/" + name)

#
# Multi-class classification
#

def build_iris(name, objective = "multiclass", boosting_type = "gbdt", num_iteration = 0, **kwargs):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	lgbm = LGBMClassifier(objective = objective, boosting_type = boosting_type, n_estimators = (11 if name.endswith("NA") else 200), **kwargs)
	lgbm.fit(X, y)

	func = "Classification"

	if boosting_type != "gbdt":
		func = (boosting_type.upper() + func)

	if num_iteration == 0:
		store_lgbm(lgbm, func + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	species = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"]).replace("setosa", "0").replace("versicolor", "1").replace("virginica", "2")
	species_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)", "probability(2)"])
	store_csv(pandas.concat((species, species_proba), axis = 1), func + name + ".csv")

build_iris("Iris")
build_iris("Iris", num_iteration = 7)
build_iris("Iris", boosting_type = "rf", bagging_freq = 3, bagging_fraction = 0.75)
build_iris("IrisNA")
build_iris("IrisNA", num_iteration = 7)

#
# Binary classification
#

def build_audit(name, objective = "binary", boosting_type = "gbdt", num_iteration = 0, **kwargs):
	df = load_csv(name + ".csv", ["Employment", "Education", "Marital", "Occupation", "Gender", "Deductions"])
	X = df[["Age", "Employment", "Education", "Marital", "Occupation", "Income", "Gender", "Deductions", "Hours"]]
	y = df["Adjusted"]

	lgbm = LGBMClassifier(objective = objective, boosting_type = boosting_type, n_estimators = 31, **kwargs)
	lgbm.fit(X, y)

	func = "Classification"

	if boosting_type != "gbdt":
		func = (boosting_type.upper() + func)

	if num_iteration == 0:
		store_lgbm(lgbm, func + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	adjusted = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	adjusted_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((adjusted, adjusted_proba), axis = 1), func + name + ".csv")

build_audit("Audit")
build_audit("Audit", num_iteration = 17)
build_audit("Audit", boosting_type = "rf", bagging_freq = 10, bagging_fraction = 0.75)
build_audit("AuditNA", objective = "cross_entropy")
build_audit("AuditNA", objective = "cross_entropy", num_iteration = 17)

def build_audit_invalid():
	df = load_csv("AuditInvalid.csv", ["Employment", "Education", "Marital", "Occupation", "Gender", "Deductions"])
	X = df[["Age", "Employment", "Education", "Marital", "Occupation", "Income", "Gender", "Deductions", "Hours"]]
	booster = lightgbm.Booster(model_file = "lgbm/ClassificationAudit.txt")

	result = booster.predict(X)

	adjusted = DataFrame(numpy.where(result < 0.5, 0, 1), columns = ["_target"])
	adjusted_proba = DataFrame(numpy.vstack((1.0 - result, result)).transpose(), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((adjusted, adjusted_proba), axis = 1), "ClassificationAuditInvalid.csv")

build_audit_invalid()

def build_audit_bin(name, objective = "binary"):
	df = load_csv(name + ".csv")
	X = df[["Age", "Income", "Hours", "Employment", "Education", "Marital", "Occupation", "Gender"]]
	y = df["Adjusted"]

	Xt = pandas.get_dummies(X, columns = ["Employment", "Education", "Marital", "Occupation", "Gender"])

	lgbm = LGBMClassifier(objective = objective, n_estimators = 31)
	lgbm.fit(Xt, y, categorical_feature = [i for i in range(3, Xt.shape[1] - 3)])

	name = re.sub("Audit", "AuditBin", name);

	store_lgbm(lgbm, "Classification" + name + ".txt")

	adjusted = DataFrame(lgbm.predict(Xt), columns = ["_target"])
	adjusted_proba = DataFrame(lgbm.predict_proba(Xt), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((adjusted, adjusted_proba), axis = 1), "Classification" + name + ".csv")

build_audit_bin("Audit")
build_audit_bin("AuditNA")

def build_versicolor(name, objective = "binary", num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[["Sepal.Length", "Sepal.Width", "Dummy", "Petal.Length", "Petal.Width"]]
	y = df["Species"]

	lgbm = LGBMClassifier(objective = objective, n_estimators = 11)
	lgbm.fit(X, y)

	if num_iteration == 0:
		store_lgbm(lgbm, "Classification" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	versicolor = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	versicolor_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((versicolor, versicolor_proba), axis = 1), "Classification" + name + ".csv")

build_versicolor("Versicolor")
build_versicolor("Versicolor", num_iteration = 9)

#
# Regression
#

def build_auto(name, objective = "regression", boosting_type = "gbdt", num_iteration = 0, **kwargs):
	df = load_csv(name + ".csv", ["cylinders", "model_year", "origin"])
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	lgbm = LGBMRegressor(objective = objective, boosting_type = boosting_type, n_estimators = 31, **kwargs)
	lgbm.fit(X, y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"])

	func = "Regression"

	if boosting_type != "gbdt":
		func = (boosting_type.upper() + func)

	if num_iteration == 0:
		store_lgbm(lgbm, func + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	mpg = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(mpg, func + name + ".csv")

build_auto("Auto")
build_auto("Auto", num_iteration = 17)
build_auto("Auto", boosting_type = "rf", bagging_freq = 5, bagging_fraction = 0.75)
build_auto("AutoNA")
build_auto("AutoNA", num_iteration = 17)

def build_auto_direct(name):
	df = load_csv(name + ".csv")
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	lgbm = LGBMRegressor(n_estimators = 31)
	lgbm.fit(X.values, y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"], categorical_feature =  ["cylinders", "model_year", "origin"])

	name = re.sub("Auto", "AutoDirect", name);

	store_lgbm(lgbm, "Regression" + name + ".txt")

	mpg = DataFrame(lgbm.predict(X.values), columns = ["_target"])
	store_csv(mpg, "Regression" + name + ".csv")

build_auto_direct("Auto")
build_auto_direct("AutoNA")

def build_housing(name, objective = "regression", num_iteration = 0):
	df = load_csv(name + ".csv", ["CHAS"])
	X = df[df.columns.difference(["MEDV"])]
	y = df["MEDV"]

	lgbm = LGBMRegressor(objective = objective, n_estimators = 51)
	lgbm.fit(X, y)

	if num_iteration == 0:
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	medv = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(medv, "Regression" + name + ".csv")

build_housing("Housing", objective = "mean_squared_error")
build_housing("Housing", objective = "mean_squared_error", num_iteration = 31)
build_housing("HousingNA", objective = "quantile")
build_housing("HousingNA", objective = "quantile", num_iteration = 31)

#
# Poisson regression
#

def build_visit(name, objective = "poisson", num_iteration = 0):
	df = load_csv(name + ".csv", ["outwork", "female", "married", "kids", "self"])
	X = df[["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"]]
	y = df["docvis"]

	lgbm = LGBMRegressor(objective = objective, n_estimators = 71)
	lgbm.fit(X, y, feature_name = ["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"])

	if num_iteration == 0:
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	docvis = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(docvis, "Regression" + name + ".csv")

build_visit("Visit")
build_visit("Visit", num_iteration = 31)
build_visit("VisitNA", objective = "tweedie")
build_visit("VisitNA", objective = "tweedie", num_iteration = 31)
