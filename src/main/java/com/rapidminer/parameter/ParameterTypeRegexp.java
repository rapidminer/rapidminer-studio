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
package com.rapidminer.parameter;

import java.util.Collection;


/**
 * A parameter type for regular expressions.
 * 
 * @author Tobias Malbrecht
 */
public class ParameterTypeRegexp extends ParameterTypeString {

	/**
	 * General parameter key for this type of parameter that indicates what to replace
	 *
	 * @since 9.3
	 */
	public static final String PARAMETER_REPLACE_WHAT = "replace_what";

	/**
	 * General parameter key for an accompanying string parameter for the parameter with key {@value #PARAMETER_REPLACE_WHAT}
	 * that describes the replacement
	 *
	 * @since 9.3
	 */
	public static final String PARAMETER_REPLACE_BY = "replace_by";

	private static final long serialVersionUID = -4177652183651031337L;

	/** @since 9.3 */
	private ParameterTypeString replacementParameter;

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

	/**
	 * Set the {@link ParameterTypeString} that might be linked to the replacement field
	 * in the {@link com.rapidminer.gui.properties.RegexpPropertyDialog}.
	 *
	 * @param replacementParameter
	 * 		the parameter linked to replacement; can be {@code null}
	 * @since 9.3
	 */
	public void setReplacementParameter(ParameterTypeString replacementParameter) {
		this.replacementParameter = replacementParameter;
	}

	/**
	 * Returns the {@link ParameterTypeString} linked to the replacement field
	 * in the {@link com.rapidminer.gui.properties.RegexpPropertyDialog} if it was set.
	 *
	 * @return the parameter linked to replacement; can be {@code null}
	 * @since 9.3
	 */
	public ParameterTypeString getReplacementParameter(){
		return replacementParameter;
	}

}
