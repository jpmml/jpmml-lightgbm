/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.testing.ModelEncoderBatch;
import org.jpmml.converter.testing.ModelEncoderBatchTest;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.RealNumberEquivalence;
import org.jpmml.lightgbm.GBDT;
import org.jpmml.lightgbm.LightGBMEncoder;
import org.jpmml.lightgbm.LightGBMUtil;
import org.junit.Test;

public class PandasDummiesTest extends ModelEncoderBatchTest implements LightGBMAlgorithms, LightGBMDatasets {

	public PandasDummiesTest(){
		super(new RealNumberEquivalence(2));
	}

	@Override
	public ModelEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		ModelEncoderBatch result = new ModelEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public PandasDummiesTest getArchiveBatchTest(){
				return PandasDummiesTest.this;
			}

			public String getModelTxtPath(){
				return "/lgbm/" + (getAlgorithm() + getDataset()) + ".txt";
			}

			@Override
			public PMML getPMML() throws Exception {
				GBDT gbdt;

				try(InputStream is = open(getModelTxtPath())){
					gbdt = LightGBMUtil.loadGBDT(is);
				}

				LightGBMEncoder encoder = new LightGBMEncoder();

				Label label;

				{
					// XXX
					DataField dataField = encoder.createDataField("_target", OpType.CATEGORICAL, DataType.STRING, Arrays.asList("0", "1"));

					label = new CategoricalLabel(dataField);
				}

				List<Feature> features = new ArrayList<>();

				String[] featureNames = gbdt.getFeatureNames();
				for(String featureName : featureNames){
					int index = featureName.indexOf('_');

					if(index < 0){
						DataField dataField = encoder.createDataField(featureName, OpType.CONTINUOUS, DataType.DOUBLE);

						features.add(new ContinuousFeature(encoder, dataField));
					} else

					{
						String name = featureName.substring(0, index);
						String value = featureName.substring(index + 1);

						DataField dataField = encoder.getDataField(name);
						if(dataField == null){
							dataField = encoder.createDataField(name, OpType.CATEGORICAL, DataType.STRING);
						}

						PMMLUtil.addValues(dataField, Collections.singletonList(value));

						features.add(new BinaryFeature(encoder, dataField, value));
					}
				}

				Schema schema = new Schema(encoder, label, features);

				Schema lgbmSchema = gbdt.toLightGBMSchema(schema);

				MiningModel miningModel = gbdt.encodeMiningModel(Collections.emptyMap(), lgbmSchema);

				return encoder.encodePMML(miningModel);
			}

			@Override
			public String getInputCsvPath(){
				String path = super.getInputCsvPath();

				path = path.replace("Bin", "");

				return path;
			}
		};

		return result;
	}

	@Test
	public void evaluateAuditBin() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_BIN);
	}

	@Test
	public void evaluateAuditBinNA() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_BIN_NA);
	}
}