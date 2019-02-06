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
package com.rapidminer.gui.tools.syntax;

/*
 * TextAreaDefaults.java - Encapsulates default values for various settings Copyright (C) 1999 Slava
 * Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import java.awt.Color;

import javax.swing.JPopupMenu;


/**
 * Encapsulates default settings for a text area. This can be passed to the constructor once the
 * necessary fields have been filled out. The advantage of doing this over calling lots of set()
 * methods after creating the text area is that this method is faster.
 * 
 * Important bug fix: static defaults with 1 (!) document no longer used!
 * 
 * @author Slava Pestov, Tom Bradford, Ingo Mierswa
 */
public class TextAreaDefaults {

	public InputHandler inputHandler;

	public SyntaxDocument document;

	public boolean editable;

	public boolean caretVisible;

	public boolean caretBlinks;

	public boolean blockCaret;

	public int electricScroll;

	public int cols;

	public int rows;

	public SyntaxStyle[] styles;

	public Color caretColor;

	public Color selectionColor;

	public Color lineHighlightColor;

	public boolean lineHighlight;

	public Color bracketHighlightColor;

	public boolean bracketHighlight;

	public Color eolMarkerColor;

	public boolean eolMarkers;

	public boolean paintInvalid;

	public JPopupMenu popup;

	/**
	 * Returns a new TextAreaDefaults object with the default values filled in.
	 */
	public static TextAreaDefaults getDefaults() {
		TextAreaDefaults defaults = new TextAreaDefaults();

		defaults.inputHandler = new DefaultInputHandler();
		defaults.inputHandler.addDefaultKeyBindings();
		defaults.document = new SyntaxDocument();
		defaults.editable = true;

		defaults.caretVisible = true;
		defaults.caretBlinks = true;
		defaults.electricScroll = 3;

		defaults.cols = 80;
		defaults.rows = 25;
		defaults.styles = SyntaxUtilities.getDefaultSyntaxStyles();
		defaults.caretColor = Color.red;
		defaults.selectionColor = new Color(0xccccff);
		defaults.lineHighlightColor = new Color(0xe0e0e0);
		defaults.lineHighlight = true;
		defaults.bracketHighlightColor = Color.black;
		defaults.bracketHighlight = true;
		defaults.eolMarkerColor = new Color(0x009999);
		defaults.eolMarkers = true;
		defaults.paintInvalid = true;

		return defaults;
	}
}
