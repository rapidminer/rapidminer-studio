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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.MetaDataStatisticsViewer;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEvent;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEvent.EventType;
import com.rapidminer.gui.viewer.metadata.event.MetaDataStatisticsEventListener;
import com.rapidminer.tools.Ontology;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.EventListenerList;


/**
 * Data model and controller for the {@link MetaDataStatisticsViewer}. The statistics for the
 * {@link ExampleSet} are created in a {@link com.rapidminer.gui.tools.MultiSwingWorker}, so model creation does not block the
 * GUI.
 * <p>
 * Note that the {@link ExampleSet} which needs to be pased to the constructor is only stored via
 * {@link WeakReference}, so no example set memory leak can happen here.
 * 
 * @author Marco Boeck
 * 
 */
public class MetaDataStatisticsModel {

	/**
	 * Enum indicating the supported types of sorting.
	 * 
	 */
	public static enum SortingType {
		NAME, TYPE, MISSING;
	}

	/**
	 * Enum indicating if sorting should be done ascending or descending.
	 * 
	 */
	public static enum SortingDirection {
		UNDEFINED, ASCENDING, DESCENDING;
	}

	/** the size of each page: {@value} */
	public static final int PAGE_SIZE = 1000;

	/** event listener for this model */
	private final EventListenerList eventListener;

	/** {@link WeakReference} on the {@link ExampleSet} */
	private final WeakReference<ExampleSet> weakExampleSet;

	/**
	 * this map contains every {@link AbstractAttributeStatisticsModel} and a boolean indicating if
	 * the model is visible or not (filters)
	 */
	private final Map<AbstractAttributeStatisticsModel, Boolean> mapOfStatModelVisibility;

	/**
	 * stores the number of stat models which are visible. Used for performance reasons (100.000+
	 * atributes iteration is slow)
	 */
	private int visibleCount;

	/**
	 * the ordered list containing all {@link AbstractAttributeStatisticsModel}s in the correct
	 * order
	 */
	private List<AbstractAttributeStatisticsModel> orderedModelList;

	/** holds the sorting settings for all possible sorting types */
	private final Map<SortingType, SortingDirection> mapOfSortingSettings;

	/** the attribute name filter {@link String} */
	private String filterNameString;

	/** if true, shows special attributes */
	private boolean showSpecialAttributes;

	/** if true, shows regular attributes */
	private boolean showRegularAttributes;

	/** if true, attributes with missing values will be shown */
	private boolean showOnlyMissingsAttributes;

	/** if this is <code>true</code>, filtering and sorting is allowed; otherwise it is not */
	private volatile boolean allowSortingAndFiltering;

	/** the index of the current page of {@link AttributeStatisticsPanel}s */
	private int currentPageIndex;

	/** indicates for all {@link Ontology#VALUE_TYPE}s if they are visible */
	private final Map<Integer, Boolean> mapOfValueTypeVisibility;

