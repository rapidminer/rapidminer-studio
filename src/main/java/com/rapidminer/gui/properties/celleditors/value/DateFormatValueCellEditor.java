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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeDateFormat;


/**
 * Value cell editor for date formats. The user can select among a predefined set of values and copy
 * nominal values retrieved from an example set at an input port to the combo box as an editing
 * help.
 *
 * @author Simon Fischer
 */
public class DateFormatValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -1889899793777695100L;
	/** the maximal number of menu items derived from nominal value meta data */
	private static final int MAX_MENU_ITEMS_FROM_VALUESET = 100;
	private JPanel panel;
	private JComboBox<String> formatCombo;
	private AbstractButton selectButton;
	private final ParameterTypeDateFormat type;

	public DateFormatValueCellEditor(ParameterTypeDateFormat type_param) {
		this.type = type_param;
		panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.RELATIVE;
		formatCombo = new JComboBox<>(type.getValues());
		formatCombo.setEditable(true);
		panel.add(formatCombo, c);
		selectButton = new JButton(new ResourceAction(true, "dateformat.select_sample") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				JPopupMenu menu = new JPopupMenu();
				if (type.getInputPort() != null) {
					MetaData md = type.getInputPort().getMetaData();
					if (md instanceof ExampleSetMetaData) {
						ExampleSetMetaData emd = (ExampleSetMetaData) md;
						final ParameterTypeAttribute attributeParameterType = type.getAttributeParameterType();
						if (attributeParameterType != null) {
							String selectedAttributeName = type.getInputPort().getPorts().getOwner().getOperator()
									.getParameters().getParameterOrNull(attributeParameterType.getKey());
							AttributeMetaData selectedAttribute = emd.getAttributeByName(selectedAttributeName);
							if (selectedAttribute != null && selectedAttribute.isNominal()
									&& selectedAttribute.getValueSet() != null) {
								boolean isNotMenuEmpty = false;
								int count = 0;
								for (final String value : selectedAttribute.getValueSet()) {
									menu.add(new JMenuItem(new LoggedAbstractAction(value) {

										private static final long serialVersionUID = 1L;

										@Override
										public void loggedActionPerformed(ActionEvent e) {
											formatCombo.setSelectedItem(value);
										}
									}));
									isNotMenuEmpty = true;
									count++;
									if (count > MAX_MENU_ITEMS_FROM_VALUESET) {
										break;
									}
								}
								if (!isNotMenuEmpty) {
									menu.add(new JMenuItem(new ResourceAction("no_matches_found") {

										private static final long serialVersionUID = 5312694774573705215L;

										@Override
										public void loggedActionPerformed(ActionEvent e) {}
									}));
								}
								menu.show(selectButton, 0, selectButton.getHeight());
							} else if (emd.getAllAttributes() != null) {
								int j = 0;
								boolean isNotMenuEmpty = false;
								for (final AttributeMetaData amd : emd.getAllAttributes()) {
									if (amd.isNominal() && amd.getValueSet() != null) {
										JMenu subMenu = new JMenu(amd.getName());
										menu.add(subMenu);
										int i = 0;
										for (final String value : amd.getValueSet()) {
											subMenu.add(new JMenuItem(new LoggedAbstractAction(value) {

												private static final long serialVersionUID = 1L;

												@Override
												public void loggedActionPerformed(ActionEvent e) {
													formatCombo.setSelectedItem(value);
													if (attributeParameterType != null && type.getInputPort() != null) {
														type.getInputPort()
																.getPorts()
																.getOwner()
																.getOperator()
																.getParameters()
																.setParameter(attributeParameterType.getKey(), amd.getName());
													}
												}
											}));
											i++;
											if (i > 21) {
												break;
											}
										}
										isNotMenuEmpty = true;
										j++;
										if (j > 13) {
											break;
										}
									}
								}
								if (!isNotMenuEmpty) {
									menu.add(new JMenuItem(new ResourceAction("no_matches_found") {

										private static final long serialVersionUID = 5312694774573705015L;

										@Override
										public void loggedActionPerformed(ActionEvent e) {}
									}));
								}
								menu.show(selectButton, 0, selectButton.getHeight());
							}
						}
					}
				}
			}
		});
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		c.insets = new Insets(0, 5, 0, 0);
		selectButton.setText(null);
		panel.add(selectButton, c);

		formatCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}
		});
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
		return panel;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		formatCombo.setSelectedItem(value);
		selectButton.setEnabled(type.getInputPort() != null);
		return panel;
	}

	@Override
	public Object getCellEditorValue() {
		return formatCombo.getSelectedItem();
	}

	@Override
	public void activate() {
		selectButton.doClick();
	}
}
