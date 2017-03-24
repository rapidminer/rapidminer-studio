/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Policy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.rapidminer.FileProcessLocation;
import com.rapidminer.NoOpUserError;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.RapidMiner;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.autosave.AutoSave;
import com.rapidminer.gui.dialog.EULADialog;
import com.rapidminer.gui.docking.RapidDockableContainerFactory;
import com.rapidminer.gui.internal.GUIStartupListener;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.fc.BookmarkIO;
import com.rapidminer.gui.look.ui.RapidDockingUISettings;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.safemode.SafeMode;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.tools.logging.LogHandlerModel;
import com.rapidminer.gui.tools.logging.LogModel;
import com.rapidminer.gui.tools.logging.LogModelRegistry;
import com.rapidminer.gui.tools.logging.LogViewer;
import com.rapidminer.gui.viewer.MetaDataViewerTableModel;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.security.PluginSecurityManager;
import com.rapidminer.studio.io.data.internal.file.LocalFileDataSourceFactory;
import com.rapidminer.studio.io.data.internal.file.binary.BinaryDataSourceFactory;
import com.rapidminer.studio.io.data.internal.file.csv.CSVDataSourceFactory;
import com.rapidminer.studio.io.data.internal.file.excel.ExcelDataSourceFactory;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LaunchListener;
import com.rapidminer.tools.LaunchListener.RemoteControlHandler;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.update.internal.UpdateManagerRegistry;
import com.rapidminer.tools.usagestats.UsageStatistics;
import com.rapidminer.tools.usagestats.UsageStatsTransmissionDialog;
import com.vlsolutions.swing.docking.DockableContainerFactory;
import com.vlsolutions.swing.docking.ui.DockingUISettings;


