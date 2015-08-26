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

import com.rapidminer.gui.new_plotter.templates.ScatterTemplate;
import com.rapidminer.gui.new_plotter.templates.actions.ExportAdvancedChartAction;
import com.rapidminer.tools.I18N;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This class contains the GUI for the {@link ScatterTemplate}.
 * 
 * @author Marco Boeck
 * 
 */
public class ScatterTemplatePanel extends PlotterTemplatePanel implements Observer {

	/** the {@link ScatterTemplate} for this panel */
	private ScatterTemplate scatterTemplate;

	/** for selecting the x-axis attribute */
	private JComboBox xAxisComboBox;

	/** for selecting the y-axis attribute */
	private JComboBox yAxisComboBox;

	/** for selecting the color attribute */
	private JComboBox colorComboBox;

	/** checkbox for selecting log scale x-axis */
	private JCheckBox xAxisLogCheckBox;

	/** checkbox for selecting log scale y-axis */
	private JCheckBox yAxisLogCheckBox;

	/** checkbox for selecting log scale color */
	private JCheckBox colorLogCheckBox;

	/** slider to control the jitter */
	private JSlider jitterSlider;

	// /** checkbox for rotating labels */
	// private JCheckBox rotateLabelsCheckBox;

	private static final long serialVersionUID = -9129694544029851477L;

