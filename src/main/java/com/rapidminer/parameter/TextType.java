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

import java.io.Serializable;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;


/**
 * The possible text types for the {@link ParameterTypeText} type.
 *
 * @author Ingo Mierswa
 */
public enum TextType implements Serializable {

	PLAIN(SyntaxConstants.SYNTAX_STYLE_NONE, false, false), XML(SyntaxConstants.SYNTAX_STYLE_XML, true, false), HTML(
			SyntaxConstants.SYNTAX_STYLE_HTML, true, false), SQL(SyntaxConstants.SYNTAX_STYLE_SQL, false, true), JAVA(
					SyntaxConstants.SYNTAX_STYLE_JAVA, true, true), GROOVY(SyntaxConstants.SYNTAX_STYLE_GROOVY, true, true), PYTHON(
			SyntaxConstants.SYNTAX_STYLE_PYTHON, true, true), R("text/r", true, true);

	private String syntaxIdentifier;
	private boolean isAutoIntending;
	private boolean isBracketMatching;

	TextType(String syntaxIdentifier, boolean isAutoIntending, boolean isBracketMatching) {
		this.syntaxIdentifier = syntaxIdentifier;
		this.isAutoIntending = isAutoIntending;
		this.isBracketMatching = isBracketMatching;
	}

	public String getSyntaxIdentifier() {
		return syntaxIdentifier;
	}

	public boolean isAutoIntending() {
		return isAutoIntending;
	}

	public boolean isBracketMatching() {
		return isBracketMatching;
	}
}
