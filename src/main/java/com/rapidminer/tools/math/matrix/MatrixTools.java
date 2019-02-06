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
package com.rapidminer.tools.math.matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;

import Jama.Matrix;


/**
 * This is a class containing general methods for matrices.
 *
 * @author Sebastian Land
 */
public class MatrixTools {

	/**
	 * This method copies the complete data of the exampleSet into an Matrix backed by an array.
	 */
	public static final Matrix getDataAsMatrix(ExampleSet exampleSet) {
		Attributes attributes = exampleSet.getAttributes();
		double[][] data = new double[exampleSet.size()][attributes.size()];
		int c = 0;
		for (Attribute attribute : attributes) {
			int r = 0;
			for (Example example : exampleSet) {
				data[r][c] = example.getValue(attribute);
				r++;
			}
			c++;
		}
		return new Matrix(data);
	}
}
