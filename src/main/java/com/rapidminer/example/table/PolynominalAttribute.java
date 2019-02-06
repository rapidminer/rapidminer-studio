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

import com.rapidminer.tools.Ontology;

import java.util.Iterator;


/**
 * This class holds all information on a single nominal attribute. In addition to the generic
 * attribute fields this class keeps information about the nominal values and the value to index
 * mappings. If one of the methods designed for numerical attributes was invoked a RuntimeException
 * will be thrown.
 * 
 * It will be guaranteed that all values are mapped to indices without any missing values. This
 * could, however, be changed in future versions thus operators should not rely on this fact.
 * 
 * This class is one of the two available implementations of {@link NominalAttribute} available in
 * RapidMiner. In contrast to the {@link BinominalAttribute}, which stores the possible values
 * internally very efficient, this class allows an arbitrary number of nominal values and uses a
 * {@link PolynominalMapping} for the internal representation mapping.
 * 
 * @author Ingo Mierswa Exp $
 */
public class PolynominalAttribute extends NominalAttribute {

	private static final long serialVersionUID = 3713022530244256813L;

	/** The maximum number of nominal values displayed in result strings. */
	private static final int MAX_NUMBER_OF_SHOWN_NOMINAL_VALUES = 100;

	private NominalMapping nominalMapping = new PolynominalMapping();

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 */
	/* pp */PolynominalAttribute(String name) {
		this(name, Ontology.NOMINAL);
	}

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 */
	/* pp */PolynominalAttribute(String name, int valueType) {
		super(name, valueType);
	}

	/**
	 * Clone constructor.
	 */
	private PolynominalAttribute(PolynominalAttribute a) {
		super(a);
		// this.nominalMapping = (NominalMapping)a.nominalMapping.clone();
		this.nominalMapping = a.nominalMapping;
	}

	/** Clones this attribute. */
	@Override
	public Object clone() {
		return new PolynominalAttribute(this);
	}

	@Override
	public NominalMapping getMapping() {
		return this.nominalMapping;
	}

	@Override
	public void setMapping(NominalMapping newMapping) {
		this.nominalMapping = new PolynominalMapping(newMapping);
	}

	// ================================================================================
	// string and result methods
	// ================================================================================

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append("/values=[");
		Iterator<String> i = this.nominalMapping.getValues().iterator();
		int index = 0;
		while (i.hasNext()) {
			if (index >= MAX_NUMBER_OF_SHOWN_NOMINAL_VALUES) {
				result.append(", ... (" + (this.nominalMapping.getValues().size() - MAX_NUMBER_OF_SHOWN_NOMINAL_VALUES)
						+ " values) ...");
				break;
			}
			if (index != 0) {
				result.append(", ");
			}
			result.append(i.next());
			index++;
		}
		result.append("]");
		return result.toString();
	}

	@Override
	public boolean isDateTime() {
		return false;
	}
}
