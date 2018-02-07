/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.gui.processeditor.results;

import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import com.rapidminer.BreakpointListener;
import com.rapidminer.LoggingListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.PerspectiveModel;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.CloseAllResultsAction;
import com.rapidminer.gui.processeditor.ProcessLogTab;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.DecisionRememberingConfirmDialog;
import com.rapidminer.gui.viewer.DataTableViewer;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableActionCustomizer;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;
import com.vlsolutions.swing.docking.event.DockingActionCloseEvent;
import com.vlsolutions.swing.docking.event.DockingActionDockableEvent;
import com.vlsolutions.swing.docking.event.DockingActionEvent;
import com.vlsolutions.swing.docking.event.DockingActionListener;


/**
 * {@link ResultDisplay} that adds each result to an individual {@link Dockable}. In addition, it
 * displays a result history overview.
 *
 * @author Simon Fischer
 *
 */
public class DockableResultDisplay extends JPanel implements ResultDisplay {

	private static final long serialVersionUID = 1L;

	private final DockKey dockKey = new ResourceDockKey(RESULT_DOCK_KEY);

	private final Map<String, DataTable> dataTables = new HashMap<>();

	private final UpdateQueue tableUpdateQueue = new UpdateQueue("ResultDisplayDataTableViewUpdater");

	private final ResultOverview overview = new ResultOverview();

	/**
	 * flag indicating if the user chose "close old results" when starting the process. Will be set
	 * after processStarts listener fired.
	 */
	private Boolean closeResultsPerRun;

	public DockableResultDisplay() {
		this.dockKey.setDockGroup(MainFrame.DOCK_GROUP_RESULTS);
		DockableActionCustomizer customizer = new DockableActionCustomizer() {

			@Override
			public void visitTabSelectorPopUp(JPopupMenu popUpMenu, Dockable dockable) {
				popUpMenu.add(new JMenuItem(new CloseAllResultsAction(RapidMinerGUI.getMainFrame())));
			}
		};
		customizer.setTabSelectorPopUpCustomizer(true); // enable tabbed dock custom popup menu
														 // entries
		this.dockKey.setActionCustomizer(customizer);
		setLayout(new BorderLayout());
		ExtendedJScrollPane overviewScrollpane = new ExtendedJScrollPane(overview);
		overviewScrollpane.setBorder(null);
		overviewScrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		overviewScrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(overviewScrollpane, BorderLayout.CENTER);
		tableUpdateQueue.start();
	}

