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
package com.rapidminer.gui.viewer.metadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel.SortingDirection;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel.SortingType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * This class is the controller for the {@link MetaDataStatisticsViewer}. It makes changes to the
 * {@link MetaDataStatisticsModel} when the view tells it the user changes something.
 *
 * @author Marco Boeck
 *
 */
public class MetaDataStatisticsController {

	/**
	 * the barrier which is used to update the stats of all {@link AttributeStatisticsPanel}s once
	 * the {@link ExampleSet} statistics have been calculated
	 */
	private final CountDownLatch barrier;

	private volatile boolean aborted = false;

	/** the model backing this */
	private MetaDataStatisticsModel model;

	/** the {@link UpdateQueue} used to sort */
	private UpdateQueue sortingQueue;

	/** the {@link ProgressThread} to recalculate statistics */
	private ProgressThread worker;

	/**
	 * needed to restore the initial order; faster way as opposed to sorting (which is not easily
	 * possible because role information is not available)
	 */
	private List<AbstractAttributeStatisticsModel> backupInitialOrderList;

	/**
	 * Creates a new {@link MetaDataStatisticsController} instance. Does not store
	 *
	 * @param view
	 * @param model
	 */
	public MetaDataStatisticsController(MetaDataStatisticsViewer view, MetaDataStatisticsModel model) {
		if (model.getExampleSetOrNull() == null) {
			throw new IllegalArgumentException("model exampleSet must not be null at construction time!");
		}
		this.model = model;

		backupInitialOrderList = new ArrayList<>();
		barrier = new CountDownLatch(1);

		// start up sorting queue (for future sorting)
		sortingQueue = new UpdateQueue("Attribute Sorting");
		sortingQueue.start();

		// init sorting to none
		resetSorting();

		calculateStatistics(model.getExampleSetOrNull());
	}

