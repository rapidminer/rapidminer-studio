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
import com.rapidminer.tools.XMLException;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;


/**
 * Rearranges the ordering of two subprocesses.
 * 
 * @author Simon Fischer
 * 
 */
public class ExchangeSubprocessesRule extends AbstractParseRule {

	private final int subprocess1;
	private final int subprocess2;

	public ExchangeSubprocessesRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		subprocess1 = Integer.parseInt(XMLTools.getTagContents(element, "subprocess1"));
		subprocess2 = Integer.parseInt(XMLTools.getTagContents(element, "subprocess2"));
	}

	@Override
	protected String apply(Operator operator, String operatorTypeName, XMLImporter importer) {
		OperatorChain chain = (OperatorChain) operator;
		ExecutionUnit unit1 = chain.getSubprocess(subprocess1);
		ExecutionUnit unit2 = chain.getSubprocess(subprocess2);
		List<Operator> ops1 = new LinkedList<Operator>(unit1.getOperators());
		List<Operator> ops2 = new LinkedList<Operator>(unit2.getOperators());

		for (Operator op : ops1) {
			op.remove();
		}
		for (Operator op : ops2) {
			op.remove();
		}

		for (Operator op : ops1) {
			unit2.addOperator(op);
		}
		for (Operator op : ops2) {
			unit1.addOperator(op);
		}
		return "Exchanged subprocesses " + (subprocess1 + 1) + " and " + (subprocess2 + 1) + " in <var>"
				+ operator.getName() + "</var> (<code>" + operator.getOperatorDescription().getName() + "</code>)";
	}

}
