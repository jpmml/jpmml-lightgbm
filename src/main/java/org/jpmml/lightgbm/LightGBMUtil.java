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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LightGBMUtil {

	private LightGBMUtil(){
	}

	static
	public GBDT loadGBDT(InputStream is) throws IOException {
		List<Map<String, String>> blocks = loadText(is);

		GBDT gbdt = new GBDT();
		gbdt.load(blocks);

		return gbdt;
	}

	static
	private List<Map<String, String>> loadText(InputStream is) throws IOException {
		List<Map<String, String>> result = new ArrayList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "US-ASCII")){

			@Override
			public void close(){
			}
		};

		Map<String, String> map = new LinkedHashMap<>();

		loop:
		while(true){
			String line = reader.readLine();
			if(line == null){
				break;
			} // End if

			if(("").equals(line)){

				if(map.size() > 0){
					result.add(map);

					map = new LinkedHashMap<>();
				}

				continue loop;
			}

			String key;
			String value;

			int equals = line.indexOf('=');
			if(equals > 0){
				key = line.substring(0, equals);
				value = line.substring(equals + 1);
			} else

			{
				key = line;
				value = null;
			}

			map.put(key, value);
		}

		if(map.size() > 0){
			result.add(map);
		}

		reader.close();

		return result;
	}

	static
	public String[] parseStringArray(int length, String string){
		String[] result = string.split("\\s");

		if(result.length != length){
			throw new IllegalArgumentException();
		}

		return result;
	}

	static
	public int[] parseIntArray(int length, String string){
		int[] result = new int[length];

		String[] values = parseStringArray(length, string);
		for(int i = 0; i < length; i++){
			result[i] = Integer.parseInt(values[i]);
		}

		return result;
	}

	static
	public double[] parseDoubleArray(int length, String string){
		double[] result = new double[length];

		String[] values = parseStringArray(length, string);
		for(int i = 0; i < length; i++){
			result[i] = Double.parseDouble(values[i]);
		}

		return result;
	}
}