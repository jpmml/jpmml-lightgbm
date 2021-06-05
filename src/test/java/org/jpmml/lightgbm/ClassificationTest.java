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
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.ArchiveBatch;
import org.jpmml.evaluator.testing.RealNumberEquivalence;
import org.junit.Test;

public class ClassificationTest extends LightGBMTest {

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		ArchiveBatch result = new LightGBMTestBatch(name, dataset, predicate, equivalence){

			@Override
			public ClassificationTest getIntegrationTest(){
				return ClassificationTest.this;
			}

			@Override
			public String getModelTxtPath(){
				String path = super.getModelTxtPath();

				path = path.replace("Invalid", "");

				return path;
			}

			@Override
			public List<Map<FieldName, String>> getInput() throws IOException {
				String[] dataset = parseDataset();

				List<Map<FieldName, String>> table = super.getInput();

				if(("AuditNA").equals(dataset[0])){
					FieldName income = FieldName.create("Income");

					for(Map<FieldName, String> row : table){
						String value = row.get(income);

						if(value == null){
							row.put(income, "NaN");
						}
					}
				}

				return table;
			}
		};

		return result;
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate("Classification", "Audit", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateRFAudit() throws Exception {
		evaluate("RFClassification", "Audit", new RealNumberEquivalence(4));
	}

	@Test
	public void evaluateAuditLimit() throws Exception {
		evaluate("Classification", "Audit@17", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateAuditInvalid() throws Exception {
		evaluate("Classification", "AuditInvalid", new RealNumberEquivalence(4));
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate("Classification", "AuditNA", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateAuditNALimit() throws Exception {
		evaluate("Classification", "AuditNA@17");
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("Classification", "Iris");
	}

	@Test
	public void evaluateRFIris() throws Exception {
		evaluate("RFClassification", "Iris");
	}

	@Test
	public void evaluateIrisLimit() throws Exception {
		evaluate("Classification", "Iris@7", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateIrisNA() throws Exception {
		evaluate("Classification", "IrisNA");
	}

	@Test
	public void evaluateIrisNALimit() throws Exception {
		evaluate("Classification", "IrisNA@7");
	}

	@Test
	public void evaluateVersicolor() throws Exception {
		evaluate("Classification", "Versicolor");
	}

	@Test
	public void evaluateVersicolorLimit() throws Exception {
		evaluate("Classification", "Versicolor@9");
	}
}