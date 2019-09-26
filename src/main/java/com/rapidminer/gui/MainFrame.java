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
package com.rapidminer.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.ProcessStorageListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.actions.AboutAction;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.AutoWireAction;
import com.rapidminer.gui.actions.BrowseAction;
import com.rapidminer.gui.actions.CreateConnectionAction;
import com.rapidminer.gui.actions.ExitAction;
import com.rapidminer.gui.actions.ExportProcessAction;
import com.rapidminer.gui.actions.ImportDataAction;
import com.rapidminer.gui.actions.ImportProcessAction;
import com.rapidminer.gui.actions.ManageConfigurablesAction;
import com.rapidminer.gui.actions.NewPerspectiveAction;
import com.rapidminer.gui.actions.PauseAction;
import com.rapidminer.gui.actions.PropagateRealMetaDataAction;
import com.rapidminer.gui.actions.RedoAction;
import com.rapidminer.gui.actions.RestoreDefaultPerspectiveAction;
import com.rapidminer.gui.actions.RunAction;
import com.rapidminer.gui.actions.SaveAction;
import com.rapidminer.gui.actions.SaveAsAction;
import com.rapidminer.gui.actions.SettingsAction;
import com.rapidminer.gui.actions.StopAction;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.actions.UndoAction;
import com.rapidminer.gui.actions.ValidateAutomaticallyAction;
import com.rapidminer.gui.actions.ValidateProcessAction;
import com.rapidminer.gui.actions.export.ShowPrintAndExportDialogAction;
import com.rapidminer.gui.actions.search.ActionsGlobalSearch;
import com.rapidminer.gui.actions.search.ActionsGlobalSearchManager;
import com.rapidminer.gui.actions.startup.NewAction;
import com.rapidminer.gui.actions.startup.OpenAction;
import com.rapidminer.gui.actions.startup.TutorialAction;
import com.rapidminer.gui.dialog.UnknownParametersInfoDialog;
import com.rapidminer.gui.flow.ErrorTable;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.operatortree.OperatorTree;
import com.rapidminer.gui.operatortree.OperatorTreePanel;
import com.rapidminer.gui.operatortree.actions.CutCopyPasteDeleteAction;
import com.rapidminer.gui.operatortree.actions.ToggleBreakpointItem;
import com.rapidminer.gui.osx.OSXAdapter;
import com.rapidminer.gui.osx.OSXQuitListener;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.processeditor.MacroViewer;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.processeditor.ProcessContextProcessEditor;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.processeditor.XMLEditor;
import com.rapidminer.gui.processeditor.results.ResultDisplay;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.properties.OperatorPropertyPanel;
import com.rapidminer.gui.search.action.GlobalSearchAction;
import com.rapidminer.gui.security.PasswordManager;
import com.rapidminer.gui.tools.ProcessGUITools;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.ResultWarningPreventionRegistry;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.SystemMonitor;
import com.rapidminer.gui.tools.bubble.BubbleWindow;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizardFactory;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizardRegistry;
import com.rapidminer.gui.tools.ioobjectcache.IOObjectCacheViewer;
import com.rapidminer.gui.tools.logging.LogViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tutorial.Tutorial;
import com.rapidminer.tutorial.gui.TutorialBrowser;
import com.rapidminer.tutorial.gui.TutorialSelector;
import com.vlsolutions.swing.docking.DockGroup;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.toolbars.ToolBarContainer;


/**
 * The main component class of the RapidMiner GUI. The class holds a lot of Actions that can be used
 * for the tool bar and for the menu bar. MainFrame has methods for handling the process (saving,
 * opening, creating new). It keeps track of the state of the process and enables/disables buttons.
 * It must be notified whenever the process changes and propagates this event to its children. Most
 * of the code is enclosed within the Actions.
 *
 * @author Ingo Mierswa, Simon Fischer, Sebastian Land, Marius Helf, Jan Czogalla
 */
@SuppressWarnings("deprecation")
public class MainFrame extends ApplicationFrame implements WindowListener {

	/**
	 * This listener takes care of changes relevant to the {@link MainFrame} itself, such as the
	 * {@link ProcessThread} and saving, loading or setting a process.
	 *
	 * @since 7.5
	 * @author Jan Czogalla
	 */
	private class MainProcessListener implements ExtendedProcessEditor, ProcessStorageListener {

		private Process oldProcess;

		@Override
		public void processChanged(Process process) {
			Process currentProcess = oldProcess;
			oldProcess = process;
			boolean firstProcess = currentProcess == null;
			if (!firstProcess) {
				currentProcess.removeObserver(processObserver);
				if (currentProcess.getProcessState() != Process.PROCESS_STATE_STOPPED) {
					if (processThread != null) {
						processThread.stopProcess();
					}
				}
			}

			if (process != null) {

				synchronized (process) {
					processThread = new ProcessThread(process);
					process.addObserver(processObserver, true);
					process.addBreakpointListener(breakpointListener);
					if (VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
						// not running at this point!
						validateProcess(true);
					}
				}
			}
			processPanel.getProcessRenderer().repaint();
			setTitle();
			getStatusBar().clearSpecialText();
			processPanel.getViewPort().firePropertyChange(ProcessPanel.SCROLLER_UPDATE, false, true);
		}

		@Override
		public void setSelection(List<Operator> selection) {

		}

		@Override
		public void processUpdated(Process process) {
			setTitle();
			if (getProcess().getProcessLocation() != null) {
				MainFrame.this.SAVE_ACTION.setEnabled(processModel.hasChanged());
			}
			processPanel.getProcessRenderer().repaint();
		}

		@Override
		public void stored(Process process) {
			SAVE_ACTION.setEnabled(false);
			setTitle();
			updateRecentFileList();
		}