	@Override
	public void init(MainFrame mf) {
		DockingDesktop desktop = mf.getDockingDesktop();
		desktop.addDockingActionListener(new DockingActionListener() {

			@Override
			public void dockingActionPerformed(DockingActionEvent arg0) {}

			@Override
			public boolean acceptDockingAction(DockingActionEvent e) {
				if (e instanceof DockingActionCloseEvent
						&& ((DockingActionDockableEvent) e).getDockable() == DockableResultDisplay.this) {
					return SwingTools.showConfirmDialog("result.really_close",
							ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.YES_OPTION;
				} else {
					return true;
				}
			}
		});

		desktop.addDockableStateChangeListener(new DockableStateChangeListener() {

			@Override
			public void dockableStateChanged(DockableStateChangeEvent e) {
				if (e.getNewState().isClosed()) {
					if (e.getNewState().getDockable() instanceof ResultTab) {
						ResultTab rt = (ResultTab) e.getNewState().getDockable();
						rt.freeResources();
						RapidMinerGUI.getMainFrame().getPerspectiveController().removeFromAllPerspectives(rt);
						getDockKey().resetPropertyChangeListener();
					} else if (e.getNewState().getDockable() instanceof ProcessLogTab) {
						ProcessLogTab pt = (ProcessLogTab) e.getNewState().getDockable();
						if (pt != null) {
							if (pt.getDataTable() != null) {
								dataTables.remove(pt.getDataTable().getName());
							}
							pt.freeResources();
							RapidMinerGUI.getMainFrame().getPerspectiveController().removeFromAllPerspectives(pt);
						}
					}
				}
			}
		});
	}

	private boolean isAskingForPerspectiveSwitch = false;

	private void askForPerspectiveSwitch() {
		if (isAskingForPerspectiveSwitch || RapidMinerGUI.getMainFrame().getPerspectiveController().getModel()
				.getSelectedPerspective().getName().equals("result")) {
			return;
		} else {
			try {
				isAskingForPerspectiveSwitch = true;
				if (DecisionRememberingConfirmDialog.confirmAction("show_results_on_creation",
						MainFrame.PROPERTY_RAPIDMINER_GUI_AUTO_SWITCH_TO_RESULTVIEW)) {
					if (SwingUtilities.isEventDispatchThread()) {
						RapidMinerGUI.getMainFrame().getPerspectiveController().showPerspective(PerspectiveModel.RESULT);
					} else {
						try {
							SwingUtilities.invokeAndWait(new Runnable() {

								@Override
								public void run() {
									RapidMinerGUI.getMainFrame().getPerspectiveController()
											.showPerspective(PerspectiveModel.RESULT);
								}
							});
						} catch (InterruptedException e) {
							// LogService.getRoot().log(Level.WARNING,
							// "Error switching perspectives: "+e, e);
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.gui.processeditor.results.DockableResultDisplay.error_switching_perspectives",
											e),
									e);
						} catch (InvocationTargetException e) {
							// LogService.getRoot().log(Level.WARNING,
							// "Error switching perspectives: "+e, e);
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.gui.processeditor.results.DockableResultDisplay.error_switching_perspectives",
											e),
									e);
						}
					}
				}
			} finally {
				isAskingForPerspectiveSwitch = false;
			}
		}
	}

	@Override
	public void showData(final IOContainer container, final String statusMessage) {
		if (container == null || container.size() == 0) {
			return;
		}
		final List<IOObject> ioobjects = Arrays.asList(container.getIOObjects());
		new ProgressThread("creating_display") {

			@Override
			public void run() {
				try {
					getProgressListener().setTotal(ioobjects.size() + 1);
					overview.addResults(RapidMinerGUI.getMainFrame().getProcess(), ioobjects, statusMessage);
					getProgressListener().setCompleted(1);
					int i = 0;
					for (IOObject ioobject : ioobjects) {
						if (ioobject instanceof ResultObject) {
							i++;
							showResultNow((ResultObject) ioobject, "process_" + i);
							getProgressListener().setCompleted(i + 1);
						}
					}
				} finally {
					getProgressListener().complete();
				}
			}
		}.start();
		askForPerspectiveSwitch();
	}

	private static int currentId;

	@Override
	public void showResult(final ResultObject result) {
		showResult(result, "dynamic_" + currentId++);
		askForPerspectiveSwitch();
	}

	/** Creates the display in a ProgressThread. */
	private void showResult(final ResultObject result, final String id) {
		new ProgressThread("creating_display") {

			@Override
			public void run() {
				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);
				showResultNow(result, id);
				getProgressListener().setCompleted(100);
				getProgressListener().complete();
			}
		}.start();
	}

	/** Creates a display on the current thread and displays it (on the EDT). */
	private void showResultNow(final ResultObject result, final String id) {
		ResultTab tab = (ResultTab) RapidMinerGUI.getMainFrame().getDockingDesktop().getContext()
				.getDockableByKey(ResultTab.DOCKKEY_PREFIX + id);
		if (tab == null) {
			tab = new ResultTab(ResultTab.DOCKKEY_PREFIX + id);
		}
		showTab(tab);
		tab.showResult(result);
	}

	/**
	 * Update the DataTableViewers. This does not happen on the EDT and is executed asynchronously
	 * by an {@link UpdateQueue}.
	 */
	private void updateDataTables() {
		final Collection<DataTable> copy = new LinkedList<>(dataTables.values());
		// this is time consuming, so execute off EDT
		tableUpdateQueue.execute(new Runnable() {

			@Override
			public void run() {
				final Collection<DataTableViewer> viewers = new LinkedList<>();
				for (DataTable table : copy) {
					viewers.add(new DataTableViewer(table, true, DataTableViewer.TABLE_MODE));
				}
				installDataTableViewers(viewers);
			}

			@Override
			public String toString() {
				return "Update data table list to size " + copy.size();
			}
		});
	}

	/** Adds the collection of components on the EDT (after removing the old tables. */
	private void installDataTableViewers(final Collection<DataTableViewer> viewers) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				for (DataTableViewer viewer : viewers) {
					ProcessLogTab tab = (ProcessLogTab) RapidMinerGUI.getMainFrame().getDockingDesktop().getContext()
							.getDockableByKey(ProcessLogTab.DOCKKEY_PREFIX + viewer.getDataTable().getName());
					if (tab == null) {
						tab = new ProcessLogTab(ProcessLogTab.DOCKKEY_PREFIX + viewer.getDataTable().getName());
					}
					showTab(tab);
					tab.setDataTableViewer(viewer);
				}
			}
		});
	}

	private void showTab(final Dockable dockable) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				RapidMinerGUI.getMainFrame().getPerspectiveController().showTabInAllPerspectives(dockable,
						DockableResultDisplay.this);
			}
		});
	}

	@Override
	public void addDataTable(final DataTable dataTable) {
		DockableResultDisplay.this.dataTables.put(dataTable.getName(), dataTable);
		updateDataTables();
	}

	private void clear() {
		clear(true);
	}

	private void clear(boolean alsoClearLogs) {
		if (SwingUtilities.isEventDispatchThread()) {
			clearNow(alsoClearLogs);
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						clearNow();
					}
				});
			} catch (InterruptedException e) {
				// LogService.getRoot().log(Level.WARNING, "Interupted while closing result tabs.",
				// e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.processeditor.results.DockableResultDisplay.interrupted_while_closing_result_tabs"),
						e);

			} catch (InvocationTargetException e) {
				// LogService.getRoot().log(Level.WARNING,
				// "Exception while closing result tabs: "+e, e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.processeditor.results.DockableResultDisplay.exception_while_closing_result_tabs",
								e),
						e);
			}
		}
	}

	private void clearNow() {
		clearNow(true);
	}

	private void clearNow(boolean alsoClearLogs) {
		List<Dockable> toClose = new LinkedList<>();
		for (DockableState state : RapidMinerGUI.getMainFrame().getDockingDesktop().getContext().getDockables()) {
			if (state.getDockable().getDockKey().getKey().startsWith(ResultTab.DOCKKEY_PREFIX + "process_")
					|| alsoClearLogs && state.getDockable().getDockKey().getKey().startsWith(ProcessLogTab.DOCKKEY_PREFIX)) {
				toClose.add(state.getDockable());
			}
		}
		if (!toClose.isEmpty() || !dataTables.isEmpty()) {
			// fix for "delete old results" dialog after breakpoint resume
			if (closeResultsPerRun) {
				DockableResultDisplay.this.dataTables.clear();
				// updateDataTables();
				for (Dockable dockable : toClose) {
					if (dockable instanceof ResultTab) {
						((ResultTab) dockable).freeResources();
					} else if (dockable instanceof ProcessLogTab) {
						((ProcessLogTab) dockable).freeResources();
					}
					// RapidMinerGUI.getMainFrame().getDockingDesktop().close(dockable);
					RapidMinerGUI.getMainFrame().getPerspectiveController().removeFromAllPerspectives(dockable);
				}
				getDockKey().resetPropertyChangeListener();
			}
		}
	}

	@Override
	public void clearAll() {
		for (DockableState state : RapidMinerGUI.getMainFrame().getDockingDesktop().getContext().getDockables()) {
			if (state.getDockable().getDockKey().getKey().startsWith(ResultTab.DOCKKEY_PREFIX)
					|| state.getDockable().getDockKey().getKey().startsWith(ProcessLogTab.DOCKKEY_PREFIX)) {
				RapidMinerGUI.getMainFrame().getPerspectiveController().removeFromAllPerspectives(state.getDockable());
			}
		}
	}

	// Listeners

	private final LoggingListener logListener = new LoggingListener() {

		@Override
		public void addDataTable(final DataTable dataTable) {
			DockableResultDisplay.this.addDataTable(dataTable);
		}

		@Override
		public void removeDataTable(final DataTable dataTable) {
			DockableResultDisplay.this.dataTables.remove(dataTable.getName());
			updateDataTables();
		}
	};

	private final ProcessListener processListener = new ProcessListener() {

		@Override
		public void processEnded(Process process) {
			// set to null so we know the process has ended and we need to ask the user again next
			// time
			DockableResultDisplay.this.closeResultsPerRun = null;
		}

		@Override
		public void processFinishedOperator(Process process, Operator op) {}

		@Override
		public void processStartedOperator(Process process, Operator op) {}

		@Override
		public void processStarts(Process process) {
			if (closeResultsPerRun == null) {
				closeResultsPerRun = DecisionRememberingConfirmDialog.confirmAction("result.close_before_run",
						RapidMinerGUI.PROPERTY_CLOSE_RESULTS_BEFORE_RUN);
			}
			clear();
		}
	};

	private final BreakpointListener breakpointListener = new BreakpointListener() {

		@Override
		public void resume() {
			clear(false);
		}

		@Override
		public void breakpointReached(Process process, Operator op, IOContainer iocontainer, int location) {}
	};

	// Dockable

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return dockKey;
	}

	// ProcessEditor

	private Process process;

	@Override
	public void processChanged(Process process) {
		if (this.process != null) {
			this.process.removeLoggingListener(logListener);
			this.process.getRootOperator().removeProcessListener(processListener);
			this.process.removeBreakpointListener(breakpointListener);
		}
		this.process = process;
		if (this.process != null) {
			this.process.addLoggingListener(logListener);
			this.process.getRootOperator().addProcessListener(processListener);
			this.process.addBreakpointListener(breakpointListener);
		}
	}

	@Override
	public void processUpdated(Process process) {}

	@Override
	public void setSelection(List<Operator> selection) {}
}
