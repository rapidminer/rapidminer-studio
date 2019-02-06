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

import com.rapidminer.tools.usagestats.ActionStatisticsCollector.UsageObject;

/**
 * Default implementation of {@link UsageLoggable} that simply stores the {@link UsageObject} and can log it if necessary.
 * Prefers a {@link Runnable} logger over a {@link UsageObject} when executing {@link #logUsageStats()};
 * will do nothing if both are not set.
 *
 * @since 8.1.2
 * @author Jan Czogalla
 */
public class DefaultUsageLoggable implements UsageLoggable {

	protected Runnable usageLogger;
	protected UsageObject usageObject;

	@Override
	public void logUsageStats() {
		if (usageLogger != null) {
			usageLogger.run();
			return;
		}
		if (usageObject != null) {
			usageObject.logUsage();
		}
	}

	@Override
	public void setUsageStatsLogger(Runnable usageLogger) {
		this.usageLogger = usageLogger;
	}

	@Override
	public void setUsageObject(UsageObject usageObject) {
		this.usageObject = usageObject;
	}
}
