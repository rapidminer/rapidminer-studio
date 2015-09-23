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
package com.rapidminer.gui.new_plotter.templates;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.engine.jfreechart.JFreeChartPlotEngine;
import com.rapidminer.gui.new_plotter.templates.style.DefaultPlotterStyleProvider;
import com.rapidminer.gui.new_plotter.templates.style.PlotterStyleProvider;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;


/**
 * This class contains the main template GUI for the new plotters. All available templates for
 * specific plot types must be registered here.
 * 
 * @author Marco Boeck
 * 
 */
public class TemplateChooser {

	/** the panel holding the chart chooser JComboBox and the according template control GUI */
	private JPanel controlContainerPanel;

	/** the container panel holding the chart style provider */
	private JPanel styleProviderContainerPanel;

	/** the {@link JFreeChartPlotEngine} object */
	private JFreeChartPlotEngine plotEngine;

	/** the panel holding the according template GUI */
	JPanel templatePanel;

	/** the panel holding the according template chart */
	JPanel templateChartPanel;

	/** the panel holding the according template style GUI */
	JPanel stylePanel;

	/** the combo box which allows plot type choosing */
	private JComboBox plotTypeComboBox;

	/** a {@link Vector} containing all {@link PlotterTemplate}s */
	private final Vector<PlotterTemplate> chartTemplates;

	/**
	 * 
	 */
	public enum StyleProviderMode {
		GLOBAL, SINGLE,
	}

	/**
	 * Standard constructor.
	 */
	public TemplateChooser(StyleProviderMode styleProviderMode) {
		chartTemplates = new Vector<PlotterTemplate>(16);

		/*
		 * !!! register Templates here !!!
		 */
		chartTemplates.add(new ScatterTemplate());
		chartTemplates.add(new SeriesTemplate());
		chartTemplates.add(new SeriesMultipleTemplate());
		chartTemplates.add(new HistogramTemplate());
		/*
		 * !!! register Templates here !!!
		 */

		PlotterStyleProvider styleProvider = new DefaultPlotterStyleProvider();
		for (PlotterTemplate template : chartTemplates) {
			if (styleProviderMode.equals(StyleProviderMode.GLOBAL)) {
				template.setStyleProvider(styleProvider);
			} else if (styleProviderMode.equals(StyleProviderMode.SINGLE)) {
				template.setStyleProvider(styleProvider);
				styleProvider = new DefaultPlotterStyleProvider();
			}
		}

		// add all template configuration panels to the card layout
		templatePanel = new JPanel();
		templatePanel.setLayout(new CardLayout());
		if (chartTemplates.size() <= 0) {
			// add empty panel when no templates have been registered
			templatePanel.add(new JPanel(), "");
		}
		for (PlotterTemplate template : chartTemplates) {
			templatePanel.add(template.getTemplateConfigurationPanel(), template.getChartType());
		}

		// add all template style provider panels to the card layout
		stylePanel = new JPanel();
		stylePanel.setLayout(new CardLayout());
		if (chartTemplates.size() <= 0) {
			// add empty panel when no templates have been registered
			stylePanel.add(new JPanel(), "");
		}
		for (PlotterTemplate template : chartTemplates) {
			stylePanel.add(template.getStyleProvider().getStyleProviderPanel(), template.getChartType());
		}

		// the template chart panel
		templateChartPanel = new JPanel(new BorderLayout());

		setupGUI();
	}

