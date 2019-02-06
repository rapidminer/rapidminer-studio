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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.Tools;


/**
 * Converts all numerical attributes to nominal ones. Each numerical value is simply used as nominal
 * value of the new attribute. If the value is missing, the new value will be missing. Please note
 * that this operator might drastically increase memory usage if many different numerical values are
 * used. Please use the available discretization operators then.
 *
 * @author Ingo Mierswa
 */
public class NumericToPolynominal extends NumericToNominal {

	public NumericToPolynominal(OperatorDescription description) {
		super(description);
	}

	@Override
	protected void setValue(Example example, Attribute newAttribute, double value) {
		if (Double.isNaN(value)) {
			example.setValue(newAttribute, Double.NaN);
		} else {
			example.setValue(newAttribute, newAttribute.getMapping().mapString(Tools.formatIntegerIfPossible(value)));
		}
	}

	@Override
	protected int getGeneratedAttributevalueType() {
		return Ontology.NOMINAL;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NumericToPolynominal.class,
				null);
	}
}
