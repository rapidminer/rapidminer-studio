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
package com.rapidminer.gui.new_plotter.templates.style;

import com.rapidminer.gui.new_plotter.gui.FontDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * This class provides a GUI for the {@link DefaultPlotterStyleProvider}.
 * 
 * @author Marco Boeck
 * 
 */
public class DefaultPlotterStyleProviderGUI extends JPanel implements Observer {

	/** the panel containing all style settings GUI elements */
	private JPanel stylePanel;

	/** the label with the style provider description text */
	private JLabel descriptionLabel;

	/** the field to input a chart title */
	private JTextField titleField;

	/** the axes font change button */
	private JButton axesFontButton;

	/** the legend font change button */
	private JButton legendFontButton;

	/** the title font change button */
	private JButton titleFontButton;

	/** the plot background color button */
	private JButton plotBackgroundColorButton;

	/** the frame background color button */
	private JButton frameBackgroundColorButton;

	/** the combo box containing the color schemes */
	private JComboBox colorSchemeComboBox;

	/** the checkbox defining whether to show or hide the legend */
	private JCheckBox showLegendCheckBox;

	private static final long serialVersionUID = -6394913829696833045L;

	/**
	 * Creates a new {@link JPanel} to edit the {@link DefaultPlotterStyleProvider} settings.
	 * 
	 * @param defaultStylProvider
	 */
	public DefaultPlotterStyleProviderGUI(final DefaultPlotterStyleProvider defaultStyleProvider) {
		defaultStyleProvider.addObserver(this);
		final Font axesFont = defaultStyleProvider.getAxesFont();
		final Font titleFont = defaultStyleProvider.getTitleFont();
		final Font legendFont = defaultStyleProvider.getLegendFont();

		this.setLayout(new GridBagLayout());
		GridBagConstraints outerPanelGBC = new GridBagConstraints();
		GridBagConstraints stylePanelGBC = new GridBagConstraints();

		// start layout
		outerPanelGBC.gridx = 0;
		outerPanelGBC.gridy = 0;
		outerPanelGBC.insets = new Insets(1, 4, 5, 2);
		outerPanelGBC.anchor = GridBagConstraints.NORTH;
		outerPanelGBC.fill = GridBagConstraints.HORIZONTAL;
		outerPanelGBC.weightx = 1;
		outerPanelGBC.weighty = 0;
		descriptionLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.description.label"));
		this.add(descriptionLabel, outerPanelGBC);

		stylePanel = new JPanel();
		stylePanel.setLayout(new GridBagLayout());

		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 1;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.weighty = 0;
		stylePanelGBC.insets = new Insets(5, 2, 5, 2);
		stylePanelGBC.anchor = GridBagConstraints.WEST;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		JLabel titleLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.title.label"));
		stylePanel.add(titleLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 1;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		titleField = new JTextField();
		titleField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				// update title when focus is lost
				defaultStyleProvider.setTitleText(titleField.getText());
			}
		});
		titleField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// update title when ENTER is pressed
				defaultStyleProvider.setTitleText(titleField.getText());
			}
		});
		titleField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.title.tip"));
		stylePanel.add(titleField, stylePanelGBC);

		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 2;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		JLabel titleFontLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.title.label"));
		stylePanel.add(titleFontLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 2;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		titleFontButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.button.label"));
		titleFontButton.setFont(new Font(titleFont.getFamily(), titleFont.getStyle(),
				DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
		titleFontButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.title.tip"));
		titleFontButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FontDialog fontDialog = new FontDialog(null, defaultStyleProvider.getTitleFont(), "select_font");
				fontDialog.setVisible(true);
				fontDialog.requestFocusInWindow();
				if (fontDialog.getReturnStatus() == FontDialog.RET_OK) {
					if (fontDialog.getFont() != null) {
						Font titleFont = fontDialog.getFont();
						defaultStyleProvider.setTitleFont(titleFont);
						titleFontButton.setFont(new Font(titleFont.getName(), titleFont.getStyle(),
								DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
					}
				}
				fontDialog.dispose();
			}
		});
		stylePanel.add(titleFontButton, stylePanelGBC);

		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 3;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		JLabel axesFontLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.axes.label"));
		stylePanel.add(axesFontLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 3;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		axesFontButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.button.label"));
		axesFontButton.setFont(new Font(axesFont.getFamily(), axesFont.getStyle(),
				DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
		axesFontButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.axes.tip"));
		axesFontButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FontDialog fontDialog = new FontDialog(null, defaultStyleProvider.getAxesFont(), "select_font");
				fontDialog.setVisible(true);
				fontDialog.requestFocusInWindow();
				if (fontDialog.getReturnStatus() == FontDialog.RET_OK) {
					if (fontDialog.getFont() != null) {
						Font axesFont = fontDialog.getFont();
						defaultStyleProvider.setAxesFont(axesFont);
						axesFontButton.setFont(new Font(axesFont.getName(), axesFont.getStyle(),
								DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
					}
				}
				fontDialog.dispose();
			}
		});
		stylePanel.add(axesFontButton, stylePanelGBC);

		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 4;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		JLabel legendFontLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.legend.label"));
		stylePanel.add(legendFontLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 4;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		legendFontButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.button.label"));
		legendFontButton.setFont(new Font(legendFont.getFamily(), legendFont.getStyle(),
				DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
		legendFontButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.font.legend.tip"));
		legendFontButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FontDialog fontDialog = new FontDialog(null, defaultStyleProvider.getLegendFont(), "select_font");
				fontDialog.setVisible(true);
				fontDialog.requestFocusInWindow();
				if (fontDialog.getReturnStatus() == FontDialog.RET_OK) {
					if (fontDialog.getFont() != null) {
						Font legendFont = fontDialog.getFont();
						defaultStyleProvider.setLegendFont(legendFont);
						legendFontButton.setFont(new Font(legendFont.getName(), legendFont.getStyle(),
								DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
					}
				}
				fontDialog.dispose();
			}
		});
		stylePanel.add(legendFontButton, stylePanelGBC);

		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 5;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.gridwidth = 2;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		showLegendCheckBox = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.legend.hide_legend.label"));
		showLegendCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.legend.hide_legend.tip"));
		showLegendCheckBox.setSelected(defaultStyleProvider.isShowLegend());
		showLegendCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				defaultStyleProvider.setShowLegend(showLegendCheckBox.isSelected());
			}
		});
		stylePanel.add(showLegendCheckBox, stylePanelGBC);

		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 6;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.gridwidth = 1;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		JLabel colorLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.colorscheme.label"));
		stylePanel.add(colorLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 6;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		colorSchemeComboBox = new JComboBox(defaultStyleProvider.getColorSchemes().toArray());
		colorSchemeComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.styleprovider.colorscheme.tip"));
		colorSchemeComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				defaultStyleProvider.setSelectedColorSchemeIndex(colorSchemeComboBox.getSelectedIndex());
			}
		});
		stylePanel.add(colorSchemeComboBox, stylePanelGBC);

		JLabel plotBackgroundColorLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.styleprovider.plot_bg_color.label"));
		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 7;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		stylePanel.add(plotBackgroundColorLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 7;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		plotBackgroundColorButton = new JButton(new ResourceAction(true, "plotter.configuration_dialog.select_plot_color") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				createPlotBackgroundColorDialog(defaultStyleProvider);
			}
		});
		stylePanel.add(plotBackgroundColorButton, stylePanelGBC);

		JLabel frameBackgroundColorLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.styleprovider.frame_bg_color.label"));
		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 8;
		stylePanelGBC.weightx = 0;
		stylePanelGBC.fill = GridBagConstraints.NONE;
		stylePanel.add(frameBackgroundColorLabel, stylePanelGBC);

		stylePanelGBC.gridx = 1;
		stylePanelGBC.gridy = 8;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.fill = GridBagConstraints.HORIZONTAL;
		frameBackgroundColorButton = new JButton(
				new ResourceAction(true, "plotter.configuration_dialog.select_frame_color") {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						createFrameBackgroundColorDialog(defaultStyleProvider);
					}
				});
		stylePanel.add(frameBackgroundColorButton, stylePanelGBC);

		// fill empty area
		stylePanelGBC.gridx = 0;
		stylePanelGBC.gridy = 999;
		stylePanelGBC.gridwidth = 2;
		stylePanelGBC.weightx = 1;
		stylePanelGBC.weighty = 1;
		stylePanelGBC.fill = GridBagConstraints.BOTH;
		stylePanel.add(new JLabel(), stylePanelGBC);

		outerPanelGBC.gridx = 0;
		outerPanelGBC.gridy = 1;
		outerPanelGBC.fill = GridBagConstraints.BOTH;
		outerPanelGBC.weightx = 1;
		outerPanelGBC.weighty = 1;
		outerPanelGBC.insets = new Insets(0, 0, 0, 0);
		this.add(stylePanel, stylePanelGBC);
	}

	/**
	 * Shows a dialog where the user can select the frame background color.
	 */
	private void createFrameBackgroundColorDialog(DefaultPlotterStyleProvider styleProvider) {
		Color oldColor = ColorRGB.convertToColor(styleProvider.getFrameBackgroundColor());
		if (oldColor == null) {
			oldColor = Color.white;
		}
		Color newBackgroundColor = JColorChooser.showDialog(null,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.chart_background_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !(newBackgroundColor.equals(oldColor))) {
			styleProvider.setFrameBackgroundColor(ColorRGB.convertColorToColorRGB(newBackgroundColor));
		}
	}

	/**
	 * Shows a dialog where the user can select the plot background color.
	 */
	private void createPlotBackgroundColorDialog(DefaultPlotterStyleProvider styleProvider) {
		Color oldColor = ColorRGB.convertToColor(styleProvider.getPlotBackgroundColor());
		if (oldColor == null) {
			oldColor = Color.WHITE;
		}
		Color newBackgroundColor = JColorChooser.showDialog(null,
				I18N.getGUILabel("plotter.configuration_dialog.global_config_panel.plot_background_color_title.label"),
				oldColor);
		if (newBackgroundColor != null && !(newBackgroundColor.equals(oldColor))) {
			styleProvider.setPlotBackgroundColor(ColorRGB.convertColorToColorRGB(newBackgroundColor));
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof DefaultPlotterStyleProvider) {
			// update ComboBox with ColorSchemes (and make sure no events are fired during that
			// time)
			DefaultPlotterStyleProvider defaultStyleProvider = (DefaultPlotterStyleProvider) o;
			colorSchemeComboBox.setModel(new DefaultComboBoxModel(defaultStyleProvider.getColorSchemes().toArray()));
			ActionListener[] actionListeners = colorSchemeComboBox.getActionListeners();
			for (ActionListener l : actionListeners) {
				colorSchemeComboBox.removeActionListener(l);
			}
			colorSchemeComboBox.setSelectedIndex(defaultStyleProvider.getSelectedColorSchemeIndex());
			for (ActionListener l : actionListeners) {
				colorSchemeComboBox.addActionListener(l);
			}

			// update font buttons
			axesFontButton.setFont(new Font(defaultStyleProvider.getAxesFont().getName(), defaultStyleProvider.getAxesFont()
					.getStyle(), DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
			titleFontButton.setFont(new Font(defaultStyleProvider.getTitleFont().getName(), defaultStyleProvider
					.getTitleFont().getStyle(), DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));
			legendFontButton.setFont(new Font(defaultStyleProvider.getLegendFont().getName(), defaultStyleProvider
					.getLegendFont().getStyle(), DefaultPlotterStyleProvider.FONT_SIZE_DEFAULT));

			// update title field
			titleField.setText(defaultStyleProvider.getTitleText());

			showLegendCheckBox.setSelected(defaultStyleProvider.isShowLegend());
		}
	}

}
