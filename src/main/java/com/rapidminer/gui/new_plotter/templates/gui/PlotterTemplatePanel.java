/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.new_plotter.templates.gui;

import com.rapidminer.gui.new_plotter.ConfigurationChangeResponse;
import com.rapidminer.gui.new_plotter.MasterOfDesaster;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.MasterOfDesasterListener;
import com.rapidminer.gui.new_plotter.templates.PlotterTemplate;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;


/**
 * This class is the abstract superclass for all {@link PlotterTemplate} GUIs.
 * 
 * @author Marco Boeck
 * 
 */
public abstract class PlotterTemplatePanel extends JPanel implements Observer {

	/** this label indicates a chart config error */
	protected JLabel errorIndicatorLabel;

	/** the {@link MasterOfDesasterListener} */
	private MasterOfDesasterListener listener;

	/** the current {@link PlotInstance} */
	private PlotInstance currentPlotInstance;

	private static final long serialVersionUID = -7451641816924895335L;

	/**
	 * Standard constructor. Adds the {@link PlotterTemplatePanel} as an {@link Observer} to the
	 * {@link PlotterTemplate}.
	 */
	public PlotterTemplatePanel(final PlotterTemplate template) {
		errorIndicatorLabel = new JLabel();
		errorIndicatorLabel.setIcon(SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.template.chart_ok.icon")));
		// show the tooltip longer for the errorIndicator
		errorIndicatorLabel.addMouseListener(new MouseAdapter() {

			private int defaultDismissDelay;

			@Override
			public void mouseEntered(MouseEvent me) {
				defaultDismissDelay = ToolTipManager.sharedInstance().getDismissDelay();
				ToolTipManager.sharedInstance().setDismissDelay(60000);
			}

			@Override
			public void mouseExited(MouseEvent me) {
				ToolTipManager.sharedInstance().setDismissDelay(defaultDismissDelay);
			}
		});

		template.addObserver(this);
	}

	/**
	 * This method is called each time the {@link PlotInstance} changes.
	 * 
	 * @param plotInstance
	 *            the new {@link PlotInstance}
	 */
	public void updatePlotInstance(final PlotInstance plotInstance) {
		if (listener == null) {
			listener = new MasterOfDesasterListener() {

				@Override
				public void masterOfDesasterChanged(final MasterOfDesaster masterOfDesaster) {
					List<PlotConfigurationError> errors = plotInstance.getErrors();
					List<ConfigurationChangeResponse> configurationChangeResponses = masterOfDesaster
							.getConfigurationChangeResponses();
					boolean warningFound = false;
					boolean errorFound = !errors.isEmpty();
					for (ConfigurationChangeResponse response : configurationChangeResponses) {
						if (!response.getErrors().isEmpty()) {
							errorFound = true;
							break;
						}
						if (!response.getWarnings().isEmpty()) {
							warningFound = true;
						}
					}
					final ImageIcon newIcon;
					if (!errorFound && !warningFound) {
						newIcon = SwingTools.createIcon("16/"
								+ I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.template.chart_ok.icon"));
					} else if (!errorFound && warningFound) {
						newIcon = SwingTools.createIcon("16/"
								+ I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.template.chart_warning.icon"));
					} else {
						newIcon = SwingTools.createIcon("16/"
								+ I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.template.chart_error.icon"));
					}
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							errorIndicatorLabel.setIcon(newIcon);
							errorIndicatorLabel.setToolTipText(masterOfDesaster.toHtmlString());
						}

					});
				}
			};
		}

		// remove listener from previous plotInstance, add to new one
		if (currentPlotInstance != null) {
			currentPlotInstance.getMasterOfDesaster().removeListener(listener);
		}
		plotInstance.getMasterOfDesaster().addListener(listener);
		currentPlotInstance = plotInstance;
	}
}
