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
package com.rapidminer.gui.viewer.metadata.model;

import java.awt.Font;
import java.lang.ref.WeakReference;

import javax.swing.event.EventListenerList;

import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEvent.EventType;
import com.rapidminer.gui.viewer.metadata.event.AttributeStatisticsEventListener;
import com.rapidminer.tools.FontTools;


/**
 * Abstract model for the {@link AttributeStatisticsPanel}. See implementations for details.
 *
 * @author Marco Boeck
 *
 */
public abstract class AbstractAttributeStatisticsModel {

	/** the {@link Attribute} for this model */
	private final Attribute attribute;

	/** stores the {@link ExampleSet} as a {@link WeakReference} */
	private final WeakReference<ExampleSet> weakExampleSet;

	/** if not <code>null</code>, the attribute is a special attribute */
	private final String specialAttName;

	/** if true, the panel should be drawn in an alternating color scheme */
	private boolean alternating;

	/** if true, the display should be enlarged */
	private boolean enlarged;

	/** if true, the construction value will be displayed */
	private boolean showConstruction;

	/** the number of missing values */
	protected double missing;

	/** the construction value for the attribute */
	private final String construction;

	/** event listener for this model */
	private final EventListenerList eventListener;

	/**
	 * Inits the
	 *
	 * @param exampleSet
	 * @param attribute
	 */
	protected AbstractAttributeStatisticsModel(final ExampleSet exampleSet, final Attribute attribute) {
		this.attribute = attribute;
		this.weakExampleSet = new WeakReference<>(exampleSet);
		this.specialAttName = exampleSet.getAttributes().findRoleByName(attribute.getName()).getSpecialName();
		this.construction = attribute.getConstruction();

		this.eventListener = new EventListenerList();
	}

	/**
	 * Adds a {@link AttributeStatisticsEventListener} which will be informed of all changes to this
	 * model.
	 *
	 * @param listener
	 */
	public void registerEventListener(final AttributeStatisticsEventListener listener) {
		eventListener.add(AttributeStatisticsEventListener.class, listener);
	}

	/**
	 * Removes the {@link AttributeStatisticsEventListener} from this model.
	 *
	 * @param listener
	 */
	public void removeEventListener(final AttributeStatisticsEventListener listener) {
		eventListener.remove(AttributeStatisticsEventListener.class, listener);
	}

	/**
	 * Sets if this panel should be drawn in an alternating color scheme (slightly darker) to make
	 * reading of many rows easier.
	 *
	 * @param alternating
	 */
	public void setAlternating(final boolean alternating) {
		if (this.alternating != alternating) {
			this.alternating = alternating;

			fireAlternatingChangedEvent();
		}
	}

	/**
	 * Returns <code>true</code> if this is an alternating attribute statistics model.
	 *
	 * @return
	 */
	public boolean isAlternating() {
		return alternating;
	}

	/**
	 * Gets the enlarged status which determines how many information to display.
	 *
	 * @return
	 */
	public boolean isEnlarged() {
		return enlarged;
	}

	/**
	 * Sets the enlarged status.
	 *
	 * @param enlarged
	 */
	public void setEnlarged(final boolean enlarged) {
		this.enlarged = enlarged;
		if (enlarged && getExampleSetOrNull() != null) {
			prepareCharts();
		}

		fireEnlargedChangedEvent();
	}

	/**
	 * Gets the show construction status which determines if the construction value is shown.
	 *
	 * @return
	 */
	public boolean isShowConstruction() {
		return showConstruction;
	}

	/**
	 * Sets the show construction status.
	 *
	 * @param showConstruction
	 */
	public void setShowConstruction(final boolean showConstruction) {
		this.showConstruction = showConstruction;

		fireShowConstructionChangedEvent();
	}

	/**
	 * Returns <code>true</code> if this attribute has a special {@link AttributeRole};
	 * <code>false</code> otherwise.
	 *
	 * @return
	 */
	public boolean isSpecialAtt() {
		return specialAttName != null;
	}

	/**
	 * Returns the name of the special {@link AttributeRole} for this {@link Attribute}. If this is
	 * not a special attribute, returns <code>null</code> .
	 *
	 * @return
	 */
	public String getSpecialAttName() {
		return specialAttName;
	}

	/**
	 * Gets the {@link Attribute} backing this model.
	 *
	 * @return
	 */
	public Attribute getAttribute() {
		return attribute;
	}

	/**
	 * Gets the {@link ExampleSet} backing this model or <code>null</code> if the
	 * {@link WeakReference} to it was removed.
	 *
	 * @return
	 */
	public ExampleSet getExampleSetOrNull() {
		return weakExampleSet.get();
	}

	/**
	 * Fire when the show construction status has changed.
	 */
	protected void fireShowConstructionChangedEvent() {
		fireEvent(EventType.SHOW_CONSTRUCTION_CHANGED);
	}

	/**
	 * Fire when the enlarged status has changed.
	 */
	protected void fireEnlargedChangedEvent() {
		fireEvent(EventType.ENLARGED_CHANGED);
	}

	/**
	 * Fire when the statistics of an attribute have changed.
	 */
	protected void fireStatisticsChangedEvent() {
		fireEvent(EventType.STATISTICS_CHANGED);
	}

	/**
	 * Fire when alternation has changed.
	 */
	protected void fireAlternatingChangedEvent() {
		fireEvent(EventType.ALTERNATING_CHANGED);
	}

	/**
	 * Fires the given {@link EventType}.
	 *
	 * @param type
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
	 * Updates the statistics of this model via the given {@link ExampleSet}.
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} for which the attribute statistics should be updated. No
	 *            reference to it is stored to prevent memory leaks.
	 */
	public abstract void updateStatistics(ExampleSet exampleSet);

	/**
	 * Returns the number of missing values.
	 *
	 * @return
	 */
	public double getNumberOfMissingValues() {
		return missing;
	}

	/**
	 * Returns the construction value for this attribute.
	 *
	 * @return
	 */
	public String getConstruction() {
		return construction;
	}

	/**
	 * Returns the given {@link JFreeChart} for the given index. If the given index is invalid,
	 * returns <code>null</code>.
	 *
	 * @param index
	 * @return
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
	protected static void setDefaultChartFonts(JFreeChart chart) {
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
