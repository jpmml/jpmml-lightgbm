/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jpmml.converter.HasNativeConfiguration;
import org.jpmml.converter.HasOptions;

public interface HasLightGBMOptions extends HasOptions, HasNativeConfiguration {

	String OPTION_COMPACT = "compact";

	String OPTION_NAN_AS_MISSING = "nan_as_missing";

	String OPTION_NUM_ITERATION = "num_iteration";

	@Override
	default
	public Map<String, ?> getNativeConfiguration(){
		Map<String, Object> result = new LinkedHashMap<>();
		result.put(HasLightGBMOptions.OPTION_COMPACT, Boolean.FALSE);

		return result;
	}
}