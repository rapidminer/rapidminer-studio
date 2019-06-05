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
package com.rapidminer.connection.gui.listener;

import java.util.logging.Level;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.rapidminer.tools.LogService;

import javafx.beans.property.Property;


/**
 * Listener that calls the given Consumer every time the documents changes
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class TextChangedDocumentListener implements DocumentListener {

	private final Property<String> textProperty;

	public TextChangedDocumentListener(Property<String> textProperty) {
		this.textProperty = textProperty;
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		updateText(e);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		updateText(e);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		updateText(e);
	}

	private void updateText(DocumentEvent e) {
		Document document = e.getDocument();
		try {
			String newValue = document.getText(0, document.getLength());
			if (!newValue.equals(textProperty.getValue())) {
				textProperty.setValue(newValue);
			}
		} catch (BadLocationException ble) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.gui.model.TextChangedDocumentListener.document_get_text_failed", ble);
		}
	}
}
