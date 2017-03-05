import pandas
import random

def insert_missing(in_name, out_name, columns):
	df = pandas.read_csv("csv/" + in_name)
	for column in columns:
		for row in random.sample(range(df.shape[0]), int(round(0.2 * df.shape[0]))):
			df.ix[row, column] = None
	df.to_csv("csv/" + out_name, index = False, na_rep = "N/A")

random.seed(42)

insert_missing("Auto.csv", "AutoNA.csv", ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"])
insert_missing("Housing.csv", "HousingNA.csv", ["CRIM", "ZN", "INDUS", "CHAS", "NOX", "RM", "AGE", "DIS", "RAD", "TAX", "PTRATIO", "B", "LSTAT"])
insert_missing("Iris.csv", "IrisNA.csv", ["Sepal.Length", "Sepal.Width", "Petal.Length", "Petal.Width"])
insert_missing("Visit.csv", "VisitNA.csv", ["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"])
