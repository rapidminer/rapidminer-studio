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
package com.rapidminer.operator.postprocessing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * A convenience class that contains the parameters of a PlattScalingModel.
 * 
 * @author Martin Scholz
 */
public class PlattParameters implements Serializable {

	private static final long serialVersionUID = 7677598913328136657L;

	double a;

	double b;

	public PlattParameters() {}

	public PlattParameters(double a, double b) {
		this.a = a;
		this.b = b;
	}

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	void readParameters(ObjectInputStream in) throws IOException {
		this.a = in.readDouble();
		this.b = in.readDouble();
	}

	void writeParameters(ObjectOutputStream out) throws IOException {
		out.writeDouble(this.a);
		out.writeDouble(this.b);
	}

	@Override
	public String toString() {
		return ("Platt's scaling parameters: A=" + this.getA() + ", B=" + this.getB());
	}

}
