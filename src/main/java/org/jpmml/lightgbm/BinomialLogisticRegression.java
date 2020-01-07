/*
 * Copyright (c) 2017 Villu Ruusmann
 *
 * This file is part of JPMML-LightGBM
 *
 * JPMML-LightGBM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-LightGBM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-LightGBM.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.lightgbm;

import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class BinomialLogisticRegression extends Classification {

	private double sigmoid_;


	public BinomialLogisticRegression(boolean average_output, double sigmoid){
		super(average_output, 2);

		this.sigmoid_ = sigmoid;
	}

	@Override
	public MiningModel encodeMiningModel(List<Tree> trees, Integer numIteration, Schema schema){
		Schema segmentSchema = schema.toAnonymousRegressorSchema(DataType.DOUBLE);

		MiningModel miningModel = createMiningModel(trees, numIteration, segmentSchema)
			.setOutput(ModelUtil.createPredictedOutput(FieldName.create("lgbmValue"), OpType.CONTINUOUS, DataType.DOUBLE));

		return MiningModelUtil.createBinaryLogisticClassification(miningModel, BinomialLogisticRegression.this.sigmoid_, 0d, RegressionModel.NormalizationMethod.LOGIT, true, schema);
	}
}