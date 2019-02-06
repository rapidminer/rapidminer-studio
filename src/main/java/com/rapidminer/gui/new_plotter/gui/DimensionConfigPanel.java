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
package com.rapidminer.gui.new_plotter.gui;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.ChartConfigurationException;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager;
import com.rapidminer.gui.new_plotter.configuration.DomainConfigManager.Sorting;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.ValueGroupingFactory;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.popup.PopupAction;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class DimensionConfigPanel extends AbstractConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private final PlotDimension dimension;

	// label configuration components
	private ResourceLabel dimensionLabelLabel;
	private JTextField dimensionLabelTextField;
	private JCheckBox automateLabelCheckBox;

	// date format configuration components
	private ResourceLabel dateFormatLabel;
	private JTextField dateFormatTextField;
	private JCheckBox useDefaultDateFormatCheckBox;

	private JCheckBox logarithmicCheckBox;
	private JLabel upperBoundLabel;
	private JSpinner upperBoundSpinner;
	private JLabel lowerBoundLabel;
	private JSpinner lowerBoundSpinner;
	private JLabel logarithmicLabel;
	private JLabel groupingLabel;
	private JComboBox<GroupingType> groupingComboBox;
	private JToggleButton groupingConfigButton;

	private GroupingConfigurationPanel groupingConfigurationPanel;
	private JCheckBox lowerBoundCheckBox;
	private JCheckBox upperBoundCheckBox;
	private JLabel sortingLabel;
	private AbstractButton sortedCheckBox;

	// private JLabel automateLabel;

	public DimensionConfigPanel(PlotDimension dimension, JTree plotConfigurationTree, PlotInstance plotInstance) {
		super(plotInstance);

		this.dimension = dimension;

		groupingConfigurationPanel = new GroupingConfigurationPanel(plotConfigurationTree, plotInstance, dimension);
		addPlotInstanceChangeListener(groupingConfigurationPanel);

		createComponents();
		registerAsPlotConfigurationListener();
		adaptGUI();

	}

	private void createComponents() {

		// // add automated dimension axis label check box
		// {
		// automateLabel = new ResourceLabel("plotter.configuration_dialog.automatic_axis_label");
		//
		// automateLabelCheckBox = new JCheckBox();
		// automateLabel.setLabelFor(automateLabelCheckBox);
		// automateLabelCheckBox.addActionListener(new ActionListener() {
		//
		// @Override
		// public void loggedActionPerformed(ActionEvent e) {
		// DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
		// if (dimensionConfig != null) {
		// dimensionConfig.setAutoNaming(automateLabelCheckBox.isSelected());
		// }
		// }
		//
		// });
		//
		// addTwoComponentRow(this, automateLabel, automateLabelCheckBox);
		// }

		// create dimension axis label row
		{
			dimensionLabelLabel = new ResourceLabel("plotter.configuration_dialog.dimension_axis_title");

			// create x-axis legend text field
			dimensionLabelTextField = new JTextField();
			dimensionLabelLabel.setLabelFor(dimensionLabelTextField);
			dimensionLabelTextField.addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {
					return; // Nothing to be done here
				}

				@Override
				public void keyReleased(KeyEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						String oldLabel = dimensionConfig.getLabel();
						String newLabel = dimensionLabelTextField.getText();
						if (oldLabel != null && !oldLabel.equals(newLabel) || oldLabel == null && newLabel.length() > 0) {
							if (newLabel.length() > 0) {
								dimensionConfig.setLabel(newLabel);
							} else {
								dimensionConfig.setLabel(null);
							}
						}
					}
				}

				@Override
				public void keyPressed(KeyEvent e) {
					return; // Nothing to be done here

				}
			});
			dimensionLabelTextField.setPreferredSize(new Dimension(100, 22));

			automateLabelCheckBox = new JCheckBox(
					I18N.getGUILabel("plotter.configuration_dialog.automatic_axis_label.label"));
			// automateLabel.setLabelFor(automateLabelCheckBox);
			automateLabelCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						dimensionConfig.setAutoNaming(automateLabelCheckBox.isSelected());
					}
				}

			});

			addThreeComponentRow(this, dimensionLabelLabel, dimensionLabelTextField, automateLabelCheckBox);
		}

		// setup date format configuration components
		{
			dateFormatLabel = new ResourceLabel("plotter.configuration_dialog.date_format");

			// create x-axis legend text field
			dateFormatTextField = new JTextField();
			dateFormatLabel.setLabelFor(dateFormatTextField);
			dateFormatTextField.addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {
					// do nothing
				}

				@Override
				public void keyPressed(KeyEvent e) {
					// do nothing
				}

				@Override
				public void keyReleased(KeyEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						String oldFormat = dimensionConfig.getUserDefinedDateFormatString();
						String newFormat = dateFormatTextField.getText();
						if (oldFormat != null && !oldFormat.equals(newFormat) || oldFormat == null && newFormat.length() > 0) {
							if (newFormat.length() > 0) {
								dimensionConfig.setUserDefinedDateFormatString(newFormat);
							} else {
								dimensionConfig.setLabel(null);
							}
						}
					}
				}

			});

			useDefaultDateFormatCheckBox = new JCheckBox(
					I18N.getGUILabel("plotter.configuration_dialog.use_default_date_format.label"));
			useDefaultDateFormatCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						dimensionConfig.setUseUserDefinedDateFormat(!useDefaultDateFormatCheckBox.isSelected());
					}
				}
			});

			addThreeComponentRow(this, dateFormatLabel, dateFormatTextField, useDefaultDateFormatCheckBox);
		}

		// if (dimension != PlotDimension.DOMAIN) {
		// automateLabel.setVisible(false);
		// automateLabelCheckBox.setVisible(false);
		// dimensionLabelLabel.setVisible(false);
		// dimensionLabelTextField.setVisible(false);
		// }

		// add grouping combo box
		{
			// create label
			groupingLabel = new ResourceLabel("plotter.configuration_dialog.grouping");
			groupingLabel.setPreferredSize(new Dimension(80, 15));

			// create combo box
			groupingComboBox = new JComboBox<>(GroupingType.values());
			groupingLabel.setLabelFor(groupingComboBox);
			groupingComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.grouping_type"));
			groupingComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						GroupingType groupingType = (GroupingType) groupingComboBox.getSelectedItem();

						ValueGrouping grouping = dimensionConfig.getGrouping();
						boolean newGroupingNoneAndOldGroupingSet = groupingType == GroupingType.NONE && grouping != null;
						boolean newGroupingAndNotGrouping = groupingType != GroupingType.NONE && grouping == null;
						boolean groupingTypeChanged = grouping != null && groupingType != grouping.getGroupingType();

						if ((newGroupingNoneAndOldGroupingSet || newGroupingAndNotGrouping || groupingTypeChanged)) {
							ValueGrouping oldGrouping = grouping;

							boolean categorical = true;
							if (oldGrouping != null) {
								categorical = oldGrouping.isCategorical();
							}

							ValueGrouping newGrouping = null;
							try {
								newGrouping = ValueGroupingFactory.getValueGrouping(groupingType,
										dimensionConfig.getDataTableColumn(), categorical, dimensionConfig.getDateFormat());
								dimensionConfig.setGrouping(newGrouping);
							} catch (ChartConfigurationException e1) {
								PlotConfigurationError plotConfigurationError = e1.getResponse().getErrors().get(0);
								SwingTools.showVerySimpleErrorMessage(plotConfigurationError.getErrorId(),
										plotConfigurationError.getMessageParameters());
								if (oldGrouping == null) {
									groupingComboBox.setSelectedItem(GroupingType.NONE);
								} else {
									groupingComboBox.setSelectedItem(oldGrouping.getGroupingType());
								}

								adaptGUI();
								return;
							}
						}
					}

				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;
				}
			});

			groupingConfigButton = new JToggleButton(new PopupAction(true,
					"plotter.configuration_dialog.configure_grouping", groupingConfigurationPanel,
					PopupAction.PopupPosition.HORIZONTAL));
			addThreeComponentRow(this, groupingLabel, groupingComboBox, groupingConfigButton);

		}

		// // add value source selection if dimension is domain dimension
		// {
		//
		// JTable valueSourceGroupingSelectionTable = new JTable(new
		// GroupingSelectionTableModel(getPlotConfiguration()));
		// valueSourceGroupingSelectionTable.setBackground(Color.red);
		// valueSourceGroupingSelectionTable.setTableHeader(null);
		//
		// JScrollPane valueSourceGroupingPane = new JScrollPane(valueSourceGroupingSelectionTable);
		// valueSourceGroupingPane.setPreferredSize(new Dimension(100,50));
		//
		// GridBagConstraints itemConstraint = new GridBagConstraints();
		// itemConstraint.fill = GridBagConstraints.HORIZONTAL;
		// itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
		//
		// this.add(valueSourceGroupingPane, itemConstraint);
		//
		// if (dimension != PlotDimension.DOMAIN) {
		// // set visible false
		// valueSourceGroupingPane.setVisible(false);
		// }
		//
		// }

		{
			logarithmicLabel = new ResourceLabel("plotter.configuration_dialog.logarithmic");

			logarithmicCheckBox = new JCheckBox();
			logarithmicLabel.setLabelFor(logarithmicCheckBox);
			logarithmicCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						dimensionConfig.setLogarithmic(logarithmicCheckBox.isSelected());
					}
				}
			});

			addTwoComponentRow(this, logarithmicLabel, logarithmicCheckBox);

		}

		// add sorting row
		if (dimension == PlotDimension.DOMAIN) {
			{
				sortingLabel = new ResourceLabel("plotter.configuration_dialog.sorting");

				// create checkbox
				sortedCheckBox = new JCheckBox();
				sortingLabel.setLabelFor(sortedCheckBox);
				sortedCheckBox.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						DomainConfigManager domainMngr = getPlotConfiguration().getDomainConfigManager();
						if (sortedCheckBox.isSelected()) {
							domainMngr.setSortingMode(DomainConfigManager.Sorting.ASCENDING);
						} else {
							domainMngr.setSortingMode(DomainConfigManager.Sorting.NONE);
						}
					}
				});

				addTwoComponentRow(this, sortingLabel, sortedCheckBox);
			}
		}

		// add upper bound spinner
		{
			upperBoundLabel = new ResourceLabel("plotter.configuration_dialog.upper_filter");

			// create spinner
			upperBoundSpinner = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.1));
			upperBoundLabel.setLabelFor(upperBoundSpinner);
			upperBoundSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						Double oldUpperBound = dimensionConfig.getUserDefinedUpperBound();
						Double newUpperBound = (Double) upperBoundSpinner.getValue();
						if (!Objects.equals(oldUpperBound, newUpperBound)) {
							// Double currentLowerBound =
							// dimensionConfig.getUserDefinedLowerBound();
							// if (oldUpperBound != null && currentLowerBound != null) {
							// if (Math.abs(newUpperBound - currentLowerBound) < 1E-5 ||
							// newUpperBound <= currentLowerBound) {
							// upperBoundSpinner.setValue(oldUpperBound);
							// return;
							// }
							dimensionConfig.setUpperBound(newUpperBound);
							//
							// }
						}
					}
				}

			});

			// create checkbox
			upperBoundCheckBox = new JCheckBox(I18N.getGUILabel("plotter.configuration_dialog.use_filter.label"));
			upperBoundCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						dimensionConfig.setUseUserDefinedUpperBound(upperBoundCheckBox.isSelected());
					}
				}

			});

			addThreeComponentRow(this, upperBoundLabel, upperBoundSpinner, upperBoundCheckBox);
		}

		// add lower bound spinner
		{

			lowerBoundLabel = new ResourceLabel("plotter.configuration_dialog.lower_filter");

			// create spinner
			lowerBoundSpinner = new JSpinner(new SpinnerNumberModel(0.0, null, null, 0.1));
			lowerBoundLabel.setLabelFor(lowerBoundSpinner);
			lowerBoundSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {

						Double oldLowerBound = dimensionConfig.getUserDefinedLowerBound();
						Double newLowerBound = (Double) lowerBoundSpinner.getValue();
						if (!Objects.equals(oldLowerBound, newLowerBound)) {
							dimensionConfig.setLowerBound(newLowerBound);
						}
					}
				}
			});

			// create checkbox
			lowerBoundCheckBox = new JCheckBox(I18N.getGUILabel("plotter.configuration_dialog.use_filter.label"));
			lowerBoundCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						dimensionConfig.setUseUserDefinedLowerBound(lowerBoundCheckBox.isSelected());
					}
				}

			});

			addThreeComponentRow(this, lowerBoundLabel, lowerBoundSpinner, lowerBoundCheckBox);
		}

		// add spacer panel
		{
			JPanel spacerPanel = new JPanel();
			GridBagConstraints itemConstraint = new GridBagConstraints();
			itemConstraint.fill = GridBagConstraints.BOTH;
			itemConstraint.weightx = 1;
			itemConstraint.weighty = 1;
			itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			this.add(spacerPanel, itemConstraint);
		}

	}

	private void upperBoundChanged(Double upperBound) {
		upperBoundSpinner.setValue(upperBound);
	}

	private void lowerBoundChanged(Double lowerBound) {
		lowerBoundSpinner.setValue(lowerBound);
	}

	private void useLowerBoundChanged(boolean useLowerBound, boolean nominalAxis) {
		lowerBoundCheckBox.setSelected(useLowerBound);
		lowerBoundSpinner.setEnabled(useLowerBound && !nominalAxis);
		lowerBoundCheckBox.setEnabled(!nominalAxis);
		lowerBoundLabel.setEnabled(!nominalAxis);
	}

	private void useUpperBoundChanged(boolean useUpperBound, boolean nominalAxis) {
		upperBoundCheckBox.setSelected(useUpperBound);
		upperBoundSpinner.setEnabled(useUpperBound && !nominalAxis);
		upperBoundCheckBox.setEnabled(!nominalAxis);
		upperBoundLabel.setEnabled(!nominalAxis);
	}

	private void logarithmicChanged(boolean logarithmic) {
		logarithmicCheckBox.setSelected(logarithmic);
	}

	private void dimensionLabelChanged(String label) {
		if (!dimensionLabelTextField.getText().equals(label)) {
			if (label != null) {
				dimensionLabelTextField.setText(label);
			} else {
				dimensionLabelTextField.setText("");
			}
		}
	}

	private void autoNamingChanged(boolean autoNaming) {
		automateLabelCheckBox.setSelected(autoNaming);
		dimensionLabelTextField.setEnabled(!autoNaming);
	}

	@Override
	protected void adaptGUI() {

		DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
		DataTable dataTable = getCurrentPlotInstance().getPlotData().getOriginalDataTable();
		if (dimensionConfig != null && dimensionConfig.getDataTableColumn().isValidForDataTable(dataTable)) {
			enableAllComponents();

			boolean enableOptions = !dimensionConfig.getDataTableColumn().isNominal()
					&& !dimensionConfig.getDataTableColumn().isDate();

			if (dimension == PlotDimension.DOMAIN) {
				DomainConfigManager domCnfMngr = (DomainConfigManager) dimensionConfig;
				sortedCheckBox.setSelected(domCnfMngr.getSortingMode() != Sorting.NONE);
				sortedCheckBox.setVisible(enableOptions);
				sortingLabel.setVisible(enableOptions);
				// boolean groupingVisible = dimensionConfig.getDataTableColumn().getValueType() !=
				// ValueType.NOMINAL;
				// groupingLabel.setVisible(groupingVisible);
				// groupingComboBox.setVisible(groupingVisible);
				// groupingConfigButton.setVisible(groupingVisible);
			}

			// configure date format (if original data (not aggregated data) is date)
			boolean dateFormatConfigVisible = dimensionConfig.getDataTableColumn().isDate();
			dateFormatTextField.setVisible(dateFormatConfigVisible);
			dateFormatLabel.setVisible(dateFormatConfigVisible);
			useDefaultDateFormatCheckBox.setVisible(dateFormatConfigVisible);

			String userDefinedDateFormatString = dimensionConfig.getUserDefinedDateFormatString();
			String text = dateFormatTextField.getText();
			if (!text.equals(userDefinedDateFormatString)) {
				dateFormatTextField.setText(userDefinedDateFormatString);
			}
			dateFormatTextField.setEnabled(dimensionConfig.isUsingUserDefinedDateFormat());
			useDefaultDateFormatCheckBox.setSelected(!dimensionConfig.isUsingUserDefinedDateFormat());

			lowerBoundCheckBox.setVisible(enableOptions);
			lowerBoundLabel.setVisible(enableOptions);
			lowerBoundSpinner.setVisible(enableOptions);

			upperBoundCheckBox.setVisible(enableOptions);
			upperBoundLabel.setVisible(enableOptions);
			upperBoundSpinner.setVisible(enableOptions);

			logarithmicLabel.setVisible(enableOptions);
			logarithmicCheckBox.setVisible(enableOptions);

			// fill grouping combo box
			Vector<GroupingType> validGroupings = dimensionConfig.getValidGroupingTypes();
			DefaultComboBoxModel<GroupingType> model = new DefaultComboBoxModel<>(validGroupings);
			groupingComboBox.setModel(model);

			autoNamingChanged(dimensionConfig.isAutoNaming());

			dimensionLabelChanged(dimensionConfig.getLabel());

			boolean groupingEnabled = false;
			ValueGrouping grouping = dimensionConfig.getGrouping();
			GroupingType groupingType = null;
			if (grouping != null) {
				groupingType = grouping.getGroupingType();

				// normally DISTINCT_VALUES grouping cannot be configured,
				// only if the values to be grouped are non-nominal, it can
				// be configured to create a nominal grouping
				if ((groupingType != GroupingType.DISTINCT_VALUES || dimensionConfig.getDataTableColumn().getValueType() != ValueType.NOMINAL)) {
					groupingEnabled = true;
				}
				groupingComboBox.setSelectedItem(groupingType);
			} else {
				groupingComboBox.setSelectedItem(GroupingType.NONE);
			}

			groupingConfigButton.setEnabled(groupingEnabled);

			logarithmicChanged(dimensionConfig.isLogarithmic());

			boolean nominal = (dimensionConfig.getDataTableColumn().getValueType() == ValueType.NOMINAL)
					&& dimensionConfig.isNominal();

			lowerBoundChanged(dimensionConfig.getUserDefinedLowerBound());
			useLowerBoundChanged(dimensionConfig.isUsingUserDefinedLowerBound(), nominal);

			upperBoundChanged(dimensionConfig.getUserDefinedUpperBound());
			useUpperBoundChanged(dimensionConfig.isUsingUserDefinedUpperBound(), nominal);

		} else {
			disableAllComponents();
		}
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		adaptGUI();
		return true;
	}
}
