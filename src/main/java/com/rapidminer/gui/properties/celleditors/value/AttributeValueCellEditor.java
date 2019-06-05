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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;


/**
 * Value cell editor for attribute type.
 *
 * @author Sebastian Land
 */
public class AttributeValueCellEditor extends DefaultRMCellEditor implements PropertyValueCellEditor {

	private static final String COMBO_BOX_EDITED = "comboBoxEdited";
	private static final String COMBO_BOX_CHANGED = "comboBoxChanged";

	private static final long serialVersionUID = -1889899793777695100L;

	@SuppressWarnings("unchecked")
	public AttributeValueCellEditor(ParameterTypeAttribute type) {
		super(new AttributeComboBox(type));
		// this is the AttributeComboBox from above
		final AttributeComboBox comboBox = (AttributeComboBox) editorComponent;
		final JTextComponent textField = (JTextComponent) comboBox.getEditor().getEditorComponent();

		comboBox.removeItemListener(this.delegate);
		comboBox.setEditable(true);
		comboBox.removeActionListener(delegate); // this is important since we are replacing the
		// original delegate

		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = -5592150438626222295L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					comboBox.setSelectedItem(null);
				} else {
					String value = x.toString();
					super.setValue(x);
					comboBox.setSelectedItem(value);
					if (value != null) {
						textField.setText(value);
					} else {
						textField.setText("");
					}
				}

			}

			@Override
			public Object getCellEditorValue() {
				String selected = textField.getText();
				if (selected != null && selected.trim().length() == 0) {
					selected = null;
				}
				return selected;
			}

			@Override
			public void actionPerformed(ActionEvent event) {
				String actionCommand = event.getActionCommand();
				if (COMBO_BOX_EDITED.equals(actionCommand) || COMBO_BOX_CHANGED.equals(actionCommand)) {
					super.actionPerformed(event);
				}
			}
		};
		comboBox.addActionListener(delegate);

		textField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				// The event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to RapidMiner would
				// not be saved for the same reasons as stated above.
				if (!e.isTemporary()) {
					comboBox.actionPerformed(new ActionEvent(comboBox, 12, COMBO_BOX_EDITED));
				}
			}
		});

		textField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER && !comboBox.isPopupVisible()) {
					comboBox.actionPerformed(new ActionEvent(comboBox, 12, COMBO_BOX_EDITED));
					e.consume();
				}
			}
		});
		comboBox.setRenderer(createAttributeTypeListRenderer((AttributeComboBox.AttributeComboBoxModel) comboBox.getModel()));
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {

	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, hasFocus, row, column);
	}

	/**
	 * Create a list cell renderer for lists that show attributes and their value types.
	 *
	 * @return the renderer, never {@code null}
	 */
	private DefaultListCellRenderer createAttributeTypeListRenderer(AttributeComboBox.AttributeComboBoxModel model) {
		return new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Pair<String, Integer> valueTypePair = index >= 0 ? model.getAttributePairs().get(index) : null;
				if (valueTypePair != null) {
					Integer type = valueTypePair.getSecond();
					if (type != null) {
						Icon icon;
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL)) {
							icon = AttributeGuiTools.NUMERICAL_COLUMN_ICON;
						} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NOMINAL)) {
							icon = AttributeGuiTools.NOMINAL_COLUMN_ICON;
						} else {
							icon = AttributeGuiTools.DATE_COLUMN_ICON;
						}
						label.setIcon(icon);
					}
				}
				return label;
			}
		};
	}
}
