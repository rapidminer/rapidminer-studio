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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMinerVersion;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.license.License;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.settings.Telemetry;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.XMLException;


/**
 * Collects statistics about usage of operators. Statistics can be sent to a server collecting them.
 * Counting and resetting is thread safe.
 *
 * @see ActionStatisticsCollector ActionStatisticsCollector for the actual hooks to log usage stats.
 *
 * @author Simon Fischer
 *
 */
public class UsageStatistics {

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	private static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
		return new SimpleDateFormat(ParameterTypeDateFormat.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS); // this is a legacy wrong format to parse old dates
	});

	/** URL to send the statistics values to. */
	private static final String WEB_SERVICE_URL = "https://stats.rapidminer.com/usage-stats/upload/rapidminer";

	private static final UsageStatistics INSTANCE = new UsageStatistics();

	private Date initialSetup;
	private Date nextTransmission;
	private String randomKey;

	/**
	 * The reason for usage statistics upload
	 */
	public enum Reason {
		SHUTDOWN, ALWAYS, ASK, CTA, ROWLIMIT
	}

	/**
	 * Singleton object of UsageStatistics.
	 *
	 * @return
	 */
	public static UsageStatistics getInstance() {
		return INSTANCE;
	}

	private UsageStatistics() {
		load();
	}

	private void init() {
		this.randomKey = createRandomKey();
		this.initialSetup = new Date();
		ActionStatisticsCollector.getInstance().init();
	}

	/** Loads the statistics from the user file. */
	private void load() {
		if (Telemetry.USAGESTATS.isDenied()) {
			LogService.getRoot().log(Level.CONFIG,
					"com.rapidminer.gui.telemetry.accessing_online_services_disallowed");
			return;
		}
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
				Document doc = XMLTools.createDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				String lastResetString = root.getAttribute("last-reset");
				Date lastReset = parseDate(lastResetString);
				if (lastReset == null) {
					lastReset = new Date();
				}

				this.randomKey = root.getAttribute("random-key");
				if (randomKey == null || randomKey.isEmpty()) {
					this.randomKey = createRandomKey();
				}

				String initialSetup = root.getAttribute("initial-setup");
				this.initialSetup = parseDate(initialSetup);

				String nextTransmission = root.getAttribute("next-transmission");
				this.nextTransmission = parseDate(nextTransmission);
				if (this.nextTransmission == null) {
					this.nextTransmission = new Date();
				}

				ActionStatisticsCollector.getInstance().load(root, lastReset);
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.tools.usagestats.UsageStatistics.loading_operator_usage_error", e), e);
				init();
			}
		} else {
			init();
		}
	}

	/**
	 * Tries to parse with the new date format first, old format second, returns null if both fail.
	 *
	 * @param date
	 * @return
	 */
	private Date parseDate(String date) {
		if (date != null && !date.trim().isEmpty()) {
			try {
				return Date.from(ZonedDateTime.parse(date).toInstant());
			} catch (DateTimeParseException e) {
				try {
					return getDateFormat().parse(date);
				} catch (ParseException e2) {
					return null;
				}
			}
		}

		return null;
	}

	private String formatDate(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toString();
	}

	/**
	 * Checks whether the usage statistics should be transmitted on studio shutdown.
	 *
	 * @return {@code true} if the usage statistics should be transmitted
	 */

	private String createRandomKey() {
		StringBuilder randomKey = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 16; i++) {
			randomKey.append((char) ('A' + random.nextInt(26)));
		}
		return randomKey.toString();
	}

	private Document getXML(ActionStatisticsCollector.ActionStatisticsSnapshot snapshot) {
		return getXML(null, snapshot);
	}

	private Document getXML(Reason reason, ActionStatisticsCollector.ActionStatisticsSnapshot snapshot) {
		Document doc;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Cannot create parser: " + e, e);
		}
		Element root = doc.createElement("usageStatistics");

		root.setAttribute("last-reset", formatDate(snapshot.getFrom()));

		root.setAttribute("closed-at", formatDate(snapshot.getTo()));
		if (nextTransmission != null) {
			root.setAttribute("next-transmission", formatDate(nextTransmission));
		}
		root.setAttribute("random-key", this.randomKey);
		if (this.initialSetup != null) {
			root.setAttribute("initial-setup", formatDate(initialSetup));
		}
		root.setAttribute("rapidminer-version", new RapidMinerVersion().toString());
		root.setAttribute("os-name", System.getProperties().getProperty("os.name"));
		root.setAttribute("os-version", System.getProperties().getProperty("os.version"));
		root.setAttribute("os-cores", "" + SystemInfoUtilities.getNumberOfProcessors());
		String osMemory = getOSMemory();
		if (osMemory != null) {
			root.setAttribute("os-memory", osMemory);
		}
		root.setAttribute("jvm-max-heap", "" + SystemInfoUtilities.getMaxHeapMemorySize());
		License activeLicense = ProductConstraintManager.INSTANCE.getActiveLicense();
		if (activeLicense != null && activeLicense.getLicenseID() != null) {
			root.setAttribute("lid", activeLicense.getLicenseID());
		}
		if (reason != null) {
			root.setAttribute("reason", reason.toString());
		}

		doc.appendChild(root);

		snapshot.toXML(doc, root);

		return doc;
	}

	/**
	 * @return the total memory of the os or {@code null} if it cannot be read
	 */
	private String getOSMemory() {
		try {
			Long total = SystemInfoUtilities.getTotalPhysicalMemorySize();
			if (total != null) {
				return total.toString();
			}
			return null;
		} catch (IOException e) {
			// cannot read total memory
			return null;
		}
	}

	/**
	 * 	Saves the statistics to a user file.
	 */
	public void save() {
		if (Telemetry.USAGESTATS.isDenied()) {
			LogService.getRoot().config(
					"com.rapidminer.gui.telemetry.accessing_online_services_disallowed");
			return;
		}

		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LogService.getRoot().config(
					"com.rapidminer.gui.tools.usagestats.UsageStatistics.accessing_file_system_error_bypassing_save");
			return;
		}

		File file = FileSystemService.getUserConfigFile("usagestats.xml");
		try {
			LogService.getRoot().log(Level.CONFIG,
					"com.rapidminer.gui.tools.usagestats.UsageStatistics.saving_operator_usage");
			XMLTools.stream(getXML(ActionStatisticsCollector.getInstance().getActionStatisticsSnapshot(false)), file, StandardCharsets.UTF_8);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.tools.usagestats.UsageStatistics.saving_operator_usage_error", e), e);
		}
	}


	private DateFormat getDateFormat() {
		return DATE_FORMAT.get();
	}

	/**
	 * Uploads the usage statistics
	 *
	 * @param progressListener
	 * @param reason
	 * @throws Exception
	 */
	void transferUsageStats(Reason reason, ActionStatisticsCollector.ActionStatisticsSnapshot snapshot, ProgressListener progressListener) throws XMLException, IOException {
		if (Telemetry.USAGESTATS.isDenied()) {
			return;
		}
		progressListener.setCompleted(10);
		String xml = XMLTools.toString(getXML(reason, snapshot));
		progressListener.setCompleted(20);
		URL url = new URL(WEB_SERVICE_URL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		WebServiceTools.setURLConnectionDefaults(con);
		try (Writer writer = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8)) {
			progressListener.setCompleted(30);
			writer.write(xml);
			writer.flush();
			progressListener.setCompleted(90);
			if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("Response from server: " + con.getResponseMessage());
			}
		} finally {
			progressListener.complete();
		}
	}

	/**
	 * Returns the user key for this session.
	 *
	 * @return the user key
	 */
	public String getUserKey() {
		return randomKey;
	}

	/**
	 * Returns the date at which the next transmission should be scheduled.
	 *
	 * @return
	 */
	Date getNextTransmission() {
		return nextTransmission;
	}

	/**
	 * Sets the date at which the next transmission should be scheduled.
	 *
	 * @param nextTransmission
	 */
	void setNextTransmission(Date nextTransmission) {
		this.nextTransmission = nextTransmission;
	}

}
