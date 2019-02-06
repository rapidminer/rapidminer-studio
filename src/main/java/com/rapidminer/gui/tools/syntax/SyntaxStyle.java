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
 * SyntaxStyle.java - A simple text style class Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;

import com.rapidminer.tools.FontTools;


/**
 * A simple text style class. It can specify the color, italic flag, and bold flag of a run of text.
 *
 * @author Slava Pestov, Ingo Mierswa
 */
public class SyntaxStyle {

	/**
	 * Creates a new SyntaxStyle.
	 *
	 * @param color
	 *            The text color
	 * @param italic
	 *            True if the text should be italics
	 * @param bold
	 *            True if the text should be bold
	 */
	public SyntaxStyle(Color color, boolean italic, boolean bold) {
		this.color = color;
		this.italic = italic;
		this.bold = bold;
	}

	/**
	 * Returns the color specified in this style.
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Returns true if no font styles are enabled.
	 */
	public boolean isPlain() {
		return !(bold || italic);
	}

	/**
	 * Returns true if italics is enabled for this style.
	 */
	public boolean isItalic() {
		return italic;
	}

	/**
	 * Returns true if boldface is enabled for this style.
	 */
	public boolean isBold() {
		return bold;
	}

	/**
	 * Returns the specified font, but with the style's bold and italic flags applied.
	 */
	public Font getStyledFont(Font font) {
		if (font == null) {
			throw new NullPointerException("font param must not" + " be null");
		}
		if (font.equals(lastFont)) {
			return lastStyledFont;
		}
		lastFont = font;
		lastStyledFont = FontTools.getFont(font.getFamily(),
				(bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), font.getSize());
		return lastStyledFont;
	}

	/**
	 * Returns the font metrics for the styled font.
	 */
	@SuppressWarnings("deprecation")
	public FontMetrics getFontMetrics(Font font) {
		if (font == null) {
			throw new NullPointerException("font param must not" + " be null");
		}
		if (font.equals(lastFont) && fontMetrics != null) {
			return fontMetrics;
		}
		lastFont = font;
		lastStyledFont = FontTools.getFont(font.getFamily(),
				(bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), font.getSize());
		fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics(lastStyledFont);
		return fontMetrics;
	}

	/**
	 * Sets the foreground color and font of the specified graphics context to that specified in
	 * this style.
	 *
	 * @param gfx
	 *            The graphics context
	 * @param font
	 *            The font to add the styles to
	 */
	public void setGraphicsFlags(Graphics gfx, Font font) {
		Font _font = getStyledFont(font);
		gfx.setFont(_font);
		gfx.setColor(color);
	}

	/**
	 * Returns a string representation of this object.
	 */
	@Override
	public String toString() {
		return getClass().getName() + "[color=" + color + (italic ? ",italic" : "") + (bold ? ",bold" : "") + "]";
	}

	// private members
	private Color color;

	private boolean italic;

	private boolean bold;

	private Font lastFont;

	private Font lastStyledFont;

	private FontMetrics fontMetrics;
}
