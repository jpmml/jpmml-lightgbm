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
import java.util.function.Predicate;

import com.google.common.collect.Iterables;

public class Section extends LinkedHashMap<String, String> {

	public Section(){
	}

	public Section(Section section){
		super(section);
	}

	public boolean checkId(String id){
		return checkId(id::equals);
	}

	public boolean checkId(Predicate<String> predicate){
		return predicate.test(id());
	}

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

	public long parseUnsignedInt(String key){
		return Long.parseLong(get(key));
	}

	public long[] getUnsignedIntArray(String key, int length){
		return LightGBMUtil.parseUnsignedIntArray(get(key), length);
	}

	public double getDouble(String key){
		return Double.parseDouble(get(key));
	}

	public double[] getDoubleArray(String key, int length){
		return LightGBMUtil.parseDoubleArray(get(key), length);
	}

	public String getString(String key){
		String result = get(key, false);

		result = LightGBMUtil.unescape(result);

		return result;
	}

	public String[] getStringArray(String key, int length){
		String[] result = LightGBMUtil.parseStringArray(get(key), length);

		for(int i = 0; i < result.length; i++){
			result[i] = LightGBMUtil.unescape(result[i]);
		}

		return result;
	}

	public String get(String key){
		return get(key, true);
	}

	public String get(String key, boolean required){

		if(required && !super.containsKey(key)){
			throw new IllegalArgumentException(key);
		}

		return super.get(key);
	}

	public String put(String string){

		if(string.startsWith("[") && string.endsWith("]")){
			return put(string.substring("[".length(), string.length() - "]".length()), ':');
		}

		return put(string, '=');
	}

	public String put(String string, char separator){
		String key;
		String value;

		int index = string.indexOf(separator);
		if(index > 0){
			key = string.substring(0, index);
			value = string.substring(index + 1);

			value = value.trim();

			if(value.length() == 0){
				value = null;
			}
		} else

		{
			key = string;
			value = null;
		}

		return super.put(key, value);
	}
}