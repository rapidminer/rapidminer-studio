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
package com.rapidminer.parameter.conditions;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * This {@link ParameterCondition} implementation checks whether the currently selected
 * {@link OperatorVersion} is above a predefined one.
 * 
 * @author Nils Woehler
 */
public class AboveOperatorVersionCondition extends ParameterCondition {

	private static final String ELEMENT_VERSION = "IsAtMostVersion";
	private VersionNumber aboveVersion;
	private Operator operator;

	public AboveOperatorVersionCondition(Element element) throws XMLException {
		super(element);
		aboveVersion = new VersionNumber(XMLTools.getTagContents(element, ELEMENT_VERSION, true));
	}

	public AboveOperatorVersionCondition(Operator operator, VersionNumber aboveVersion) {
		super(operator, false);
		this.operator = operator;
		this.aboveVersion = aboveVersion;

	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
		super.setOperator(operator);
	}

	@Override
	public boolean isConditionFullfilled() {
		if (operator == null) {
			return true;
		}
		return !operator.getCompatibilityLevel().isAtMost(aboveVersion);
	}

	@Override
	public void getDefinitionAsXML(Element element) {
		XMLTools.addTag(element, ELEMENT_VERSION, aboveVersion.getLongVersion());
	}

}
