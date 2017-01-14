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

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.Iterables;

public class Section extends LinkedHashMap<String, String> {

	public String id(){
		Map.Entry<String, String> entry = Iterables.getFirst(entrySet(), null);

		if(entry == null){
			throw new IllegalStateException();
		}

		String key = entry.getKey();
		String value = entry.getValue();

		return (value != null ? (key + "=" + value) : key);
	}

	public int getInt(String key){
		return Integer.parseInt(get(key));
	}

	public int[] getIntArray(String key, int length){
		return LightGBMUtil.parseIntArray(get(key), length);
	}

	public double getDouble(String key){
		return Double.parseDouble(get(key));
	}

	public double[] getDoubleArray(String key, int length){
		return LightGBMUtil.parseDoubleArray(get(key), length);
	}

	public String getString(String key){
		return get(key);
	}

	public String[] getStringArray(String key, int length){
		return LightGBMUtil.parseStringArray(get(key), length);
	}
}