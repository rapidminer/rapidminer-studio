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

import com.rapidminer.gui.new_plotter.listener.AggregationWindowingListener;
import com.rapidminer.gui.new_plotter.utility.AggregatedValueRange;
import com.rapidminer.gui.new_plotter.utility.ValueRange;

import java.util.LinkedList;
import java.util.List;


/**
 * This class can be used to create cumulative plots.
 * 
 * Used as a parameter to the grouping creation functions it tells the ValueGrouping to include
 * values from a certain amount of groups from the left or from the right in a group. To include all
 * values left of the current group in the current group, set grabLeft to -1. grabLeft works
 * analogously.
 * 
 * If there are less than grabLeft groups on the left of the current group, all left groups are
 * taken.
 * 
 * @author Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class AggregationWindowing implements Cloneable {

	private int grabLeft = 0;
	private int grabRight = 0;
	private boolean includeIncompleteGroups = true;
	private List<AggregationWindowingListener> listeners = new LinkedList<AggregationWindowingListener>();

	public AggregationWindowing(int grabLeft, int grabRight, boolean includeIncompleteGroups) {
		super();
		this.grabLeft = grabLeft;
		this.grabRight = grabRight;
		this.includeIncompleteGroups = includeIncompleteGroups;
	}

	public int getGrabLeft() {
		return grabLeft;
	}

	public void setGrabLeft(int grabLeft) {
		if (grabLeft != this.grabLeft) {
			this.grabLeft = grabLeft;
			fireAggregationWindowingChanged();
		}
	}

	public int getGrabRight() {
		return grabRight;
	}

	public void setGrabRight(int grabRight) {
		if (grabRight != this.grabRight) {
			this.grabRight = grabRight;
			fireAggregationWindowingChanged();
		}
	}

	/**
	 * @return the includeIncompleteGroups
	 */
	public boolean isIncludingIncompleteGroups() {
		return includeIncompleteGroups;
	}

	/**
	 * @param includeIncompleteGroups
	 *            the includeIncompleteGroups to set
	 */
	public void setIncludeIncompleteGroups(boolean includeIncompleteGroups) {
		if (includeIncompleteGroups != this.includeIncompleteGroups) {
			this.includeIncompleteGroups = includeIncompleteGroups;
			fireAggregationWindowingChanged();
		}
	}

	/**
	 * Creates a new grouping model (i.e. a list of {@link ValueRange}s) with this aggregation
	 * windowing applied. Does not change the input grouping.
	 * 
	 * When includeIncompleteGroups is false, the resulting list will contain null values for the
	 * the incomplete groups.
	 */
	public List<ValueRange> applyOnGrouping(List<ValueRange> grouping) {
		if (grabLeft == 0 && grabRight == 0) {
			return grouping;
		}

		LinkedList<ValueRange> leftGrabbedList = new LinkedList<ValueRange>();
		LinkedList<ValueRange> rightGrabbedList = new LinkedList<ValueRange>();

		List<ValueRange> cumulatedGrouping = new LinkedList<ValueRange>();

		ValueRange currentRange = null;
		for (ValueRange range : grouping) {
			rightGrabbedList.add(range);

			if (currentRange != null) {
				leftGrabbedList.add(currentRange);
				if (grabLeft > -1 && leftGrabbedList.size() > grabLeft) {
					leftGrabbedList.removeFirst();
				}
			}

			if (grabRight > -1 && rightGrabbedList.size() > grabRight) {
				currentRange = rightGrabbedList.removeFirst();
			}

			// check if we want to add the current range
			boolean addAggregatedRange = false;
			if (currentRange != null) {
				if (includeIncompleteGroups) {
					addAggregatedRange = true;
				} else if (grabLeft == -1) {
					addAggregatedRange = true;
				} else if (leftGrabbedList.size() >= grabLeft) {
					addAggregatedRange = true;
				}
			}

			// add current range
			if (addAggregatedRange) {
				AggregatedValueRange aggregatedRange = createAggregatedRange(leftGrabbedList, rightGrabbedList, currentRange);
				cumulatedGrouping.add(aggregatedRange);
			} else if (currentRange != null) {
				cumulatedGrouping.add(null);
			}
		}

		if (includeIncompleteGroups || grabRight == -1) {
			while (!rightGrabbedList.isEmpty()) {
				if (currentRange != null) {
					leftGrabbedList.add(currentRange);
				}
				currentRange = rightGrabbedList.removeFirst();
				if (grabLeft > -1 && leftGrabbedList.size() > grabLeft) {
					leftGrabbedList.removeFirst();
				}
				cumulatedGrouping.add(createAggregatedRange(leftGrabbedList, rightGrabbedList, currentRange));
			}
		} else {
			while (cumulatedGrouping.size() < grouping.size()) {
				cumulatedGrouping.add(null);
			}
		}
		return cumulatedGrouping;
	}

	private AggregatedValueRange createAggregatedRange(LinkedList<ValueRange> leftGrabbedList,
			LinkedList<ValueRange> rightGrabbedList, ValueRange currentRange) {
		AggregatedValueRange aggregatedRange = new AggregatedValueRange();
		aggregatedRange.addSubRange(currentRange);
		for (ValueRange subRange : leftGrabbedList) {
			aggregatedRange.addSubRange(subRange);
		}
		for (ValueRange subRange : rightGrabbedList) {
			aggregatedRange.addSubRange(subRange);
		}
		return aggregatedRange;
	}

	public void addAggregationWindowingListener(AggregationWindowingListener l) {
		listeners.add(l);
	}

	public void removeAggregationWindowingListener(AggregationWindowingListener l) {
		listeners.remove(l);
	}

	private void fireAggregationWindowingChanged() {
		for (AggregationWindowingListener l : listeners) {
			l.aggregationWindowingChanged(this);
		}
	}

	@Override
	public AggregationWindowing clone() {
		return new AggregationWindowing(grabLeft, grabRight, includeIncompleteGroups);
	}
}
