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

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;


/**
 * This is a JTextPane that will assign each word a color specified by the getColor method.
 * 
 * @author Sebastian Land
 * 
 */
public abstract class ColoredJTextPane extends JTextPane {

	private static final long serialVersionUID = 1L;

	public ColoredJTextPane() {
		super();
		setDocument(new DefaultStyledDocument() {

			private static final long serialVersionUID = 1L;

			@Override
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				super.insertString(offs, str, a);

				// now color all texts again
				// do coloring of words but ignore keys unless it is word termination
				String text = getText(0, getLength());
				char[] chars = text.toCharArray();
				StyleContext context = StyleContext.getDefaultStyleContext();
				int lastStart = 0;
				boolean charactersSinceLastStart = false;
				for (int i = 0; i < text.length(); i++) {
					if (!Character.isLetter(chars[i])) {
						if (charactersSinceLastStart) {
							AttributeSet attributeSet = context.addAttribute(SimpleAttributeSet.EMPTY,
									StyleConstants.Foreground, getColor(new String(chars, lastStart, i - lastStart)));
							setCharacterAttributes(lastStart, i - lastStart, attributeSet, true);
						}
						lastStart = i + 1;
						charactersSinceLastStart = false;
					} else {
						charactersSinceLastStart = true;
					}
				}
			}
		});

	}

	/**
	 * This must return the color of the given word.
	 */
	protected abstract Color getColor(String word);

}
