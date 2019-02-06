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
package com.rapidminer.example.table;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.tools.RandomGenerator;

import java.util.List;


/**
 * This class is used for example sets which should provide random values for the given attributes,
 * i.e. each random value lies in the same range as values of the given attributes (min and max) of
 * the base example set. Please note that the attributes must already have proper minimum and
 * maximum values. The random values are constructed by a {@link RandomDataRowReader}.
 * 
 * @author Ingo Mierswa Exp $
 * @deprecated since 7.5.4.
 */
@Deprecated
public class RandomExampleTable extends AbstractExampleTable {

	private static final long serialVersionUID = 5675878166499224680L;

	private ExampleSet baseExampleSet;

	private int size;

	public RandomExampleTable(ExampleSet baseExampleSet, List<Attribute> attributes, int size) {
		super(attributes);
		this.baseExampleSet = baseExampleSet;
		this.size = size;
	}

	@Override
	public DataRowReader getDataRowReader() {
		return new RandomDataRowReader(baseExampleSet, getAttributes(), size);
	}

	@Override
	public DataRow getDataRow(int index) {
		RandomGenerator random = RandomGenerator.getGlobalRandomGenerator();
		double[] data = new double[size];
		for (int i = 0; i < data.length; i++) {
			double min = baseExampleSet.getStatistics(getAttributes()[i], Statistics.MINIMUM);
			double max = baseExampleSet.getStatistics(getAttributes()[i], Statistics.MAXIMUM);
			data[i] = random.nextDoubleInRange(min, max);
		}
		return new DoubleArrayDataRow(data);
	}

	@Override
	public int size() {
		return size;
	}
}
