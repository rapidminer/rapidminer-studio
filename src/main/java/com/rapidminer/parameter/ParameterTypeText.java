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

import com.rapidminer.gui.properties.TextPropertyDialog;
import com.rapidminer.tools.XMLException;


/**
 * A parameter type for longer texts. In the GUI this might lead to a button opening a text editor.
 *
 * @author Ingo Mierswa
 */
public class ParameterTypeText extends ParameterTypeString {

	private static final long serialVersionUID = 8056689512740292084L;

	private static final String ATTRIBUTE_TEXT_TYPE = "text-type";

	private TextType type = TextType.PLAIN;

	private String templateText;

	public ParameterTypeText(Element element) throws XMLException {
		super(element);

		type = TextType.valueOf(element.getAttribute(ATTRIBUTE_TEXT_TYPE));
	}

	/** Creates a new optional parameter type for longer texts. */
	public ParameterTypeText(String key, String description, TextType type) {
		super(key, description, true);
		setTextType(type);
	}

	/** Creates a new parameter type for longer texts with the given default value. */
	public ParameterTypeText(String key, String description, TextType type, String defaultValue) {
		super(key, description, defaultValue);
		setTextType(type);
	}

	/** Creates a new parameter type for longer texts. */
	public ParameterTypeText(String key, String description, TextType type, boolean optional) {
		super(key, description, optional);
		setTextType(type);
	}

	public void setTextType(TextType type) {
		this.type = type;
	}

	public TextType getTextType() {
		return this.type;
	}

	/**
	 * Sets the template text that is shown in the {@link TextPropertyDialog} if no text is set and
	 * no default value defined.
	 *
	 * @param templateText
	 *            the template text to show in the {@link TextPropertyDialog} if no text is set and
	 *            no default value defined
	 * @since 6.5
	 */
	public void setTemplateText(String templateText) {
		this.templateText = templateText;
	}

	/**
	 * Returns the template text that is shown in the {@link TextPropertyDialog} if no text is set
	 * and no default value defined.
	 *
	 * @return the template text
	 * @since 6.5
	 */
	public String getTemplateText() {
		return templateText;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		super.writeDefinitionToXML(typeElement);

		typeElement.setAttribute(ATTRIBUTE_TEXT_TYPE, type.toString());
	}
}
