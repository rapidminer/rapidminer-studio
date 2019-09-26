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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.RapidMinerVersion;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.bubble.WindowChoreographer;
import com.rapidminer.license.License;
import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.settings.Telemetry;
import com.rapidminer.studio.internal.RuleProviderRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.abtesting.AbGroupProvider;
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

	private static final String USER_COUNTRY = "user_country";
	private static final String USER_LANGUAGE = "user_language";
	private static final String USER_TIMEZONE = "user_timezone";

	private static final String SETTINGS_LANGUAGE = "settings_language";

	private static final String STUDIO_VERSION_FULL = "studio_version_full";
	private static final String STUDIO_VERSION_MAJOR = "studio_version_major";
	private static final String STUDIO_VERSION_MINOR = "studio_version_minor";
	private static final String STUDIO_VERSION_PATCH = "studio_version_patch";
	private static final String STUDIO_PLATFORM = "studio_platform";
	private static final String STUDIO_EXPERT_MODE = "studio_expert_mode";
	private static final String STUDIO_AB_TEST_GROUP = "studio_ab_test_group";

	private static final String LICENSE_EDITION = "license_edition";
	private static final String LICENSE_PRECEDENCE = "license_precedence";
	private static final String LICENSE_EMAIL = "license_email";
	private static final String LICENSE_START = "license_start";
	private static final String LICENSE_EXPIRATION = "license_expiration";
	private static final String LICENSE_ANNOTATION = "license_annotation";
	private static final String LICENSE_UPCOMING_EDITION = "license_upcoming_edition";
	private static final String LICENSE_UPCOMING_PRECEDENCE = "license_upcoming_precedence";
	private static final String LICENSE_UPCOMING_EMAIL = "license_upcoming_email";
	private static final String LICENSE_UPCOMING_START = "license_upcoming_start";
	private static final String LICENSE_UPCOMING_EXPIRATION = "license_upcoming_expiration";
	private static final String LICENSE_UPCOMING_ANNOTATION = "license_upcoming_annotation";

	private static WindowChoreographer choreographer = new WindowChoreographer();


	private ScheduledExecutorService exec;
	private ScheduledFuture<?> persistFuture;
	private ScheduledFuture<?> cleanupFuture;


	/**
	 * Trigger class initialization. If not in {@link ExecutionMode#UI}, does nothing.
	 */
	public void init() {
		// Only initialize in UI mode with telemetry available
		if (!RapidMiner.getExecutionMode().equals(ExecutionMode.UI) || Telemetry.CTA.isDenied()) {
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

		// fill in studio constants on first start
		fillStudioConstants();

		// on license change, we need to update the studio constants table
		LicenseManagerRegistry.INSTANCE.get().registerLicenseManagerListener(new LicenseManagerListener() {
			@Override
			public <S, C> void handleLicenseEvent(LicenseEvent<S, C> event) {
				fillStudioConstants();
			}
		});
	}

	/**
	 * Tries to terminate the CTA scheduler. Will wait for a couple seconds for the currently
	 * running task to complete before aborting.
	 */
	public void shutdown() {
		boolean terminatedGracefully = false;
		try {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.usagestats.CallToActionScheduler.shutdown.start");
			if (persistFuture != null) {
				persistFuture.cancel(false);
			}
			if (cleanupFuture != null) {
				cleanupFuture.cancel(false);
			}
			if (exec != null) {
				exec.submit(this::persistEvents);
				exec.shutdown();
				terminatedGracefully = exec.awaitTermination(DELAY, TimeUnit.SECONDS);
			} else {
				terminatedGracefully = true;
			}
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
	 * Updates the studio_constants table with the latest values.
	 *
	 * @since 8.1.1
	 */
	private void fillStudioConstants() {
		try {
			CtaDao.INSTANCE.mergeConstant(STUDIO_VERSION_FULL, new RapidMinerVersion().getLongVersion());
			CtaDao.INSTANCE.mergeConstant(STUDIO_VERSION_MAJOR, String.valueOf(new RapidMinerVersion().getMajorNumber()));
			CtaDao.INSTANCE.mergeConstant(STUDIO_VERSION_MINOR, String.valueOf(new RapidMinerVersion().getMinorNumber()));
			CtaDao.INSTANCE.mergeConstant(STUDIO_VERSION_PATCH, String.valueOf(new RapidMinerVersion().getPatchLevel()));
			CtaDao.INSTANCE.mergeConstant(STUDIO_PLATFORM, PlatformUtilities.getReleasePlatform() == null ? null : PlatformUtilities.getReleasePlatform().name());
			CtaDao.INSTANCE.mergeConstant(STUDIO_EXPERT_MODE, String.valueOf(RapidMinerGUI.getMainFrame().getPropertyPanel().isExpertMode()));
			CtaDao.INSTANCE.mergeConstant(STUDIO_AB_TEST_GROUP, String.valueOf(AbGroupProvider.get().getGroup(10)));

			License activeLicense = ProductConstraintManager.INSTANCE.getActiveLicense();
			CtaDao.INSTANCE.mergeConstant(LICENSE_EDITION, activeLicense.getProductEdition());
			CtaDao.INSTANCE.mergeConstant(LICENSE_PRECEDENCE, String.valueOf(activeLicense.getPrecedence()));
			CtaDao.INSTANCE.mergeConstant(LICENSE_EMAIL, activeLicense.getLicenseUser().getEmail());
			CtaDao.INSTANCE.mergeConstant(LICENSE_START, activeLicense.getStartDate() == null ? null : activeLicense.getStartDate().toString()); // yyyy-MM-dd
			CtaDao.INSTANCE.mergeConstant(LICENSE_EXPIRATION, activeLicense.getExpirationDate() == null ? null : activeLicense.getExpirationDate().toString()); // yyyy-MM-dd
			CtaDao.INSTANCE.mergeConstant(LICENSE_ANNOTATION, activeLicense.getAnnotations());

			License upcomingLicense = ProductConstraintManager.INSTANCE.getUpcomingLicense();
			CtaDao.INSTANCE.mergeConstant(LICENSE_UPCOMING_EDITION, upcomingLicense.getProductEdition());
			CtaDao.INSTANCE.mergeConstant(LICENSE_UPCOMING_PRECEDENCE, String.valueOf(upcomingLicense.getPrecedence()));
			CtaDao.INSTANCE.mergeConstant(LICENSE_UPCOMING_EMAIL, upcomingLicense.getLicenseUser().getEmail());
			CtaDao.INSTANCE.mergeConstant(LICENSE_UPCOMING_START, upcomingLicense.getStartDate() == null ? null : upcomingLicense.getStartDate().toString()); // yyyy-MM-dd
			CtaDao.INSTANCE.mergeConstant(LICENSE_UPCOMING_EXPIRATION, upcomingLicense.getExpirationDate() == null ? null : upcomingLicense.getExpirationDate().toString()); // yyyy-MM-dd
			CtaDao.INSTANCE.mergeConstant(LICENSE_UPCOMING_ANNOTATION, upcomingLicense.getAnnotations());

			CtaDao.INSTANCE.mergeConstant(USER_COUNTRY, I18N.getOriginalLocale().getCountry());
			CtaDao.INSTANCE.mergeConstant(USER_LANGUAGE, I18N.getOriginalLocale().getLanguage());
			CtaDao.INSTANCE.mergeConstant(USER_TIMEZONE, TimeZone.getDefault().getID());

			CtaDao.INSTANCE.mergeConstant(SETTINGS_LANGUAGE, ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE));
		} catch (SQLException | RuntimeException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CallToActionScheduler.constant.failure", e);
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
				CTAVisualizationRegistry.CTA cta = CTAVisualizationRegistry.getVisualization(rule.getRule().getMessage());
				if (cta == null) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.usagestats.CallToActionScheduler.rule.visualization.unavailable", rule.getRule().getId());
					return;
				}
				rule.setDisplaying(true);
				SwingUtilities.invokeLater(() -> choreographer.addWindow(cta.getWindow()));

				// wait for results in separate thread (user may not click on CTA for ages)
				new Thread(() -> {
					// Store the triggered state
					try {
						String result = cta.getResult();
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
						new Object[]{rule.getRule().getId(), System.currentTimeMillis() - before});
			}

		}
	}
}
