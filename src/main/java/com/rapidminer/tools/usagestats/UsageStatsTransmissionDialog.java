/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.Timer;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;


/**
 * Asks the user to transmit usage statistics. This class also contains static methods to schedule
 * timers that open this dialog and perform the transfer.
 *
 * @author Simon Fischer
 */
public class UsageStatsTransmissionDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;
	private static final long MIN_FIRE_INTERVAL = 1000 * 60; // Wait at least 60 seconds after start
	// of RM

	public static final int ASK = 0;
	public static final int ALWAYS = 1;
	public static final int NEVER = 2;

	private static final int YES = 0;
	private static final int NO = 1;

	private int answer;

	private UsageStatsTransmissionDialog() {
		super(ApplicationFrame.getApplicationFrame(), "transmit_usage_statistics", ModalityType.APPLICATION_MODAL,
				new Object[] {});

		JButton neverButton = new JButton(new ResourceAction("never") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS, "never");
				ParameterService.saveParameters();
				answer = NO;
			}
		});
		JButton alwaysButton = new JButton(new ResourceAction("always") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				answer = YES;
				ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS, "always");
				ParameterService.saveParameters();
				dispose();
			}
		});

		JTable table = new ExtendedJTable(UsageStatistics.getInstance().getAsDataTable(), true, true, true);
		table.setFocusable(false);
		table.setBorder(null);
		ExtendedJScrollPane tablePane = new ExtendedJScrollPane(table);
		tablePane.setBorder(createBorder());
		layoutDefault(tablePane, NORMAL, alwaysButton, makeOkButton("yes_later"), makeCancelButton("no_later"), neverButton);
	}

	@Override
	public void ok() {
		answer = YES;
		super.ok();
	}

	@Override
	public void cancel() {
		answer = NO;
		super.cancel();
	}

	/** Starts the first timer. */
	public static void init() {
		startTimer();
		ActionStatisticsCollector.getInstance().start();
	}

	/**
	 * Pops up a dialog (unless "always" or "never" was chosen before asking the user to transmit
	 * usage statistics.
	 */
	private static boolean askForTransmission() {
		String property = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS);
		if ("never".equals(property)) {
			return false;
		} else if ("always".equals(property)) {
			return true;
		} else {
			UsageStatsTransmissionDialog trd = new UsageStatsTransmissionDialog();
			trd.setVisible(true);
			return trd.answer == YES;
		}
	}

	/**
	 * Schedules the next dialog popup to the next transmission time of the UsageStatistics, but at
	 * least {@link #MIN_FIRE_INTERVAL} milliseconds from now.
	 */
	private static void startTimer() {
		if (UsageStatistics.getInstance().hasFailedToday()) {
			return;
		}
		long timeToFire = UsageStatistics.getInstance().getNextTransmission().getTime() - System.currentTimeMillis();
		if (timeToFire < 0) {
			timeToFire = MIN_FIRE_INTERVAL;
		}
		Timer timer = new Timer((int) timeToFire, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (UsageStatsTransmissionDialog.askForTransmission()) {
					new ProgressThread("transmit_usagestats") {

						@Override
						public void run() {
							getProgressListener().setTotal(100);
							getProgressListener().setCompleted(10);
							try {
								if (UsageStatistics.getInstance().transferUsageStats(getProgressListener())) {
									UsageStatistics.getInstance().reset();
									UsageStatistics.getInstance().scheduleTransmission(false);
								} else {
									UsageStatistics.getInstance().scheduleTransmission(true);
								}
							} catch (Exception e) {
								LogService
								.getRoot()
								.log(Level.WARNING,
										I18N.getMessage(LogService.getRoot().getResourceBundle(),
												"com.rapidminer.tools.usagestats.UsageStatsTransmissionDialog.submitting_operator_usage_statistics_error"),
												e);
								UsageStatistics.getInstance().scheduleTransmission(true);
							}
							getProgressListener().setCompleted(90);
							startTimer();
							getProgressListener().setCompleted(100);
							getProgressListener().complete();
						}
					}.start();
				} else {
					UsageStatistics.getInstance().scheduleTransmissionFromNow();
					startTimer();
				}
			}
		});
		timer.setRepeats(false);
		timer.start();
	}
}
