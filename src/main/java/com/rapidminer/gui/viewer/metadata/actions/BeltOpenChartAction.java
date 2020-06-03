/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.ButtonBarCardPanel;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.AbstractBeltColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltDateTimeColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltNominalColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltNumericalColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltTimeColumnStatisticsModel;
import com.rapidminer.tools.LogService;


/**
 * This action is only to be used by the {@link BeltColumnPopupMenu} or as a button inside a
 * {@link BeltColumnStatisticsPanel}.
 *
 * @author Marco Boeck, Michael Knopf, Nils Woehler, Gisa Meier
 * @since 9.7.0
 */
public class BeltOpenChartAction extends ResourceAction {

	private static final String VISUALIZATIONS_CLASS_NAME = "com.rapidminer.extension.html5charts.gui.ChartViewer";
	private static final String SHOW_AGGREGATED_COLUMN_METHOD_NAME = "showAggregatedColumnChart";
	private static final String SHOW_HISTOGRAM_METHOD_NAME = "showHistogramChart";


	/**
	 * Creates a new {@link BeltOpenChartAction} instance.
	 */
	public BeltOpenChartAction() {
		super(true, "meta_data_stats.open_chart");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {

		// look up the panel invoking the pop up invoking the action
		BeltColumnStatisticsPanel asp = null;

		// the action should only be invoked by AttributePopupMenus
		Container parent = ((JComponent) e.getSource()).getParent();
		if ((parent instanceof BeltColumnPopupMenu)) {
			asp = ((BeltColumnPopupMenu) parent).getColumnStatisticsPanel();
		} else {
			asp = (BeltColumnStatisticsPanel) SwingUtilities.getAncestorOfClass(BeltColumnStatisticsPanel.class, parent);
			if (asp == null) {
				// we are not inside a AttributesStatisticPanel
				return;
			}
		}

		ButtonBarCardPanel cardPanel = (ButtonBarCardPanel) SwingUtilities.getAncestorOfClass(ButtonBarCardPanel.class, asp);
		AbstractBeltColumnStatisticsModel model = asp.getModel();

		// select the visualizations view
		cardPanel.selectCard("visualizations");

		// get the opened visualization
		getOpenedVisualizations(cardPanel, model);
	}

	/**
	 * Shows the opened visualization.
	 */
	private void getOpenedVisualizations(ButtonBarCardPanel cardPanel, AbstractBeltColumnStatisticsModel model) {
		JPanel outerPanel = (JPanel) cardPanel.getShownComponent();
		for (Component innerComp : outerPanel.getComponents()) {
			if (innerComp != null && innerComp.getClass().getName().equals(VISUALIZATIONS_CLASS_NAME)) {
				// adjust settings
				String attributeName = model.getColumnName();
				try {
					if (model instanceof BeltNominalColumnStatisticsModel) {
						Method showAggregatedColumnChart = innerComp.getClass().getDeclaredMethod(SHOW_AGGREGATED_COLUMN_METHOD_NAME, String.class);
						showAggregatedColumnChart.setAccessible(true);
						showAggregatedColumnChart.invoke(innerComp, attributeName);
					} else if (model instanceof BeltNumericalColumnStatisticsModel
							|| model instanceof BeltDateTimeColumnStatisticsModel || model instanceof BeltTimeColumnStatisticsModel) {
						Method showHistogramChart = innerComp.getClass().getDeclaredMethod(SHOW_HISTOGRAM_METHOD_NAME, String.class);
						showHistogramChart.setAccessible(true);
						showHistogramChart.invoke(innerComp, attributeName);
					}
				} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.viewer.metadata.actions.OpenChartAction.cannot_show_visualization", e1);
				}
				break;
			}
		}
	}
}
