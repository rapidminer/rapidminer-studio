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
package com.rapidminer.gui.tools.autocomplete;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;


/**
 * Addition which allows a JComboBox to auto fills itself if the entered string is prefix of any
 * combo box item.
 *
 * @author Marco Boeck, Sebastian Land
 *
 */
public class AutoCompleteComboBoxAddition {

	/** the document listener for the combo box editor */
	private final AutoCompletionDocumentListener<?> docListener;

	/**
	 * Adds an auto completion feature to the given JComboBox. Will set
	 * {@link JComboBox#setEditable(boolean)} to true. When the user enters one or more characters,
	 * it will automatically fill in the first match where the characters are a prefix of an item
	 * from the {@link ComboBoxModel}. As soon as the constructor is called
	 *
	 * @param box
	 *            the JComboBox which should get the auto completion feature
	 */
	public AutoCompleteComboBoxAddition(JComboBox<?> box) {
		docListener = new AutoCompletionDocumentListener<>(box);
	}

	/**
	 * Sets the auto-fill feature to case sensitive.
	 *
	 * @param caseSensitive
	 *            If set to true, matching is case sensitive; false otherwise
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.docListener.setCaseSensitive(caseSensitive);
	}

}
