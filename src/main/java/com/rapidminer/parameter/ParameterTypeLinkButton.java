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

import org.w3c.dom.Element;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.gui.tools.components.LinkRemoteButton;


/**
 * A parameter type for either a {@link LinkLocalButton} or a {@link LinkRemoteButton} that executes
 * a given {@link ResourceAction}.
 *
 * @author Gisa Schaefer
 * @since 6.4.0
 */
public class ParameterTypeLinkButton extends ParameterTypeSingle {

	private static final long serialVersionUID = 1L;

	private final ResourceAction action;

	private final boolean isLocalAction;

	/**
	 * Creates a parameter type that shows either a {@link LinkLocalButton} which executes the
	 * action.
	 *
	 * @param key
	 *            the parameter key
	 * @param description
	 *            the parameter description
	 * @param action
	 *            the action to execute when the button is clicked
	 */
	public ParameterTypeLinkButton(String key, String description, ResourceAction action) {
		this(key, description, action, true);
	}

	/**
	 * Creates a parameter type that shows either a {@link LinkLocalButton} or a
	 * {@link LinkRemoteButton} which executes the action.
	 *
	 * @param key
	 *            the parameter key
	 * @param description
	 *            the parameter description
	 * @param action
	 *            the action to execute when the button is clicked
	 * @param isLocalAction
	 *            if {@code true}, a {@link LinkLocalButton} will be used to indicate the action is
	 *            only triggering an in-application action. If {@code false}, a
	 *            {@link LinkRemoteButton} is used to indicate a website will be opened in the
	 *            browser
	 */
	public ParameterTypeLinkButton(String key, String description, ResourceAction action, boolean isLocalAction) {
		super(key, description);
		this.action = action;
		this.isLocalAction = isLocalAction;
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

	/**
	 * Returns whether this action is only an in-application action or opens a browser.
	 *
	 * @return {@code true} for in-application; {@code false} for browser
	 */
	public boolean isLocalAction() {
		return isLocalAction;
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
