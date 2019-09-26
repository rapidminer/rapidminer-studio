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
package com.rapidminer.gui.properties.tablepanel.cells.implementations;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldDefault;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldNumerical;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.tools.I18N;


/**
 * This class contains some helper methods for the cell type implementations.
 *
 * @author Marco Boeck
 *
 */
public final class CellTypeImplHelper {

	/** Maximal number of allowed radio buttons in dropdown. Without threshold the UI can freeze. */
	private static final int MAX_RADIO_BUTTONS = 100;

	/**
	 * Adds content assist to the given field. Does not validate the model, so make sure this call
	 * works!
	 *
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 * @param field
	 * @param button
	 * @param cellClass
	 */
	static void addContentAssist(final TablePanelModel model, final int rowIndex, final int columnIndex,
			final JFormattedTextField field, final JButton button, final Class<? extends CellType> cellClass) {

		List<String> valuesList = model.getPossibleValuesForCellOrNull(rowIndex, columnIndex);
		if (valuesList == null) {
			valuesList = Collections.<String> emptyList();
		}
		final boolean multipleValuesAllowed = model.canCellHaveMultipleValues(rowIndex, columnIndex);
		final List<String> possibleValues = valuesList;

		// add ctrl+space shortcut for content assist
		field.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"contentAssistAction");
		field.getActionMap().put("contentAssistAction", new AbstractAction() {

			private static final long serialVersionUID = 7930480602861798587L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				button.doClick();
			}
		});

		// sort content assist values if they are for a string textfield
		if (CellTypeTextFieldDefault.class.isAssignableFrom(cellClass)) {
			Collections.sort(possibleValues);
		}

		// add mouse listener to show content assist popup
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				showContentAssistPopup();
			}

			/**
			 * Shows the content assist popup.
			 *
			 */
			private void showContentAssistPopup() {
				final ScrollableJPopupMenu popupMenu = createContentAssistPopup(field, possibleValues, multipleValuesAllowed,
						cellClass);

				popupMenu.show(field, field.getX(), field.getY() + field.getHeight());
				popupMenu.requestFocusInWindow();
			}

			/**
			 * Creates the content assist popup.
			 *
			 * @param field
			 * @param possibleValues
			 * @param multipleValuesAllowed
			 * @param cellClass
			 */
			private ScrollableJPopupMenu createContentAssistPopup(final JFormattedTextField field,
					final List<String> possibleValues, final boolean multipleValuesAllowed,
					final Class<? extends CellType> cellClass) {
				final ScrollableJPopupMenu popupMenu = new ScrollableJPopupMenu(ScrollableJPopupMenu.SIZE_SMALL);

				// customize content assist look
				popupMenu.setBackground(Color.WHITE);
				popupMenu.setCustomWidth(field.getWidth());

				// no values available, so show user there is nothing
				if (possibleValues == null || possibleValues.size() <= 0) {
					JMenuItem emptyItem = new JMenuItem(
							I18N.getMessage(I18N.getGUIBundle(), "gui.label.table_panel.no_content_assist_items.title"));
					emptyItem.setToolTipText(
							I18N.getMessage(I18N.getGUIBundle(), "gui.label.table_panel.no_content_assist_items.tip"));
					emptyItem.setOpaque(false);
					popupMenu.add(emptyItem);
					return popupMenu;
				}

				// either add checkboxes or radiobuttons depending on whether multiple values are
				// allowed
				if (multipleValuesAllowed) {
					fillMultipleValuesSelectionPopup(field, possibleValues, popupMenu, cellClass);
				} else {
					fillSingleValueSelectionPopup(field, possibleValues, popupMenu, cellClass);
				}

				return popupMenu;
			}

			/**
			 * Fills the given popup menu for single value selection.
			 *
			 * @param field
			 * @param possibleValues
			 * @param popupMenu
			 * @param cellClass
			 */
			private void fillSingleValueSelectionPopup(final JFormattedTextField field, final List<String> possibleValues,
					final ScrollableJPopupMenu popupMenu, final Class<? extends CellType> cellClass) {
				ButtonGroup group = new ButtonGroup();
				String existingValue = field.getText();
				int count = 0;
				for (String item : possibleValues) {
					final JRadioButton radioButton = new JRadioButton(item);
					radioButton.setOpaque(false);

					// pre-select the corresponding radiobutton for the text in the field
					if (item.split(" ")[0].equals(existingValue)) {
						radioButton.setSelected(true);
					}

					// on click set selected item to textfield
					radioButton.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent e1) {
							// radio button, only one selection allowed anyway
							String text = radioButton.getText();
							// numerical values may have an annotation behind the value, split after
							// " " and just add
							// the first (aka number) value if applicable
							if (CellTypeTextFieldNumerical.class.isAssignableFrom(cellClass)) {
								text = text.split(" ")[0];
							}
							field.setText(text);
						}
					});

					group.add(radioButton);
					popupMenu.add(radioButton);
					count++;
					if(count > MAX_RADIO_BUTTONS){
						break;
					}
				}
			}

			/**
			 * Fills the given popup menu for multiple value selection.
			 *
			 * @param field
			 * @param possibleValues
			 * @param popupMenu
			 * @param cellClass
			 */
			private void fillMultipleValuesSelectionPopup(final JFormattedTextField field, final List<String> possibleValues,
					final ScrollableJPopupMenu popupMenu, final Class<? extends CellType> cellClass) {
				String encodedValue = field.getText();
				List<String> currentValuesList = model.convertEncodedStringValueToList(encodedValue);

				// iterate over all possible items, create checkbox for each
				for (String item : possibleValues) {
					final JCheckBox checkbox = new JCheckBox(item);
					checkbox.setOpaque(false);

					// pre-select the corresponding checkboxes
					if (currentValuesList.contains(item)) {
						checkbox.setSelected(true);
					}

					// on click set currently selected checkbox items to textfield
					checkbox.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent e1) {
							List<String> selectedItems = new LinkedList<>();
							for (Component comp : popupMenu.getComponentsInsideScrollpane()) {
								if (comp instanceof JCheckBox) {
									JCheckBox checkbox = (JCheckBox) comp;
									// add all selected checkboxes to the selected items list
									if (checkbox.isSelected()) {
										selectedItems.add(checkbox.getText());
									}
								}
							}

							// set the new text according to selected items
							field.setText(model.encodeListOfStringsToValue(selectedItems));
						}
					});

					popupMenu.add(checkbox);
				}
			}

		});
	}

	/**
	 * Creates a {@link JFormattedTextField} for the specified cell. Does not validate the model, so
	 * make sure this call works!
	 *
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public static JFormattedTextField createFormattedTextField(final TablePanelModel model, final int rowIndex,
			final int columnIndex) {
		final JFormattedTextField field = new JFormattedTextField();
		field.setToolTipText(model.getHelptextAt(rowIndex, columnIndex));

		// either add document listener (editable) or disable editing (not editable)
		if (model.isCellEditable(rowIndex, columnIndex)) {
			field.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void removeUpdate(final DocumentEvent e) {
					handleChange(e);
				}

				@Override
				public void insertUpdate(final DocumentEvent e) {
					handleChange(e);
				}

				@Override
				public void changedUpdate(final DocumentEvent e) {
					handleChange(e);
				}

				private void handleChange(final DocumentEvent e) {
					try {
						String newValue = e.getDocument().getText(0, e.getDocument().getLength());
						model.setValueAt(newValue, rowIndex, columnIndex);
					} catch (BadLocationException e1) {
					} // should not happen, if it does ignore it
				}
			});
		} else {
			field.setEnabled(false);
		}

		return field;
	}

}
