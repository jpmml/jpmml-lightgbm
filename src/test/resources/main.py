from lightgbm import LGBMClassifier, LGBMRegressor
from pandas import DataFrame

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

def build_iris(name, objective = "multiclass", num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	lgbm = LGBMClassifier(objective = objective, n_estimators = 11)
	lgbm.fit(X, y)

	if(num_iteration == 0):
		store_lgbm(lgbm, "Classification" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	species = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"]).replace("setosa", "0").replace("versicolor", "1").replace("virginica", "2")
	species_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)", "probability(2)"])
	store_csv(pandas.concat((species, species_proba), axis = 1), "Classification" + name + ".csv")

build_iris("Iris")
build_iris("Iris", num_iteration = 7)
build_iris("IrisNA")
build_iris("IrisNA", num_iteration = 7)

#
# Binary classification
#

def build_audit(name, objective = "binary", num_iteration = 0):
	df = load_csv(name + ".csv", ["Employment", "Education", "Marital", "Occupation", "Gender"])
	X = df[["Age", "Employment", "Education", "Marital", "Occupation", "Income", "Gender", "Hours"]]
	y = df["Adjusted"]

	lgbm = LGBMClassifier(objective = objective, n_estimators = 31)
	lgbm.fit(X, y)

	if(num_iteration == 0):
		store_lgbm(lgbm, "Classification" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	adjusted = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	adjusted_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((adjusted, adjusted_proba), axis = 1), "Classification" + name + ".csv")

build_audit("Audit")
build_audit("Audit", num_iteration = 17)
build_audit("AuditNA")
build_audit("AuditNA", num_iteration = 17)

def build_versicolor(name, objective = "binary", num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[["Sepal.Length", "Sepal.Width", "Dummy", "Petal.Length", "Petal.Width"]]
	y = df["Species"]

	lgbm = LGBMClassifier(objective = objective, n_estimators = 11)
	lgbm.fit(X, y)

	if(num_iteration == 0):
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

def build_auto(name, objective = "regression", num_iteration = 0):
	df = load_csv(name + ".csv", ["cylinders", "model_year", "origin"])
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	lgbm = LGBMRegressor(objective = objective, n_estimators = 31)
	lgbm.fit(X, y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"])

	if(num_iteration == 0):
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	mpg = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(mpg, "Regression" + name + ".csv")

build_auto("Auto")
build_auto("Auto", num_iteration = 17)
build_auto("AutoNA")
build_auto("AutoNA", num_iteration = 17)

def build_auto_direct(name):
	df = load_csv(name + ".csv")
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	lgbm = LGBMRegressor(n_estimators = 31)
	lgbm.fit(X.as_matrix(), y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"], categorical_feature =  ["cylinders", "model_year", "origin"])

	name = re.sub("Auto", "AutoDirect", name);

	store_lgbm(lgbm, "Regression" + name + ".txt")

	mpg = DataFrame(lgbm.predict(X.as_matrix()), columns = ["_target"])
	store_csv(mpg, "Regression" + name + ".csv")

build_auto_direct("Auto")
build_auto_direct("AutoNA")

def build_housing(name, objective = "regression", num_iteration = 0):
	df = load_csv(name + ".csv", ["CHAS"])
	X = df[df.columns.difference(["MEDV"])]
	y = df["MEDV"]

	lgbm = LGBMRegressor(objective = objective, n_estimators = 51)
	lgbm.fit(X, y)

	if(num_iteration == 0):
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	medv = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(medv, "Regression" + name + ".csv")

build_housing("Housing", objective = "mean_squared_error")
build_housing("Housing", objective = "mean_squared_error", num_iteration = 31)
build_housing("HousingNA", objective = "mean_absolute_error")
build_housing("HousingNA", objective = "mean_absolute_error", num_iteration = 31)

#
# Poisson regression
#

def build_visit(name, objective = "poisson", num_iteration = 0):
	df = load_csv(name + ".csv", ["outwork", "female", "married", "kids", "self"])
	X = df[["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"]]
	y = df["docvis"]

	lgbm = LGBMRegressor(objective = objective, n_estimators = 71)
	lgbm.fit(X, y, feature_name = ["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"])

	if(num_iteration == 0):
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	docvis = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(docvis, "Regression" + name + ".csv")

build_visit("Visit")
build_visit("Visit", num_iteration = 31)
build_visit("VisitNA", objective = "tweedie")
build_visit("VisitNA", objective = "tweedie", num_iteration = 31)
