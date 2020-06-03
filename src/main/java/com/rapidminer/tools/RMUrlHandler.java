/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.OperatorDocumentationBrowser;
import com.rapidminer.gui.Perspective;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.dialog.BrowserUnavailableDialogFactory;
import com.rapidminer.gui.security.PasswordManager;
import com.rapidminer.gui.tools.DockingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.tools.update.internal.UpdateManagerRegistry;


/**
 * Convenience class for invoking certain actions from URLs, e.g. from inside a
 * {@link HyperlinkListener}. URLs of the form "rm://actionName" will be interpreted. Actions can
 * register themselves by invoking {@link RMUrlHandler#register(String, Action)}.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class RMUrlHandler {

	/** internal rm:// url, e.g. for triggering actions */
	public static final String URL_PREFIX = "rm://";

	/**
	 * @deprecated not used. Deprecated in 9.0, scheduled to be removed with 10.0
	 */
	@Deprecated
	public static final String PREFERENCES_URL = URL_PREFIX + "preferences";

	public static final String ACTION_MANAGE_DB_CONNECTIONS = "manage_db_connections";
	public static final String ACTION_MANAGE_DB_DRIVERS = "manage_db_drivers";
	public static final String ACTION_MANAGE_BUILDING_BLOCKS = "manage_building_blocks";
	public static final String ACTION_RUN_PROCESS_BACKGROUND = "run_process_background";
	public static final String ACTION_RUN_PROCESS_RMSERVER_NOW = "run_process_rmserver_now";
	public static final String ACTION_RUN_PROCESS_RMSERVER_SCHEDULE = "run_process_rmserver_schedule";
	public static final String ACTION_CONNECT_TO_PROJECT = "connect_to_project";
	public static final String ACTION_MARKETPLACE = "marketplace";
	/** @since 9.5.0 */
	public static final String ACTION_MARKETPLACE_UPDATES = "marketplace-updates";
	public static final String ACTION_MANAGE_EXTENSIONS = "manage_extensions";
	public static final String ACTION_MANAGE_LICENSES = "manage_licenses";

	/** use together with the appended name of the view you want to switch to */
	private static final String ACTION_VIEW_NEW = "new_view";
	private static final String ACTION_VIEW_RESTORE = "restore_view";
	private static final String ACTION_VIEW_OPEN_PREFIX = "open_view_";
	private static final String ACTION_STOP_PROCESS = "stop_process";
	private static final String ACTION_RUN_PROCESS = "run_process";
	private static final String ACTION_MANAGE_PASSWORDS = "manage_passwords";
	private static final String ACTION_MANAGE_CONFIGURABLES = "manage_configurables";
	private static final String ACTION_SETTINGS = "preferences";
	private static final String ACTION_TUTORIALS = "tutorials";
	private static final String ACTION_ABOUT = "about";
	private static final String ACTION_PROCESS_NEW = "process_new";
	private static final String ACTION_PROCESS_OPEN = "process_open";
	private static final String ACTION_PROCESS_SAVE = "process_save";
	private static final String ACTION_PROCESS_SAVE_AS = "process_save_as";
	private static final String ACTION_PROCESS_IMPORT = "process_import";
	private static final String ACTION_PROCESS_EXPORT = "process_export";
	private static final String ACTION_IMPORT_DATA = "import_data";
	private static final String ACTION_ADD_REPOSITORY = "add_repository";
	private static final String ACTION_AUTOWIRE = "autowire";
	private static final String ACTION_UNDO = "undo";
	private static final String ACTION_REDO = "redo";
	private static final String ACTION_SCREEN_EXPORT = "screen_export";

	private static final Map<String, Action> ACTION_MAP = new HashMap<>();

	private static final Logger LOGGER = Logger.getLogger(RMUrlHandler.class.getCanonicalName());
	private static final String SCHEMA_RAPIDMINER = "rapidminer";
	private static final String RAPIDMINER_SCHEMA_EXTENSION = "extension";
	private static final String RAPIDMINER_SCHEMA_REPOSITORY = "repository";
	private static final String RAPIDMINER_SCHEMA_OPERATOR_TUTORIAL_PROCESS = "operator_tutorial_process";
	private static final String RAPIDMINER_SCHEMA_PROCESS_URL = "process_url";
	private static final String RAPIDMINER_SCHEMA_CONNECT_TO_PROJECT = "project_url";
	private static final String INTERNAL_SCHEMA_OPDOC = "opdoc/";
	private static final String INTERNAL_SCHEMA_OPERATOR = "operator/";
	private static final String SCHEMA_HTTP = "http://";
	private static final String SCHEMA_HTTPS = "https://";


	/**
	 * Inits all actions. Has no effect if called more than once.
	 *
	 * @since 9.0.0
	 */
	public static void initActions() {
		register(ACTION_PROCESS_NEW, RapidMinerGUI.getMainFrame().NEW_ACTION);
		register(ACTION_PROCESS_OPEN, RapidMinerGUI.getMainFrame().OPEN_ACTION);
		register(ACTION_PROCESS_SAVE, RapidMinerGUI.getMainFrame().SAVE_ACTION);
		register(ACTION_PROCESS_SAVE_AS, RapidMinerGUI.getMainFrame().SAVE_AS_ACTION);
		register(ACTION_IMPORT_DATA, RapidMinerGUI.getMainFrame().IMPORT_DATA_ACTION);
		register(ACTION_PROCESS_IMPORT, RapidMinerGUI.getMainFrame().IMPORT_PROCESS_ACTION);
		register(ACTION_PROCESS_EXPORT, RapidMinerGUI.getMainFrame().EXPORT_PROCESS_ACTION);
		register(ACTION_UNDO, RapidMinerGUI.getMainFrame().UNDO_ACTION);
		register(ACTION_REDO, RapidMinerGUI.getMainFrame().REDO_ACTION);
		register(ACTION_RUN_PROCESS, RapidMinerGUI.getMainFrame().RUN_ACTION);
		register(ACTION_STOP_PROCESS, RapidMinerGUI.getMainFrame().STOP_ACTION);
		register(ACTION_AUTOWIRE, RapidMinerGUI.getMainFrame().AUTO_WIRE);
		register(ACTION_VIEW_NEW, RapidMinerGUI.getMainFrame().NEW_PERSPECTIVE_ACTION);
		register(ACTION_MANAGE_CONFIGURABLES, RapidMinerGUI.getMainFrame().MANAGE_CONFIGURABLES_ACTION);
		register(ACTION_SCREEN_EXPORT, RapidMinerGUI.getMainFrame().EXPORT_ACTION);
		register(ACTION_SETTINGS, RapidMinerGUI.getMainFrame().SETTINGS_ACTION);
		register(ACTION_VIEW_RESTORE, RapidMinerGUI.getMainFrame().RESTORE_PERSPECTIVE_ACTION);
		register(ACTION_TUTORIALS, RapidMinerGUI.getMainFrame().TUTORIAL_ACTION);
		register(ACTION_ABOUT, RapidMinerGUI.getMainFrame().ABOUT_ACTION);
		register(ACTION_MANAGE_PASSWORDS, PasswordManager.OPEN_WINDOW);
		register(ACTION_ADD_REPOSITORY, RepositoryBrowser.ADD_REPOSITORY_ACTION);

		// add view switch actions
		for (Perspective perspective : RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getAllPerspectives()) {
			if (!perspective.isUserDefined()) {
				register(ACTION_VIEW_OPEN_PREFIX + perspective.getName(), RapidMinerGUI.getMainFrame().getPerspectiveController().createPerspectiveAction(perspective));
			}
		}
	}

	/**
	 * Handles the given url string. Can understand urls like "https://" etc, but also RapidMiner specific urls like
	 * "rapidminer:///extension/extension_id" and rm://{action}. Action names are only available since Studio 9.0!
	 *
	 * @return true iff we understand the url
	 */
	public static boolean handleUrl(String url) {
		if (url.startsWith(SCHEMA_HTTP) || url.startsWith(SCHEMA_HTTPS)) {
			openInBrowser(url);
			return true;
		} else if (url.startsWith(RapidMiner.RAPIDMINER_URL_PREFIX)) {
			handleRapidMinerURL(url);
			return true;
		} else if (url.startsWith(URL_PREFIX)) {
			handleRMUrl(url);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Registers the given action under the given name as a URL handler. The URL of the format {@code rm://name} can
	 * then be passed to {@link #handleUrl(String)}, which then triggers the registered action.
	 *
	 * @param name
	 * 		the name under which the action can be called again
	 * @param action
	 * 		the action to trigger when a url of the format {@code rm://name}  is passed to {@link #handleUrl(String)}
	 */
	public static void register(String name, Action action) {
		ACTION_MAP.put(name, action);
	}

	/**
	 * On systems where Desktop.getDesktop().browse() does not work for http://, creates an HTML
	 * page which redirects to the given URI and calls Desktop.browse() with this file through the
	 * file:// url which seems to work better, at least for KDE.
	 *
	 * @deprecated Replaced by {@link #openInBrowser(URI)}
	 */
	@Deprecated
	public static void browse(URI uri) throws IOException {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {
				File tempFile = TempFileTools.createTempFile("rmredirect", ".html").toFile();

				try (FileWriter out = new FileWriter(tempFile)) {
					out.write(String.format(
							"<!DOCTYPE html>\n"
									+ "<html><meta http-equiv=\"refresh\" content=\"0; URL=%s\"><body>You are redirected to %s</body></html>",
							uri.toString(), uri.toString()));
				}
				Desktop.getDesktop().browse(tempFile.toURI());
			} catch (UnsupportedOperationException e1) {
				showBrowserUnavailableMessage(uri.toString());
			}
		} else {
			showBrowserUnavailableMessage(uri.toString());
		}

	}

	/**
	 * Tries to open an URL in the system browser
	 * <p>
	 * Fallback: Shows the URL in a dialog
	 * </p>
	 *
	 * @param url
	 * 		the url to open
	 */
	public static void openInBrowser(URL url) {
		try {
			URI uri = url.toURI();
			openInBrowser(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL: " + url.toString(), e);
		}
	}

	/**
	 * Tries to open an URI String in the system browser
	 * <p>
	 * Fallback: Shows the URI String in a dialog
	 * </p>
	 *
	 * @param uriString
	 * 		the uri as a string
	 */
	public static void openInBrowser(String uriString) {
		try {
			URI uri = new URI(uriString);
			openInBrowser(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URI String: " + uriString, e);
		}
	}

	/**
	 * Tries to open an URI in the system browser
	 * <p>
	 * Fallback: Shows the URI String in a dialog
	 * </p>
	 */
	public static void openInBrowser(URI uri) {
		if (uri == null) {
			return;
		}
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {
				openInBrowserWithTempFile(uri);
			}
		} else {
			showBrowserUnavailableMessage(uri.toString());
		}

	}

	/**
	 * Browse the uri using a local temp file
	 * <p>
	 * On systems where Desktop.getDesktop().browse() does not work for http://, creates an HTML page which redirects to
	 * the given URI and calls Desktop.browse() with this file through the file:// url which seems to work better, at
	 * least for KDE.
	 * </p>
	 *
	 * @param uri
	 * 		the uri to open
	 */
	private static void openInBrowserWithTempFile(URI uri) {
		try {
			File tempFile = TempFileTools.createTempFile("rmredirect", ".html").toFile();
			try (FileWriter out = new FileWriter(tempFile)) {
				out.write(String.format(
						"<!DOCTYPE html>\n"
								+ "<html><meta http-equiv=\"refresh\" content=\"0; URL=%s\"><body>You are redirected to %s</body></html>",
						uri.toString(), uri.toString()));
				Desktop.getDesktop().browse(tempFile.toURI());
			}
		} catch (IOException e) {
			showBrowserUnavailableMessage(uri.toString());
		}
	}

	/**
	 * Displays the uri in a {@link ButtonDialog}
	 *
	 * @param uri
	 * 		the uri which could not be displayed
	 */
	private static void showBrowserUnavailableMessage(String uri) {
		ButtonDialog dialog = BrowserUnavailableDialogFactory.createNewDialog(uri);
		dialog.setVisible(true);
		LOGGER.log(Level.SEVERE, "Failed to open web page in browser, browsing is not supported on this platform.");
	}

	/**
	 * Handles rm:// urls. Supported patterns are {@value #INTERNAL_SCHEMA_OPDOC}, {@value #INTERNAL_SCHEMA_OPERATOR}
	 * and registered actions.
	 *
	 * @param url
	 * 		the url starting with rm://
	 * @see #initActions()
	 * @see #register(String, Action)
	 */
	private static void handleRMUrl(String url) {
		String suffix = url.substring(URL_PREFIX.length());

		if (suffix.startsWith(INTERNAL_SCHEMA_OPDOC)) {
			// operator doc display change
			String opName = suffix.substring(INTERNAL_SCHEMA_OPDOC.length());
			try {
				RapidMinerGUI.getMainFrame().getOperatorDocViewer().setDisplayedOperator(OperatorService.createOperator(opName));
				DockingTools.openDockable(OperatorDocumentationBrowser.OPERATOR_HELP_DOCK_KEY);
			} catch (OperatorCreationException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.RMUrlHandler.creating_operator_error", opName), e);
			}
		} else if (suffix.startsWith(INTERNAL_SCHEMA_OPERATOR)) {
			// operator selection
			String opName = suffix.substring(INTERNAL_SCHEMA_OPERATOR.length());
			MainFrame mainFrame = RapidMinerGUI.getMainFrame();
			mainFrame.selectAndShowOperator(mainFrame.getProcess().getOperator(opName), true);
		} else {
			// special case for connect to project
			// example: rm://connect_to_project/http:||192.168.1.242:8080|executions|git|studio-test.git
			if (suffix.startsWith(ACTION_CONNECT_TO_PROJECT)) {
				int furtherSlashIndex = suffix.indexOf('/');
				String escapedProjectUrl = null;
				if (furtherSlashIndex > 0 && suffix.length() > furtherSlashIndex) {
					escapedProjectUrl = suffix.substring(furtherSlashIndex + 1);
				}
				executeConnectToProjectAction(escapedProjectUrl);
			} else {
				// try if an action is registered under than name and trigger it
				Action action = ACTION_MAP.get(suffix);
				if (action != null) {

					action.actionPerformed(null);
				} else {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.RMUrlHandler.no_action_associated_with_url", url);
				}

			}
		}
	}

	/**
	 * Called with {@link #SCHEMA_RAPIDMINER rapidminer://} URLs passed as command line arguments. Currently the only supported
	 * patterns are {@link #RAPIDMINER_SCHEMA_EXTENSION rapidminer://extension/&#123;extensionId&#125;} to install an extension
	 * with the given name and {@link #RAPIDMINER_SCHEMA_REPOSITORY rapidminer://repository/&#123;repository_path&#125;}
	 * to open data from a repository path. The path has to escape "/" with ":".
	 *
	 * This method just logs a warning method if it cannot handle the rapidminer:// URL for
	 * syntactical reasons.
	 *
	 * @throws IllegalArgumentException
	 *             if this is not a rapidminer:// url
	 */
	private static void handleRapidMinerURL(String urlStr) {
		URI url;

		try {
			url = new URI(urlStr);
		} catch (URISyntaxException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.malformed_rapidminer_url",
					new Object[] { urlStr, e.getMessage() });
			return;
		}

		if (!SCHEMA_RAPIDMINER.equals(url.getScheme())) {
			// in this case we should not be called, so throw
			throw new IllegalArgumentException("Can handle only " + RapidMiner.RAPIDMINER_URL_PREFIX + " URLs!");
		}
		String path = url.getPath();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (url.getAuthority() != null) {
			path = url.getAuthority() + "/" + path;
		}
		String[] components = path.split("/");
		if (components.length < 2) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unknown_rapidminer_url",
					new Object[] { urlStr });
			return;
		}
		switch (components[0]) {
			case RAPIDMINER_SCHEMA_EXTENSION:
				String extensionKey = components[1];
				// asynchronous
				try {
					UpdateManagerRegistry.INSTANCE.get().showUpdateDialog(false, extensionKey);
				} catch (URISyntaxException | IOException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.RapidMinerGUI.error_connecting_to_updateserver", e);
				}
				break;
			case RAPIDMINER_SCHEMA_REPOSITORY:
				String locString = components[1].replaceAll(":", "/");
				try {
					// in case of duplicates (should not happen), open first
					final RepositoryLocation location = new RepositoryLocationBuilder().withExpectedDataEntryType(DataEntry.class).buildFromAbsoluteLocation(locString);
					DataEntry entry = location.locateData();
					// don't open binary entries this way, security risk (quickly execute a .cmd or .py script or similar
					if (entry instanceof BinaryEntry) {
						LogService.getRoot().log(Level.INFO, "Not opening binary entry via rapidminer:// url: " + locString);
						return;
					}
					OpenAction.open(entry, true);
				} catch (RepositoryException | MalformedRepositoryLocationException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.rapidminer_url_repo_broken_location",
							new Object[] { locString });
				}
				break;
			case RAPIDMINER_SCHEMA_OPERATOR_TUTORIAL_PROCESS:
				try {
					int lastColumnPos = components[1].lastIndexOf(':');
					String operatorKey = components[1].substring(0, lastColumnPos);
					int tutorialPos = Integer.parseInt(components[1].substring(lastColumnPos + 1));
					OperatorDocumentationBrowser.openTutorialProcess(operatorKey, tutorialPos);
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unknown_rapidminer_url",
							new Object[]{urlStr});
				}
				break;
			case RAPIDMINER_SCHEMA_PROCESS_URL:
				String webUrlString = components[1];
				if (webUrlString.contains("|")) {
					// VERSION 9.7+:
					// example: rapidminer://process_url/https:||s3.amazonaws.com|rapidminer.community|XML|61083.xml
					// example: rapidminer://process_url/https:||myServer.com:8080|path|file.xml
					// urls must use pipe instead of slashes since we split along the slashes above
					locString = webUrlString.toLowerCase(Locale.ENGLISH).replaceAll("\\|", "/");
				} else {
					// PRE-VERSION 9.7:
					// example: rapidminer://process_url/s3.amazonaws.com:rapidminer.community:XML:61083.xml
					// urls must use colon instead of slashes since we split along the slashes above, for that reason we
					// also cannot accept a url with the schema but https:// will be added in front automatically
					locString = SCHEMA_HTTPS + webUrlString.replaceAll(":", "/");
				}
				URL processURL = null;
				try {
					processURL = new URL(locString);
				} catch (MalformedURLException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.malformed_rapidminer_url",
							new Object[] { urlStr, e.getMessage() });
				}
				if (processURL != null) {
					try {
						Process p = new Process(processURL, null);
						// ensure this is tracked in UsageStats by setting the process origin
						ProcessOriginProcessXMLFilter.setProcessOriginState(p, ProcessOriginProcessXMLFilter.ProcessOriginState.WEB_URL);
						if (RapidMinerGUI.getMainFrame().close()) {
							SwingUtilities.invokeLater(() -> RapidMinerGUI.getMainFrame().setOpenedProcess(p));
						}
					} catch (IOException | XMLException | RuntimeException e) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.url_process_failed", new Object[] { urlStr });
					}
				}
				break;
				case RAPIDMINER_SCHEMA_CONNECT_TO_PROJECT:
					// example: rapidminer://project_url/http:||www.myserver.com:8080|executions|git|my-git-repo.git
					// example: rapidminer://project_url/https:||www.myserver.com:8080|executions|git|my-git-repo.git
					// if the url does not start with http or https, it is discarded in the action later
					// action is registered in PluginInitRemoteRepository of the Remote Repository Extension
					executeConnectToProjectAction(components[1]);
					break;
			default:
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unknown_rapidminer_url",
						new Object[] { urlStr });
		}
	}

	/**
	 * Execute the action to connect to a project.
	 *
	 * @param escapedProjectUrl the project url, or {@code null} if nothing should be pre-filled
	 */
	private static void executeConnectToProjectAction(String escapedProjectUrl) {
		if (escapedProjectUrl != null) {
			escapedProjectUrl = escapedProjectUrl.toLowerCase(Locale.ENGLISH).replaceAll("\\|", "/");
		} else {
			// (Swing returns the swing component getText() content if passing null as action command, so make it an empty string...)
			escapedProjectUrl = "";
		}
		Action connectToProjectAction = ACTION_MAP.get(ACTION_CONNECT_TO_PROJECT);
		if (connectToProjectAction != null) {
			connectToProjectAction.actionPerformed(new ActionEvent(new Object(), -1, escapedProjectUrl));
		} else {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.RMUrlHandler.connect_to_project_not_supported");
		}
	}
}