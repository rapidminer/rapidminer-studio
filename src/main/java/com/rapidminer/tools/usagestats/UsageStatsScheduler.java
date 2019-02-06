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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dialog.EULADialog;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.settings.Telemetry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;

/**
 * This scheduler is responsible to transmit the usage statistics in a timely manner
 *
 * @author Peter Toth
 * @since 8.0
 */
public final class UsageStatsScheduler  {

	/** Transmit usage statistics every day */
	private static final long DAILY_TRANSMISSION_INTERVAL = 1000L * 60 * 60 * 24;
	/** Transmit usage statistics every hour */
	private static final long HOURLY_TRANSMISSION_INTERVAL = 1000L * 60 * 60;
	/** Schedule extra transmission 10 minutes from now */
	private static final long SOON_TRANSMISSION_INTERVAL = 1000L * 60 * 10;

	/** A schedule attempt no sooner can schedule than this interval */
	private static final long MIN_FIRE_INTERVAL = 1000L * 60;

	private static final Object TIMER_LOCK = new Object();
	private static Timer timer;
	private static Date timerDate;

	/**
	 * Please use UsageStatsScheduler static methods only.
	 */
	private UsageStatsScheduler() {}

	/** Starts the first timer. */
	public static void init() {
		if (Telemetry.USAGESTATS.isDenied()) {
			return;
		}
		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {
			@Override
			public void informParameterChanged(String key, String value) {
				if (RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS.equals(key)) {
					setTimer(true);
				}
			}

			public void informParameterSaved() {
				// No need to do anything
			}
		});
		setTimer(false);
		ActionStatisticsCollector.getInstance().start();
	}

	/**
	 * Schedules the next transmission or disabled scheduling based on Studio parameters.
	 * Can compute the time of schedule if needed.
	 *
	 * @param reschedule
	 */
	private static void setTimer(boolean reschedule) {
		if (RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS_ANSWERS[UsageStatsTransmissionDialog.ALWAYS]
				.equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS))) {
			scheduleTransmission(UsageStatistics.Reason.ALWAYS, reschedule);
		} else if (RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS_ANSWERS[UsageStatsTransmissionDialog.ASK]
				.equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS))) {
			scheduleTransmission(UsageStatistics.Reason.ASK, reschedule);
		} else {
			stopTimer();
		}
	}

	private static void stopTimer() {
		synchronized (TIMER_LOCK) {
			if (timerDate != null) {
				timerDate = null;
				timer.cancel();
				timer = null;
			}
		}
	}

	/**
	 * Computes the next usage stat upload schedule date from current date and reason to schedule.
	 *
	 * @param reason
	 * @return
	 */
	private static Date getTransmissionDate(UsageStatistics.Reason reason) {
		switch (reason) {
			case ALWAYS:
				return new Date(System.currentTimeMillis() + HOURLY_TRANSMISSION_INTERVAL);
			case ASK:
				return new Date(System.currentTimeMillis() + DAILY_TRANSMISSION_INTERVAL);
			case CTA:
			case ROWLIMIT:
				return new Date(System.currentTimeMillis() + SOON_TRANSMISSION_INTERVAL);
			case SHUTDOWN:
			default:
				return new Date();
		}
	}

	private static synchronized void transmit(UsageStatistics.Reason reason, boolean wait) {
		if (Telemetry.USAGESTATS.isDenied()) {
			return;
		}
		ActionStatisticsCollector.ActionStatisticsSnapshot snapshot = ActionStatisticsCollector.getInstance().getActionStatisticsSnapshot(true);
		if (UsageStatsTransmissionDialog.askForTransmission(snapshot)) {
			stopTimer();
			ProgressThread pt = new ProgressThread("transmit_usagestats") {

				@Override
				public void run() {
					getProgressListener().setTotal(100);
					getProgressListener().setCompleted(10);
					try {
						UsageStatistics.getInstance().transferUsageStats(reason, snapshot, getProgressListener());
					} catch (Exception e) {
						ActionStatisticsCollector.getInstance().addActionStatisticsSnapshot(snapshot);
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.usagestats.UsageStatsTransmissionDialog.submitting_operator_usage_statistics_error"),
								e);
					} finally {
						UsageStatistics.getInstance().save();
					}
					setTimer(true);
					getProgressListener().setCompleted(100);
					getProgressListener().complete();
				}
			};

			if (wait) {
				pt.startAndWait();
			} else {
				pt.start();
			}
		} else {
			ActionStatisticsCollector.getInstance().addActionStatisticsSnapshot(snapshot);
			UsageStatistics.getInstance().save();
			stopTimer();
			setTimer(true);
		}
	}

	/**
	 * Schedules the next time for usage transmission. It has to be at least {@link #MIN_FIRE_INTERVAL}
	 * milliseconds from now.
	 * If sooner transmission has already scheduled then does nothing.
	 * Can compute the time of schedule if needed.
	 *
	 * @param reason
	 * @param reschedule
	 */
	static void scheduleTransmission(UsageStatistics.Reason reason, boolean reschedule) {
		if (RapidMiner.getExecutionMode().isHeadless()) {
			return;
		}

		Date nextTransmission = UsageStatistics.getInstance().getNextTransmission();
		Date timeToFire = (reschedule || nextTransmission == null) ? getTransmissionDate(reason) : nextTransmission;

		Date nextAvailableTimeToFire = new Date(System.currentTimeMillis() + MIN_FIRE_INTERVAL);
		if (timeToFire.getTime() < nextAvailableTimeToFire.getTime()) {
			timeToFire = nextAvailableTimeToFire;
		}

		synchronized (TIMER_LOCK) {
			if (timerDate == null || timeToFire.getTime() < timerDate.getTime()) {
				stopTimer();
				timerDate = timeToFire;
				timer = new Timer("UsageStatTimer", true);
				UsageStatistics.getInstance().setNextTransmission(timeToFire);
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						transmit(reason, false);
					}

				}, timerDate);
			}
		}

	}

	private static boolean shouldTransmitOnShutdown() {
		return EULADialog.getEULAAccepted();
	}

	/**
	 * Transmits user statistics data if required. Expects to be called from the shutdown hook on
	 * the EDT.
	 */
	public static void transmitOnShutdown() {
		if (Telemetry.USAGESTATS.isDenied()) {
			return;
		}

		stopTimer();

		if (shouldTransmitOnShutdown()) {
			if (UsageStatistics.getInstance().getNextTransmission() == null) {
				UsageStatistics.getInstance().setNextTransmission(getTransmissionDate(UsageStatistics.Reason.SHUTDOWN));
			}
			transmit(UsageStatistics.Reason.SHUTDOWN, true);
			UsageStatistics.getInstance().save();
		}
	}

}
