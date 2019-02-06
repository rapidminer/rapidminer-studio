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
package com.rapidminer.parameter;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * A parameter type for ordering rules of attributes. This parameter type has been introduced to
 * allow a different cell rendered for this class.
 * 
 * @author Nils Woehler
 */
public class ParameterTypeAttributeOrderingRules extends ParameterTypeAttribute {

	private static final long serialVersionUID = 1L;

	public ParameterTypeAttributeOrderingRules(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeAttributeOrderingRules(final String key, String description, InputPort inPort) {
		this(key, description, inPort, true, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttributeOrderingRules(final String key, String description, InputPort inPort, int... valueTypes) {
		this(key, description, inPort, true, valueTypes);
	}

	public ParameterTypeAttributeOrderingRules(final String key, String description, InputPort inPort, boolean optional) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttributeOrderingRules(final String key, String description, InputPort inPort, boolean optional,
			int... valueTypes) {
		super(key, description, inPort, optional, valueTypes);
	}

	public ParameterTypeAttributeOrderingRules(final String key, String description, InputPort inPort, boolean optional,
			boolean expert) {
		this(key, description, inPort, optional);
		setExpert(expert);
	}

	@Override
	protected boolean isFilteredOut(AttributeMetaData amd) {
		if (amd.isSpecial()) {
			return true;
		}
		return super.isFilteredOut(amd);
	}
}
