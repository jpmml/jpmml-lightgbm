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

import java.util.Arrays;
import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SigmoidTransformation;
import org.jpmml.converter.mining.MiningModelUtil;

public class LogisticClassification extends ObjectiveFunction {

	private int num_class_;

	private double sigmoid_;


	public LogisticClassification(int num_class, double sigmoid){
		this.num_class_ = num_class;
		this.sigmoid_ = sigmoid;

		if(num_class != 1){
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Label encodeLabel(FieldName name, PMMLEncoder encoder){
		List<String> categories = Arrays.asList("0", "1");

		DataField dataField = encoder.createDataField(name, OpType.CATEGORICAL, DataType.STRING, categories);

		return new CategoricalLabel(dataField);
	}

	@Override
	public MiningModel encodeMiningModel(List<Tree> trees, Schema schema){
		Schema segmentSchema = new Schema(new ContinuousLabel(null, DataType.DOUBLE), schema.getFeatures());

		MiningModel miningModel = createMiningModel(trees, segmentSchema)
			.setOutput(ModelUtil.createPredictedOutput(FieldName.create("lgbmValue"), OpType.CONTINUOUS, DataType.DOUBLE, new SigmoidTransformation(-2d * LogisticClassification.this.sigmoid_)));

		return MiningModelUtil.createBinaryLogisticClassification(schema, miningModel, RegressionModel.NormalizationMethod.NONE, 0d, 1d, true);
	}
}