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
package com.rapidminer.tools.expression;

import com.rapidminer.tools.Ontology;


/**
 * Enum for the type of an {@link Expression}. Knows the {@link Ontology#ATTRIBUTE_VALUE_TYPE}
 * associated to an ExpressionType and the other way around.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public enum ExpressionType {
	STRING(Ontology.NOMINAL), DOUBLE(Ontology.REAL), INTEGER(Ontology.INTEGER), BOOLEAN(Ontology.BINOMINAL), DATE(
			Ontology.DATE_TIME);	// TEXT(Ontology.STRING);

	private int attributeType;

	private ExpressionType(int attributeType) {
		this.attributeType = attributeType;
	}

	/**
	 * @return the associated {@link Ontology#ATTRIBUTE_VALUE_TYPE}
	 */
	public int getAttributeType() {
		return attributeType;
	}

	/**
	 * Returns the {@link ExpressionType} associated to the attributeType.
	 *
	 * @param attributeType
	 *            the {@link Ontology#ATTRIBUTE_VALUE_TYPE}
	 * @return the expression type associated to the attributeType
	 */
	public static ExpressionType getExpressionType(int attributeType) {
		switch (attributeType) {
			case Ontology.DATE:
			case Ontology.DATE_TIME:
			case Ontology.TIME:
				return DATE;
			case Ontology.INTEGER:
				return INTEGER;
			case Ontology.REAL:
			case Ontology.NUMERICAL:
				return DOUBLE;
			default:
				return STRING;
		}
	}
}
