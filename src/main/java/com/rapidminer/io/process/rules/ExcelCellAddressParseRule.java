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
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A simple parse rule that will transform two given parameters into one excel range stored into a
 * third operator.
 * 
 * @author Sebastian Land
 */
public class ExcelCellAddressParseRule extends AbstractConditionedParseRule {

	private String parameterColumn;
	private String parameterRow;
	private String parameterAddress;

	public ExcelCellAddressParseRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		assert (element.getTagName().equals("replaceByCellAddress"));
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("columnParameter")) {
					parameterColumn = childElem.getTextContent();
				} else if (childElem.getTagName().equals("rowParameter")) {
					parameterRow = childElem.getTextContent();
				} else if (childElem.getTagName().equals("addressParameter")) {
					parameterAddress = childElem.getTextContent();
				}
			}
		}

	}

	@Override
	protected String conditionedApply(Operator operator, String operatorTypeName, XMLImporter importer) {

		try {
			int column = operator.getParameterAsInt(parameterRow);
			int row = operator.getParameterAsInt(parameterColumn);
			operator.setParameter(parameterAddress, Tools.getExcelColumnName(column) + (row + 1));
			return "Replaced column and row offset by cell address parameter in <var>\"" + operator.getName() + "\"</var>.";
		} catch (UndefinedParameterError e) {
			return null;
		}
	}

}
