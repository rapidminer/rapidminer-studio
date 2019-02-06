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

import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;


/**
 * This Kit provides the basic Java HTML render capabilities, but extends the Java kit such that
 * each Kit has its own StyleSheet.
 * 
 * @author Sebastian Land
 */
public class ExtendedHTMLEditorKit extends HTMLEditorKit {

	private static final long serialVersionUID = 7021507953344383064L;

	private StyleSheet styleSheet;

	public ExtendedHTMLEditorKit() {
		styleSheet = new StyleSheet();
		try {
			InputStream is = HTMLEditorKit.class.getResourceAsStream(DEFAULT_CSS);
			Reader r = new BufferedReader(new InputStreamReader(is, "ISO-8859-1"));
			styleSheet.loadRules(r, null);
			r.close();
		} catch (Exception e) {
			// LogService.getRoot().log(Level.WARNING, "Cannot install stylesheet: "+e, e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.tools.ExtendedHTMLEditorKit.installing_stylesheet_error", e), e);
			// on error we simply have no styles... the html
			// will look mighty wrong but still function.
		}
	}

	@Override
	public StyleSheet getStyleSheet() {
		return styleSheet;
	}

	@Override
	public void setStyleSheet(StyleSheet s) {
		this.styleSheet = s;
	}
}
