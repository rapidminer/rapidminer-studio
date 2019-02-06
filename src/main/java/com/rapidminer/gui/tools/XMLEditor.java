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
package com.rapidminer.gui.tools;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.rapidminer.gui.tools.syntax.JEditTextArea;
import com.rapidminer.gui.tools.syntax.TextAreaDefaults;
import com.rapidminer.gui.tools.syntax.XMLTokenMarker;


/**
 * A generic XML editor.
 *
 * @deprecated Use {@link RSyntaxTextArea} with XML highlighting instead
 * @author Ingo Mierswa
 */
@Deprecated
public class XMLEditor extends JEditTextArea {

	private static final long serialVersionUID = 5515907668417632521L;

	public XMLEditor() {
		super(getDefaults());
		setTokenMarker(new XMLTokenMarker());
	}

	private static TextAreaDefaults getDefaults() {
		TextAreaDefaults defaultSettings = TextAreaDefaults.getDefaults();
		defaultSettings.styles = SwingTools.getSyntaxStyles();
		return defaultSettings;
	}
}
