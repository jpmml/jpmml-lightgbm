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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.RealNumberEquivalence;
import org.jpmml.lightgbm.HasLightGBMOptions;
import org.jpmml.lightgbm.ObjectiveFunction;
import org.jpmml.lightgbm.Regression;
import org.jpmml.lightgbm.Section;
import org.jpmml.model.visitors.AbstractVisitor;
import org.junit.jupiter.api.Test;

public class RegressionTest extends ValidatingLightGBMEncoderBatchTest implements LightGBMAlgorithms, LightGBMDatasets {

	@Override
	public ValidatingLightGBMEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		ValidatingLightGBMEncoderBatch result = new ValidatingLightGBMEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public RegressionTest getArchiveBatchTest(){
				return RegressionTest.this;
			}

			@Override
			public List<Map<String, Object>> getOptionsMatrix(){
				List<Map<String, Object>> optionsMatrix = super.getOptionsMatrix();

				String algorithm = getAlgorithm();

				if((LINEARTREE_REGRESSION).equals(algorithm)){
					Map<String, Object> options = new LinkedHashMap<>();
					options.put(HasLightGBMOptions.OPTION_NAN_AS_MISSING, true);

					return Collections.singletonList(options);
				}

				return optionsMatrix;
			}

			@Override
			public ObjectiveFunction getObjectiveFunction(){
				ObjectiveFunction objectiveFunction = super.getObjectiveFunction();

				String algorithm = getAlgorithm();
				String dataset = getDataset();

				if((REGRESSION).equals(algorithm)){

					if((AUTO).equals(dataset) || (AUTO_LIMIT).equals(dataset)){
						Section config = new Section();
						config.put(ObjectiveFunction.CONFIG_NAME, "custom_regression");

						return new Regression(config);
					}
				}

				return objectiveFunction;
			}

			@Override
			public PMML getPMML() throws Exception {
				PMML pmml = super.getPMML();

				String dataset = truncate(getDataset());

				// XXX
				if((HOUSING).equals(dataset) || (HOUSING_NA).equals(dataset)){
					Visitor visitor = new AbstractVisitor(){

						@Override
						public VisitorAction visit(MiningField miningField){
							miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

							return super.visit(miningField);
						}
					};

					visitor.applyTo(pmml);
				}

				return pmml;
			}
		};

		return result;
	}

	@Test
	public void evaluateAuto() throws Exception {
		evaluate(REGRESSION, AUTO);
	}

	@Test
	public void evaluateLinearTreeAuto() throws Exception {
		evaluate(LINEARTREE_REGRESSION, AUTO, new RealNumberEquivalence(4));
	}

	@Test
	public void evaluateRFAuto() throws Exception {
		evaluate(RF_REGRESSION, AUTO);
	}

	@Test
	public void evaluateAutoLimit() throws Exception {
		evaluate(REGRESSION, AUTO_LIMIT);
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate(REGRESSION, AUTO_NA);
	}

	@Test
	public void evaluateLinearTreeAutoNA() throws Exception {
		evaluate(LINEARTREE_REGRESSION, AUTO_NA, new RealNumberEquivalence(4));
	}

	@Test
	public void evaluateAutoNALimit() throws Exception {
		evaluate(REGRESSION, AUTO_NA_LIMIT);
	}

	@Test
	public void evaluateHousing() throws Exception {
		evaluate(REGRESSION, HOUSING);
	}

	@Test
	public void evaluateHousingLimit() throws Exception {
		evaluate(REGRESSION, HOUSING_LIMIT);
	}

	@Test
	public void evaluateHousingNA() throws Exception {
		evaluate(REGRESSION, HOUSING_NA);
	}

	@Test
	public void evaluateHousingNALimit() throws Exception {
		evaluate(REGRESSION, HOUSING_NA_LIMIT);
	}

	@Test
	public void evaluateVisit() throws Exception {
		evaluate(REGRESSION, VISIT);
	}

	@Test
	public void evaluateVisitLimit() throws Exception {
		evaluate(REGRESSION, VISIT_LIMIT);
	}

	@Test
	public void evaluateVisitNA() throws Exception {
		evaluate(REGRESSION, VISIT_NA);
	}

	@Test
	public void evaluateVisitNALimit() throws Exception {
		evaluate(REGRESSION, VISIT_NA_LIMIT);
	}
}