	/**
	 * !! Testing method only !! TODO: REMOVE
	 * 
	 * @throws RepositoryException
	 * @throws MalformedRepositoryLocationException
	 */
	public static void main(String[] args) throws MalformedRepositoryLocationException, RepositoryException {
		RapidMiner.setExecutionMode(ExecutionMode.UI);
		try {
			UIManager.setLookAndFeel(new RapidLookAndFeel());
		} catch (Exception e) {
			// LogService.getRoot().log(Level.WARNING,
			// "Cannot setup rapid look and feel, using default.", e);
			LogService.getRoot().log(
					Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.new_plotter_templates.TemplateChooser.setup_rapid_look_and_feel_error"), e);

		}
		RapidMiner.init();
		ExampleSet iris = (ExampleSet) ((IOObjectEntry) new RepositoryLocation("//Samples/data/Iris").locateEntry())
				.retrieveData(null);

		final TemplateChooser chooser = new TemplateChooser(StyleProviderMode.SINGLE);
		chooser.fireDataUpdated("iris", iris);

		JFrame frame = new JFrame("New Plotter Templates");
		JPanel containerPanel = new JPanel(new BorderLayout());
		containerPanel.add(BorderLayout.EAST, chooser.getTemplateChooserControlPanel());
		containerPanel.add(BorderLayout.CENTER, chooser.getTemplateChooserChartPanel());
		containerPanel.add(BorderLayout.WEST, chooser.getTemplateChooserStyleProviderPanel());
		frame.setContentPane(containerPanel);
		frame.setLocation(200, 200);
		frame.setSize(new Dimension(1200, 500));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	/**
	 * Returns the panel containg the control panel of the selected template.
	 * 
	 * @return
	 */
	public JPanel getTemplateChooserControlPanel() {
		return controlContainerPanel;
	}

	/**
	 * Returns the panel containg the chart panel of the selected template.
	 * 
	 * @return
	 */
	public JPanel getTemplateChooserChartPanel() {
		return templateChartPanel;
	}

	/**
	 * Returns the panel containg the style provider panel of the selected template.
	 * 
	 * @return
	 */
	public JPanel getTemplateChooserStyleProviderPanel() {
		return styleProviderContainerPanel;
	}

	/**
	 * Inits the GUI.
	 */
	private void setupGUI() {
		// start layout
		controlContainerPanel = new JPanel(new BorderLayout());

		// create chart type selection panel
		JPanel typeChooserPanel = new JPanel();
		typeChooserPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		plotTypeComboBox = new JComboBox(chartTemplates);
		plotTypeComboBox.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.chooser.type.tip"));
		plotTypeComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// get the currently selected template
				PlotterTemplate template = ((PlotterTemplate) plotTypeComboBox.getSelectedItem());
				// show its configuration panel
				((CardLayout) templatePanel.getLayout()).show(templatePanel, template.getChartType());

				// show its style provider panel
				((CardLayout) stylePanel.getLayout()).show(stylePanel, template.getChartType());

				// tell the plot engine the PlotConfiguration has been switched
				plotEngine.setPlotInstance(template.getPlotInstance());
			}

		});
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		JLabel typeLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.chooser.type.label"));
		typeLabel.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.plotter.chooser.type.tip"));
		typeChooserPanel.add(typeLabel, gbc);

		gbc.gridy = 1;
		typeChooserPanel.add(plotTypeComboBox, gbc);

		gbc.gridy = 2;
		typeChooserPanel.add(new JSeparator(), gbc);

		// add the chart type chooser combobox panel at the top
		controlContainerPanel.add(typeChooserPanel, BorderLayout.NORTH);

		// add the template panel in the center
		controlContainerPanel.add(templatePanel, BorderLayout.CENTER);

		// show the first (or an empty) template control panel
		if (chartTemplates.size() > 0) {
			((CardLayout) templatePanel.getLayout()).show(templatePanel, chartTemplates.get(0).getChartType());
		} else {
			((CardLayout) templatePanel.getLayout()).show(templatePanel, "");
		}

		// style provider panel
		styleProviderContainerPanel = new JPanel(new BorderLayout());
		styleProviderContainerPanel.add(BorderLayout.CENTER, stylePanel);

		// show the first (or an empty) template style panel
		if (chartTemplates.size() > 0) {
			((CardLayout) stylePanel.getLayout()).show(stylePanel, chartTemplates.get(0).getChartType());
		} else {
			((CardLayout) stylePanel.getLayout()).show(stylePanel, "");
		}
	}

	/**
	 * Call to notify all plotter templates that the data has changed.
	 * 
	 * @param dataSetName
	 *            the identifying name of the new data
	 * @param exampleSet
	 *            the new data
	 */
	public void fireDataUpdated(String dataSetName, final ExampleSet exampleSet) {
		DataTable dataTable = new DataTableExampleSetAdapter(exampleSet, null);
		DataTableColumn domainDataTableColumn = new DataTableColumn(dataTable, 0);

		// now that we have a data set, create newplotter engine to create an empty chart
		PlotInstance plotInstance = createBasicPlotInstance(dataTable, domainDataTableColumn);
		plotEngine = new JFreeChartPlotEngine(plotInstance, true);

		// set new PlotConfig and new data on all templates
		for (PlotterTemplate template : chartTemplates) {
			template.setPlotInstance(plotInstance);
			template.setPlotEngine(plotEngine);
			template.fireDataUpdated(dataTable);
			plotInstance = createBasicPlotInstance(dataTable, domainDataTableColumn);
		}

		// set plotConfig to selected chart
		plotEngine.setPlotInstance(((PlotterTemplate) plotTypeComboBox.getSelectedItem()).getPlotInstance());

		templateChartPanel.removeAll();
		templateChartPanel.add(plotEngine.getChartPanel(), BorderLayout.CENTER);
	}

	/**
	 * Creates a basic {@link PlotConfiguration} which can be used to display an empty plot.
	 * 
	 * @param dataTable
	 *            the {@link DataTable} for which the plot should be generated
	 * @param domainDataTableColumn
	 *            the {@link DataTableColumn} which should be the domain column
	 * @return
	 */
	private PlotInstance createBasicPlotInstance(DataTable dataTable, DataTableColumn domainDataTableColumn) {
		PlotConfiguration plotConfiguration = new PlotConfiguration(domainDataTableColumn);
		PlotInstance plotInstance = new PlotInstance(plotConfiguration, dataTable);
		return plotInstance;
	}

}
