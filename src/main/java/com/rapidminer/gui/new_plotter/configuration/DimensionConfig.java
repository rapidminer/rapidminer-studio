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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DataTableColumn.ValueType;
import com.rapidminer.gui.new_plotter.configuration.ValueGrouping.GroupingType;
import com.rapidminer.gui.new_plotter.listener.DimensionConfigListener;
import com.rapidminer.gui.new_plotter.utility.NumericalValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;
import com.rapidminer.tools.I18N;

import java.text.DateFormat;
import java.util.List;
import java.util.Set;
import java.util.Vector;


/**
 * Defines where a dimension gets its values from. Could be: the values of an attribute, all
 * attributes, etc.
 * 
 * Also defines the sort order and the value range.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public interface DimensionConfig extends Cloneable {

	public static final String DEFAULT_DATE_FORMAT_STRING = "dd.MM.yyyy HH:mm";
	public static final String DEFAULT_AXIS_LABEL = "";
	public static boolean DEFAULT_USE_USER_DEFINED_DATE_FORMAT = false;
	public static double DEFAULT_USER_DEFINED_LOWER_BOUND = 0;
	public static double DEFAULT_USER_DEFINED_UPPER_BOUND = 1;

	public enum PlotDimension {
		VALUE(null, null), DOMAIN(I18N.getGUILabel("plotter.configuration_dialog.plot_dimension.domain.label"), I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.domain.short_label")), COLOR(I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.color.label"), I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.color.short_label")), SHAPE(I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.shape.label"), I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.shape.short_label")), SIZE(I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.size.label"), I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.size.short_label")), SELECTED(I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.selected.label"), I18N
				.getGUILabel("plotter.configuration_dialog.plot_dimension.selected.short_label"));

		// LINESTYLE

		private final String name;
		private String shortName;

		private PlotDimension(String name, String shortName) {
			this.name = name;
			this.shortName = shortName;
		}

		/**
		 * @return The display name of the enum value.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return A shorter display name of the enum value.
		 */
		public String getShortName() {
			return shortName;
		}
	}

	public Double getUserDefinedUpperBound();

	public Double getUserDefinedLowerBound();

	public PlotDimension getDimension();

	/**
	 * Returns the {@link DataTableColumn} from which this DimensionConfig gets its raw values.
	 */
	public DataTableColumn getDataTableColumn();

	public ValueGrouping getGrouping();

	/**
	 * Returns the range of data which is used to create the diagram. Note that this is not
	 * necessarily the data the user sees, because he might apply further filtering by zooming.
	 * 
	 * Might return null, which indicates that all values should be used.
	 * 
	 * Returns a clone of the actual range, so changing the returned object does not actually change
	 * the range of this {@link DimensionConfig}.
	 */
	public ValueRange getUserDefinedRangeClone(DataTable dataTable);

	/**
	 * Returns the label of the dimension config that will be shown in the GUI.
	 */
	public String getLabel();

	public List<PlotConfigurationError> getErrors();

	public List<PlotConfigurationError> getWarnings();

	public Vector<GroupingType> getValidGroupingTypes();

	public ValueType getValueType();

	public Set<DataTableColumn.ValueType> getSupportedValueTypes();

	public int getId();

	public boolean isValid();

	public boolean isAutoRangeRequired();

	public boolean isLogarithmic();

	public boolean isAutoNaming();

	public boolean isGrouping();

	public boolean isNominal();

	public boolean isNumerical();

	public boolean isDate();

	public boolean isUsingUserDefinedLowerBound();

	public boolean isUsingUserDefinedUpperBound();

	public void setGrouping(ValueGrouping grouping);

	// public void setSortProvider(SortProvider sortProvider);
	public void setDataTableColumn(DataTableColumn column);

	public void setUserDefinedRange(NumericalValueRange range);

	public void setLogarithmic(boolean logarithmic);

	public void setAutoNaming(boolean autoNaming);

	public void setLabel(String label);

	public void setUpperBound(Double upperBound);

	public void setLowerBound(Double lowerBound);

	public void setUseUserDefinedUpperBound(boolean useUpperBound);

	public void setUseUserDefinedLowerBound(boolean useLowerBound);

	public void removeDimensionConfigListener(DimensionConfigListener l);

	public void addDimensionConfigListener(DimensionConfigListener l);

	public void colorSchemeChanged();

	/**
	 * Returns a {@link DateFormat} to be used for formatting dates on this axis.
	 * 
	 * @return the date format used to format dates on this axis.
	 */
	public DateFormat getDateFormat();

	public void setUserDefinedDateFormatString(String formatString);

	public String getUserDefinedDateFormatString();

	public void setUseUserDefinedDateFormat(boolean yes);

	boolean isUsingUserDefinedDateFormat();
}
