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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.FillStyle;
import com.rapidminer.gui.new_plotter.configuration.SeriesFormat.StackingMode;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent.RangeAxisConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent.ValueSourceChangeType;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class AreaAndBarChartConfigurationPanel extends AbstractTreeSelectionDependentPanel {

	private static final long serialVersionUID = 1L;
	private JLabel stackingModeLabel;
	private JComboBox<StackingMode> stackingModeComboBox;
	private JLabel itemColorLabel;
	private JButton itemColorButton;
	private JLabel fillStyleLabel;
	private JComboBox<FillStyle> fillStyleComboBox;
	private JLabel opacityLabel;
	private JSlider opacitySlider;

	public AreaAndBarChartConfigurationPanel(boolean smallIcons, JTree plotConfigurationTree, PlotInstance plotInstance) {
		super(plotConfigurationTree, plotInstance);
		createComponents(smallIcons);
		registerAsPlotConfigurationListener();
		initComponents();
		setPreferredSize(new Dimension(220, 200));
	}

	private void createComponents(boolean smallIcons) {

		// add fillstyle
		{
			// create fill style label
			fillStyleLabel = new ResourceLabel("plotter.configuration_dialog.fill_style");

			// create fill style combobox
			fillStyleComboBox = new JComboBox<>(FillStyle.values());
			fillStyleLabel.setLabelFor(fillStyleComboBox);
			fillStyleComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.fillstyle"));
			fillStyleComboBox.setSelectedIndex(0);
			fillStyleComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					getSelectedValueSource().getSeriesFormat().setAreaFillStyle(
							(FillStyle) fillStyleComboBox.getSelectedItem());

				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;
				}
			});

			// add fill color row
			addTwoComponentRow(this, fillStyleLabel, fillStyleComboBox);

		}

		{
			// create stacking mode label
			stackingModeLabel = new ResourceLabel("plotter.configuration_dialog.stacking_mode");

			// create stacking mode combo box
			stackingModeComboBox = new JComboBox<>(StackingMode.values());
			stackingModeLabel.setLabelFor(stackingModeComboBox);
			stackingModeComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.stacking_mode"));
			stackingModeComboBox.setSelectedIndex(0);
			stackingModeComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					getSelectedValueSource().getSeriesFormat().setStackingMode(
							(StackingMode) stackingModeComboBox.getSelectedItem());

				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;

				}
			});

			// add stacking mode row
			addTwoComponentRow(this, stackingModeLabel, stackingModeComboBox);
		}

		// add item color
		{
			// add item color label
			itemColorLabel = new ResourceLabel("plotter.configuration_dialog.item_color");
			itemColorLabel.setPreferredSize(new Dimension(80, 15));

			// create item color button
			itemColorButton = new JButton(new ResourceAction(smallIcons, "plotter.configuration_dialog.choose_item_color") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					createItemColorDialog();
				}

			});
			itemColorLabel.setLabelFor(itemColorButton);

			addTwoComponentRow(this, itemColorLabel, itemColorButton);

		}

		// add opacity slider
		{
			// create opacity label
			opacityLabel = new ResourceLabel("plotter.configuration_dialog.opacity");

			// create opacity slider
			opacitySlider = new JSlider(0, 255, 125);
			opacityLabel.setLabelFor(opacitySlider);
			opacitySlider.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						getSelectedValueSource().getSeriesFormat().setOpacity(source.getValue());
					}
				}

			});

			// add opacity slider
			addTwoComponentRow(this, opacityLabel, opacitySlider);

		}

		// fill space with JPanel
		GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.fill = GridBagConstraints.BOTH;
		itemConstraint.weightx = 1;
		itemConstraint.weighty = 1;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
		this.add(new JPanel(), itemConstraint);

	}

	private void initComponents() {
		adaptGUI();
	}

	private void createItemColorDialog() {
		if (getSelectedValueSource() != null) {
			Color itemColor = getSelectedValueSource().getSeriesFormat().getItemColor();
			Color newItemColor = JColorChooser.showDialog(this,
					I18N.getGUILabel("plotter.configuration_dialog.choose_color.label"), itemColor);
			if (newItemColor != null && !(newItemColor.equals(itemColor))) {
				getSelectedValueSource().getSeriesFormat().setItemColor(newItemColor);
			}
		}
	}

	private void opacityChanged(Integer integer) {
		opacitySlider.setValue(integer);
	}

	private void fillStyleSelectionChanged(FillStyle fillStyle) {
		fillStyleComboBox.setSelectedItem(fillStyle);
	}

	private void stackingModeSelectionChanged(StackingMode stackingMode) {
		stackingModeComboBox.setSelectedItem(stackingMode);
	}

	@Override
	protected void adaptGUI() {
		if (getSelectedValueSource() != null) {
			SeriesFormat format = getSelectedValueSource().getSeriesFormat();
			opacityChanged(format.getOpacity());
			fillStyleSelectionChanged(format.getAreaFillStyle());
			stackingModeSelectionChanged(format.getStackingMode());
		}
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		if (type == PlotConfigurationChangeType.RANGE_AXIS_CONFIG_CHANGED) {
			RangeAxisConfigChangeEvent rangeAxisConfigChange = change.getRangeAxisConfigChange();
			if (rangeAxisConfigChange.getType() == RangeAxisConfigChangeType.VALUE_SOURCE_CHANGED) {
				ValueSourceChangeEvent valueSourceChange = rangeAxisConfigChange.getValueSourceChange();
				if (valueSourceChange.getType() == ValueSourceChangeType.SERIES_FORMAT_CHANGED) {
					adaptGUI();
				}
				if (valueSourceChange.getType() == ValueSourceChangeType.USES_GROUPING) {
					adaptGUI();
				}
			}
		}
		if (type == PlotConfigurationChangeType.DIMENSION_CONFIG_ADDED) {
			if (change.getDimension() == PlotDimension.COLOR) {
				adaptGUI();
			}
		}
		if (type == PlotConfigurationChangeType.DIMENSION_CONFIG_REMOVED) {
			if (change.getDimension() == PlotDimension.COLOR) {
				adaptGUI();
			}
		}
		if (type == PlotConfigurationChangeType.META_CHANGE) {
			processPlotConfigurationMetaChange(change);
		}
		return true;
	}
}
