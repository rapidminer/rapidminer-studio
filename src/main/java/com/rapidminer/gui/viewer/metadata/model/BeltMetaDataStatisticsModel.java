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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.EventListenerList;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.BeltMetaDataStatisticsViewer;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEvent.EventType;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEventListener;
import com.rapidminer.tools.Ontology;


/**
 * Data model and controller for the {@link BeltMetaDataStatisticsViewer}. The statistics for the
 * {@link com.rapidminer.belt.table.Table} are created in a {@link com.rapidminer.gui.tools.MultiSwingWorker},
 * so model creation does not block the GUI.
 * <p>
 * Note that the {@link IOTable} which needs to be passed to the constructor is only stored via
 * {@link WeakReference}, so no memory leak can happen here.
 * 
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltMetaDataStatisticsModel {

	/**
	 * Enum indicating the supported types of sorting.
	 */
	public enum SortingType {
		NAME, TYPE, MISSING;
	}

	/**
	 * Enum indicating if sorting should be done ascending or descending.
	 */
	public enum SortingDirection {
		UNDEFINED, ASCENDING, DESCENDING;
	}

	/** the size of each page: {@value} */
	public static final int PAGE_SIZE = 1000;

	/** event listener for this model */
	private final EventListenerList eventListener;

	/** {@link WeakReference} on the {@link IOTable} */
	private final WeakReference<IOTable> weakTable;

	/** this map contains every {@link AbstractBeltColumnStatisticsModel} and a boolean indicating if
	 * the model is visible or not (filters) */
	private final Map<AbstractBeltColumnStatisticsModel, Boolean> mapOfStatModelVisibility;

	/** stores the number of stat models which are visible. Used for performance reasons (100.000+
	 * columns iteration is slow) */
	private int visibleCount;

	/** the ordered list containing all {@link AbstractBeltColumnStatisticsModel}s in the correct
	 * order */
	private List<AbstractBeltColumnStatisticsModel> orderedModelList;

	/** holds the sorting settings for all possible sorting types */
	private final Map<SortingType, SortingDirection> mapOfSortingSettings;

	/** the column name filter {@link String} */
	private String filterNameString;

	/** if true, shows special columns */
	private boolean showSpecialColumns;

	/** if true, shows regular columns */
	private boolean showRegularColumns;

	/** if true, columns with missing values will be shown */
	private boolean showOnlyMissingsColumns;

	/** if this is {@code true}, filtering and sorting is allowed; otherwise it is not */
	private volatile boolean allowSortingAndFiltering;

	/** the index of the current page of {@link BeltColumnStatisticsPanel}s */
	private int currentPageIndex;

	/** indicates for all {@link Ontology#VALUE_TYPE}s if they are visible */
	private final Map<Column.Category, Boolean> mapOfValueTypeVisibility;

	/**
	 * Creates a new {@link BeltMetaDataStatisticsModel} instance.
	 *
	 * @param table
	 *            the {@link IOTable} for which the meta data statistics should be created. No
	 *            reference to it is stored to prevent memory leaks.
	 */
	public BeltMetaDataStatisticsModel(final IOTable table) {
		this.weakTable = new WeakReference<>(table);
		this.eventListener = new EventListenerList();
		visibleCount = -1;
		currentPageIndex = 0;

		mapOfStatModelVisibility = new HashMap<>();
		mapOfSortingSettings = new EnumMap<>(SortingType.class);
		mapOfValueTypeVisibility = new EnumMap<>(Column.Category.class);
		for (Column.Category category : Column.Category.values()) {
			mapOfValueTypeVisibility.put(category, Boolean.TRUE);
		}
		allowSortingAndFiltering = false;
		orderedModelList = new ArrayList<>();	// has to be ArrayList because we access the index
												// from 1-n on filtering
		showSpecialColumns = true;
		showRegularColumns = true;
		showOnlyMissingsColumns = false;
		filterNameString = "";
	}

	/**
	 * Returns the ordered {@link List} of {@link AbstractBeltColumnStatisticsModel}s.
	 * 
	 * @return the ordered models
	 */
	public List<AbstractBeltColumnStatisticsModel> getOrderedColumnStatisticsModels() {
		return orderedModelList;
	}

	/**
	 * Returns the total number of {@link AbstractBeltColumnStatisticsModel}s in this model.
	 * 
	 * @return the total number of models
	 */
	public int getTotalSize() {
		if (!mapOfStatModelVisibility.keySet().isEmpty()) {
			return mapOfStatModelVisibility.keySet().size();
		}
		return 0;
	}

	/**
	 * Returns the number of {@link AbstractBeltColumnStatisticsModel}s in this model which are
	 * currently visible.
	 * 
	 * @return the visible columns
	 */
	public int getVisibleSize() {
		if (visibleCount == -1) {
			return getTotalSize();
		}
		return visibleCount;
	}

	/**
	 * Sets the number of visible columns.
	 *
	 * @param visibleCount
	 * 		the visible columns
	 */
	public void setVisibleCount(final int visibleCount) {
		this.visibleCount = visibleCount;
	}

	/**
	 * Returns the {@link SortingDirection} for the given {@link SortingType}.
	 *
	 * @param type
	 * 		the sorting type
	 * @return the sorting direction
	 */
	public SortingDirection getSortingDirection(final SortingType type) {
		return mapOfSortingSettings.get(type);
	}

	/**
	 * Set the {@link SortingDirection} for the specified {@link SortingType}.
	 *
	 * @param type
	 * 		the sorting type
	 * @param direction
	 * 		the sorting direction
	 */
	public void setSortingDirection(final SortingType type, final SortingDirection direction) {
		mapOfSortingSettings.put(type, direction);
	}

	/**
	 * Returns if the given {@link AbstractBeltColumnStatisticsModel} is visible. Returns {@code null} if the given
	 * stat
	 * model is unknown to the model.
	 *
	 * @param model
	 * 		the model
	 * @return whether the model is visible
	 */
	public Boolean isColumnStatisticsModelsVisible(final AbstractBeltColumnStatisticsModel model) {
		return mapOfStatModelVisibility.get(model);
	}

	/**
	 * Sets if the given {@link AbstractBeltColumnStatisticsModel} is visible.
	 *
	 * @param model
	 * 		the model
	 * @return the original visibility value
	 */
	public Boolean setColumnStatisticsModelVisible(final AbstractBeltColumnStatisticsModel model, final boolean visible) {
		return mapOfStatModelVisibility.put(model, visible);
	}

	/**
	 * The {@link String} to filter column names against.
	 * 
	 * @return the filter name
	 */
	public String getFilterNameString() {
		return filterNameString;
	}

	/**
	 * Set the column name filter.
	 *
	 * @param filterNameString
	 * 		the filter name
	 */
	public void setFilterNameString(final String filterNameString) {
		this.filterNameString = filterNameString;
	}

	/**
	 * If {@code true}, show only columns with missing values.
	 * 
	 * @return whether missing columns should be shown
	 */
	public boolean isShowOnlyMissingsColumns() {
		return showOnlyMissingsColumns;
	}

	/**
	 * Sets whether only columns with missing values should be shown.
	 *
	 * @param showOnlyMissingsColumns
	 * 		whether missing columns should be shown
	 */
	public void setShowOnlyMissingsColumns(final boolean showOnlyMissingsColumns) {
		this.showOnlyMissingsColumns = showOnlyMissingsColumns;
	}

	/**
	 * If {@code true}, show special columns.
	 * 
	 * @return whether regular columns should be shown
	 */
	public boolean isShowSpecialColumns() {
		return showSpecialColumns;
	}

	/**
	 * Sets whether special columns should be shown.
	 *
	 * @param showSpecialColumns
	 * 		whether special columns should be shown
	 */
	public void setShowSpecialColumns(final boolean showSpecialColumns) {
		this.showSpecialColumns = showSpecialColumns;
	}

	/**
	 * If {@code true}, show regular columns.
	 * 
	 * @return whether regular columns should be shown
	 */
	public boolean isShowRegularColumns() {
		return showRegularColumns;
	}

	/**
	 * Sets whether regular columns should be shown.
	 *
	 * @param showRegularColumns
	 * 		whether regular columns should be shown
	 */
	public void setShowRegularColumns(final boolean showRegularColumns) {
		this.showRegularColumns = showRegularColumns;
	}

	/**
	 * Tries to return the {@link IOTable} for this model. However this is stored via a
	 * {@link WeakReference}, so it might be gone when this method is called.
	 * 
	 * @return the table or {@code null}
	 */
	public IOTable getTableOrNull() {
		return weakTable.get();
	}

	/**
	 * Sets the visibility of {@link com.rapidminer.belt.column.Column.Category}s.
	 *
	 * @param category
	 * 		the category for which to change the visibility
	 * @param visible
	 * 		if the category should be visible
	 * @return the previous value
	 */
	public Boolean setColumnTypeVisibility(Column.Category category, final boolean visible) {
		return mapOfValueTypeVisibility.put(category, visible);
	}

	/**
	 * Returns the visibility for the given category.
	 *
	 * @param category
	 * 		the category to check
	 * @return if the category is visible
	 */
	public Boolean getColumnTypeVisibility(Column.Category category) {
		return mapOfValueTypeVisibility.get(category);
	}

	/**
	 * Adds a {@link MetaDataStatisticsEventListener} which will be informed of all changes to this
	 * model.
	 *
	 * @param listener
	 * 		the listener
	 */
	public void registerEventListener(final MetaDataStatisticsEventListener listener) {
		eventListener.add(MetaDataStatisticsEventListener.class, listener);
	}

	/**
	 * Removes the {@link MetaDataStatisticsEventListener} from this model.
	 *
	 * @param listener
	 * 		the listener
	 */
	public void removeEventListener(final MetaDataStatisticsEventListener listener) {
		eventListener.remove(MetaDataStatisticsEventListener.class, listener);
	}

	/**
	 * Returns the number of pages which are currently needed to display all visible stat models.
	 * 
	 * @return the total number of pages
	 */
	public int getNumberOfPages() {
		// double cast is needed so 1001/1000 returns 2 pages
		int pages = (int) Math.ceil((double) getVisibleSize() / BeltMetaDataStatisticsModel.PAGE_SIZE);
		// no pages = 1 page
		pages = pages == 0 ? 1 : pages;
		return pages;
	}

	/**
	 * Sets the current page index. Note that index stats at 0.
	 *
	 * @param index
	 * 		the current page index
	 */
	public void setCurrentPageIndex(final int index) {
		currentPageIndex = index;
	}

	/**
	 * Returns the currently active page index. Note that index starts at 0.
	 * 
	 * @return the currently active page
	 */
	public int getCurrentPageIndex() {
		return currentPageIndex;
	}

	/**
	 * Returns {@code true} if the user applied a filter; {@code false} otherwise.
	 * 
	 * @return whether a filter is used
	 */
	public boolean isFiltering() {
		return getVisibleSize() < getTotalSize();
	}

	/**
	 * Sets sorting and filtering to allowed.
	 */
	public void setAllowSortingAndFiltering() {
		allowSortingAndFiltering = true;
	}

	/**
	 * Returns {@code true} if filtering and sorting is allowed; {@code false} otherwise.
	 * Both are only allowed after the data has been fully displayed and calculated.
	 * 
	 * @return the allowSortingAndFiltering whether sorting and filtering is allowed
	 */
	public boolean isSortingAndFilteringAllowed() {
		return allowSortingAndFiltering;
	}

	/**
	 * Sets the list of ordered {@link AbstractBeltColumnStatisticsModel}s.
	 *
	 * @param orderedList
	 * 		the ordered models
	 */
	public void setOrderedModelList(final List<AbstractBeltColumnStatisticsModel> orderedList) {
		this.orderedModelList = new ArrayList<>(orderedList);
	}

	/**
	 * Fire when the initialization is complete.
	 */
	public void fireInitDoneEvent() {
		fireEvent(EventType.INIT_DONE);
	}

	/**
	 * Fire when pagination has changed.
	 */
	public void firePaginationChangedEvent() {
		fireEvent(EventType.PAGINATION_CHANGED);
	}

	/**
	 * Fire when the filters have changed.
	 */
	public void fireFilterChangedEvent() {
		fireEvent(EventType.FILTER_CHANGED);
	}

	/**
	 * Fire when the order of columns has changed.
	 */
	public void fireOrderChangedEvent() {
		fireEvent(EventType.ORDER_CHANGED);
	}

	/**
	 * Fires the given {@link EventType}.
	 */
	private void fireEvent(final EventType type) {
		Object[] listeners = eventListener.getListenerList();
		// Process the listeners last to first
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == MetaDataStatisticsEventListener.class) {
				MetaDataStatisticsEvent e = new MetaDataStatisticsEvent(type);
				((MetaDataStatisticsEventListener) listeners[i + 1]).modelChanged(e);
			}
		}
	}

}
