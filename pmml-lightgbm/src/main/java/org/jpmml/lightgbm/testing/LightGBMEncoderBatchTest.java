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

import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.converter.testing.ModelEncoderBatchTest;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.RealNumberEquivalence;

public class LightGBMEncoderBatchTest extends ModelEncoderBatchTest {

	public LightGBMEncoderBatchTest(){
		super(new RealNumberEquivalence(1));
	}

	@Override
	public LightGBMEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		LightGBMEncoderBatch result = new LightGBMEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public LightGBMEncoderBatchTest getArchiveBatchTest(){
				return LightGBMEncoderBatchTest.this;
			}
		};

		return result;
	}
}