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
package com.rapidminer.example.table;

import com.rapidminer.example.MinMaxStatistics;
import com.rapidminer.example.NumericalStatistics;
import com.rapidminer.example.UnknownStatistics;
import com.rapidminer.example.WeightedNumericalStatistics;
import com.rapidminer.tools.Ontology;


/**
 * This class holds all information on a single numerical attribute. In addition to the information
 * of the superclass this is some statistics data like minimum, maximum and average of the values.
 *
 * @author Ingo Mierswa
 */
public class NumericalAttribute extends AbstractAttribute {

	private static final long serialVersionUID = -7425486508057529570L;

	/**
	 * Indicates the default number of fraction digits which is defined by the system property
	 * rapidminer.gui.fractiondigits.numbers.
	 */
	public static final int DEFAULT_NUMBER_OF_DIGITS = -1;

	/** Indicates an unlimited number of fraction digits. */
	public static final int UNLIMITED_NUMBER_OF_DIGITS = -2;

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 */
	protected NumericalAttribute(String name) {
		this(name, Ontology.NUMERICAL);
	}

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 */
	/* pp */ NumericalAttribute(String name, int valueType) {
		super(name, valueType);
		registerStatistics(new NumericalStatistics());
		registerStatistics(new WeightedNumericalStatistics());
		registerStatistics(new MinMaxStatistics());
		registerStatistics(new UnknownStatistics());
	}

	/**
	 * Clone constructor.
	 */
	private NumericalAttribute(NumericalAttribute a) {
		super(a);
	}

	/** Clones this attribute. */
	@Override
	public Object clone() {
		return new NumericalAttribute(this);
	}

	@Override
	public boolean isNominal() {
		return false;
	}

	@Override
	public boolean isNumerical() {
		return true;
	}

	@Override
	public NominalMapping getMapping() {
		throw new UnsupportedOperationException(
				"The method getNominalMapping() is not supported by numerical attributes! You probably tried to execute an operator on a numerical data which is only able to handle nominal values. You could use one of the discretization operators before this application.");
	}

	/** Does nothing. */
	@Override
	public void setMapping(NominalMapping mapping) {}

	/**
	 * Returns a string representation of value. If the numberOfDigits is greater than 0 this number
	 * is used to format the string. Otherwise the value of the system property
	 * rapidminer.gui.fractiondigits.numbers will be used (see {@link com.rapidminer.tools.Tools} in
	 * case of DEFAULT_NUMBER_OF_DIGITS or an unlimited number of digits in case of
	 * UNLIMITED_NUMBER_OF_DIGITS. For the value NaN a &quot;?&quot; will be returned.
	 */
	@Override
	public String getAsString(double value, int numberOfDigits, boolean quoteNominal) {
		if (Double.isNaN(value)) {
			return "?";
		} else {
			switch (numberOfDigits) {
				case UNLIMITED_NUMBER_OF_DIGITS:
					return Double.toString(value);
				case DEFAULT_NUMBER_OF_DIGITS:
					return com.rapidminer.tools.Tools.formatIntegerIfPossible(value);
				default:
					return com.rapidminer.tools.Tools.formatIntegerIfPossible(value, numberOfDigits);
			}
		}
	}

	@Override
	public boolean isDateTime() {
		return false;
	}
}
