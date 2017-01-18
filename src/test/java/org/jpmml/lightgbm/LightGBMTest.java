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

import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.IntegrationTest;

public class LightGBMTest extends IntegrationTest {

	@Override
	protected ArchiveBatch createBatch(String name, String dataset){
		ArchiveBatch result = new ArchiveBatch(name, dataset){

			@Override
			public InputStream open(String path){
				Class<? extends LightGBMTest> clazz = LightGBMTest.this.getClass();

				return clazz.getResourceAsStream(path);
			}

			@Override
			public PMML getPMML() throws Exception {
				GBDT gbdt;

				try(InputStream is = open("/lgbm/" + getName() + getDataset() + ".txt")){
					gbdt = LightGBMUtil.loadGBDT(is);
				}

				PMML pmml = gbdt.encodePMML(null, null);

				return pmml;
			}
		};

		return result;
	}
}