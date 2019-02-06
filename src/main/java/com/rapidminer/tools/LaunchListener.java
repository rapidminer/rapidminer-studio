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
package com.rapidminer.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.io.process.XMLTools;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * When started for the first time, listens on a given socket on localhost. If started for the
 * second time, contacts this socket and passes command line options to this socket. We use a lock
 * file mechanism to make sure only one server instance exists.
 *
 * The port number on which we listen is stored in a file in the users home directory,
 * .RapidMiner/rapidminer.lock. The lock file contains two lines: the port number of the first
 * instance and a random session id. It is readable only to the current user.
 *
 * In order to use this class, first try to contact another instance by calling
 * {@link #sendArgsToOtherInstanceIfUp(String...)}. If true is returned, commands were sent to and
 * processed by the other instance successfully and we can terminate. If false is returned, the
 * other instance is not running. In that case, call {@link #installListener(RemoteControlHandler)}.
 * Now, when another instance is started, callbacks are made to the {@link RemoteControlHandler}
 * passed. Precisely this is done when calling
 * {@link #defaultLaunchWithArguments(String[], RemoteControlHandler)}.
 *
 * Currently, the only supported message accepted by the server has this format:
 *
 * <args session-id="12345678"><arg>arg1</arg><arg>arg2</arg>...</args>
 *
 * When opening the socket, the server will respond with <hello/> and confirm success with <ok/>.
 * Error messages come as <error>message</error>.
 *
 * This class is deliberately not using any fancy stuff like RMI to make debugging easy. Just
 * connect to socket and type to debug. Also, no sophisticated protocol is necessary since this
 * class will only talk to itself, and never even to a server of another version.
 *
 * @author Simon Fischer
 *
 */
public enum LaunchListener {

	/** The singleton instance. */
	INSTANCE;

	/** Callbacks will be made to this interface when another client contacts us. */
	public static interface RemoteControlHandler {

		/** Callback method called when another client starts. */
		boolean handleArguments(String[] args);

	}

	/** Returned to client in case the command could not be executed. */
	private static final String ERROR_COMMAND_FAILED = "<error>command execution failed</error>";

	/** Error message indicating that the transmitted command is unknown. */
	private static final String ERROR_UNKNOWN_COMMAND = "<error>unknown command</error>";

	/** Error message indicating that the session id is missing from the command sent by the client. */
	private static final String ERROR_NO_SESSION_ID = "<error>no session id</error>";

	/**
	 * Error message indicating that the session id sent by the client does not match the one
	 * created by the server.
	 */
	private static final String ERROR_WRONG_SESSION_ID = "<error>wrong session id</error>";

	/** The other instance rejected execution of the command. */
	private static final String ERROR_REJECTED = "<error>rejected</error>";

	/** Confirms successful execution on the command on the remote instance. */
	private static final String RESPONSE_OK = "<ok/>";

	/** Sent by the other instance to confirm it is RapidMiner. */
	private static final String HELLO_MESSAGE = "<hello/>";

	/** XML-Attribute in the root element holding the session id. */
	private static final String ATTRIBUTE_SESSION_ID = "session-id";

	private static final Logger LOGGER = Logger.getLogger(LaunchListener.class.getName());

	/** Commands received from the client will be sent to this handler. */
	private RemoteControlHandler handler;

	/** Identifies the session that this instance started or that is used by the other instance. */
	private long sessionId;

	/**
	 * Lock file holding the port number and session id. Should be readable only by the current
	 * user.
	 */
	private File getLockFile() {
		return FileSystemService.getUserConfigFile("rapidminer.lock");
	}

	/** Returns the singleton instance */
	public static LaunchListener getInstance() {
		return INSTANCE;
	}

	/**
	 * Starts a server socket on a random port that sends commands that it reads to the given
	 * handler.
	 *
	 * Also creates {@link #getLockFile()} with information on how to contact this socket.
	 */
	private void installListener(final RemoteControlHandler handler) throws IOException {
		// port 0 = let system assign port
		// backlog 1 = we don't expect simultaneous requests
		final ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
		final File socketFile = getLockFile();
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				LOGGER.config("Deleting " + socketFile);
				socketFile.delete();
				try {
					serverSocket.close();
				} catch (IOException e) {
					// silent - we're dying anyway
				}
			}
		});
		final int port = serverSocket.getLocalPort();
		LOGGER.config("Listening for other instances on port " + port + ". Writing " + socketFile + ".");
		PrintStream socketOut = new PrintStream(socketFile);
		socketOut.println(String.valueOf(port));
		sessionId = new Random().nextLong();
		socketOut.println(String.valueOf(sessionId));
		socketOut.close();

		try {
			OperatingSystem os = SystemInfoUtilities.getOperatingSystem();
			if (os != OperatingSystem.WINDOWS) {
				Files.setPosixFilePermissions(socketFile.toPath(),
						EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
			}
		} catch (UnsupportedOperationException e) {
			// ignore
		}
		socketFile.deleteOnExit();

		Thread listenerThread = new Thread("Launch-Listener") {

			@Override
			public void run() {
				LaunchListener.this.handler = handler;
				while (true) {
					Socket client;
					try {
						client = serverSocket.accept();
						// We don't spawn another thread here.
						// Assume no malicious client and communication is quick.
						readFromSecondClient(client);
					} catch (SocketException e) {
						// small log level because the JVMOptionBuilder check can trigger it
						LogService.getRoot().log(
								Level.FINE,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.LaunchListener.accepting_socket_connection_error",
										e.getMessage()));
					} catch (IOException e) {
						LogService.getRoot().log(
								Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.LaunchListener.accepting_socket_connection_error",
										e.getMessage()), e);
					}
				}
			}
		};
		listenerThread.setDaemon(true);
		listenerThread.start();
	}

	/** Reads and processes a command from the second client. */
	private void readFromSecondClient(Socket client) {
		try {
			LOGGER.config("Second client launched.");
			PrintStream out = new PrintStream(client.getOutputStream());
			out.println(HELLO_MESSAGE);
			out.flush();
			Document doc;
			try {
				doc = XMLTools.parse(client.getInputStream());
			} catch (SAXException e) {
				LOGGER.log(Level.FINE, "Unknown command from other client: " + e.getMessage());
				out.println(ERROR_UNKNOWN_COMMAND);
				return;
			}
			LOGGER.config("Read XML document from other client: ");
			String sessionIdStr = doc.getDocumentElement().getAttribute(ATTRIBUTE_SESSION_ID);
			if (sessionIdStr == null || sessionIdStr.isEmpty()) {
				out.println(ERROR_NO_SESSION_ID);
				LOGGER.warning("Missing session id in call from other client.");
			} else if (!sessionIdStr.equals(String.valueOf(sessionId))) {
				out.println(ERROR_WRONG_SESSION_ID);
				LOGGER.warning("Wrong session id in call from other client.");
			} else {
				final String command = doc.getDocumentElement().getTagName();
				if ("args".equals(command)) {
					NodeList argsElems = doc.getDocumentElement().getElementsByTagName("arg");
					List<String> args = new LinkedList<String>();
					for (int i = 0; i < argsElems.getLength(); i++) {
						args.add(argsElems.item(i).getTextContent());
					}
					if (handler != null) {
						LOGGER.config("Handling <args> command from other client.");
						try {
							if (handler.handleArguments(args.toArray(new String[args.size()]))) {
								out.println(RESPONSE_OK);
								out.flush();
							} else {
								out.println(ERROR_REJECTED);
							}
						} catch (Exception e) {
							LOGGER.log(Level.WARNING, "Error executing remote control command: " + e, e);
							out.println(ERROR_COMMAND_FAILED);
						}
					} else {
						LOGGER.warning("Other client sent <args> command, but I don't have a handler installed.");
						out.println(ERROR_COMMAND_FAILED);
					}
				} else {
					out.println(ERROR_UNKNOWN_COMMAND);
					LOGGER.warning("Unknown command from second client: <" + command + ">.");
				}
			}
			client.close();
		} catch (SocketException e) {
			LOGGER.log(Level.CONFIG, "Talking to client aborted. Assume launcher instance availability check.");
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to talk to client: " + e, e);
		}
	}

	/** Obtains a socket talking to another RapidMiner Studio instance. */
	private Socket getOtherInstance() {
		File socketFile = getLockFile();
		if (!socketFile.exists()) {
			LOGGER.config("Lock file " + socketFile + " does not exist. Assuming I am the first instance.");
			return null;
		}
		int port;

		try (BufferedReader in = new BufferedReader(new FileReader(socketFile))) {
			String portStr = in.readLine();
			String sessionIdStr = in.readLine();
			port = Integer.parseInt(portStr);
			sessionId = Long.parseLong(sessionIdStr);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read socket file '" + socketFile + "': " + e, e);
			return null;
		}
		LOGGER.config("Checking for running instance on port " + port + ".");
		try {
			return new Socket(InetAddress.getLoopbackAddress(), port);
		} catch (IOException e) {
			LOGGER.warning("Found lock file but no other instance running. Assuming unclean shutdown of previous launch.");
			return null;
		}
	}

	/**
	 * Checks whether the buffer contains a {@link #HELLO_MESSAGE} to verify this is a RapidMiner
	 * Studio instance.
	 */
	private boolean readHelloMessage(BufferedReader in) throws IOException {
		boolean isRM;
		String line = in.readLine();
		if (HELLO_MESSAGE.equals(line)) {
			LOGGER.log(Level.INFO, "Found other RapidMiner instance.");
			isRM = true;
		} else {
			LOGGER.config("Read unknown string from other instance: " + line);
			isRM = false;
		}
		return isRM;
	}

	/**
	 * Reads the lock file and attempts to contact the other instance. If successful, sends the
	 * arguments and returns true. Returns false otherwise (i.e. if the lock file is missing, the
	 * other instance does not respond in case of an unclean shutdown, or the other instance
	 * responds with an unexpected message).
	 */
	private boolean sendArgsToOtherInstanceIfUp(String... args) {
		final Socket other = getOtherInstance();
		if (other == null) {
			return false;
		}
		try (BufferedReader in = new BufferedReader(new InputStreamReader(other.getInputStream()))) {
			boolean isRM = readHelloMessage(in);
			if (!isRM) {
				LOGGER.warning("Found other instance listening, but does not look like a RapidMiner instance.");
				return false;
			} else {
				// Only send if we really have arguments. Otherwise just check whether other
				// instance
				// is up so we do not spawn a new one.
				if (args.length > 0) {
					LOGGER.config("Sending arguments to other RapidMiner instance: " + Arrays.toString(args));
					Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					Element root = doc.createElement("args");
					root.setAttribute(ATTRIBUTE_SESSION_ID, String.valueOf(sessionId));
					doc.appendChild(root);
					for (String arg : args) {
						Element argElem = doc.createElement("arg");
						argElem.setTextContent(arg);
						root.appendChild(argElem);
					}
					XMLTools.stream(doc, other.getOutputStream(), null);
				}
				other.getOutputStream().close();
				return true;
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to talk to other instance: " + e, e);
			return false;
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.WARNING, "Cannot create XML document: " + e, e);
			return false;
		} catch (XMLException e) {
			LOGGER.log(Level.WARNING, "Cannot create XML document: " + e, e);
			return false;
		}
	}

	/**
	 * Sends the arguments to the other client, if up.
	 *
	 * @return true if other client is not up, so we must continue launching our APP.
	 * */
	public static boolean defaultLaunchWithArguments(String[] args, RemoteControlHandler handler) throws IOException {
		ParameterService.init();
		if (!getInstance().sendArgsToOtherInstanceIfUp(args)) {
			getInstance().installListener(handler);
			return true;
		} else {
			return false;
		}
	}
}
