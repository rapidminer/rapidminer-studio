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
package com.rapidminer.operator.annotation;

import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * Only highest order terms taken into account. Functions can be of the form
 * 
 * c * log(n)^d1 * n^d2 * log(m)^*d3 * m^d4
 * 
 * 
 * @author Simon Fischer
 */
public class PolynomialFunction {

	private double coefficient;
	private double degreeExamples;
	private double degreeAttributes;
	private double logDegreeExamples;
	private double logDegreeAttributes;

	public PolynomialFunction(double coefficient, double degreeExamples, double degreeAttributes) {
		this(coefficient, degreeExamples, 0, degreeAttributes, 0);
	}

	public PolynomialFunction(double coefficient, double degreeExamples, double logDegreeExamples, double degreeAttributes,
			double logDegreeAttributes) {
		super();
		this.coefficient = coefficient;
		this.degreeAttributes = degreeAttributes;
		this.degreeExamples = degreeExamples;
		this.logDegreeAttributes = logDegreeAttributes;
		this.logDegreeExamples = logDegreeExamples;
	}

	public static PolynomialFunction makeLinearFunction(double coefficient) {
		return new PolynomialFunction(coefficient, 1, 1);
	}

	public long evaluate(int numExamples, int numAttributes) {
		return (long) (coefficient * Math.pow(numExamples, degreeExamples)
				* Math.pow(Math.log(numExamples), logDegreeExamples) * Math.pow(numAttributes, degreeAttributes) * Math.pow(
				Math.log(numAttributes), logDegreeAttributes));
	}

	@Override
	public String toString() {
		// this is true for both 0.0 and -0.0 because primitive compare is defined that way
		if (coefficient == 0.0d) {
			return "n/a";
		}
		NumberFormat formatter = new DecimalFormat("#.##");
		StringBuffer resourceString = new StringBuffer();
		resourceString.append("f() = ");
		resourceString.append(formatter.format(coefficient));
		if (degreeExamples > 0 || degreeAttributes > 0) {
			resourceString.append(" * (");
		}
		if (degreeExamples > 0) {
			if (logDegreeExamples > 0) {
				resourceString.append("log");
				resourceString.append(formatter.format(logDegreeExamples));
				if (degreeExamples > 1) {
					resourceString.append("(examples^");
					resourceString.append(formatter.format(degreeExamples));
				} else {
					resourceString.append("(examples");
				}
				resourceString.append(')');
			} else {
				if (degreeExamples > 1) {
					resourceString.append("examples^");
					resourceString.append(formatter.format(degreeExamples));
				} else {
					resourceString.append("examples");
				}
			}
			if (degreeAttributes > 0) {
				resourceString.append(" * ");
			}
		}
		if (degreeAttributes > 0) {
			if (logDegreeAttributes > 0) {
				resourceString.append("log");
				resourceString.append(formatter.format(logDegreeAttributes));
				if (degreeAttributes > 1) {
					resourceString.append("(attributes^");
					resourceString.append(formatter.format(degreeAttributes));
				} else {
					resourceString.append("(attributes");
				}
				resourceString.append(')');
			} else {
				if (degreeAttributes > 1) {
					resourceString.append("attributes^");
					resourceString.append(formatter.format(degreeAttributes));
				} else {
					resourceString.append("attributes");
				}
			}
		}
		if (degreeExamples > 0 || degreeAttributes > 0) {
			resourceString.append(')');
		}
		return resourceString.toString();
	}
}
