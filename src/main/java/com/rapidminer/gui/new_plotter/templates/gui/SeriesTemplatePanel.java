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

import com.rapidminer.gui.new_plotter.templates.SeriesTemplate;
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
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * This class contains the GUI for the {@link SeriesTemplate}.
 * 
 * @author Marco Boeck
 * 
 */
public class SeriesTemplatePanel extends PlotterTemplatePanel implements Observer {

	/** the {@link SeriesTemplate} for this panel */
	private SeriesTemplate seriesTemplate;

	/** for selecting the x-axis attribute */
	private JComboBox lowerBoundComboBox;

	/** for selecting the y-axis attribute */
	private JComboBox upperBoundComboBox;

	/** for selecting the color attribute */
	private JComboBox indexDimensionComboBox;

	/** list for selecting the plot column(s) */
	private JList plotSeriesList;

	/** the checkbox indicating whether to use relative utilities or not */
	private JCheckBox useRelativeUtilitiesCheckBox;

	/** the plot list selection listener */
	private ListSelectionListener updatePlotListSelectionListener;

	// /** checkbox for rotating labels */
	// private JCheckBox rotateLabelsCheckBox;

	private static final long serialVersionUID = -4999581517852957478L;

	/**
	 * Creates a new GUI for a {@link SeriesTemplate}.
	 * 
	 * @param scatterTemplate
	 */
	public SeriesTemplatePanel(SeriesTemplate seriesTemplate) {
		super(seriesTemplate);

		this.seriesTemplate = seriesTemplate;
		this.updatePlotListSelectionListener = new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					SeriesTemplatePanel.this.seriesTemplate.setPlotSelection(plotSeriesList.getSelectedValues());
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
		JLabel upperBoundLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.upper_bound.column.label"));
		upperBoundLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series.upper_bound.column.tip"));
		this.add(upperBoundLabel, gbc);

		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		this.add(errorIndicatorLabel, gbc);

		upperBoundComboBox = new JComboBox();
		upperBoundComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				seriesTemplate.setUpperBoundName(String.valueOf(upperBoundComboBox.getSelectedItem()));
			}
		});
		upperBoundComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series.upper_bound.column.tip"));
		gbc.insets = new Insets(2, 5, 2, 5);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 1;
		gbc.gridwidth = 2;
		this.add(upperBoundComboBox, gbc);

		JLabel lowerBoundLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.lower_bound.column.label"));
		lowerBoundLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series.lower_bound.column.tip"));
		gbc.gridy = 2;
		this.add(lowerBoundLabel, gbc);

		lowerBoundComboBox = new JComboBox();
		lowerBoundComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				seriesTemplate.setLowerBoundName(String.valueOf(lowerBoundComboBox.getSelectedItem()));
			}
		});
		lowerBoundComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series.lower_bound.column.tip"));
		gbc.gridy = 3;
		this.add(lowerBoundComboBox, gbc);

		JLabel indexDimensionLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.index_dimension.column.label"));
		indexDimensionLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.index_dimension.column.tip"));
		gbc.gridy = 4;
		this.add(indexDimensionLabel, gbc);

		indexDimensionComboBox = new JComboBox();
		indexDimensionComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				seriesTemplate.setIndexDimensionName(String.valueOf(indexDimensionComboBox.getSelectedItem()));
			}
		});
		indexDimensionComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.index_dimension.column.tip"));
		gbc.gridy = 5;
		this.add(indexDimensionComboBox, gbc);

		JLabel plotSeriesLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.plot_series.column.label"));
		plotSeriesLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series.plot_series.column.tip"));
		gbc.gridy = 6;
		this.add(plotSeriesLabel, gbc);

		plotSeriesList = new JList();
		plotSeriesList.setBorder(BorderFactory.createLoweredBevelBorder());
		plotSeriesList.addListSelectionListener(updatePlotListSelectionListener);
		plotSeriesList.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.series.plot_series.column.tip"));
		gbc.gridy = 7;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		ExtendedJScrollPane plotSeriesScrollPane = new ExtendedJScrollPane(plotSeriesList);
		this.add(plotSeriesScrollPane, gbc);

		useRelativeUtilitiesCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.relative_utilities.label"));
		useRelativeUtilitiesCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.plotter.series.relative_utilities.tip"));
		useRelativeUtilitiesCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				seriesTemplate.setUseRelativeUtilities(useRelativeUtilitiesCheckBox.isSelected());
			}
		});
		gbc.gridy = 8;
		this.add(useRelativeUtilitiesCheckBox, gbc);

		JPanel exportPanel = new JPanel(new BorderLayout());
		JButton exportImageButton = new JButton(new ExportAdvancedChartAction(seriesTemplate));
		exportPanel.add(exportImageButton, BorderLayout.PAGE_START);
		gbc.gridy = 9;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		this.add(exportPanel, gbc);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof SeriesTemplate) {
			final SeriesTemplate seriesTemplate = (SeriesTemplate) o;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					// update list & combo boxes
					DefaultListModel modelPlotsList = new DefaultListModel();
					Vector<String> attrNameVector = new Vector<String>(seriesTemplate.getDataTable().getColumnNames().length);
					attrNameVector.add(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.column.empty_selection.label"));
					for (String attName : seriesTemplate.getDataTable().getColumnNames()) {
						modelPlotsList.addElement(attName);
						attrNameVector.add(attName);
					}
					plotSeriesList.removeListSelectionListener(updatePlotListSelectionListener);
					plotSeriesList.setModel(modelPlotsList);
					int[] selectedIndicies = new int[modelPlotsList.size()];
					Arrays.fill(selectedIndicies, -1);
					int i = 0;
					for (Object plot : seriesTemplate.getPlotSelection()) {
						selectedIndicies[i++] = modelPlotsList.indexOf(plot);
					}
					plotSeriesList.setSelectedIndices(selectedIndicies);
					plotSeriesList.addListSelectionListener(updatePlotListSelectionListener);
					DefaultComboBoxModel modelLowerBound = new DefaultComboBoxModel(attrNameVector);
					DefaultComboBoxModel modelUpperBound = new DefaultComboBoxModel(attrNameVector);
					DefaultComboBoxModel modelIndexDimension = new DefaultComboBoxModel(attrNameVector);

					lowerBoundComboBox.setModel(modelLowerBound);
					upperBoundComboBox.setModel(modelUpperBound);
					indexDimensionComboBox.setModel(modelIndexDimension);

					// select correct values (and make sure they don't fire events)
					ActionListener[] actionListeners = indexDimensionComboBox.getActionListeners();
					for (ActionListener l : actionListeners) {
						indexDimensionComboBox.removeActionListener(l);
					}
					indexDimensionComboBox.setSelectedItem(seriesTemplate.getIndexDimensionName());
					for (ActionListener l : actionListeners) {
						indexDimensionComboBox.addActionListener(l);
					}
					actionListeners = lowerBoundComboBox.getActionListeners();
					for (ActionListener l : actionListeners) {
						lowerBoundComboBox.removeActionListener(l);
					}
					lowerBoundComboBox.setSelectedItem(seriesTemplate.getLowerBoundName());
					for (ActionListener l : actionListeners) {
						lowerBoundComboBox.addActionListener(l);
					}
					actionListeners = upperBoundComboBox.getActionListeners();
					for (ActionListener l : actionListeners) {
						upperBoundComboBox.removeActionListener(l);
					}
					upperBoundComboBox.setSelectedItem(seriesTemplate.getUpperBoundName());
					for (ActionListener l : actionListeners) {
						upperBoundComboBox.addActionListener(l);
					}

					useRelativeUtilitiesCheckBox.setSelected(seriesTemplate.isUsingRelativeUtilities());
				}

			});
		}
	}
}
