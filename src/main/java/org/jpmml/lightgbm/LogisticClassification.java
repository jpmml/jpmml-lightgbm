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

import com.google.common.collect.Iterables;
import org.dmg.pmml.Apply;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
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

		Label label = new CategoricalLabel(dataField);

		return label;
	}

	@Override
	public MiningModel encodeMiningModel(List<TreeModel> treeModels, Schema schema){
		Schema segmentSchema = schema.toAnonymousSchema();

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(segmentSchema))
			.setSegmentation(createSegmentation(treeModels))
			.setOutput(createOutput(this.sigmoid_));

		return fixClassifer(MiningModelUtil.createBinaryLogisticClassification(schema, miningModel, -1d, true));
	}

	static
	private MiningModel fixClassifer(MiningModel miningModel){
		Segmentation segmentation = miningModel.getSegmentation();

		Segment lastSegment = Iterables.getLast(segmentation.getSegments());

		RegressionModel regressionModel = (RegressionModel)lastSegment.getModel();
		regressionModel.setNormalizationMethod(null);

		RegressionTable regressionTable = Iterables.getFirst(regressionModel.getRegressionTables(), null);
		regressionTable.setIntercept(1d);

		return miningModel;
	}

	static
	private Output createOutput(double sigmoid){
		Output output = new Output();

		OutputField lgbmValue = new OutputField(FieldName.create("lgbmValue"), DataType.DOUBLE)
			.setOpType(OpType.CONTINUOUS)
			.setResultFeature(ResultFeature.PREDICTED_VALUE)
			.setFinalResult(false);

		// "1 / (1 + exp(-2 * sigmoid * y))"
		Apply apply = PMMLUtil.createApply("/", PMMLUtil.createConstant(1d), PMMLUtil.createApply("+", PMMLUtil.createConstant(1d), PMMLUtil.createApply("exp", PMMLUtil.createApply("*", PMMLUtil.createConstant(-2d * sigmoid), new FieldRef(lgbmValue.getName())))));

		OutputField transformedLgbmValue = new OutputField(FieldName.create("transformedLgbmValue"), DataType.DOUBLE)
			.setOpType(OpType.CONTINUOUS)
			.setResultFeature(ResultFeature.TRANSFORMED_VALUE)
			.setFinalResult(false)
			.setExpression(apply);

		output.addOutputFields(lgbmValue, transformedLgbmValue);

		return output;
	}
}