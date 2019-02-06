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
package com.rapidminer.gui.renderer.models;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.gui.actions.export.AbstractPrintableIOObjectPanel;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.plotter.LabelRotatingPlotterAdapter;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.RangeablePlotterAdapter;
import com.rapidminer.gui.plotter.charts.DistributionPlotter;
import com.rapidminer.gui.plotter.settings.ListeningJComboBox;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.learner.bayes.DistributionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.report.Reportable;


/**
 * A renderer for the plot view of a distribution model.
 *
 * @author Ingo Mierswa
 */
public class DistributionModelPlotRenderer extends AbstractRenderer {

	public static final String PARAMETER_ATTRIBUTE_NAME = "attribute_name";

	public static final String PARAMETER_ROTATE_LABELS = "rotate_labels";

	public static final String PARAMETER_RANGE = "range_";
	public static final String PARAMETER_RANGE_MIN = "range_min";
	public static final String PARAMETER_RANGE_MAX = "range_max";

	private static final class DistributionModelPlotVisualizer extends AbstractPrintableIOObjectPanel {

		private static final long serialVersionUID = 1L;
		private JComponent plotterComponent;

		public DistributionModelPlotVisualizer(DistributionModel distributionModel) {
			super(distributionModel, "plot_view");
			setLayout(new BorderLayout());
			DataTable table = new SimpleDataTable("Dummy", distributionModel.getAttributeNames());
			final PlotterConfigurationModel settings = new PlotterConfigurationModel(
					PlotterConfigurationModel.COMPLETE_PLOTTER_SELECTION, table);
			final Plotter plotter = new DistributionPlotter(settings, distributionModel);
			settings.setPlotter(plotter);

			plotterComponent = plotter.getPlotter();
			plotterComponent.setBorder(BorderFactory.createMatteBorder(15, 0, 10, 5, Colors.WHITE));
			add(plotterComponent, BorderLayout.CENTER);

			final ListeningJComboBox<String> combo = new ListeningJComboBox<>(settings,
					PlotterAdapter.PARAMETER_PLOT_COLUMN + "_"
							+ PlotterAdapter.transformParameterName(plotter.getPlotName()),
					distributionModel.getAttributeNames());
			combo.setPreferredSize(new Dimension(200, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
			combo.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0.0d;
			c.weightx = 1.0d;
			c.insets = new Insets(4, 4, 4, 4);
			c.gridwidth = GridBagConstraints.REMAINDER;

			JPanel boxPanel = new JPanel(layout);
			boxPanel.setBorder(BorderFactory.createMatteBorder(10, 8, 5, 10, Colors.WHITE));
			boxPanel.setOpaque(true);
			boxPanel.setBackground(Colors.WHITE);
			JLabel label = new JLabel("Attribute:");
			layout.setConstraints(label, c);
			boxPanel.add(label);

			layout.setConstraints(combo, c);
			boxPanel.add(combo);

			final JCheckBox rotateLabels = new JCheckBox("Rotate Labels", false);
			layout.setConstraints(rotateLabels, c);
			boxPanel.add(rotateLabels);

			c.weighty = 1.0;
			JLabel filler = new JLabel();
			layout.setConstraints(filler, c);
			boxPanel.add(filler);

			add(boxPanel, BorderLayout.WEST);

			combo.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					settings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMN, combo.getSelectedItem().toString());
				}
			});

			rotateLabels.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					settings.setParameterAsBoolean(LabelRotatingPlotterAdapter.PARAMETER_ROTATE_LABELS,
							rotateLabels.isSelected());
				}
			});

			combo.setSelectedIndex(0);
		}

		@Override
		public Component getExportComponent() {
			return plotterComponent;
		}

	}

	@Override
	public String getName() {
		return "Plot View";
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int width, int height) {
		DistributionModel distributionModel = (DistributionModel) renderable;

		String attributeName = "";
		String range = null;
		try {
			attributeName = getParameterAsString(PARAMETER_ATTRIBUTE_NAME);
			range = getParameterAsString(PARAMETER_RANGE);
		} catch (UndefinedParameterError e) {
			// do nothing
		}

		boolean rotateLabels = getParameterAsBoolean(PARAMETER_ROTATE_LABELS);

		DataTable table = new SimpleDataTable("Dummy", distributionModel.getAttributeNames());
		PlotterConfigurationModel settings = new PlotterConfigurationModel(
				PlotterConfigurationModel.COMPLETE_PLOTTER_SELECTION, table);
		Plotter plotter = new DistributionPlotter(settings, distributionModel);
		settings.setPlotter(plotter);
		settings.setParameterAsString(PlotterAdapter.PARAMETER_PLOT_COLUMN, attributeName);
		settings.setParameterAsBoolean(LabelRotatingPlotterAdapter.PARAMETER_ROTATE_LABELS, rotateLabels);

		if (range != null) {
			String rangeList = ParameterTypeList.transformList2String(Collections.singletonList(new String[] {
					DistributionPlotter.MODEL_DOMAIN_AXIS_NAME, range }));
			settings.setParameterAsString(RangeablePlotterAdapter.PARAMETER_PREFIX_RANGE_LIST, rangeList);
		}

		plotter.getRenderComponent().setSize(width, height);
		return plotter;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		DistributionModel distributionModel = (DistributionModel) renderable;

		return new DistributionModelPlotVisualizer(distributionModel);
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = super.getParameterTypes(inputPort);
		if (inputPort != null) {
			types.add(new ParameterTypeAttribute(PARAMETER_ATTRIBUTE_NAME,
					"Indicates for which attribute the distribution should be plotted.", inputPort, false));
		} else {
			types.add(new ParameterTypeString(PARAMETER_ATTRIBUTE_NAME,
					"Indicates for which attribute the distribution should be plotted.", false));
		}

		types.add(new ParameterTypeTupel(PARAMETER_RANGE, "Defines the range of the corresponding axis.",
				new ParameterTypeDouble(PARAMETER_RANGE_MIN, "Defines the lower bound of the axis.",
						Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), new ParameterTypeDouble(PARAMETER_RANGE_MAX,
						"Defines the upper bound of the axis.", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)));
		types.add(new ParameterTypeBoolean(PARAMETER_ROTATE_LABELS, "Indicates if the labels should be rotated.", false));

		return types;
	}
}
