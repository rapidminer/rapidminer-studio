/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.viewer.metadata;

import static com.rapidminer.gui.viewer.metadata.model.BeltMetaDataStatisticsModel.PAGE_SIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.column.Statistics.Result;
import com.rapidminer.belt.column.Statistics.Statistic;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Table;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.viewer.metadata.model.AbstractBeltColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltMetaDataStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltMetaDataStatisticsModel.SortingDirection;
import com.rapidminer.gui.viewer.metadata.model.BeltMetaDataStatisticsModel.SortingType;
import com.rapidminer.tools.LogService;


/**
 * This class is the controller for the {@link BeltMetaDataStatisticsViewer}. It makes changes to the {@link
 * BeltMetaDataStatisticsModel} when the view tells it the user changes something.
 *
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
class BeltMetaDataStatisticsController {

	private static final Set<Statistics.Statistic> MAXIMAL_STATISTICS = new HashSet<>(Arrays.asList(Statistics.Statistic.COUNT, Statistics.Statistic.MIN,
			Statistics.Statistic.MAX, Statistics.Statistic.MEAN, Statistics.Statistic.SD, Statistics.Statistic.INDEX_COUNTS));

	private static final String INTERRUPTED_MESSAGE = "com.rapidminer.gui.meta_data_view.calc_interrupted";

	private static final String CALC_ERROR_MESSAGE = "com.rapidminer.gui.meta_data_view.calc_error";

	/**
	 * the barrier which is used to update the stats of all {@link BeltColumnStatisticsPanel}s once the {@link
	 * Table} statistics have been calculated
	 */
	private final CountDownLatch barrier;

	private volatile boolean aborted = false;

	/**
	 * the model backing this
	 */
	private BeltMetaDataStatisticsModel model;

	/**
	 * the {@link UpdateQueue} used to sort
	 */
	private UpdateQueue sortingQueue;

	/**
	 * the {@link ProgressThread} to recalculate statistics
	 */
	private ProgressThread worker;

	/**
	 * needed to restore the initial order; faster way as opposed to sorting (which is not easily possible because role
	 * information is not available)
	 */
	private List<AbstractBeltColumnStatisticsModel> backupInitialOrderList;

	private FutureTask<Map<String, Map<Statistic, Result>>> statisticsFuture;

	/**
	 * Creates a new {@link BeltMetaDataStatisticsController} instance.
	 */
	BeltMetaDataStatisticsController(BeltMetaDataStatisticsModel model) {
		if (model.getTableOrNull() == null) {
			throw new IllegalArgumentException("model table must not be null at construction time!");
		}
		this.model = model;

		backupInitialOrderList = new ArrayList<>();
		barrier = new CountDownLatch(1);

		// start up sorting queue (for future sorting)
		sortingQueue = new UpdateQueue("Column Sorting");
		sortingQueue.start();

		// init sorting to none
		resetSorting();

		calculateStatistics(model.getTableOrNull().getTable());
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
			LogService.getRoot().log(Level.INFO, INTERRUPTED_MESSAGE);
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * Changes the current page index to the first page (if possible).
	 */
	void setCurrentPageIndexToFirstPage() {
		if (model.getCurrentPageIndex() > 0) {
			model.setCurrentPageIndex(0);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Decrements the current page index (if possible).
	 */
	void decrementCurrentPageIndex() {
		if (model.getCurrentPageIndex() > 0) {
			model.setCurrentPageIndex(model.getCurrentPageIndex() - 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Increments the current page index (if possible).
	 */
	void incrementCurrentPageIndex() {
		if (model.getCurrentPageIndex() < model.getNumberOfPages() - 1) {
			model.setCurrentPageIndex(model.getCurrentPageIndex() + 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Changes the current page index to the last page (if possible).
	 */
	void setCurrentPageIndexToLastPage() {
		if (model.getCurrentPageIndex() < model.getNumberOfPages() - 1) {
			model.setCurrentPageIndex(model.getNumberOfPages() - 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Changes the current page to the human index page (which obviously starts at 1 rather than 0).
	 *
	 * @param humanPageIndex
	 * 		the index
	 */
	void jumpToHumanPageIndex(int humanPageIndex) {
		if (model.getCurrentPageIndex() != humanPageIndex - 1) {
			model.setCurrentPageIndex(humanPageIndex - 1);
			model.firePaginationChangedEvent();
		}
	}

	/**
	 * Call when the column name sorting should be cycled.
	 */
	void cycleColumnNameSorting() {
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
	 * Call when the column type sorting should be cycled.
	 */
	void cycleColumnTypeSorting() {
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
	 * Call when the column missings sorting should be cycled.
	 */
	void cycleColumnMissingSorting() {
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
	 * Sets the {@link List} of ordered {@link AbstractBeltColumnStatisticsModel}s.
	 *
	 */
	void setColumnStatisticsModels(List<AbstractBeltColumnStatisticsModel> orderedModelList) {
		model.setOrderedModelList(new ArrayList<>(orderedModelList));
		this.backupInitialOrderList = new ArrayList<>(orderedModelList);
		for (AbstractBeltColumnStatisticsModel statModel : orderedModelList) {
			model.setColumnStatisticsModelVisible(statModel, true);
		}
	}

	/**
	 * Set the column name filter.
	 */
	void setFilterNameString(String filterNameString) {
		model.setFilterNameString(filterNameString);
		applyFilters();
	}

	/**
	 * Sets whether only columns with missing values should be shown.
	 *
	 * @param showOnlyMissingsColumns
	 * 		whether to show only columns with missing values
	 */
	void setShowOnlyMissingsColumns(boolean showOnlyMissingsColumns) {
		model.setShowOnlyMissingsColumns(showOnlyMissingsColumns);
		applyFilters();
	}

	/**
	 * Sets whether special columns should be shown.
	 *
	 * @param showSpecialColumns
	 * 		whether columns with a role should be shown
	 */
	void setShowSpecialColumns(boolean showSpecialColumns) {
		model.setShowSpecialColumns(showSpecialColumns);
		applyFilters();
	}

	/**
	 * Sets whether regular columns should be shown.
	 *
	 * @param showRegularColumns
	 * 		whether columns without a role should be shown
	 */
	void setShowRegularColumns(boolean showRegularColumns) {
		model.setShowRegularColumns(showRegularColumns);
		applyFilters();
	}

	/**
	 * Sets the visibility of {@link Column.Category}s.
	 *
	 * @param category
	 * 		the category of concern
	 * @param visible
	 * 		the visibility
	 */
	void setColumnTypeVisibility(Column.Category category, boolean visible) {
		model.setColumnTypeVisibility(category, visible);
		applyFilters();
	}

	/**
	 * Returns the ordered {@link List} of {@link AbstractBeltColumnStatisticsModel}s of the given page index. Only
	 * returns models which are visible and only returns max {@value BeltMetaDataStatisticsModel#PAGE_SIZE} models.
	 * If pageIndex * {@value BeltMetaDataStatisticsModel#PAGE_SIZE} > {@link BeltMetaDataStatisticsModel#getTotalSize()}
	 * returns an empty list.
	 */
	List<AbstractBeltColumnStatisticsModel> getPagedAndVisibleColumnStatisticsModels() {
		List<AbstractBeltColumnStatisticsModel> resultList = new ArrayList<>();

		// this would be the starting index of no other stat models where hidden before this index
		int i = model.getCurrentPageIndex() * PAGE_SIZE;
		// but we need to know how many are hidden before this index
		int hiddenCount = 0;
		for (int j = Math.min(i, model.getTotalSize() - 1); j >= 0; j--) {
			if (!model.isColumnStatisticsModelsVisible(model.getOrderedColumnStatisticsModels().get(j))) {
				hiddenCount++;
			}
		}
		// now add hidden count to starting index, because that many more elements are shown on the
		// page(s) before
		i = i + hiddenCount;
		int count = 1;
		while (i < model.getTotalSize() && count <= PAGE_SIZE) {
			AbstractBeltColumnStatisticsModel statMmodel = model.getOrderedColumnStatisticsModels().get(i++);
			if (model.isColumnStatisticsModelsVisible(statMmodel)) {
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
		visibleCount = updateVisibility(visibleCount);

		IOTable tableOrNull = model.getTableOrNull();
		if (tableOrNull == null) {
			return;
		}
		Table table = tableOrNull.getTable();
		// apply column filters
		for (AbstractBeltColumnStatisticsModel statModel : model.getOrderedColumnStatisticsModels()) {
			// if special columns should not be visible and the column is special, hide it
			if (!model.isShowSpecialColumns() && statModel.isSpecialColumn() && model.setColumnStatisticsModelVisible(statModel, false)) {
				visibleCount--;
			}

			// if regular columns should not be visible and the column is regular, hide it
			if (!model.isShowRegularColumns() && !statModel.isSpecialColumn() && model.setColumnStatisticsModelVisible(statModel, false)) {
				visibleCount--;
			}

			// if column has no missing values and only missing values should be visible, hide it
			visibleCount = filterMissingValues(visibleCount, table, statModel);

			if (table != null) {
				// if the column type should not be visible, hide it
				boolean showColumnType = hideColumnType(table, statModel);
				if (!showColumnType && model.setColumnStatisticsModelVisible(statModel, false)) {
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
	 * Filters by missing values.
	 */
	private int filterMissingValues(int visibleCount, Table table, AbstractBeltColumnStatisticsModel statModel) {
		Map<String, Map<Statistic, Result>> statistics = null;
		try {
			statistics = statisticsFuture.get();
		} catch (InterruptedException e) {
			LogService.getRoot().log(Level.INFO, INTERRUPTED_MESSAGE);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LogService.getRoot().log(Level.INFO, CALC_ERROR_MESSAGE, e);
		}
		if (table != null && statistics != null && model.isShowOnlyMissingsColumns()
				&& (table.height() - statistics.get(statModel.getColumnName()).get(Statistic.COUNT).getNumeric() <= 0)
				&& model.setColumnStatisticsModelVisible(statModel, false)) {
			visibleCount--;
		}
		return visibleCount;
	}

	/**
	 * Update the visibility of the statistic models.
	 */
	private int updateVisibility(int visibleCount) {
		// reset filters on empty filter string
		if ("".equals(model.getFilterNameString())) {
			for (AbstractBeltColumnStatisticsModel statModel : backupInitialOrderList) {
				model.setColumnStatisticsModelVisible(statModel, true);
			}
		} else {
			// apply filter on non empty string
			for (AbstractBeltColumnStatisticsModel statModel : model.getOrderedColumnStatisticsModels()) {
				String attName = statModel.getColumnName();
				boolean show = attName.toLowerCase(Locale.ENGLISH).contains(model.getFilterNameString().toLowerCase(Locale.ENGLISH));
				if (!show) {
					visibleCount--;
				}
				model.setColumnStatisticsModelVisible(statModel, show);
			}
		}
		return visibleCount;
	}


	/**
	 * Hide the column if it is a type that should be hidden.
	 */
	private boolean hideColumnType(Table table, AbstractBeltColumnStatisticsModel statModel) {
		boolean showColumnType = true;
		for (Column.Category category : Column.Category.values()) {
			Column column = table.column(statModel.getColumnName());
			if (column.type().category() == category) {
				showColumnType = model.getColumnTypeVisibility(category);
				if (!showColumnType) {
					break;
				}
			}
		}
		return showColumnType;
	}

	/**
	 * Applies the current sorting.
	 */
	private void applySorting() {
		sortingQueue.execute(() -> {
			if (model.getSortingDirection(SortingType.NAME) != SortingDirection.UNDEFINED) {
				sortByName(model.getOrderedColumnStatisticsModels(), model.getSortingDirection(SortingType.NAME));
			}
			if (model.getSortingDirection(SortingType.TYPE) != SortingDirection.UNDEFINED) {
				sortByType(model.getOrderedColumnStatisticsModels(), model.getSortingDirection(SortingType.TYPE));
			}
			if (model.getSortingDirection(SortingType.MISSING) != SortingDirection.UNDEFINED) {
				sortByMissing(model.getOrderedColumnStatisticsModels(),
						model.getSortingDirection(SortingType.MISSING));
			}

			model.fireOrderChangedEvent();
		});
	}

	/**
	 * Sorts the {@link AbstractBeltColumnStatisticsModel}s via their column names.
	 */
	private void sortByName(List<AbstractBeltColumnStatisticsModel> list, final SortingDirection direction) {
		sort(list, (o1, o2) -> {
			int sortResult = o1.getColumnName().compareTo(o2.getColumnName());
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
	 * Sorts the {@link AbstractBeltColumnStatisticsModel}s via their column types.
	 */
	private void sortByType(List<AbstractBeltColumnStatisticsModel> list, final SortingDirection direction) {
		sort(list, (o1, o2) -> {
			IOTable tableOrNull = model.getTableOrNull();
			if (tableOrNull == null) {
				return 0;
			}
			Table table = tableOrNull.getTable();
			Column.TypeId type1 = table.column(o1.getColumnName()).type().id();
			Column.TypeId type2 = table.column(o2.getColumnName()).type().id();
			int sortResult = type1.compareTo(type2);
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
	 * Sorts the {@link AbstractBeltColumnStatisticsModel}s via their column types.
	 */
	private void sortByMissing(List<AbstractBeltColumnStatisticsModel> list, final SortingDirection direction) {
		sort(list, (o1, o2) -> {
			IOTable tableOrNull = model.getTableOrNull();
			if (tableOrNull == null) {
				return 0;
			}
			Table table = tableOrNull.getTable();
			Map<String, Map<Statistic, Result>> statistics = null;
			Double missing1 = null;
			Double missing2 = null;
			try {
				statistics = statisticsFuture.get();
				int height = table.height();
				missing1 = height - statistics.get(o1.getColumnName()).get(Statistic.COUNT).getNumeric();
				missing2 = height - statistics.get(o2.getColumnName()).get(Statistic.COUNT).getNumeric();
			} catch (InterruptedException e) {
				LogService.getRoot().log(Level.INFO, INTERRUPTED_MESSAGE);
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				LogService.getRoot().log(Level.INFO, CALC_ERROR_MESSAGE, e);
			}


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
	 * Resets the current sorting to none and restores the initial order of the {@link
	 * AbstractBeltColumnStatisticsModel}s.
	 */
	private void resetSorting() {
		for (SortingType type : SortingType.values()) {
			model.setSortingDirection(type, SortingDirection.UNDEFINED);
		}

		restoreInitialStatModelOrder();
	}

	/**
	 * Restores the initial order of the column stat models (special at the top, regular below). This is done
	 * because we no longer may have access to column roles after construction time.
	 */
	private void restoreInitialStatModelOrder() {
		model.setOrderedModelList(backupInitialOrderList);
	}

	/**
	 * Update the statistics part of all {@link BeltColumnStatisticsPanel}s.
	 */
	private void updateStatistics() {
		IOTable tableOrNull = model.getTableOrNull();
		if (tableOrNull == null) {
			throw new IllegalArgumentException("model table must not be null at construction time!");
		}

		Map<String, Map<Statistic, Result>> statistics = null;
		try {
			statistics = statisticsFuture.get();
		} catch (InterruptedException e) {
			LogService.getRoot().log(Level.INFO, INTERRUPTED_MESSAGE);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			LogService.getRoot().log(Level.INFO, CALC_ERROR_MESSAGE, e);
		}
		// update stats on all column stat models
		for (AbstractBeltColumnStatisticsModel statModel : model.getOrderedColumnStatisticsModels()) {
			statModel.updateStatistics(statistics);
		}

		// allow sorting and filtering
		model.setAllowSortingAndFiltering();

		// signal that everything is done
		model.fireInitDoneEvent();
	}

	/**
	 * Calculates the statistics of the given {@link Table} in a {@link ProgressThread}. Once the statistics are
	 * calculated, will update the stats on all {@link BeltColumnStatisticsPanel}s.
	 *
	 * @param table
	 * 		the table of which to recalculate the statistics
	 */
	private void calculateStatistics(final Table table) {
		DisplayContext context = new DisplayContext();


		// wrap into a future task so that cancelling with an interrupt is possible
		statisticsFuture = new FutureTask<>(() -> {
			Map<String, Map<Statistic, Result>> allStatistics1 = calculateAllStatistics(table, context);
			barrier.countDown();
			return allStatistics1;
		});

		//execute with indeterminate progress thread
		worker = new ProgressThread("statistics_calculation") {

			@Override
			public void run() {
				statisticsFuture.run();
			}

			@Override
			protected void executionCancelled() {
				context.stop();
				aborted = true;
				barrier.countDown();
			}
		};
		worker.setIndeterminate(true);
		worker.start();
	}

	private Map<String, Map<Statistics.Statistic,
			Statistics.Result>> calculateAllStatistics(Table table, Context context) {
		Map<String,
				Map<Statistics.Statistic, Statistics.Result>> allStatistics = new HashMap<>();
		for (String name : table.labels()) {
			Column column = table.column(name);
			Set<Statistics.Statistic> statisticsToCalc = new HashSet<>();
			for (Statistics.Statistic statistic : MAXIMAL_STATISTICS) {
				if (Statistics.supported(column, statistic)) {
					statisticsToCalc.add(statistic);
				}
			}
			// fill
			if (!context.isActive()) {
				return allStatistics;
			}
			Map<Statistics.Statistic, Statistics.Result> resultMap = Statistics.compute(column, statisticsToCalc, context);
			allStatistics.put(name, resultMap);
		}
		return allStatistics;
	}

	/**
	 * Sorts the given {@link List} of {@link AbstractBeltColumnStatisticsModel}s with the given {@link Comparator}.
	 */
	private static void sort(List<AbstractBeltColumnStatisticsModel> listOfStatModels,
							 Comparator<AbstractBeltColumnStatisticsModel> comp) {
		listOfStatModels.sort(comp);
	}

}
