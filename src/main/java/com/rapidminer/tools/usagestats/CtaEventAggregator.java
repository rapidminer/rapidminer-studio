/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector.Key;


/**
 * Aggregates UsageStats events for the cta system
 *
 * The event counts are aggregated, to reduce the load on the database.
 *
 * Every time {@link pullEvents()} is called, the aggregation starts again.
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5.0
 *
 */
enum CtaEventAggregator {
	INSTANCE;

	/** The current event map */
	private volatile Map<Key, Long> eventMap = new ConcurrentHashMap<>();
	/** Locks for synchronization */
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ReadLock readLock = lock.readLock();
	private WriteLock writeLock = lock.writeLock();
	private static final boolean NOT_IN_UI_MODE = !RapidMiner.getExecutionMode().equals(ExecutionMode.UI);

	/**
	 * Log the event
	 *
	 * @param type
	 * @param value
	 * @param arg
	 * @param count
	 */
	public void log(String type, String value, String arg, long count) {
		log(new Key(type, value, arg), count);
	}

	/**
	 * Log the event
	 *
	 * @param event
	 * @param count
	 */
	public void log(Key event, long count) {
		// Disable logging on server
		if (NOT_IN_UI_MODE) {
			return;
		}
		readLock.lock();
		try {
			eventMap.merge(event, count, Long::sum);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Remove and return all events
	 *
	 * @return
	 */
	public Map<Key, Long> pullEvents() {
		Map<Key, Long> result = eventMap;
		writeLock.lock();
		try {
			eventMap = new ConcurrentHashMap<>();
		} finally {
			writeLock.unlock();
		}
		return result;
	}
}
