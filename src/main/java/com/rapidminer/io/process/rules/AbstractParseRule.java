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

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 * This superclass of all parse rules retrieves the operatorType affected by this rule. Subclasses
 * might access them by using the operatorTypeName.
 * 
 * @author Sebastian Land
 * 
 */
public abstract class AbstractParseRule implements ParseRule {

	private static final String APPLIES_BEFORE_VERSION = "appliesBefore";
	private static final String APPLIES_SINCE_VERSION = "appliesSince";
	private static final String APPLIES_BEFORE_VERSION_DEFAULT = "5.0.000";
	private static final String APPLIES_SINCE_VERSION_DEFAULT = "1.0.000";

	protected String operatorTypeName;
	protected VersionNumber appliesBefore;
	protected VersionNumber appliesSince;

	public AbstractParseRule(String operatorTypeName, Element element) throws XMLException {
		this.operatorTypeName = operatorTypeName;
		String beforeVersion = element.getAttribute(APPLIES_BEFORE_VERSION);
		String afterVersion = element.getAttribute(APPLIES_SINCE_VERSION);
		if (beforeVersion.isEmpty()) {
			beforeVersion = APPLIES_BEFORE_VERSION_DEFAULT;
		}
		if (afterVersion.isEmpty()) {
			afterVersion = APPLIES_SINCE_VERSION_DEFAULT;
		}
		this.appliesBefore = new VersionNumber(beforeVersion);
		this.appliesSince = new VersionNumber(afterVersion);
	}

	@Override
	public String apply(Operator operator, VersionNumber processVersion, XMLImporter importer) {
		if (operator.getOperatorDescription().getKey().equals(operatorTypeName)) {
			if (processVersion == null || processVersion.compareTo(appliesSince) >= 0
					&& processVersion.compareTo(appliesBefore) < 0) {
				return apply(operator, operatorTypeName, importer);
			}
		}
		return null;
	}

	protected abstract String apply(Operator operator, String operatorTypeName, XMLImporter importer);
}
