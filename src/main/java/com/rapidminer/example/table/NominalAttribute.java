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

import com.rapidminer.example.NominalStatistics;
import com.rapidminer.example.UnknownStatistics;
import com.rapidminer.tools.Tools;


/**
 * This class holds all information on a single nominal attribute. In addition to the generic
 * attribute fields this class keeps information about the nominal values and the value to index
 * mappings. If one of the methods designed for numerical attributes was invoked a RuntimeException
 * will be thrown.
 *
 * It will be guaranteed that all values are mapped to indices without any missing values. This
 * could, however, be changed in future versions thus operators should not rely on this fact.
 *
 * @author Ingo Mierswa Exp $
 */
public abstract class NominalAttribute extends AbstractAttribute {

	private static final long serialVersionUID = -3830980883541763869L;

	protected NominalAttribute(String name, int valueType) {
		super(name, valueType);
		registerStatistics(new NominalStatistics());
		registerStatistics(new UnknownStatistics());
	}

	protected NominalAttribute(NominalAttribute other) {
		super(other);
	}

	@Override
	public boolean isNominal() {
		return true;
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	/**
	 * Convert negative values directly to missing value ({@link Double#NaN}).
	 */
	@Override
	public void setValue(DataRow row, double value) {
		if (value < 0) {
			value = Double.NaN;
		}
		super.setValue(row, value);
	}

	/**
	 * Returns a string representation and maps the value to a string if type is nominal. The number
	 * of digits is ignored.
	 */
	@Override
	public String getAsString(double value, int digits, boolean quoteNominal) {
		if (Double.isNaN(value)) {
			return "?";
		} else {
			try {
				String result = getMapping().mapIndex((int) value);
				if (quoteNominal) {
					result = Tools.escape(result);
					result = "\"" + result + "\"";
				}
				return result;
			} catch (Throwable e) {
				return "?";
			}
		}
	}
}