/**
 * The main class if RapidMiner is started in GUI mode. This class keeps a reference to the
 * {@link MainFrame} and some other GUI relevant information and methods.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class RapidMinerGUI extends RapidMiner {

	public static final String PROPERTY_GEOMETRY_X = "rapidminer.gui.geometry.x";
	public static final String PROPERTY_GEOMETRY_Y = "rapidminer.gui.geometry.y";
	public static final String PROPERTY_GEOMETRY_EXTENDED_STATE = "rapidminer.gui.geometry.extendedstate";
	public static final String PROPERTY_GEOMETRY_WIDTH = "rapidminer.gui.geometry.width";
	public static final String PROPERTY_GEOMETRY_HEIGHT = "rapidminer.gui.geometry.height";
	public static final String PROPERTY_GEOMETRY_DIVIDER_MAIN = "rapidminer.gui.geometry.divider.main";
	public static final String PROPERTY_GEOMETRY_DIVIDER_EDITOR = "rapidminer.gui.geometry.divider.editor";;
	public static final String PROPERTY_GEOMETRY_DIVIDER_LOGGING = "rapidminer.gui.geometry.divider.logging";
	public static final String PROPERTY_GEOMETRY_DIVIDER_GROUPSELECTION = "rapidminer.gui.geometry.divider.groupselection";
	public static final String PROPERTY_EXPERT_MODE = "rapidminer.gui.expertmode";
	public static final String PROPERTY_SHOW_PARAMETER_HELP = "rapidminer.gui.show_parameter_help";

	// GUI Properties

	public static final String PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS = "rapidminer.gui.max_statistics_rows";
	public static final String PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS = "rapidminer.gui.max_sortable_rows";
	public static final String PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES = "rapidminer.gui.max_displayed_values";
	public static final String PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID = "rapidminer.gui.snap_to_grid";
	public static final String PROPERTY_AUTOWIRE_INPUT = "rapidminer.gui.autowire_input";
	public static final String PROPERTY_AUTOWIRE_OUTPUT = "rapidminer.gui.autowire_output";
	public static final String PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS = "rapidminer.gui.resolve_relative_repository_locations";
	public static final String PROPERTY_CLOSE_RESULTS_BEFORE_RUN = "rapidminer.gui.close_results_before_run";
	public static final String PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY = "rapidminer.gui.add_breakpoint_results_to_history";
	public static final String PROPERTY_CONFIRM_EXIT = "rapidminer.gui.confirm_exit";
	public static final String PROPERTY_RUN_REMOTE_NOW = "rapidminer.gui.run_process_on_rapidanalytics_now";
	public static final String PROPERTY_OPEN_IN_FILEBROWSER = "rapidminer.gui.entry_open_in_filebrowser";
	public static final String PROPERTY_CLOSE_ALL_RESULTS_NOW = "rapidminer.gui.close_all_results_without_confirmation";
	public static final String PROPERTY_FETCH_DATA_BASE_TABLES_NAMES = "rapidminer.gui.fetch_data_base_table_names";
	public static final String PROPERTY_DISCONNECT_ON_DISABLE = "rapidminer.gui.disconnect_on_disable";
	/** determines if a warning notification bubble is shown when no result ports are connected */
	public static final String PROPERTY_SHOW_NO_RESULT_WARNING = "rapidminer.gui.no_result_port_connected";

	public static final String PROPERTY_TRANSFER_USAGESTATS = "rapidminer.gui.transfer_usagestats";
	public static final String[] PROPERTY_TRANSFER_USAGESTATS_ANSWERS = { "ask", "always", "never" };

	public static final String PROPERTY_DRAG_TARGET_HIGHLIGHTING = "rapidminer.gui.drag_target_highlighting";
	public static final String[] PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES = { "full", "border", "none" };
	public static final int DRAG_TARGET_HIGHLIGHTING_FULL = 0;

	// Update Properties

	public static final String PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK = "rapidminer.update.purchased.not_installed.check";
	public static final String PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK = "rapidminer.update.check";

	static {

		// GUI Parameters

		RapidMiner.registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS, "", 1, Integer.MAX_VALUE, 100000));
		RapidMiner.registerParameter(
				new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS, "", 1, Integer.MAX_VALUE, 100000));
		RapidMiner.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GUI_MAX_DISPLAYED_VALUES, "", 1,
				Integer.MAX_VALUE, MetaDataViewerTableModel.DEFAULT_MAX_DISPLAYED_VALUES));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_INPUT, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_AUTOWIRE_OUTPUT, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS, "", true));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_CLOSE_RESULTS_BEFORE_RUN, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.TRUE));
		RapidMiner.registerParameter(
				new ParameterTypeBoolean(RapidMinerGUI.PROPERTY_ADD_BREAKPOINT_RESULTS_TO_HISTORY, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_CONFIRM_EXIT, "", false));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_OPEN_IN_FILEBROWSER, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));
		RapidMiner.registerParameter(new ParameterTypeCategory(PROPERTY_CLOSE_ALL_RESULTS_NOW, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.ASK));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_FETCH_DATA_BASE_TABLES_NAMES, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_DISCONNECT_ON_DISABLE, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_SHOW_NO_RESULT_WARNING, "", true));
		RapidMiner.registerParameter(new ParameterTypeCategory(RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS, "",
				RapidMinerGUI.PROPERTY_TRANSFER_USAGESTATS_ANSWERS, UsageStatsTransmissionDialog.ALWAYS));
		RapidMiner.registerParameter(new ParameterTypeCategory(RapidMinerGUI.PROPERTY_DRAG_TARGET_HIGHLIGHTING, "",
				PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES, DRAG_TARGET_HIGHLIGHTING_FULL));

		// GUI Parameters MainFrame

		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_MATRIXPLOT_SIZE, "", 1,
				Integer.MAX_VALUE, 200));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_ROWS_MAXIMUM, "", 1,
				Integer.MAX_VALUE, PlotterPanel.DEFAULT_MAX_NUMBER_OF_DATA_POINTS));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_DEFAULT_MAXIMUM, "", -1,
				Integer.MAX_VALUE, 100000));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_CLASSLIMIT, "",
				-1, Integer.MAX_VALUE, 10));
		RapidMiner.registerParameter(
				new ParameterTypeColor(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR, "", java.awt.Color.blue));
		RapidMiner.registerParameter(
				new ParameterTypeColor(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR, "", java.awt.Color.red));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT, "",
				-1, Integer.MAX_VALUE, 10));
		RapidMiner.registerParameter(
				new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE, "", 1, Integer.MAX_VALUE, 100));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_ATTRIBUTEEDITOR_ROWLIMIT, "", -1,
				Integer.MAX_VALUE, 50));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_BEEP_SUCCESS, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_BEEP_ERROR, "", false));
		RapidMiner.registerParameter(new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_BEEP_BREAKPOINT, "", false));
		RapidMiner.registerParameter(new ParameterTypeInt(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT, "", -1,
				Integer.MAX_VALUE, LogViewer.DEFAULT_LOG_ENTRY_NUMBER));
		RapidMiner.registerParameter(new ParameterTypeCategory(MainFrame.PROPERTY_RAPIDMINER_GUI_SAVE_BEFORE_RUN, "",
				DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.FALSE));
		RapidMiner.registerParameter(
				new ParameterTypeBoolean(MainFrame.PROPERTY_RAPIDMINER_GUI_SAVE_ON_PROCESS_CREATION, "", false));
		RapidMiner.registerParameter(new ParameterTypeCategory(MainFrame.PROPERTY_RAPIDMINER_GUI_AUTO_SWITCH_TO_RESULTVIEW,
				"", DecisionRememberingConfirmDialog.PROPERTY_VALUES, DecisionRememberingConfirmDialog.TRUE));
		RapidMiner.registerParameter(new ParameterTypeCategory(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL, "",
				LogViewer.SELECTABLE_LEVEL_NAMES, LogViewer.DEFAULT_LEVEL_INDEX));

		// Update Parameters
		RapidMiner.registerParameter(
				new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_PURCHASED_NOT_INSTALLED_CHECK, "", true));
		RapidMiner.registerParameter(new ParameterTypeBoolean(PROPERTY_RAPIDMINER_GUI_UPDATE_CHECK, "", true));

		// ---------- Load bundled fonts----------- -----------------
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Light.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans.ttf")));
			ge.registerFont(
					Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Semibold.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Bold.ttf")));
			ge.registerFont(
					Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-ExtraBold.ttf")));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.MainFrame.font_load_failed", e.getMessage());
		}
		// ----------------------------------------------------------
	}

	private static final int NUMBER_OF_RECENT_FILES = 8;

	private static MainFrame mainFrame;

	private static LinkedList<ProcessLocation> recentFiles = new LinkedList<>();

	private static SafeMode safeMode;

	private static LogModel defaultLogModel;

	private static List<GUIStartupListener> startupListener = new LinkedList<>();
	private static boolean startupStarted = false;

	private static AutoSave autosave = new AutoSave();

	/**
	 * This thread listens for System shutdown and cleans up after shutdown. This included saving
	 * the recent file list and other GUI properties.
	 */
	private static class ShutdownHook extends Thread {

		@Override
		public void run() {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.running_shutdown_sequence");
			RapidMinerGUI.saveRecentFileList();
			RapidMinerGUI.saveGUIProperties();
			UsageStatistics.getInstance().save();
			RepositoryManager.shutdown();
			UsageStatsTransmissionDialog.transmitOnShutdown();
		}
	}

	// private static UpdateManager updateManager = new CommunityUpdateManager();

	public synchronized void run(final String openLocation) throws Exception {
		startupStarted = true;

		// check if resources were copied
		URL logoURL = Tools.getResource("rapidminer_logo.png");
		if (logoURL == null) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.RapidMinerGUI.finding_resources_error");
			RapidMiner.quit(RapidMiner.ExitMode.ERROR);
		}

		// Initialize Docking UI -- important must be done as early as possible!
		DockingUISettings.setInstance(new RapidDockingUISettings());
		DockableContainerFactory.setFactory(new RapidDockableContainerFactory());

		// inform listeners that splash is about to be shown
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.splashWillBeShown();
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		RapidMiner.showSplash();

		RapidMiner.splashMessage("basic");

		// initialize RapidMiner
		// As side effect this also initialized the ConstraintManager
		// with the default product and constraints
		RapidMiner.init();

		// store (possibly new) active license (necessary, since no
		// ACTIVE_LICENSE_CHANGED event is fired on startup)
		LicenseManagerRegistry.INSTANCE.get().getAllActiveLicenses().forEach((l) -> {
			LicenseTools.storeActiveLicenseProperties(l);
		});

		// init logging GUI
		defaultLogModel = new LogHandlerModel(LogService.getRoot(),
				SwingTools.createIcon("16/" + I18N.getGUIMessage("gui.logging.default.icon")),
				I18N.getGUIMessage("gui.logging.default.label"), false);
		// set log level for our own log to the defined one
		String levelName = ParameterService.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL);
		Level logViewLevel = Level.INFO;
		if (levelName != null) {
			logViewLevel = Level.parse(levelName);
		}
		defaultLogModel.setLogLevel(logViewLevel);
		LogService.getRoot().setLevel(logViewLevel);
		LogModelRegistry.INSTANCE.register(defaultLogModel);

		RapidMiner.splashMessage("workspace");
		RapidMiner.splashMessage("plaf");
		setupToolTipManager();
		setupGUI();

		// check whether current EULA has been accepted
		if (!EULADialog.getEULAAccepted()) {
			// show EULA dialog
			RapidMiner.splashMessage("eula");
			SwingTools.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					EULADialog dialog = new EULADialog();
					dialog.setVisible(true);
				}
			});
		}

		RapidMiner.splashMessage("history");
		loadRecentFileList();

		RepositoryManager.getInstance(null).createRepositoryIfNoneIsDefined();

		RapidMiner.splashMessage("create_frame");

		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				setMainFrame(new MainFrame());
			}
		});
		RapidMiner.splashMessage("gui_properties");
		loadGUIProperties(mainFrame);

		RapidMiner.splashMessage("plugin_gui");
		Plugin.initPluginGuis(mainFrame);

		// only load the perspective here after all plugins have registered their dockables
		// otherwise you may end up breaking the perspective and VLDocking exists in invalid states
		// and explodes into the smoking piece of crap it is
		mainFrame.getPerspectiveController().showPerspective(PerspectiveModel.DESIGN);

		autosave.init();

		// inform listeners that the Mainframe was initialized
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.mainFrameInitialized(mainFrame);
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				getMainFrame().finishInitialization();
			}
		});

		RapidMiner.splashMessage("show_frame");

		mainFrame.setVisible(true);

		RapidMiner.splashMessage("checks");
		Plugin.initFinalChecks();

		RapidMiner.splashMessage("ready");

		// set log model back to default log of Studio
		// otherwise the last extension log is active
		mainFrame.getLogViewer().getLogSelectionModel().setSelectedLogModel(RapidMinerGUI.getDefaultLogModel());

		RapidMiner.hideSplash();

		// inform listeners that the Splash screen was hidden
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.splashWasHidden();
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		UsageStatsTransmissionDialog.init();

		if (openLocation != null) {
			if (!RepositoryLocation.isAbsolute(openLocation)) {
				SwingTools.showVerySimpleErrorMessage("malformed_repository_location", openLocation);
			} else {
				OpenAction.open(openLocation, false);
			}
		}

		// check for updates
		Plugin.initPluginUpdateManager();

		// inform listeners that GUI startup has finished
		for (GUIStartupListener sl : startupListener) {
			try {
				sl.startupCompleted();
			} catch (RuntimeException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.startup_listener_error", e);
			}
		}

		// register data source factories
		DataSourceFactoryRegistry.INSTANCE.register(new LocalFileDataSourceFactory());

		// Register file data source factories: binary data source factory is registered first,
		// since it is the first choice for unknown formats.
		DataSourceFactoryRegistry.INSTANCE.register(new BinaryDataSourceFactory());
		DataSourceFactoryRegistry.INSTANCE.register(new ExcelDataSourceFactory());
		DataSourceFactoryRegistry.INSTANCE.register(new CSVDataSourceFactory());
	}

	private void setupToolTipManager() {
		// setup tool tip text manager
		ToolTipManager manager = ToolTipManager.sharedInstance();
		manager.setDismissDelay(25000); // original: 4000
		manager.setInitialDelay(1500);   // original: 750
		manager.setReshowDelay(50);    // original: 500
	}

	/**
	 * This default implementation only setup the tool tip durations. Subclasses might override this
	 * method.
	 */
	protected void setupGUI() throws NoOpUserError {
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_DIR, FileSystemService.getUserRapidMinerDir().getAbsolutePath());
		System.setProperty(BookmarkIO.PROPERTY_BOOKMARKS_FILE, ".bookmarks");

		try {
			if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
				// to support OS Xs menu bar shown in the OS X menu bar,
				// we have to load the default system look and feel
				// to exchange the MenuBarUI from RapidLookAndFeel with the
				// default OS X look and feel UI class.
				// See here for more information:
				// http://www.pushing-pixels.org/2008/07/13/swing-applications-and-mac-os-x-menu-bar.html
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				Map<String, Object> macUIDefaults = new HashMap<>();
				macUIDefaults.put("MenuBarUI", UIManager.get("MenuBarUI"));
				UIManager.setLookAndFeel(new RapidLookAndFeel(macUIDefaults));
			} else {
				UIManager.setLookAndFeel(new RapidLookAndFeel());
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.RapidMinerGUI.setting_up_modern_look_and_feel_error"), e);
		}
	}

	public static void setMainFrame(final MainFrame mf) {
		mainFrame = mf;
	}

	public static MainFrame getMainFrame() {
		return mainFrame;
	}

	/**
	 * Checks whether the repository location belongs to the currently loaded process.
	 *
	 * @param location
	 *            Repository location to check.
	 * @return true if the process belonging to this entry is currently presented on the GUI, false
	 *         otherwise.
	 */
	public static boolean isMainFrameProcessLocation(RepositoryLocation location) {
		if (getMainFrame() != null && getMainFrame().getProcess() != null) {
			RepositoryLocation currentProcessLocation = getMainFrame().getProcess().getRepositoryLocation();
			if (location.equals(currentProcessLocation)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Resets the location of the MainFrame process without reloading it. Use only if the currently
	 * displayed process is stored in repository, and got renamed or moved.
	 *
	 * @param repositoryLocation
	 *            The new process location.
	 */
	public static void resetProcessLocation(RepositoryProcessLocation repositoryLocation) {
		getMainFrame().getProcess().setProcessLocation(repositoryLocation);
		getMainFrame().setTitle();
		addToRecentFiles(repositoryLocation);
		getMainFrame().updateRecentFileList();
	}

	public static void useProcessFile(final Process process) {
		ProcessLocation location = process.getProcessLocation();
		addToRecentFiles(location);
	}

	public static void addToRecentFiles(final ProcessLocation location) {
		if (location != null) {
			while (recentFiles.contains(location)) {
				recentFiles.remove(location);
			}
			recentFiles.addFirst(location);
			while (recentFiles.size() > NUMBER_OF_RECENT_FILES) {
				recentFiles.removeLast();
			}
			saveRecentFileList();
		}
	}

	public static List<ProcessLocation> getRecentFiles() {
		return recentFiles;
	}

	/**
	 * Returns the default {@link LogModel} for RapidMiner Studio.
	 *
	 * @return
	 */
	public static LogModel getDefaultLogModel() {
		return defaultLogModel;
	}

	/**
	 * @return the object that handles autosave information
	 */
	public static AutoSave getAutoSave() {
		return autosave;
	}

	private static void loadRecentFileList() {
		File file = FileSystemService.getUserConfigFile("history");
		if (!file.exists()) {
			return;
		}
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			recentFiles.clear();
			String line = null;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("file ")) {
					recentFiles.add(new FileProcessLocation(new File(line.substring(5))));
				} else if (line.startsWith("repository ")) {
					try {
						recentFiles.add(new RepositoryProcessLocation(new RepositoryLocation(line.substring(11))));
					} catch (MalformedRepositoryLocationException e) {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unparseable_line", line);
					}
				} else {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unparseable_line", line);
				}
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.gui.RapidMinerGUI.reading_history_file_error"), e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.RapidMinerGUI.reading_history_file_error"), e);
				}
			}
		}
	}

	private static void saveRecentFileList() {
		File file = FileSystemService.getUserConfigFile("history");
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			for (ProcessLocation loc : recentFiles) {
				out.println(loc.toHistoryFileString());
			}
		} catch (IOException e) {
			SwingTools.showSimpleErrorMessage("cannot_write_history_file", e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private static void saveGUIProperties() {
		Properties properties = new Properties();
		MainFrame mainFrame = getMainFrame();
		if (mainFrame != null) {
			properties.setProperty(PROPERTY_GEOMETRY_X, "" + (int) mainFrame.getLocation().getX());
			properties.setProperty(PROPERTY_GEOMETRY_Y, "" + (int) mainFrame.getLocation().getY());
			properties.setProperty(PROPERTY_GEOMETRY_WIDTH, "" + mainFrame.getWidth());
			properties.setProperty(PROPERTY_GEOMETRY_HEIGHT, "" + mainFrame.getHeight());
			properties.setProperty(PROPERTY_GEOMETRY_EXTENDED_STATE, "" + mainFrame.getExtendedState());
			properties.setProperty(PROPERTY_SHOW_PARAMETER_HELP, "" + mainFrame.getPropertyPanel().isShowParameterHelp());
			properties.setProperty(PROPERTY_EXPERT_MODE, "" + mainFrame.getPropertyPanel().isExpertMode());
			File file = FileSystemService.getUserConfigFile("gui.properties");
			OutputStream out = null;
			try {
				out = new FileOutputStream(file);
				properties.store(out, "RapidMiner Studio GUI properties");
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.gui.RapidMinerGUI.writing_gui_properties_error", e.getMessage()), e);
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
				}
			}
			mainFrame.getResultDisplay().clearAll();
			// mainFrame.getApplicationPerspectiveController().saveAll();
			mainFrame.getPerspectiveController().saveAll();
		}
	}

	private static void loadGUIProperties(final MainFrame mainFrame) {
		Properties properties = new Properties();
		File file = FileSystemService.getUserConfigFile("gui.properties");
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				properties.load(in);
			} catch (IOException e) {
				setDefaultGUIProperties();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					throw new Error(e); // should not occur
				}
			}
			try {
				mainFrame.setLocation(Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_X)),
						Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_Y)));
				mainFrame.setSize(new Dimension(Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_WIDTH)),
						Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_HEIGHT))));
				int extendedState;
				if (properties.getProperty(PROPERTY_GEOMETRY_EXTENDED_STATE) == null) {
					extendedState = Frame.NORMAL;
				} else {
					extendedState = Integer.parseInt(properties.getProperty(PROPERTY_GEOMETRY_EXTENDED_STATE));
				}
				mainFrame.setExtendedState(extendedState);
				mainFrame.getPropertyPanel()
						.setExpertMode(Boolean.valueOf(properties.getProperty(PROPERTY_EXPERT_MODE)).booleanValue());

				// If the property is not set we want the parameter help to be shown
				String showHelpProperty = properties.getProperty(PROPERTY_SHOW_PARAMETER_HELP);
				if (showHelpProperty == null || showHelpProperty.isEmpty()) {
					mainFrame.getPropertyPanel().setShowParameterHelp(true);
				} else {
					mainFrame.getPropertyPanel().setShowParameterHelp(Boolean.valueOf(showHelpProperty).booleanValue());

				}
			} catch (NumberFormatException e) {
				setDefaultGUIProperties();
			}
		} else {
			setDefaultGUIProperties();
		}
	}

	/**
	 * This method sets some default GUI properties. This method can be invoked if the properties
	 * file was not found or produced any error messages (which might happen after version changes).
	 */
	private static void setDefaultGUIProperties() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		mainFrame.setLocation((int) (0.05d * screenSize.getWidth()), (int) (0.05d * screenSize.getHeight()));
		mainFrame.setSize((int) (0.9d * screenSize.getWidth()), (int) (0.9d * screenSize.getHeight()));
		mainFrame.getPropertyPanel().setExpertMode(false);
	}

	public static void main(String[] args) throws Exception {
		Policy.setPolicy(new PluginSandboxPolicy());
		System.setSecurityManager(new PluginSecurityManager());

		RapidMiner.addShutdownHook(new ShutdownHook());
		setExecutionMode(
				System.getProperty(PROPERTY_HOME_REPOSITORY_URL) == null ? ExecutionMode.UI : ExecutionMode.WEBSTART);

		boolean shouldLaunch = true;
		if (!LaunchListener.defaultLaunchWithArguments(args, new RemoteControlHandler() {

			@Override
			public boolean handleArguments(final String[] args) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.RapidMinerGUI.received_message",
						Arrays.toString(args));
				mainFrame.requestFocus();
				if (args.length >= 1) {
					String arg = args[0];
					if (arg.startsWith(RapidMiner.RAPIDMINER_URL_PREFIX)) {
						handleRapidMinerURL(args[0]);
					} else if (!args[0].trim().isEmpty()) {
						OpenAction.open(args[0], false);
					}
				}

				return true;
			}
		})) {
			// always start if there are no args so user can start new Studio instances at will
			shouldLaunch = args.length == 0;
		}

		if (shouldLaunch) {
			safeMode = new SafeMode();
			safeMode.launchStarts();
			launch(args);
			safeMode.launchComplete();
		} else {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.gui.RapidMinerGUI.other_instance_up");
		}
	}

	private static void launch(final String[] args) throws Exception {
		String openLocation = null;
		String rapidminerURL = null;

		if (args.length > 0) {
			if (args.length != 1) {
				System.out.println("java " + RapidMinerGUI.class.getName() + " [processfile]");
				return;
			}
			if (args[0].startsWith(RapidMiner.RAPIDMINER_URL_PREFIX)) {
				rapidminerURL = args[0];
			} else if (!args[0].trim().isEmpty()) {
				openLocation = args[0];
			}
		}
		RapidMiner.setInputHandler(new GUIInputHandler());

		new RapidMinerGUI().run(openLocation);
		if (rapidminerURL != null) {
			handleRapidMinerURL(rapidminerURL);
		}
	}

	/**
	 * @return the safeMode
	 */
	public static SafeMode getSafeMode() {
		return safeMode;
	}

	public enum DragHighlightMode {
		FULL, BORDER, NONE
	}

	public static DragHighlightMode getDragHighlighteMode() {
		String dragParameter = ParameterService.getParameterValue(PROPERTY_DRAG_TARGET_HIGHLIGHTING);
		if (dragParameter.equals(PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES[0])) {
			return DragHighlightMode.FULL;
		} else if (dragParameter.equals(PROPERTY_DRAG_TARGET_HIGHLIGHTING_VALUES[1])) {
			return DragHighlightMode.BORDER;
		} else {
			return DragHighlightMode.NONE;
		}
	}

	public static Color getBodyHighlightColor() {
		return new Color(255, 255, 242);
	}

	public static Color getBorderHighlightColor() {
		return SwingTools.RAPIDMINER_ORANGE;
	}

	/**
	 * Called with rapidminer:// URLs passed as command line arguments. Currently the only supported
	 * pattern is rapidminer://extension/{extensionkey} to install an extension with the given name.
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

		if (!"rapidminer".equals(url.getScheme())) {
			// in this case we should not be called, so throw
			throw new IllegalArgumentException("Can handle only " + RapidMiner.RAPIDMINER_URL_PREFIX + " URLs!");
		}
		String path = url.getPath();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		String components[] = path.split("/");
		if (components.length < 2) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unknown_rapidminer_url",
					new Object[] { urlStr });
			return;
		}
		switch (components[0]) {
			case "extension":
				String extensionKey = components[1];
				// asynchronous
				try {
					UpdateManagerRegistry.INSTANCE.get().showUpdateDialog(false, extensionKey);
				} catch (URISyntaxException | IOException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.RapidMinerGUI.error_connecting_to_updateserver", e);
				}
				break;
			default:
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.RapidMinerGUI.unknown_rapidminer_url",
						new Object[] { urlStr });
				return;
		}
	}

	/**
	 * Register a {@link GUIStartupListener}.
	 *
	 * @param listener
	 *            the listener to register
	 * @throws IllegalStateException
	 *             if the startup process has already been started
	 */
	public static synchronized void registerStartupListener(GUIStartupListener listener) {
		if (startupStarted) {
			throw new IllegalStateException("Cannot register a Startup listener after startup has been started");
		}
		startupListener.add(listener);
	}
}
