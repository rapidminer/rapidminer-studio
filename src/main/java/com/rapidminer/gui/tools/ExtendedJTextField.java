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

import java.util.LinkedList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * This extends the usual text field with a model, that will reflect the changes to the content of
 * the text field and offers a listener concept to be informed of the changes.
 * 
 * @author Sebastian Land
 */
public class ExtendedJTextField extends JTextField {

	private static final long serialVersionUID = -3490766980163546570L;

	public static interface TextChangeListener {

		/**
		 * This method will be called, whenever the content of the textfield changes, no matter if
		 * the changed happened by user input or programmatically.
		 */
		public void informTextChanged(String newValue);
	}

	/**
	 * This is the model of an {@link ExtendedJTextField}. It offers to set and get the value and
	 * has a listener concept to be informed of changes to the text content.
	 * 
	 * @author Sebastian Land
	 */
	public static class ExtendedJTextFieldModel {

		private String value;
		private List<TextChangeListener> listeners = new LinkedList<ExtendedJTextField.TextChangeListener>();

		public ExtendedJTextFieldModel() {}

		public ExtendedJTextFieldModel(String value) {
			this.value = value;
		}

		/**
		 * This method must be called, when the model is added to the {@link ExtendedJTextField}.
		 */
		/* pp */void registerOnComponent(final ExtendedJTextField component) {
			component.setText(value);
			// use a document listener instead of a KeyListener to react also on
			// programmatical changes
			component.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(DocumentEvent e) {
					changed(e);
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					changed(e);
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					changed(e);
				}

				private void changed(DocumentEvent e) {
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							setValue(component.getText());
						}
					});
				}
			});
		}

		/**
		 * This sets the value of the text field.
		 */
		public void setValue(String newValue) {
			this.value = newValue;
			informListenersOfChange();
		}

		/**
		 * This method returns the current text stored in the component of this model.
		 */
		public String getValue() {
			return this.value;
		}

		/* Listener Handling */
		private void informListenersOfChange() {
			for (TextChangeListener listener : listeners) {
				listener.informTextChanged(value);
			}
		}

		public void addTextChangeListener(TextChangeListener listener) {
			listeners.add(listener);
		}

		public void removeTextChangeListener(TextChangeListener listener) {
			listeners.remove(listener);
		}
	}

	private ExtendedJTextFieldModel model;

	public ExtendedJTextField() {
		this("");
	}

	public ExtendedJTextField(String value) {
		this.model = new ExtendedJTextFieldModel(value);
		model.registerOnComponent(this);
	}

	public ExtendedJTextField(ExtendedJTextFieldModel model) {
		this.model = model;
		model.registerOnComponent(this);
	}

	/**
	 * Use this method to set a new model to this component.
	 */
	public void setModel(ExtendedJTextFieldModel model) {
		this.model = model;
		model.registerOnComponent(this);
	}

	/**
	 * This method returns the model of this component.
	 */
	public ExtendedJTextFieldModel getModel() {
		return this.model;
	}
}
