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

import java.util.List;
import java.util.logging.Level;

import com.rapidminer.NoBugError;
import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.core.license.DatabaseConstraintViolationException;
import com.rapidminer.core.license.LicenseViolationException;
import com.rapidminer.gui.tools.ProcessGUITools;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * A Thread for running an process in the RapidMinerGUI. This thread is necessary in order to keep
 * the GUI running (and working). Please note that this class can only be used from a running
 * RapidMiner GUI since several dependencies to the class {@link RapidMinerGUI} and
 * {@link MainFrame} exist. If you want to perform an process in its own thread from your own
 * program simply use a Java Thread peforming the method process.run() in its run()-method.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class ProcessThread extends Thread {

	private Process process;

	public ProcessThread(final Process process) {
		super("ProcessThread");
		this.process = process;
	}

	@Override
	public void run() {
		try {
			IOContainer results = process.run();
			beep("success");
			process.getRootOperator().sendEmail(results, null);
			RapidMinerGUI.getMainFrame().processEnded(process, results);
		} catch (DatabaseConstraintViolationException ex) {
			// Check DatabaseConstraintViolationException first as it is a subclass of
			// the more general LicenseViolationException
			if (ex.getOperatorName() != null) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.gui.ProcessThread.database_constraint_violation_exception_in_operator",
						new Object[] { ex.getDatabaseURL(), ex.getOperatorName() });
			} else {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.gui.ProcessThread.database_constraint_violation_exception",
						new Object[] { ex.getDatabaseURL() });
			}
		} catch (LicenseViolationException ex) {
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.gui.ProcessThread.operator_constraint_violation_exception",
					new Object[] { ex.getOperatorName() });
		} catch (ProcessStoppedException ex) {
			process.getLogger().info(ex.getMessage());
			// here the process ended method is not called ! let the thread finish the
			// current operator and send no events to the main frame...
			// also no beep...
		} catch (Throwable e) {
			if (!(e instanceof OperatorException)) { // otherwise it was already counted
				ActionStatisticsCollector.getInstance().log(process.getCurrentOperator(),
						ActionStatisticsCollector.OPERATOR_EVENT_FAILURE);
				ActionStatisticsCollector.getInstance().log(process.getCurrentOperator(),
						ActionStatisticsCollector.OPERATOR_EVENT_RUNTIME_EXCEPTION);
			}

			beep("error");
			String debugProperty = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE);
			boolean debugMode = Tools.booleanValue(debugProperty, false);
			String message = e.getMessage();
			if (!debugMode) {
				if (e instanceof RuntimeException) {
					if (e.getMessage() != null) {
						message = "operator cannot be executed (" + e.getMessage() + "). Check the log messages...";
					} else {
						message = "operator cannot be executed. Check the log messages...";
					}
				}
			}
			process.getLogger().log(Level.SEVERE, "Process failed: " + message, e);
			logProcessTreeList(10, "==>", process.getCurrentOperator());

			try {
				process.getRootOperator().sendEmail(null, e);
			} catch (UndefinedParameterError ex) {
				// cannot happen
				process.getLogger().log(Level.WARNING, "Problems during sending result mail: " + ex.getMessage(), ex);
			}

			if (e instanceof OutOfMemoryError) {
				// out of memory, give memory hint
				SwingTools.showVerySimpleErrorMessage("proc_failed_out_of_mem");
				ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_ERROR, "out_of_memory",
						String.valueOf(SystemInfoUtilities.getMaxHeapMemorySize()));
			} else if (e instanceof NoBugError) {
				// no bug? Show nice error screen (user error infos)
				if (e instanceof UserError) {
					ProcessGUITools.displayBubbleForUserError((UserError) e);
				} else {
					handleError(debugMode, e, new Object[] {});
				}
			} else {
				if (debugMode) {
					handleError(true, e, new Object[] {});
				} else {
					// perform process check. No bug report if errors...
					if (e instanceof NullPointerException || e instanceof ArrayIndexOutOfBoundsException) {
						LogService.getRoot().log(Level.SEVERE, e.toString(), e);
						SwingTools.showVerySimpleErrorMessage("proc_failed_without_obv_reason");
					} else {
						SwingTools.showSimpleErrorMessage("process_failed_simple", e, true, new Object[] {});
					}
				}
			}
			RapidMinerGUI.getMainFrame().processEnded(this.process, null);
		} finally {
			if (process.getProcessState() != Process.PROCESS_STATE_STOPPED) {
				process.stop();
			}
			this.process = null;
		}
	}

	private void logProcessTreeList(int indent, String mark, Operator markOperator) {
		process.getLogger().log(Level.SEVERE, "Here: ");
		List<String> processTreeList = process.getRootOperator().createProcessTreeList(indent, "", "", markOperator, mark);
		for (String logEntry : processTreeList) {
			process.getLogger().log(Level.SEVERE, logEntry);
		}
	}

	public static void beep(final String reason) {
		if (Tools.booleanValue(ParameterService.getParameterValue("rapidminer.gui.beep." + reason), false)) {
			java.awt.Toolkit.getDefaultToolkit().beep();
		}
	}

	public void stopProcess() {
		if (process != null) {
			this.process.stop();
		}
	}

	public void pauseProcess() {
		if (process != null) {
			this.process.pause();
		}
	}

	@Override
	public String toString() {
		return "ProcessThread (" + process.getProcessLocation() + ")";
	}

	/**
	 * Displays an error dialog for {@link Throwable}s.
	 *
	 * @param debugMode
	 *            whether debug mode is enabled
	 * @param t
	 *            the throwable instance
	 * @param args
	 *            optional i18n arguments
	 */
	private void handleError(boolean debugMode, Throwable t, Object[] args) {
		SwingTools.showFinalErrorMessage("process_failed_simple", t, debugMode, new Object[] {});
	}
}
