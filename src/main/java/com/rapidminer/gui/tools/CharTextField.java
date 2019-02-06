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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTextField;
import javax.swing.event.ChangeListener;


/**
 * A text field which allows to input (only) a single character.
 * 
 * @author Tobias Malbrecht
 */
public class CharTextField extends JTextField {

	private static final long serialVersionUID = 2226618111016685226L;

	public CharTextField() {
		super();
		addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				setText(getText());
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				selectAll();
			}
		});
		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {

			}
		});
	}

	public CharTextField(char character) {
		this();
		setCharacter(character);
	}

	@Override
	public void setText(String text) {
		if (text.length() > 0) {
			super.setText(text.substring(text.length() - 1));
		}
		selectAll();
	}

	public void setCharacter(char character) {
		super.setText(String.valueOf(character));
		selectAll();
	}

	public char getCharacter() {
		return getText().charAt(0);
	}

	public boolean isSet() {
		return getText().length() > 0;
	}

	public void addChangeListener(ChangeListener l) {

	}
}
