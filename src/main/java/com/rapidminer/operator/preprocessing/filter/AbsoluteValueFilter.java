/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.preprocessing.AbstractValueProcessing;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;


/**
 * This operator simply replaces all values by their absolute respective value.
 * 
 * @author Ingo Mierswa, Sebastian Land, Tobias Malbrecht
 */
public class AbsoluteValueFilter extends AbstractValueProcessing {

	public AbsoluteValueFilter(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData metaData) {
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (amd.isNumerical() && !amd.isSpecial()) {
				Range range = amd.getValueRange();
				amd.setValueRange(new Range(0, Math.max(Math.abs(range.getLower()), Math.abs(range.getUpper()))),
						amd.getValueSetRelation());
				amd.getMean().setUnkown();
			}
		}

		return metaData;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		for (Example example : exampleSet) {
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (attribute.isNumerical()) {
					double value = example.getValue(attribute);
					value = Math.abs(value);
					example.setValue(attribute, value);
				}
			}
		}
		return exampleSet;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}

	@Override
	public boolean writesIntoExistingData() {
		return true;
	}
}
