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
package com.rapidminer.gui.new_plotter.gui.groupingpanel;

import com.rapidminer.gui.new_plotter.configuration.AbstractValueGrouping;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.AbstractConfigurationPanel;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent.DimensionConfigChangeType;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.tools.ResourceLabel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;


/**
 * Abstract panel for displaying settings of {@link ValueGrouping}s. Each value grouping is assigned
 * to a {@link PlotDimension} and can either be categorical or not. Furthermore it registers to the
 * {@link PlotConfiguration} to listen for change events.
 * <p>
 * When subclassing this Panel the methods adaptGUI and createComponents should overloaded to fill
 * the GUI.
 * 
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class AbstractGroupingCardPanel extends AbstractConfigurationPanel {

	private static final long serialVersionUID = 1L;

	private final PlotDimension dimension;

	private ResourceLabel categoricalLabel;
	private JCheckBox categoricalCheckBox;

	public AbstractGroupingCardPanel(PlotInstance plotInstance, PlotDimension dimension) {
		super(plotInstance);
		this.dimension = dimension;
		createComponents();
		registerAsPlotConfigurationListener();
		adaptGUI();
	}

	protected void createComponents() {

		// add categorical row
		{
			categoricalLabel = new ResourceLabel("plotter.configuration_dialog.categorical");

			// create checkbox
			categoricalCheckBox = new JCheckBox();
			categoricalLabel.setLabelFor(categoricalCheckBox);
			categoricalCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
					if (dimensionConfig != null) {
						AbstractValueGrouping grouping = (AbstractValueGrouping) dimensionConfig.getGrouping();
						if (grouping != null) {
							grouping.setCategorical(categoricalCheckBox.isSelected());
						}
					}
				}
			});

			addTwoComponentRow(this, categoricalLabel, categoricalCheckBox);
		}
	}

	@Override
	protected void adaptGUI() {
		DimensionConfig dimensionConfig = getPlotConfiguration().getDimensionConfig(dimension);
		if (dimensionConfig != null) {
			boolean enableOptions = !dimensionConfig.getDataTableColumn().isNominal();

			boolean groupingEnabled = false;
			ValueGrouping grouping = dimensionConfig.getGrouping();
			if (grouping != null) {
				groupingEnabled = true;
				categoricalCheckBox.setSelected(grouping.isCategorical());
			}

			categoricalCheckBox.setEnabled(groupingEnabled && enableOptions);
			categoricalLabel.setEnabled(groupingEnabled && enableOptions);

			categoricalLabel.setVisible(enableOptions);
			categoricalCheckBox.setVisible(enableOptions);
		}
	}

	/**
	 * @return the dimension
	 */
	public PlotDimension getDimension() {
		return dimension;
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		if (change.getType() == PlotConfigurationChangeType.DIMENSION_CONFIG_CHANGED) {
			DimensionConfigChangeEvent dimensionChange = change.getDimensionChange();
			if (dimensionChange.getDimension() == getDimension()) {
				if (dimensionChange.getType() == DimensionConfigChangeType.GROUPING_CHANGED
						|| dimensionChange.getType() == DimensionConfigChangeType.COLUMN) {
					adaptGUI();
				}
			}
		}
		if (change.getType() == PlotConfigurationChangeType.META_CHANGE) {
			processPlotConfigurationMetaChange(change);
		}
		return true;
	}
}
