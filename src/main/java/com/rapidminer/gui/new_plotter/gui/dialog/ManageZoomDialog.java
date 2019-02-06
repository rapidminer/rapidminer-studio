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
package com.rapidminer.gui.new_plotter.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.configuration.RangeAxisConfig;
import com.rapidminer.gui.new_plotter.data.DimensionConfigData;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection.SelectionType;
import com.rapidminer.gui.new_plotter.utility.ContinuousColorProvider;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.Pair;


/**
 * This dialog allows the user to manually zoom in/do a selection on the chart.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class ManageZoomDialog extends JDialog {

	/** the ok {@link JButton} */
	private JButton okButton;

	/** the cancel {@link JButton} */
	private JButton cancelButton;

	/** the restore color {@link JButton} */
	private JButton restoreColorButton;

	/**
	 * the {@link JComboBox} where the {@link RangeAxisConfig} will be selected for y axis
	 * zoom/selection
	 */
	private JComboBox<RangeAxisConfig> rangeAxisSelectionCombobox;

	/** the {@link JTextField} for the x value */
	private JTextField domainRangeLowerBoundField;

	/** the {@link JTextField} for the y value */
	private JTextField domainRangeUpperBoundField;

	/** the {@link JTextField} for the width value */
	private JTextField valueRangeLowerBoundField;

	/** the {@link JTextField} for the height value */
	private JTextField valueRangeUpperBoundField;

	/** the {@link JTextField} for the min color value */
	private JTextField colorMinValueField;

	/** the {@link JTextField} for the max color value */
	private JTextField colorMaxValueField;

	/** the domain axis range lower bound the user specified */
	private double domainRangeLowerBound;

	/** the domain axis upper bound the user specified */
	private double domainRangeUpperBound;

	/** the value axis lower bound the user specified */
	private double valueRangeLowerBound;

	/** the value axis upper bound the user specified */
	private double valueRangeUpperBound;

	/** the min value for the color provider */
	private double colorMinValue;

	/** the max value for the color provider */
	private double colorMaxValue;

	/** if selected, it will zoom */
	private JRadioButton zoomRadiobutton;

	/** if selected, instead of zooming there will be a selection */
	private JRadioButton selectionRadiobutton;

	/** the current {@link JFreeChartPlotEngine} */
	private JFreeChartPlotEngine engine;

	private static final long serialVersionUID = 1932257219370926682L;

	/**
	 * Creates a new {@link AddParallelLineDialog}.
	 */
	public ManageZoomDialog() {
		super(ApplicationFrame.getApplicationFrame());
		domainRangeLowerBound = 0.0;
		domainRangeUpperBound = 0.0;
		valueRangeLowerBound = 0.0;
		valueRangeUpperBound = 0.0;
		colorMinValue = 0.0;
		colorMaxValue = 0.0;

		setupGUI();
	}

	/**
	 * Setup the GUI.
	 */
	private void setupGUI() {
		JPanel mainPanel = new JPanel();
		this.setContentPane(mainPanel);

		// start layout
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 2, 5);
		JPanel radioPanel = new JPanel();
		radioPanel.setLayout(new BorderLayout());
		zoomRadiobutton = new JRadioButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.zoom.label"));
		zoomRadiobutton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.zoom.tip"));
		zoomRadiobutton.setSelected(true);
		radioPanel.add(zoomRadiobutton, BorderLayout.LINE_START);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		selectionRadiobutton = new JRadioButton(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.selection.label"));
		selectionRadiobutton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.selection.tip"));
		selectionRadiobutton.setHorizontalAlignment(SwingConstants.CENTER);
		radioPanel.add(selectionRadiobutton, BorderLayout.LINE_END);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		this.add(radioPanel, gbc);

		ButtonGroup group = new ButtonGroup();
		group.add(zoomRadiobutton);
		group.add(selectionRadiobutton);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.CENTER;
		rangeAxisSelectionCombobox = new JComboBox<>();
		rangeAxisSelectionCombobox
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.range_axis_combobox.tip"));
		rangeAxisSelectionCombobox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateValueRange();
			}
		});
		this.add(rangeAxisSelectionCombobox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 1;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 2, 5);
		JLabel domainRangeLowerBoundLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.domain_lower_bound.label"));
		this.add(domainRangeLowerBoundLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.insets = new Insets(2, 5, 2, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		domainRangeLowerBoundField = new JTextField();
		domainRangeLowerBoundField.setText(String.valueOf(domainRangeLowerBound));
		domainRangeLowerBoundField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyDomainRangeLowerBoundInput(input);
			}
		});
		domainRangeLowerBoundField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.domain_lower_bound.tip"));
		this.add(domainRangeLowerBoundField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.NONE;
		JLabel domainRangeUpperBoundLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.domain_upper_bound.label"));
		this.add(domainRangeUpperBoundLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		domainRangeUpperBoundField = new JTextField();
		domainRangeUpperBoundField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.domain_upper_bound.tip"));
		domainRangeUpperBoundField.setText(String.valueOf(domainRangeUpperBound));
		domainRangeUpperBoundField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyDomainRangeUpperBoundInput(input);
			}
		});
		this.add(domainRangeUpperBoundField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.fill = GridBagConstraints.NONE;
		JLabel valueRangeLowerBoundLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.value_lower_bound.label"));
		this.add(valueRangeLowerBoundLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		valueRangeLowerBoundField = new JTextField();
		valueRangeLowerBoundField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.value_lower_bound.tip"));
		valueRangeLowerBoundField.setText(String.valueOf(valueRangeLowerBound));
		valueRangeLowerBoundField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyValueRangeLowerBoundInput(input);
			}
		});
		this.add(valueRangeLowerBoundField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.fill = GridBagConstraints.NONE;
		JLabel valueRangeUpperBoundLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.value_upper_bound.label"));
		this.add(valueRangeUpperBoundLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		valueRangeUpperBoundField = new JTextField();
		valueRangeUpperBoundField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.value_upper_bound.tip"));
		valueRangeUpperBoundField.setText(String.valueOf(valueRangeUpperBound));
		valueRangeUpperBoundField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyValueRangeUpperBoundInput(input);
			}
		});
		this.add(valueRangeUpperBoundField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		this.add(new JSeparator(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 7;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		JLabel colorMinValueLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.color_min_value.label"));
		this.add(colorMinValueLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 7;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		colorMinValueField = new JTextField();
		colorMinValueField
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.color_min_value.tip"));
		colorMinValueField.setText(String.valueOf(colorMinValue));
		colorMinValueField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyColorInput(input);
			}
		});
		colorMinValueField.setEnabled(false);
		this.add(colorMinValueField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 8;
		gbc.fill = GridBagConstraints.NONE;
		JLabel colorMaxValueLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.action.manage_zoom.color_max_value.label"));
		this.add(colorMaxValueLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 8;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		colorMaxValueField = new JTextField();
		colorMaxValueField
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.color_max_value.tip"));
		colorMaxValueField.setText(String.valueOf(colorMaxValue));
		colorMaxValueField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyColorInput(input);
			}
		});
		colorMaxValueField.setEnabled(false);
		this.add(colorMaxValueField, gbc);

		gbc.gridx = 1;
		gbc.gridy = 9;
		gbc.fill = GridBagConstraints.NONE;
		restoreColorButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.restore_color.label"));
		restoreColorButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.restore_color.tip"));
		restoreColorButton.setIcon(SwingTools.createIcon("16/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.restore_color.icon")));
		restoreColorButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LinkAndBrushSelection linkAndBrushSelection = new LinkAndBrushSelection(SelectionType.RESTORE_COLOR,
						new LinkedList<Pair<Integer, Range>>(), new LinkedList<Pair<Integer, Range>>(), null, null, engine
								.getPlotInstance());
				engine.getChartPanel().informLinkAndBrushSelectionListeners(linkAndBrushSelection);
				updateColorValues();
			}
		});
		restoreColorButton.setEnabled(false);
		this.add(restoreColorButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 10;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(2, 5, 5, 5);
		this.add(new JSeparator(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 11;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		okButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.ok.label"));
		okButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.ok.tip"));
		okButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.ok.icon")));
		okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.ok.mne").toCharArray()[0]);
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// make sure fields have correct values
				boolean fieldsPassedChecks = checkFields();
				if (!fieldsPassedChecks) {
					return;
				}

				Object selectedItem = rangeAxisSelectionCombobox.getSelectedItem();
				if (selectedItem != null && selectedItem instanceof RangeAxisConfig) {
					RangeAxisConfig config = (RangeAxisConfig) selectedItem;
					boolean zoomOnLinkAndBrushSelection = engine.getChartPanel().getZoomOnLinkAndBrushSelection();
					Range domainRange = new Range(Double.parseDouble(domainRangeLowerBoundField.getText()), Double
							.parseDouble(domainRangeUpperBoundField.getText()));
					Range valueRange = new Range(Double.parseDouble(valueRangeLowerBoundField.getText()), Double
							.parseDouble(valueRangeUpperBoundField.getText()));
					LinkedList<Pair<Integer, Range>> domainRangeList = new LinkedList<Pair<Integer, Range>>();
					// only add domain zoom if != 0
					if (domainRange.getUpperBound() != 0) {
						domainRangeList.add(new Pair<Integer, Range>(0, domainRange));
					}
					LinkedList<Pair<Integer, Range>> valueRangeList = new LinkedList<Pair<Integer, Range>>();
					// only add range zoom if at least one RangeAxisConfig exists
					if (engine.getPlotInstance().getMasterPlotConfiguration().getRangeAxisConfigs().size() > 0) {
						if (valueRange.getUpperBound() != 0) {
							// only add value zoom if != 0
							valueRangeList.add(new Pair<Integer, Range>(engine.getPlotInstance()
									.getMasterPlotConfiguration().getIndexOfRangeAxisConfigById(config.getId()), valueRange));
						}
					}

					// zoom or select or color
					LinkAndBrushSelection linkAndBrushSelection = null;
					if (zoomRadiobutton.isSelected()) {
						linkAndBrushSelection = new LinkAndBrushSelection(SelectionType.ZOOM_IN, domainRangeList,
								valueRangeList);
						if (isColorChanged(Double.parseDouble(colorMinValueField.getText()),
								Double.parseDouble(colorMaxValueField.getText()))) {
							linkAndBrushSelection = new LinkAndBrushSelection(SelectionType.COLOR_ZOOM, domainRangeList,
									valueRangeList, Double.parseDouble(colorMinValueField.getText()), Double
											.parseDouble(colorMaxValueField.getText()), engine.getPlotInstance());
						}
					} else if (selectionRadiobutton.isSelected()) {
						linkAndBrushSelection = new LinkAndBrushSelection(SelectionType.SELECTION, domainRangeList,
								valueRangeList);
						if (isColorChanged(Double.parseDouble(colorMinValueField.getText()),
								Double.parseDouble(colorMaxValueField.getText()))) {
							linkAndBrushSelection = new LinkAndBrushSelection(SelectionType.COLOR_SELECTION,
									domainRangeList, valueRangeList, Double.parseDouble(colorMinValueField.getText()),
									Double.parseDouble(colorMaxValueField.getText()), engine.getPlotInstance());
						}
					}
					engine.getChartPanel().informLinkAndBrushSelectionListeners(linkAndBrushSelection);
					engine.getChartPanel().setZoomOnLinkAndBrushSelection(zoomOnLinkAndBrushSelection);
				} else {
					return;
				}

				ManageZoomDialog.this.dispose();
			}
		});
		okButton.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					okButton.doClick();
				}
			}
		});
		this.add(okButton, gbc);

		gbc.gridx = 2;
		gbc.gridy = 11;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		cancelButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.cancel.label"));
		cancelButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.cancel.tip"));
		cancelButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.cancel.icon")));
		cancelButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.cancel.mne").toCharArray()[0]);
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// cancel requested, close dialog
				ManageZoomDialog.this.dispose();
			}
		});
		this.add(cancelButton, gbc);

		// misc settings
		this.setMinimumSize(new Dimension(375, 360));
		// center dialog
		this.setLocationRelativeTo(getOwner());
		this.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.action.manage_zoom.title.label"));
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setModal(true);

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
				okButton.requestFocusInWindow();
			}
		});
	}

	/**
	 * Sets the current {@link JFreeChartPlotEngine} for this dialog.
	 * 
	 * @param engine
	 */
	public void setChartEngine(JFreeChartPlotEngine engine) {
		if (engine == null) {
			throw new IllegalArgumentException("engine must not be null!");
		}

		this.engine = engine;
		setPlotConfiguration(engine.getPlotInstance().getMasterPlotConfiguration());
	}

	/**
	 * Sets the current {@link PlotConfiguration} for this dialog.
	 * 
	 * @param plotConfig
	 */
	private void setPlotConfiguration(PlotConfiguration plotConfig) {
		if (plotConfig == null) {
			throw new IllegalArgumentException("plotConfig must not be null!");
		}

		Vector<RangeAxisConfig> rangeConfigsVector = new Vector<RangeAxisConfig>();
		String selectedItem = String.valueOf(rangeAxisSelectionCombobox.getSelectedItem());
		for (RangeAxisConfig config : plotConfig.getRangeAxisConfigs()) {
			rangeConfigsVector.add(config);
		}
		rangeAxisSelectionCombobox.setModel(new DefaultComboBoxModel<>(rangeConfigsVector));

		// reselect the previously selected RangeAxisConfig (if it is still there)
		if (selectedItem != null) {
			for (int i = 0; i < rangeAxisSelectionCombobox.getItemCount(); i++) {
				if (String.valueOf(rangeAxisSelectionCombobox.getItemAt(i)).equals(selectedItem)) {
					rangeAxisSelectionCombobox.setSelectedIndex(i);
					break;
				}
			}
		}

		// fill in values of current chart
		Plot plot = engine.getChartPanel().getChart().getPlot();
		double domainLowerBound = 0;
		double domainUpperBound = 0;
		boolean disableDomainZoom = false;
		NumericalValueRange effectiveRange = engine.getPlotInstance().getPlotData().getDomainConfigManagerData()
				.getEffectiveRange();
		if (plot instanceof XYPlot) {
			ValueAxis domainAxis = ((XYPlot) plot).getDomainAxis();
			if (domainAxis != null) {
				Range range = domainAxis.getRange();
				domainLowerBound = range.getLowerBound();
				domainUpperBound = range.getUpperBound();
			} else {
				if (effectiveRange != null) {
					domainLowerBound = effectiveRange.getLowerBound();
					domainUpperBound = effectiveRange.getUpperBound();
				} else {
					disableDomainZoom = true;
				}
			}
		} else {
			if (effectiveRange != null) {
				domainLowerBound = effectiveRange.getLowerBound();
				domainUpperBound = effectiveRange.getUpperBound();
			} else {
				disableDomainZoom = true;
			}
		}
		domainRangeLowerBoundField.setText(String.valueOf(domainLowerBound));
		domainRangeUpperBoundField.setText(String.valueOf(domainUpperBound));

		// happens on nominal domain axis
		domainRangeLowerBoundField.setEnabled(!disableDomainZoom);
		domainRangeUpperBoundField.setEnabled(!disableDomainZoom);

		updateValueRange();
		updateColorValues();
	}

	/**
	 * Shows the dialog.
	 */
	public void showDialog() {
		setVisible(true);
	}

	/**
	 * Verify that the x-value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyDomainRangeUpperBoundInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			double domainUpperBound;
			if (inputString.startsWith("-")) {
				domainUpperBound = Double.parseDouble(inputString.substring(1));
				domainUpperBound = -domainUpperBound;
			} else {
				domainUpperBound = Double.parseDouble(inputString);
			}
			// TODO: fix check for actual ranges
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Verify that the y-value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyDomainRangeLowerBoundInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			double domainLowerBound;
			if (inputString.startsWith("-")) {
				domainLowerBound = Double.parseDouble(inputString.substring(1));
				domainLowerBound = -domainLowerBound;
			} else {
				domainLowerBound = Double.parseDouble(inputString);
			}
			// TODO: fix check for actual ranges
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Verify that the width value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyValueRangeLowerBoundInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			double valueLowerBound;
			if (inputString.startsWith("-")) {
				valueLowerBound = Double.parseDouble(inputString.substring(1));
				valueLowerBound = -valueLowerBound;
			} else {
				valueLowerBound = Double.parseDouble(inputString);
			}
			// TODO: fix check for actual ranges
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Verify that the height value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyValueRangeUpperBoundInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			double valueUpperBound;
			if (inputString.startsWith("-")) {
				valueUpperBound = Double.parseDouble(inputString.substring(1));
				valueUpperBound = -valueUpperBound;
			} else {
				valueUpperBound = Double.parseDouble(inputString);
			}
			// TODO: fix check for actual ranges
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Verify that the color value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyColorInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			double color;
			if (inputString.startsWith("-")) {
				color = Double.parseDouble(inputString.substring(1));
				color = -color;
			} else {
				color = Double.parseDouble(inputString);
			}
			// TODO: fix check for actual ranges
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Checks all {@link JTextField}s of this dialog for valid entries.
	 * 
	 * @return true if all fields are valid; false otherwise
	 */
	private boolean checkFields() {
		// make sure value is valid, otherwise don't do anything!
		if (!domainRangeLowerBoundField.getInputVerifier().verify(domainRangeLowerBoundField)) {
			domainRangeLowerBoundField.requestFocusInWindow();
			return false;
		}
		// make sure value is valid, otherwise don't do anything!
		if (!domainRangeUpperBoundField.getInputVerifier().verify(domainRangeUpperBoundField)) {
			domainRangeUpperBoundField.requestFocusInWindow();
			return false;
		}
		// make sure value is valid, otherwise don't do anything!
		if (!valueRangeLowerBoundField.getInputVerifier().verify(valueRangeLowerBoundField)) {
			valueRangeLowerBoundField.requestFocusInWindow();
			return false;
		}
		// make sure value is valid, otherwise don't do anything!
		if (!valueRangeUpperBoundField.getInputVerifier().verify(valueRangeUpperBoundField)) {
			valueRangeUpperBoundField.requestFocusInWindow();
			return false;
		}
		// make sure min color value is valid, otherwise don't do anything!
		if (!colorMinValueField.getInputVerifier().verify(colorMinValueField)) {
			colorMinValueField.requestFocusInWindow();
			return false;
		}
		// make sure min color value is valid, otherwise don't do anything!
		if (!colorMaxValueField.getInputVerifier().verify(colorMaxValueField)) {
			colorMaxValueField.requestFocusInWindow();
			return false;
		}

		double domainLowerBound = Double.parseDouble(domainRangeLowerBoundField.getText());
		double domainUpperBound = Double.parseDouble(domainRangeUpperBoundField.getText());
		double valueLowerBound = Double.parseDouble(valueRangeLowerBoundField.getText());
		double valueUpperBound = Double.parseDouble(valueRangeUpperBoundField.getText());

		// same value is forbidden unless both 0 (no zoom)
		if (domainLowerBound == domainUpperBound && domainLowerBound != 0) {
			domainRangeUpperBoundField.requestFocusInWindow();
			return false;
		}

		// same value is forbidden unless both 0 (no zoom)
		if (valueLowerBound == valueUpperBound && valueLowerBound != 0) {
			valueRangeUpperBoundField.requestFocusInWindow();
			return false;
		}

		// if lower bound is bigger than upper bound -> check failed
		if (domainLowerBound > domainUpperBound) {
			domainRangeUpperBoundField.requestFocusInWindow();
			return false;
		}

		// if lower bound is bigger than upper bound -> check failed
		if (valueLowerBound > valueUpperBound) {
			valueRangeUpperBoundField.requestFocusInWindow();
			return false;
		}
		// all checks passed, values are fine
		return true;
	}

	/**
	 * Updates the x and y axes range values.
	 */
	private void updateValueRange() {
		PlotConfiguration plotConfig = engine.getPlotInstance().getMasterPlotConfiguration();
		Plot plot = engine.getChartPanel().getChart().getPlot();
		int index = rangeAxisSelectionCombobox.getSelectedIndex();
		if (index == -1) {
			index = 0;
		}

		// should be always true..
		if (plotConfig.getRangeAxisConfigs().size() > index) {
			RangeAxisConfig config = plotConfig.getRangeAxisConfigs().get(index);
			double valueLowerBound;
			double valueUpperBound;
			if (plot instanceof XYPlot) {
				ValueAxis rangeAxis = ((XYPlot) plot).getRangeAxis(index);
				if (rangeAxis != null) {
					// this is the actual visible axis
					Range range = rangeAxis.getRange();
					valueLowerBound = range.getLowerBound();
					valueUpperBound = range.getUpperBound();
				} else {
					valueLowerBound = engine.getPlotInstance().getPlotData().getRangeAxisData(config).getLowerViewBound();
					valueUpperBound = engine.getPlotInstance().getPlotData().getRangeAxisData(config).getUpperViewBound();
				}
			} else if (plot instanceof CategoryPlot) {
				ValueAxis rangeAxis = ((CategoryPlot) plot).getRangeAxis(index);
				if (rangeAxis != null) {
					Range range = rangeAxis.getRange();
					valueLowerBound = range.getLowerBound();
					valueUpperBound = range.getUpperBound();
				} else {
					valueLowerBound = engine.getPlotInstance().getPlotData().getRangeAxisData(config).getLowerViewBound();
					valueUpperBound = engine.getPlotInstance().getPlotData().getRangeAxisData(config).getUpperViewBound();
				}
			} else {
				// data bounds, a bit smaller than the visible axis
				valueLowerBound = engine.getPlotInstance().getPlotData().getRangeAxisData(config).getLowerViewBound();
				valueUpperBound = engine.getPlotInstance().getPlotData().getRangeAxisData(config).getUpperViewBound();
			}
			valueRangeLowerBoundField.setText(String.valueOf(valueLowerBound));
			valueRangeUpperBoundField.setText(String.valueOf(valueUpperBound));
		}
	}

	/**
	 * Updates the min/max color value fields with the current values.
	 */
	private void updateColorValues() {
		DimensionConfigData dimensionConfigData = engine
				.getPlotInstance()
				.getPlotData()
				.getDimensionConfigData(
						engine.getPlotInstance().getMasterPlotConfiguration().getDefaultDimensionConfigs()
								.get(PlotDimension.COLOR));
		if (dimensionConfigData != null && dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
			ContinuousColorProvider colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
			colorMinValueField.setText(String.valueOf(colProv.getMinValue()));
			colorMaxValueField.setText(String.valueOf(colProv.getMaxValue()));
			colorMinValueField.setEnabled(true);
			colorMaxValueField.setEnabled(true);
			restoreColorButton.setEnabled(true);
		} else {
			// we cannot show color modification here
			colorMinValueField.setText("0");
			colorMaxValueField.setText("0");
			colorMinValueField.setEnabled(false);
			colorMaxValueField.setEnabled(false);
			restoreColorButton.setEnabled(false);
		}
	}

	/**
	 * Checks if the ContinuousColorProvider has different original min/max color values than the
	 * supplied values. If not, no color event needs to be fired.
	 * 
	 * @return
	 */
	private boolean isColorChanged(double minValue, double maxValue) {
		DimensionConfigData dimensionConfigData = engine
				.getPlotInstance()
				.getPlotData()
				.getDimensionConfigData(
						engine.getPlotInstance().getMasterPlotConfiguration().getDefaultDimensionConfigs()
								.get(PlotDimension.COLOR));
		if (dimensionConfigData != null && dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
			ContinuousColorProvider colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
			if (colProv.isColorMinMaxValueDifferentFromOriginal(minValue, maxValue)) {
				return true;
			}
		}
		return false;
	}
}
