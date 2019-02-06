/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.tools;

import com.rapidminer.operator.annotation.PolynomialExampleSetResourceConsumptionEstimator;
import com.rapidminer.operator.annotation.PolynomialFunction;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.tools.AttributeSubsetSelector;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * This class handles all existing ResourceConsumptionEstimator values, which are stored in the
 * "OperatorsResourceConsumption.csv" file. If no value is found for a given Operator,
 * <code>null</code> will be returned.
 * 
 * @author Marco Boeck
 */
public class OperatorResourceConsumptionHandler {

	/** name of the csv resource consumption file */
	private static final String OPERATORS_RESOURCE_CONSUMPTION = "OperatorsResourceConsumption.csv";

	/** number of values the csv file contains per row */
	private static final int RESOURCE_CONSUMPTION_CSV_SPLITPARTS = 11;

	/** map holding the class name and ResourceConsumptionValues */
	private static Map<String, String[]> resourceMap;

	private static final Logger LOGGER = Logger.getLogger(OperatorResourceConsumptionHandler.class.getName());

	static {
		resourceMap = new HashMap<String, String[]>();
		BufferedReader reader = null;
		String resource = "/" + Tools.RESOURCE_PREFIX + OPERATORS_RESOURCE_CONSUMPTION;
		try {
			URL url = OperatorResourceConsumptionHandler.class.getResource(resource);
			if (url == null) {
				LOGGER.warning(I18N.getMessage(I18N.getErrorBundle(), "profiler.error.no_csv_file",
						OPERATORS_RESOURCE_CONSUMPTION));
			} else {
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				String[] splitString;
				String row;
				int i = 0;
				while ((row = reader.readLine()) != null) {
					i++;
					// skip empty rows
					if (row.trim().equals("")) {
						continue;
					}
					// skip comments
					if (row.trim().charAt(0) == '#') {
						continue;
					}
					splitString = row.trim().split(";");
					// malformed csv file
					if (splitString.length != RESOURCE_CONSUMPTION_CSV_SPLITPARTS) {
						LOGGER.warning(I18N.getMessage(I18N.getErrorBundle(), "profiler.error.malformed_csv_file",
								OPERATORS_RESOURCE_CONSUMPTION, i));
					}
					resourceMap.put(splitString[0], splitString);
				}
			}
		} catch (FileNotFoundException e) {
			LOGGER.warning(e.getLocalizedMessage());
		} catch (IOException e) {
			LOGGER.warning(e.getLocalizedMessage());
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Gets an array with the cpu time consumption values. <br>
	 * [0] is coefficient, [1] is degreeExamples, [2] is degreeAttributes, [3] is
	 * degreeLogarithmusExamples, [4] is degreeLogarithmusAttributes <br>
	 * Returns <code>null</code> if no values are found in the CSV file.
	 * 
	 * @param className
	 *            use XYZ.class where XYZ is the operator class
	 * @return an array containg cpu time consumption values. [0] is coefficient, [1] is
	 *         degreeExamples, [2] is degreeAttributes, [3] is degreeLogarithmusExamples, [4] is
	 *         degreeLogarithmusAttributes
	 */
	public static String[] getTimeConsumption(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz must not be null!");
		}
		if (resourceMap.get(clazz.toString()) == null) {
			return null;
		}
		String[] savedString = resourceMap.get(clazz.toString());
		try {
			return new String[] { savedString[1], savedString[2], savedString[3], savedString[4], savedString[5] };
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Gets an array with the memory consumption values. <br>
	 * [0] is coefficient, [1] is degreeExamples, [2] is degreeAttributes, [3] is
	 * degreeLogarithmusExamples, [4] is degreeLogarithmusAttributes <br>
	 * Returns <code>null</code> if no values are found in the CSV file.
	 * 
	 * @param className
	 *            use XYZ.class where XYZ is the operator class
	 * @return an array containg memory consumption values. [0] is coefficient, [1] is
	 *         degreeExamples, [2] is degreeAttributes, [3] is degreeLogarithmusExamples, [4] is
	 *         degreeLogarithmusAttributes
	 */
	public static String[] getMemoryConsumption(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz must not be null!");
		}
		if (resourceMap.get(clazz.toString()) == null) {
			return null;
		}
		String[] savedString = resourceMap.get(clazz.toString());
		try {
			return new String[] { savedString[6], savedString[7], savedString[8], savedString[9], savedString[10] };
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Gets the ResourceConsumptionEstimator for a given class.
	 * 
	 * @param inputPort
	 *            the input port
	 * @param clazz
	 *            the class for which the ResourceConsumptionEstimator should be created
	 * @param attributeSelector
	 *            the attributeSelector (if existing)
	 * @return the ResourceConsumptionEstimator for the given class
	 */
	public static ResourceConsumptionEstimator getResourceConsumptionEstimator(InputPort inputPort, Class<?> clazz,
			AttributeSubsetSelector attributeSelector) {
		String[] timeConsumption = getTimeConsumption(clazz);
		String[] memoryConsumption = getMemoryConsumption(clazz);
		if (timeConsumption == null || memoryConsumption == null) {
			return null;
		}

		PolynomialFunction timeFunction = new PolynomialFunction(Double.parseDouble(timeConsumption[0]),
				Double.parseDouble(timeConsumption[1]), Double.parseDouble(timeConsumption[3]),
				Double.parseDouble(timeConsumption[2]), Double.parseDouble(timeConsumption[4]));
		PolynomialFunction memoryFunction = new PolynomialFunction(Double.parseDouble(memoryConsumption[0]),
				Double.parseDouble(memoryConsumption[1]), Double.parseDouble(memoryConsumption[3]),
				Double.parseDouble(memoryConsumption[2]), Double.parseDouble(memoryConsumption[4]));

		return new PolynomialExampleSetResourceConsumptionEstimator(inputPort, attributeSelector, timeFunction,
				memoryFunction);
	}
}
