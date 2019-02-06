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

import java.util.Date;

import com.rapidminer.example.MinMaxStatistics;
import com.rapidminer.example.UnknownStatistics;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This class holds all information on a single date attribute. In addition to the information of
 * the superclass, this is some statistics data like minimum, maximum and average of the values.
 * 
 * @author Ingo Mierswa
 */
public class DateAttribute extends AbstractAttribute {

	private static final long serialVersionUID = -685655991653799960L;

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 */
	protected DateAttribute(String name) {
		this(name, Ontology.DATE);
	}

	/**
	 * Creates a simple attribute which is not part of a series and does not provide a unit string.
	 */
	protected DateAttribute(String name, int valueType) {
		super(name, valueType);
		registerStatistics(new MinMaxStatistics());
		registerStatistics(new UnknownStatistics());
	}

	/**
	 * Clone constructor.
	 */
	private DateAttribute(DateAttribute a) {
		super(a);
	}

	@Override
	public Object clone() {
		return new DateAttribute(this);
	}

	@Override
	public String getAsString(double value, int digits, boolean quoteNominal) {
		if (Double.isNaN(value)) {
			return "?";
		} else {
			long milliseconds = (long) value;
			String result = null;
			if (getValueType() == Ontology.DATE) {
				result = Tools.formatDate(new Date(milliseconds));
			} else if (getValueType() == Ontology.TIME) {
				result = Tools.formatTime(new Date(milliseconds));
			} else if (getValueType() == Ontology.DATE_TIME) {
				result = Tools.formatDateTime(new Date(milliseconds), "dd/MM/yyyy HH:mm:ss aa zzz");
			}
			if (quoteNominal) {
				result = "\"" + result + "\"";
			}
			return result;
		}
	}

	/** Returns null. */
	@Override
	public NominalMapping getMapping() {
		throw new UnsupportedOperationException(
				"The method getNominalMapping() is not supported by date attributes! You probably tried to execute an operator on a date or time data which is only able to handle nominal values. You could use one of the Date to Nominal operator before this application.");
	}

	/** Returns false. */
	@Override
	public boolean isNominal() {
		return false;
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	/** Do nothing. */
	@Override
	public void setMapping(NominalMapping nominalMapping) {}

	@Override
	public boolean isDateTime() {
		return true;
	}
}
