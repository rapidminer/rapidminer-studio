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
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class AxisConfigurationContainer extends AbstractConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private ResourceLabel axesFontLabel;
	private JButton axesFontChooserButton;
	private JSpinner domainAxisWidthSpinner;
	private JButton domainAxisLineColorChooserButton;

	private final int fontSize = 12;

	public AxisConfigurationContainer(PlotInstance plotInstance) {
		super(plotInstance);
		createComponents();
		registerAsPlotConfigurationListener();
		adaptGUI();
	}

	private void createComponents() {

		// create axes font row
		{
			axesFontLabel = new ResourceLabel("plotter.configuration_dialog.axes_font");

			axesFontChooserButton = new JButton(new ResourceAction(true, "plotter.configuration_dialog.axis_font") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					createAxesFontDialog();
				}
			});
			axesFontLabel.setLabelFor(axesFontChooserButton);

			addTwoComponentRow(this, axesFontLabel, axesFontChooserButton);
		}

		// add domain axis line color chooser
		{
			JLabel domainAxisLineColorLabel = new ResourceLabel(
					"plotter.configuration_dialog.global_config_panel.axis_color");

			domainAxisLineColorChooserButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.global_config_panel.axis_color") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createDomainAxisColorDialog();
						}
					});
			domainAxisLineColorLabel.setLabelFor(domainAxisLineColorChooserButton);

			addTwoComponentRow(this, domainAxisLineColorLabel, domainAxisLineColorChooserButton);

		}

		// add domain axis width spinner
		{
			JLabel domainAxisWidthLabel = new ResourceLabel("plotter.configuration_dialog.global_config_panel.axis_width");

			domainAxisWidthSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
			domainAxisWidthLabel.setLabelFor(domainAxisWidthSpinner);
			domainAxisWidthSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					Double value = (Double) domainAxisWidthSpinner.getValue();
					getPlotConfiguration().setAxisLineWidth(value.floatValue());
				}

			});

			addTwoComponentRow(this, domainAxisWidthLabel, domainAxisWidthSpinner);
		}

	}

	@Override
	protected void adaptGUI() {

		// init axes font button
		Font axesFont = getPlotConfiguration().getAxesFont();
		if (axesFont != null) {
			axesFontLabel
					.setFont(FontTools.getFont(axesFont.getFamily(), axesFont.getStyle(), fontSize));
		}

		domainAxisLineWidthChanged(getPlotConfiguration().getAxisLineWidth());

	}

	private void domainAxisLineWidthChanged(Float domainAxisLineWidth) {
		domainAxisWidthSpinner.setValue(domainAxisLineWidth.doubleValue());
	}

	private void createDomainAxisColorDialog() {
		Color oldColor = getPlotConfiguration().getAxisLineColor();
		if (oldColor == null) {
			oldColor = Color.black;
		}
		Color newLineColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.plot_background_color_title.label"),
				oldColor);
		if (newLineColor != null && !newLineColor.equals(oldColor)) {
			getPlotConfiguration().setAxisLineColor(newLineColor);
		}
	}

	// private void createItemColorDialog() {
	// if (getSelectedValueSource() != null) {
	// Color itemColor = getSelectedValueSource().getFormat().getItemColor();
	// Color newItemColor = JColorChooser.showDialog(this,
	// I18N.getGUILabel("plotter.configuration_dialog.choose_color.label"), itemColor);
	// if (newItemColor != null) {
	// getSelectedValueSource().getFormat().setItemColor(newItemColor);
	// }
	// }
	// }

	private void createAxesFontDialog() {
		Font axesFont = getPlotConfiguration().getAxesFont();

		if (axesFont == null) {
			axesFont = PlotConfiguration.DEFAULT_AXES_FONT;
		}

		FontDialog fontDialog = new FontDialog(this, axesFont,
				"plotter.configuration_dialog.global_config_panel.select_axes_font");
		fontDialog.setVisible(true);
		fontDialog.requestFocus();
		if (fontDialog.getReturnStatus() == FontDialog.RET_OK) {
			getPlotConfiguration().setAxesFont(fontDialog.getFont());
		}
		fontDialog.dispose();
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		switch (type) {
			case META_CHANGE:
				adaptGUI();
				break;
			case AXES_FONT:
				adaptGUI();
				break;
			case AXIS_LINE_WIDTH:
				adaptGUI();
				break;
			default:
		}

		return true;
	}
}
