/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * An abstract superclass for single, i.e. non-list, parameters.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class ParameterTypeSingle extends ParameterType {

	private static final long serialVersionUID = 1144201124955949715L;

	public ParameterTypeSingle(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeSingle(String key, String description) {
		super(key, description);
	}

	@Override
	@Deprecated
	public Element getXML(String key, String value, boolean hideDefault, Document doc) {
		return getXML(key, value, hideDefault, EncryptionProvider.DEFAULT_CONTEXT, doc);
	}

	@Override
	public Element getXML(String key, String value, boolean hideDefault, String encryptionContext, Document doc) {
		Element element = doc.createElement("parameter");
		element.setAttribute("key", key);
		if (value != null) {
			if (toString(value).equals(toString(getDefaultValue()))) {
				if (!hideDefault) {
					element.setAttribute("value", value);
				} else {
					return null;
				}
			} else {
				element.setAttribute("value", toXMLString(value, encryptionContext));
			}
		} else {
			if (!hideDefault && getDefaultValue() != null) {
				element.setAttribute("value", getDefaultValue().toString());
			} else {
				return null;
			}
		}
		return element;
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		return mh.resolveMacros(getKey(), parameterValue);
	}
}
