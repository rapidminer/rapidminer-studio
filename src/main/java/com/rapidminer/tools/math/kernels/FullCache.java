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
package com.rapidminer.tools.math.kernels;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import java.util.Iterator;


/**
 * Stores all distances in a matrix (attention: should only be used for smaller data sets).
 *
 * @author Ingo Mierswa
 */
public class FullCache implements KernelCache {

	private double[][] distances;

	public FullCache(ExampleSet exampleSet, Kernel kernel) {
		int size = exampleSet.size();
		this.distances = new double[size][size];
		Iterator<Example> reader = exampleSet.iterator();
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		int i = 0;
		while (reader.hasNext()) {
			Example example1 = reader.next();
			double[] x1 = new double[regularAttributes.length];
			int x = 0;
			for (Attribute attribute : regularAttributes) {
				x1[x++] = example1.getValue(attribute);
			}
			Iterator<Example> innerReader = exampleSet.iterator();
			int j = 0;
			while (innerReader.hasNext()) {
				Example example2 = innerReader.next();
				double[] x2 = new double[regularAttributes.length];
				x = 0;
				for (Attribute attribute : regularAttributes) {
					x2[x++] = example2.getValue(attribute);
				}
				double distance = kernel.calculateDistance(x1, x2);
				this.distances[i][j] = distance;
				this.distances[j][i] = distance;
				j++;
			}
			i++;
		}
	}

	@Override
	public double get(int i, int j) {
		return this.distances[i][j];
	}

	@Override
	public void store(int i, int j, double value) {
		this.distances[i][j] = value;
	}
}
