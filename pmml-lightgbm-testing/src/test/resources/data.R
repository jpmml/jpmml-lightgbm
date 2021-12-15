insert_missing = function(in_name, out_name, columns, na_rep = "N/A"){
	mod = function(x){
		return (x[sample(c(TRUE, NA), prob = c(0.80, 0.20), size = length(x), replace = TRUE)])
	}

	set.seed(42)

	df = read.table(paste("csv", in_name, sep = "/"), header = TRUE, sep = ",")
	for(column in columns){
		df[column] = lapply(df[column], mod)
	}
	write.table(df, paste("csv", out_name, sep = "/"), row.names = FALSE, col.names = TRUE, sep = ",", na = na_rep, quote = FALSE)
}

insert_missing("Audit.csv", "AuditNA.csv", c("Age", "Employment", "Education", "Marital", "Occupation", "Income", "Gender", "Deductions", "Hours"))
insert_missing("Audit.csv", "AuditInvalid.csv", c("Age", "Employment", "Marital", "Income", "Hours"), na_rep = "-999")
insert_missing("Auto.csv", "AutoNA.csv", c("displacement", "horsepower", "weight", "acceleration", "model_year", "origin"))
insert_missing("Housing.csv", "HousingNA.csv", c("CRIM", "ZN", "INDUS", "CHAS", "NOX", "RM", "AGE", "DIS", "RAD", "TAX", "PTRATIO", "B", "LSTAT"))
insert_missing("Iris.csv", "IrisNA.csv", c("Sepal.Length", "Sepal.Width", "Petal.Length", "Petal.Width"))
insert_missing("Visit.csv", "VisitNA.csv", c("age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"))
