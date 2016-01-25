/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.new_plotter;

import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.RangeAxisConfigChangeEvent;
import com.rapidminer.gui.new_plotter.listener.events.ValueSourceChangeEvent;


/**
 * @author Nils Woehler
 * 
 */
public class StaticDebug {

	private static String format = "%25.25s %25.25s %25.25s %25.25s %25.25s %25.25s %25.25s%n";

	public static void debug(String message) {
		// if (message.toLowerCase().contains("error")) {
		// System.err.println(new java.sql.Timestamp(new java.util.Date().getTime())+ " #Thread ID:"
		// + Thread.currentThread().getId() + "# " + message
		// + " ");
		// } else {
		// System.out.println(new java.sql.Timestamp(new java.util.Date().getTime()) +
		// " #Thread ID:" + Thread.currentThread().getId() + "# " +
		// message + " ");
		// }
	}

	public static void debug(Object message) {
		// System.err.println(new java.sql.Timestamp(new java.util.Date().getTime()) + " " + message
		// + " ");
	}

	private static void formattedDebug(String format, Object[] inputs) {
		// Object[] inputsWithTimeStamp = new Object[inputs.length + 1];
		// inputsWithTimeStamp[0] = new java.sql.Timestamp(new java.util.Date().getTime()) + " ";
		// for (int i = 0; i < inputs.length; ++i) {
		// if(inputs[i] == null) {
		// inputsWithTimeStamp[i + 1] = "";
		// } else {
		// inputsWithTimeStamp[i + 1] = inputs[i];
		// }
		// }
		// System.out.format(String.format(format, inputsWithTimeStamp));
	}

	public static void debug(Double value) {
		// System.out.println(new java.sql.Timestamp(new java.util.Date().getTime()) + " " +
		// value.toString() + " ");
	}

	public static void debug(Integer value) {
		// System.out.println(new java.sql.Timestamp(new java.util.Date().getTime()) + " " +
		// value.toString() + " ");
	}

	public static void emptyDebugLine() {
		// System.out.println();
	}

	public static void finalize(Object clazz) {
		// StaticDebug.debug("#####################################################");
		// StaticDebug.debug("#####################################################");
		// StaticDebug.debug("######");
		// StaticDebug.debug("###### " + clazz.getClass().toString() + ": finalize()");
		// StaticDebug.debug("######");
		// StaticDebug.debug("#####################################################");
		// StaticDebug.debug("#####################################################");
	}

	public static void debugEvent(int pad, PlotConfigurationChangeEvent currentEvent) {
		// PlotConfigurationChangeType type = currentEvent.getType();
		// Object[] inputs = new Object[6];
		// inputs[pad] = type;
		// switch (type) {
		// case DIMENSION_CONFIG_ADDED:
		// case DIMENSION_CONFIG_REMOVED:
		// inputs[pad+1] = "'"+currentEvent.getDimensionConfig().getDimension()+"'";
		// inputs[pad+2] = "ID:"+currentEvent.getDimensionConfig().getId();
		// break;
		// case RANGE_AXIS_CONFIG_ADDED:
		// case RANGE_AXIS_CONFIG_MOVED:
		// case RANGE_AXIS_CONFIG_REMOVED:
		// inputs[pad+1] = "'"+currentEvent.getRangeAxisConfig().getLabel()+"'";
		// inputs[pad+2] = "ID:"+currentEvent.getRangeAxisConfig().getId();
		// break;
		// case RANGE_AXIS_CONFIG_CHANGED:
		// inputs[pad+1] = "'"+currentEvent.getRangeAxisConfigChange().getSource().getLabel()+"'" ;
		// inputs[pad+2] = "ID:"+currentEvent.getRangeAxisConfigChange().getSource().getId();
		// break;
		// case DIMENSION_CONFIG_CHANGED:
		// inputs[pad+1] = "'"+currentEvent.getDimensionChange().getSource().getDimension()+"'";
		// inputs[pad+2] = "ID:"+currentEvent.getDimensionChange().getSource().getId();
		// break;
		// }
		// StaticDebug.formattedDebug(format, inputs);
		// switch (type) {
		// case META_CHANGE:
		// for (PlotConfigurationChangeEvent event : currentEvent.getPlotConfigChangeEvents()) {
		// debugEvent(pad, event);
		// }
		// break;
		// case RANGE_AXIS_CONFIG_CHANGED:
		// debugRangeAxisChange(pad, currentEvent.getRangeAxisConfigChange());
		// break;
		// case DIMENSION_CONFIG_CHANGED:
		// debugDimensionChange(pad, currentEvent.getDimensionChange());
		// break;
		// }
	}

