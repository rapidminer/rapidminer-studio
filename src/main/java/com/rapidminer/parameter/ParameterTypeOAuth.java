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

import com.rapidminer.gui.properties.celleditors.value.OAuthValueCellEditor;

import org.w3c.dom.Element;


/**
 * A ParameterType for OAuth. Requires an {@link OAuthMechanism} for the authentication.
 * Authentication workflow is triggered by the {@link OAuthValueCellEditor}.
 * 
 * @author Marcel Michel
 * @since 6.0.003
 * 
 */
public class ParameterTypeOAuth extends ParameterTypePassword {

	private static final long serialVersionUID = -2367046707430250941L;

	protected OAuthMechanism oAuth;

	public ParameterTypeOAuth(String key, String description, OAuthMechanism oAuthMechanism) {
		super(key, description);
		oAuth = oAuthMechanism;
	}

	public ParameterTypeOAuth(String key, String description, boolean optional, OAuthMechanism oAuthMechanism) {
		this(key, description, oAuthMechanism);
		setOptional(optional);
	}

	public ParameterTypeOAuth(String key, String description, boolean optional, boolean expert, OAuthMechanism oAuthMechanism) {
		this(key, description, oAuthMechanism);
		setExpert(expert);
		setOptional(optional);
	}

	@Override
	public boolean isNumerical() {
		return false;
	}

	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {}

	public OAuthMechanism getOAuthMechanism() {
		return oAuth;
	};

	public void setOAuthMechanism(OAuthMechanism oAuthMechanism) {
		oAuth = oAuthMechanism;
	}
}
