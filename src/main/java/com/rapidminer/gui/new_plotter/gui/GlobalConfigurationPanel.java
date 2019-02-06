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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jfree.chart.plot.PlotOrientation;

import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.ColorSchemeComboBoxRenderer;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.EnumComboBoxCellRenderer;
import com.rapidminer.gui.popup.PopupAction;
import com.rapidminer.gui.popup.PopupAction.PopupPosition;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent.PlotConfigurationChangeType;
import com.rapidminer.gui.new_plotter.templates.style.ColorScheme;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class GlobalConfigurationPanel extends AbstractConfigurationPanel {

	private static final long serialVersionUID = 1L;

	private JTextField titleTextField;

	private JComboBox<PlotOrientation> plotOrientationComboBox;

	private final int fontSize = 12;

	private JToggleButton titleConfigButton;

	private AxisConfigurationContainer axisConfigurationContainer;

	private LegendConfigurationPanel legendConfigContainer;

	private JButton plotBackgroundColorChooserButton;
	private JButton frameBackgroundColorChooserButton;
	private JLabel plotBackGroundColorLabel;
	private JLabel frameBackGroundColorLabel;

	private DefaultComboBoxModel<ColorScheme> colorsSchemesComboBoxModel;
	private JComboBox<ColorScheme> colorSchemesComboBox;

	private ChartTitleConfigurationContainer chartTitleConfigurationContainer;

	public GlobalConfigurationPanel(PlotInstance plotInstance) {
		super(plotInstance);
		axisConfigurationContainer = new AxisConfigurationContainer(plotInstance);
		addPlotInstanceChangeListener(axisConfigurationContainer);
		legendConfigContainer = new LegendConfigurationPanel(plotInstance);
		addPlotInstanceChangeListener(legendConfigContainer);
		chartTitleConfigurationContainer = new ChartTitleConfigurationContainer(getCurrentPlotInstance());
		addPlotInstanceChangeListener(chartTitleConfigurationContainer);
		createComponents();
		registerAsPlotConfigurationListener();
		adaptGUI();
	}

	private void createComponents() {

		// create panel for global configuration

		{
			// add title label
			JLabel titleLabel = new ResourceLabel("plotter.configuration_dialog.chart_title");

			String title = getPlotConfiguration().getTitleText();
			if (title == null) {
				title = "";
			}

			titleTextField = new JTextField(title);
			titleLabel.setLabelFor(titleTextField);
			titleTextField.addKeyListener(new KeyListener() {

				@Override
				public void keyTyped(KeyEvent e) {
					return;
				}

				@Override
				public void keyReleased(KeyEvent e) {
					String newTitle = titleTextField.getText();
					String titleText = getCurrentPlotInstance().getMasterPlotConfiguration().getTitleText();
					if (titleText != null) {
						if (!titleText.equals(newTitle) || titleText == null && newTitle.length() > 0) {
							if (newTitle.length() > 0) {
								getPlotConfiguration().setTitleText(newTitle);
							} else {
								getPlotConfiguration().setTitleText(null);
							}
						}
					} else {
						if (newTitle.length() > 0) {
							getPlotConfiguration().setTitleText(newTitle);
						} else {
							getPlotConfiguration().setTitleText(null);
						}
					}
				}

				@Override
				public void keyPressed(KeyEvent e) {
					return;
				}
			});
			titleTextField.setPreferredSize(new Dimension(115, 23));

			titleConfigButton = new JToggleButton(new PopupAction(true, "plotter.configuration_dialog.open_popup",
					chartTitleConfigurationContainer, PopupPosition.HORIZONTAL));

			addThreeComponentRow(this, titleLabel, titleTextField, titleConfigButton);
		}

		// add orientation check box
		{
			JLabel plotOrientationLabel = new ResourceLabel(
					"plotter.configuration_dialog.global_config_panel.plot_orientation");

			PlotOrientation[] orientations = { PlotOrientation.HORIZONTAL, PlotOrientation.VERTICAL };
			plotOrientationComboBox = new JComboBox<>(orientations);
			plotOrientationLabel.setLabelFor(plotOrientationComboBox);
			plotOrientationComboBox.setRenderer(new EnumComboBoxCellRenderer<>("plotter"));
			plotOrientationComboBox.setSelectedIndex(0);
			plotOrientationComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					getPlotConfiguration().setOrientation((PlotOrientation) plotOrientationComboBox.getSelectedItem());
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;
				}
			});

			addTwoComponentRow(this, plotOrientationLabel, plotOrientationComboBox);
		}

		// add legend popup button
		{
			JLabel legendStyleConfigureLabel = new ResourceLabel(
					"plotter.configuration_dialog.global_config_panel.legend_style");

			JToggleButton legendStyleConfigButton = new JToggleButton(new PopupAction(true,
					"plotter.configuration_dialog.open_popup", legendConfigContainer, PopupAction.PopupPosition.HORIZONTAL));
			legendStyleConfigureLabel.setLabelFor(legendStyleConfigButton);

			addTwoComponentRow(this, legendStyleConfigureLabel, legendStyleConfigButton);

		}

		// add legend popup button
		{
			JLabel axisStyleConfigureLabel = new ResourceLabel(
					"plotter.configuration_dialog.global_config_panel.axis_style");

			JToggleButton axisStyleConfigureButton = new JToggleButton(
					new PopupAction(true, "plotter.configuration_dialog.open_popup", axisConfigurationContainer,
							PopupAction.PopupPosition.HORIZONTAL));
			axisStyleConfigureLabel.setLabelFor(axisStyleConfigureButton);

			addTwoComponentRow(this, axisStyleConfigureLabel, axisStyleConfigureButton);
		}

		// add color scheme dialog button
		{
			JLabel colorConfigureLabel = new ResourceLabel("plotter.configuration_dialog.global_config_panel.color_scheme");

			colorsSchemesComboBoxModel = new DefaultComboBoxModel<>();
			colorSchemesComboBox = new JComboBox<>(colorsSchemesComboBoxModel);
			colorConfigureLabel.setLabelFor(colorSchemesComboBox);
			colorSchemesComboBox.setRenderer(new ColorSchemeComboBoxRenderer());
			colorSchemesComboBox.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					return;

				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					ColorScheme colorScheme = (ColorScheme) colorSchemesComboBox.getSelectedItem();
					if (colorScheme != null) {
						getPlotConfiguration().setActiveColorScheme(colorScheme.getName());
					}
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
					return;
				}
			});

			JButton colorConfigButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.open_color_scheme_dialog") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createColorSchemeDialog();
						}
					});

			addThreeComponentRow(this, colorConfigureLabel, colorSchemesComboBox, colorConfigButton);

		}

		// add plot background color
		{
			plotBackGroundColorLabel = new ResourceLabel(
					"plotter.configuration_dialog.global_config_panel.select_plot_background_color");

			plotBackgroundColorChooserButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.select_plot_color") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createPlotBackgroundColorDialog();

						}
					});
			plotBackGroundColorLabel.setLabelFor(plotBackgroundColorChooserButton);

			addTwoComponentRow(this, plotBackGroundColorLabel, plotBackgroundColorChooserButton);

		}

		// add chart background color
		{
			frameBackGroundColorLabel = new ResourceLabel(
					"plotter.configuration_dialog.global_config_panel.select_frame_background_color");

			frameBackgroundColorChooserButton = new JButton(
					new ResourceAction(true, "plotter.configuration_dialog.select_frame_color") {

						private static final long serialVersionUID = 1L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							createFrameBackgroundColorDialog();
						}
					});
			frameBackGroundColorLabel.setLabelFor(frameBackgroundColorChooserButton);

			addTwoComponentRow(this, frameBackGroundColorLabel, frameBackgroundColorChooserButton);

			// GridBagConstraints itemConstraint = new GridBagConstraints();
			// itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
			// itemConstraint.weightx = 1.0;
			// this.add(frameBackgroundColorChooserButton, itemConstraint);

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

	@Override
	protected void adaptGUI() {

		// init title textfield
		String title = getPlotConfiguration().getTitleText();
		if (title == null) {
			title = "";
		}
		if (!title.equals(titleTextField.getText())) {
			titleTextField.setText(title);
		}

		// init title font button
		Font titleFont = getPlotConfiguration().getTitleFont();
		if (titleFont != null) {
			titleTextField
					.setFont(FontTools.getFont(titleFont.getFamily(), titleFont.getStyle(), fontSize));
		}

		plotOrientationChanged(getPlotConfiguration().getOrientation());

		// init plot background color label
		Color backgroundColor = getPlotConfiguration().getPlotBackgroundColor();
		if (backgroundColor == null) {
			backgroundColor = Color.white;
		}
		// plotBackgroundColorChooserButton.setIcon(createColoredRectangleIcon(backgroundColor));

		// init chart background color label
		Color chartBackgroundColor = getPlotConfiguration().getChartBackgroundColor();
		if (chartBackgroundColor == null) {
			chartBackgroundColor = Color.white;
		}
		// frameBackgroundColorChooserButton.setIcon(createColoredRectangleIcon(chartBackgroundColor));

		// init colors schemes comobox
		Map<String, ColorScheme> colorSchemes = getPlotConfiguration().getColorSchemes();
		colorsSchemesComboBoxModel.removeAllElements();
		for (ColorScheme scheme : colorSchemes.values()) {
			colorsSchemesComboBoxModel.addElement(scheme);
		}
		colorsSchemesComboBoxModel.setSelectedItem(getPlotConfiguration().getActiveColorScheme());

	}

	private void plotOrientationChanged(PlotOrientation orientation) {
		plotOrientationComboBox.setSelectedItem(orientation);
	}

	private void createPlotBackgroundColorDialog() {
		Color oldColor = getPlotConfiguration().getPlotBackgroundColor();
		if (oldColor == null) {
			oldColor = Color.white;
		}
		Color newBackgroundColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.plot_background_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !newBackgroundColor.equals(oldColor)) {
			getPlotConfiguration().setPlotBackgroundColor(newBackgroundColor);
		}
	}

	private void createFrameBackgroundColorDialog() {
		Color oldColor = getPlotConfiguration().getChartBackgroundColor();
		if (oldColor == null) {
			oldColor = Color.white;
		}
		Color newBackgroundColor = JColorChooser.showDialog(this,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.chart_background_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !newBackgroundColor.equals(oldColor)) {
			getPlotConfiguration().setFrameBackgroundColor(newBackgroundColor);
		}
	}

	private void createColorSchemeDialog() {
		ColorSchemeDialog colorSchemeDialog = new ColorSchemeDialog(this, "plotter.configuration_dialog.color_scheme_dialog",
				getPlotConfiguration());
		colorSchemeDialog.setVisible(true);
	}

	@Override
	public boolean plotConfigurationChanged(PlotConfigurationChangeEvent change) {
		PlotConfigurationChangeType type = change.getType();
		switch (type) {
			case CHART_TITLE:
				adaptGUI();
				break;
			case PLOT_ORIENTATION:
				adaptGUI();
				break;
			case PLOT_BACKGROUND_COLOR:
				adaptGUI();
				break;
			case FRAME_BACKGROUND_COLOR:
				adaptGUI();
				break;
			case COLOR_SCHEME:
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
