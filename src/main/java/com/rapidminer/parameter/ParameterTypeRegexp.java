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

import java.util.Collection;


/**
 * A parameter type for regular expressions.
 * 
 * @author Tobias Malbrecht
 */
public class ParameterTypeRegexp extends ParameterTypeString {

	private static final long serialVersionUID = -4177652183651031337L;

	public ParameterTypeRegexp(final String key, String description) {
		this(key, description, true);
	}

	public ParameterTypeRegexp(final String key, String description, boolean optional) {
		super(key, description, optional);
	}

	/**
	 * This constructer additionally specifies if this parameter type is expert. Please note that
	 * expert parameters are always optional!
	 */
	public ParameterTypeRegexp(final String key, String description, boolean optional, boolean expert) {
		super(key, description, optional || expert);
		setExpert(expert);
	}

	public ParameterTypeRegexp(final String key, String description, String defaultValue) {
		super(key, description, defaultValue);
	}

	public Collection<String> getPreviewList() {
		return null;
	}

}
