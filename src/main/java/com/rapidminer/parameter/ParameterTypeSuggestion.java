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

import com.rapidminer.tools.XMLException;

import org.w3c.dom.Element;


/**
 *
 * A parameter type providing dynamic suggestions to the user via the provided
 * {@link SuggestionProvider}.
 *
 * @author Nils Woehler
 * @since 6.0.003
 */
public class ParameterTypeSuggestion extends ParameterTypeString {

	private static final long serialVersionUID = 1L;

	private SuggestionProvider<?> provider;

	public ParameterTypeSuggestion(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeSuggestion(String key, String description, SuggestionProvider<?> provider) {
		this(key, description, provider, null, true);
	}

	public ParameterTypeSuggestion(String key, String description, SuggestionProvider<?> provider, boolean optional) {
		this(key, description, provider, null, optional);
	}

	public ParameterTypeSuggestion(String key, String description, SuggestionProvider<?> provider, String defaultValue) {
		this(key, description, provider, defaultValue, true);
	}

	public ParameterTypeSuggestion(String key, String description, SuggestionProvider<?> provider, String defaultValue,
			boolean optional) {
		super(key, description, defaultValue);
		this.provider = provider;
		setOptional(optional);
		setExpert(false);
	}

	public SuggestionProvider<?> getSuggestionProvider() {
		return provider;
	}

}
