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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.jpmml.converter.ConstantFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Schema;
import org.jpmml.converter.UnsupportedFeatureException;
import org.jpmml.converter.WildcardFeature;
import org.jpmml.evaluator.ResultField;
import org.jpmml.lightgbm.GBDT;
import org.jpmml.lightgbm.LightGBMEncoder;
import org.jpmml.lightgbm.LightGBMException;
import org.jpmml.lightgbm.LightGBMUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class WildcardFeatureTest extends SchemaBatchTest {

	@Override
	public SchemaBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		SchemaBatch result = new SchemaBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public WildcardFeatureTest getArchiveBatchTest(){
				return WildcardFeatureTest.this;
			}

			@Override
			public List<Feature> createFeatures(GBDT gbdt, LightGBMEncoder encoder){
				List<Feature> result = new ArrayList<>();

				String[] featureNames = gbdt.getFeatureNames();
				String[] featureInfos = gbdt.getFeatureInfos();

				for(int i = 0; i < featureNames.length; i++){
					String featureName = featureNames[i];
					String featureInfo = featureInfos[i];

					if(LightGBMUtil.isNone(featureInfo)){
						result.add(new ConstantFeature(encoder, 0d));
					} else

					{
						DataField dataField = encoder.createDataField(featureName, OpType.CONTINUOUS, DataType.DOUBLE);

						result.add(new WildcardFeature(encoder, dataField));
					}
				}

				return result;
			}

			@Override
			public String getInputCsvPath(){
				String path = super.getInputCsvPath();

				path = path.replace("ConstantCat", "");
				path = path.replace("OrderedCat", "");

				return path;
			}
		};

		return result;
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate(CLASSIFICATION, AUDIT);
	}

	@Test
	public void evaluateAuditConstantCat() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_CONSTANT_CAT);
	}

	@Test
	public void evaluateAuditOrderedCat() throws Exception {
		assertThrows(LightGBMException.class, () -> evaluate(CLASSIFICATION, AUDIT_ORDERED_CAT));
	}

	@Test
	public void evaluateAuditContinuous() throws Exception {
		GBDT gbdt;

		try(SchemaBatch batch = createBatch(CLASSIFICATION, AUDIT, excludeFields(), getEquivalence())){
			gbdt = batch.loadGBDT();
		}

		String[] featureNames = gbdt.getFeatureNames();

		LightGBMEncoder encoder = new LightGBMEncoder();

		List<Feature> features = new ArrayList<>();

		for(String featureName : featureNames){
			DataField dataField = encoder.createDataField(featureName, OpType.CONTINUOUS, DataType.DOUBLE);

			features.add(new ContinuousFeature(encoder, dataField));
		}

		Schema schema = new Schema(encoder, null, features);

		assertThrows(UnsupportedFeatureException.class, () -> gbdt.toLightGBMSchema(schema));
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate(REGRESSION, AUTO_NA);
	}
}
