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
package com.rapidminer.gui.tools.logging;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import com.rapidminer.Process;
import com.rapidminer.gui.GeneralProcessListener;
import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.dialog.SearchDialog;
import com.rapidminer.gui.dialog.SearchableJTextComponent;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ExtendedStyledDocument;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.components.DropDownPopupButton.PopupMenuProvider;
import com.rapidminer.gui.tools.logging.LogModel.LogMode;
import com.rapidminer.gui.tools.logging.actions.ClearMessageAction;
import com.rapidminer.gui.tools.logging.actions.LogCloseAction;
import com.rapidminer.gui.tools.logging.actions.LogRefreshAction;
import com.rapidminer.gui.tools.logging.actions.LogSearchAction;
import com.rapidminer.gui.tools.logging.actions.SaveLogFileAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * A text area displaying log outputs. The model contains all opened log outputs. One model is
 * displayed. Since keeping all lines might dramatically increase memory usage and slow down
 * RapidMiner, only a maximum number of lines is displayed.
 *
 * @author Ingo Mierswa, Nils Woehler, Sabrina Kirstein, Marco Boeck
 */
public class LogViewer extends JPanel implements Dockable {

	/**
	 * This is the menu for selecting the log level of the current log.
	 */
	private class LogLevelMenu extends ResourceMenu {

		private static final long serialVersionUID = 1L;

		public LogLevelMenu() {
			super("log_level");

			for (final Level level : LogViewer.SELECTABLE_LEVELS) {
				JMenuItem item = new JMenuItem(new LoggedAbstractAction(level.getName()) {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						new Thread(new Runnable() {

							@Override
							public void run() {
								// change the log level outside the EDT
								// no progress thread because the part that may take some time (the
								// GUI refresh by Swing) cannot be cancelled anyway
								setLogLevel(level);
							}
						}).start();
					}
				});

				// highlight current log level
				if (getLogSelectionModel().getCurrentLogModel() != null) {
					if (level.equals(getLogSelectionModel().getCurrentLogModel().getLogLevel())) {
						item.setFont(item.getFont().deriveFont(Font.BOLD));
					}
				}
				add(item);
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private static final Level[] SELECTABLE_LEVELS = { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG,
			Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF };

	/** index of the default log level (INFO) */
	public static final int DEFAULT_LEVEL_INDEX = 5;

	/** human readable names of the available log levels */
	public static final String[] SELECTABLE_LEVEL_NAMES = new String[SELECTABLE_LEVELS.length];

	static {
		for (int i = 0; i < SELECTABLE_LEVELS.length; i++) {
			SELECTABLE_LEVEL_NAMES[i] = SELECTABLE_LEVELS[i].getName();
		}
	}

	/** default number of log view entries before old ones get discarded for new ones */
	public static final int DEFAULT_LOG_ENTRY_NUMBER = 1000;

	/** the initial delay of the batch append timer */
	private static final int BATCH_APPEND_TIMER_DELAY = 500;
	/** the interval in which the batch append timer triggers */
	private static final int BATCH_APPEND_TIMER_INTERVAL = 500;

	public static final String LOG_VIEWER_DOCK_KEY = "log_viewer";

	private final DockKey DOCK_KEY = new ResourceDockKey(LOG_VIEWER_DOCK_KEY);

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@SuppressWarnings("unused")
	private final GeneralProcessListener PROCESS_CHANGE_LISTENER;

	private final ToggleAction TOGGLE_CLEAR_ON_START_ACTION = new ToggleAction(true, "clear_on_start") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionToggled(ActionEvent e) {
			// do nothing, state is queried by PROCESS_CHANGE_LISTENER
		}
	};

	/** the model which contains the log selection */
	private final LogSelectionModel selectionModel;

	private final Action CLEAR_MESSAGE_VIEWER_ACTION = new ClearMessageAction(this);

	private final Action SAVE_LOGFILE_ACTION = new SaveLogFileAction(this);

	private final ResourceAction SEARCH_ACTION = new LogSearchAction(this);

	private final Action REFRESH_ACTION = new LogRefreshAction(this);

	private final Action CLOSE_ACTION = new LogCloseAction(this);

	/** remembers the length of each line in the current log */
	private final LinkedList<Integer> lineLengths = new LinkedList<>();

	private final JTextPane textPane;

	private final ExtendedStyledDocument logStyledDocument;

	private final JToolBar toolBar;

	private final JButton closeButton;

	/** maximum number of rows to display in the log (oldest ones will be discarded for new ones) */
	private int maxRows;

