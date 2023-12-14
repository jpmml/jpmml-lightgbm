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
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;

public class PandasCategoricalParser {

	private String string = null;


	public PandasCategoricalParser(String string){
		setString(string);
	}

	public List<List<?>> parsePandasCategorical(){
		String string = getString();

		if(!string.startsWith(PandasCategoricalParser.PREFIX)){
			throw new IllegalArgumentException(string);
		}

		string = string.substring(PandasCategoricalParser.PREFIX.length());

		JsonElement element = JsonParser.parseString(string);

		Gson gson = new GsonBuilder()
		    .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
		    .create();

		List<List<?>> result = gson.fromJson(element, ListOfLists.class);
		if(result == null){
			result = Collections.emptyList();
		}

		return result;
	}

	public String getString(){
		return this.string;
	}

	private void setString(String string){
		this.string = string;
	}

	static
	private class ListOfLists extends ArrayList<List<?>> {
	}

	private static final String PREFIX = "pandas_categorical:";
}