	/**
	 * Creates a new GUI for a {@link ScatterTemplate}.
	 * 
	 * @param scatterTemplate
	 */
	public ScatterTemplatePanel(ScatterTemplate scatterTemplate) {
		super(scatterTemplate);
		this.scatterTemplate = scatterTemplate;
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
		JLabel xAxisLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.xaxis.column.label"));
		xAxisLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.xaxis.column.tip"));
		this.add(xAxisLabel, gbc);

		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		this.add(errorIndicatorLabel, gbc);

		xAxisComboBox = new JComboBox();
		xAxisComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				scatterTemplate.setXAxisColum(String.valueOf(xAxisComboBox.getSelectedItem()));
			}
		});
		xAxisComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.xaxis.column.tip"));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.weightx = 1;
		gbc.gridwidth = 2;
		this.add(xAxisComboBox, gbc);

		xAxisLogCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.xaxis.log.label"));
		xAxisLogCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.xaxis.log.tip"));
		xAxisLogCheckBox.setSelected(scatterTemplate.isXAxisLogarithmic());
		xAxisLogCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				scatterTemplate.setXAxisLogarithmic(xAxisLogCheckBox.isSelected());
			}
		});
		gbc.gridy = 2;
		this.add(xAxisLogCheckBox, gbc);

		JLabel yAxisLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.yaxis.column.label"));
		yAxisLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.yaxis.column.tip"));
		gbc.gridy = 3;
		this.add(yAxisLabel, gbc);

		yAxisComboBox = new JComboBox();
		yAxisComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				scatterTemplate.setYAxisColum(String.valueOf(yAxisComboBox.getSelectedItem()));
			}
		});
		yAxisComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.yaxis.column.tip"));
		gbc.gridy = 4;
		this.add(yAxisComboBox, gbc);

		yAxisLogCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.yaxis.log.label"));
		yAxisLogCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.yaxis.log.tip"));
		yAxisLogCheckBox.setSelected(scatterTemplate.isYAxisLogarithmic());
		yAxisLogCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				scatterTemplate.setYAxisLogarithmic(yAxisLogCheckBox.isSelected());
			}
		});
		gbc.gridy = 5;
		this.add(yAxisLogCheckBox, gbc);

		JLabel colorAxisLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.color.column.label"));
		colorAxisLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.color.column.tip"));
		gbc.gridy = 6;
		this.add(colorAxisLabel, gbc);

		colorComboBox = new JComboBox();
		colorComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				scatterTemplate.setColorColum(String.valueOf(colorComboBox.getSelectedItem()));
			}
		});
		colorComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.color.column.tip"));
		gbc.gridy = 7;
		this.add(colorComboBox, gbc);

		colorLogCheckBox = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.color.log.label"));
		colorLogCheckBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.color.log.tip"));
		colorLogCheckBox.setSelected(scatterTemplate.isColorLogarithmic());
		colorLogCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				scatterTemplate.setColorLogarithmic(colorLogCheckBox.isSelected());
			}
		});
		gbc.gridy = 8;
		this.add(colorLogCheckBox, gbc);

		JLabel jitterLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.jitter.label"));
		jitterLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.jitter.tip"));
		gbc.gridy = 9;
		// TODO: Add again when jitter is supported
		// this.add(jitterLabel, gbc);

		jitterSlider = new JSlider(0, 100, scatterTemplate.getJitter());
		jitterSlider.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.scatter.jitter.tip"));
		jitterSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() instanceof JSlider) {
					JSlider source = (JSlider) e.getSource();
					if (!source.getValueIsAdjusting()) {
						scatterTemplate.setJitter(jitterSlider.getValue());
					}
				}
			}
		});
		gbc.gridy = 10;
		// TODO: Add again when jitter is supported
		// this.add(jitterSlider, gbc);

		// fill bottom
		gbc.gridx = 0;
		gbc.gridy = 11;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		this.add(new JLabel(), gbc);

		// add export buttons
		JPanel exportPanel = new JPanel(new BorderLayout());
		JButton exportImageButton = new JButton(new ExportAdvancedChartAction(scatterTemplate));
		exportPanel.add(exportImageButton, BorderLayout.PAGE_START);
		gbc.gridy = 12;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		this.add(exportPanel, gbc);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof ScatterTemplate) {
			final ScatterTemplate scatterTemplate = (ScatterTemplate) o;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					// update combo boxes
					Vector<String> attrNameVector = new Vector<String>(
							scatterTemplate.getDataTable().getColumnNames().length);
					attrNameVector.add(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.column.empty_selection.label"));
					for (String attName : scatterTemplate.getDataTable().getColumnNames()) {
						attrNameVector.add(attName);
					}

					DefaultComboBoxModel modelXAxis = new DefaultComboBoxModel(attrNameVector);
					DefaultComboBoxModel modelYAxis = new DefaultComboBoxModel(attrNameVector);
					DefaultComboBoxModel modelColorAxis = new DefaultComboBoxModel(attrNameVector);
					xAxisComboBox.setModel(modelXAxis);
					yAxisComboBox.setModel(modelYAxis);
					colorComboBox.setModel(modelColorAxis);

					// select correct values (and make sure they don't fire events)
					ActionListener[] actionListeners = xAxisComboBox.getActionListeners();
					for (ActionListener l : actionListeners) {
						xAxisComboBox.removeActionListener(l);
					}
					xAxisComboBox.setSelectedItem(scatterTemplate.getXAxisColumn());
					for (ActionListener l : actionListeners) {
						xAxisComboBox.addActionListener(l);
					}
					actionListeners = yAxisComboBox.getActionListeners();
					for (ActionListener l : actionListeners) {
						yAxisComboBox.removeActionListener(l);
					}
					yAxisComboBox.setSelectedItem(scatterTemplate.getYAxisColumn());
					for (ActionListener l : actionListeners) {
						yAxisComboBox.addActionListener(l);
					}
					actionListeners = colorComboBox.getActionListeners();
					for (ActionListener l : actionListeners) {
						colorComboBox.removeActionListener(l);
					}
					colorComboBox.setSelectedItem(scatterTemplate.getColorColumn());
					for (ActionListener l : actionListeners) {
						colorComboBox.addActionListener(l);
					}

					xAxisLogCheckBox.setSelected(scatterTemplate.isXAxisLogarithmic());
					yAxisLogCheckBox.setSelected(scatterTemplate.isYAxisLogarithmic());
					colorLogCheckBox.setSelected(scatterTemplate.isColorLogarithmic());

					jitterSlider.setValue(scatterTemplate.getJitter());
				}

			});
		}
	}
}
