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
package org.jpmml.lightgbm.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.PMML;
import org.jpmml.lightgbm.GBDT;
import org.jpmml.lightgbm.HasLightGBMOptions;
import org.jpmml.lightgbm.LightGBMUtil;
import org.jpmml.lightgbm.ObjectiveFunction;
import org.jpmml.model.JAXBSerializer;
import org.jpmml.model.metro.MetroJAXBSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	@Parameter (
		names = {"--lgbm-input"},
		description = "LightGBM text input file",
		required = true,
		order = 1
	)
	private File input = null;

	@Parameter (
		names = {"--pmml-output"},
		description = "PMML output file",
		required = true,
		order = 2
	)
	private File output = null;

	@Parameter (
		names = {"--objective"},
		description = "Custom objective function",
		order = 3
	)
	private String objectiveFunction = null;

	@Parameter (
		names = {"--target-name"},
		description = "Target name. Defaults to \"_target\"",
		order = 4
	)
	private String targetName = null;

	@Parameter (
		names = {"--target-categories"},
		description = "Target categories. Defaults to 0-based index [0, 1, .., num_class - 1]",
		order = 5
	)
	private List<String> targetCategories = null;

	@Parameter (
		names = {"--X-" + HasLightGBMOptions.OPTION_COMPACT},
		description = "Transform LightGBM-style trees to PMML-style trees",
		arity = 1,
		order = 6
	)
	private boolean compact = true;

	@Parameter (
		names = {"--X-" + HasLightGBMOptions.OPTION_NAN_AS_MISSING},
		description = "Treat Not-a-Number (NaN) values as missing values",
		arity = 1,
		order = 7
	)
	private boolean nanAsMissing = true;

	@Parameter (
		names = {"--X-" + HasLightGBMOptions.OPTION_NUM_ITERATION},
		description = "Limit the number of trees. Defaults to all trees",
		order = 8
	)
	private Integer numIteration = null;

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true,
		order = Integer.MAX_VALUE
	)
	private boolean help = false;

	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		IUsageFormatter usageFormatter = new DefaultUsageFormatter(commander);

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			usageFormatter.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			usageFormatter.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		GBDT gbdt;

		try(InputStream is = new FileInputStream(this.input)){
			logger.info("Loading GBDT..");

			long begin = System.currentTimeMillis();
			gbdt = LightGBMUtil.loadGBDT(is);
			long end = System.currentTimeMillis();

			logger.info("Loaded GBDT in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to load GBDT", e);

			throw e;
		}

		if(this.objectiveFunction != null){
			logger.info("Setting custom objective function");

			ObjectiveFunction objectiveFunction = LightGBMUtil.parseObjectiveFunction(this.objectiveFunction);

			gbdt.setObjectiveFunction(objectiveFunction);
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasLightGBMOptions.OPTION_COMPACT, this.compact);
		options.put(HasLightGBMOptions.OPTION_NAN_AS_MISSING, this.nanAsMissing);
		options.put(HasLightGBMOptions.OPTION_NUM_ITERATION, this.numIteration);

		PMML pmml;

		try {
			logger.info("Converting GBDT to PMML..");

			long begin = System.currentTimeMillis();
			pmml = gbdt.encodePMML(options, this.targetName, this.targetCategories);
			long end = System.currentTimeMillis();

			logger.info("Converted GBDT to PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to convert GBDT to PMML", e);

			throw e;
		}

		try(OutputStream os = new FileOutputStream(this.output)){
			logger.info("Marshalling PMML..");

			JAXBSerializer jaxbSerializer = new MetroJAXBSerializer();

			long begin = System.currentTimeMillis();
			jaxbSerializer.serializePretty(pmml, os);
			long end = System.currentTimeMillis();

			logger.info("Marshalled PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to marshal PMML", e);

			throw e;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
}