	/** the timer that periodically checks whether a new batch append has to be triggered */
	private final Timer logAppendTimer;

	/** indicates if currently a batch update is in progress */
	private volatile boolean batchUpdateRunning;

	/**
	 *
	 *
	 * /** Creates the {@link LogViewer} instance for the {@link MainFrame}.
	 *
	 * @param mainFrame
	 */
	public LogViewer(MainFrame mainFrame) {
		super(new BorderLayout());

		// set maximum number of rows to display in the log before oldest entries are discarded
		this.maxRows = DEFAULT_LOG_ENTRY_NUMBER;
		try {
			String maxRowsString = ParameterService
					.getParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT);
			if (maxRowsString != null) {
				maxRows = Integer.parseInt(maxRowsString);
			}
		} catch (NumberFormatException e) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.tools.LoggingViewer.bad_integer_format_for_property");
		}

		// listen for changes to the maxRows limit at runtime
		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {

			@Override
			public void informParameterSaved() {
				// don't care
			}

			@Override
			public void informParameterChanged(String key, String value) {
				if (MainFrame.PROPERTY_RAPIDMINER_GUI_MESSAGEVIEWER_ROWLIMIT.equals(key)) {
					try {
						if (value != null) {
							maxRows = Integer.parseInt(value);
							logStyledDocument.setMaxRows(maxRows);
						}
					} catch (NumberFormatException e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.gui.tools.LoggingViewer.bad_integer_format_for_property");
					}
				}
			}
		});

		selectionModel = new LogSelectionModel();

		logStyledDocument = new ExtendedStyledDocument(maxRows);
		textPane = new JTextPane(logStyledDocument);
		textPane.setBackground(Colors.PANEL_BACKGROUND);
		textPane.setEditable(false);

		toolBar = new ExtendedJToolBar(true);
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.TAB_BORDER));
		JToggleButton clearOnStartToggleButton = TOGGLE_CLEAR_ON_START_ACTION.createToggleButton();
		clearOnStartToggleButton.setText("");
		toolBar.add(CLEAR_MESSAGE_VIEWER_ACTION);
		toolBar.add(SEARCH_ACTION);
		toolBar.add(clearOnStartToggleButton);
		toolBar.add(Box.createHorizontalGlue());
		closeButton = toolBar.add(CLOSE_ACTION);
		JButton button = makeDropDownButton();
		toolBar.add(button);

		add(toolBar, BorderLayout.NORTH);

		// allow search action to be shown via Ctrl+F
		SEARCH_ACTION.addToActionMap(textPane, JComponent.WHEN_FOCUSED);

		// prepare toolbar for currently selected log
		if (getLogSelectionModel().getCurrentLogModel() == null) {
			// this is done via index because we only have actions, not components
			closeButton.setVisible(false);
		} else {
			// only show close button if log is closable
			closeButton.setVisible(getLogSelectionModel().getCurrentLogModel().isClosable());
		}
		JScrollPane scrollPane = new ExtendedJScrollPane(textPane);
		scrollPane.setBorder(null);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);

		PROCESS_CHANGE_LISTENER = new GeneralProcessListener(mainFrame) {

			@Override
			public void processStarts(Process process) {
				if (TOGGLE_CLEAR_ON_START_ACTION.isEnabled() && TOGGLE_CLEAR_ON_START_ACTION.isSelected()) {
					RapidMinerGUI.getDefaultLogModel().clearLog();
				}
			}

			@Override
			public void processStartedOperator(Process process, Operator op) {
				// noop
			}

			@Override
			public void processFinishedOperator(Process process, Operator op) {
				// noop
			}

			@Override
			public void processEnded(Process process) {
				// noop
			}
		};

		// add mouse listener for popup menu
		MouseListener mouseListener = new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				evaluatePopup(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				evaluatePopup(e);
			}

			private void evaluatePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					createPopupMenu().show(textPane, e.getX(), e.getY());
				}
			}

		};
		addMouseListener(mouseListener);
		this.textPane.addMouseListener(mouseListener);

		// add observer so we are notified when current log model updates
		final Observer<List<LogEntry>> currentModelObserver = new Observer<List<LogEntry>>() {

			@Override
			public void update(Observable<List<LogEntry>> observable, List<LogEntry> newEntries) {
				// discard all events which are NOT from the currently selected model
				if (observable != LogViewer.this.getLogSelectionModel().getCurrentLogModel()) {
					return;
				}

				// if the event is from the current model, process it
				if (newEntries == null) {
					// model has been cleared
					clearLogArea();
				} else {
					Level currentLogLevel = getLogSelectionModel().getCurrentLogModel().getLogLevel();
					for (LogEntry entry : newEntries) {
						// skip entries whose level is less than the currently selected one
						if (entry.getLogLevel() != null && currentLogLevel != null
								&& entry.getLogLevel().intValue() < currentLogLevel.intValue()) {
							return;
						}

						logStyledDocument.appendLineForBatch(entry.getFormattedString(), entry.getSimpleAttributeSet());
					}
				}
			}
		};
		// register as listener to current log model if it is a PUSH log
		LogModel currentModel = getLogSelectionModel().getCurrentLogModel();
		if (currentModel != null && currentModel.getLogMode() == LogMode.PUSH) {
			currentModel.addObserver(currentModelObserver, false);
		}

		// add observer so we are notified when selected model changes
		Observer<LogModel> selectionObserver = new Observer<LogModel>() {

			@Override
			public void update(Observable<LogModel> observable, LogModel currentModel) {
				// make sure that we register to all current PUSH models as observer
				if (currentModel.getLogMode() == LogMode.PUSH) {
					try {
						currentModel.removeObserver(currentModelObserver);
					} catch (NoSuchElementException e) {
						// ignore this exception as it serves no purpose and we cannot check if we
						// already registered
					}
					currentModel.addObserver(currentModelObserver, false);
				}

				// discard potential batch update entries once we switch logs.
				// they will be displayed anyway when we switch back to their log
				logStyledDocument.clearBatch();

				// show close button only for closable logs
				closeButton.setVisible(currentModel.isClosable());

				// replace with new log
				batchFill(currentModel.getLogEntries());
			}
		};
		getLogSelectionModel().addObserver(selectionObserver, true);

		// create and start batch update timer
		logAppendTimer = new Timer(true);
		logAppendTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if (!batchUpdateRunning) {
					// only do the batch update if the log is visible to the user and there is
					// something to update
					if (textPane.isShowing() && logStyledDocument.getBatchSize() > 0) {
						try {
							// prevent multiple parallel executions of this task
							batchUpdateRunning = true;
							// apply batch and store length of each added row
							List<Integer> addedRowLengths = logStyledDocument.executeBatchAppend();
							lineLengths.addAll(addedRowLengths);
							// worstcase scenario, we now have 2*maxRows rows. Remove them.
							// Inserting and removing (as we append but remove from the top) will
							// always be two calls
							trimLog();
						} catch (Exception e1) {
							// should not happen (but catch all to keep the timer going)
							// rather dump to stderr than logging and having this method called back
							e1.printStackTrace();
						} finally {
							// always reset this flag
							batchUpdateRunning = false;
						}
					}
				}
			}
		}, BATCH_APPEND_TIMER_DELAY, BATCH_APPEND_TIMER_INTERVAL);
	}

	/**
	 * Returns the complete log which is currently displayed.
	 *
	 * @return
	 */
	public String getLogAsText() {
		return textPane.getText();
	}

	/**
	 * Clears the currently selected log model and thus the displayed log.
	 */
	public void clearLog() {
		if (getLogSelectionModel().getCurrentLogModel() == null) {
			return;
		}

		getLogSelectionModel().getCurrentLogModel().clearLog();

		// PULL logs don't update themselves, so we update right now
		if (getLogSelectionModel().getCurrentLogModel().getLogMode() == LogMode.PULL) {
			clearLogArea();
		}
	}

	/**
	 * Opens a save dialog to save the currently displayed log into a file.
	 */
	public void saveLog() {
		File file = new File("." + File.separator);
		String logFile = null;
		try {
			logFile = RapidMinerGUI.getMainFrame().getProcess().getRootOperator()
					.getParameterAsString(ProcessRootOperator.PARAMETER_LOGFILE);
		} catch (UndefinedParameterError ex) {
			// tries to use process file name for initialization
		}
		if (logFile != null) {
			file = RapidMinerGUI.getMainFrame().getProcess().resolveFileName(logFile);
		}
		file = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), file, false, "log", "log file");
		if (file != null) {
			try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
				out.println(textPane.getText());
			} catch (IOException ex) {
				SwingTools.showSimpleErrorMessage("cannot_write_log_file", ex);
			}
		}
	}

	/**
	 * Opens a search dialog for the currently displayed log.
	 */
	public void performSearch() {
		new SearchDialog(textPane, new SearchableJTextComponent(textPane)).setVisible(true);
	}

	/**
	 * Requests a refresh from a {@link LogMode#PULL} model. Executed in a ProgressThread. Updates
	 * the GUI automatically afterwards.
	 */
	public void performRefresh() {
		final LogModel currentModel = getLogSelectionModel().getCurrentLogModel();
		if (currentModel == null) {
			return;
		}

		// only needed for PULL logs
		if (currentModel.getLogMode() == LogMode.PULL) {
			final ProgressThread requestRefresh = new ProgressThread("request_log_refresh", false, currentModel.getName()) {

				@Override
				public void run() {
					getProgressListener().setTotal(100);
					try {
						currentModel.updateEntries(getProgressListener());
					} catch (final LogUpdateException e) {
						// update failed, add error to current log view
						if (getLogSelectionModel().getCurrentLogModel().equals(currentModel)) {
							LogRecordEntry errorEntry = new LogRecordEntry(
									new LogRecord(Level.WARNING, I18N.getGUIMessage("gui.logging.error.update.label")));
							logStyledDocument.appendLineForBatch(errorEntry.getFormattedString(),
									errorEntry.getSimpleAttributeSet());
						}
						return;
					}
					getProgressListener().complete();

					// is the user after the update has completed still displaying the same log? If
					// so, update it. Otherwise we can ignore this as it will be visually updated
					// when switching to it anyway
					if (getLogSelectionModel().getCurrentLogModel().equals(currentModel)) {
						batchFill(currentModel.getLogEntries());
					}
				}
			};
			requestRefresh.start();
		}
	}

	/**
	 * Set the log level of the currently selected {@link LogModel}.
	 *
	 * @param level
	 */
	private void setLogLevel(Level level) {
		if (getLogSelectionModel().getCurrentLogModel() == null) {
			return;
		}

		getLogSelectionModel().getCurrentLogModel().setLogLevel(level);
		batchFill(getLogSelectionModel().getCurrentLogModel().getLogEntries());

		// only do this for our own RapidMiner Studio log
		if (getLogSelectionModel().getCurrentLogModel().equals(RapidMinerGUI.getDefaultLogModel())) {
			LogService.getRoot().setLevel(level);
			ParameterService.setParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_LOG_LEVEL, level.getName());
			ParameterService.saveParameters();
		}
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	/**
	 * Returns the model which backs the available logs. Can be used to change the currently
	 * selected log.
	 *
	 * @return
	 */
	public LogSelectionModel getLogSelectionModel() {
		return selectionModel;
	}

	/**
	 * Creates a {@link DropDownPopupButton} button which will show the available log entries to
	 * allow the user to select the currently displayed one.
	 *
	 * @return
	 */
	private DropDownPopupButton makeDropDownButton() {
		// init dialog so listener is registered, otherwise starting a comic via
		// the popup will not
		// be registered by the GUI
		PopupMenuProvider menuProvider = new PopupMenuProvider() {

			@Override
			public JPopupMenu getPopupMenu() {
				JPopupMenu menu = new ScrollableJPopupMenu(ScrollableJPopupMenu.SIZE_NORMAL);
				for (JMenuItem item : createItems()) {
					menu.add(item);
				}
				return menu;
			}

			/**
			 * Creates a list of menu items, one for each available log.
			 *
			 * @return
			 */
			private List<JMenuItem> createItems() {
				List<JMenuItem> list = new LinkedList<>();
				for (final LogModel logmodel : LogModelRegistry.INSTANCE.getRegisteredObjects()) {
					final JMenuItem item = new JMenuItem();
					item.setText(logmodel.getName());
					if (logmodel.getIcon() != null) {
						item.setIcon(logmodel.getIcon());
					}

					item.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent e) {
							LogViewer.this.getLogSelectionModel().setSelectedLogModel(logmodel);
						}
					});
					list.add(item);

					// highlight item for currently selected log
					if (logmodel.equals(getLogSelectionModel().getCurrentLogModel())) {
						item.setFont(item.getFont().deriveFont(Font.BOLD));
					}
				}
				return list;
			}

		};
		final DropDownPopupButton dropDownToReturn = new DropDownPopupButton(
				new ResourceActionAdapter(true, "logging.selection"), menuProvider);
		dropDownToReturn.setMaximumSize(new Dimension(50, 30));
		return dropDownToReturn;
	}

	/**
	 * Creates the log level menu for the current log.
	 *
	 * @return
	 */
	private JMenu makeLogLevelMenu() {
		return new LogLevelMenu();
	}

	/**
	 * Creates the popup menu for the log text area.
	 *
	 * @return
	 */
	private JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		LogModel currentModel = getLogSelectionModel().getCurrentLogModel();

		// Studio log can be reset on each process start
		if (currentModel != null && currentModel.equals(RapidMinerGUI.getDefaultLogModel())) {
			menu.add(TOGGLE_CLEAR_ON_START_ACTION.createMenuItem());
		}

		// pull logs cannot be cleared
		if (currentModel != null && currentModel.getLogMode() != LogMode.PULL) {
			menu.add(CLEAR_MESSAGE_VIEWER_ACTION);
		} else {
			menu.add(CLOSE_ACTION);
		}

		menu.add(SAVE_LOGFILE_ACTION);
		menu.add(SEARCH_ACTION);

		// pull logs can be manually refreshed
		if (currentModel != null && currentModel.getLogMode() == LogMode.PULL) {
			menu.add(REFRESH_ACTION);
		}
		menu.addSeparator();

		menu.add(makeLogLevelMenu());

		return menu;
	}

	/**
	 * Trims the log to match the maxRows setting (if set) and moves the caret to the end of the
	 * document.
	 *
	 */
	private void trimLog() {
		// if trimming is enabled
		if (maxRows > 0) {
			int removeLength = 0;
			// find out how much of the beginning of the document (first n rows) we have to remove
			while (lineLengths.size() > maxRows) {
				removeLength += lineLengths.removeFirst();
			}
			// if we have to remove something do it now
			if (removeLength > 0) {
				try {
					logStyledDocument.remove(0, removeLength);
				} catch (BadLocationException e) {
					SwingTools.showSimpleErrorMessage("error_during_logging", e);
				}
			}
		}

		try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					textPane.setCaretPosition(logStyledDocument.getLength());
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// not important, can be ignored
		}
	}

	/**
	 * Fills the log text area with the specified {@link LogEntry}s. Will not add more than maxRows
	 * (if set). If the minimum log level of the current log is higher than that of an entry, it is
	 * not added.
	 *
	 * @param entries
	 */
	private void batchFill(List<LogEntry> entries) {
		try {
			// prevent the timer from triggering an update mid-batch
			batchUpdateRunning = true;

			// should not happen but just in case
			if (getLogSelectionModel().getCurrentLogModel() == null) {
				batchUpdateRunning = false;
				return;
			}

			// clear old contents
			clearLogArea();

			Level currentLogLevel = getLogSelectionModel().getCurrentLogModel().getLogLevel();
			for (int counter = 0; counter < entries.size(); counter++) {
				int index = counter;

				// check if we are allowed to add this row or if we run into the row limit (if set)
				if (maxRows > 0) {
					if (counter >= maxRows) {
						break;
					}

				}

				if (entries.size() > maxRows) {
					// calculate correct index in case row limit is smaller than entries.size
					// start at the end of the array
					index = entries.size() - maxRows + counter;
				}

				LogEntry entry = entries.get(index);
				// skip entries whose level is less than the currently selected one
				if (entry.getLogLevel() != null && currentLogLevel != null
						&& entry.getLogLevel().intValue() < currentLogLevel.intValue()) {
					continue;
				}

				// add entry to batch
				logStyledDocument.appendLineForBatch(entry.getFormattedString(), entry.getSimpleAttributeSet());
			}

			// if we actually have a batch now, execute it
			if (logStyledDocument.getBatchSize() > 0) {
				try {
					List<Integer> addedRowLengths = logStyledDocument.executeBatchAppend();
					lineLengths.addAll(addedRowLengths);
				} catch (BadLocationException e) {
					// cannot happen
					// rather dump to stderr than logging and having this method called back
					e.printStackTrace();
				}
			}

			// set caret to end position
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					textPane.setCaretPosition(logStyledDocument.getLength());
				}
			});
		} finally {
			// always reset this flag
			batchUpdateRunning = false;
		}
	}

	/**
	 * Clears the text area displaying the current log. As such, the log batch is also reset.
	 */
	private void clearLogArea() {
		try {
			logStyledDocument.remove(0, logStyledDocument.getLength());
		} catch (BadLocationException e) {
			// should not happen, fallback:
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						textPane.setText("");
					}
				});
			} catch (InterruptedException | InvocationTargetException e1) {
				// should not happen
				// rather dumpt to stderr instead of logging
				e1.printStackTrace();
			}
		}
		lineLengths.clear();
		logStyledDocument.clearBatch();
	}
}
