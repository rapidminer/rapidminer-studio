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


/**
 * Create data rows based on random data in the range of the minimum and maximum values of the
 * attributes of the given base example set.
 * 
 * @author Ingo Mierswa Exp $
 * @deprecated since 7.5.4.
 */
@Deprecated
public class RandomDataRowReader extends AbstractDataRowReader {

	private int size;

	private ExampleSet baseExampleSet;

	private Attribute[] attributes;

	private int counter = 0;

	public RandomDataRowReader(ExampleSet baseExampleSet, Attribute[] attributes, int size) {
		super(new DataRowFactory(DataRowFactory.TYPE_DOUBLE_ARRAY, '.'));
		this.baseExampleSet = baseExampleSet;
		this.attributes = attributes;
		this.size = size;
	}

	@Override
	public boolean hasNext() {
		return (counter < size);
	}

	@Override
	public DataRow next() {
		RandomGenerator random = RandomGenerator.getGlobalRandomGenerator();
		double[] data = new double[attributes.length];
		for (int i = 0; i < data.length; i++) {
			double min = baseExampleSet.getStatistics(attributes[i], Statistics.MINIMUM);
			double max = baseExampleSet.getStatistics(attributes[i], Statistics.MAXIMUM);
			data[i] = random.nextDoubleInRange(min, max);
		}
		return new DoubleArrayDataRow(data);
	}
}
