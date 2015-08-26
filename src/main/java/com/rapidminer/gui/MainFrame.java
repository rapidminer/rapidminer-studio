/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.EventListenerList;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.ProcessStorageListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExitMode;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.actions.AboutAction;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.AnovaCalculatorAction;
import com.rapidminer.gui.actions.AutoWireAction;
import com.rapidminer.gui.actions.BrowseAction;
import com.rapidminer.gui.actions.ComicAction;
import com.rapidminer.gui.actions.ExitAction;
import com.rapidminer.gui.actions.ExportProcessAction;
import com.rapidminer.gui.actions.ImportProcessAction;
import com.rapidminer.gui.actions.NewAction;
import com.rapidminer.gui.actions.NewPerspectiveAction;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.PauseAction;
import com.rapidminer.gui.actions.PropagateRealMetaDataAction;
import com.rapidminer.gui.actions.RedoAction;
import com.rapidminer.gui.actions.RunAction;
import com.rapidminer.gui.actions.SaveAction;
import com.rapidminer.gui.actions.SaveAsAction;
import com.rapidminer.gui.actions.SettingsAction;
import com.rapidminer.gui.actions.StopAction;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.actions.ToggleExpertModeAction;
import com.rapidminer.gui.actions.UndoAction;
import com.rapidminer.gui.actions.ValidateAutomaticallyAction;
import com.rapidminer.gui.actions.ValidateProcessAction;
import com.rapidminer.gui.actions.export.ShowPrintAndExportDialogAction;
import com.rapidminer.gui.dialog.UnknownParametersInfoDialog;
import com.rapidminer.gui.flow.ErrorTable;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.ProcessUndoManager;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent.OperatorEvent;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.license.LicenseTools;
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
import com.rapidminer.gui.security.PasswordManager;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ProcessGUITools;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.ResultWarningPreventionRegistry;
import com.rapidminer.gui.tools.StatusBar;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.SystemMonitor;
import com.rapidminer.gui.tools.WelcomeScreen;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizard;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizardFactory;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.DataImportWizardRegistry;
import com.rapidminer.gui.tools.dialogs.wizards.dataimport.WizardCreationException;
import com.rapidminer.gui.tools.ioobjectcache.IOObjectCacheViewer;
import com.rapidminer.gui.tools.logging.LogViewer;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.gui.ComicDialog;
import com.rapidminer.gui.tour.comic.states.ComicRenderer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.operator.nio.CSVImportWizard;
import com.rapidminer.operator.nio.ExcelImportWizard;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.template.gui.TemplateView;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.gui.ConfigurableDialog;
import com.rapidminer.tools.container.Pair;
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
 * @author Ingo Mierswa, Simon Fischer, Sebastian Land, Marius Helf
 */
public class MainFrame extends ApplicationFrame implements WindowListener {

	private static final long serialVersionUID = 1L;

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

	/** The property name for &quot;Maximum number of states in the undo list.&quot; */
	public static final String PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE = "rapidminer.gui.undolist.size";

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
	public static final int WELCOME_MODE = 2;

	private final EventListenerList processEditors = new EventListenerList();

	public final transient Action AUTO_WIRE = new AutoWireAction(this, "wire", CompatibilityLevel.PRE_VERSION_5, false,
	        true);
	public final transient Action AUTO_WIRE_RECURSIVELY = new AutoWireAction(this, "wire_recursive",
	        CompatibilityLevel.PRE_VERSION_5, true, true);
	public final transient Action REWIRE = new AutoWireAction(this, "rewire", CompatibilityLevel.PRE_VERSION_5, false,
	        false);
	public final transient Action REWIRE_RECURSIVELY = new AutoWireAction(this, "rewire_recursive",
	        CompatibilityLevel.PRE_VERSION_5, true, false);

	public final transient Action NEW_ACTION = new NewAction(this);
	public final transient Action OPEN_ACTION = new OpenAction();
	public final transient SaveAction SAVE_ACTION = new SaveAction();
	public final transient Action SAVE_AS_ACTION = new SaveAsAction();
	public final transient Action PROPAGATE_REAL_METADATA_ACTION = new PropagateRealMetaDataAction(this);

