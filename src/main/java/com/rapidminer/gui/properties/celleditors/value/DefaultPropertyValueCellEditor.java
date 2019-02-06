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
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.gui.properties.PropertyTable;
import com.rapidminer.gui.tools.CharTextField;
import com.rapidminer.gui.tools.ExtendedJComboBox;
import com.rapidminer.gui.tools.autocomplete.AutoCompleteComboBoxAddition;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.tools.Tools;


/**
 * Editor for parameter values string, int, double, category, and boolean. This can be used in all
 * {@link PropertyTable}s to show or editing the properties / parameters. For more special parameter
 * types other solutions exist.
 *
 * @see FileValueCellEditor
 * @see ListValueCellEditor
 * @see ColorValueCellEditor
 * @see OperatorValueValueCellEditor
 * @author Ingo Mierswa, Simon Fischer, Nils Woehler
 */
public class DefaultPropertyValueCellEditor extends DefaultRMCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 3594466409311826645L;

	private static final int TEXT_FIELD_WIDTH = 10;

	private boolean useEditorAsRenderer = false;

	private boolean rendersLabel = false;

	public DefaultPropertyValueCellEditor(final ParameterTypeCategory type) {
		super(new ExtendedJComboBox<>(type.getValues()));
		useEditorAsRenderer = true;
		((JComboBox<?>) editorComponent).removeItemListener(this.delegate);
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = -2104662561680969750L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JComboBox<?>) editorComponent).setSelectedIndex(-1);
				} else {
					try {
						Integer index = Integer.valueOf(x.toString());
						super.setValue(index);
						((JComboBox<?>) editorComponent).setSelectedIndex(index);
					} catch (NumberFormatException e) {
						// try to get index from string...
						int index = type.getIndex(x.toString());
						super.setValue(index);
						((JComboBox<?>) editorComponent).setSelectedIndex(index);
					}
				}
			}

			@Override
			public Object getCellEditorValue() {
				return ((JComboBox<?>) editorComponent).getSelectedItem();
			}
		};
		((JComboBox<?>) editorComponent).addItemListener(delegate);
	}

	public DefaultPropertyValueCellEditor(final ParameterTypeStringCategory type) {
		super(new JComboBox<>(type.getValues()));

		if (type.isEditable()) {
			AutoCompleteComboBoxAddition autoCompleteCBA = new AutoCompleteComboBoxAddition((JComboBox<?>) editorComponent);
			autoCompleteCBA.setCaseSensitive(false);
		}

		final JTextComponent textField = (JTextComponent) ((JComboBox<?>) editorComponent).getEditor().getEditorComponent();

		useEditorAsRenderer = true;
		((JComboBox<?>) editorComponent).removeItemListener(this.delegate);
		((JComboBox<?>) editorComponent).setEditable(type.isEditable());
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = -5592150438626222295L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JComboBox<?>) editorComponent).setSelectedItem(null);
				} else {
					String value = x.toString();
					super.setValue(value);
					((JComboBox<?>) editorComponent).setSelectedItem(value);
					if (value != null) {
						textField.setText(value.toString());
					} else {
						textField.setText("");
					}
				}
			}

			@Override
			public Object getCellEditorValue() {
				if (type.isEditable()) {
					String selected = textField.getText();
					if (selected != null && selected.trim().length() == 0) {
						selected = null;
					}
					return selected;
				} else {
					return ((JComboBox<?>) editorComponent).getSelectedItem();
				}
			}
		};
		editorComponent.setToolTipText(type.getDescription());
		((JComboBox<?>) editorComponent).addItemListener(delegate);
	}

	public DefaultPropertyValueCellEditor(final ParameterTypeBoolean type) {
		super(new JCheckBox());
		rendersLabel = true;
		((JCheckBox) editorComponent).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		((JCheckBox) editorComponent).setText(type.getKey().replace('_', ' '));
		if (type.isExpert()) {
			editorComponent.setFont(editorComponent.getFont().deriveFont(Font.ITALIC));
		}
		// editorComponent.setBackground(javax.swing.UIManager.getColor("Table.cellBackground"));
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = 152467444047540403L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JCheckBox) editorComponent).setSelected((Boolean) type.getDefaultValue());
				} else {
					Boolean value = Tools.booleanValue(x.toString(), (Boolean) type.getDefaultValue());
					super.setValue(value);
					((JCheckBox) editorComponent).setSelected(value);
				}
			}

			@Override
			public Object getCellEditorValue() {
				return Boolean.valueOf(
						Tools.booleanValue(Boolean.valueOf(((JCheckBox) editorComponent).isSelected()).toString(),
								(Boolean) type.getDefaultValue())).toString();
			}
		};

		useEditorAsRenderer = true;
	}

	public DefaultPropertyValueCellEditor(final ParameterTypeInt type) {
		super(new JTextField(TEXT_FIELD_WIDTH));
		setClickCountToStart(1);
		editorComponent.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
		((JTextField) editorComponent).setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		// ((JTextField) editorComponent).setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = 152467444047540403L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JTextField) editorComponent).setText(null);
				} else {
					super.setValue(x.toString());
					try {
						if (value != null) {
							Integer value = Integer.valueOf(x.toString());
							((JTextField) editorComponent).setText(value.toString());
						} else {
							Integer defaultValue = (Integer) type.getDefaultValue();
							if (defaultValue != null) {
								((JTextField) editorComponent).setText(type.getDefaultValue().toString());
							} else {
								((JTextField) editorComponent).setText(null);
							}
						}
					} catch (NumberFormatException e) {
						String text = x.toString();
						// try macro...
						if (text.startsWith("%{") && text.endsWith("}")) {
							((JTextField) editorComponent).setText(text);
						} else {
							// no macro --> set to default
							Integer defaultValue = (Integer) type.getDefaultValue();
							if (defaultValue != null) {
								((JTextField) editorComponent).setText(type.getDefaultValue().toString());
							} else {
								((JTextField) editorComponent).setText(null);
							}
						}
					}
				}
			}

			@Override
			public Object getCellEditorValue() {
				String text = ((JTextField) editorComponent).getText();
				try {
					int i = Integer.parseInt(text);
					if (i < type.getMinValue()) {
						i = (int) type.getMinValue();
					}
					if (i > type.getMaxValue()) {
						i = (int) type.getMaxValue();
					}
					((JTextField) editorComponent).setText(Integer.toString(i));
					return Integer.toString(i);
				} catch (NumberFormatException e) {
					// try macro...
					if (text.startsWith("%{") && text.endsWith("}")) {
						return text;
					} else {
						// no macro --> set to default
						Integer defaultValue = (Integer) type.getDefaultValue();
						if (defaultValue != null) {
							((JTextField) editorComponent).setText(type.getDefaultValue().toString());
							return type.getDefaultValue(); // .toString();
						} else {
							((JTextField) editorComponent).setText("");
							// no default --> return null
							return null;
						}
					}
				}
			}
		};

		editorComponent.setToolTipText(type.getDescription() + " (" + type.getRange() + ")");
		useEditorAsRenderer = true;
	}

	public DefaultPropertyValueCellEditor(final ParameterTypeDouble type) {
		super(new JTextField(TEXT_FIELD_WIDTH));
		setClickCountToStart(1);
		editorComponent.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
		((JTextField) editorComponent).setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = 5764937097891322370L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JTextField) editorComponent).setText(null);
				} else {
					super.setValue(x.toString());
					try {
						if (value != null) {
							Double value = Double.valueOf(x.toString());
							((JTextField) editorComponent).setText(value.toString());
						} else {
							Double defaultValue = (Double) type.getDefaultValue();
							if (defaultValue != null) {
								((JTextField) editorComponent).setText(type.getDefaultValue().toString());
							} else {
								((JTextField) editorComponent).setText(null);
							}
						}
					} catch (NumberFormatException e) {
						String text = x.toString();
						// try macro...
						if (text.startsWith("%{") && text.endsWith("}")) {
							((JTextField) editorComponent).setText(text);
						} else {
							// no macro --> set to default
							Double defaultValue = (Double) type.getDefaultValue();
							if (defaultValue != null) {
								((JTextField) editorComponent).setText(type.getDefaultValue().toString());
							} else {
								((JTextField) editorComponent).setText(null);
							}
						}
					}
				}
			}

			@Override
			public Object getCellEditorValue() {
				String text = ((JTextField) editorComponent).getText();
				try {
					double d = Double.parseDouble(text);
					if (d < type.getMinValue()) {
						d = type.getMinValue();
					}
					if (d > type.getMaxValue()) {
						d = type.getMaxValue();
					}
					((JTextField) editorComponent).setText(Double.valueOf(d).toString());
					return Double.valueOf(d).toString();
				} catch (NumberFormatException e) {
					// try macro...
					if (text.startsWith("%{") && text.endsWith("}")) {
						return text;
					} else {
						// no macro --> set to default
						Double defaultValue = (Double) type.getDefaultValue();
						if (defaultValue != null) {
							((JTextField) editorComponent).setText(type.getDefaultValue().toString());
							return type.getDefaultValue(); // .toString();
						} else {
							((JTextField) editorComponent).setText("");
							// no default --> return null
							return null;
						}
					}
				}
			}
		};

		editorComponent.setToolTipText(type.getDescription() + " (" + type.getRange() + ")");
		useEditorAsRenderer = true;
	}

	public DefaultPropertyValueCellEditor(final ParameterTypePassword type) {
		super(new JPasswordField());
		setClickCountToStart(1);
		editorComponent.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = -2736861014783898296L;

			@Override
			public void setValue(Object x) {
				if (x == null) {
					super.setValue(null);
					((JTextField) editorComponent).setText(null);
				} else {
					String value = x.toString();
					super.setValue(value);
					((JTextField) editorComponent).setText(value.toString());
				}
			}

			@Override
			public Object getCellEditorValue() {
				String text = ((JTextField) editorComponent).getText();
				if (text == null || text.length() == 0) {
					if (type.getDefaultValue() != null) {
						return type.getDefaultValue().toString();
					} else {
						return null;
					}
				} else {
					return text.toString();
				}
			}
		};
		useEditorAsRenderer = true;
	}

	public DefaultPropertyValueCellEditor(final ParameterTypeChar type) {
		super(new CharTextField());
		setClickCountToStart(1);
		editorComponent.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
	}

	public DefaultPropertyValueCellEditor(final ParameterType type) {
		super(new JTextField(TEXT_FIELD_WIDTH));
		setClickCountToStart(1);
		editorComponent.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {}

			@Override
			public void focusLost(FocusEvent e) {
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
		this.delegate = new EditorDelegate() {

			private static final long serialVersionUID = -2868203350553070093L;

			@Override
			public void setValue(Object x) {
				super.setValue(x);
				if (x != null) {
					((JTextField) editorComponent).setText(x.toString());
				}
			}

			@Override
			public Object getCellEditorValue() {
				String text = ((JTextField) editorComponent).getText();
				if (text == null || text.length() == 0) {
					if (type.getDefaultValue() == null) {
						return null;
					} else {
						return type.toString(type.getDefaultValue());
					}
				} else {
					return text.toString();
				}
			}
		};
		useEditorAsRenderer = true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Component c;
		if (table == null) {
			c = editorComponent;
			delegate.setValue(value);
			((JComponent) c).setOpaque(!(c instanceof JCheckBox) && !(c instanceof JComboBox)); // otherwise
			// we
			// have
			// a
			// white
			// border
			// around
			// check
			// boxes
		} else {
			c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		}
		return c;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, hasFocus, row, column);
	}

	@Override
	public boolean useEditorAsRenderer() {
		return useEditorAsRenderer;
	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator) {}

	@Override
	public boolean rendersLabel() {
		return rendersLabel;
	}
}
