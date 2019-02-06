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
package com.rapidminer.tools.usagestats;

import java.awt.datatransfer.DataFlavor;

import com.rapidminer.tools.usagestats.ActionStatisticsCollector.UsageObject;


/**
 * An interface to log usage statistics in arbitrary and hard to reach places. was first introduced to track dragging
 * from the {@link com.rapidminer.gui.search.GlobalSearchCategoryPanel GlobalSearchCategoryPanel}. Is now part of
 * {@link com.rapidminer.gui.dnd.TransferableOperator TransferableOperator} and {@link com.rapidminer.gui.dnd.TransferableRepositoryEntry TransferableRepositoryEntry},
 * as well as the {@link java.awt.dnd.DragGestureRecognizer DragGestureRecognizers} associated with operator and repository
 * {@link com.rapidminer.search.GlobalSearchCategory GlobalSearchCategories}.
 * <p>
 * Implementing classes should either directly inherit from {@link DefaultUsageLoggable} where possible or
 * use a delegate object of type {@link DefaultUsageLoggable}.
 *
 * @author Jan Czogalla
 * @since 8.1.2
 */
public interface UsageLoggable {

	/** Marker {@link DataFlavor} to trigger logging in {@link java.awt.datatransfer.Transferable Transferables} */
	DataFlavor USAGE_FLAVOR = new DataFlavor(UsageObject.class, "Flag for loggable usage statistics");

	/** Log the usage by using the stored usage logger or {@link UsageObject} if present */
	void logUsageStats();

	/** Set the usage logger for this {@link UsageLoggable} */
	void setUsageStatsLogger(Runnable usageLogger);

	/** Sets the {@link UsageObject} for this {@link UsageLoggable} */
	void setUsageObject(UsageObject usageObject);
}
