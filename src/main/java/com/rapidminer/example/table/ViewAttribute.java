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
package com.rapidminer.example.table;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.MinMaxStatistics;
import com.rapidminer.example.NominalStatistics;
import com.rapidminer.example.NumericalStatistics;
import com.rapidminer.example.UnknownStatistics;
import com.rapidminer.example.WeightedNumericalStatistics;
import com.rapidminer.operator.ViewModel;
import com.rapidminer.tools.Ontology;


/**
 * A view attribute is based on a ViewModel (Preprocessing Model) and applies the model on the fly.
 *
 * @author Sebastian Land
 */
public class ViewAttribute extends AbstractAttribute {

	private static final long serialVersionUID = -4075558616549596028L;

	private NominalMapping mapping;

	private boolean isNominal;

	private boolean isNumerical;

	private ViewModel model;

	private Attribute parent;

	protected ViewAttribute(ViewAttribute other) {
		super(other);
		if (other.mapping != null) {
			this.mapping = (NominalMapping) other.mapping.clone();
		}
		this.isNominal = other.isNominal;
		this.isNumerical = other.isNumerical;
		this.model = other.model;
		if (other.parent != null) {
			this.parent = (Attribute) other.parent.clone();
		}
	}

	public ViewAttribute(ViewModel model, Attribute parent, String name, int valueType, NominalMapping mapping) {
		super(name, valueType);
		this.model = model;
		this.mapping = mapping;
		this.isNominal = mapping != null && Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL);
		this.isNumerical = Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NUMERICAL);

		this.parent = parent;
		if (isNominal) {
			registerStatistics(new NominalStatistics());
			registerStatistics(new UnknownStatistics());
		} else {
			registerStatistics(new NumericalStatistics());
			registerStatistics(new WeightedNumericalStatistics());
			registerStatistics(new MinMaxStatistics());
			registerStatistics(new UnknownStatistics());
		}
	}

	@Override
	public double getValue(DataRow row) {
		return model.getValue(this, row.get(parent));
	}

	@Override
	public Object clone() {
		return new ViewAttribute(this);
	}

	@Override
	public String getAsString(double value, int numberOfDigits, boolean quoteNominal) {
		if (isNominal) {
			if (Double.isNaN(value)) {
				return "?";
			} else {
				try {
					String result = mapping.mapIndex((int) value);
					if (quoteNominal) {
						result = result.replaceAll("\"", "\\\\\"");
						result = "\"" + result + "\"";
					}
					return result;
				} catch (Throwable e) {
					return "?";
				}
			}
		} else {
			if (Double.isNaN(value)) {
				return "?";
			} else {
				switch (numberOfDigits) {
					case NumericalAttribute.UNLIMITED_NUMBER_OF_DIGITS:
						return Double.toString(value);
					case NumericalAttribute.DEFAULT_NUMBER_OF_DIGITS:
						return com.rapidminer.tools.Tools.formatIntegerIfPossible(value);
					default:
						return com.rapidminer.tools.Tools.formatIntegerIfPossible(value, numberOfDigits);
				}
			}
		}

	}

	@Override
	public NominalMapping getMapping() {
		return mapping;
	}

	@Override
	public boolean isNominal() {
		return isNominal;
	}

	@Override
	public boolean isNumerical() {
		return isNumerical;
	}

	@Override
	public void setMapping(NominalMapping nominalMapping) {
		mapping = nominalMapping;
	}

	@Override
	public int getTableIndex() {
		// return Attribute.VIEW_ATTRIBUTE_INDEX;
		// TODO: is this correct? without parent index, this might lead to problems,
		// e.g. for discretizations of the label inside of an AttributeSubsetPreprocessing
		// (index of label is then -1)
		return parent.getTableIndex();
	}

	@Override
	public boolean isDateTime() {
		// TODO Auto-generated method stub
		return false;
	}
}
