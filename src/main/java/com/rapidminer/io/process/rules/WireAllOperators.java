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
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * Wires all immediate children of an operator chain.
 * 
 * @author Simon Fischer
 * 
 */
public class WireAllOperators extends AbstractParseRule {

	private final int subprocess;

	public WireAllOperators(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		subprocess = Integer.parseInt(XMLTools.getTagContents(element, "subprocess"));
	}

	@Override
	protected String apply(final Operator operator, String operatorTypeName, XMLImporter importer) {
		importer.doAfterAutoWire(new Runnable() {

			@Override
			public void run() {
				OperatorChain chain = (OperatorChain) operator;
				ExecutionUnit unit = chain.getSubprocess(subprocess);
				for (Operator op : unit.getOperators()) {
					unit.autoWireSingle(op, CompatibilityLevel.PRE_VERSION_5, true, true);
				}
			}
		});
		return null;
	}

}
