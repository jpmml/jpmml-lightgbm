/*
 * Copyright (c) 2022 Villu Ruusmann
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
import org.jpmml.evaluator.ResultField;

public class ValidatingLightGBMEncoderBatchTest extends LightGBMEncoderBatchTest {

	@Override
	public ValidatingLightGBMEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		ValidatingLightGBMEncoderBatch result = new ValidatingLightGBMEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public ValidatingLightGBMEncoderBatchTest getArchiveBatchTest(){
				return ValidatingLightGBMEncoderBatchTest.this;
			}
		};

		return result;
	}
}