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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;


/**
 * This combo box adds auto completion support to the ExtendedJComboBox. Please take into account
 * that only editors with an editor component inheriting from a JTextField are usable.
 *
 * @author Sebastian Land
 **/
public class AutoCompletionComboBox<E> extends ExtendedJComboBox<E> {

	private static final long serialVersionUID = 1L;

	private final class AutoCompletionDocumentListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) {
			try {
				Vector<String> vectorOfStrings = new Vector<String>();
				for (int i = 0; i < getModel().getSize(); i++) {
					vectorOfStrings.add(String.valueOf(getModel().getElementAt(i)));
				}
				Document document = e.getDocument();
				String documentText = document.getText(0, document.getLength());
				final String result = checkForMatch(documentText, vectorOfStrings, caseSensitive);
				final String newString = (result == null) ? documentText : result;
				final int startSelect = document.getLength();
				final int endSelect = newString.length();
				final JTextField editorComponent = (JTextField) getEditor().getEditorComponent();

				if (startSelect == e.getOffset() + e.getLength()) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setSelectedItem(newString);
							editorComponent.getDocument().removeDocumentListener(docListener);
							editorComponent.setText(newString);
							editorComponent.getDocument().addDocumentListener(docListener);
							Caret caret = editorComponent.getCaret();
							caret.setDot(endSelect);
							caret.moveDot(startSelect);
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

	private final class AutoCompletionComboBoxEditor implements ComboBoxEditor {

		private JTextField editorComponent;
		private ComboBoxEditor editor;

		public AutoCompletionComboBoxEditor(ComboBoxEditor editor) {
			if ((editor.getEditorComponent() instanceof JTextField)) {
				this.editor = editor;
				editorComponent = (JTextField) editor.getEditorComponent();
				editorComponent.getDocument().addDocumentListener(docListener);
				editorComponent.addKeyListener(new KeyAdapter() {

					@Override
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_ENTER) {
							setSelectedItem(editorComponent.getText());
							actionPerformed(new ActionEvent(this, 0, "editingStoped"));
							e.consume();
						} else if (e.getKeyCode() == KeyEvent.VK_TAB) {
							if (isPopupVisible()) {
								hidePopup();
							} else {
								showPopup();
							}
							e.consume();
						} else {
							super.keyPressed(e);
						}
					}
				});
			} else {
				throw new IllegalArgumentException("Only JTextField allowed as editor component");
			}
		}

		@Override
		public void setItem(Object anObject) {
			editorComponent.getDocument().removeDocumentListener(docListener);
			editor.setItem(anObject);
			editorComponent.getDocument().addDocumentListener(docListener);
		}

		@Override
		public Object getItem() {
			return editor.getItem();
		}

		@Override
		public Component getEditorComponent() {
			return editorComponent;
		}

		@Override
		public void selectAll() {
			editor.selectAll();
		}

		@Override
		public void addActionListener(ActionListener l) {
			editor.addActionListener(l);
		}

		@Override
		public void removeActionListener(ActionListener l) {
			editor.removeActionListener(l);
		}

	}

	/** if set to true, matching will be case sensitive; false otherwise */
	private boolean caseSensitive;

	/** the document listener for the combo box editor */
	private final DocumentListener docListener = new AutoCompletionDocumentListener();

	public AutoCompletionComboBox(boolean wide, ComboBoxModel<E> model) {
		this(false, -1, -1, wide, model);
	}

	public AutoCompletionComboBox(boolean caseSensitive, int preferredWidth, int preferredHeight, boolean wide,
			ComboBoxModel<E> model) {
		super(preferredWidth, preferredHeight, wide, model);

		this.caseSensitive = caseSensitive;

		setEditable(true);
		setEditor(getEditor());

		addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				setSelectedItem(((JTextField) getEditor().getEditorComponent()).getText());
				actionPerformed(new ActionEvent(this, 0, "editingStopped"));
			}
		});
	}

	@Override
	public void setEditor(ComboBoxEditor anEditor) {
		// check if editor component has changed at all: Otherwise listener already registered
		if (getEditor() == null || anEditor.getEditorComponent() != getEditor().getEditorComponent()) {
			super.setEditor(new AutoCompletionComboBoxEditor(anEditor));
		}
	}

	@Override
	public synchronized void addFocusListener(FocusListener l) {
		// workaround for java bug #6433257
		for (Component c : getComponents()) {
			c.addFocusListener(l);
		}
	}

	@Override
	public void setSelectedItem(Object anObject) {
		super.setSelectedItem(anObject);
		getEditor().setItem(anObject);
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
