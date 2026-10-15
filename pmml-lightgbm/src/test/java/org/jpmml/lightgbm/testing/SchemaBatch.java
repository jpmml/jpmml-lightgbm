/*
 * Copyright (c) 2026 Villu Ruusmann
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
package org.jpmml.lightgbm.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.Schema;
import org.jpmml.converter.testing.ModelEncoderBatch;
import org.jpmml.evaluator.ResultField;
import org.jpmml.lightgbm.GBDT;
import org.jpmml.lightgbm.LightGBMEncoder;
import org.jpmml.lightgbm.LightGBMUtil;
import org.jpmml.lightgbm.ObjectiveFunction;

abstract
public class SchemaBatch extends ModelEncoderBatch {

	public SchemaBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);
	}

	@Override
	abstract
	public SchemaBatchTest getArchiveBatchTest();

	abstract
	public List<Feature> createFeatures(GBDT gbdt, LightGBMEncoder encoder);

	public String getModelTxtPath(){
		return "/lgbm/" + (getAlgorithm() + getDataset()) + ".txt";
	}

	public GBDT loadGBDT() throws IOException {

		try(InputStream is = open(getModelTxtPath())){
			return LightGBMUtil.loadGBDT(is);
		}
	}

	@Override
	public PMML getPMML() throws Exception {
		GBDT gbdt = loadGBDT();

		ObjectiveFunction objectiveFunction = gbdt.requireObjectiveFunction();

		LightGBMEncoder encoder = new LightGBMEncoder();

		Label label = objectiveFunction.encodeLabel("_target", null, encoder);

		List<Feature> features = createFeatures(gbdt, encoder);

		Schema schema = new Schema(encoder, label, features);

		Schema lgbmSchema = gbdt.toLightGBMSchema(schema);

		Model model = gbdt.encodeModel(Collections.emptyMap(), lgbmSchema);

		return encoder.encodePMML(model);
	}
}
