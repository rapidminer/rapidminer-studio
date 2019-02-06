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
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration;
import com.rapidminer.gui.new_plotter.configuration.LegendConfiguration.LegendPosition;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler, Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class LegendConfigurationPanel extends AbstractConfigurationPanel {

	private static final long serialVersionUID = 1L;

	private final int fontSize = 12;

	private JComboBox<LegendPosition> legendPositionComboBox;

	private ResourceLabel legendFontLabel;
	private ResourceLabel showDimensionTypeLabel;
	private JCheckBox showDimensionTypeCheckBox;

	private JButton legendFontChooserButton;

	private ResourceLabel legendBackGroundColorLabel;

	private JButton legendBackgroundColorChooserButton;

	private ResourceLabel legendFrameColorLabel;

	private JButton legendFrameColorChooserButton;

	private ResourceLabel showLegendFrameLabel;

	private JCheckBox showLegendFrameCheckBox;

	private ResourceLabel legendFontColorLabel;

	private JButton legendFontColorChooserButton;

	public LegendConfigurationPanel(PlotInstance plotInstance) {
		super(plotInstance);
		createComponents();
		registerAsPlotConfigurationListener();
		adaptGUI();
	}

	private void createComponents() {
		{
			JLabel legendPositionLabel = new ResourceLabel("plotter.configuration_dialog.legend_position");

			legendPositionComboBox = new JComboBox<>(LegendPosition.values());
			legendPositionLabel.setLabelFor(legendPositionComboBox);
			legendPositionComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter.legendposition"));
			legendPositionComboBox.setSelectedIndex(0);
			legendPositionComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					getPlotConfiguration().getLegendConfiguration()
							.setLegendPosition((LegendPosition) legendPositionComboBox.getSelectedItem());
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;
				}
			});

			addTwoComponentRow(this, legendPositionLabel, legendPositionComboBox);

		}

		// create legend font row
		{
			legendFontLabel = new ResourceLabel("plotter.configuration_dialog.legend_font");

			legendFontChooserButton = new JButton(new ResourceAction(true, "plotter.configuration_dialog.legend_font") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					createLegendFontDialog();
				}
			});
			legendFontLabel.setLabelFor(legendFontChooserButton);

			addTwoComponentRow(this, legendFontLabel, legendFontChooserButton);
		}

		// add legend font color
		{
			legendFontColorLabel = new ResourceLabel(
					"plotter.configuration_dialog.legend_color_dialog.select_legend_font_color");

			legendFontColorChooserButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.select_legend_font_color") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createLegendFontColorDialog();
						}

					});
			legendFontColorLabel.setLabelFor(legendFontColorChooserButton);

			addTwoComponentRow(this, legendFontColorLabel, legendFontColorChooserButton);

		}

		// create show dimension type row
		{
			showDimensionTypeLabel = new ResourceLabel("plotter.configuration_dialog.show_dimension_type");

			showDimensionTypeCheckBox = new JCheckBox();
			showDimensionTypeLabel.setLabelFor(showDimensionTypeCheckBox);
			showDimensionTypeCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					getPlotConfiguration().getLegendConfiguration()
							.setShowDimensionType(showDimensionTypeCheckBox.isSelected());
				}
			});

			addTwoComponentRow(this, showDimensionTypeLabel, showDimensionTypeCheckBox);
		}

		// add legend background color
		{
			legendBackGroundColorLabel = new ResourceLabel(
					"plotter.configuration_dialog.legend_color_dialog.select_legend_background_color");

			legendBackgroundColorChooserButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.select_legend_background_color") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createLegendBackgroundColorDialog();

						}

					});
			legendBackGroundColorLabel.setLabelFor(legendBackgroundColorChooserButton);

			addTwoComponentRow(this, legendBackGroundColorLabel, legendBackgroundColorChooserButton);

		}

		// create show dimension type row
		{
			showLegendFrameLabel = new ResourceLabel("plotter.configuration_dialog.show_legend_frame");

			showLegendFrameCheckBox = new JCheckBox();
			showLegendFrameLabel.setLabelFor(showLegendFrameCheckBox);
			showLegendFrameCheckBox.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					getPlotConfiguration().getLegendConfiguration().setShowLegendFrame(showLegendFrameCheckBox.isSelected());
				}
			});

			addTwoComponentRow(this, showLegendFrameLabel, showLegendFrameCheckBox);
		}

		// add legend frame color
		{
			legendFrameColorLabel = new ResourceLabel(
					"plotter.configuration_dialog.legend_color_dialog.select_legend_frame_color");

			legendFrameColorChooserButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.select_legend_frame_color") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createLegendFrameColorDialog();

						}

					});
			legendFrameColorLabel.setLabelFor(legendFrameColorChooserButton);

			addTwoComponentRow(this, legendFrameColorLabel, legendFrameColorChooserButton);

		}
	}

	private void createLegendBackgroundColorDialog() {
		Color oldColor = getPlotConfiguration().getLegendConfiguration().getLegendBackgroundColor();
		if (oldColor == null) {
			oldColor = LegendConfiguration.DEFAULT_LEGEND_BACKGROUND_COLOR;
		}
		Color newBackgroundColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.legend_background_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !newBackgroundColor.equals(oldColor)) {
			getPlotConfiguration().getLegendConfiguration().setLegendBackgroundColor(newBackgroundColor);
		}
	}

	private void createLegendFontColorDialog() {
		Color oldColor = getPlotConfiguration().getLegendConfiguration().getLegendBackgroundColor();
		if (oldColor == null) {
			oldColor = LegendConfiguration.DEFAULT_LEGEND_FONT_COLOR;
		}
		Color newBackgroundColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.legend_font_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !newBackgroundColor.equals(oldColor)) {
			getPlotConfiguration().getLegendConfiguration().setLegendFontColor(newBackgroundColor);
		}
	}

	private void createLegendFrameColorDialog() {
		Color oldColor = getPlotConfiguration().getLegendConfiguration().getLegendFrameColor();
		if (oldColor == null) {
			oldColor = LegendConfiguration.DEFAULT_LEGEND_FRAME_COLOR;
		}
		Color newBackgroundColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.legend_frame_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !newBackgroundColor.equals(oldColor)) {
			getPlotConfiguration().getLegendConfiguration().setLegendFrameColor(newBackgroundColor);
		}
	}

	@Override
	protected void adaptGUI() {

		// init legend font button
		Font legendFont = getPlotConfiguration().getLegendConfiguration().getLegendFont();
		if (legendFont != null) {
			legendFontLabel
					.setFont(FontTools.getFont(legendFont.getFamily(), legendFont.getStyle(), fontSize));
		}

		// init combo box selected item
		legendPositionComboBox.setSelectedItem(getPlotConfiguration().getLegendConfiguration().getLegendPosition());

		showDimensionTypeCheckBox.setSelected(getPlotConfiguration().getLegendConfiguration().isShowDimensionType());

		showLegendFrameCheckBox.setSelected(getPlotConfiguration().getLegendConfiguration().isShowLegendFrame());
	}

	private void createLegendFontDialog() {
		Font legendFont = getPlotConfiguration().getLegendConfiguration().getLegendFont();

		if (legendFont == null) {
			legendFont = LegendConfiguration.DEFAULT_LEGEND_FONT;
		}

		FontDialog fontDialog = new FontDialog(this, legendFont,
				"plotter.configuration_dialog.global_config_panel.select_legend_font");
		fontDialog.setVisible(true);
		fontDialog.requestFocus();
		if (fontDialog.getReturnStatus() == FontDialog.RET_OK) {
			getPlotConfiguration().getLegendConfiguration().setLegendFont(fontDialog.getFont());
		}
		fontDialog.dispose();
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		switch (type) {
			case LEGEND_CHANGED:
				adaptGUI();
				break;
			case META_CHANGE:
				adaptGUI();
				break;
			default:
		}

		return true;
	}
}
