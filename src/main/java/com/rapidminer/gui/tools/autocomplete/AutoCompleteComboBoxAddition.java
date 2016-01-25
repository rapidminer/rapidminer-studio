/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;


/**
 * Addition which allows a JComboBox to auto fills itself if the entered string is prefix of any
 * combo box item.
 *
 * @author Marco Boeck, Sebastian Land
 *
 */
public class AutoCompleteComboBoxAddition {

	private final class AutoCompletionDocumentListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) {
			try {
				if (!allowAutoFill) {
					// see #focusGained() down below
					return;
				}

				Vector<String> vectorOfStrings = new Vector<String>();
				for (int i = 0; i < comboBox.getModel().getSize(); i++) {
					vectorOfStrings.add(String.valueOf(comboBox.getModel().getElementAt(i)));
				}
				Document document = e.getDocument();
				final String documentText = document.getText(0, document.getLength());
				final String result = checkForMatch(documentText, vectorOfStrings, caseSensitive);
				final JTextField editorComponent = (JTextField) comboBox.getEditor().getEditorComponent();
				// reset the comboBox selection if there is no match
				if (result == null) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							comboBox.getModel().setSelectedItem(documentText);
						}
					});
					return;
				}
				final int startSelect = document.getLength();
				final int endSelect = result.length();

				if (startSelect == e.getOffset() + e.getLength()) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							comboBox.getModel().setSelectedItem(result);
							editorComponent.getDocument().removeDocumentListener(docListener);
							editorComponent.setText(result);
							editorComponent.getDocument().addDocumentListener(docListener);
							editorComponent.setCaretPosition(startSelect);
							editorComponent.setSelectionStart(startSelect);
							editorComponent.setSelectionEnd(endSelect);
						}
					});
				}
			} catch (BadLocationException e1) {
			}
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			// not needed, only match on new input
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			// never fired for Document
		}
	}

	private final class AutoCompletionComboBoxEditor extends BasicComboBoxEditor {

		@Override
		public void setItem(Object anObject) {
			((JTextField) getEditorComponent()).getDocument().removeDocumentListener(docListener);
			super.setItem(anObject);
			((JTextField) getEditorComponent()).getDocument().addDocumentListener(docListener);
		}
	}

	/** only set this to true after the first time we gained the focus */
	private boolean allowAutoFill;

	/** if set to true, matching will be case sensitive; false otherwise */
	private boolean caseSensitive;

	/** the document listener for the combo box editor */
	private final DocumentListener docListener;

	/** the JComboBox to which it is attached */
	private final JComboBox comboBox;
	private final BasicComboBoxEditor comboBoxEditor;

	/**
	 * Adds an auto completion feature to the given JComboBox. Will set
	 * {@link JComboBox#setEditable(boolean)} to true. When the user enters one or more characters,
	 * it will automatically fill in the first match where the characters are a prefix of an item
	 * from the {@link ComboBoxModel}. As soon as the constructor is called
	 *
	 * @param box
	 *            the JComboBox which should get the auto completion feature
	 */
	public AutoCompleteComboBoxAddition(JComboBox box) {
		comboBox = box;

		caseSensitive = false;
		allowAutoFill = false;
		comboBox.setEditable(true);
		comboBoxEditor = new AutoCompletionComboBoxEditor();
		comboBox.setEditor(comboBoxEditor);
		docListener = new AutoCompletionDocumentListener();
		((JTextField) comboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(docListener);

		// workaround for java bug #6433257
		comboBoxEditor.getEditorComponent().addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				// if we did not prevent this, it would auto fill each time the combo box is
				// re-created after a
				// custom value has been entered,
				// therefore overwriting any value which happens to be a prefix of something with
				// the first match.
				// this happens due to some RapidLookComboBoxEditor mechanics.
				allowAutoFill = true;

				// needed because otherwise the border will not update itself
				comboBox.repaint();
			}

			@Override
			public void focusLost(FocusEvent e) {
				allowAutoFill = false;
				if (!e.isTemporary()) {
					final JTextField editorComponent = (JTextField) comboBox.getEditor().getEditorComponent();
					editorComponent.setCaretPosition(editorComponent.getCaretPosition());
				}
			}

		});
	}

	/**
	 * Sets the auto-fill feature to case sensitive.
	 *
	 * @param caseSensitive
	 *            If set to true, matching is case sensitive; false otherwise
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Returns the first string which starts with the given String.
	 *
	 * @param givenString
	 *            the result must start with this string
	 * @param collectionOfStrings
	 *            the collection of strings to match against the given string
	 * @param caseSensitive
	 * @see {@link #setCaseSensitive(boolean)}
	 * @return the first match or {@code null} if no match is found
	 */
	private String checkForMatch(String givenString, Collection<String> collectionOfStrings, boolean caseSensitive) {
		if (givenString == null || collectionOfStrings == null) {
			return null;
		}
		String returnString = null;
		Collections.sort(new ArrayList<String>(collectionOfStrings));
		for (String vectorString : collectionOfStrings) {
			if (vectorString == null) {
				continue;
			}
			if (caseSensitive) {
				if (vectorString.startsWith(givenString)) {
					returnString = vectorString;
					break;
				}
			} else {
				if (vectorString.toLowerCase(Locale.ENGLISH).startsWith(givenString.toLowerCase(Locale.ENGLISH))) {
					returnString = vectorString;
					break;
				}
			}
		}
		return returnString;
	}

}
