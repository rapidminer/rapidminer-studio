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
package com.rapidminer.example;

import java.io.Serializable;


/**
 * The superclass for all attribute statistics objects.
 * 
 * @author Ingo Mierswa
 */
public interface Statistics extends Serializable, Cloneable {

	public static final String UNKNOWN = "unknown";
	public static final String AVERAGE = "average";
	public static final String AVERAGE_WEIGHTED = "average_weighted";
	public static final String VARIANCE = "variance";
	public static final String VARIANCE_WEIGHTED = "variance_weighted";
	public static final String MINIMUM = "minimum";
	public static final String MAXIMUM = "maximum";
	public static final String MODE = "mode";
	public static final String LEAST = "least";
	public static final String COUNT = "count";
	public static final String SUM = "sum";
	public static final String SUM_WEIGHTED = "sum_weighted";

	public Object clone();

	public void startCounting(Attribute attribute);

	public void count(double value, double weight);

	public boolean handleStatistics(String statisticsName);

	public double getStatistics(Attribute attribute, String statisticsName, String parameter);

}
