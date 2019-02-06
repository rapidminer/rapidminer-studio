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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.examples;

import java.io.Serializable;


/**
 * An Example for the kernel based algorithms provided by Stefan Rueping. Since RapidMiner cannot
 * deliver the example with index i directly, a new data structure is needed.
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class SVMExample implements Serializable {

	private static final long serialVersionUID = 8539279195547132597L;

	public int[] index;

	public double[] att;

	public SVMExample() {
		index = null;
		att = null;
	}

	public SVMExample(double[] values) {
		index = new int[values.length];
		for (int i = 0; i < index.length; i++) {
			index[i] = i;
		}
		this.att = values;
	}

	/*
	 * For internal purposes only!!!
	 */
	public SVMExample(SVMExample e) {
		this.index = e.index;
		this.att = e.att;
	}

	public SVMExample(int[] new_index, double[] new_att) {
		index = new_index;
		att = new_att;
	}

	public double[] toDense(int dim) {
		double[] dense;
		dense = new double[dim];
		int pos = 0;
		if (index != null) {
			for (int i = 0; i < index.length; i++) {
				while (pos < index[i]) {
					dense[pos] = 0.0;
					pos++;
				}
				dense[pos] = att[i];
				pos++;
			}
		}
		while (pos < dim) {
			dense[pos] = 0.0;
			pos++;
		}
		return dense;
	}
}
