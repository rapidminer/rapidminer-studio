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
package com.rapidminer.tools;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.tools.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.xmlrpc.client.XmlRpcClient;


/**
 * A bug report can be send by the user. It should only be used in cases where an exception does not
 * occur due to a user error.
 * 
 * @author Simon Fischer, Ingo Mierswa, Marco Boeck
 */
public class BugReport {

	private static final int BUFFER_SIZE = 1024;

	/** the last number of lines to be added from the rm.log logfile */
	private static final int MAX_LOGFILE_LINES = 500;

	private static void getSystemProperties(String prefix, StringBuffer string) {
		string.append(prefix + " properties:" + Tools.getLineSeparator());

		Enumeration keys = System.getProperties().propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (key.startsWith(prefix)) {
				string.append("  " + key + "\t= " + System.getProperty(key) + Tools.getLineSeparator());
			}
		}
	}

	private static void getRapidMinerParameters(StringBuffer string) {
		string.append("RapidMiner Parameters:" + Tools.getLineSeparator());

		for (String key : ParameterService.getParameterKeys()) {
			string.append("  " + key + "\t= " + ParameterService.getParameterValue(key) + Tools.getLineSeparator());
		}
	}

	/**
	 * Creates a {@link String} of the last {@value #MAX_LOGFILE_LINES} lines of the rm.log file.
	 * 
	 * @return
	 */
	private static String getRelevantLogContent() {
		StringBuffer buffer = new StringBuffer();

		buffer.append("Log contents:" + Tools.getLineSeparator());
		buffer.append("------------" + Tools.getLineSeparator() + Tools.getLineSeparator());

		BufferedReader reader = null;
		try {
			List<String> logLineList = new LinkedList<>();
			reader = new BufferedReader(new FileReader(FileSystemService.getLogFile()));
			String line = reader.readLine();
			while (line != null) {
				logLineList.add(line);
				line = reader.readLine();

				// truncate list to only contain MAX_LOGFILE_LINES
				if (logLineList.size() > MAX_LOGFILE_LINES) {
					logLineList.remove(0);
				}
			}

			for (String lineString : logLineList) {
				buffer.append(lineString);
				buffer.append(Tools.getLineSeparator());
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.logservice.logfile.failed_to_read", e.getMessage());
			buffer.append("Failed to read log file:");
			buffer.append(e.getMessage());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.logservice.logfile.failed_to_close",
							e.getMessage());
				}
			}
		}

		return buffer.toString();
	}

	private static String getProperties() {
		StringBuffer string = new StringBuffer();
		string.append("System properties:" + Tools.getLineSeparator());
		string.append("------------" + Tools.getLineSeparator() + Tools.getLineSeparator());
		getSystemProperties("os", string);
		getSystemProperties("java", string);
		getRapidMinerParameters(string);
		return string.toString();
	}

	private static String getStackTrace(Throwable throwable) {
		StringBuffer string = new StringBuffer();
		string.append("Stack trace:" + Tools.getLineSeparator());
		string.append("------------" + Tools.getLineSeparator() + Tools.getLineSeparator());
		while (throwable != null) {
			string.append("Exception:\t" + throwable.getClass().getName() + Tools.getLineSeparator());
			string.append("Message:\t" + throwable.getMessage() + Tools.getLineSeparator());
			string.append("Stack trace:" + Tools.getLineSeparator());
			StackTraceElement[] ste = throwable.getStackTrace();
			for (StackTraceElement element : ste) {
				string.append("  " + element + Tools.getLineSeparator());
			}
			string.append(Tools.getLineSeparator());

			throwable = throwable.getCause();
			if (throwable != null) {
				string.append("");
				string.append("Cause:");
			}
		}
		return string.toString();
	}

	public static void createBugReport(File reportFile, Throwable exception, String userMessage, Process process,
			String logMessage, File[] attachments) throws IOException {
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(reportFile));
		zipOut.setComment("RapidMiner bug report - generated " + new Date());
		write("message.txt", "User message", userMessage, zipOut);
		write("_process.xml", "Process as in memory.", process.getRootOperator().getXML(false), zipOut);
		if (process.getProcessLocation() != null) {
			try {
				String contents = process.getProcessLocation().getRawXML();
				write(process.getProcessLocation().getShortName(), "Raw process file in repository.", contents, zipOut);
			} catch (Throwable t) {
				write(process.getProcessLocation().getShortName(), "Raw process file in repository.",
						"could not read: " + t, zipOut);
			}
		}
		write("_log.txt", "Log message", logMessage, zipOut);
		write("_properties.txt", "System properties, information about java version and operating system", getProperties(),
				zipOut);
		write("_exception.txt", "Exception stack trace", getStackTrace(exception), zipOut);

		for (File attachment : attachments) {
			writeFile(attachment, zipOut);
		}
		zipOut.close();
	}

	/**
	 * Creates the BugZilla bugreport.
	 * 
	 * @param client
	 *            the logged in BugZilla client
	 * @param exception
	 *            the exception which was thrown by the bug
	 * @param userSummary
	 *            summary of the bug
	 * @param completeDescription
	 *            description of the bug
	 * @param component
	 *            the component which malfunctionied
	 * @param version
	 *            the RM version
	 * @param severity
	 *            the severity of the bug
	 * @param platform
	 *            the platform (e.g. PC or Mac)
	 * @param os
	 *            the OS
	 * @param attachments
	 *            a list of optional attachements
	 * @param attachProcess
	 *            if <code>true</code>, will attach the current process xml
	 * @param attachSystemProps
	 *            if <code>true</code>, will attach system properties
	 * @throws Exception
	 */
	public static void createBugZillaReport(XmlRpcClient client, Throwable exception, String userSummary,
			String completeDescription, String component, String version, String severity, String platform, String os,
			File[] attachments, boolean attachProcess, boolean attachSystemProps, boolean attachLogFile) throws Exception {

		// create temp files with all the data we need
		// create process xml file for later attachement if user agreed to it
		File processFile = File.createTempFile("_process", ".xml");
		processFile.deleteOnExit();
		String xmlProcess;
		if (RapidMinerGUI.getMainFrame().getProcess().getProcessLocation() != null) {
			try {
				xmlProcess = RapidMinerGUI.getMainFrame().getProcess().getProcessLocation().getRawXML();
			} catch (Throwable t) {
				xmlProcess = "could not read: " + t;
			}
		} else {
			xmlProcess = "no process available";
		}
		writeFile(processFile, xmlProcess);

		// create system properties tempfile for later attachement if user agreed to it
		File propertiesFile = File.createTempFile("_properties", ".txt");
		propertiesFile.deleteOnExit();
		writeFile(propertiesFile, getProperties());

		// create log tempfile (last MAX_LOGFILE_LINES rows) for later attachement
		File logTempFile = File.createTempFile("_log", ".txt");
		logTempFile.delete();
		writeFile(logTempFile, getRelevantLogContent());

		// append the RM version to the description
		StringBuffer buffer = new StringBuffer(completeDescription);
		buffer.append(Tools.getLineSeparator());
		buffer.append(Tools.getLineSeparator());
		buffer.append(getStackTrace(exception));
		buffer.append(Tools.getLineSeparator());
		buffer.append(Tools.getLineSeparator());
		buffer.append("RapidMiner: ");
		buffer.append(RapidMiner.getVersion());
		buffer.append(Tools.getLineSeparator());
		for (Plugin plugin : Plugin.getAllPlugins()) {
			buffer.append(plugin.getName());
			buffer.append(": ");
			buffer.append(plugin.getVersion());
			buffer.append(Tools.getLineSeparator());
		}
		completeDescription = buffer.toString();

		// call BugZilla via xml-rpc
		XmlRpcClient rpcClient = client;

		Map<String, String> bugMap = new HashMap<>();
		bugMap.put("product", "RapidMiner");
		bugMap.put("component", component);
		bugMap.put("summary", userSummary);
		bugMap.put("description", completeDescription);
		bugMap.put("version", version);
		bugMap.put("op_sys", os);
		bugMap.put("platform", platform);
		bugMap.put("severity", severity);
		bugMap.put("status", "NEW");

		Map createResult = (Map) rpcClient.execute("Bug.create", new Object[] { bugMap });
		// LogService.getRoot().fine("Bug submitted successfully. Bug ID: " +
		// createResult.get("id"));
		LogService.getRoot().log(Level.FINE, "com.rapidminer.tools.BugReport.bug_submitted", createResult.get("id"));

		String id = String.valueOf(createResult.get("id"));
		Map<String, Object> attachmentMap = new HashMap<>();
		FileInputStream fileInputStream = null;
		// add process xml file attachment if selected
		if (attachProcess) {
			attachmentMap.put("ids", new String[] { id });
			// BugZilla API states Base64 encoded string is needed, but it does not work
			// attachmentMap.put("data", Base64.encodeFromFile(processFile.getPath()));
			try {
				fileInputStream = new FileInputStream(processFile);
				byte[] data = new byte[(int) processFile.length()];
				fileInputStream.read(data);
				attachmentMap.put("data", data);
				attachmentMap.put("file_name", "process.xml");
				attachmentMap.put("summary", "process.xml");
				attachmentMap.put("content_type", "application/xml");

				createResult = (Map) rpcClient.execute("Bug.add_attachment", new Object[] { attachmentMap });
				attachmentMap.clear();
			} finally {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			}
		}

		// add system properties file attachment if selected
		if (attachSystemProps) {
			attachmentMap.put("ids", new String[] { id });
			// BugZilla API states Base64 encoded string is needed, but it does not work
			// attachmentMap.put("data", Base64.encodeFromFile(propertiesFile.getPath()));
			try {
				fileInputStream = new FileInputStream(propertiesFile);
				byte[] data = new byte[(int) propertiesFile.length()];
				fileInputStream.read(data);
				attachmentMap.put("data", data);
				attachmentMap.put("file_name", "system-properties.txt");
				attachmentMap.put("summary", "system-properties.txt");
				attachmentMap.put("content_type", "text/plain");

				createResult = (Map) rpcClient.execute("Bug.add_attachment", new Object[] { attachmentMap });
				attachmentMap.clear();
			} finally {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			}
		}

		// add rm.log file attachment
		if (attachLogFile) {
			attachmentMap.put("ids", new String[] { id });
			// BugZilla API states Base64 encoded string is needed, but it does not work
			// attachmentMap.put("data", Base64.encodeFromFile(propertiesFile.getPath()));
			try {
				fileInputStream = new FileInputStream(logTempFile);
				byte[] data = new byte[(int) logTempFile.length()];
				fileInputStream.read(data);
				attachmentMap.put("data", data);
				attachmentMap.put("file_name", "rm.log");
				attachmentMap.put("summary", "rm.log");
				attachmentMap.put("content_type", "text/plain");

				createResult = (Map) rpcClient.execute("Bug.add_attachment", new Object[] { attachmentMap });
				attachmentMap.clear();
			} finally {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			}
		}

		// add attachments by user
		for (File file : attachments) {
			attachmentMap.put("ids", new String[] { id });
			// BugZilla API states Base64 encoded string is needed, but it does not work
			// attachmentMap.put("data", Base64.encodeFromFile(file.getPath()));
			try {
				fileInputStream = new FileInputStream(file);
				byte[] data = new byte[(int) file.length()];
				fileInputStream.read(data);
				attachmentMap.put("data", data);
				attachmentMap.put("file_name", file.getName());
				attachmentMap.put("summary", file.getName());
				attachmentMap.put("content_type", "application/data");

				createResult = (Map) rpcClient.execute("Bug.add_attachment", new Object[] { attachmentMap });
				attachmentMap.clear();
			} finally {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			}
		}

	}

	/**
	 * Creates the complete description of the bug including user description, exception stack
	 * trace, system properties and RM and plugin versions.
	 * 
	 * @param userDescription
	 *            the description the user entered
	 * @param exception
	 *            the {@link Throwable} on which the bug report is based upon
	 * @param attachProcess
	 *            if true, will attach the process xml
	 * @param attachSystemProps
	 *            if true, will attach the system properties
	 * @return the human readable complete bug report
	 */
	public static String createCompleteBugDescription(String userDescription, Throwable exception, boolean attachProcess,
			boolean attachSystemProps, boolean attachLog) {
		StringBuffer buffer = new StringBuffer();

		// append the user description
		buffer.append(userDescription);

		// append RapidMiner and plugin versions
		buffer.append(Tools.getLineSeparator());
		buffer.append(Tools.getLineSeparator());
		buffer.append("RapidMiner: ");
		buffer.append(RapidMiner.getVersion());
		buffer.append(Tools.getLineSeparator());
		for (Plugin plugin : Plugin.getAllPlugins()) {
			buffer.append(plugin.getName());
			buffer.append(": ");
			buffer.append(plugin.getVersion());
			buffer.append(Tools.getLineSeparator());
		}

		// append stack trace
		buffer.append(Tools.getLineSeparator());
		buffer.append(Tools.getLineSeparator());
		buffer.append(Tools.getLineSeparator());
		buffer.append(getStackTrace(exception));

		// if user selected it, attach process xml
		if (attachProcess) {
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			buffer.append("Process:");
			buffer.append(Tools.getLineSeparator());
			buffer.append("------------");
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			String xmlProcess;
			if (RapidMinerGUI.getMainFrame().getProcess() != null) {
				try {
					xmlProcess = RapidMinerGUI.getMainFrame().getProcess().getRootOperator().getXML(false);
				} catch (Throwable t) {
					xmlProcess = "could not read: " + t;
				}
			} else {
				xmlProcess = "no process available";
			}
			buffer.append(xmlProcess);
		}

		// if user agreed to it, attach system properties
		if (attachSystemProps) {
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			buffer.append(getProperties());
		}

		if (attachLog) {
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			buffer.append(Tools.getLineSeparator());
			buffer.append(getRelevantLogContent());
		}

		return buffer.toString();
	}

	private static void writeFile(File file, ZipOutputStream out) throws IOException {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			out.putNextEntry(new ZipEntry(file.getName()));
			byte[] buffer = new byte[BUFFER_SIZE];
			int read = -1;
			do {
				read = in.read(buffer);
				if (read > -1) {
					out.write(buffer, 0, read);
				}
			} while (read > -1);
			out.closeEntry();
		} catch (IOException e) {
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private static void write(String name, String comment, String string, ZipOutputStream out) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setComment(comment);
		out.putNextEntry(entry);

		PrintStream print = new PrintStream(out);
		print.println(string);
		print.flush();

		out.closeEntry();
	}

	private static void writeFile(File file, String contents) throws IOException {
		FileWriter writer = new FileWriter(file);
		writer.write(contents);
		writer.close();
	}
}
