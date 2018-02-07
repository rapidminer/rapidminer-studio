/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.gui.tools.BrowserPopup;
import com.rapidminer.gui.tools.bubble.WindowChoreographer;
import com.rapidminer.studio.internal.RuleProviderRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector.Key;


/**
 * Collects statistics and displays messages to the user if certain conditions are fulfilled
 *
 * @author Jonas Wilms-Pfau, Marco Boeck
 * @since 7.5.0
 */
public enum CallToActionScheduler {

	/** the singleton instance */
	INSTANCE;

	/** Pull every 5 seconds */
	private static final int DELAY = 5;

	/** Start immediately */
	private static final int INITIAL_DELAY = 0;

	/** Clean on start-up */
	private static final int CLEAN_DELAY = 0;

	/** Clean every hour */
	private static final int CLEAN_INTERVAL = 1;

	private static WindowChoreographer choreographer = new WindowChoreographer();

	private ScheduledExecutorService exec;
	private ScheduledFuture<?> persistFuture;
	private ScheduledFuture<?> cleanupFuture;

	/**
	 * Trigger class initialization. If not in {@link ExecutionMode#UI}, does nothing.
	 */
	public void init() {
		// Only initialize in UI mode
		if (!RapidMiner.getExecutionMode().equals(ExecutionMode.UI)) {
			return;
		}
		// Register rule provider
		RuleProviderRegistry.INSTANCE.register(new LocalRuleProvider(), RuleProviderRegistry.PRECEDENCE_LOCAL);
		RuleProviderRegistry.INSTANCE.register(new RemoteRuleProvider(), RuleProviderRegistry.PRECEDENCE_REMOTE);

		LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.usagestats.CallToActionScheduler.init.start");
		exec = Executors.newSingleThreadScheduledExecutor();
		// If any execution of the task encounters an exception, subsequent executions are
		// suppressed
		persistFuture = exec.scheduleAtFixedRate(() -> {
			boolean isDebug = (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE)));
			if (isDebug) {
				LogService.getRoot().log(Level.FINEST,
						"com.rapidminer.tools.usagestats.CallToActionScheduler.scheduler.started");
			}
			persistEvents();
			checkRules();
			if (isDebug) {
				LogService.getRoot().log(Level.FINEST,
						"com.rapidminer.tools.usagestats.CallToActionScheduler.scheduler.finished");
			}
		}, INITIAL_DELAY, DELAY, TimeUnit.SECONDS);

		cleanupFuture = exec.scheduleAtFixedRate(() -> {
			try {
				boolean isDebug = (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE)));
				if (isDebug) {
					LogService.getRoot().log(Level.FINEST,
							"com.rapidminer.tools.usagestats.CallToActionScheduler.cleanup.started");
				}
				CtaDao.INSTANCE.cleanUpDatabase();
				if (isDebug) {
					LogService.getRoot().log(Level.FINEST,
							"com.rapidminer.tools.usagestats.CallToActionScheduler.cleanup.finished");
				}
			} catch (SQLException e) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.usagestats.CallToActionScheduler.db.clean.failed", e);
			}
		}, CLEAN_DELAY, CLEAN_INTERVAL, TimeUnit.HOURS);
	}

	/**
	 * Tries to terminate the CTA scheduler. Will wait for a couple seconds for the currently
	 * running task to complete before aborting.
	 */
	public void shutdown() {
		boolean terminatedGracefully = false;
		try {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.usagestats.CallToActionScheduler.shutdown.start");
			persistFuture.cancel(false);
			cleanupFuture.cancel(false);
			exec.submit(this::persistEvents);
			exec.shutdown();
			terminatedGracefully = exec.awaitTermination(DELAY, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			if (terminatedGracefully) {
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.tools.usagestats.CallToActionScheduler.shutdown.finish_success");
			} else {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.usagestats.CallToActionScheduler.shutdown.finish_failure");
			}
		}
	}

	/**
	 * Pull & persist the current collected events
	 */
	private void persistEvents() {
		Map<Key, Long> events = CtaEventAggregator.INSTANCE.pullEvents();
		// There is no white list yet
		try {
			CtaDao.INSTANCE.storeEvents(events);
		} catch (SQLException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CallToActionScheduler.storing.failure",
					e);
			throw new IllegalStateException("The database died, shutting down CTA.", e);
		}
	}

	/**
	 * Fetch Rules and verifies them. If fulfilled, display the message and store the users
	 * reaction.
	 */
	private void checkRules() {
		Collection<VerifiableRule> rules = RuleService.INSTANCE.getRules();
		boolean isDebug = (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE)));
		for (VerifiableRule rule : rules) {
			// check if rule is fulfilled, if it is, create CTA popup and show it
			long before = System.currentTimeMillis();
			if (rule.isRuleFulfilled()) {
				if (isDebug) {
					LogService.getRoot().log(Level.FINE,
							"com.rapidminer.tools.usagestats.CallToActionScheduler.rule.verification.triggered",
							rule.getRule().getId());
				}

				// Display the CTA
				final BrowserPopup cta = new BrowserPopup(rule.getRule().getMessage());
				rule.setDisplaying(true);
				SwingUtilities.invokeLater(() -> choreographer.addWindow(cta));

				// wait for results in separate thread (user may not click on CTA for ages)
				new Thread(() -> {
					// Store the triggered state
					try {
						String result = cta.get();
						rule.setDisplaying(false);
						CtaDao.INSTANCE.triggered(rule.getRule(), result);
						ActionStatisticsCollector.INSTANCE.logCtaRuleTriggered(rule.getRule().getId(), result);
					} catch (SQLException e) {
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.usagestats.CallToActionScheduler.rule.triggered.storage.failed",
										rule.getRule().getId()),
								e);
					}
				}).start();

			}
			if (isDebug) {
				LogService.getRoot().log(Level.FINEST,
						"com.rapidminer.tools.usagestats.CallToActionScheduler.rule.verification.verify",
						new Object[] { rule.getRule().getId(), System.currentTimeMillis() - before });
			}

		}
	}
}
