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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LightGBMUtil {

	private LightGBMUtil(){
	}

	static
	public GBDT loadGBDT(InputStream is) throws IOException {
		return loadGBDT(parseText(is));
	}

	static
	public GBDT loadGBDT(Iterator<String> lines){
		List<Section> sections = loadText(lines);

		GBDT gbdt = new GBDT();
		gbdt.load(sections);

		return gbdt;
	}

	static
	private List<Section> loadText(Iterator<String> lines){
		List<Section> sections = new ArrayList<>();

		Section section = new Section();

		loop:
		while(lines.hasNext()){
			String line = lines.next();

			if(("").equals(line)){

				if(section.size() > 0){
					sections.add(section);

					section = new Section();
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

			section.put(key, value);
		}

		if(section.size() > 0){
			sections.add(section);
		}

		return sections;
	}

	static
	public Iterator<String> parseText(InputStream is) throws IOException {
		Reader reader = new InputStreamReader(is, "US-ASCII");

		return parseText(reader);
	}

	static
	public Iterator<String> parseText(Reader reader) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(reader){

			@Override
			public void close(){
			}
		};

		try {
			List<String> lines = new ArrayList<>();

			while(true){
				String line = bufferedReader.readLine();
				if(line == null){
					break;
				}

				lines.add(line);
			}

			return lines.iterator();
		} finally {
			bufferedReader.close();
		}
	}

	static
	public String[] parseStringArray(String string, int length){
		String[] result = string.split("\\s");

		if(result.length != length){
			throw new IllegalArgumentException();
		}

		return result;
	}

	static
	public int[] parseIntArray(String string, int length){
		int[] result = new int[length];

		String[] values = parseStringArray(string, length);
		for(int i = 0; i < length; i++){
			result[i] = Integer.parseInt(values[i]);
		}

		return result;
	}

	static
	public double[] parseDoubleArray(String string, int length){
		double[] result = new double[length];

		String[] values = parseStringArray(string, length);
		for(int i = 0; i < length; i++){
			result[i] = Double.parseDouble(values[i]);
		}

		return result;
	}
}