	private static void debugRangeAxisChange(int pad, RangeAxisConfigChangeEvent change) {
		// RangeAxisConfigChangeType type = change.getType();
		// Object[] inputs = new Object[6];
		// inputs[pad] = "--> "+type;
		// switch (type) {
		// case VALUE_SOURCE_ADDED:
		// case VALUE_SOURCE_MOVED:
		// case VALUE_SOURCE_REMOVED:
		// inputs[pad+1] = "'"+change.getValueSource().getLabel()+"'";
		// inputs[pad+2] = "ID:"+change.getValueSource().getId();
		// break;
		// case VALUE_SOURCE_CHANGED:
		// inputs[pad+1] = "'"+change.getValueSourceChange().getSource().getLabel()+"'";
		// inputs[pad+2] = "ID:"+change.getValueSourceChange().getSource().getId();
		// break;
		// case AUTO_NAMING:
		// inputs[pad+1] = change.getAutoNaming();
		// break;
		// case CLEARED:
		// break;
		// case CROSSHAIR_LINES_CHANGED:
		// break;
		// case LABEL:
		// inputs[pad+1] = change.getLabel();
		// break;
		// case RANGE_CHANGED:
		// inputs[pad+1] = change.getValueRangeChange().getSource();
		// break;
		// case SCALING:
		// inputs[pad+1] = change.getLogarithmic();
		// break;
		// }
		// StaticDebug.formattedDebug(format, inputs);
		// if (type == RangeAxisConfigChangeType.VALUE_SOURCE_CHANGED) {
		// debugValueSourceChange(pad, change.getValueSourceChange());
		// }
	}

	private static void debugValueSourceChange(int pad, ValueSourceChangeEvent change) {
		// ValueSourceChangeType type = change.getType();
		// Object[] inputs = new Object[6];
		// inputs[pad] = "------> "+type;
		// switch (type) {
		// case AGGREGATION_FUNCTION_MAP:
		// inputs[pad+1] = change.getSeriesUsagType();
		// inputs[pad+2] = change.getAggregationFunctionType();
		// break;
		// case AGGREGATION_WINDOWING_CHANGED:
		// break;
		// case AUTO_NAMING:
		// break;
		// case DATATABLE_COLUMN_MAP:
		// inputs[pad+1] = change.getSeriesUsagType();
		// DataTableColumn dataTableColumn = change.getDataTableColumn();
		// if (dataTableColumn != null) {
		// inputs[pad+2] = dataTableColumn.getName();
		// inputs[pad+3] = dataTableColumn.getValueType();
		// } else {
		// inputs[pad+2] = "null";
		// }
		// break;
		// case LABEL:
		// inputs[pad+1] = change.getLabel();
		// break;
		// case SERIES_FORMAT_CHANGED:
		// break;
		// case UPDATED:
		// break;
		// case USES_GROUPING:
		// inputs[pad+1] = change.getUsesGrouping();
		// break;
		// case USE_RELATIVE_UTILITIES:
		// inputs[pad+1] = change.getUseRelative();
		// break;
		//
		// }
		// StaticDebug.formattedDebug(format, inputs);
	}

	private static void debugDimensionChange(int pad, DimensionConfigChangeEvent change) {
		// DimensionConfigChangeType type = change.getType();
		// Object[] inputs = new Object[6];
		// inputs[pad] = "--> "+type;
		// switch (type) {
		// case ABOUT_TO_CHANGE_GROUPING:
		// break;
		// case AUTO_NAMING:
		// break;
		// case COLOR_PROVIDER:
		// break;
		// case COLOR_SCHEME:
		// break;
		// case COLUMN:
		// inputs[pad+1] = change.getDataTableColumn().getName();
		// inputs[pad+2] = change.getDataTableColumn().getValueType();
		// break;
		// case CROSSHAIR_LINES_CHANGED:
		// break;
		// case DATE_FORMAT_CHANGED:
		// break;
		// case GROUPING_CHANGED:
		// inputs[pad+1] = change.getGroupingChangeEvent().getSource();
		// break;
		// case LABEL:
		// inputs[pad+1] = change.getLabel();
		// break;
		// case RANGE:
		// inputs[pad+1] = change.getValueRangeChangedEvent().getSource();
		// break;
		// case RESET:
		// break;
		// case SCALING:
		// inputs[pad+1] = change.getLogarithmic();
		// break;
		// case SHAPE_PROVIDER:
		// break;
		// case SIZE_PROVIDER:
		// break;
		// case SORTING:
		// inputs[pad+1] = change.getSortingMode();
		// break;
		// }
		// StaticDebug.formattedDebug(format, inputs);
	}

}
