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
package com.rapidminer.operator.generator;

import java.util.Random;


/**
 * Helper class for clusters information.
 * 
 * @author Ingo Mierswa
 */
public class Cluster {

	double[] coordinates;

	double[] sigmas;

	double size;

	int label;

	public Cluster(double[] coordinates, double[] sigmas, double size, int label) {
		this.coordinates = coordinates;
		this.sigmas = sigmas;
		this.size = size;
		this.label = label;
	}

	public double[] createArguments(Random random) {
		double[] args = new double[coordinates.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = coordinates[i] + random.nextGaussian() * sigmas[i];
		}
		return args;
	}
}
