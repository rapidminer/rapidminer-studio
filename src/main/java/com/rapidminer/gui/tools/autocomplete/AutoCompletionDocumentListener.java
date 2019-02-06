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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.JTextComponent;

import com.rapidminer.gui.tools.SwingTools;


/**
 * Listener which allows a JComboBox to auto fills itself if the entered string is prefix of any
 * combo box item.
 *
 * @author Marco Boeck, Sebastian Land, Jonas Wilms-Pfau
 * @since 8.1.2
 */
class AutoCompletionDocumentListener<E> implements DocumentListener {

	/** Delay in milliseconds for the autocomplete to begin */
	private static final int AUTOCOMPLETE_DELAY_MS = 250;

	/** Delay in milliseconds for the autosave to begin */
	private static final int AUTOSAVE_DELAY_MS = 1100;

	/** Property names of JComboBox components */
	private static final String MODEL = "model";
	private static final String EDITOR = "editor";

	/** The comboBox to autocomplete */
	private final JComboBox<E> comboBox;

	/** The input field of the comboBox editor */
	private volatile JTextComponent comboBoxInput;

	/** The current underlying comboBoxModel */
	private volatile ComboBoxModel<E> comboBoxModel;

	/** If set to true, matching will be case sensitive; false otherwise */
	private volatile boolean caseSensitive;

	/** If this is true the autocomplete updated the value itself */
	private volatile boolean selfUpdate;

	/** Only set this to true after the first time we gained the focus */
	private volatile boolean hasFocus;

	/** Indicate if events should be ignored since it's loading in the moment */
	private volatile boolean isLoading;

	/** Triggers autosave after a specific time */
	private final SuccessiveExecutionTimer autoSave = new SuccessiveExecutionTimer(AUTOSAVE_DELAY_MS, new SaveRunnable());

	/** Triggers autocomplete after a specific time */
	private final SuccessiveExecutionTimer autoComplete = new SuccessiveExecutionTimer(AUTOCOMPLETE_DELAY_MS, new MakeSuggestionRunnable());

	/** Updates the hasFocus property and fires the ActionEvents on focus lost */
	private final FocusListener focusChangeListener = new FocusAdapter() {

		@Override
		public void focusGained(FocusEvent e) {
			/* if we did not prevent this, it would auto fill each time the combo box is
			 * re-created after a custom value has been entered, therefore overwriting any value
			 * which happens to be a prefix of something with the first match.
			 * This happens due to some RapidLookComboBoxEditor mechanics. */
			hasFocus = true;

			// needed because otherwise the border will not update itself
			comboBox.repaint();
		}

		@Override
		public void focusLost(FocusEvent e) {
			hasFocus = false;
			autoSave.runNow();
			if (e.isTemporary()) {
				comboBoxInput.setCaretPosition(comboBoxInput.getCaretPosition());
			}
		}
	};

