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

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeature;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class SoftMaxClassification extends ObjectiveFunction {

	private int num_class_;


	public SoftMaxClassification(int num_class){
		this.num_class_ = num_class;

		if(num_class < 3){
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Label encodeLabel(FieldName name, PMMLEncoder encoder){
		List<String> categories = createCategories(this.num_class_);

		DataField dataField = encoder.createDataField(name, OpType.CATEGORICAL, DataType.STRING, categories);

		Label label = new CategoricalLabel(dataField);

		return label;
	}

	@Override
	public MiningModel encodeMiningModel(List<TreeModel> treeModels, Schema schema){
		Schema segmentSchema = schema.toAnonymousSchema();

		List<MiningModel> miningModels = new ArrayList<>();

		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

		for(int i = 0, rows = categoricalLabel.size(), columns = (treeModels.size() / rows); i < rows; i++){
			MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(segmentSchema))
				.setSegmentation(createSegmentation(getRow(treeModels, i, rows, columns)))
				.setOutput(createOutput(categoricalLabel.getValue(i)));

			miningModels.add(miningModel);
		}

		return MiningModelUtil.createClassification(schema, miningModels, RegressionModel.NormalizationMethod.SOFTMAX, true);
	}

	static
	public Output createOutput(String targetCategory){
		Output output = new Output();

		OutputField lgbmValue = new OutputField(FieldName.create("lgbmValue_" + targetCategory), DataType.DOUBLE)
			.setOpType(OpType.CONTINUOUS)
			.setResultFeature(ResultFeature.PREDICTED_VALUE)
			.setFinalResult(false);

		output.addOutputFields(lgbmValue);

		return output;
	}

	static
	private <E> List<E> getRow(List<E> values, int index, int rows, int columns){

		if((rows * columns) != values.size()){
			throw new IllegalArgumentException();
		}

		List<E> result = new ArrayList<>();

		for(int i = 0; i < columns; i++){
			E value = values.get((i * rows) + index);

			result.add(value);
		}

		return result;
	}

	static
	private List<String> createCategories(int size){
		List<String> result = new ArrayList<>();

		for(int i = 0; i < size; i++){
			result.add(String.valueOf(i));
		}

		return result;
	}
}