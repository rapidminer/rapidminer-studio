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

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import org.jfree.chart.JFreeChart;
import org.jfree.data.Range;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.plotter.charts.AbstractChartPanel;
import com.rapidminer.gui.plotter.charts.AbstractChartPanel.Selection;
import com.rapidminer.gui.plotter.charts.AbstractChartPanel.SelectionListener;
import com.rapidminer.gui.plotter.charts.ChartPanelShiftController;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.container.Pair;


/**
 * Plotter extending this adapter must be able to set their ranges during plotting if the dimensions
 * name is registered here.
 * 
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class RangeablePlotterAdapter extends LabelRotatingPlotterAdapter implements AxisNameResolver {

	private static final long serialVersionUID = 1L;

	public static final String PARAMETER_PREFIX_RANGE_LIST = "range_list";
	public static final String PARAMETER_PREFIX_RANGE = "range_";
	public static final String PARAMETER_DIMENSION_NAME = "dimension";
	public static final String PARAMETER_PREFIX_RANGE_MIN = "range_min";
	public static final String PARAMETER_PREFIX_RANGE_MAX = "range_max";

	private Map<String, Range> nameRangeMap = new HashMap<>();

	private DataTable dataTable;

	private CoordinateTransformation coordinateTransformation;

	private AbstractChartPanel panel = null;

	private List<SelectionListener> plotterSelectionListener = new LinkedList<>();

	public RangeablePlotterAdapter(final PlotterConfigurationModel settings) {
		super(settings);

		// adding default zoom listener
		plotterSelectionListener.add(new SelectionListener() {

			@Override
			public void selected(Selection selection, MouseEvent selectionEvent) {
				for (Pair<String, Range> delimiter : selection.getDelimiters()) {
					setRange(delimiter.getFirst(), delimiter.getSecond());
				}
			}
		});
	}

	@Override
	public List<ParameterType> getAdditionalParameterKeys(InputPort inputPort) {
		List<ParameterType> types = super.getAdditionalParameterKeys(inputPort);

		boolean inputDeliversAttributes = false;
		if (inputPort != null) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null && (metaData instanceof ExampleSetMetaData || metaData instanceof ModelMetaData)) {
				inputDeliversAttributes = true;
			}
		}

		if (inputDeliversAttributes) {
			types.add(new ParameterTypeList(PARAMETER_PREFIX_RANGE_LIST, "Defines the ranges for the given attribute",
					new ParameterTypeAttribute(PARAMETER_DIMENSION_NAME,
							"This is the name of the dimension, the range should be applied onto.", inputPort),
					new ParameterTypeTupel(PARAMETER_PREFIX_RANGE, "Defines the range of the corresponding axis.",
							new ParameterTypeDouble(PARAMETER_PREFIX_RANGE_MIN, "Defines the lower bound of the axis.",
									Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), new ParameterTypeDouble(
									PARAMETER_PREFIX_RANGE_MAX, "Defines the upper bound of the axis.",
									Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))));
		} else {
			types.add(new ParameterTypeList(PARAMETER_PREFIX_RANGE_LIST, "Defines the ranges for the given attribute",
					new ParameterTypeString(PARAMETER_DIMENSION_NAME,
							"This is the name of the dimension, the range should be applied onto."), new ParameterTypeTupel(
							PARAMETER_PREFIX_RANGE, "Defines the range of the corresponding axis.", new ParameterTypeDouble(
									PARAMETER_PREFIX_RANGE_MIN, "Defines the lower bound of the axis.",
									Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), new ParameterTypeDouble(
									PARAMETER_PREFIX_RANGE_MAX, "Defines the upper bound of the axis.",
									Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))));

		}
		return types;
	}

	/**
	 * This method returns the range for the axis if was defined by parameters or null if automatic
	 * ranging should be used.
	 */
	public Range getRangeForDimension(int dimension) {
		if (dimension >= 0 && dimension < dataTable.getNumberOfColumns()) {
			return nameRangeMap.get(PlotterAdapter.transformParameterName(dataTable.getColumnName(dimension)));
		}
		return null;
	}

	public Range getRangeForName(String columnName) {
		return nameRangeMap.get(PlotterAdapter.transformParameterName(columnName));
	}

	@Override
	public final void setDataTable(DataTable dataTable) {
		this.dataTable = dataTable;
		dataTableSet();
	}

	public abstract void dataTableSet();

	protected DataTable getDataTable() {
		return dataTable;
	}

	/**
	 * This is a convenience method for setting the correct range parameter
	 */
	public void setRange(int dimension, Range range) {
		setRange(dataTable.getColumnName(dimension), range);
	}

	public void setRange(String columnName, Range range) {
		// inserting in map
		nameRangeMap.put(PlotterAdapter.transformParameterName(columnName), range);

		// translating current ranges to string
		List<String[]> entryList = new LinkedList<>();
		for (String dimensionName : nameRangeMap.keySet()) {
			Range currentRange = nameRangeMap.get(dimensionName);
			String[] entry = new String[2];
			entry[0] = dimensionName;
			entry[1] = ParameterTypeTupel.transformTupel2String(new Pair<>(currentRange.getLowerBound() + "", currentRange
					.getUpperBound() + ""));
			entryList.add(entry);
		}

		// finally set it in the settings
		settings.setParameterAsString(PARAMETER_PREFIX_RANGE_LIST, ParameterTypeList.transformList2String(entryList));
	}

	@Override
	public void setAdditionalParameter(String key, String value) {
		super.setAdditionalParameter(key, value);
		if (key.startsWith(PARAMETER_PREFIX_RANGE_LIST)) {
			List<String[]> dimensionRangePairs = ParameterTypeList.transformString2List(value);
			for (String[] dimensionRangePair : dimensionRangePairs) {
				String[] rangeTupel = ParameterTypeTupel.transformString2Tupel(dimensionRangePair[1]);
				if (rangeTupel.length == 2) {
					try {
						Range range = new Range(Double.parseDouble(rangeTupel[0]), Double.parseDouble(rangeTupel[1]));
						nameRangeMap.put(PlotterAdapter.transformParameterName(dimensionRangePair[0]), range);
						updatePlotter();
					} catch (NumberFormatException e) {
					}
				}
			}
			return;
		}
	}

	protected AbstractChartPanel createPanel(JFreeChart chart) {
		panel = new AbstractChartPanel(chart, getWidth(), getHeight() - MARGIN);

		panel.registerAxisNameResolver(this);

		if (coordinateTransformation != null) {
			panel.setCoordinateTransformation(coordinateTransformation);
		}

		// adding all listener
		for (SelectionListener listener : plotterSelectionListener) {
			panel.registerSelectionListener(listener);
		}

		panel.registerSelectionListener(new SelectionListener() {

			@Override
			public void selected(Selection selection, MouseEvent selectionEvent) {
				for (Pair<String, Range> delimiter : selection.getDelimiters()) {
					setRange(delimiter.getFirst(), delimiter.getSecond());
				}
				dataTable.setSelection(selection);
			}
		});

		final ChartPanelShiftController controller = new ChartPanelShiftController(panel);
		panel.addMouseListener(controller);
		panel.addMouseMotionListener(controller);

		return panel;
	}

	public void registerPlotterSelectionListener(SelectionListener listener) {
		this.plotterSelectionListener.add(listener);
		if (panel != null) {
			panel.registerSelectionListener(listener);
		}
	}

	public void clearPlotterSelectionListener() {
		this.plotterSelectionListener.clear();
		if (panel != null) {
			panel.clearSelectionListener();
		}
	}

	@Override
	public void setCoordinateTransformation(CoordinateTransformation transformation) {
		this.coordinateTransformation = transformation;
		if (panel != null) {
			panel.setCoordinateTransformation(transformation);
		}
	}

	@Override
	public final JComponent getPlotter() {
		if (panel == null) {
			updatePlotter();
		}
		return panel;
	}

	public AbstractChartPanel getPlotterPanel() {
		return panel;
	}
}
