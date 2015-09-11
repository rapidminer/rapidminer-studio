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

import com.rapidminer.gui.new_plotter.templates.HistogramTemplate;
import com.rapidminer.gui.new_plotter.templates.actions.ExportAdvancedChartAction;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.tools.I18N;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * This class contains the GUI for the {@link HistogramTemplate}.
 * 
 * @author Marco Boeck
 * 
 */
public class HistogrammTemplatePanel extends PlotterTemplatePanel implements Observer {

	/** the {@link HistogramTemplate} for this panel */
	private HistogramTemplate histogramTemplate;

	/** list for selecting the plot column(s) */
	private JList plotList;

	/** the plot list selection listener */
	private ListSelectionListener updatePlotListSelectionListener;

	/** checkbox for absolute values */
	private JCheckBox absoluteValuesCheckBox;

	/** checkbox for selecting log scale */
	private JCheckBox columnLogCheckBox;

	/** slider to control the number of bins */
	private JSlider binsSlider;

	/** slider to control the opaqueness */
	private JSlider opaqueSlider;

	// /** checkbox for rotating labels */
	// private JCheckBox rotateLabelsCheckBox;

	private static final long serialVersionUID = -3325519427272856192L;

	/**
	 * Creates a new GUI for a {@link HistogramTemplate}.
	 * 
	 * @param scatterTemplate
	 */
	public HistogrammTemplatePanel(HistogramTemplate histogramTemplate) {
		super(histogramTemplate);

		this.histogramTemplate = histogramTemplate;
		this.updatePlotListSelectionListener = new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					HistogrammTemplatePanel.this.histogramTemplate.setPlotSelection(plotList.getSelectedValues());
				}
			}
		};
		setupGUI();
	}

	/**
	 * Setup GUI.
	 */
	private void setupGUI() {
		// start layout
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(2, 5, 2, 5);
		JLabel plotsLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.plots.column.label"));
		plotsLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.plots.column.tip"));
		this.add(plotsLabel, gbc);

		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		this.add(errorIndicatorLabel, gbc);

		plotList = new JList();
		plotList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		plotList.setBorder(BorderFactory.createLoweredBevelBorder());
		plotList.addListSelectionListener(updatePlotListSelectionListener);
		plotList.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.plots.column.tip"));
		gbc.insets = new Insets(2, 5, 2, 5);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 1;
		gbc.gridwidth = 2;
		ExtendedJScrollPane plotSeriesScrollPane = new ExtendedJScrollPane(plotList);
		this.add(plotSeriesScrollPane, gbc);

		absoluteValuesCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.histogram.absolute_values.label"));
		absoluteValuesCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.histogram.absolute_values.tip"));
		absoluteValuesCheckBox.setSelected(histogramTemplate.isUsingAbsoluteValues());
		absoluteValuesCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				histogramTemplate.setUseAbsoluteValues(absoluteValuesCheckBox.isSelected());
			}
		});
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		// TODO: Add again when supported
		// this.add(absoluteValuesCheckBox, gbc);

		columnLogCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.plots.log.label"));
		columnLogCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.plots.log.tip"));
		columnLogCheckBox.setSelected(histogramTemplate.isYAxisLogarithmic());
		columnLogCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				histogramTemplate.setYAxisLogarithmic(columnLogCheckBox.isSelected());
			}
		});
		gbc.gridy = 4;
		this.add(columnLogCheckBox, gbc);

		// rotateLabelsCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
		// "gui.plotter.histogram.rotate_labels.label"));
		// rotateLabelsCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
		// "gui.plotter.histogram.rotate_labels.tip"));
		// rotateLabelsCheckBox.addActionListener(updatePlotActionListener);
		// gbc.gridy = 5;
		// this.add(rotateLabelsCheckBox, gbc);

		JLabel binsLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.bins.label"));
		binsLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.bins.tip"));
		gbc.gridy = 6;
		this.add(binsLabel, gbc);

		binsSlider = new JSlider(1, 100, histogramTemplate.getBins());
		binsSlider.setMajorTickSpacing(99);
		binsSlider.setMinorTickSpacing(10);
		binsSlider.setPaintTicks(true);
		binsSlider.setPaintLabels(true);
		binsSlider.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.bins.tip",
				binsSlider.getValue()));
		binsSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				binsSlider.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.bins.tip",
						binsSlider.getValue()));
			}
		});
		binsSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() instanceof JSlider) {
					JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						histogramTemplate.setBins(binsSlider.getValue());
					}
				}
			}
		});
		gbc.gridy = 7;
		this.add(binsSlider, gbc);

		JLabel opaquenessLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.opaqueness.label"));
		opaquenessLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.opaqueness.tip"));
		gbc.gridy = 8;
		this.add(opaquenessLabel, gbc);

		opaqueSlider = new JSlider(1, 255, histogramTemplate.getOpacity());
		opaqueSlider.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.histogram.opaqueness.tip"));
		opaqueSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() instanceof JSlider) {
					JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						histogramTemplate.setOpaque(opaqueSlider.getValue());
					}
				}
			}
		});
		gbc.gridy = 9;
		this.add(opaqueSlider, gbc);

		// add export buttons
		JPanel exportPanel = new JPanel(new BorderLayout());
		JButton exportImageButton = new JButton(new ExportAdvancedChartAction(histogramTemplate));
		exportPanel.add(exportImageButton, BorderLayout.PAGE_START);
		gbc.gridy = 10;
		this.add(exportPanel, gbc);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof HistogramTemplate) {
			final HistogramTemplate histogramTemplate = (HistogramTemplate) o;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {

					// update list
					DefaultListModel modelPlotsList = new DefaultListModel();
					for (int i = 0; i < histogramTemplate.getDataTable().getColumnNumber(); i++) {
						// only show numerical attributes in the list
						if (!histogramTemplate.getDataTable().isNumerical(i)) {
							continue;
						}
						String attName = histogramTemplate.getDataTable().getColumnName(i);
						modelPlotsList.addElement(attName);
					}
					plotList.removeListSelectionListener(updatePlotListSelectionListener);
					plotList.setModel(modelPlotsList);
					int[] selectedIndicies = new int[modelPlotsList.size()];
					Arrays.fill(selectedIndicies, -1);
					int i = 0;
					for (Object plot : histogramTemplate.getPlotSelection()) {
						selectedIndicies[i++] = modelPlotsList.indexOf(plot);
					}
					plotList.setSelectedIndices(selectedIndicies);
					plotList.addListSelectionListener(updatePlotListSelectionListener);

					// select correct value
					absoluteValuesCheckBox.setSelected(histogramTemplate.isUsingAbsoluteValues());
					columnLogCheckBox.setSelected(histogramTemplate.isYAxisLogarithmic());

					binsSlider.setValue(histogramTemplate.getBins());
					opaqueSlider.setValue(histogramTemplate.getOpacity());
				}

			});
		}
	}
}
