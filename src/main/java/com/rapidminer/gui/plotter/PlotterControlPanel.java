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
package com.rapidminer.gui.plotter;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterChangedListener;
import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.gui.plotter.PlotterPanel.LineStyleCellRenderer;
import com.rapidminer.gui.plotter.settings.ListeningJCheckBox;
import com.rapidminer.gui.plotter.settings.ListeningJComboBox;
import com.rapidminer.gui.plotter.settings.ListeningJSlider;
import com.rapidminer.gui.plotter.settings.ListeningListSelectionModel;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJList;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedListModel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Panel containing control elements for a {@link Plotter}. Depending on the selected plotter type
 * the options panel part is created or adapted. The option panel usually contains selectors for up
 * to three axis and other options depending on the plotter like a plot amount slider or option
 * buttons.
 *
 * @see PlotterPanel
 * @author Simon Fischer, Michael Knopf
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotterControlPanel extends JPanel implements PlotterChangedListener {

	private static final long serialVersionUID = 1L;

	private PlotterConfigurationModel plotterSettings;

	/** The plotter selection combo box. */
	private final PlotterChooser plotterCombo = new PlotterChooser();

	private List<PlotterSettingsChangedListener> changeListenerElements = new LinkedList<>();

	private final transient ItemListener plotterComboListener = e -> {
		Object selectedItem = plotterCombo.getSelectedItem();
		if (selectedItem != null) {
			plotterSettings.setPlotter(selectedItem.toString());
		}
	};

	public PlotterControlPanel(PlotterConfigurationModel plotterSettings) {
		this.plotterSettings = plotterSettings;
		this.plotterCombo.setSettings(plotterSettings);
		this.setLayout(new GridBagLayout());
		updatePlotterCombo();
		updateControls();
	}

	private void updateControls() {

		final Plotter plotter = plotterSettings.getPlotter();
		DataTable dataTable = plotterSettings.getDataTable();
		changeListenerElements = new LinkedList<>();

		// 0. Clear GUI
		removeAll();

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(6, 2, 2, 2);
		c.weightx = 1;

		// 1. register mouse listener on plotter

		final JLabel coordinatesLabel = new JLabel("                      ");
		PlotterMouseHandler mouseHandler = new PlotterMouseHandler(plotter, plotterSettings.getDataTable(),
				coordinatesLabel::setText);
		plotter.addMouseMotionListener(mouseHandler);
		plotter.addMouseListener(mouseHandler);

		// 2. Construct Plotter list
		JLabel label = null;
		String toolTip = null;
		if (plotterSettings.getAvailablePlotters().size() > 1) {
			label = new JLabel(I18N.getGUILabel("plotter_panel.selection.label") + ":");
			this.add(label, c);

			ImageIcon buttonIcon = SwingTools
					.createImage("icons/chartPreview/32/" + plotter.getPlotterName().replace(' ', '_') + ".png");

			plotterCombo.setIcon(buttonIcon);
			plotterCombo.setIconTextGap(6);
			plotterCombo.setVerticalAlignment(SwingConstants.CENTER);
			plotterCombo.setHorizontalAlignment(SwingConstants.LEFT);
			plotterCombo.setHorizontalTextPosition(SwingConstants.RIGHT);
			plotterCombo.setVerticalTextPosition(SwingConstants.CENTER);
			plotterCombo.setPreferredSize(new Dimension(200, 40));
			this.add(plotterCombo, c);

			this.add(createFiller(20), c);
		}

		List<Integer> plottedDimensionList = new LinkedList<>();
		for (int i = 0; i < dataTable.getNumberOfColumns(); i++) {
			if (plotter.getPlotColumn(i)) {
				plottedDimensionList.add(i);
			}
		}

		// 3b. Setup axes selection panel (main)
		final List<JComboBox<String>> axisCombos = new LinkedList<>();
		for (int axisIndex = 0; axisIndex < plotter.getNumberOfAxes(); axisIndex++) {
			toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.select_column_axis.tip",
					plotter.getAxisName(axisIndex));
			label = new JLabel(plotter.getAxisName(axisIndex) + ":");
			label.setToolTipText(toolTip);
			this.add(label, c);
			final int finalAxisIndex = axisIndex;
			final ListeningJComboBox<String> axisCombo = new ListeningJComboBox<>(PlotterAdapter.PARAMETER_SUFFIX_AXIS
					+ PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex)), 200);
			axisCombo.setToolTipText(toolTip);
			axisCombo.setPreferredSize(
					new Dimension(axisCombo.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
			axisCombo.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
			axisCombo.addItem(I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.no_selection.label"));
			for (int j = 0; j < dataTable.getNumberOfColumns(); j++) {
				axisCombo.addItem(dataTable.getColumnName(j));
			}
			changeListenerElements.add(axisCombo);

			axisCombo.addItemListener(e -> {
				String value = PlotterAdapter.PARAMETER_SUFFIX_AXIS
						+ PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex));
				Object selectedItem = axisCombo.getSelectedItem();
				if (selectedItem != null) {
					plotterSettings.setParameterAsString(value, selectedItem.toString());
				}
			});

			this.add(axisCombo, c);
			if (!plotter.isSupportingLogScale(axisIndex)) {
				this.add(createFiller(10), c);
			}
			axisCombos.add(axisCombo);

			// log scale
			if (plotter.isSupportingLogScale(axisIndex)) {
				final ListeningJCheckBox logScaleBox = new ListeningJCheckBox(
						PlotterAdapter.PARAMETER_SUFFIX_AXIS
								+ PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex))
								+ PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE,
						I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.log_scale.label"), false);
				changeListenerElements.add(logScaleBox);
				logScaleBox.addActionListener(e -> plotterSettings.setParameterAsBoolean(PlotterAdapter.PARAMETER_SUFFIX_AXIS
						+ PlotterAdapter.transformParameterName(plotter.getAxisName(finalAxisIndex))
						+ PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE, logScaleBox.isSelected()));
				this.add(logScaleBox, c);
				this.add(createFiller(10), c);
			}
		}

		// 4. Specific settings (colors, values, etc.)
		if (plotter.getValuePlotSelectionType() != Plotter.NO_SELECTION) {
			JLabel plotLabel;
			if (plotter.getPlotName() == null) {
				plotLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.plots.label") + ":");
				toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.select_column.tip");
			} else {
				plotLabel = new JLabel(plotter.getPlotName() + ":");
				toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.select_column_axis.tip",
						plotter.getPlotName());
			}
			plotLabel.setToolTipText(toolTip);
			this.add(plotLabel, c);
		}

		switch (plotter.getValuePlotSelectionType()) {
			case Plotter.MULTIPLE_SELECTION:
				final ExtendedListModel<String> model = new ExtendedListModel<>();
				for (String name : dataTable.getColumnNames()) {
					model.addElement(name,
							I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.select_column_name", name));
				}
				final JList<String> plotList = new ExtendedJList<>(model, 200);
				ListeningListSelectionModel selectionModel = new ListeningListSelectionModel(
						PlotterAdapter.PARAMETER_PLOT_COLUMNS, plotList);
				changeListenerElements.add(selectionModel);
				plotList.setSelectionModel(selectionModel);
				plotList.setToolTipText(toolTip);

				plotList.setCellRenderer(new LineStyleCellRenderer<>(plotter));

				plotList.addListSelectionListener(e -> {
					if (e.getValueIsAdjusting()) {
						return;
					}
					List<String> list = new LinkedList<>();
					for (int i = 0; i < plotList.getModel().getSize(); i++) {
						if (plotList.isSelectedIndex(i)) {
							list.add(model.get(i));
						}
					}
					String result = ParameterTypeEnumeration.transformEnumeration2String(list);

					plotterSettings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMNS, result);
				});
				plotList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				JScrollPane listScrollPane = new ExtendedJScrollPane(plotList);
				listScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Colors.TEXTFIELD_BORDER));
				listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				c.weighty = 1.0;

				this.add(listScrollPane, c);
				c.weighty = 0.0;

				break;
			case Plotter.SINGLE_SELECTION:
				final ListeningJComboBox<String> plotCombo = new ListeningJComboBox<>(PlotterAdapter.PARAMETER_PLOT_COLUMN,
						200);
				plotCombo.setToolTipText(toolTip);
				plotCombo.setPreferredSize(
						new Dimension(plotCombo.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
				plotCombo.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
				plotCombo.addItem(I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.no_selection.label"));

				changeListenerElements.add(plotCombo);
				for (int j = 0; j < dataTable.getNumberOfColumns(); j++) {
					plotCombo.addItem(dataTable.getColumnName(j));
				}
				plotCombo.addItemListener(e -> {
					Object selectedItem = plotCombo.getSelectedItem();
					if (selectedItem != null) {
						plotterSettings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMN, selectedItem.toString());
					}
				});

				this.add(plotCombo, c);

				break;
			case Plotter.NO_SELECTION:
			default:
				// do nothing
				break;
		}

		// log scale
		if (plotter.isSupportingLogScaleForPlotColumns()) {
			final ListeningJCheckBox logScaleBox = new ListeningJCheckBox(
					PlotterAdapter.PARAMETER_PLOT_COLUMNS + PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE,
					I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.log_scale.label"), false);
			changeListenerElements.add(logScaleBox);
			logScaleBox.addActionListener(e -> plotterSettings.setParameterAsBoolean(
					PlotterAdapter.PARAMETER_PLOT_COLUMNS + PlotterAdapter.PARAMETER_SUFFIX_LOG_SCALE,
					logScaleBox.isSelected()));
			this.add(logScaleBox, c);
			this.add(createFiller(10), c);
		}

		// sorting
		if (plotter.isSupportingSorting()) {
			final ListeningJCheckBox sortingBox = new ListeningJCheckBox(PlotterAdapter.PARAMETER_SUFFIX_SORTING,
					I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.sorting.label"), false);
			changeListenerElements.add(sortingBox);
			sortingBox.addActionListener(e -> plotterSettings.setParameterAsBoolean(PlotterAdapter.PARAMETER_SUFFIX_SORTING, sortingBox.isSelected()));
			this.add(sortingBox, c);
			this.add(createFiller(10), c);
		}

		// sorting
		if (plotter.isSupportingAbsoluteValues()) {
			final ListeningJCheckBox absoluteBox = new ListeningJCheckBox(PlotterAdapter.PARAMETER_SUFFIX_ABSOLUTE_VALUES,
					I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.abs_values.label"), false);
			changeListenerElements.add(absoluteBox);
			absoluteBox.addActionListener(e -> plotterSettings.setParameterAsBoolean(
					PlotterAdapter.PARAMETER_SUFFIX_ABSOLUTE_VALUES, absoluteBox.isSelected()));
			this.add(absoluteBox, c);
			this.add(createFiller(10), c);
		}

		// zooming
		if (plotter.canHandleZooming()) {
			label = new JLabel(
					I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.set_zooming_factor.label") + ":");
			toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.set_zooming_factor.tip");
			label.setToolTipText(toolTip);
			this.add(label, c);
			final ListeningJSlider zoomingSlider = new ListeningJSlider(PlotterAdapter.PARAMETER_SUFFIX_ZOOM_FACTOR, 1, 100,
					plotter.getInitialZoomFactor());
			changeListenerElements.add(zoomingSlider);
			zoomingSlider.setToolTipText(toolTip);
			this.add(zoomingSlider, c);
			this.add(createFiller(10), c);
			zoomingSlider.addChangeListener(e -> plotterSettings.setParameterAsInt(
					PlotterAdapter.PARAMETER_SUFFIX_ZOOM_FACTOR, zoomingSlider.getValue()));
		}

		// jitter
		if (plotter.canHandleJitter()) {
			label = new JLabel(
					I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.set_jittering_amount.label") + ":");
			toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.set_jittering_amount.tip");
			label.setToolTipText(toolTip);
			this.add(label, c);
			final ListeningJSlider jitterSlider = new ListeningJSlider(PlotterAdapter.PARAMETER_JITTER_AMOUNT, 0, 10, 0);
			changeListenerElements.add(jitterSlider);
			jitterSlider.setToolTipText(toolTip);
			jitterSlider.setPaintLabels(false);
			jitterSlider.setMajorTickSpacing(10);
			this.add(jitterSlider, c);
			this.add(createFiller(10), c);
			jitterSlider.addChangeListener(e -> plotterSettings.setParameterAsInt(
					PlotterAdapter.PARAMETER_JITTER_AMOUNT, jitterSlider.getValue()));
		}

		// option dialog
		if (plotter.hasOptionsDialog()) {
			toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.open_options_dialog.tip");
			JButton optionsButton = new JButton(
					I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.open_options_dialog.label"));
			optionsButton.setToolTipText(toolTip);
			this.add(optionsButton, c);
			this.add(createFiller(10), c);
			optionsButton.addActionListener(e -> plotter.showOptionsDialog());
		}

		// Add the plotter options components for user interaction, if provided
		int componentCounter = 0;
		while (plotter.getOptionsComponent(componentCounter) != null) {
			Component options = plotter.getOptionsComponent(componentCounter);
			if (options instanceof JComboBox<?>) {
				((JComboBox<?>) options).putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
			} else {
				options.setBackground(Colors.WHITE);
			}
			this.add(options, c);
			componentCounter++;
		}

		// coordinates
		if (plotter.isProvidingCoordinates()) {
			toolTip = I18N.getMessage(I18N.getGUIBundle(), "gui.label.plotter_panel.coordinates.label");
			coordinatesLabel.setToolTipText(toolTip);
			coordinatesLabel.setBorder(BorderFactory.createEtchedBorder());
			coordinatesLabel.setFont(FontTools.getFont(Font.MONOSPACED, Font.PLAIN, coordinatesLabel.getFont().getSize()));
			this.add(coordinatesLabel, c);
		}

		// add fill component if necessary (glue)
		if (plotter.getValuePlotSelectionType() != Plotter.MULTIPLE_SELECTION) {
			c.weighty = 1.0;
			this.add(new JLabel(), c);
			c.weighty = 0.0;
		}

		this.setAlignmentX(LEFT_ALIGNMENT);

		revalidate();
		repaint();

	}

	public void updatePlotterCombo() {
		plotterCombo.removeItemListener(plotterComboListener);
		plotterCombo.removeAllItems();
		plotterSettings.getAvailablePlotters().forEach((plotterName, plotterClass) -> {
			try {
				if (plotterClass != null) {
					plotterCombo.addItem(plotterName);
				}
			} catch (IllegalArgumentException | SecurityException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.gui.plotter.PlotterControlPanel.instatiating_plotter_error", plotterName);
			}
		});
		plotterCombo.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.plotter_panel.select_chart.tip"));
		plotterCombo.addItemListener(plotterComboListener);
	}

	@Override
	public List<PlotterSettingsChangedListener> getListeningObjects() {
		return changeListenerElements;
	}

	@Override
	public void plotterChanged(String plotterName) {
		plotterCombo.setSelectedItem(plotterName);
		updateControls();
	}

	/**
	 * Creates a {@link JLabel} which has a preferred height of the specified value to create some
	 * spacing
	 *
	 * @param height
	 *            pref height
	 * @return the filler label
	 */
	private static JLabel createFiller(final int height) {
		return new JLabel() {

			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(super.getPreferredSize().width, height);
			}
		};
	}

}
