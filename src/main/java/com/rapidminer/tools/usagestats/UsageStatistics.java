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

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMinerVersion;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.license.License;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.WebServiceTools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;

import javax.swing.table.TableModel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Collects statistics about usage of operators. Statistics can be sent to a server collecting them.
 * Counting and resetting is thread safe.
 * 
 * @see UsageStatsTransmissionDialog
 * 
 * @author Simon Fischer
 * 
 */
public class UsageStatistics {

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		}
	};

	/** URL to send the statistics values to. TODO: Use correct URL */
	private static final String WEB_SERVICE_URL = "http://stats.rapidminer.com/usage-stats/upload/rapidminer";

	private static final long TRANSMISSION_INTERVAL = 1000 * 60 * 60 * 24 * 14; // 14 days

	private Date lastReset;
	private Date nextTransmission;

	private static final UsageStatistics INSTANCE = new UsageStatistics();

	private String randomKey;

	private transient boolean failedToday = false;

	public static UsageStatistics getInstance() {
		return INSTANCE;
	}

	private UsageStatistics() {
		load();
	}

	/** Loads the statistics from the user file. */
	private void load() {
		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LogService.getRoot().log(Level.CONFIG,
					"com.rapidminer.gui.tools.usagestats.UsageStatistics.accessing_file_system_error_bypassing_loading");
			return;
		}
		File file = FileSystemService.getUserConfigFile("usagestats.xml");
		if (file.exists()) {
			try {
				LogService.getRoot().log(Level.CONFIG,
						"com.rapidminer.gui.tools.usagestats.UsageStatistics.loading_operator_statistics");
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				String lastReset = root.getAttribute("last-reset");
				if ((lastReset != null) && !lastReset.isEmpty()) {
					try {
						this.lastReset = getDateFormat().parse(lastReset);
					} catch (ParseException e) {
						this.lastReset = new Date();
					}
				} else {
					this.lastReset = new Date();
				}

				this.randomKey = root.getAttribute("random-key");
				if ((randomKey == null) || randomKey.isEmpty()) {
					this.randomKey = createRandomKey();
				}

				String nextTransmission = root.getAttribute("next-transmission");
				if ((lastReset != null) && !lastReset.isEmpty()) {
					try {
						this.nextTransmission = getDateFormat().parse(nextTransmission);
					} catch (ParseException e) {
						scheduleTransmission(true);
					}
				} else {
					scheduleTransmission(false);
				}

				Element actionStats = XMLTools.getChildElement(root, ActionStatisticsCollector.XML_TAG, false);
				if (actionStats != null) {
					ActionStatisticsCollector.getInstance().load(actionStats);
				}
			} catch (Exception e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.tools.usagestats.UsageStatistics.loading_operator_usage_error", e), e);
			}
		} else {
			this.randomKey = createRandomKey();
		}
	}

	private String createRandomKey() {
		StringBuilder randomKey = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 16; i++) {
			randomKey.append((char) ('A' + random.nextInt(26)));
		}
		return randomKey.toString();
	}

	/** Sets all current counters to 0 and sets the last reset date to the current time. */
	public synchronized void reset() {
		ActionStatisticsCollector.getInstance().clear();
		this.lastReset = new Date();
	}

	private Document getXML() {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Cannot create parser: " + e, e);
		}
		Element root = doc.createElement("usageStatistics");

		if (lastReset != null) {
			root.setAttribute("last-reset", getDateFormat().format(lastReset));
		}
		if (nextTransmission != null) {
			root.setAttribute("next-transmission", getDateFormat().format(nextTransmission));
		}
		root.setAttribute("random-key", this.randomKey);
		root.setAttribute("rapidminer-version", new RapidMinerVersion().toString());
		root.setAttribute("os-name", System.getProperties().getProperty("os.name"));
		root.setAttribute("os-version", System.getProperties().getProperty("os.version"));
		License activeLicense = ProductConstraintManager.INSTANCE.getActiveLicense();
		if ((activeLicense != null) && (activeLicense.getLicenseID() != null)) {
			root.setAttribute("lid", activeLicense.getLicenseID());
		}

		doc.appendChild(root);

		root.appendChild(ActionStatisticsCollector.getInstance().getXML(doc));
		return doc;
	}

	/** Saves the statistics to a user file. */
	public void save() {
		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			File file = FileSystemService.getUserConfigFile("usagestats.xml");
			try {
				LogService.getRoot().log(Level.CONFIG,
						"com.rapidminer.gui.tools.usagestats.UsageStatistics.saving_operator_usage");
				XMLTools.stream(getXML(), file, null);
			} catch (Exception e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.tools.usagestats.UsageStatistics.saving_operator_usage_error", e), e);
			}
		} else {
			LogService.getRoot().config(
					"com.rapidminer.gui.tools.usagestats.UsageStatistics.accessing_file_system_error_bypassing_save");
		}
	}

	/** Returns the statistics as a data table that can be displayed to the user. */
	public TableModel getAsDataTable() {
		return new ActionStatisticsTable(ActionStatisticsCollector.getInstance().getCounts());
	}

	private DateFormat getDateFormat() {
		return DATE_FORMAT.get();
	}

	/**
	 * 
	 * @return true on success
	 */
	public boolean transferUsageStats(ProgressListener progressListener) throws Exception {
		progressListener.setCompleted(10);
		String xml = XMLTools.toString(getXML());
		progressListener.setCompleted(20);
		URL url = new URL(WEB_SERVICE_URL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		WebServiceTools.setURLConnectionDefaults(con);
		try (Writer writer = new OutputStreamWriter(con.getOutputStream())) {
			progressListener.setCompleted(30);
			writer.write(xml);
			writer.flush();
			progressListener.setCompleted(90);
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("Responde from server: " + con.getResponseMessage());
			} else {
				return true;
			}
		} finally {
			progressListener.complete();
		}
	}

	/** Sets the date for the next transmission. Starts no timers. */
	void scheduleTransmission(boolean lastAttemptFailed) {
		this.failedToday = true;
		this.nextTransmission = new Date(lastReset.getTime() + TRANSMISSION_INTERVAL);
	}

	/**
	 * Returns the user key for this session.
	 * 
	 * @return the user key
	 */
	public String getUserKey() {
		return randomKey;
	}

	/** Returns the date at which the next transmission should be scheduled. */
	public Date getNextTransmission() {
		if (nextTransmission == null) {
			scheduleTransmissionFromNow();
		}
		return nextTransmission;
	}

	public void scheduleTransmissionFromNow() {
		this.nextTransmission = new Date(System.currentTimeMillis() + TRANSMISSION_INTERVAL);
	}

	public boolean hasFailedToday() {
		return failedToday;
	}
}
