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
package com.rapidminer.tools.math.optimization.ec.es;

import java.util.Random;


/**
 * Like RouletteWheel this population operator selects a given fixed number of individuals by
 * subdividing a roulette wheel in sections of size proportional to the individuals' fitness values.
 * The fitness values are filtered according to the Boltzmann theorem.
 * 
 * @author Ingo Mierswa
 */
public class BoltzmannSelection extends RouletteWheel {

	private double temperature;

	private double delta = 0.0d;

	public BoltzmannSelection(int popSize, double temperature, int maxGenerations, boolean dynamic, boolean keepBest,
			Random random) {
		super(popSize, keepBest, random);
		this.temperature = temperature;
		if (dynamic) {
			this.delta = this.temperature / (maxGenerations + 1);
		}
	}

	/**
	 * Returns a fitness based on the Boltzmann theorem, i.e. exp(fitness/temperature). Like for
	 * simulated annealing the temperature should decrease over time.
	 */
	@Override
	public double filterFitness(double fitness) {
		return Math.exp(fitness / temperature);
	}

	/**
	 * Applies the method from the superclass and decreases the temperature in the adaptive case.
	 */
	@Override
	public void operate(Population population) {
		super.operate(population);
		this.temperature -= delta;
	}
}
