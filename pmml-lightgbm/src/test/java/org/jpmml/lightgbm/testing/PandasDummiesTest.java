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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldUtil;
import org.jpmml.evaluator.ResultField;
import org.jpmml.lightgbm.GBDT;
import org.jpmml.lightgbm.LightGBMEncoder;
import org.junit.jupiter.api.Test;

public class PandasDummiesTest extends SchemaBatchTest {

	@Override
	public SchemaBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		SchemaBatch result = new SchemaBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public PandasDummiesTest getArchiveBatchTest(){
				return PandasDummiesTest.this;
			}

			@Override
			public List<Feature> createFeatures(GBDT gbdt, LightGBMEncoder encoder){
				List<Feature> result = new ArrayList<>();

				String[] featureNames = gbdt.getFeatureNames();
				for(String featureName : featureNames){
					int index = featureName.indexOf('_');

					if(index < 0){
						DataField dataField = encoder.createDataField(featureName, OpType.CONTINUOUS, DataType.DOUBLE);

						result.add(new ContinuousFeature(encoder, dataField));
					} else

					{
						String name = featureName.substring(0, index);
						String value = featureName.substring(index + 1);

						DataField dataField = encoder.getDataField(name);
						if(dataField == null){
							dataField = encoder.createDataField(name, OpType.CATEGORICAL, DataType.STRING);
						}

						FieldUtil.addValue(dataField, value);

						result.add(new BinaryFeature(encoder, dataField, value));
					}
				}

				return result;
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