	/**
	 * Call to let the controller know that GUI is done. Once statistics calculation is also done (aka count down latch
	 * counted down), the GUI will be notified to display everything.
	 *
	 * @return whether the statistics calculation has been successful
	 */
	boolean waitAtBarrier() {
		try {
			// GUI is done, wait until calculations are done and the GUI can be updated
			barrier.await();
			if (aborted) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.meta_data_view.calc_cancelled");
				return false;
			} else {
				updateStatistics();
				return true;
			}
		} catch (InterruptedException e) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.meta_data_view.calc_interrupted");
			return false;
		}
	}

	/**
	 * Changes the current page index to the first page (if possible).
	 */
	public void setCurrentPageIndexToFirstPage() {
		if (model.getCurrentPageIndex() > 0) {
			model.setCurrentPageIndex(0);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Decrements the current page index (if possible).
	 */
	public void decrementCurrentPageIndex() {
		if (model.getCurrentPageIndex() > 0) {
			model.setCurrentPageIndex(model.getCurrentPageIndex() - 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Increments the current page index (if possible).
	 */
	public void incrementCurrentPageIndex() {
		if (model.getCurrentPageIndex() < model.getNumberOfPages() - 1) {
			model.setCurrentPageIndex(model.getCurrentPageIndex() + 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Changes the current page index to the last page (if possible).
	 */
	public void setCurrentPageIndexToLastPage() {
		if (model.getCurrentPageIndex() < model.getNumberOfPages() - 1) {
			model.setCurrentPageIndex(model.getNumberOfPages() - 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Changes the current page to the human index page (which obviously starts at 1 rather than 0).
	 *
	 * @param humanPageIndex
	 */
	public void jumpToHumanPageIndex(int humanPageIndex) {
		if (model.getCurrentPageIndex() != humanPageIndex - 1) {
			model.setCurrentPageIndex(humanPageIndex - 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Call when the attribute name sorting should be cycled.
	 */
	public void cycleAttributeNameSorting() {
		SortingDirection direction = model.getSortingDirection(SortingType.NAME);

		switch (direction) {
			case UNDEFINED:
				setSorting(SortingType.NAME, SortingDirection.DESCENDING);
				break;
			case DESCENDING:
				setSorting(SortingType.NAME, SortingDirection.ASCENDING);
				break;
			case ASCENDING:
				setSorting(SortingType.NAME, SortingDirection.UNDEFINED);
				break;
			default:
				setSorting(SortingType.NAME, SortingDirection.UNDEFINED);
		}
	}

	/**
	 * Call when the attribute type sorting should be cycled.
	 */
	public void cycleAttributeTypeSorting() {
		SortingDirection direction = model.getSortingDirection(SortingType.TYPE);

		switch (direction) {
			case UNDEFINED:
				setSorting(SortingType.TYPE, SortingDirection.DESCENDING);
				break;
			case DESCENDING:
				setSorting(SortingType.TYPE, SortingDirection.ASCENDING);
				break;
			case ASCENDING:
				setSorting(SortingType.TYPE, SortingDirection.UNDEFINED);
				break;
			default:
				setSorting(SortingType.TYPE, SortingDirection.UNDEFINED);
		}
	}

	/**
	 * Call when the attribute missings sorting should be cycled.
	 */
	public void cycleAttributeMissingSorting() {
		SortingDirection direction = model.getSortingDirection(SortingType.MISSING);

		switch (direction) {
			case UNDEFINED:
				setSorting(SortingType.MISSING, SortingDirection.DESCENDING);
				break;
			case DESCENDING:
				setSorting(SortingType.MISSING, SortingDirection.ASCENDING);
				break;
			case ASCENDING:
				setSorting(SortingType.MISSING, SortingDirection.UNDEFINED);
				break;
			default:
				setSorting(SortingType.MISSING, SortingDirection.UNDEFINED);
		}
	}

	/**
	 * Sets the {@link List} of ordered {@link AbstractAttributeStatisticsModel}s.
	 *
	 * @param list
	 */
	public void setAttributeStatisticsModels(List<AbstractAttributeStatisticsModel> orderedModelList) {
		model.setOrderedModelList(new ArrayList<>(orderedModelList));
		this.backupInitialOrderList = new ArrayList<>(orderedModelList);
		for (AbstractAttributeStatisticsModel statModel : orderedModelList) {
			model.setAttributeStatisticsModelVisible(statModel, true);
		}
	}

	/**
	 * Set the attribute name filter.
	 *
	 * @param filterNameString
	 */
	public void setFilterNameString(String filterNameString) {
		model.setFilterNameString(filterNameString);
		applyFilters();
	}

	/**
	 * Sets whether only attributes with missing values should be shown.
	 *
	 * @param showOnlyMissingsAttributes
	 */
	public void setShowOnlyMissingsAttributes(boolean showOnlyMissingsAttributes) {
		model.setShowOnlyMissingsAttributes(showOnlyMissingsAttributes);
		applyFilters();
	}

	/**
	 * Sets whether special attributes should be shown.
	 *
	 * @param showSpecialAttributes
	 */
	public void setShowSpecialAttributes(boolean showSpecialAttributes) {
		model.setShowSpecialAttributes(showSpecialAttributes);
		applyFilters();
	}

	/**
	 * Sets whether regular attributes should be shown.
	 *
	 * @param showRegularAttributes
	 */
	public void setShowRegularAttributes(boolean showRegularAttributes) {
		model.setShowRegularAttributes(showRegularAttributes);
		applyFilters();
	}

	/**
	 * Sets the visibility of {@link Ontology#VALUE_TYPE}s.
	 *
	 * @param valueType
	 * @param visible
	 */
	public void setAttributeTypeVisibility(int valueType, boolean visible) {
		model.setAttributeTypeVisibility(valueType, visible);
		applyFilters();
	}

	/**
	 * Returns the ordered {@link List} of {@link AbstractAttributeStatisticsModel}s of the given
	 * page index. Only returns models which are visible and only returns max {@value #PAGE_SIZE}
	 * models. If pageIndex * {@value #PAGE_SIZE} > {@link #getTotalSize()} returns an empty list.
	 *
	 * @return
	 */
	public List<AbstractAttributeStatisticsModel> getPagedAndVisibleAttributeStatisticsModels() {
		List<AbstractAttributeStatisticsModel> resultList = new LinkedList<>();

		// this would be the starting index of no other stat models where hidden before this index
		int i = model.getCurrentPageIndex() * MetaDataStatisticsModel.PAGE_SIZE;
		// but we need to know how many are hidden before this index
		int hiddenCount = 0;
		for (int j = Math.min(i, model.getTotalSize() - 1); j >= 0; j--) {
			if (!model.isAttributeStatisticsModelsVisible(model.getOrderedAttributeStatisticsModels().get(j))) {
				hiddenCount++;
			}
		}
		// now add hidden count to starting index, because that many more elements are shown on the
		// page(s) before
		i = i + hiddenCount;
		int count = 1;
		while (i < model.getTotalSize() && count <= MetaDataStatisticsModel.PAGE_SIZE) {
			AbstractAttributeStatisticsModel statMmodel = model.getOrderedAttributeStatisticsModels().get(i++);
			if (model.isAttributeStatisticsModelsVisible(statMmodel)) {
				resultList.add(statMmodel);
				count++;
			}
		}

		return resultList;
	}

	/**
	 * Stops the statistics recalculation and the sorting queue.
	 */
	void stop() {
		worker.cancel();
		sortingQueue.shutdown();
	}

	/**
	 * Sets the desired {@link SortingDirection} for the given {@link SortingType}.
	 *
	 * @param type
	 * @param direction
	 */
	private void setSorting(SortingType type, SortingDirection direction) {
		// make sure we are allowed to do so
		if (!model.isSortingAndFilteringAllowed()) {
			return;
		}

		// we only allow one type of sorting at the same time, so reset everything beforehand
		resetSorting();

		model.setSortingDirection(type, direction);

		applySorting();
	}

	/**
	 * Applies the current filters.
	 */
	private void applyFilters() {
		// make sure we are allowed to do so
		if (!model.isSortingAndFilteringAllowed()) {
			return;
		}

		// count this for performance reasons
		int visibleCount = model.getTotalSize();
		// reset filters on empty filter string
		if ("".equals(model.getFilterNameString())) {
			for (AbstractAttributeStatisticsModel statModel : backupInitialOrderList) {
				model.setAttributeStatisticsModelVisible(statModel, true);
			}
		} else {
			// apply filter on non empty string
			for (AbstractAttributeStatisticsModel statModel : model.getOrderedAttributeStatisticsModels()) {
				String attName = statModel.getAttribute().getName();
				boolean show = attName.toLowerCase().contains(model.getFilterNameString().toLowerCase());
				if (!show) {
					visibleCount--;
				}
				model.setAttributeStatisticsModelVisible(statModel, show);
			}
		}

		// apply attribute filters
		for (AbstractAttributeStatisticsModel statModel : model.getOrderedAttributeStatisticsModels()) {
			// if special attributes should not be visible and the attribute is special, hide it
			if (!model.isShowSpecialAttributes() && statModel.isSpecialAtt()) {
				if (model.setAttributeStatisticsModelVisible(statModel, false)) {
					visibleCount--;
				}
			}

			// if regular attributes should not be visible and the attribute is regular, hide it
			if (!model.isShowRegularAttributes() && !statModel.isSpecialAtt()) {
				if (model.setAttributeStatisticsModelVisible(statModel, false)) {
					visibleCount--;
				}
			}

			// if attribute has no missing values and only missing values should be visible, hide it
			ExampleSet exSet = model.getExampleSetOrNull();
			if (exSet != null) {
				if (model.isShowOnlyMissingsAttributes()
						&& exSet.getStatistics(statModel.getAttribute(), Statistics.UNKNOWN) <= 0) {
					if (model.setAttributeStatisticsModelVisible(statModel, false)) {
						visibleCount--;
					}
				}
			}

			// if the attribute value type should not be visible, hide it
			boolean showValueType = true;
			for (String valueTypeString : Ontology.VALUE_TYPE_NAMES) {
				int valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeString);
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(statModel.getAttribute().getValueType(), valueType)) {
					showValueType &= model.getAttributeTypeVisibility(valueType);
					if (!showValueType) {
						break;
					}
				}
			}
			if (!showValueType) {
				if (model.setAttributeStatisticsModelVisible(statModel, false)) {
					visibleCount--;
				}
			}
		}
		model.setVisibleCount(visibleCount);

		// update pagination system to current data
		if (model.getCurrentPageIndex() >= model.getNumberOfPages()) {
			// not firing this here because we fire filter changed directly after and they overlap
			// here
			model.setCurrentPageIndex(0);
		}

		model.fireFilterChangedEvent();
	}

	/**
	 * Applies the current sorting.
	 */
	private void applySorting() {
		sortingQueue.execute(() -> {
			if (model.getSortingDirection(SortingType.NAME) != SortingDirection.UNDEFINED) {
				sortByName(model.getOrderedAttributeStatisticsModels(), model.getSortingDirection(SortingType.NAME));
			}
			if (model.getSortingDirection(SortingType.TYPE) != SortingDirection.UNDEFINED) {
				sortByType(model.getOrderedAttributeStatisticsModels(), model.getSortingDirection(SortingType.TYPE));
			}
			if (model.getSortingDirection(SortingType.MISSING) != SortingDirection.UNDEFINED) {
				sortByMissing(model.getOrderedAttributeStatisticsModels(),
						model.getSortingDirection(SortingType.MISSING));
			}

			model.fireOrderChangedEvent();
		});
	}

	/**
	 * Sorts the {@link AbstractAttributeStatisticsModel}s via their attribute names.
	 *
	 * @param direction
	 */
	private void sortByName(List<AbstractAttributeStatisticsModel> list, final SortingDirection direction) {
		sort(list, (o1, o2) -> {
			int sortResult = o1.getAttribute().getName().compareTo(o2.getAttribute().getName());
			switch (direction) {
				case ASCENDING:
					return -1 * sortResult;
				case DESCENDING:
					return sortResult;
				case UNDEFINED:
					return 0;
				default:
					return sortResult;
			}
		});
	}

	/**
	 * Sorts the {@link AbstractAttributeStatisticsModel}s via their attribute types.
	 *
	 * @param direction
	 */
	private void sortByType(List<AbstractAttributeStatisticsModel> list, final SortingDirection direction) {
		sort(list, (o1, o2) -> {
			int sortResult = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(o1.getAttribute().getValueType())
					.compareTo(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(o2.getAttribute().getValueType()));
			switch (direction) {
				case ASCENDING:
					return -1 * sortResult;
				case DESCENDING:
					return sortResult;
				case UNDEFINED:
					return 0;
				default:
					return sortResult;
			}
		});
	}

	/**
	 * Sorts the {@link AbstractAttributeStatisticsModel}s via their attribute types.
	 *
	 * @param direction
	 */
	private void sortByMissing(List<AbstractAttributeStatisticsModel> list, final SortingDirection direction) {
		sort(list, (o1, o2) -> {
			ExampleSet exSet = model.getExampleSetOrNull();
			if (exSet == null) {
				return 0;
			}

			Double missing1 = exSet.getStatistics(o1.getAttribute(), Statistics.UNKNOWN);
			Double missing2 = exSet.getStatistics(o2.getAttribute(), Statistics.UNKNOWN);

			if (missing1 == null || missing2 == null) {
				return 0;
			}
			int sortResult = missing1.compareTo(missing2);
			switch (direction) {
				case ASCENDING:
					return -1 * sortResult;
				case DESCENDING:
					return sortResult;
				case UNDEFINED:
					return 0;
				default:
					return sortResult;
			}
		});
	}

	/**
	 * Resets the current sorting to none and restores the initial order of the
	 * {@link AbstractAttributeStatisticsModel}s.
	 */
	private void resetSorting() {
		for (SortingType type : SortingType.values()) {
			model.setSortingDirection(type, SortingDirection.UNDEFINED);
		}

		restoreInitialStatModelOrder();
	}

	/**
	 * Restores the initial order of the attribute stat models (special at the top, regular below).
	 * This is done because we no longer may have access to attribute roles after construction time.
	 */
	private void restoreInitialStatModelOrder() {
		model.setOrderedModelList(backupInitialOrderList);
	}

	/**
	 * Update the statistics part of all {@link AttributeStatisticsPanel}s.
	 */
	private void updateStatistics() {
		ExampleSet exampleSet = model.getExampleSetOrNull();
		if (exampleSet == null) {
			throw new IllegalArgumentException("model exampleSet must not be null at construction time!");
		}

		// update stats on all attribute stat models
		for (AbstractAttributeStatisticsModel statModel : model.getOrderedAttributeStatisticsModels()) {
			statModel.updateStatistics(exampleSet);
		}

		// allow sorting and filtering
		model.setAllowSortingAndFiltering();

		// signal that everything is done
		model.fireInitDoneEvent();
	}

	/**
	 * Calculates the statistics of the given {@link ExampleSet} in a {@link ProgressThread}. Once the statistics are
	 * calculated, will update the stats on all {@link AttributeStatisticsPanel}s.
	 *
	 * @param exampleSet
	 * 		the example of which to recalculate the statistics
	 */
	private void calculateStatistics(final ExampleSet exampleSet) {

		// wrap into a future task so that cancelling with an interrupt is possible
		FutureTask<Void> task = new FutureTask<>(() -> {
			exampleSet.recalculateAllAttributeStatistics();
			barrier.countDown();
			return null;
		});

		//execute with indeterminate progress thread
		worker = new ProgressThread("statistics_calculation") {

			@Override
			public void run() {
				task.run();
			}

			@Override
			protected void executionCancelled() {
				task.cancel(true);
				aborted = true;
				barrier.countDown();
			}
		};
		worker.setIndeterminate(true);
		worker.start();
	}

	/**
	 * Sorts the given {@link List} of {@link AbstractAttributeStatisticsModel}s with the given
	 * {@link Comparator}.
	 *
	 * @param listOfStatModels
	 * @param comp
	 */
	private static void sort(List<AbstractAttributeStatisticsModel> listOfStatModels,
							 Comparator<AbstractAttributeStatisticsModel> comp) {
		listOfStatModels.sort(comp);
	}

}
