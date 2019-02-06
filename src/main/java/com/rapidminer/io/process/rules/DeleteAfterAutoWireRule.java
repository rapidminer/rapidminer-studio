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
package com.rapidminer.io.process.rules;

import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * 
 * @author Sebastian Land
 */
public class DeleteAfterAutoWireRule extends AbstractParseRule {

	public DeleteAfterAutoWireRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
	}

	@Override
	protected String apply(final Operator operator, final String operatorTypeName, final XMLImporter importer) {
		importer.doAfterAutoWire(new Runnable() {

			@Override
			public void run() {
				operator.remove();
				importer.addMessage("Removed operator '<var>" + operator.getName() + "</var>' (<code>" + operatorTypeName
						+ "</code>)");
			}
		});
		return null;
	}
}
