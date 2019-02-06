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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
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
	 * Represents the keys that are not to be written to the CTA database.
	 * The log items should be filtered by checking for all blacklist items,
	 * that determine which log parts are to be matched.
	 */
	private static class BlackListItem {

		/**
		 * The key to be matched.
		 */
		private final Key key;
		/**
		 * Match for the type of the key is obligatory.
		 */
		private final boolean useType;
		/**
		 * Match for the value of the key is obligatory.
		 */
		private final boolean useValue;
		/**
		 * Match for the arg of the key is obligatory.
		 */
		private final boolean useArg;

		BlackListItem(Key key, boolean useType, boolean useValue, boolean useArg) {
			this.key = key;
			this.useType = useType;
			this.useValue = useValue;
			this.useArg = useArg;
		}

		boolean matchesKey(Key key) {
			if (key == null) {
				return false;
			}
			if (useType && (
					this.key.getType() == null && key.getType() != null
							|| this.key.getType() != null && !this.key.getType().equals(key.getType())
			)) {
				return false;
			}
			if (useValue && (
					this.key.getValue() == null && key.getValue() != null
							|| this.key.getValue() != null && !this.key.getValue().equals(key.getValue())
			)) {
				return false;
			}
			if (useArg && (
					this.key.getArgWithIndicators() == null && key.getArgWithIndicators() != null
							|| this.key.getArgWithIndicators() != null && !this.key.getArgWithIndicators().equals(key.getArgWithIndicators())
			)) {
				return false;
			}
			return true;
		}

	}

	private static final BlackListItem[] BLACKLIST = new BlackListItem[4 + ProcessOriginProcessXMLFilter.ProcessOriginState.values().length];

	static {
		// the argument of logged exceptions are too long and irrelevant
		int i = 0;
		BLACKLIST[i++] = new BlackListItem(new Key(ActionStatisticsCollector.TYPE_PROCESS, ActionStatisticsCollector.VALUE_EXCEPTION,
		null), true, true, false);
		for (ProcessOriginProcessXMLFilter.ProcessOriginState state : ProcessOriginProcessXMLFilter.ProcessOriginState.values()) {
			BLACKLIST[i++] = new BlackListItem(new Key(state.getPrefix() + ActionStatisticsCollector.TYPE_PROCESS, ActionStatisticsCollector.VALUE_EXCEPTION,
					null), true, true, false);
		}

		// progress-thread typed logs are irrelevant
		BLACKLIST[i++] = new BlackListItem(new Key(ActionStatisticsCollector.TYPE_PROGRESS_THREAD, null,
		null), true, false, false);

		// resource-action typed logs are irrelevant, use "action" type in CTA rules instead
				BLACKLIST[i++] = new BlackListItem(new Key(ActionStatisticsCollector.TYPE_RESOURCE_ACTION, null,
		null), true, false, false);

		// simple-action typed logs are irrelevant, use "action" type in CTA rules instead
				BLACKLIST[i++] = new BlackListItem(new Key(ActionStatisticsCollector.TYPE_SIMPLE_ACTION, null,
		null), true, false, false);
	}

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
		if (NOT_IN_UI_MODE || !validateKey(event)) {
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
	
	/**
	 * Returns true if no item of the blacklist matched the key.
	 * 
	 * @param key
	 * @return
	 */
	private boolean validateKey(Key key) {
		for (BlackListItem item : BLACKLIST) {
			if (item.matchesKey(key)) {
				return false;
			}
		}
		return true;
	}
}
