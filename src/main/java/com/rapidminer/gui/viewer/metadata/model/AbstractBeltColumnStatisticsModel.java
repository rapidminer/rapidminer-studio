/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.viewer.metadata.model;

import java.awt.Font;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import javax.swing.event.EventListenerList;

import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;

import com.rapidminer.belt.column.Statistics.Result;
import com.rapidminer.belt.column.Statistics.Statistic;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent.EventType;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEventListener;
import com.rapidminer.tools.FontTools;


/**
 * Abstract model for the {@link BeltColumnStatisticsPanel}. See implementations for details.
 *
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public abstract class AbstractBeltColumnStatisticsModel {

	/**
	 * The column type of the model
	 */
	public enum Type{
		BINOMINAL, NOMINAL, NUMERIC, DATETIME, TIME, OTHER_OBJECT;
	}

	/**
	 * the name of the column for this model
	 */
	private final String columnName;

	/**
	 * stores the {@link Table} as a {@link WeakReference}
	 */
	private final WeakReference<Table> weakTable;

	/** if not {@code null}, the column is a special column */
	private final String specialColumnName;

	/** if true, the panel should be drawn in an alternating color scheme */
	private boolean alternating;

	/** if true, the display should be enlarged */
	private boolean enlarged;

	/** the number of missing values */
	protected double missing;

	/** event listener for this model */
	private final EventListenerList eventListener;

	protected AbstractBeltColumnStatisticsModel(final Table table, final String columnName) {
		this.columnName = columnName;
		this.weakTable = new WeakReference<>(table);
		ColumnRole role = table.getFirstMetaData(columnName, ColumnRole.class);
		this.specialColumnName = role != null ? role.toString().toLowerCase(Locale.ENGLISH) : null;

		this.eventListener = new EventListenerList();
	}

	/**
	 * Adds a {@link AttributeStatisticsEventListener} which will be informed of all changes to this model.
	 *
	 * @param listener
	 * 		the listener
	 */
	public void registerEventListener(final AttributeStatisticsEventListener listener) {
		eventListener.add(AttributeStatisticsEventListener.class, listener);
	}

	/**
	 * Removes the {@link AttributeStatisticsEventListener} from this model.
	 *
	 * @param listener
	 * 		the listener
	 */
	public void removeEventListener(final AttributeStatisticsEventListener listener) {
		eventListener.remove(AttributeStatisticsEventListener.class, listener);
	}

	/**
	 * Sets if this panel should be drawn in an alternating color scheme (slightly darker) to make reading of many rows
	 * easier.
	 *
	 * @param alternating
	 * 		whether an alternating color scheme should be used
	 */
	public void setAlternating(final boolean alternating) {
		if (this.alternating != alternating) {
			this.alternating = alternating;

			fireAlternatingChangedEvent();
		}
	}

	/**
	 * Returns {@code true} if this is an alternating column statistics model.
	 *
	 * @return if an alternate color scheme is used
	 */
	public boolean isAlternating() {
		return alternating;
	}

	/**
	 * Gets the enlarged status which determines how many information to display.
	 *
	 * @return the enlarged status
	 */
	public boolean isEnlarged() {
		return enlarged;
	}

	/**
	 * Sets the enlarged status.
	 *
	 * @param enlarged whether the display is enlarged
	 */
	public void setEnlarged(final boolean enlarged) {
		this.enlarged = enlarged;
		if (enlarged && getTableOrNull() != null) {
			prepareCharts();
		}

		fireEnlargedChangedEvent();
	}

	/**
	 * Returns {@code true} if this column has a special {@link ColumnRole};
	 * {@code false} otherwise.
	 *
	 * @return whether the column has a role
	 */
	public boolean isSpecialColumn() {
		return specialColumnName != null;
	}

	/**
	 * Returns the name of the special {@link ColumnRole} for this column. If this has
	 * no role, returns {@code null} .
	 *
	 * @return the name of the column role
	 */
	public String getSpecialColumnName() {
		return specialColumnName;
	}

	/**
	 * Gets the name of the column backing this model.
	 *
	 * @return the column name
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Gets the {@link Table} backing this model or {@code null} if the {@link WeakReference} to it was removed.
	 *
	 * @return a table or {@code null}
	 */
	public Table getTableOrNull() {
		return weakTable.get();
	}

	/**
	 * Fire when the enlarged status has changed.
	 */
	private void fireEnlargedChangedEvent() {
		fireEvent(EventType.ENLARGED_CHANGED);
	}

	/**
	 * Fire when the statistics of an column have changed.
	 */
	protected void fireStatisticsChangedEvent() {
		fireEvent(EventType.STATISTICS_CHANGED);
	}

	/**
	 * Fire when alternation has changed.
	 */
	private void fireAlternatingChangedEvent() {
		fireEvent(EventType.ALTERNATING_CHANGED);
	}

	/**
	 * Fires the given {@link EventType}.
	 *
	 * @param type
	 * 		the event type
	 */
	protected void fireEvent(final EventType type) {
		Object[] listeners = eventListener.getListenerList();
		// Process the listeners last to first
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == AttributeStatisticsEventListener.class) {
				AttributeStatisticsEvent e = new AttributeStatisticsEvent(type);
				((AttributeStatisticsEventListener) listeners[i + 1]).modelChanged(e);
			}
		}
	}

	/**
	 * Updates the statistics of this model via the given map.
	 *
	 * @param allStatistics
	 *            map containing all the statistics
	 */
	public abstract void updateStatistics(Map<String, Map<Statistic, Result>> allStatistics);


	/**
	 * @return the {@link Type} of the model
	 */
	public abstract Type getType();

	/**
	 * Returns the number of missing values.
	 *
	 * @return the number of missing values
	 */
	public double getNumberOfMissingValues() {
		return missing;
	}

	/**
	 * Returns the given {@link JFreeChart} for the given index. If the given index is invalid, returns {@code null}.
	 *
	 * @param index
	 * 		the index
	 * @return the chart
	 */
	public abstract JFreeChart getChartOrNull(int index);

	/**
	 * Prepares the charts if needed.
	 */
	protected abstract void prepareCharts();

	/**
	 * Changes the font of {@link JFreeChart}s to Sans Serif. This method uses a
	 * {@link StandardChartTheme} to do so, so any changes to the look of the chart must be done
	 * after calling this method.
	 *
	 * @param chart
	 *            the chart to change fonts for
	 */
	static void setDefaultChartFonts(JFreeChart chart) {
		final ChartTheme chartTheme = StandardChartTheme.createJFreeTheme();

		if (StandardChartTheme.class.isAssignableFrom(chartTheme.getClass())) {
			StandardChartTheme standardTheme = (StandardChartTheme) chartTheme;
			// The default font used by JFreeChart cannot render japanese etc symbols
			final Font oldExtraLargeFont = standardTheme.getExtraLargeFont();
			final Font oldLargeFont = standardTheme.getLargeFont();
			final Font oldRegularFont = standardTheme.getRegularFont();
			final Font oldSmallFont = standardTheme.getSmallFont();

			final Font extraLargeFont = FontTools.getFont(Font.SANS_SERIF, oldExtraLargeFont.getStyle(),
					oldExtraLargeFont.getSize());
			final Font largeFont = FontTools.getFont(Font.SANS_SERIF, oldLargeFont.getStyle(), oldLargeFont.getSize());
			final Font regularFont = FontTools.getFont(Font.SANS_SERIF, oldRegularFont.getStyle(), oldRegularFont.getSize());
			final Font smallFont = FontTools.getFont(Font.SANS_SERIF, oldSmallFont.getStyle(), oldSmallFont.getSize());

			standardTheme.setExtraLargeFont(extraLargeFont);
			standardTheme.setLargeFont(largeFont);
			standardTheme.setRegularFont(regularFont);
			standardTheme.setSmallFont(smallFont);

			standardTheme.apply(chart);
		}
	}
}
