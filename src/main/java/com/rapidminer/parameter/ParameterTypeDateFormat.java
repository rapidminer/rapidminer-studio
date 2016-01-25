/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.parameter;

import com.rapidminer.operator.ports.InputPort;


/**
 * A ParameterType for DateFormats.
 *
 * @author Simon Fischer
 *
 */
public class ParameterTypeDateFormat extends ParameterTypeStringCategory {

	private static final long serialVersionUID = 1L;

	private transient InputPort inPort;

	private ParameterTypeAttribute attributeParameter;

	public static final String[] PREDEFINED_DATE_FORMATS = new String[] { "", "yyyy.MM.dd G 'at' HH:mm:ss z",
			"EEE, MMM d, ''yy", "h:mm a", "hh 'o''clock' a, zzzz", "K:mm a, z", "yyyy.MMMMM.dd GGG hh:mm aaa",
			"EEE, d MMM yyyy HH:mm:ss Z", "yyMMddHHmmssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd HH:mm:ss",
			"M/d/yy h:mm a" };

	/**
	 * This is the constructor for date format if no example set meta data is available.
	 */
	public ParameterTypeDateFormat(String key, String description, boolean expert) {
		this(null, key, description, null, expert);
	}

	/**
	 * This is the constructor for date format if no example set meta data is available.
	 */
	public ParameterTypeDateFormat(String key, String description, String defaultValue, boolean expert) {
		this(null, key, description, defaultValue, null, expert);
	}

	/**
	 * This is the constructor for parameter types of operators which transform an example set.
	 */
	public ParameterTypeDateFormat(ParameterTypeAttribute attributeParameter, String key, String description,
			InputPort inPort, boolean expert) {
		this(attributeParameter, key, description, "", inPort, expert);
	}

	/**
	 * This is the constructor for parameter types of operators which transform an example set.
	 */
	public ParameterTypeDateFormat(ParameterTypeAttribute attributeParameter, String key, String description,
			String defaultValue, InputPort inPort, boolean expert) {
		super(key, description, PREDEFINED_DATE_FORMATS, defaultValue, true);
		setExpert(expert);
		this.inPort = inPort;
		this.attributeParameter = attributeParameter;
	}

	public InputPort getInputPort() {
		return inPort;
	}

	/**
	 * This method returns the referenced attribute parameter or null if non exists.
	 */
	public ParameterTypeAttribute getAttributeParameterType() {
		return attributeParameter;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return always {@code false}
	 */
	@Override
	public boolean isSensitive() {
		return false;
	}
}
