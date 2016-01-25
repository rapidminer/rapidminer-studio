/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.viewer.metadata.actions;

import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterConfigurationSettings;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.ButtonBarCardPanel;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.DateTimeAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NominalAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.NumericalAttributeStatisticsModel;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * This action is only to be used by the {@link AttributePopupMenu} or as a button inside a
 * {@link AttributeStatisticsPanel}.
 * 
 * @author Marco Boeck, Michael Knopf, Nils Woehler
 * 
 */
public class OpenChartAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link OpenChartAction} instance.
	 */
	public OpenChartAction() {
		super(true, "meta_data_stats.open_chart");
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// look up the panel invoking the pop up invoking the action
		AttributeStatisticsPanel asp = null;

		// the action should only be invoked by AttributePopupMenus
		Container parent = ((JComponent) e.getSource()).getParent();
		if ((parent instanceof AttributePopupMenu)) {
			asp = ((AttributePopupMenu) parent).getAttributeStatisticsPanel();
		} else {
			asp = (AttributeStatisticsPanel) SwingUtilities.getAncestorOfClass(AttributeStatisticsPanel.class, parent);
			if (asp == null) {
				// we are not inside a AttributesStatisticPanel
				return;
			}
		}

		ButtonBarCardPanel cardPanel = (ButtonBarCardPanel) SwingUtilities.getAncestorOfClass(ButtonBarCardPanel.class, asp);
		AbstractAttributeStatisticsModel model = asp.getModel();

		// select the plotter view
		cardPanel.selectCard("plot_view");

		// get the opened plotter
		JPanel outerPanel = (JPanel) cardPanel.getShownComponent();
		for (Component innerComp : outerPanel.getComponents()) {
			if (innerComp instanceof PlotterPanel) {
				PlotterPanel plotterPanel = (PlotterPanel) outerPanel.getComponent(0);
				PlotterConfigurationModel settings = plotterPanel.getPlotterSettings();

				// adjust settings
				if (model instanceof NominalAttributeStatisticsModel) {
					settings.setPlotter(PlotterConfigurationModel.BAR_CHART);
					settings.setParameterAsString(PlotterConfigurationSettings.AXIS_PLOT_COLUMN, model.getAttribute()
							.getName());
					settings.setParameterAsString(PlotterConfigurationSettings.GROUP_BY_COLUMN, model.getAttribute()
							.getName());
				} else if (model instanceof NumericalAttributeStatisticsModel
						|| model instanceof DateTimeAttributeStatisticsModel) {
					settings.setPlotter(PlotterConfigurationModel.HISTOGRAM_PLOT);
					settings.setParameterAsString(PlotterConfigurationSettings.NUMBER_OF_BINS, "10");
					settings.setParameterAsString(PlotterConfigurationSettings.AXIS_PLOT_COLUMNS, model.getAttribute()
							.getName());
				}
				break;
			}
		}
	}
}