	/** Allows the use of the enter key to save the current comboBox value */
	private final KeyListener updateOnEnterKey = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (!comboBox.isPopupVisible() && e.getKeyCode() == KeyEvent.VK_ENTER) {
				comboBox.setSelectedItem(comboBoxInput.getText());
				comboBoxInput.setCaretPosition(comboBoxInput.getText().length());
			}
		}
	};

	AutoCompletionDocumentListener(JComboBox<E> box) {
		if (box == null) {
			throw new IllegalArgumentException("comboBox must not be null");
		}
		comboBox = box;
		comboBox.setEditable(true);
		if (comboBox.getEditor() == null) {
			comboBox.setEditor(new BasicComboBoxEditor());
		}
		//Only do this once
		comboBox.setEditor(new ComboBoxEditorWrapper(comboBox.getEditor()) {
			@Override
			public void setItem(Object anObject) {
				isLoading = true;
				super.setItem(anObject);
				isLoading = false;
			}
		});
		if (comboBox.getModel() == null) {
			comboBox.setModel(new DefaultComboBoxModel<>());
		}
		comboBox.addPropertyChangeListener(MODEL, this::initComboBoxModel);
		comboBox.addPropertyChangeListener(EDITOR, this::initComboBoxEditor);
		initComboBoxModel();
		initComboBoxEditor();
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

	@Override
	public void insertUpdate(DocumentEvent e) {
		if (!hasFocus || selfUpdate || isLoading) {
			return;
		}
		autoSave.restart();
		autoComplete.restart();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		autoSave.restart();
		autoComplete.stop();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		// never fired for Document
	}

	/**
	 * Initializes the ComboBoxInput variable
	 *
	 * @param ignored not used
	 */
	private void initComboBoxEditor(Object... ignored) {
		ComboBoxEditor editor = comboBox.getEditor();
		if (editor == null) {
			return;
		}
		if (editor.getEditorComponent() instanceof JTextComponent) {
			if (comboBoxInput != null) {
				//Clear old listener
				comboBoxInput.getDocument().removeDocumentListener(this);
				comboBoxInput.removeFocusListener(focusChangeListener);
				comboBoxInput.removeKeyListener(updateOnEnterKey);
			}
			comboBoxInput = ((JTextComponent) editor.getEditorComponent());
			comboBoxInput.getDocument().addDocumentListener(this);
			// workaround for java bug #6433257
			comboBoxInput.addFocusListener(focusChangeListener);
			// allow enter to use current value
			comboBoxInput.addKeyListener(updateOnEnterKey);
		}
	}

	/**
	 * Initializes the comboBoxModel and comboBoxModelIterable variable
	 *
	 * @param ignored not used
	 */
	private void initComboBoxModel(Object... ignored) {
		if (comboBox.getModel() != null) {
			comboBoxModel = comboBox.getModel();
		}
	}

	/**
	 * Saves the current value
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 8.1.2
	 */
	private class SaveRunnable implements Runnable {

		/**
		 * Reference to the last saved value
		 */
		private final AtomicReference<String> savedValue = new AtomicReference<>();

		@Override
		public void run() {
			String newValue = comboBoxInput.getText();
			BooleanSupplier sameValue = () -> String.valueOf(comboBoxModel.getSelectedItem()).equals(newValue);
			BooleanSupplier alreadyStored = () -> Objects.equals(newValue, savedValue.getAndSet(newValue));
			if (comboBoxInput.hasFocus() && !sameValue.getAsBoolean() && !alreadyStored.getAsBoolean()) {
				final int start = comboBoxInput.getSelectionStart();
				final int end = comboBoxInput.getSelectionEnd();
				SwingTools.invokeAndWait(() -> {
					comboBox.setSelectedItem(comboBoxInput.getText());
					comboBoxInput.setCaretPosition(end);
					comboBoxInput.moveCaretPosition(start);
				});
			}
		}
	}

	/**
	 * Tries to autocomplete the currently entered letters
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 8.1.2
	 */
	private class MakeSuggestionRunnable implements Runnable {

		@Override
		public void run() {
			//Should be thread-safe thanks to {@link AbstractDocument}
			final E result = checkForMatch(comboBoxInput.getText());

			// abort if there is no match
			if (result == null || result.equals(comboBoxInput.getText())) {
				return;
			}

			SwingTools.invokeAndWait(() -> {
				final String userInput = comboBoxInput.getText();
				final String resultString = result.toString();
				//last check if user typed in the meantime
				if (!result.equals(userInput) && startsWith(resultString, userInput)) {
					selfUpdate = true;
					comboBoxInput.setText(resultString);
					selfUpdate = false;
					comboBoxInput.setCaretPosition(resultString.length());
					comboBoxInput.moveCaretPosition(userInput.length());
				}
			});
		}

		/**
		 * Returns the first string which starts with the given String.
		 *
		 * @param givenString
		 * 		the result must start with this string
		 * @return the first match or {@code null} if no match is found
		 */
		private E checkForMatch(String givenString) {
			if (givenString == null || givenString.isEmpty()) {
				return null;
			}
			for (int i = 0; i < comboBoxModel.getSize(); i++) {
				E currentValue = comboBoxModel.getElementAt(i);
				if (currentValue != null) {
					String currentString = String.valueOf(currentValue);
					if (startsWith(currentString, givenString)) {
						return currentValue;
					}
				}
			}
			return null;
		}

		/**
		 * Check if fullString starts with prefix
		 *
		 * @param fullString
		 * 		the full string
		 * @param prefix
		 * 		the prefix that the fullString should start with
		 * @return true if fullString startsWith prefix
		 */
		private boolean startsWith(String fullString, String prefix) {
			if (fullString != null && prefix != null && prefix.length() <= fullString.length()) {
				return fullString.regionMatches(!caseSensitive, 0, prefix, 0, prefix.length());
			} else {
				return false;
			}
		}
	}

}
