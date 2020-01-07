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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.Schema;

public class Regression extends ObjectiveFunction {

	public Regression(boolean average_output){
		super(average_output);
	}

	@Override
	public Label encodeLabel(FieldName targetField, List<?> targetCategories, PMMLEncoder encoder){

		if(targetCategories != null && targetCategories.size() > 0){
			throw new IllegalArgumentException("Regression requires zero target categories");
		}

		DataField dataField = encoder.createDataField(targetField, OpType.CONTINUOUS, DataType.DOUBLE);

		return new ContinuousLabel(dataField);
	}

	@Override
	public MiningModel encodeMiningModel(List<Tree> trees, Integer numIteration, Schema schema){
		MiningModel miningModel = createMiningModel(trees, numIteration, schema);

		return miningModel;
	}
}