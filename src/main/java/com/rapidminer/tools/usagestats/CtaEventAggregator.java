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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.settings.Telemetry;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector.Key;


/**
 * Aggregates UsageStats events for the cta system
 *
 * The event counts are aggregated, to reduce the load on the database.
 *
 * Every time {@link #pullEvents()} is called, the aggregation starts again.
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5.0
 *
 */
public enum CtaEventAggregator {
	INSTANCE;

	/**
	 * The current event map
	 */
	private volatile Map<Key, Long> eventMap = new ConcurrentHashMap<>();
	/** this is set to true during init of the CTA extension */
	private AtomicBoolean ctaSystemLive = new AtomicBoolean(false);
	/** this is set to true if the aggregator is killed due to the CTA system not being live after extensions have been loaded */
	private AtomicBoolean killed = new AtomicBoolean(false);
	/** Locks for synchronization */
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ReadLock readLock = lock.readLock();
	private WriteLock writeLock = lock.writeLock();
	private static final boolean UI_MODE = RapidMiner.getExecutionMode().equals(ExecutionMode.UI);

	/**
	 * Checks if logging is allowed
	 *
	 * @return {@code true} if logging is not allowed
	 */
	private static boolean loggingAllowed() {
		return UI_MODE && !Telemetry.USAGESTATS.isDenied();
	}

	/**
	 * Represents the keys that are not to be written to the CTA database. The log items should be filtered by checking
	 * for all blacklist items, that determine which log parts are to be matched.
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
			if (useType && !Objects.equals(this.key.getType(), key.getType())) {
				return false;
			}
			if (useValue && !Objects.equals(this.key.getValue(), key.getValue())) {
				return false;
			}
			if (useArg && !Objects.equals(this.key.getArgWithIndicators(), key.getArgWithIndicators())) {
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
	 * @param type the type of the event
	 * @param value the value of the event
	 * @param arg the argument of the event
	 * @param count the number of time the event happened
	 */
	public void log(String type, String value, String arg, long count) {
		log(new Key(type, value, arg), count);
	}

	/**
	 * Log the event
	 *
	 * @param event
	 * 		the event
	 * @param count
	 * 		the number of times the event occured
	 */
	public void log(Key event, long count) {
		if (loggingAllowed() && !isBlacklisted(event) && !killed.get()) {
			logNow(event, count);
		}
	}

	/**
	 * Allows to log a short version of a blacklisted event
	 *
	 * @param event
	 * 		the event
	 * @param count
	 * 		the number of times the event occurred
	 * @since 9.5.0
	 */
	void logBlacklistedKey(Key event, long count) {
		if (loggingAllowed() && isBlacklisted(event)) {
			logNow(event, count);
		}
	}

	/**
	 * Log the event
	 * <p>Warning: this method does not check anything, call {@link #loggingAllowed()} before calling this method.</p>
	 *
	 * @param event
	 * 		the event
	 * @param count
	 * 		the number of times the event occurred
	 */
	private void logNow(Key event, long count) {
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
	 * @return events with count since the last invocation of this method
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
	 * Sets the CTA system to be available (the CTA extension calls this during loading).
	 *
	 * @since 9.5.0
	 */
	public void setCtaSystemLive() {
		Tools.requireInternalPermission();

		ctaSystemLive.set(true);
	}

	/**
	 * Whether the CTA system is available or not.
	 *
	 * @return {@code true} if the CTA system is available (i.e. the extension is loaded); {@code false} otherwise
	 * @since 9.5.0
	 */
	public boolean isCtaSystemLive() {
		return ctaSystemLive.get();
	}

	/**
	 * Internal API, do not call. Disable event aggregation to avoid building a giant map when the CTA extension is not
	 * loaded. After this is called, no further calls are expected by this class during the lifetime of that Studio
	 * instance.
	 *
	 * @since 9.5.0
	 */
	public void killAggregator() {
		Tools.requireInternalPermission();

		LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CtaEventAggregator.killed");
		killed.set(true);
		// kill old map by ignoring pull results
		pullEvents();
	}
	/**
	 * Returns {@code false} if no item of the blacklist matched the key.
	 *
	 * @param key the event
	 * @return {@code true} if the event is blacklisted
	 */
	private static boolean isBlacklisted(Key key) {
		for (BlackListItem item : BLACKLIST) {
			if (item.matchesKey(key)) {
				return true;
			}
		}
		return false;
	}
}
