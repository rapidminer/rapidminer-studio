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
package com.rapidminer.tools.math.som;

import java.util.BitSet;
import java.util.Random;
import java.util.Vector;


/**
 * The RandomDataContainer is an implementation of the KohonenTrainingsData interface, and therefor
 * provides examples of data for a KohonenNet. The data is returned to the KohonenNet via an
 * iterator, which shuffels the data examples.
 * 
 * @author Sebastian Land
 */
public class RandomDataContainer implements KohonenTrainingsData {

	private static final long serialVersionUID = -3565717014239190320L;

	private Vector<double[]> data = new Vector<double[]>();

	private Random generator;

	private BitSet flag;

	public void addData(double[] data) {
		this.data.add(data);
	}

	@Override
	public int countData() {
		return data.size();
	}

	@Override
	public double[] getNext() {
		int chosen = -1;
		while (chosen < 0) {
			int dice = generator.nextInt(data.size());
			if (!flag.get(dice)) {
				flag.set(dice);
				return (data.elementAt(dice));
			}
		}
		return null;
	}

	@Override
	public void reset() {
		this.flag = new BitSet(data.size());
	}

	@Override
	public void setRandomGenerator(Random generator) {
		this.generator = generator;
	}

	@Override
	public double[] get(int index) {
		return data.elementAt(index);
	}
}
