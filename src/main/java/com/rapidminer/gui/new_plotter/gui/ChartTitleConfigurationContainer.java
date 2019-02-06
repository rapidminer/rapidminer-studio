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

import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class ChartTitleConfigurationContainer extends AbstractConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private ResourceLabel titleFontLabel;
	private JButton titleFontChooserButton;
	private JButton titleColorChooserButton;

	public ChartTitleConfigurationContainer(PlotInstance plotInstance) {
		super(plotInstance);
		createComponents();
		adaptGUI();
	}

	private void createComponents() {

		// create axes font row
		{
			titleFontLabel = new ResourceLabel("plotter.configuration_dialog.title_font");

			titleFontChooserButton = new JButton(new ResourceAction(true, "plotter.configuration_dialog.title_font") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					createTitleFontDialog();
				}
			});
			titleFontLabel.setLabelFor(titleFontChooserButton);

			addTwoComponentRow(this, titleFontLabel, titleFontChooserButton);
		}

		// add domain axis line color chooser
		{
			JLabel titleColorLabel = new ResourceLabel("plotter.configuration_dialog.title_color");

			titleColorChooserButton = new JButton(new ResourceAction(true, "plotter.configuration_dialog.title_color") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					createTitleColorDialog();
				}
			});
			titleColorLabel.setLabelFor(titleColorChooserButton);

			addTwoComponentRow(this, titleColorLabel, titleColorChooserButton);

		}

	}

	@Override
	protected void adaptGUI() {

	}

	private void createTitleColorDialog() {
		Color oldColor = getPlotConfiguration().getTitleColor();
		if (oldColor == null) {
			oldColor = PlotConfiguration.DEFAULT_TITLE_COLOR;
		}
		Color newLineColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.title_font_color_title.label"), oldColor);
		if (newLineColor != null && !newLineColor.equals(oldColor)) {
			getPlotConfiguration().setTitleColor(newLineColor);
		}
	}

	private void createTitleFontDialog() {
		Font titleFont = getPlotConfiguration().getTitleFont();

		if (titleFont == null) {
			titleFont = FontTools.getFont(Font.DIALOG, Font.PLAIN, 10);
		}

		FontDialog fontDialog = new FontDialog(this, titleFont,
				"plotter.configuration_dialog.global_config_panel.select_title_font");

		fontDialog.setVisible(true);
		fontDialog.requestFocus();
		if (fontDialog.getReturnStatus() == FontDialog.RET_OK) {
			Font font = fontDialog.getFont();
			getPlotConfiguration().setTitleFont(font);
		}
		fontDialog.dispose();
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		return true;
	}
}
