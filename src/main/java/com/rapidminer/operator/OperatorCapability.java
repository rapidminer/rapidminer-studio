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
package com.rapidminer.operator;

/**
 * The possible capabilities for all learners.
 * 
 * @author Julien Nioche, Ingo Mierswa
 */
public enum OperatorCapability {

	POLYNOMINAL_ATTRIBUTES("polynominal attributes"), BINOMINAL_ATTRIBUTES("binominal attributes"), NUMERICAL_ATTRIBUTES(
			"numerical attributes"), POLYNOMINAL_LABEL("polynominal label"), BINOMINAL_LABEL("binominal label"), NUMERICAL_LABEL(
			"numerical label"), ONE_CLASS_LABEL("one class label"), NO_LABEL("unlabeled"), UPDATABLE("updatable"), WEIGHTED_EXAMPLES(
			"weighted examples"), FORMULA_PROVIDER("formula provider"), MISSING_VALUES("missing values");

	private String description;

	private OperatorCapability(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}

}
