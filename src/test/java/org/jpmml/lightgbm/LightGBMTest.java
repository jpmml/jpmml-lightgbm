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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.ArchiveBatch;
import org.jpmml.evaluator.testing.IntegrationTest;
import org.jpmml.evaluator.testing.IntegrationTestBatch;
import org.jpmml.evaluator.testing.RealNumberEquivalence;
import org.jpmml.model.visitors.AbstractVisitor;

public class LightGBMTest extends IntegrationTest {

	public LightGBMTest(){
		super(new RealNumberEquivalence(1));
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		ArchiveBatch result = new IntegrationTestBatch(name, dataset, predicate, equivalence){

			@Override
			public IntegrationTest getIntegrationTest(){
				return LightGBMTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				GBDT gbdt;

				String[] dataset = parseDataset();

				dataset[0] = dataset[0].replace("Invalid", "");

				try(InputStream is = open("/lgbm/" + getName() + dataset[0] + ".txt")){
					gbdt = LightGBMUtil.loadGBDT(is);
				}

				Integer numIteration = null;
				if(dataset.length > 1){
					numIteration = new Integer(dataset[1]);
				}

				Map<String, Object> options = new LinkedHashMap<>();
				options.put(HasLightGBMOptions.OPTION_COMPACT, numIteration != null);
				options.put(HasLightGBMOptions.OPTION_NUM_ITERATION, numIteration);

				PMML pmml = gbdt.encodePMML(null, null, options);

				// XXX
				if(("Housing").equals(dataset[0]) || ("HousingNA").equals(dataset[0])){
					Visitor visitor = new AbstractVisitor(){

						@Override
						public VisitorAction visit(MiningField miningField){
							miningField.setInvalidValueTreatment(InvalidValueTreatmentMethod.AS_IS);

							return super.visit(miningField);
						}
					};

					visitor.applyTo(pmml);
				}

				validatePMML(pmml);

				return pmml;
			}

			@Override
			public List<Map<FieldName, String>> getInput() throws IOException {
				String[] dataset = parseDataset();

				dataset[0] = dataset[0].replace("Direct", "");

				return loadRecords("/csv/" + dataset[0] + ".csv");
			}

			@Override
			public List<Map<FieldName, String>> getOutput() throws IOException {
				return loadRecords("/csv/" + getName() + getDataset() + ".csv");
			}

			private String[] parseDataset(){
				String dataset = getDataset();

				int index = dataset.indexOf('@');
				if(index > -1){
					return new String[]{dataset.substring(0, index), dataset.substring(index + 1)};
				}

				return new String[]{dataset};
			}
		};

		return result;
	}
}