	/**
	 * Creates a new {@link MetaDataStatisticsModel} instance.
	 * 
	 * @param exampleSet
	 *            the {@link ExampleSet} for which the meta data statistics should be created. No
	 *            reference to it is stored to prevent memory leaks.
	 */
	public MetaDataStatisticsModel(final ExampleSet exampleSet) {
		this.weakExampleSet = new WeakReference<>(exampleSet);
		this.eventListener = new EventListenerList();
		visibleCount = -1;
		currentPageIndex = 0;

		mapOfStatModelVisibility = new HashMap<>();
		mapOfSortingSettings = new HashMap<>();
		mapOfValueTypeVisibility = new HashMap<>();
		for (String valueTypeString : Ontology.VALUE_TYPE_NAMES) {
			int valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeString);
			mapOfValueTypeVisibility.put(valueType, Boolean.TRUE);
		}
		allowSortingAndFiltering = false;
		orderedModelList = new ArrayList<>();	// has to be ArrayList because we access the index
												// from 1-n on filtering
		showSpecialAttributes = true;
		showRegularAttributes = true;
		showOnlyMissingsAttributes = false;
		filterNameString = "";
	}

	/**
	 * Returns the ordered {@link List} of {@link AbstractAttributeStatisticsModel}s.
	 * 
	 * @return
	 */
	public List<AbstractAttributeStatisticsModel> getOrderedAttributeStatisticsModels() {
		return orderedModelList;
	}

	/**
	 * Returns the total number of {@link AbstractAttributeStatisticsModel}s in this model.
	 * 
	 * @return
	 */
	public int getTotalSize() {
		if (!mapOfStatModelVisibility.keySet().isEmpty()) {
			return mapOfStatModelVisibility.keySet().size();
		}
		return 0;
	}

	/**
	 * Returns the number of {@link AbstractAttributeStatisticsModel}s in this model which are
	 * currently visible.
	 * 
	 * @return
	 */
	public int getVisibleSize() {
		if (visibleCount == -1) {
			return getTotalSize();
		}
		return visibleCount;
	}

	/**
	 * Sets the number of visible attributes.
	 * 
	 * @param visibleCount
	 */
	public void setVisibleCount(final int visibleCount) {
		this.visibleCount = visibleCount;
	}

	/**
	 * Returns the {@link SortingDirection} for the given {@link SortingType}.
	 * 
	 * @param type
	 * @return
	 */
	public SortingDirection getSortingDirection(final SortingType type) {
		return mapOfSortingSettings.get(type);
	}

	/**
	 * Set the {@link SortingDirection} for the specified {@link SortingType}.
	 * 
	 * @param type
	 * @param direction
	 */
	public void setSortingDirection(final SortingType type, final SortingDirection direction) {
		mapOfSortingSettings.put(type, direction);
	}

	/**
	 * Returns if the given {@link AbstractAttributeStatisticsModel} is visible. Returns
	 * <code>null</code> if the given stat model is unknown to the model.
	 * 
	 * @param model
	 * @return
	 */
	public Boolean isAttributeStatisticsModelsVisible(final AbstractAttributeStatisticsModel model) {
		return mapOfStatModelVisibility.get(model);
	}

	/**
	 * Sets if the given {@link AbstractAttributeStatisticsModel} is visible.
	 * 
	 * @param model
	 * @return the original visibility value
	 */
	public Boolean setAttributeStatisticsModelVisible(final AbstractAttributeStatisticsModel model, final boolean visible) {
		return mapOfStatModelVisibility.put(model, visible);
	}

	/**
	 * The {@link String} to filter attribute names against.
	 * 
	 * @return
	 */
	public String getFilterNameString() {
		return filterNameString;
	}

	/**
	 * Set the attribute name filter.
	 * 
	 * @param filterNameString
	 */
	public void setFilterNameString(final String filterNameString) {
		this.filterNameString = filterNameString;
	}

	/**
	 * If <code>true</code>, show only attributes with missing values.
	 * 
	 * @return
	 */
	public boolean isShowOnlyMissingsAttributes() {
		return showOnlyMissingsAttributes;
	}

	/**
	 * Sets whether only attributes with missing values should be shown.
	 * 
	 * @param showOnlyMissingsAttributes
	 */
	public void setShowOnlyMissingsAttributes(final boolean showOnlyMissingsAttributes) {
		this.showOnlyMissingsAttributes = showOnlyMissingsAttributes;
	}

	/**
	 * If <code>true</code>, show special attributes.
	 * 
	 * @return
	 */
	public boolean isShowSpecialAttributes() {
		return showSpecialAttributes;
	}

	/**
	 * Sets whether special attributes should be shown.
	 * 
	 * @param showSpecialAttributes
	 */
	public void setShowSpecialAttributes(final boolean showSpecialAttributes) {
		this.showSpecialAttributes = showSpecialAttributes;
	}

	/**
	 * If <code>true</code>, show regular attributes.
	 * 
	 * @return
	 */
	public boolean isShowRegularAttributes() {
		return showRegularAttributes;
	}

	/**
	 * Sets whether regular attributes should be shown.
	 * 
	 * @param showRegularAttributes
	 */
	public void setShowRegularAttributes(final boolean showRegularAttributes) {
		this.showRegularAttributes = showRegularAttributes;
	}

	/**
	 * Tries to return the {@link ExampleSet} for this model. However this is stored via a
	 * {@link WeakReference}, so it might be gone when this method is called.
	 * 
	 * @return
	 */
	public ExampleSet getExampleSetOrNull() {
		return weakExampleSet.get();
	}

	/**
	 * Sets the visibility of {@link Ontology#VALUE_TYPE}s.
	 * 
	 * @param valueType
	 * @param visible
	 * @return the previous value
	 */
	public Boolean setAttributeTypeVisibility(final int valueType, final boolean visible) {
		return mapOfValueTypeVisibility.put(valueType, visible);
	}

	/**
	 * Returns the visibility for the given value type.
	 * 
	 * @param valueType
	 * @return
	 */
	public Boolean getAttributeTypeVisibility(final int valueType) {
		return mapOfValueTypeVisibility.get(valueType);
	}

	/**
	 * Adds a {@link MetaDataStatisticsEventListener} which will be informed of all changes to this
	 * model.
	 * 
	 * @param listener
	 */
	public void registerEventListener(final MetaDataStatisticsEventListener listener) {
		eventListener.add(MetaDataStatisticsEventListener.class, listener);
	}

	/**
	 * Removes the {@link MetaDataStatisticsEventListener} from this model.
	 * 
	 * @param listener
	 */
	public void removeEventListener(final MetaDataStatisticsEventListener listener) {
		eventListener.remove(MetaDataStatisticsEventListener.class, listener);
	}

	/**
	 * Returns the number of pages which are currently needed to display all visible stat models.
	 * 
	 * @return
	 */
	public int getNumberOfPages() {
		// double cast is needed so 1001/1000 returns 2 pages
		int pages = (int) Math.ceil((double) getVisibleSize() / MetaDataStatisticsModel.PAGE_SIZE);
		// no pages = 1 page
		pages = pages == 0 ? 1 : pages;
		return pages;
	}

	/**
	 * Sets the current page index. Note that index stats at 0.
	 * 
	 * @param index
	 */
	public void setCurrentPageIndex(final int index) {
		currentPageIndex = index;
	}

	/**
	 * Returns the currently active page index. Note that index starts at 0.
	 * 
	 * @return
	 */
	public int getCurrentPageIndex() {
		return currentPageIndex;
	}

	/**
	 * Returns <code>true</code> if the user applied a filter; <code>false</code> otherwise.
	 * 
	 * @return
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
	 * Returns <code>true</code> if filtering and sorting is allowed; <code>false</code> otherwise.
	 * Both are only allowed after the data has been fully displayed and calculated.
	 * 
	 * @return the allowSortingAndFiltering
	 */
	public boolean isSortingAndFilteringAllowed() {
		return allowSortingAndFiltering;
	}

	/**
	 * Sets the list of ordered {@link AbstractAttributeStatisticsModel}s.
	 * 
	 * @param orderedList
	 */
	public void setOrderedModelList(final List<AbstractAttributeStatisticsModel> orderedList) {
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
	 * Fire when the order of attributes has changed.
	 */
	public void fireOrderChangedEvent() {
		fireEvent(EventType.ORDER_CHANGED);
	}

	/**
	 * Fires the given {@link EventType}.
	 * 
	 * @param type
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
