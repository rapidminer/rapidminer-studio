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
package com.rapidminer.tools.expression.internal.function.mathematical;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.internal.function.Abstract1DoubleInputFunction;


/**
 * A {@link Function} computing the natural logarithm (base e) of a number.
 *
 * @author Marcel Seifert
 *
 */
public class NaturalLogarithm extends Abstract1DoubleInputFunction {

	public NaturalLogarithm() {
		super("mathematical.ln", Ontology.NUMERICAL);
	}

	@Override
	protected double compute(double value) {
		return Math.log(value);
	}

}