		@Override
		public void opened(Process process) {
			if (process.getImportMessage() != null && process.getImportMessage().contains("error")) {
				SwingTools.showLongMessage("import_message", process.getImportMessage());
			}

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					SAVE_ACTION.setEnabled(false);
					setTitle();
				}
			});

			List<UnknownParameterInformation> unknownParameters = null;
			synchronized (process) {
				RapidMinerGUI.useProcessFile(process);
				unknownParameters = process.getUnknownParameters();
			}

			updateRecentFileList();

			// show unsupported parameters info?
			if (unknownParameters != null && unknownParameters.size() > 0) {
				final UnknownParametersInfoDialog unknownParametersInfoDialog = new UnknownParametersInfoDialog(
						MainFrame.this, unknownParameters);
				if (SwingUtilities.isEventDispatchThread()) {
					unknownParametersInfoDialog.setVisible(true);
				} else {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {

							@Override
							public void run() {
								unknownParametersInfoDialog.setVisible(true);
							}
						});
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING, "Error opening the unknown parameter dialog: " + e, e);
					}
				}
			}
		}

		@Override
		public void processViewChanged(Process process) {}

	}

	private static final long serialVersionUID = 1L;

	/** The property name for the max row check scale modifier for HTML5 visualizations */
	public static final String PROPERTY_RAPIDMINER_GUI_VISUALIZATIONS_MAX_ROWS_MODIFIER = "rapidminer.gui.visualizations.max_row_modifier";

	/** The property name whether legacy simple charts should still be hidden in the results */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_SHOW_LEGACY_SIMPLE_CHARTS = "rapidminer.gui.plotter.legacy.simple_charts.show";

	/** The property name whether legacy simple charts should still be shown in the results */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_SHOW_LEGACY_ADVANCED_CHARTS = "rapidminer.gui.plotter.legacy.advanced_charts.show";

	/** The property name for &quot;The pixel size of each plot in matrix plots.&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_MATRIXPLOT_SIZE = "rapidminer.gui.plotter.matrixplot.size";

	/**
	 * The property name for &quot;The maximum number of rows used for a plotter, using only a
	 * sample of this size if more rows are available.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_ROWS_MAXIMUM = "rapidminer.gui.plotter.rows.maximum";

	/**
	 * The property name for the &quot; The maximum number of examples in a data set for which
	 * default plotter settings will be generated.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_DEFAULT_MAXIMUM = "rapidminer.gui.plotter.default.maximum";

	/**
	 * The property name for &quot;Limit number of displayed classes plotter legends. -1 for no
	 * limit.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_CLASSLIMIT = "rapidminer.gui.plotter.legend.classlimit";

	/** The property name for &quot;The color for minimum values of the plotter legend.&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR = "rapidminer.gui.plotter.legend.mincolor";

	/** The property name for &quot;The color for maximum values of the plotter legend.&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR = "rapidminer.gui.plotter.legend.maxcolor";

	/**
	 * The property name for &quot;Limit number of displayed classes for colorized plots. -1 for no
	 * limit.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_PLOTTER_COLORS_CLASSLIMIT = "rapidminer.gui.plotter.colors.classlimit";

	/**
	 * The property name for &quot;Maximum number of states in the undo list.&quot;
	 *
	 * @deprecated Since 7.5. Use {@link RapidMinerGUI#PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE}
	 *             instead
	 */
	@Deprecated
	public static final String PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE = RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE;

	/**
	 * The property name for &quot;Maximum number of examples to use for the attribute editor. -1
	 * for no limit.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_ATTRIBUTEEDITOR_ROWLIMIT = "rapidminer.gui.attributeeditor.rowlimit";

	/** The property name for &quot;Beep on process success?&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_BEEP_SUCCESS = "rapidminer.gui.beep.success";

	/** The property name for &quot;Beep on error?&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_BEEP_ERROR = "rapidminer.gui.beep.error";

	/** The property name for &quot;Beep when breakpoint reached?&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_BEEP_BREAKPOINT = "rapidminer.gui.beep.breakpoint";

	/**
	 * The property name for &quot;Limit number of displayed rows in the message viewer. -1 for no
	 * limit.&quot;
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT = "rapidminer.gui.messageviewer.rowlimit";

	/** The property name for &quot;Shows process info screen after loading?&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_PROCESSINFO_SHOW = "rapidminer.gui.processinfo.show";

	public static final String PROPERTY_RAPIDMINER_GUI_SAVE_BEFORE_RUN = "rapidminer.gui.save_before_run";

	public static final String PROPERTY_RAPIDMINER_GUI_SAVE_ON_PROCESS_CREATION = "rapidminer.gui.save_on_process_creation";

	/**
	 * The property determining whether or not to switch to result view when results are produced.
	 */
	public static final String PROPERTY_RAPIDMINER_GUI_AUTO_SWITCH_TO_RESULTVIEW = "rapidminer.gui.auto_switch_to_resultview";

	/** Log level of the LoggingViewer. */
	public static final String PROPERTY_RAPIDMINER_GUI_LOG_LEVEL = "rapidminer.gui.log_level";

	private static final int MAX_LOCATION_TITLE_LENGTH = 150;

	private static final String getFrameTitle() {
		return "RapidMiner Studio "
				+ LicenseTools.translateProductEdition(ProductConstraintManager.INSTANCE.getActiveLicense()) + " "
				+ RapidMiner.getLongVersion();
	}

	// --------------------------------------------------------------------------------

	public static final int EDIT_MODE = 0;
	public static final int RESULTS_MODE = 1;

	public final transient Action AUTO_WIRE = new AutoWireAction();

	public final transient Action NEW_ACTION = new NewAction();
	public final transient Action OPEN_ACTION = new OpenAction();
	public final transient SaveAction SAVE_ACTION = new SaveAction();
	public final transient Action SAVE_AS_ACTION = new SaveAsAction();
	public final transient ToggleAction PROPAGATE_REAL_METADATA_ACTION = new PropagateRealMetaDataAction();

	public final transient Action IMPORT_DATA_ACTION = new ImportDataAction();
	public final transient Action IMPORT_PROCESS_ACTION = new ImportProcessAction();
	public final transient Action EXPORT_PROCESS_ACTION = new ExportProcessAction();

	// ---------- Export as Image/Print actions -----------------
	public final transient Action EXPORT_ACTION = new ShowPrintAndExportDialogAction(false);

	public final transient Action EXIT_ACTION = new ExitAction();

	public final transient RunAction RUN_ACTION = new RunAction();
	public final transient Action PAUSE_ACTION = new PauseAction();
	public final transient Action STOP_ACTION = new StopAction();
	public final transient Action VALIDATE_ACTION = new ValidateProcessAction();
	public final transient ToggleAction VALIDATE_AUTOMATICALLY_ACTION = new ValidateAutomaticallyAction();

	public final transient Action CREATE_CONNECTION = new CreateConnectionAction();

	private transient JButton runRemoteToolbarButton;

	public final transient Action NEW_PERSPECTIVE_ACTION = new NewPerspectiveAction();
	public final transient Action RESTORE_PERSPECTIVE_ACTION = new RestoreDefaultPerspectiveAction();
	public final transient Action SETTINGS_ACTION = new SettingsAction();
	public final transient Action UNDO_ACTION = new UndoAction();
	public final transient Action REDO_ACTION = new RedoAction();
	public final transient Action MANAGE_CONFIGURABLES_ACTION = new ManageConfigurablesAction();

	public final transient Action TUTORIAL_ACTION = new TutorialAction();
	public final transient Action BROWSE_VIDEOS_ACTION = new BrowseAction("toolbar_resources.help_videos", URI.create("https://redirects.rapidminer.com/app/studio/8.1/getting-started-video/main_tool_bar"));
	public final transient Action BROWSE_COMMUNITY_ACTION = new BrowseAction("toolbar_resources.help_forum", URI.create("https://redirects.rapidminer.com/app/studio/7.2/forum/main_tool_bar"));
	public final transient Action BROWSE_DOCUMENTATION_ACTION = new BrowseAction("toolbar_resources.documentation", URI.create("https://redirects.rapidminer.com/app/studio/7.2/documentation/main_tool_bar"));
	public final transient Action BROWSE_SUPPORT_ACTION = new BrowseAction("toolbar_resources.support", URI.create("https://redirects.rapidminer.com/app/studio/7.2/support/main_tool_bar"));
	public final transient Action ABOUT_ACTION = new AboutAction();

	// --------------------------------------------------------------------------------

	// DOCKING

	public static final DockGroup DOCK_GROUP_ROOT = new DockGroup("root");
	public static final DockGroup DOCK_GROUP_RESULTS = new DockGroup("results");

	private final DockingContext dockingContext = new DockingContext();
	private final DockingDesktop dockingDesktop = new DockingDesktop("mainDesktop", dockingContext);

	private final MainProcessListener mainProcessListener = new MainProcessListener();
	private final Actions actions = new Actions(this);

	private final OperatorDocumentationBrowser operatorDocumentationBrowser = new OperatorDocumentationBrowser();
	private final OperatorTreePanel operatorTree = new OperatorTreePanel(this);
	private final ErrorTable errorTable = new ErrorTable(this);
	private final OperatorPropertyPanel propertyPanel = new OperatorPropertyPanel(this);
	private final XMLEditor xmlEditor = new XMLEditor(this);
	private final ProcessContextProcessEditor processContextEditor = new ProcessContextProcessEditor();
	private final ProcessPanel processPanel = new ProcessPanel(this);
	private final ProcessRendererModel processModel = processPanel.getProcessRenderer().getModel();
	private final NewOperatorEditor newOperatorEditor = new NewOperatorEditor(
			processPanel.getProcessRenderer().getDragListener());
	private final RepositoryBrowser repositoryBrowser = new RepositoryBrowser(
			processPanel.getProcessRenderer().getDragListener());
	private final MacroViewer macroViewer = new MacroViewer();

	private final ResultDisplay resultDisplay = ResultDisplayTools.makeResultDisplay();
	private final LogViewer logViewer = new LogViewer(this);
	private final IOObjectCacheViewer ioobjectCacheViewer = new IOObjectCacheViewer(RapidMiner.getGlobalIOObjectCache());
	private final SystemMonitor systemMonitor = new SystemMonitor();

	private final PerspectiveController perspectiveController = new PerspectiveController(dockingContext);
	private final TutorialSelector tutorialSelector = new TutorialSelector(this, perspectiveController.getModel());
	private final TutorialBrowser tutorialBrowser = new TutorialBrowser(tutorialSelector);

	/**
	 * @deprecated use {@link #perspectiveController} instead
	 */
	@Deprecated
	private final Perspectives perspectives = new Perspectives(perspectiveController);

	/** the bubble which displays a warning that no result ports are connected */
	private BubbleWindow noResultConnectionBubble;
	/** the bubble which displays a warning that a port must receive input but is not connected */
	private BubbleWindow missingInputBubble;
	/**
	 * the bubble which displays a warning that a parameter must be set as he has no default value
	 */
	private BubbleWindow missingParameterBubble;

	private transient ActionsGlobalSearchManager actionsGlobalSearchManager;

	private final JMenuBar menuBar;

	private final MainToolBar toolBar;

	private final JMenu fileMenu;

	private final JMenu editMenu;

	private final JMenu processMenu;

	private final JMenu settingsMenu;

	private final JMenu connectionsMenu;

	private final JMenu legacyConnectionsMenu;

	private final JMenu viewMenu;

	private final JMenu helpMenu;

	private final JMenu extensionsMenu;

	private DockableMenu dockableMenu;

	private final JMenu recentFilesMenu = new ResourceMenu("recent_files");

	private Insets menuBarInsets = new Insets(0, 0, 0, 5);

	/**
	 * The host name of the system. Might be empty (no host name will be shown) and will be
	 * initialized in the first call of {@link #setTitle()}.
	 */
	private String hostname = null;

	private transient ProcessThread processThread;

	private final MetaDataUpdateQueue metaDataUpdateQueue = new MetaDataUpdateQueue(this);

	@Deprecated
	private transient final DataImportWizardRegistry importWizardRegistry = new DataImportWizardRegistry() {

		private final List<DataImportWizardFactory> factories = new ArrayList<>();

		@Override
		public void register(DataImportWizardFactory factory) {
			if (factory == null) {
				throw new IllegalArgumentException("factory must not be null");
			}
			synchronized (factories) {
				factories.add(factory);
			}
		}

		@Override
		public List<DataImportWizardFactory> getFactories() {
			synchronized (factories) {
				return new ArrayList<>(factories);
			}
		}
	};

	// --------------------------------------------------------------------------------
	// LISTENERS And OBSERVERS

	private final PerspectiveChangeListener perspectiveChangeListener = perspective -> {
		// check all ConditionalActions on perspective switch
		getActions().enableActions();

		SwingTools.invokeLater(() -> {

			// try to request focus for the process renderer so actions are enabled after
			// perspective switch and ProcessRenderer is visible
			if (getProcessPanel().getProcessRenderer().isShowing()) {
				getProcessPanel().getProcessRenderer().requestFocusInWindow();
			}
		});
	};

	private volatile long lastUpdate = 0;
	private final Timer updateTimer = new Timer(500, e -> updateProcessNow()) {

		private static final long serialVersionUID = 1L;

		{
			setRepeats(false);
		}
	};

	/**
	 * @deprecated since 7.5; a view is automatically pushed on the undo stack when using
	 *             {@link ProcessRendererModel#setDisplayedChain(OperatorChain)}
	 */
	@Deprecated
	public void addViewSwitchToUndo() {}

	/** Lets the process model check for changes in the process */
	private void updateProcessNow() {
		lastUpdate = System.currentTimeMillis();
		if (processModel.checkForNewUndoStep()) {
			validateProcess(false);
		}
		processPanel.getProcessRenderer().repaint();
	}

	public void validateProcess(final boolean force) {
		if (force || getProcessState() != Process.PROCESS_STATE_RUNNING) {
			metaDataUpdateQueue.validate(getProcess(), force || VALIDATE_AUTOMATICALLY_ACTION.isSelected());
		} else {
			processModel.fireProcessUpdated();
		}
	}

	public boolean isProcessRendererFocused() {
		return processPanel.getProcessRenderer().hasFocus();
	}

	private transient final Observer<Process> processObserver = new Observer<Process>() {

		@Override
		public void update(final Observable<Process> observable, final Process arg) {
			// if (process.getProcessState() == Process.PROCESS_STATE_RUNNING) {
			// return;
			// }
			if (System.currentTimeMillis() - lastUpdate > 500) {
				updateProcessNow();
			} else {
				if (getProcessState() == Process.PROCESS_STATE_RUNNING) {
					if (!updateTimer.isRunning()) {
						updateTimer.start();
					}
				} else {
					updateProcessNow();
				}
			}
		}
	};

	private transient final BreakpointListener breakpointListener = new BreakpointListener() {

		@Override
		public void breakpointReached(final Process process, final Operator operator, final IOContainer ioContainer,
				final int location) {
			if (process.equals(getProcess())) {
				RUN_ACTION.setState(process.getProcessState());
				ProcessThread.beep("breakpoint");
				MainFrame.this.toFront();
				MainFrame.this.selectAndShowOperator(operator, true);
				resultDisplay.showData(ioContainer,
						"Breakpoint in " + operator.getName() + ", application " + operator.getApplyCount());
			}
		}

		/** Since the mainframe triggers the resume itself this method does nothing. */
		@Override
		public void resume() {
			RUN_ACTION.setState(getProcessState());
		}
	};

	private JToolBar buttonToolbar;

	// --------------------------------------------------------------------------------

	/** Creates a new main frame containing the RapidMiner GUI. */
	public MainFrame() {
		super(getFrameTitle());

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
			// load dock icon from resources and adapt UI via OSXAdapter class
			try {
				OSXQuitListener quitListener = new OSXQuitListener() {

					@Override
					public void quit() {
					    MainFrame.this.exit(false);
					}
				};
				OSXAdapter.adaptUI(this, SETTINGS_ACTION, new AboutAction(), quitListener);
			} catch (Throwable t) {
				// catch everything - in case the OSX adapter is called without being on a OS X system
				// or the Java classes have been removed from OS X JRE it will just log an error
				// instead of breaking the program start-up
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.MainFrame.could_not_adapt_OSX_look_and_feel", t);
			}
		}

		addProcessListeners();

		SwingTools.setFrameIcon(this);

		// load perspectives now because otherwise the WSDesktop class does not know the nodes and
		// won't restore the user customized perspective
		perspectiveController.loadAll();

		dockingContext.addDesktop(dockingDesktop);
		dockingDesktop.registerDockable(repositoryBrowser);
		dockingDesktop.registerDockable(operatorTree);
		dockingDesktop.registerDockable(propertyPanel);
		dockingDesktop.registerDockable(processPanel);
		dockingDesktop.registerDockable(xmlEditor);
		dockingDesktop.registerDockable(newOperatorEditor);
		dockingDesktop.registerDockable(errorTable);
		dockingDesktop.registerDockable(resultDisplay);
		dockingDesktop.registerDockable(logViewer);
		dockingDesktop.registerDockable(ioobjectCacheViewer);
		dockingDesktop.registerDockable(systemMonitor);
		dockingDesktop.registerDockable(operatorDocumentationBrowser);
		dockingDesktop.registerDockable(processContextEditor);
		dockingDesktop.registerDockable(processPanel.getProcessRenderer().getOverviewPanel());
		dockingDesktop.registerDockable(macroViewer);
		dockingDesktop.registerDockable(tutorialBrowser);

		// Test

		ToolBarContainer toolBarContainer = ToolBarContainer.createDefaultContainer(true, true, true, true);
		toolBarContainer.setBorder(BorderFactory.createEmptyBorder(6, 3, 0, 3));
		toolBarContainer.setOpaque(true);
		toolBarContainer.setBackground(Colors.WINDOW_BACKGROUND);
		getContentPane().add(toolBarContainer, BorderLayout.CENTER);
		toolBarContainer.add(dockingDesktop, BorderLayout.CENTER);

		systemMonitor.startMonitorThread();
		processPanel.getDockKey().setCloseEnabled(false);
		resultDisplay.getDockKey().setCloseEnabled(false);
		resultDisplay.init(this);

		// menu bar
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		fileMenu = new ResourceMenu("file");
		fileMenu.setMargin(menuBarInsets);
		fileMenu.add(NEW_ACTION);
		fileMenu.add(OPEN_ACTION);
		updateRecentFileList();
		fileMenu.add(recentFilesMenu);
		fileMenu.addSeparator();
		fileMenu.add(SAVE_ACTION);
		fileMenu.add(SAVE_AS_ACTION);
		fileMenu.addSeparator();
		fileMenu.add(IMPORT_DATA_ACTION);
		fileMenu.add(IMPORT_PROCESS_ACTION);
		fileMenu.add(EXPORT_PROCESS_ACTION);
		menuBar.add(fileMenu);


		// edit menu
		((ResourceAction) actions.INFO_OPERATOR_ACTION).addToActionMap(JComponent.WHEN_FOCUSED, true, true, null,
				getProcessPanel().getProcessRenderer(), getOperatorTree());
		((ResourceAction) actions.TOGGLE_ACTIVATION_ITEM).addToActionMap(JComponent.WHEN_FOCUSED, true, true, null,
				getProcessPanel().getProcessRenderer(), getOperatorTree());
		((ResourceAction) actions.RENAME_OPERATOR_ACTION).addToActionMap(JComponent.WHEN_FOCUSED, true, true, null,
				getProcessPanel().getProcessRenderer(), getOperatorTree());
		// not added for ProcessRenderer because there the DELETE_SELECTED_CONNECTION action is
		// active
		((ResourceAction) actions.DELETE_OPERATOR_ACTION).addToActionMap(JComponent.WHEN_FOCUSED, true, true, null,
				getOperatorTree());
		((ResourceAction) actions.TOGGLE_ALL_BREAKPOINTS).addToActionMap(JComponent.WHEN_FOCUSED, true, true, null,
				getProcessPanel().getProcessRenderer(), getOperatorTree());
		editMenu = new ResourceMenu("edit");
		editMenu.setMargin(menuBarInsets);
		editMenu.add(UNDO_ACTION);
		editMenu.add(REDO_ACTION);
		editMenu.addSeparator();
		editMenu.add(actions.INFO_OPERATOR_ACTION);
		editMenu.add(actions.TOGGLE_ACTIVATION_ITEM.createMenuItem());
		editMenu.add(actions.RENAME_OPERATOR_ACTION);
		editMenu.addSeparator();
		editMenu.add(CutCopyPasteDeleteAction.CUT_ACTION);
		editMenu.add(CutCopyPasteDeleteAction.COPY_ACTION);
		editMenu.add(CutCopyPasteDeleteAction.PASTE_ACTION);
		editMenu.add(CutCopyPasteDeleteAction.DELETE_ACTION);
		editMenu.addSeparator();
		for (ToggleBreakpointItem item : actions.TOGGLE_BREAKPOINT) {
			editMenu.add(item.createMenuItem());
		}
		editMenu.add(actions.TOGGLE_ALL_BREAKPOINTS.createMenuItem());
		editMenu.add(actions.REMOVE_ALL_BREAKPOINTS);
		// editMenu.add(actions.MAKE_DIRTY_ACTION);
		menuBar.add(editMenu);


		// process menu
		processMenu = new ResourceMenu("process");
		processMenu.setMargin(menuBarInsets);
		processMenu.add(RUN_ACTION);
		processMenu.add(PAUSE_ACTION);
		processMenu.add(STOP_ACTION);
		processMenu.addSeparator();
		processMenu.add(PROPAGATE_REAL_METADATA_ACTION.createMenuItem());
		processMenu.add(VALIDATE_ACTION);
		processMenu.add(VALIDATE_AUTOMATICALLY_ACTION.createMenuItem());
		processMenu.addSeparator();

		processMenu.add(AUTO_WIRE);
		processMenu.add(processPanel.getFlowVisualizer().ALTER_EXECUTION_ORDER.createMenuItem());
		JMenu layoutMenu = new ResourceMenu("process_layout");
		layoutMenu.add(processPanel.getProcessRenderer().getArrangeOperatorsAction());
		layoutMenu.add(processPanel.getProcessRenderer().getAutoFitAction());
		processMenu.add(layoutMenu);
		menuBar.add(processMenu);


		// view menu
		viewMenu = new ResourceMenu("view");
		viewMenu.setMargin(menuBarInsets);
		viewMenu.add(new PerspectiveMenu(perspectiveController));
		viewMenu.add(NEW_PERSPECTIVE_ACTION);
		viewMenu.add(dockableMenu = new DockableMenu(dockingContext));
		viewMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				dockableMenu.fill();
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				// ignore
			}

			@Override
			public void menuCanceled(MenuEvent e) {
				// ignore
			}
		});
		viewMenu.add(RESTORE_PERSPECTIVE_ACTION);


		menuBar.add(viewMenu);

		// create settings menu (will be added in finishInitialization())
		settingsMenu = new ResourceMenu("settings");
		settingsMenu.setMargin(menuBarInsets);

		// connections menu
		connectionsMenu = new ResourceMenu("connections");
		connectionsMenu.setMargin(menuBarInsets);
		connectionsMenu.add(CREATE_CONNECTION);
		connectionsMenu.addSeparator();
		menuBar.add(connectionsMenu);

		// legacy connections menu
		legacyConnectionsMenu = new ResourceMenu("legacy_connections");

		// help menu
		helpMenu = new ResourceMenu("help");
		helpMenu.add(TUTORIAL_ACTION);
		helpMenu.add(BROWSE_VIDEOS_ACTION);
		helpMenu.add(BROWSE_COMMUNITY_ACTION);
		helpMenu.add(BROWSE_DOCUMENTATION_ACTION);
		helpMenu.add(BROWSE_SUPPORT_ACTION);
		helpMenu.addSeparator();
		helpMenu.add(ABOUT_ACTION);

		// extensions menu
		extensionsMenu = new ResourceMenu("extensions");
		extensionsMenu.setMargin(menuBarInsets);

		// main tool bar
		toolBar = new MainToolBar(this);

		getStatusBar().setBackground(Colors.WINDOW_BACKGROUND);
		getContentPane().add(toolBar, BorderLayout.NORTH);
		getContentPane().add(getStatusBar(), BorderLayout.SOUTH);
		getStatusBar().startClockThread();

		// initialize Ctrl+F shortcut to start Global Search
		new GlobalSearchAction();

		setProcess(new Process(), true);

		perspectiveController.getModel().addPerspectiveChangeListener(perspectiveChangeListener);
		pack();
		metaDataUpdateQueue.start();
	}

	/**
	 * Adds all relevant {@link ProcessEditor ProcessEditors}. Adds the {@link MainProcessListener}
	 * as first.
	 */
	private void addProcessListeners() {
		processModel.addProcessEditor(mainProcessListener);
		processModel.addProcessStorageListener(mainProcessListener);
		processModel.addProcessEditor(actions);
		processModel.addProcessEditor(xmlEditor);
		processModel.addProcessEditor(propertyPanel);
		processModel.addProcessEditor(operatorTree);
		processModel.addProcessEditor(operatorDocumentationBrowser);
		processModel.addProcessEditor(processPanel);
		processModel.addProcessEditor(errorTable);
		processModel.addProcessEditor(processContextEditor);
		processModel.addProcessEditor(getStatusBar());
		processModel.addProcessEditor(resultDisplay);
		processModel.addProcessEditor(macroViewer);
	}

	/**
	 * Finishes the MainFrame initialization. Should be called after all extension have been
	 * initialized.
	 */
	public void finishInitialization() {

		// Configurators (if they exist)
		if (!ConfigurationManager.getInstance().isEmpty()) {
			legacyConnectionsMenu.add(MANAGE_CONFIGURABLES_ACTION);
		}

		// Add legacy menu (if not empty)
		if (legacyConnectionsMenu.getMenuComponentCount() > 0) {
			connectionsMenu.addSeparator();
			connectionsMenu.add(legacyConnectionsMenu);
		}

		// remove duplicated separators
		removeDuplicatedSeparators(connectionsMenu);

		// add export and exit as last file menu actions
		fileMenu.addSeparator();
		fileMenu.add(EXPORT_ACTION);
		fileMenu.addSeparator();
		fileMenu.add(EXIT_ACTION);


		// Password Manager
		settingsMenu.add(PasswordManager.OPEN_WINDOW);
		if (SystemInfoUtilities.getOperatingSystem() != OperatingSystem.OSX || !OSXAdapter.isAdapted()) {
			settingsMenu.add(SETTINGS_ACTION);
		}

		// add settings menu as second last menu or third last if there are entries in the help menu
		menuBar.add(settingsMenu);

		// add extensions menu as last menu or second last if there are entries in the help
		// menu
		menuBar.add(extensionsMenu);

		// Add Help menu as last entry if it is not empty
		if (helpMenu.getItemCount() > 0) {
			helpMenu.setMargin(menuBarInsets);
			menuBar.add(helpMenu);
		}
	}

	public OperatorPropertyPanel getPropertyPanel() {
		return propertyPanel;
	}

	/**
	 * Returns a registry for {@link DataImportWizardFactory} instances. The factories are used to
	 * populate menus such as the main import menu.
	 *
	 * @return the registry
	 * @deprecated Use {@link DataSourceFactoryRegistry} instead. Registering a
	 *             {@link DataImportWizardRegistry} will not have an effect anymore.
	 */
	@Deprecated
	public DataImportWizardRegistry getDataImportWizardRegistry() {
		return importWizardRegistry;
	}

	public LogViewer getLogViewer() {
		return logViewer;
	}

	public NewOperatorEditor getNewOperatorEditor() {
		return newOperatorEditor;
	}

	public OperatorTree getOperatorTree() {
		return operatorTree.getOperatorTree();
	}

	public Actions getActions() {
		return actions;
	}

	public ResultDisplay getResultDisplay() {
		return resultDisplay;
	}

	/**
	 * @return the toolbar button for running processes on the Server
	 */
	public JButton getRunRemoteToolbarButton() {
		return runRemoteToolbarButton;
	}

	public int getProcessState() {
		Process process = getProcess();
		if (process == null) {
			return Process.PROCESS_STATE_UNKNOWN;
		} else {
			return process.getProcessState();
		}
	}

	/**
	 * @deprecated Use {@link #getProcess()} instead
	 */
	@Deprecated
	public final Process getExperiment() {
		return getProcess();
	}

	public final Process getProcess() {
		return processModel.getProcess();
	}

	// ====================================================
	// M A I N A C T I O N S
	// ===================================================

	/**
	 * Creates a new process. If there are unsaved changes, the user will be asked to save their
	 * work.
	 */
	public void newProcess() {
		newProcess(true);
	}

	/**
	 * Creates a new process. Depending on the given parameter, the user will or will not be asked
	 * to save unsaved changes.
	 *
	 * @param checkforUnsavedWork
	 *            Iff {@code true} the user is asked to save their unsaved work (if any), otherwise
	 *            unsaved work is discarded without warning.
	 */
	public void newProcess(final boolean checkforUnsavedWork) {
		// ask for confirmation before stopping the currently running process and opening a new one!
		if (getProcessState() == Process.PROCESS_STATE_RUNNING || getProcessState() == Process.PROCESS_STATE_PAUSED) {
			if (SwingTools.showConfirmDialog("close_running_process",
					ConfirmDialog.YES_NO_OPTION) != ConfirmDialog.YES_OPTION) {
				return;
			}
		}

		ProgressThread newProgressThread = new ProgressThread("new_process") {

			@Override
			public void run() {
				// Invoking close() will ask the user to save their work if there are unsaved
				// changes. This method can be skipped if it is already clear that changes should be
				// discarded.
				boolean resetProcess = checkforUnsavedWork ? close(false) : true;
				if (resetProcess) {
					// process changed -> clear undo history

					stopProcess();
					setProcess(new Process(), true);
					if (!"false"
							.equals(ParameterService.getParameterValue(PROPERTY_RAPIDMINER_GUI_SAVE_ON_PROCESS_CREATION))) {
						SaveAction.saveAsync(getProcess());
					}
					// always have save action enabled. If process is not yet associated with
					// location SaveAs will be used
					SAVE_ACTION.setEnabled(true);
				}
			}
		};
		newProgressThread.setIndeterminate(true);
		newProgressThread.setCancelable(false);
		newProgressThread.start();
	}

	/**
	 * Runs or resumes the current process. If the process is started, checks for potential errors
	 * first and prevents execution unless the user has disabled the pre-run check.
	 */
	public void runProcess() {
		runProcess(true);
	}

	/**
	 * Runs or resumes the current process.
	 *
	 * @param precheckBeforeExecution
	 *            if {@code true} and the process is started, checks for potential errors first and
	 *            prevents execution unless the user has disabled the pre-run check
	 */
	public void runProcess(boolean precheckBeforeExecution) {
		if (getProcessState() == Process.PROCESS_STATE_STOPPED) {
			// Run
			if (isChanged() || getProcess().getProcessLocation() == null) {
				if (DecisionRememberingConfirmDialog.confirmAction("save_before_run",
						PROPERTY_RAPIDMINER_GUI_SAVE_BEFORE_RUN)) {
					SaveAction.saveAsync(getProcess());
				}
			}

			// don't run process if showstoppers are present
			// this only returns true if the user did not disable the strict process check in the
			// preferences
			if (precheckBeforeExecution && doesProcessContainShowstoppers()) {
				return;
			}

			processThread = new ProcessThread(getProcess());
			try {
				processThread.start();
			} catch (Exception t) {
				SwingTools.showSimpleErrorMessage("cannot_start_process", t);
			}
		} else {
			getProcess().resume();
		}
	}

	/**
	 * Can be used to stop the currently running process. Please note that the ProcessThread will
	 * still be running in the background until the current operator is finished.
	 */
	public void stopProcess() {
		if (getProcessState() != Process.PROCESS_STATE_STOPPED) {
			getProcess().getLogger().info("Process stopped. Completing current operator.");
			getStatusBar().setSpecialText("Process stopped. Completing current operator.");
			if (processThread != null) {
				if (processThread.isAlive()) {
					processThread.setPriority(Thread.MIN_PRIORITY);
					processThread.stopProcess();
				}
			}
		}
	}

	public void pauseProcess() {
		if (getProcessState() == Process.PROCESS_STATE_RUNNING) {
			getProcess().getLogger().info("Process paused. Completing current operator.");
			getStatusBar().setSpecialText("Process paused. Completing current operator.");
			if (processThread != null) {
				processThread.pauseProcess();
			}
		}
	}

	/** Will be invoked from the process thread after the process was successfully ended. */
	void processEnded(final Process process, final IOContainer results) {
		if (process.equals(getProcess())) {
			if (results != null) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						MainFrame.this.toFront();
					}
				});
			}
		}
		if (process.equals(getProcess())) {
			if (results != null) {
				resultDisplay.showData(results, "Process results");
			}
		}
	}

	/**
	 * Sets a new process and registers the MainFrame listener. Please note that this method does
	 * not invoke {@link #processChanged()}. Do so if necessary.
	 *
	 * @deprecated Use {@link #setProcess(Process, boolean)} instead
	 */
	@Deprecated
	public void setExperiment(final Process process) {
		setProcess(process, true);
	}

	/**
	 * Sets a (new) process.
	 *
	 * @param process
	 *            the process to be set
	 * @param newProcess
	 *            whether the process should be treated as new
	 */
	public void setProcess(final Process process, final boolean newProcess) {
		setOrOpenProcess(process, newProcess, false);
	}

	/**
	 * Sets or loads a (new) process. Resets the undo stack for the model if necessary. <br/>
	 * Note: It should hold: open => newProcess
	 */
	private void setOrOpenProcess(final Process process, final boolean newProcess, final boolean open) {
		boolean firstProcess = getProcess() == null;
		if (newProcess) {
			// set origin if possible
			ProcessTools.setProcessOrigin(process);
		}
		processModel.setProcess(process, newProcess, open);
		if (newProcess) {
			enableUndoAction();
			if (!firstProcess) {
				// VLDocking appears to get nervous when applying two perspectives while the
				// window is not yet visible. So to avoid that we set design and then welcome
				// during startup, avoid applying design if this is the first process we create.
				perspectiveController.showPerspective(PerspectiveModel.DESIGN);
			}
		}
		updateProcessNow();
	}

	/**
	 * Sets a new process and registers the MainFrame's listeners.
	 *
	 * @deprecated Since 7.5. Use {@link #setProcess(Process, boolean)} instead, since undo steps
	 *             are automatically added when necessary.
	 */
	@Deprecated
	public void setProcess(final Process process, final boolean newProcess, final boolean addToUndoList) {
		setProcess(process, newProcess);
	}

	/**
	 * Must be called when the process changed (such that is different from the process before).
	 * Enables the correct actions if the process can be saved to disk.
	 *
	 * @deprecated this method is no longer necessary (and does nothing) since the MainFrame
	 *             observes the process using an Observer pattern. See {@link #processObserver}.
	 */
	@Deprecated
	public void processChanged() {}

	/** Returns true if the process has changed since the last save. */
	public boolean isChanged() {
		return processModel.hasChanged();
	}

	public void undo() {
		Exception e = processModel.undo();
		if (e != null) {
			SwingTools.showSimpleErrorMessage("while_changing_process", e);
		}
	}

	public void redo() {
		Exception e = processModel.redo();
		if (e != null) {
			SwingTools.showSimpleErrorMessage("while_changing_process", e);
		}
	}

	public void enableUndoAction() {
		UNDO_ACTION.setEnabled(processModel.hasUndoSteps());
		REDO_ACTION.setEnabled(processModel.hasRedoSteps());
	}

	/**
	 * Returns <code>true</code> if the current process has undo steps available.
	 *
	 * @return
	 */
	public boolean hasUndoSteps() {
		return processModel.hasUndoSteps();
	}

	/**
	 * Returns <code>true</code> if the current process has redo steps available.
	 *
	 * @return
	 */
	public boolean hasRedoSteps() {
		return processModel.hasRedoSteps();
	}

	/**
	 * Sets the window title (RapidMiner + filename + an asterisk if process was modified.
	 */
	public void setTitle() {
		if (hostname == null) {
			try {
				hostname = " @ " + InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				hostname = "";
			}
		}

		Process process = getProcess();
		if (process != null) {
			ProcessLocation loc = process.getProcessLocation();
			String locString;
			if (loc != null) {
				locString = loc.toString();
				// location string exceeding arbitrary number will be cut into repository name +
				// /.../ + process name
				if (locString.length() > MAX_LOCATION_TITLE_LENGTH) {
					if (process.getRepositoryLocation() != null) {
						locString = RepositoryLocation.REPOSITORY_PREFIX + process.getRepositoryLocation().getRepositoryName()
								+ RepositoryLocation.SEPARATOR + "..." + RepositoryLocation.SEPARATOR + loc.getShortName();
					} else {
						// build a string like /home/jdoe/.../processes/process.rmp
						int maxPartLength = MAX_LOCATION_TITLE_LENGTH / 2;
						// determine length of the first part
						int firstPartEnd = locString.lastIndexOf(File.separator, maxPartLength);
						// + 1 to include the separator char
						firstPartEnd = firstPartEnd == -1 ? maxPartLength : firstPartEnd + 1;
						int secondPartStart = locString.indexOf(File.separator, locString.length() - maxPartLength);
						secondPartStart = secondPartStart == -1 ? locString.length() - maxPartLength : secondPartStart;
						locString = new StringBuilder().append(locString, 0, firstPartEnd).append("...").append(locString, secondPartStart, locString.length()).toString();
					}
				}
			} else {
				locString = "<new process";
			}
			setTitle(locString + (isChanged() ? "*" : "") + (loc == null ? ">" : "") + " \u2013 " + getFrameTitle()
					+ hostname);
		} else {
			setTitle(getFrameTitle() + hostname);
		}
	}

	// //////////////////// File menu actions ////////////////////

	/**
	 * Closes the current process
	 *
	 * @param askForConfirmation
	 *            if <code>true</code>, will prompt the user if he really wants to close the current
	 *            process
	 * @return
	 */
	public boolean close(final boolean askForConfirmation) {
		if (!isChanged()) {
			return true;
		}
		ProcessLocation loc = getProcess().getProcessLocation();
		String locName;
		if (loc != null) {
			locName = loc.getShortName();
		} else {
			locName = "unnamed";
		}
		switch (SwingTools.showConfirmDialog("save", ConfirmDialog.YES_NO_CANCEL_OPTION, locName)) {
			case ConfirmDialog.YES_OPTION:
				SaveAction.save(getProcess());

				// it may happen that save() does not actually save the process, because the
				// user hits cancel in the
				// saveAs dialog or an error occurs. In this case the process won't be marked as
				// unchanged. Thus,
				// we return the process changed status.
				return !isChanged();
			case ConfirmDialog.NO_OPTION:
				// ask for confirmation before stopping the currently running process (if
				// askForConfirmation=true)
				if (askForConfirmation) {
					if (RapidMinerGUI.getMainFrame().getProcessState() == Process.PROCESS_STATE_RUNNING
							|| RapidMinerGUI.getMainFrame().getProcessState() == Process.PROCESS_STATE_PAUSED) {
						if (SwingTools.showConfirmDialog("close_running_process",
								ConfirmDialog.YES_NO_OPTION) != ConfirmDialog.YES_OPTION) {
							return false;
						}
					}
				}
				if (getProcessState() != Process.PROCESS_STATE_STOPPED) {
					synchronized (processThread) {
						processThread.stopProcess();
					}
				}
				return true;
			default: // cancel
				return false;
		}

	}

	public boolean close() {
		return close(true);
	}

	/**
	 * @deprecated Since 7.5. Use {@link #setOpenedProcess(Process)}, the other parameters are
	 *             irrelevant.
	 */
	@Deprecated
	public void setOpenedProcess(final Process process, final boolean showInfo, final String sourceName) {
		setOpenedProcess(process);
	}

	/** Opens the specified process. */
	public void setOpenedProcess(final Process process) {
		setOrOpenProcess(process, true, true);
	}

	public void exit(final boolean relaunch) {
		if (isChanged()) {
			ProcessLocation loc = getProcess().getProcessLocation();
			String locName;
			if (loc != null) {
				locName = loc.getShortName();
			} else {
				locName = "unnamed";
			}
			switch (SwingTools.showConfirmDialog("save", ConfirmDialog.YES_NO_CANCEL_OPTION, locName)) {
				case ConfirmDialog.YES_OPTION:
					SaveAction.save(getProcess());
					if (isChanged()) {
						return;
					}
					break;
				case ConfirmDialog.NO_OPTION:
					break;
				case ConfirmDialog.CANCEL_OPTION:
				default:
					return;
			}
		} else {
			if (!relaunch) { // in this case we have already confirmed
				// ask for special confirmation before exiting RapidMiner while a process is
				// running!
				if (getProcessState() == Process.PROCESS_STATE_RUNNING
						|| getProcessState() == Process.PROCESS_STATE_PAUSED) {
					if (SwingTools.showConfirmDialog("exit_despite_running_process",
							ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.NO_OPTION) {
						return;
					}
				} else {
					int answer = ConfirmDialog.showConfirmDialog(ApplicationFrame.getApplicationFrame(), "exit",
							ConfirmDialog.YES_NO_OPTION, RapidMinerGUI.PROPERTY_CONFIRM_EXIT, ConfirmDialog.YES_OPTION);
					if (answer != ConfirmDialog.YES_OPTION) {
						return;
					}
				}
			}
		}
		stopProcess();
		RapidMinerGUI.saveGUIProperties();
		dispose();
		RapidMiner.quit(relaunch ? RapidMiner.ExitMode.RELAUNCH : RapidMiner.ExitMode.NORMAL);
	}

	/** Updates the list of recently used files. */
	public void updateRecentFileList() {
		recentFilesMenu.removeAll();
		List<ProcessLocation> recentFiles = RapidMinerGUI.getRecentFiles();
		for (final ProcessLocation recentLocation : recentFiles) {
			// whitespaces to create a gap between icon and text as #setIconTextGap(int) sets a gap to both sides of the icon...
			JMenuItem menuItem = new JMenuItem("   " + recentLocation.toMenuString());
			menuItem.setIcon(SwingTools.createIcon("16/" + recentLocation.getIconName()));
			menuItem.addActionListener(e -> {
				if (RapidMinerGUI.getMainFrame().close()) {
					com.rapidminer.gui.actions.OpenAction.open(recentLocation, true);
				}
			});
			recentFilesMenu.add(menuItem);
		}
	}

	/**
	 * Update the elements of the main tool bar.
	 */
	public void updateToolbar() {
		toolBar.update();
	}

	@Override
	public void windowOpened(final WindowEvent e) {}

	@Override
	public void windowClosing(final WindowEvent e) {
		exit(false);
	}

	@Override
	public void windowClosed(final WindowEvent e) {}

	@Override
	public void windowIconified(final WindowEvent e) {}

	@Override
	public void windowDeiconified(final WindowEvent e) {}

	@Override
	public void windowActivated(final WindowEvent e) {}

	@Override
	public void windowDeactivated(final WindowEvent e) {}

	/**
	 * This methods provide plugins the possibility to modify the menus
	 */
	public void removeMenu(final int index) {
		menuBar.remove(menuBar.getMenu(index));
	}

	public void removeMenuItem(final int menuIndex, final int itemIndex) {
		menuBar.getMenu(menuIndex).remove(itemIndex);
	}

	public void addMenuItem(final int menuIndex, final int itemIndex, final JMenuItem item) {
		menuBar.getMenu(menuIndex).add(item, itemIndex);
	}

	public void addMenu(int menuIndex, final JMenu menu) {
		menu.setMargin(menuBarInsets);
		if (menuIndex < -1 || menuIndex >= menuBar.getComponentCount()) {
			menuIndex = -1;
		}
		menuBar.add(menu, menuIndex);
	}

	public void addMenuSeparator(final int menuIndex) {
		menuBar.getMenu(menuIndex).addSeparator();
	}

	// / LISTENERS

	public List<Operator> getSelectedOperators() {
		return processModel.getSelectedOperators();
	}

	public Operator getFirstSelectedOperator() {
		List<Operator> selectedOperators = processModel.getSelectedOperators();
		return selectedOperators.isEmpty() ? null : selectedOperators.get(0);
	}

	/**
	 * @deprecated use {@link #addExtendedProcessEditor(ExtendedProcessEditor)} instead.
	 */
	@Deprecated
	public void addProcessEditor(final ProcessEditor p) {
		processModel.addProcessEditor(p);
	}

	/**
	 * Adds the given {@link ExtendedProcessEditor} listener.
	 */
	public void addExtendedProcessEditor(final ExtendedProcessEditor p) {
		processModel.addProcessEditor(p);
	}

	/**
	 * @deprecated use {@link #removeExtendedProcessEditor(ExtendedProcessEditor)} instead.
	 */
	@Deprecated
	public void removeProcessEditor(final ProcessEditor p) {
		processModel.removeProcessEditor(p);
	}

	/**
	 * Removes the given {@link ExtendedProcessEditor} listener.
	 */
	public void removeExtendedProcessEditor(final ExtendedProcessEditor p) {
		processModel.removeProcessEditor(p);
	}

	public void addProcessStorageListener(final ProcessStorageListener listener) {
		processModel.addProcessStorageListener(listener);
	}

	public void removeProcessStorageListener(final ProcessStorageListener listener) {
		processModel.removeProcessStorageListener(listener);
	}

	/**
	 * Selects and shows a single given operator. Will switch the displayed chain either to the
	 * parent or the selected chain, depending on the provided flag. This can be used to easily
	 * update the view. Convenience method.
	 *
	 * @param currentlySelected
	 * @param showParent
	 * @since 7.5
	 * @see #selectOperators(List)
	 * @see #selectOperator(Operator)
	 */
	public void selectAndShowOperator(Operator currentlySelected, boolean showParent) {
		if (currentlySelected == null) {
			currentlySelected = getProcess().getRootOperator();
		}
		OperatorChain parent = currentlySelected.getParent();
		// if this is not a chain, it can not be displayed as such!
		showParent |= !(currentlySelected instanceof OperatorChain);
		// root chain has no parent
		showParent &= parent != null;
		OperatorChain dispChain = showParent ? parent : (OperatorChain) currentlySelected;
		processModel.setDisplayedChainAndFire(dispChain);
		selectOperators(Collections.singletonList(currentlySelected));
	}

	public void selectOperator(Operator currentlySelected) {
		if (currentlySelected == null) {
			currentlySelected = getProcess().getRootOperator();
		}
		selectOperators(Collections.singletonList(currentlySelected));
	}

	public void selectOperators(List<Operator> currentlySelected) {
		if (currentlySelected == null) {
			currentlySelected = Collections.<Operator> singletonList(getProcess().getRootOperator());
		}
		for (Operator op : currentlySelected) {
			Process selectedProcess = op.getProcess();
			if (selectedProcess == null || selectedProcess != getProcess()) {
				SwingTools.showVerySimpleErrorMessage("op_deleted", op.getName());
				return;
			}
		}

		processModel.clearOperatorSelection();
		processModel.addOperatorsToSelection(currentlySelected);
		processModel.fireOperatorSelectionChanged(currentlySelected);
	}

	public void fireProcessUpdated() {
		processModel.fireProcessUpdated();
	}

	public DockingDesktop getDockingDesktop() {
		return dockingDesktop;
	}

	/**
	 * @deprecated use {@link #getPerspectiveController()} instead
	 */
	@Deprecated
	public Perspectives getPerspectives() {
		return perspectives;
	}

	public PerspectiveController getPerspectiveController() {
		return perspectiveController;
	}

	public void handleBrokenProxessXML(final ProcessLocation location, final String xml, final Exception e) {
		SwingTools.showSimpleErrorMessage("while_loading", e, location.toString(), e.getMessage());
		Process process = new Process();
		process.setProcessLocation(location);
		setProcess(process, true);
		perspectiveController.showPerspective(PerspectiveModel.DESIGN);
		xmlEditor.setText(xml);
	}

	public OperatorDocumentationBrowser getOperatorDocViewer() {
		return operatorDocumentationBrowser;
	}

	public ProcessPanel getProcessPanel() {
		return processPanel;
	}

	public void registerDockable(final Dockable dockable) {
		dockingDesktop.registerDockable(dockable);
	}

	public void processHasBeenSaved() {
		processModel.processHasBeenSaved();
	}

	public ProcessContextProcessEditor getProcessContextEditor() {
		return processContextEditor;
	}

	public RepositoryBrowser getRepositoryBrowser() {
		return repositoryBrowser;
	}

	public Component getXMLEditor() {
		return xmlEditor;
	}

	/**
	 * This returns the file menu to change menu entries
	 */
	public JMenu getFileMenu() {
		return fileMenu;
	}

	/**
	 * This returns the connections menu to change menu entries
	 */
	public JMenu getConnectionsMenu() {
		return connectionsMenu;
	}

	/**
	 * This returns the legacy connections menu to change menu entries
	 * @since 9.3
	 */
	public JMenu getLegacyConnectionsMenu() {
		return legacyConnectionsMenu;
	}

	/**
	 * This returns the settings menu to change menu entries.
	 *
	 * @deprecated the tools menu was split into multiple menus. Use {@link #getConnectionsMenu()}
	 *             or {@link #getSettingsMenu()} instead
	 */
	@Deprecated
	public JMenu getToolsMenu() {
		return settingsMenu;
	}

	/**
	 * This returns the settings menu to change menu entries
	 *
	 * @return the settings menu
	 */
	public JMenu getSettingsMenu() {
		return settingsMenu;
	}

	/**
	 * This returns the edit menu to change menu entries
	 */
	public JMenu getEditMenu() {
		return editMenu;
	}

	/**
	 * This returns the process menu to change menu entries
	 */

	public JMenu getProcessMenu() {
		return processMenu;
	}

	/**
	 * This returns the help menu to change menu entries
	 */
	public JMenu getHelpMenu() {
		return helpMenu;
	}

	/**
	 * This returns the extensions menu to change menu entries
	 *
	 * @since 7.0.0
	 */
	public JMenu getExtensionsMenu() {
		return extensionsMenu;
	}

	public DockableMenu getDockableMenu() {
		return dockableMenu;
	}

	/**
	 *
	 * @return the toolbar containing e.g. process run buttons
	 */
	public JToolBar getButtonToolbar() {
		return buttonToolbar;
	}

	/**
	 * The {@link com.rapidminer.search.GlobalSearchManager} for {@link ResourceAction}s.
	 *
	 * @return the manager to add resource actions to the Global Search.
	 * @since 8.1
	 */
	public ActionsGlobalSearchManager getActionsGlobalSearchManager() {
		return actionsGlobalSearchManager;
	}

	/**
	 * Prepare adding menu actions to global search. Calling multiple times has no effect.
	 * @since 8.1
	 */
	protected void initActionsGlobalSearch() {
		if (actionsGlobalSearchManager == null) {
			actionsGlobalSearchManager = (ActionsGlobalSearchManager) new ActionsGlobalSearch().getSearchManager();
		}
	}

	/**
	 * The {@link TutorialSelector} holds the selected {@link Tutorial}.
	 *
	 * @return the registered tutorial selector
	 * @since 7.0.0
	 */
	public TutorialSelector getTutorialSelector() {
		return tutorialSelector;
	}

	/**
	 * Checks the current process for potential problems. If a problem is deemed big enough (e.g. an
	 * operator that requires input but is not connected), returns {@code true}. If no showstoppers
	 * are found, returns {@code false}. This method also alerts the user about the problems so
	 * after it returns, nothing else needs to be done.
	 *
	 * @return {@code true} if the process contains a problem which should prevent process
	 *         execution; {@code false} otherwise
	 */
	private boolean doesProcessContainShowstoppers() {
		// prevent two bubbles on top of each other
		getProcessPanel().getOperatorWarningHandler().killWarningBubble();

		// if any operator has a mandatory parameter with no value and no default value. As it
		// cannot predict execution behavior (e.g. Branch operators), this may turn up problems
		// which would not occur during process execution
		Process process = getProcess();
		Pair<Operator, ParameterType> missingParamPair = ProcessTools.getOperatorWithoutMandatoryParameter(process);
		if (missingParamPair != null) {
			// if there is already one of these, kill
			if (missingParameterBubble != null) {
				missingParameterBubble.killBubble(true);
			}

			missingParameterBubble = ProcessGUITools.displayPrecheckMissingMandatoryParameterWarning(
					missingParamPair.getFirst(), missingParamPair.getSecond());
			return true;
		}

		// if any port needs data but is not connected. As it cannot predict execution behavior
		// (e.g. Branch operators), this may turn up problems which would not occur during
		// process execution
		Pair<Port, ProcessSetupError> missingInputPort = ProcessTools.getPortWithoutMandatoryConnection(process);
		if (missingInputPort != null) {
			// if there is already one of these, kill
			if (missingInputBubble != null) {
				missingInputBubble.killBubble(true);
			}

			missingInputBubble = ProcessGUITools.displayPrecheckInputPortDisconnectedWarning(missingInputPort);
			return true;
		}

		// if there is already one of these, kill
		if (noResultConnectionBubble != null) {
			noResultConnectionBubble.killBubble(true);
		}

		// if the process has no connected result ports and the last executed
		// process root child operator does not prevent a warning bubble we need
		// to notify the user that no output port is connected
		boolean isWarnOnNoResultProcess = Boolean
				.parseBoolean(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_SHOW_NO_RESULT_WARNING));
		if (isWarnOnNoResultProcess) {
			boolean connectedResultPort = ProcessTools.isProcessConnectedToResultPort(process);
			Operator lastExecutedProcessRootChild = ProcessTools.getLastExecutedRootChild(process);
			if (!connectedResultPort && lastExecutedProcessRootChild != null
					&& !ResultWarningPreventionRegistry.isResultWarningSuppressed(lastExecutedProcessRootChild)) {
				noResultConnectionBubble = ProcessGUITools.displayPrecheckNoResultPortInformation(process);
				return true;
			}
		}

		// no showstopper
		return false;
	}

	/**
	 * Removes duplicated separators from a JMenu
	 *
	 * @param menu the menu
	 */
	private static void removeDuplicatedSeparators(JMenu menu) {
		int separatorCount = 0;
		for (Component component : menu.getMenuComponents()) {
			if (component instanceof JPopupMenu.Separator) {
				separatorCount++;
			} else {
				separatorCount = 0;
			}
			if (separatorCount > 1) {
				menu.remove(component);
			}
		}
	}
}
