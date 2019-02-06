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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.data.RangeAxisData;
import com.rapidminer.gui.new_plotter.gui.treenodes.RangeAxisConfigTreeNode;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class RangeAxisConfigPanel extends AbstractConfigurationPanel implements TreeSelectionListener {

	private static final long serialVersionUID = 1L;

	private RangeAxisConfig selectedRangeAxisConfig = null;

	private JTextField rangeAxisLabelTextfield;
	private JToggleButton automateLabelCheckBox;
	private JLabel rangeAxisLabelLabel;
	private JCheckBox logarithmicCheckBox;

	private JLabel logarithmicLabel;

	private JLabel lowerBoundLabel;
	private JCheckBox lowerBoundCheckBox;
	private JSpinner lowerBoundSpinner;

	private JLabel upperBoundLabel;
	private JCheckBox upperBoundCheckBox;
	private JSpinner upperBoundSpinner;

	public RangeAxisConfigPanel(JTree plotConfigTree, PlotInstance plotInstance) {
		super(plotInstance);
		createComponents();
		registerAsPlotConfigurationListener();
		plotConfigTree.addTreeSelectionListener(this);
	}

	private void createComponents() {

		// {
		// JLabel automateLabel = new
		// ResourceLabel("plotter.configuration_dialog.automatic_axis_label");
		//
		// automateLabelCheckBox = new JCheckBox();
		// automateLabel.setLabelFor(automateLabelCheckBox);
		// automateLabelCheckBox.addActionListener(new ActionListener() {
		//
		// @Override
		// public void loggedActionPerformed(ActionEvent e) {
		// if (selectedRangeAxisConfig != null) {
		// selectedRangeAxisConfig.setAutoNaming(automateLabelCheckBox.isSelected());
		// }
		// }
		//
		// });
		//
		// addTwoComponentRow(this, automateLabel, automateLabelCheckBox);
		//
		// }

		{

			rangeAxisLabelLabel = new JLabel(I18N.getGUILabel("plotter.configuration_dialog.dimension_axis_title.label"));

			// add y-axis text field
			rangeAxisLabelTextfield = new JTextField();
			rangeAxisLabelLabel.setLabelFor(rangeAxisLabelTextfield);
			rangeAxisLabelTextfield.addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {
					return;
				}

				@Override
				public void keyReleased(KeyEvent e) {
					if (selectedRangeAxisConfig != null) {
						String newTitle = rangeAxisLabelTextfield.getText();
						String titleText = selectedRangeAxisConfig.getLabel();
						if (titleText != null && !titleText.equals(newTitle) || titleText == null && newTitle.length() > 0) {
							if (newTitle.length() > 0) {
								selectedRangeAxisConfig.setLabel(newTitle);
							} else {
								selectedRangeAxisConfig.setLabel(null);
							}
						}
					}
				}

				@Override
				public void keyPressed(KeyEvent e) {
					return;
				}
			});
			rangeAxisLabelTextfield.setPreferredSize(new Dimension(100, 22));

			automateLabelCheckBox = new JCheckBox(
					I18N.getGUILabel("plotter.configuration_dialog.automatic_axis_label.label"));
			automateLabelCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedRangeAxisConfig != null) {
						selectedRangeAxisConfig.setAutoNaming(automateLabelCheckBox.isSelected());
					}
				}

			});

			// addTwoComponentRow(this, rangeAxisLabelLabel, rangeAxisLabelTextfield);
			addThreeComponentRow(this, rangeAxisLabelLabel, rangeAxisLabelTextfield, automateLabelCheckBox);

		}

		// add logarithmic check box
		{
			logarithmicLabel = new ResourceLabel("plotter.configuration_dialog.logarithmic");

			logarithmicCheckBox = new JCheckBox();
			logarithmicLabel.setLabelFor(logarithmicCheckBox);
			logarithmicCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedRangeAxisConfig != null) {
						selectedRangeAxisConfig.setLogarithmicAxis(logarithmicCheckBox.isSelected());
					}
				}
			});

			addTwoComponentRow(this, logarithmicLabel, logarithmicCheckBox);

		}

		// add upper bound spinner
		{
			upperBoundLabel = new ResourceLabel("plotter.configuration_dialog.upper_bound");

			upperBoundSpinner = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.1));
			upperBoundLabel.setLabelFor(upperBoundSpinner);
			upperBoundSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (selectedRangeAxisConfig != null) {
						RangeAxisData selectedRangeAxisData = getCurrentPlotInstance().getPlotData().getRangeAxisData(
								selectedRangeAxisConfig);
						Double oldUpperBound = selectedRangeAxisData.getUpperViewBound();
						Double newUpperBound = (Double) upperBoundSpinner.getValue();
						if (!Objects.equals(oldUpperBound, newUpperBound)) {
							selectedRangeAxisConfig.setUpperViewBound(newUpperBound);
						}
					}
				}

			});

			// create checkbox
			upperBoundCheckBox = new JCheckBox(I18N.getGUILabel("plotter.configuration_dialog.use_boundary.label"));
			upperBoundCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedRangeAxisConfig != null) {
						selectedRangeAxisConfig.setUseUserDefinedUpperViewBound(!upperBoundCheckBox.isSelected());
					}
				}

			});

			addThreeComponentRow(this, upperBoundLabel, upperBoundSpinner, upperBoundCheckBox);
		}

		// add lower bound spinner
		{

			lowerBoundLabel = new ResourceLabel("plotter.configuration_dialog.lower_bound");

			// create spinner
			lowerBoundSpinner = new JSpinner(new SpinnerNumberModel(0.0, null, null, 0.1));
			lowerBoundLabel.setLabelFor(lowerBoundSpinner);
			lowerBoundSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					if (selectedRangeAxisConfig != null) {

						RangeAxisData selectedRangeAxisData = getCurrentPlotInstance().getPlotData().getRangeAxisData(
								selectedRangeAxisConfig);
						Double oldLowerBound = selectedRangeAxisData.getLowerViewBound();
						Double newLowerBound = (Double) lowerBoundSpinner.getValue();
						if (!Objects.equals(oldLowerBound, newLowerBound)) {
							selectedRangeAxisConfig.setLowerViewBound(newLowerBound);
						}
					}
				}
			});

			// create checkbox
			lowerBoundCheckBox = new JCheckBox(I18N.getGUILabel("plotter.configuration_dialog.use_boundary.label"));
			lowerBoundCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedRangeAxisConfig != null) {
						selectedRangeAxisConfig.setUseUserDefinedLowerViewBound(!lowerBoundCheckBox.isSelected());
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

	private void rangeAxisLabelChanged(String label) {
		String oldText = rangeAxisLabelTextfield.getText();
		if (!oldText.equals(label)) {
			if (label != null) {
				rangeAxisLabelTextfield.setText(label);
			} else {
				rangeAxisLabelTextfield.setText("");
			}
		}
	}

	private void automateNamingChanged(boolean automateLabel) {
		automateLabelCheckBox.setSelected(automateLabel);
		rangeAxisLabelTextfield.setEnabled(!automateLabel);
	}

	private void upperBoundChanged(Double upperBound) {
		upperBoundSpinner.setValue(upperBound);
	}

	private void lowerBoundChanged(Double lowerBound) {
		lowerBoundSpinner.setValue(lowerBound);
	}

	private void useLowerBoundChanged(boolean useLowerBound, boolean visible) {
		lowerBoundCheckBox.setSelected(!useLowerBound);

		lowerBoundSpinner.setVisible(visible);
		lowerBoundSpinner.setEnabled(useLowerBound);
		lowerBoundCheckBox.setVisible(visible);
		lowerBoundLabel.setVisible(visible);
	}

	private void useUpperBoundChanged(boolean useUpperBound, boolean visible) {
		upperBoundCheckBox.setSelected(!useUpperBound);

		upperBoundSpinner.setVisible(visible);
		upperBoundSpinner.setEnabled(useUpperBound);
		upperBoundCheckBox.setVisible(visible);
		upperBoundLabel.setVisible(visible);
	}

	@Override
	protected void adaptGUI() {
		if (selectedRangeAxisConfig != null) {
			boolean enable = selectedRangeAxisConfig.getValueType() != ValueType.NOMINAL;
			logarithmicLabel.setVisible(enable);
			logarithmicCheckBox.setVisible(enable);
			logarithmicCheckBox.setSelected(selectedRangeAxisConfig.isLogarithmicAxis());

			rangeAxisLabelChanged(selectedRangeAxisConfig.getLabel());
			automateNamingChanged(selectedRangeAxisConfig.isAutoNaming());
			upperBoundChanged(selectedRangeAxisConfig.getUserDefinedRange().getUpperBound());
			lowerBoundChanged(selectedRangeAxisConfig.getUserDefinedRange().getLowerBound());

			useLowerBoundChanged(selectedRangeAxisConfig.isUsingUserDefinedLowerViewBound(), enable);
			useUpperBoundChanged(selectedRangeAxisConfig.isUsingUserDefinedUpperViewBound(), enable);

			boolean visibleRangSelection = (selectedRangeAxisConfig.getValueType() != ValueType.DATE_TIME);
			lowerBoundCheckBox.setVisible(visibleRangSelection);
			lowerBoundLabel.setVisible(visibleRangSelection);
			lowerBoundSpinner.setVisible(visibleRangSelection);

			upperBoundCheckBox.setVisible(visibleRangSelection);
			upperBoundLabel.setVisible(visibleRangSelection);
			upperBoundSpinner.setVisible(visibleRangSelection);
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
		if (newLeadSelectionPath == null) {
			selectedRangeAxisConfig = null;
			return;
		}
		Object lastPathComponent = newLeadSelectionPath.getLastPathComponent();
		if (lastPathComponent instanceof RangeAxisConfigTreeNode) {

			RangeAxisConfig selectedConfig = ((RangeAxisConfigTreeNode) lastPathComponent).getUserObject();

			selectedRangeAxisConfig = selectedConfig;

			adaptGUI();

		} else {
			selectedRangeAxisConfig = null;
		}
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType plotConfigChangeType = change.getType();
		if (plotConfigChangeType == PlotConfigurationChangeType.RANGE_AXIS_CONFIG_CHANGED) {
			RangeAxisConfigChangeEvent rangeAxisConfigChange = change.getRangeAxisConfigChange();

			RangeAxisConfig source = rangeAxisConfigChange.getSource();
			if (selectedRangeAxisConfig == source) {
				adaptGUI();
			}
		}
		if (plotConfigChangeType == PlotConfigurationChangeType.META_CHANGE) {
			processPlotConfigurationMetaChange(change);
		}

		return true;
	}
}
