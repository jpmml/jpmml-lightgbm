/*
 * Copyright (c) 2023 Villu Ruusmann
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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;

public class PandasUtil {

	private PandasUtil(){
	}

	static
	public List<List<?>> parsePandasCategorical(String string){

		if(!string.startsWith(PandasUtil.PREFIX_PANDAS_CATEGORICAL)){
			throw new IllegalArgumentException(string);
		}

		string = string.substring(PandasUtil.PREFIX_PANDAS_CATEGORICAL.length());

		JsonElement element = JsonParser.parseString(string);

		Gson gson = createGson();

		return gson.fromJson(element, ListOfLists.class);
	}

	static
	public String formatPandasCategorical(List<List<?>> objects){
		Gson gson = createGson();

		return PandasUtil.PREFIX_PANDAS_CATEGORICAL + gson.toJson(objects, ListOfLists.class);
	}

	static
	private Gson createGson(){
		Gson result = new GsonBuilder()
			.setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
			.create();

		return result;
	}

	static
	private class ListOfLists extends ArrayList<List<?>> {
	}

	public static final String PREFIX_PANDAS_CATEGORICAL = "pandas_categorical:";
}