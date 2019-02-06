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
package com.rapidminer.studio.concurrency.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import com.rapidminer.studio.internal.ParameterServiceRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * Supplies a ForkJoinPool if needed Only usable by {@link AbstractConcurrencyContext}
 *
 * @author Jonas Wilms-Pfau (internals by Gisa Schaefer, Michael Knopf)
 * @see AbstractConcurrencyContext
 * @since 9.1.0
 */
class LazyPool {

	/**
	 * The current ForkJoinPool implementation restricts the maximum number of running threads to 32767. Attempts to
	 * create pools with greater than the maximum number result in IllegalArgumentException.
	 */
	private static final int FJPOOL_MAXIMAL_PARALLELISM = 32767;

	/**
	 * Locks to handle access from different threads
	 */
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	/**
	 * The fork join pool all task are submitted to.
	 */
	private ForkJoinPool pool = null;

	private final String key;

	/**
	 * Creates a new pool from the given settings key
	 *
	 * @param settingsKey
	 * 		used for logging and the default desired parallelism level
	 */
	LazyPool(String settingsKey) {
		Objects.requireNonNull(settingsKey);
		this.key = settingsKey;
	}

	/**
	 * Returns a new instance that for the given context
	 *
	 * @param context
	 * 		the ConcurrencyContext
	 * @return a context specific instance
	 */
	LazyPool.Instance getInstance(AbstractConcurrencyContext context) {
		return new LazyPool.Instance(context);
	}

	/**
	 * Instance of this pool for a specific Context
	 */
	final class Instance {

		private final AbstractConcurrencyContext context;

		/**
		 * Creates a new Instance for this context
		 *
		 * @param context
		 * 		the context
		 */
		private Instance(AbstractConcurrencyContext context) {
			Objects.requireNonNull(context);
			this.context = context;
		}

		/**
		 * This method verifies and returns a JVM-wide, static FJPool. Override if a different pool behavior is needed.
		 *
		 * @return the ForkJoinPool to use for this context instance for execution.
		 */
		ForkJoinPool getForkJoinPool() {
			readLock.lock();
			try {
				if (!context.isPoolOutdated()) {
					// nothing to do
					return pool;
				}
			} finally {
				readLock.unlock();
			}
			writeLock.lock();
			try {
				if (!context.isPoolOutdated()) {
					// pool has been updated in the meantime
					// no reason to re-create the pool once again
					return pool;
				}
				if (pool != null) {
					pool.shutdown();
				}
				int desiredParallelismLevel = context.getDesiredParallelismLevel();
				pool = new ForkJoinPool(desiredParallelismLevel);
				LogService.getRoot().log(Level.CONFIG,
						"com.rapidminer.concurrency.concurrency_context.pool_creation",
						new Object[]{desiredParallelismLevel, I18N.getSettingsMessage(key, I18N.SettingsType.TITLE)});
				return pool;
			} finally {
				writeLock.unlock();
			}
		}

		/**
		 * Checks if the JVM-wide, static pool needs to be re-created. Override if a different pool behavior is needed.
		 *
		 * @return {@code true} if the current pool is {@code null} or the {@link AbstractConcurrencyContext#getDesiredParallelismLevel}
		 * is not equal to the current {@link #pool} parallelism otherwise {@code false}
		 */
		boolean isPoolOutdated() {
			return pool == null || context.getDesiredParallelismLevel() != pool.getParallelism();
		}

		/**
		 * Returns the targeted parallelism level of this pool.
		 * <p>
		 * The targeted parallelism level provides an <em>upper bound</em> for the number of tasks that will be executed
		 * in parallel by this context. It does not guarantee that this bound will be matched during execution. Note
		 * that the targeted parallelism level need not match the number of processors available to the Java {@link
		 * Runtime}.
		 * <p>
		 * You can use the targeted parallelism level as an indicator for partitioning your computation. For instance, a
		 * parallelism level of {@code 8} indicates that should partition your task into
		 * <em>at least</em> eight tasks to fully utilize the context. A parallelism level of {@code 1}
		 * indicates a single threaded execution of all submitted tasks.
		 *
		 * @return the targeted parallelism level
		 */
		int getParallelism() {
			if (pool != null) {
				return AccessController.doPrivileged((PrivilegedAction<ForkJoinPool>) context::getForkJoinPool)
						.getParallelism();
			} else {
				return context.getDesiredParallelismLevel();
			}
		}

		/**
		 * Helper method to get the desired parallelism level for the given setting
		 *
		 * @return the desired parallelism level
		 */
		int getDesiredParallelismLevel() {
			String numberOfThreads = ParameterServiceRegistry.INSTANCE.getParameterValue(key);

			int userLevel = 0;

			if (numberOfThreads != null) {
				try {
					userLevel = Integer.parseInt(numberOfThreads);
					LogService.getRoot().log(Level.FINE, "com.rapidminer.concurrency.concurrency_context.parse_success",
							new Object[]{userLevel, I18N.getSettingsMessage(key, I18N.SettingsType.TITLE)});
				} catch (NumberFormatException e) {
					// ignore and use default value
					LogService.getRoot().log(Level.FINE, "com.rapidminer.concurrency.concurrency_context.parse_failure",
							new Object[]{numberOfThreads, I18N.getSettingsMessage(key, I18N.SettingsType.TITLE)});
				}
			}

			if (userLevel <= 0) {
				userLevel = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
			}

			// should not happen, but we want to avoid any exception during pool creation
			if (userLevel > FJPOOL_MAXIMAL_PARALLELISM) {
				userLevel = FJPOOL_MAXIMAL_PARALLELISM;
			}
			return userLevel;
		}
	}
}
