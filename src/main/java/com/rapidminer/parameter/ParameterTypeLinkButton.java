/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.parameter;

import org.w3c.dom.Element;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.LinkButton;


/**
 * A parameter type for a {@link LinkButton} that executes a given {@link ResourceAction}.
 *
 * @author Gisa Schaefer
 * @since 6.4.0
 */
public class ParameterTypeLinkButton extends ParameterTypeSingle {

	private static final long serialVersionUID = 1L;

	private final ResourceAction action;

	/**
	 * Creates a parameter type that shows a {@link LinkButton} which executes the action.
	 *
	 * @param key
	 *            the parameter key
	 * @param description
	 *            the parameter description
	 * @param action
	 *            the action to execute when the button is clicked
	 */
	public ParameterTypeLinkButton(String key, String description, ResourceAction action) {
		super(key, description);
		this.action = action;
		setExpert(false);
	}

	/**
	 * Returns the action to execute when the button is clicked.
	 *
	 * @return the action
	 */
	public ResourceAction getAction() {
		return action;
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
	public String getRange() {
		return null;
	}

	@Override
	public void setDefaultValue(Object defaultValue) {
		// do nothing
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		// do nothing
	}

}
