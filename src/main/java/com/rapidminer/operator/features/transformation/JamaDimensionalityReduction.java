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
package com.rapidminer.operator.features.transformation;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;

import Jama.Matrix;


/**
 * This class is completely unnecessary and is only kept for compatibility reasons. The class
 * hierarchy is complete nonsense and will be dropped with one of the next versions. So if you
 * implement using this class, please implement this little code fragment below again or build a
 * more fitting class hierarchy.
 * 
 * This class represents an abstract framework for performing dimensionality reduction using the
 * JAMA package.
 * 
 * @author Michael Wurst, Ingo Mierswa
 * 
 */
@Deprecated
public abstract class JamaDimensionalityReduction extends DimensionalityReducer {

	public JamaDimensionalityReduction(OperatorDescription description) {
		super(description);
	}

	protected abstract Matrix callMatrixMethod(ExampleSet es, int dimension, Matrix in);

	@Override
	protected double[][] dimensionalityReduction(ExampleSet es, int dimensions) {
		// encode matrix
		Matrix in = new Matrix(es.size(), es.getAttributes().size());

		int count = 0;
		Attribute[] regularAttributes = es.getAttributes().createRegularAttributeArray();
		for (Example e : es) {
			for (int i = 0; i < regularAttributes.length; i++) {
				in.set(count, i, e.getValue(regularAttributes[i]));
			}
			count++;
		}
		Matrix result = callMatrixMethod(es, dimensions, in);

		// decode matrix
		return result.getArray();
	}
}
