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

import java.io.InputStream;

import com.google.common.base.Predicate;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.IntegrationTestBatch;
import org.jpmml.evaluator.PMMLEquivalence;

public class LightGBMTest extends IntegrationTest {

	public LightGBMTest(){
		super(new PMMLEquivalence(1e-9, 1e-9));
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<FieldName> predicate){
		ArchiveBatch result = new IntegrationTestBatch(name, dataset, predicate){

			@Override
			public IntegrationTest getIntegrationTest(){
				return LightGBMTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				GBDT gbdt;

				try(InputStream is = open("/lgbm/" + getName() + getDataset() + ".txt")){
					gbdt = LightGBMUtil.loadGBDT(is);
				}

				PMML pmml = gbdt.encodePMML(null, null);

				ensureValidity(pmml);

				return pmml;
			}
		};

		return result;
	}
}