	public final transient Action IMPORT_CSV_FILE_ACTION = new ResourceAction("import_csv_file") {

		private static final long serialVersionUID = 4632580631996166900L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			CSVImportWizard wizard;
			try {
				wizard = new CSVImportWizard();
				wizard.setVisible(true);
			} catch (OperatorException e1) {
				// should not happen if operator == null
				throw new RuntimeException("Failed to create wizard.", e1);
			}
		}
	};

	public final transient Action IMPORT_EXCEL_FILE_ACTION = new ResourceAction("import_excel_sheet") {

		private static final long serialVersionUID = 975782163819088729L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			try {
				ExcelImportWizard wizard = new ExcelImportWizard();
				wizard.setVisible(true);
			} catch (OperatorException e1) {
				// should not happen if operator == null
				throw new RuntimeException("Failed to create wizard.", e1);
			}
		}
	};

	public final transient Action IMPORT_PROCESS_ACTION = new ImportProcessAction();
	public final transient Action EXPORT_PROCESS_ACTION = new ExportProcessAction();

	// ---------- Export as Image/Print actions -----------------
	public final transient Action EXPORT_ACTION = new ShowPrintAndExportDialogAction(false);

	public final transient Action EXIT_ACTION = new ExitAction(this);

	public final transient RunAction RUN_ACTION = new RunAction(this);
	public final transient Action PAUSE_ACTION = new PauseAction(this);
	public final transient Action STOP_ACTION = new StopAction(this);
	public final transient Action VALIDATE_ACTION = new ValidateProcessAction(this);
	public final transient ToggleAction VALIDATE_AUTOMATICALLY_ACTION = new ValidateAutomaticallyAction();

	private transient JButton runRemoteToolbarButton;

	public final transient Action NEW_PERSPECTIVE_ACTION = new NewPerspectiveAction(this);
	public final transient Action SETTINGS_ACTION = new SettingsAction();
	public final transient ToggleAction TOGGLE_EXPERT_MODE_ACTION = new ToggleExpertModeAction(this);
	public final transient Action COMIC_ACTION = new ComicAction();
	public final transient Action UNDO_ACTION = new UndoAction(this);
	public final transient Action REDO_ACTION = new RedoAction(this);
	public final transient Action ANOVA_CALCULATOR_ACTION = new AnovaCalculatorAction();
	public final transient Action MANAGE_CONFIGURABLES_ACTION = new ResourceAction(true, "manage_configurables") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			ConfigurableDialog dialog = new ConfigurableDialog(process);
			dialog.setVisible(true);
		}
	};

	// --------------------------------------------------------------------------------

	// DOCKING

	public static final DockGroup DOCK_GROUP_ROOT = new DockGroup("root");
	public static final DockGroup DOCK_GROUP_RESULTS = new DockGroup("results");
	private final LinkedList<ProcessStorageListener> storageListeners = new LinkedList<>();
	private final DockingContext dockingContext = new DockingContext();
	private final DockingDesktop dockingDesktop = new DockingDesktop("mainDesktop", dockingContext);

	private final Actions actions = new Actions(this);

	private final WelcomeScreen welcomeScreen = new WelcomeScreen(this);
	private final ResultDisplay resultDisplay = ResultDisplayTools.makeResultDisplay();
	private final LogViewer logViewer = new LogViewer(this);
	private final IOObjectCacheViewer ioobjectCacheViewer = new IOObjectCacheViewer(RapidMiner.getGlobalIOObjectCache());
	private final SystemMonitor systemMonitor = new SystemMonitor();

	private final OperatorDocumentationBrowser operatorDocumentationBrowser = new OperatorDocumentationBrowser();
	private final OperatorTreePanel operatorTree = new OperatorTreePanel(this);
	private final ErrorTable errorTable = new ErrorTable(this);
	private final OperatorPropertyPanel propertyPanel = new OperatorPropertyPanel(this);
	private final XMLEditor xmlEditor = new XMLEditor(this);
	private final ProcessContextProcessEditor processContextEditor = new ProcessContextProcessEditor();
	private final ProcessPanel processPanel = new ProcessPanel(this);
	private final ComicRenderer comicRenderer = new ComicRenderer(processPanel.getProcessRenderer(), this);
	private final NewOperatorEditor newOperatorEditor = new NewOperatorEditor(
	        processPanel.getProcessRenderer().getDragListener());
	private final RepositoryBrowser repositoryBrowser = new RepositoryBrowser(
	        processPanel.getProcessRenderer().getDragListener());
	private final MacroViewer macroViewer = new MacroViewer();

	private final Perspectives perspectives = new Perspectives(dockingContext);

	private boolean changed = false;
	private int undoIndex;

	/** the bubble which displays a warning that no result ports are connected */
	private BubbleWindow noResultConnectionBubble;
	/** the bubble which displays a warning that a port must receive input but is not connected */
	private BubbleWindow missingInputBubble;
	/**
	 * the bubble which displays a warning that a parameter must be set as he has no default value
	 */
	private BubbleWindow missingParameterBubble;

	private final JMenuBar menuBar;

	private final JMenu fileMenu;
	private final JMenu importMenu;

	private final JMenu editMenu;

	private final JMenu processMenu;

	private final JMenu toolsMenu;

	private final JMenu viewMenu;

	private final JMenu helpMenu;

	private DockableMenu dockableMenu;

	private final JMenu recentFilesMenu = new ResourceMenu("recent_files");

	private final ProcessUndoManager undoManager = new ProcessUndoManager();

	/** XML representation of the process at last validation. */
	private String lastProcessXML;
	/** the OperatorChain which was last viewed */
	private OperatorChain lastProcessDisplayedOperatorChain;

	/**
	 * The host name of the system. Might be empty (no host name will be shown) and will be
	 * initialized in the first call of {@link #setTitle()}.
	 */
	private String hostname = null;

	private transient Process process = null;
	private transient ProcessThread processThread;

	private final MetaDataUpdateQueue metaDataUpdateQueue = new MetaDataUpdateQueue(this);

	private transient final DataImportWizardRegistry importWizardRegistry = new DataImportWizardRegistry() {

		private final List<DataImportWizardFactory> factories = new ArrayList<>();

		@Override
		public void register(DataImportWizardFactory factory) {
			if (factory == null) {
				throw new IllegalArgumentException("factory must not be null");
			}
			synchronized (factories) {
				factories.add(factory);
				importMenu.add(factory.createAction());
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

	private long lastUpdate = 0;
	private final Timer updateTimer = new Timer(500, new ActionListener() {

		@Override
		public void actionPerformed(final ActionEvent e) {
			updateProcessNow();
		}
	}) {

		private static final long serialVersionUID = 1L;

		{
			setRepeats(false);
		}
	};

	public void addViewSwitchToUndo() {
		fireProcessViewChanged();
		String xmlWithoutGUIInformation = process.getRootOperator().getXML(true, false);
		if (lastProcessDisplayedOperatorChain != null
		        && processPanel.getProcessRenderer().getModel().getDisplayedChain() != null
		        && !processPanel.getProcessRenderer().getModel().getDisplayedChain().getName()
		                .equals(lastProcessDisplayedOperatorChain.getName())) {
			addToUndoList(xmlWithoutGUIInformation, true);
		}
		lastProcessXML = xmlWithoutGUIInformation;
		lastProcessDisplayedOperatorChain = processPanel.getProcessRenderer().getModel().getDisplayedChain();
	}

	private void updateProcessNow() {
		lastUpdate = System.currentTimeMillis();
		String xmlWithoutGUIInformation = process.getRootOperator().getXML(true, false);
		if (!xmlWithoutGUIInformation.equals(lastProcessXML)) {
			addToUndoList(xmlWithoutGUIInformation, false);
			validateProcess(false);
		}
		processPanel.getProcessRenderer().repaint();
		lastProcessXML = xmlWithoutGUIInformation;
		lastProcessDisplayedOperatorChain = processPanel.getProcessRenderer().getModel().getDisplayedChain();
	}

	public void validateProcess(final boolean force) {
		if (force || process.getProcessState() != Process.PROCESS_STATE_RUNNING) {
			metaDataUpdateQueue.validate(process, force);
		}
		fireProcessUpdated();
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
				if (process.getProcessState() == Process.PROCESS_STATE_RUNNING) {
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
			if (process.equals(MainFrame.this.process)) {
				RUN_ACTION.setState(process.getProcessState());
				ProcessThread.beep("breakpoint");
				MainFrame.this.toFront();
				resultDisplay.showData(ioContainer,
	                    "Breakpoint in " + operator.getName() + ", application " + operator.getApplyCount());
			}
		}

		/** Since the mainframe triggers the resume itself this method does nothing. */
		@Override
		public void resume() {
			RUN_ACTION.setState(process.getProcessState());
		}
	};

	private JToolBar buttonToolbar;

	// --------------------------------------------------------------------------------

	/** Creates a new main frame containing the RapidMiner GUI. */
	public MainFrame() {
		this("welcome");
	}

	public MainFrame(final String initialPerspective) {
		super(getFrameTitle());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
			// load dock icon from resources and adapt UI via OSXAdapter class
			try {
				OSXQuitListener quitListener = new OSXQuitListener() {

					@Override
					public void quit() {
						RapidMiner.quit(ExitMode.NORMAL);
					}
				};
				OSXAdapter.adaptUI(this, SETTINGS_ACTION, new AboutAction(this), quitListener);
			} catch (Throwable t) {
				// catch everything - in case the OSX adapter is called without being on a OS X
				// system
				// or the Java classes have been removed from OS X JRE it will just log an error
				// instead of breaking the program start-up
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.MainFrame.could_not_adapt_OSX_look_and_feel", t);
			}
		}

		addProcessEditor(actions);
		addProcessEditor(xmlEditor);
		addProcessEditor(propertyPanel);
		addProcessEditor(operatorTree);
		addProcessEditor(operatorDocumentationBrowser);
		addProcessEditor(processPanel);
		addProcessEditor(errorTable);
		addProcessEditor(processContextEditor);
		addProcessEditor(getStatusBar());
		addProcessEditor(resultDisplay);
		addProcessEditor(macroViewer);

		SwingTools.setFrameIcon(this);

		dockingContext.addDesktop(dockingDesktop);
		dockingDesktop.registerDockable(welcomeScreen);
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

		TemplateView templateView = new TemplateView(this);
		templateView.getDockKey().setCloseEnabled(false);
		templateView.getDockKey().setAutoHideEnabled(false);
		dockingDesktop.registerDockable(templateView);
		// Test

		ToolBarContainer toolBarContainer = ToolBarContainer.createDefaultContainer(true, true, true, true);
		getContentPane().add(toolBarContainer, BorderLayout.CENTER);
		toolBarContainer.add(dockingDesktop, BorderLayout.CENTER);

		systemMonitor.startMonitorThread();
		resultDisplay.getDockKey().setCloseEnabled(false);
		resultDisplay.getDockKey().setAutoHideEnabled(false);
		resultDisplay.init(this);

		// menu bar
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		fileMenu = new ResourceMenu("file");
		fileMenu.add(NEW_ACTION);
		fileMenu.add(OPEN_ACTION);
		updateRecentFileList();
		fileMenu.add(recentFilesMenu);
		fileMenu.addSeparator();
		fileMenu.add(SAVE_ACTION);
		fileMenu.add(SAVE_AS_ACTION);
		fileMenu.addSeparator();
		importMenu = new ResourceMenu("file.import");
		fileMenu.add(importMenu);
		fileMenu.add(IMPORT_PROCESS_ACTION);
		fileMenu.add(EXPORT_PROCESS_ACTION);
		menuBar.add(fileMenu);

		importWizardRegistry.register(new DataImportWizardFactory() {

			@Override
			public DataImportWizard createWizard() throws WizardCreationException {
				try {
					return new CSVImportWizard();
				} catch (OperatorException e) {
					// should not happen if operator == null
					throw new WizardCreationException(e);
				}
			}

			@Override
			public Action createAction() {
				return IMPORT_CSV_FILE_ACTION;
			}
		});

		importWizardRegistry.register(new DataImportWizardFactory() {

			@Override
			public DataImportWizard createWizard() throws WizardCreationException {
				try {
					return new ExcelImportWizard();
				} catch (OperatorException e) {
					// should not happen if operator == null
					throw new WizardCreationException(e);
				}
			}

			@Override
			public Action createAction() {
				return IMPORT_EXCEL_FILE_ACTION;
			}
		});

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
		// editMenu.add(actions.MAKE_DIRTY_ACTION);
		menuBar.add(editMenu);

		// process menu
		processMenu = new ResourceMenu("process");
		processMenu.add(RUN_ACTION);
		processMenu.add(PAUSE_ACTION);
		processMenu.add(STOP_ACTION);
		processMenu.addSeparator();
		processMenu.add(VALIDATE_ACTION);
		processMenu.add(VALIDATE_AUTOMATICALLY_ACTION.createMenuItem());
		processMenu.addSeparator();

		JMenu wiringMenu = new ResourceMenu("wiring");
		wiringMenu.add(AUTO_WIRE);
		wiringMenu.add(AUTO_WIRE_RECURSIVELY);
		wiringMenu.add(REWIRE);
		wiringMenu.add(REWIRE_RECURSIVELY);
		processMenu.add(wiringMenu);
		JMenu orderMenu = new ResourceMenu("execution_order");
		orderMenu.add(processPanel.getFlowVisualizer().ALTER_EXECUTION_ORDER.createMenuItem());
		orderMenu.add(processPanel.getFlowVisualizer().SHOW_EXECUTION_ORDER);
		processMenu.add(orderMenu);
		JMenu layoutMenu = new ResourceMenu("process_layout");
		layoutMenu.add(processPanel.getProcessRenderer().getArrangeOperatorsAction());
		layoutMenu.add(processPanel.getProcessRenderer().getAutoFitAction());
		processMenu.add(layoutMenu);
		menuBar.add(processMenu);

		// tools menu
		toolsMenu = new ResourceMenu("tools");
		toolsMenu.add(ANOVA_CALCULATOR_ACTION);
		menuBar.add(toolsMenu);

		// view menu
		viewMenu = new ResourceMenu("view");
		viewMenu.add(perspectives.getWorkspaceMenu());
		viewMenu.add(NEW_PERSPECTIVE_ACTION);
		viewMenu.add(dockableMenu = new DockableMenu(dockingContext));
		viewMenu.add(perspectives.RESTORE_DEFAULT_ACTION);
		viewMenu.addSeparator();
		viewMenu.add(TOGGLE_EXPERT_MODE_ACTION.createMenuItem());
		menuBar.add(viewMenu);

		// help menu
		helpMenu = new ResourceMenu("help");
		helpMenu.add(COMIC_ACTION);
		helpMenu.add(new BrowseAction("help_documentation",
		        URI.create("http://redirects.rapidminer.com/app/studio/6/documentation")));
		helpMenu.add(new BrowseAction("help_forum", URI.create("http://forum.rapidminer.com")));

		buttonToolbar = new ExtendedJToolBar(true);

		buttonToolbar.add(makeToolbarButton(NEW_ACTION));
		buttonToolbar.add(makeToolbarButton(OPEN_ACTION));
		buttonToolbar.add(makeToolbarButton(SAVE_ACTION));
		buttonToolbar.add(makeToolbarButton(SAVE_AS_ACTION));
		buttonToolbar.add(makeToolbarButton(EXPORT_ACTION));
		buttonToolbar.addSeparator();

		buttonToolbar.add(makeToolbarButton(UNDO_ACTION));
		buttonToolbar.add(makeToolbarButton(REDO_ACTION));
		buttonToolbar.addSeparator();

		// create run remote button and disable it by default
		runRemoteToolbarButton = makeToolbarButton(new ResourceActionAdapter("run_remote_now"));
		runRemoteToolbarButton.setEnabled(false);

		buttonToolbar.add(runRemoteToolbarButton);
		buttonToolbar.add(makeToolbarButton(RUN_ACTION));
		buttonToolbar.add(makeToolbarButton(PAUSE_ACTION));
		buttonToolbar.add(makeToolbarButton(STOP_ACTION));
		buttonToolbar.addSeparator();

		DropDownButton comicDropdown = ComicDialog.makeDropDownButton();
		comicDropdown.setUsePopupActionOnMainButton();
		buttonToolbar.add(Box.createHorizontalGlue());
		comicDropdown.addToToolBar(buttonToolbar);

		JPanel toolbarPanel = new JPanel(new BorderLayout());
		toolbarPanel.add(buttonToolbar, BorderLayout.WEST);
		toolbarPanel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
		toolbarPanel.add(PerspectivesPanelBar.getPerspecitvesPanelBar(perspectives), BorderLayout.EAST);
		toolbarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		getContentPane().add(toolbarPanel, BorderLayout.NORTH);
		getContentPane().add(getStatusBar(), BorderLayout.SOUTH);
		getStatusBar().startClockThread();

		// listen for selection changes in the ProcessRendererView and notify all registered process
		// editors
		processPanel.getProcessRenderer().getModel().registerEventListener(new ProcessRendererEventListener() {

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				// ignore
			}

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				if (e.getEventType() == OperatorEvent.SELECTED_OPERATORS_CHANGED) {
					for (ProcessEditor editor : processEditors.getListeners(ProcessEditor.class)) {
						editor.setSelection(new LinkedList<Operator>(operators));
					}
					for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
						editor.setSelection(new LinkedList<Operator>(operators));
					}
				}
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				// ignore
			}
		});

		setProcess(new Process(), true);
		selectOperator(process.getRootOperator());
		addToUndoList();

		perspectives.showPerspective(initialPerspective);
		pack();
		metaDataUpdateQueue.start();
	}

	/**
	 * Finishes the MainFrame initialization. Should be called after all extension have been
	 * initialized.
	 */
	public void finishInitialization() {

		// Configurators (if they exist)
		if (!ConfigurationManager.getInstance().isEmpty()) {
			toolsMenu.addSeparator();
			toolsMenu.add(MANAGE_CONFIGURABLES_ACTION);
		}

		toolsMenu.addSeparator();

		// Password Manager
		toolsMenu.add(PasswordManager.OPEN_WINDOW);
		if (SystemInfoUtilities.getOperatingSystem() != OperatingSystem.OSX || !OSXAdapter.isAdapted()) {
			toolsMenu.add(SETTINGS_ACTION);
		}

		// add export and exit as last file menu actions
		fileMenu.addSeparator();
		fileMenu.add(EXPORT_ACTION);
		fileMenu.addSeparator();
		fileMenu.add(EXIT_ACTION);

		// ensure "About RapidMiner Studio" action is added as last action
		if (SystemInfoUtilities.getOperatingSystem() != OperatingSystem.OSX || !OSXAdapter.isAdapted()) {
			helpMenu.addSeparator();
			helpMenu.add(new AboutAction(this));
		}

		// Add Help menu as last entry
		menuBar.add(helpMenu);
	}

	private JButton makeToolbarButton(final Action action) {
		JButton button = new JButton(action);
		if (button.getIcon() != null) {
			button.setText(null);
		}
		return button;
	}

	public void setExpertMode(final boolean expert) {
		TOGGLE_EXPERT_MODE_ACTION.setSelected(expert);
		TOGGLE_EXPERT_MODE_ACTION.actionToggled(null);
	}

	public OperatorPropertyPanel getPropertyPanel() {
		return propertyPanel;
	}

	/**
	 * Returns a registry for {@link DataImportWizardFactory} instances. The factories are used to
	 * populate menus such as the main import menu.
	 *
	 * @return the registry
	 */
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

	public WelcomeScreen getWelcomeScreen() {
		return welcomeScreen;
	}

	/**
	 * @return the toolbar button for running processes on the Server
	 */
	public JButton getRunRemoteToolbarButton() {
		return runRemoteToolbarButton;
	}

	public int getProcessState() {
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
		return this.process;
	}

	// ====================================================
	// M A I N A C T I O N S
	// ===================================================

	/** Creates a new process. */
	public void newProcess() {
		// ask for confirmation before stopping the currently running process and opening a new one!
		if (getProcessState() == Process.PROCESS_STATE_RUNNING || getProcessState() == Process.PROCESS_STATE_PAUSED) {
			if (SwingTools.showConfirmDialog("close_running_process",
			        ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.NO_OPTION) {
				return;
			}
		}

		ProgressThread newProcessThread = new ProgressThread("new_process") {

			@Override
			public void run() {
				if (close(false)) {
					// process changed -> clear undo history
					resetUndo();

					stopProcess();
					changed = false;
					setProcess(new Process(), true);
					addToUndoList();
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
		newProcessThread.setIndeterminate(true);
		newProcessThread.setCancelable(false);
		newProcessThread.start();
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

			processThread = new ProcessThread(MainFrame.this.process);
			try {
				processThread.start();
			} catch (Exception t) {
				SwingTools.showSimpleErrorMessage("cannot_start_process", t);
			}
		} else {
			process.resume();
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
		if (process.equals(MainFrame.this.process)) {
			if (results != null) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						MainFrame.this.toFront();
					}
				});
			}
		}
		if (process.equals(MainFrame.this.process)) {
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
	 * Sets a new process and registers the MainFrame's listeners.
	 */
	public void setProcess(final Process process, final boolean newProcess) {
		setProcess(process, newProcess, false);
	}

	/**
	 * Sets a new process and registers the MainFrame's listeners.
	 */
	public void setProcess(final Process process, final boolean newProcess, final boolean addToUndoList) {
		boolean firstProcess = this.process == null;
		if (this.process != null) {
			// this.process.getRootOperator().removeObserver(processObserver);
			this.process.removeObserver(processObserver);
		}

		if (getProcessState() != Process.PROCESS_STATE_STOPPED) {
			if (processThread != null) {
				processThread.stopProcess();
			}
		}

		if (process != null) {
			// process.getRootOperator().addObserver(processObserver, true);
			process.addObserver(processObserver, true);

			synchronized (process) {
				this.process = process;
				this.processThread = new ProcessThread(this.process);
				this.process.addBreakpointListener(breakpointListener);
				if (addToUndoList) {
					addToUndoList(process.getRootOperator().getXML(true, false), false);
				}
				fireProcessChanged();
				processPanel.getProcessRenderer().getModel().setDisplayedChain(this.getProcess().getRootOperator());
				processPanel.getProcessRenderer().getModel().fireDisplayedChainChanged();
				selectOperator(this.process.getRootOperator());
				if (VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
					validateProcess(false);
				}
			}
		}
		if (newProcess && !firstProcess) {
			// VLDocking appears to get nervous when applying two perspectives while the
			// window is not yet visible. So to avoid that we set design and then welcome
			// during startup, avoid applying design if this is the first process we create.
			perspectives.showPerspective(Perspectives.DESIGN);
		}
		setTitle();
		getStatusBar().setTrafficLight(StatusBar.TRAFFIC_LIGHT_INACTIVE);
		getStatusBar().clearSpecialText();
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
		return changed;
	}

	private boolean addToUndoList() {
		return addToUndoList(null, false);
	}

	/**
	 * Adds the current state of the process to the undo list.
	 *
	 * Note: This method must not be exposed by making it public. It may confuse the MainFrame such
	 * that it can no longer determine correctly whether validation is possible.
	 *
	 * @return true if process really differs.
	 */
	private boolean addToUndoList(String currentStateXML, final boolean viewSwitch) {
		String lastStateXML = null;
		if (undoManager.getNumberOfUndos() != 0) {
			lastStateXML = undoManager.getXml(undoManager.getNumberOfUndos() - 1);
		}

		if (currentStateXML == null) {
			currentStateXML = this.process.getRootOperator().getXML(true);
		}
		if (currentStateXML != null) {
			// mark as changed only if the XML has changed
			if (lastStateXML == null || !lastStateXML.equals(currentStateXML) || viewSwitch) {
				if (undoIndex < undoManager.getNumberOfUndos() - 1) {
					while (undoManager.getNumberOfUndos() > undoIndex + 1) {
						undoManager.removeLast();
					}
				}
				undoManager.add(currentStateXML, getProcessPanel().getProcessRenderer().getModel().getDisplayedChain(),
				        getFirstSelectedOperator());
				String maxSizeProperty = ParameterService.getParameterValue(PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE);
				int maxSize = 20;
				try {
					if (maxSizeProperty != null) {
						maxSize = Integer.parseInt(maxSizeProperty);
					}
				} catch (NumberFormatException e) {
					LogService.getRoot().warning("com.rapidminer.gui.main_frame_warning");
				}
				while (undoManager.getNumberOfUndos() > maxSize) {
					undoManager.removeFirst();
				}
				undoIndex = undoManager.getNumberOfUndos() - 1;
				enableUndoAction();

				boolean oldChangedValue = MainFrame.this.changed;
				// mark as changed only if the XML has changed
				if (currentStateXML.equals(lastStateXML)) {
					return false;
				}

				MainFrame.this.changed = lastStateXML != null;

				if (!oldChangedValue) {
					setTitle();
				}
				if (MainFrame.this.process.getProcessLocation() != null) {
					MainFrame.this.SAVE_ACTION.setEnabled(true);
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void undo() {
		if (undoIndex > 0) {
			undoIndex--;
			setProcessIntoStateAt(undoIndex, true);
		}
		enableUndoAction();
	}

	public void redo() {
		if (undoIndex < undoManager.getNumberOfUndos()) {
			undoIndex++;
			setProcessIntoStateAt(undoIndex, false);
		}
		enableUndoAction();
	}

	private void enableUndoAction() {
		if (undoIndex > 0) {
			UNDO_ACTION.setEnabled(true);
		} else {
			UNDO_ACTION.setEnabled(false);
		}
		if (undoIndex < undoManager.getNumberOfUndos() - 1) {
			REDO_ACTION.setEnabled(true);
		} else {
			REDO_ACTION.setEnabled(false);
		}
	}

	/**
	 * Returns <code>true</code> if the current process has undo steps available.
	 *
	 * @return
	 */
	public boolean hasUndoSteps() {
		return undoIndex > 0;
	}

	/**
	 * Returns <code>true</code> if the current process has redo steps available.
	 *
	 * @return
	 */
	public boolean hasRedoSteps() {
		return undoIndex < undoManager.getNumberOfUndos() - 1;
	}

	private void setProcessIntoStateAt(final int undoIndex, final boolean undo) {
		String stateXML = undoManager.getXml(undoIndex);
		OperatorChain shownOperatorChain = null;
		if (undo) {
			shownOperatorChain = undoManager.getOperatorChain(undoIndex);
		} else {
			shownOperatorChain = undoManager.getOperatorChain(undoIndex);
		}
		Operator selectedOperator = undoManager.getSelectedOperator(undoIndex);
		try {
			synchronized (process) {
				String oldXml = process.getRootOperator().getXML(true);
				Process process = new Process(stateXML, this.process);
				// this.process.setupFromXML(stateXML);
				setProcess(process, false);
				// cannot use method processChanged() because this would add the
				// old state to the undo stack!
				if (!stateXML.equals(oldXml)) {
					this.changed = true;
					setTitle();
					if (this.process.getProcessLocation() != null) {
						this.SAVE_ACTION.setEnabled(true);
					}
				}

				// restore selected operator
				if (selectedOperator != null) {
					Operator restoredOperator = getProcess().getOperator(selectedOperator.getName());
					if (restoredOperator != null) {
						selectOperator(restoredOperator);
					}
				}

				// restore process panel view on correct subprocess on undo
				if (shownOperatorChain != null) {
					OperatorChain restoredOperatorChain = (OperatorChain) getProcess()
					        .getOperator(shownOperatorChain.getName());
					processPanel.getProcessRenderer().getModel().setDisplayedChain(restoredOperatorChain);
					processPanel.getProcessRenderer().getModel().fireDisplayedChainChanged();
				}
			}
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("while_changing_process", e);
		}

		lastProcessDisplayedOperatorChain = getProcessPanel().getProcessRenderer().getModel().getDisplayedChain();
		lastProcessXML = process.getRootOperator().getXML(true, false);
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

		if (this.process != null) {
			ProcessLocation loc = process.getProcessLocation();
			if (loc != null) {
				String locString = loc.toString();
				// location string exceeding arbitrary number will be cut into repository name +
				// /.../ + process name
				if (locString.length() > MAX_LOCATION_TITLE_LENGTH) {
					locString = RepositoryLocation.REPOSITORY_PREFIX + process.getRepositoryLocation().getRepositoryName()
					        + RepositoryLocation.SEPARATOR + "..." + RepositoryLocation.SEPARATOR + loc.getShortName();
				}
				setTitle(locString + (changed ? "*" : "") + " \u2013 " + getFrameTitle() + hostname);
			} else {
				setTitle("<new process" + (changed ? "*" : "") + "> \u2013 " + getFrameTitle() + hostname);
			}
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
		if (changed) {
			ProcessLocation loc = process.getProcessLocation();
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
							        ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.NO_OPTION) {
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
		} else {
			return true;
		}
	}

	public boolean close() {
		return close(true);
	}

	public void setOpenedProcess(final Process process, final boolean showInfo, final String sourceName) {
		setProcess(process, true);

		if (process.getImportMessage() != null && process.getImportMessage().contains("error")) {
			SwingTools.showLongMessage("import_message", process.getImportMessage());
		}

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				SAVE_ACTION.setEnabled(false);
			}
		});

		// process changed -> clear undo history
		resetUndo();

		List<UnknownParameterInformation> unknownParameters = null;
		synchronized (process) {
			RapidMinerGUI.useProcessFile(MainFrame.this.process);
			unknownParameters = process.getUnknownParameters();
		}

		addToUndoList();
		updateRecentFileList();
		changed = false;

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				SAVE_ACTION.setEnabled(false);
				setTitle();
			}
		});

		// show unsupported parameters info?
		if (unknownParameters != null && unknownParameters.size() > 0) {
			final UnknownParametersInfoDialog unknownParametersInfoDialog = new UnknownParametersInfoDialog(MainFrame.this,
			        unknownParameters);
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
		fireProcessLoaded();
	}

	private void resetUndo() {
		undoIndex = 0;
		undoManager.reset();
		enableUndoAction();
	}

	public void exit(final boolean relaunch) {
		if (changed) {
			ProcessLocation loc = process.getProcessLocation();
			String locName;
			if (loc != null) {
				locName = loc.getShortName();
			} else {
				locName = "unnamed";
			}
			switch (SwingTools.showConfirmDialog("save", ConfirmDialog.YES_NO_CANCEL_OPTION, locName)) {
				case ConfirmDialog.YES_OPTION:
					SaveAction.save(process);
					if (changed) {
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
		dispose();
		RapidMiner.quit(relaunch ? RapidMiner.ExitMode.RELAUNCH : RapidMiner.ExitMode.NORMAL);
	}

	/** Updates the list of recently used files. */
	public void updateRecentFileList() {
		recentFilesMenu.removeAll();
		List<ProcessLocation> recentFiles = RapidMinerGUI.getRecentFiles();
		int j = 1;
		for (final ProcessLocation recentLocation : recentFiles) {
			JMenuItem menuItem = new JMenuItem(j + " " + recentLocation.toMenuString());
			menuItem.setMnemonic('0' + j);
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(final ActionEvent e) {
					if (RapidMinerGUI.getMainFrame().close()) {
						OpenAction.open(recentLocation, true);
					}
				}
			});
			recentFilesMenu.add(menuItem);
			j++;
		}
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

	public void addMenu(final int menuIndex, final JMenu menu) {
		menuBar.add(menu, menuIndex);
	}

	public void addMenuSeparator(final int menuIndex) {
		menuBar.getMenu(menuIndex).addSeparator();
	}

	// / LISTENERS

	public List<Operator> getSelectedOperators() {
		return processPanel.getProcessRenderer().getModel().getSelectedOperators();
	}

	public Operator getFirstSelectedOperator() {
		return processPanel.getProcessRenderer().getModel().getSelectedOperators().isEmpty() ? null
		        : processPanel.getProcessRenderer().getModel().getSelectedOperators().get(0);
	}

	/**
	 * @deprecated use {@link #addExtendedProcessEditor(ExtendedProcessEditor)} instead.
	 */
	@Deprecated
	public void addProcessEditor(final ProcessEditor p) {
		processEditors.add(ProcessEditor.class, p);
	}

	/**
	 * Adds the given {@link ExtendedProcessEditor} listener.
	 *
	 * @param p
	 */
	public void addExtendedProcessEditor(final ExtendedProcessEditor p) {
		processEditors.add(ExtendedProcessEditor.class, p);
	}

	/**
	 * @deprecated use {@link #removeExtendedProcessEditor(ExtendedProcessEditor)} instead.
	 */
	@Deprecated
	public void removeProcessEditor(final ProcessEditor p) {
		processEditors.remove(ProcessEditor.class, p);
	}

	/**
	 * Removes the given {@link ExtendedProcessEditor} listener.
	 *
	 * @param p
	 */
	public void removeExtendedProcessEditor(final ExtendedProcessEditor p) {
		processEditors.remove(ExtendedProcessEditor.class, p);
	}

	public void addProcessStorageListener(final ProcessStorageListener listener) {
		storageListeners.add(listener);
	}

	public void removeProcessStorageListener(final ProcessStorageListener listener) {
		storageListeners.remove(listener);
	}

	public void selectOperator(Operator currentlySelected) {
		if (currentlySelected == null) {
			currentlySelected = process.getRootOperator();
		}
		selectOperators(Collections.singletonList(currentlySelected));
	}

	public void selectOperators(List<Operator> currentlySelected) {
		if (currentlySelected == null) {
			currentlySelected = Collections.<Operator> singletonList(process.getRootOperator());
		}
		for (Operator op : currentlySelected) {
			Process selectedProcess = op.getProcess();
			if (selectedProcess == null || selectedProcess != process) {
				SwingTools.showVerySimpleErrorMessage("op_deleted", op.getName());
				return;
			}
		}

		ProcessRendererModel model = processPanel.getProcessRenderer().getModel();
		model.clearOperatorSelection();
		model.addOperatorsToSelection(currentlySelected);
		model.fireOperatorSelectionChanged(currentlySelected);
	}

	public void fireProcessUpdated() {
		for (ProcessEditor editor : processEditors.getListeners(ProcessEditor.class)) {
			editor.processUpdated(process);
		}
		for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
			editor.processUpdated(process);
		}
	}

	/**
	 * Fire this when the process view has changed, e.g. when the user enters/leaves a subprocess in
	 * the process design panel.
	 */
	private void fireProcessViewChanged() {
		for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
			editor.processViewChanged(process);
		}
	}

	private void fireProcessChanged() {
		for (ProcessEditor editor : processEditors.getListeners(ProcessEditor.class)) {
			editor.processChanged(process);
		}
		for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
			editor.processChanged(process);
		}
	}

	private void fireProcessLoaded() {
		LinkedList<ProcessStorageListener> list = new LinkedList<>(storageListeners);
		for (ProcessStorageListener l : list) {
			l.opened(process);
		}
	}

	private void fireProcessStored() {
		LinkedList<ProcessStorageListener> list = new LinkedList<>(storageListeners);
		for (ProcessStorageListener l : list) {
			l.stored(process);
		}
	}

	public DockingDesktop getDockingDesktop() {
		return dockingDesktop;
	}

	public Perspectives getPerspectives() {
		return perspectives;
	}

	public void handleBrokenProxessXML(final ProcessLocation location, final String xml, final Exception e) {
		SwingTools.showSimpleErrorMessage("while_loading", e, location.toString(), e.getMessage());
		Process process = new Process();
		process.setProcessLocation(location);
		setProcess(process, true);
		perspectives.showPerspective(Perspectives.DESIGN);
		xmlEditor.setText(xml);
	}

	public OperatorDocumentationBrowser getOperatorDocViewer() {
		return operatorDocumentationBrowser;
	}

	public ProcessPanel getProcessPanel() {
		return processPanel;
	}

	public ComicRenderer getComicRenderer() {
		return comicRenderer;
	}

	public void registerDockable(final Dockable dockable) {
		dockingDesktop.registerDockable(dockable);
	}

	public void processHasBeenSaved() {
		SAVE_ACTION.setEnabled(false);
		changed = false;
		setTitle();
		updateRecentFileList();
		fireProcessStored();
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
	 * This returns the tools menu to change menu entries
	 */

	public JMenu getToolsMenu() {
		return toolsMenu;
	}

	/**
	 * This returns the complete menu bar to insert additional menus
	 */
	public JMenuBar getMainMenuBar() {
		return menuBar;
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

	public DockableMenu getDockableMenu() {
		return dockableMenu;
	}

	/**
	 *
	 * @return the toolbar containg e.g. process run buttons
	 */
	public JToolBar getButtonToolbar() {
		return buttonToolbar;
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
		// if any operator has a mandatory parameter with no value and no default value. As it
		// cannot predict execution behavior (e.g. Branch operators), this may turn up problems
		// which would not occur during process execution
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
		Port missingInputPort = ProcessTools.getPortWithoutMandatoryConnection(process);
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

		// if the process has no connected result ports, the comic isn't active and the last
		// executed process root child operator
		// does not prevent a warning bubble we need to notify the user that no output port is
		// connected
		boolean connectedResultPort = ProcessTools.isProcessConnectedToResultPort(process);
		Operator lastExecutedProcessRootChild = ProcessTools.getLastExecutedRootChild(process);
		if (!ComicManager.getInstance().isEpisodeRunning() && !connectedResultPort
		        && !ResultWarningPreventionRegistry.isResultWarningSuppressed(lastExecutedProcessRootChild)) {
			noResultConnectionBubble = ProcessGUITools.displayPrecheckNoResultPortInformation(process);
			return true;
		}

		// no showstopper
		return false;
	}

}
