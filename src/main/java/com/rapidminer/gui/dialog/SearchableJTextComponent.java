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
package com.rapidminer.gui.dialog;

import javax.swing.text.JTextComponent;


/**
 * This is the implementation of a searchable text componend which is based on Swing's
 * JTextComponents.
 * 
 * @author Ingo Mierswa
 */
public class SearchableJTextComponent implements SearchableTextComponent {

	private JTextComponent component;

	public SearchableJTextComponent(JTextComponent component) {
		this.component = component;
	}

	@Override
	public void select(int start, int end) {
		this.component.setCaretPosition(start);
		this.component.moveCaretPosition(end);
	}

	@Override
	public int getCaretPosition() {
		return this.component.getCaretPosition();
	}

	@Override
	public String getText() {
		return this.component.getText();
	}

	@Override
	public void replaceSelection(String newString) {
		this.component.replaceSelection(newString);
	}

	@Override
	public void requestFocus() {
		this.component.requestFocus();
	}

	@Override
	public void setCaretPosition(int pos) {
		this.component.setCaretPosition(pos);
	}

	@Override
	public boolean canHandleCarriageReturn() {
		return false;
	}
}
