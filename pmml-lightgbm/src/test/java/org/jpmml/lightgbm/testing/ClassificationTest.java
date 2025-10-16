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
package org.jpmml.lightgbm.testing;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.testing.RealNumberEquivalence;
import org.jpmml.lightgbm.HasLightGBMOptions;
import org.junit.jupiter.api.Test;

public class ClassificationTest extends ValidatingLightGBMEncoderBatchTest implements LightGBMAlgorithms, LightGBMDatasets {

	@Override
	public ValidatingLightGBMEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		ValidatingLightGBMEncoderBatch result = new ValidatingLightGBMEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public ClassificationTest getArchiveBatchTest(){
				return ClassificationTest.this;
			}

			@Override
			public List<Map<String, Object>> getOptionsMatrix(){
				List<Map<String, Object>> optionsMatrix = super.getOptionsMatrix();

				String algorithm = getAlgorithm();

				if((LINEARTREE_CLASSIFICATION).equals(algorithm)){
					Map<String, Object> options = new LinkedHashMap<>();
					options.put(HasLightGBMOptions.OPTION_NAN_AS_MISSING, true);

					return Collections.singletonList(options);
				}

				return optionsMatrix;
			}

			@Override
			public String getModelTxtPath(){
				String path = super.getModelTxtPath();

				path = path.replace("Invalid", "");

				return path;
			}

			@Override
			public Table getInput() throws IOException {
				Table table = super.getInput();

				String dataset = truncate(getDataset());

				if((AUDIT_NA).equals(dataset)){
					table.apply("Income", (value) -> {

						if(value == null){
							return "NaN";
						}

						return value;
					});
				}

				return table;
			}
		};

		return result;
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate(CLASSIFICATION, AUDIT, new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateLinearTreeAudit() throws Exception {
		evaluate(LINEARTREE_CLASSIFICATION, AUDIT, new RealNumberEquivalence(8 + 8));
	}

	@Test
	public void evaluateRFAudit() throws Exception {
		evaluate(RF_CLASSIFICATION, AUDIT, new RealNumberEquivalence(4));
	}

	@Test
	public void evaluateAuditLimit() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_LIMIT, new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateAuditInvalid() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_INVALID, new RealNumberEquivalence(4));
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_NA, new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateLinearTreeAuditNA() throws Exception {
		evaluate(LINEARTREE_CLASSIFICATION, AUDIT_NA, new RealNumberEquivalence(8 + 8));
	}

	@Test
	public void evaluateAuditNALimit() throws Exception {
		evaluate(CLASSIFICATION, AUDIT_NA_LIMIT);
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate(CLASSIFICATION, IRIS);
	}

	@Test
	public void evaluateRFIris() throws Exception {
		evaluate(RF_CLASSIFICATION, IRIS);
	}

	@Test
	public void evaluateIrisLimit() throws Exception {
		evaluate(CLASSIFICATION, IRIS_LIMIT, new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateIrisNA() throws Exception {
		evaluate(CLASSIFICATION, IRIS_NA);
	}

	@Test
	public void evaluateIrisNALimit() throws Exception {
		evaluate(CLASSIFICATION, IRIS_NA_LIMIT);
	}

	@Test
	public void evaluateVersicolor() throws Exception {
		evaluate(CLASSIFICATION, VERSICOLOR);
	}

	@Test
	public void evaluateVersicolorLimit() throws Exception {
		evaluate(CLASSIFICATION, VERSICOLOR_LIMIT);
	}
}