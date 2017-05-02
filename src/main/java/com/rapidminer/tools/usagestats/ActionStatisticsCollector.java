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
package com.rapidminer.tools.usagestats;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.AbstractLinkButton;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.XMLException;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;


/**
 * Supersedes the old functionality of {@link UsageStatistics} to collect usage records of the form
 * type, value, arg. Where type can be operator, error, template, and any other category. Value is
 * the object of that type which is to be counted (e.g. read_csv if type=operator). arg is most
 * often null, but can be used to allow a more fine-grained logging, e.g., in the case of operators,
 * arg can be "execute", "stop", or "fail". Note that type, value, and arg will be used as grouping
 * attributes for aggregated counts. Therefore, arg cannot be too detailed, e.g. error messages
 * "File not found: /path/to/file".
 *
 * Records can be logged using {@link #log(String, String, String)} which will add 1 to the counter.
 * Perspective switches use a timer and can call {@link #log(String, String, String, long)} to use
 * other increments than 1.
 *
 * @author Simon Fischer
 */
public enum ActionStatisticsCollector {

	INSTANCE;

	public static final String TYPE_CONSTANT = "rapidminer";
	private static final String TYPE_DOCKABLE = "dockable";
	private static final String TYPE_ACTION = "action";
	public static final String TYPE_OPERATOR = "operator";
	public static final String TYPE_PERSPECTIVE = "perspective";
	public static final String TYPE_ERROR = "error";
	public static final String TYPE_IMPORT = "import";
	public static final String TYPE_DIALOG = "dialog";
	public static final String TYPE_CONSTRAINT = "constraint";
	public static final String TYPE_LICENSE_LEVEL = "license-level";
	public static final String TYPE_PROGRESS_THREAD = "progress-thread";
	public static final String TYPE_TEMPLATE = "template";
	public static final String TYPE_RENDERER = "renderer";
	public static final String TYPE_CHART = "chart";

	/** new data access dialog (since 7.0.0) */
	public static final String TYPE_NEW_IMPORT = "new_import";

	/** start-up dialog (since 7.0.0) */
	public static final String TYPE_GETTING_STARTED = "getting_started";

	/** operator search field (since 7.1.1) */
	public static final String TYPE_OPERATOR_SEARCH = "operator_search";

	/** onboarding dialog (since 7.1.1) */
	public static final String TYPE_ONBOARDING = "onboarding";

	public static final String OPERATOR_EVENT_EXECUTION = "EXECUTE";
	public static final String OPERATOR_EVENT_STOPPED = "STOPPED";
	public static final String OPERATOR_EVENT_FAILURE = "FAILURE";
	public static final String OPERATOR_EVENT_USER_ERROR = "USER_ERROR";
	public static final String OPERATOR_EVENT_OPERATOR_EXCEPTION = "OPERATOR_EXCEPTION";
	public static final String OPERATOR_EVENT_RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION";

	/** runtime of an operator (since 7.1.1) */
	private static final String OPERATOR_RUNTIME = "RUNTIME";

	/** input and output volume of an operator port (since 7.1.1) */
	private static final String TYPE_INPUT_VOLUME = "input_volume";
	private static final String TYPE_OUTPUT_VOLUME = "output_volume";

	/** jvm total memory logging (since 7.1.1) */
	private static final String TYPE_MEMORY = "memory";
	private static final String MEMORY_USED = "used";
	private static final String MEMORY_ARG = "MEMORY";

	/** arguments to log operator port volume, cells = columns*rows, (since 7.1.1) */
	private static final String VOLUMNE_CELLS = "CELLS";
	private static final String VOLUME_COLUMNS = "COLUMNS";
	private static final String VOLUME_ROWS = "ROWS";

	/** row limit check (since 7.2) */
	public static final String TYPE_ROW_LIMIT = "row-limit";
	public static final String VALUE_ROW_LIMIT_EXCEEDED = "exceeded";
	public static final String ARG_ROW_LIMIT_CHECK = "check";
	public static final String ARG_ROW_LIMIT_DOWNSAMPLED = "downsampled";
	public static final String ARG_ROW_LIMIT_ABORTED = "aborted";
	public static final String VALUE_ROW_LIMIT_UPGRADE_FIX = "upgrade_fix";
	public static final String VALUE_ROW_LIMIT_UPGRADE_NOT_ENOUGH = "upgrade_not_enough";
	public static final String VALUE_ROW_LIMIT_UPGRADE_SELECTED = "upgrade_selected";
	public static final String ARG_ROW_LIMIT_NO_UPGRADE = "no_upgrade";

	/** commercial and educational sign up (since 7.3) */
	public static final String TYPE_SIGN_UP = "sign_up";
	public static final String VALUE_ACCOUNT_TYPE = "account_type";
	public static final String ARG_COMMERCIAL = "commercial";
	public static final String ARG_EDUCATIONAL = "educational";
	public static final String VALUE_ACCOUNT_CREATION = "account_creation";
	public static final String ARG_ACCOUNT_CREATION_ABORTED = "aborted";
	public static final String ARG_ACCOUNT_CREATION_SUCCESS = "success";
	public static final String ARG_ACCOUNT_ALREADY_EXISTS = "already_exists";
	public static final String ARG_COMMUNICATION_ERROR = "communication_error";
	public static final String VALUE_EMAIL_VERIFICATION = "email_verification";
	public static final String ARG_EMAIL_VERIFICATION_SUCCESS = "success";
	public static final String ARG_EMAIL_VERIFICATION_PENDING = "pending";

	/** row limit check additions (since 7.3) */
	public static final String VALUE_ROW_LIMIT_DIALOG = "dialog";

	/** beta features (since 7.3) */
	public static final String TYPE_BETA_FEATURES = "beta-features";
	public static final String VALUE_BETA_FEATURES_ACTIVATION = "activated";

	/** marketplace search (since 7.3) */
	public static final String TYPE_MARKETPLACE = "marketplace";
	public static final String VALUE_OPERATOR_SEARCH = "operator_search";
	public static final String VALUE_SEARCH = "search";
	public static final String VALUE_EXTENSION_INSTALLATION = "extension_installation";

	/** extension initialization (since 7.3) */
	public static final String VALUE_EXTENSION_INITIALIZATION = "extension_initialization";

	/** type cta (since 7.5) */
	public static final String TYPE_CTA = "cta";
	public static final String VALUE_CTA_FAILURE = "failure";
	public static final String VALUE_RULE_TRIGGERED = "cta_triggered";

	/**
	 * added to a key arg to indicated that this stores the maximum amount of all the amounts stored
	 * for arg
	 */
	private static final String MAXIMUM_INDICATOR = "_MAX";

	/**
	 * added to a key arg to indicated that this stores the minimum amount of all the amounts stored
	 * for arg
	 */
	private static final String MINIMUM_INDICATOR = "_MIN";

	/**
	 * added to a key arg to indicated that this stores the number of times an amount was stored for
	 * arg
	 */
	private static final String COUNT_INDICATOR = "_COUNT";

	/** conversion constant for bytes to megabytes */
	private static final int BYTE_TO_MB = 1024 * 1024;

	public static final String XML_TAG = "action-statistics";

	public static final class Key {

		private String type;
		private String value;
		private String arg;

		public Key(String type, String value, String arg) {
			super();
			this.type = type;
			this.value = value;
			this.arg = arg;
		}

		public String getType() {
			return type;
		}

		public String getValue() {
			return value;
		}

		public String getArg() {
			return arg;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (arg == null ? 0 : arg.hashCode());
			result = prime * result + (type == null ? 0 : type.hashCode());
			result = prime * result + (value == null ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Key other = (Key) obj;
			if (arg == null) {
				if (other.arg != null) {
					return false;
				}
			} else if (!arg.equals(other.arg)) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (!type.equals(other.type)) {
				return false;
			}
			if (value == null) {
				if (other.value != null) {
					return false;
				}
			} else if (!value.equals(other.value)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return type + ",\t" + value + ",\t" + arg;
		}
	}

	/** Listener that logs input and output volume at operator ports. */
	private final ProcessListener operatorVolumeListener = new ProcessListener() {

		@Override
		public void processStarts(Process process) {
			// not needed
		}

		@Override
		public void processStartedOperator(Process process, Operator op) {
			// log the input volumes of the operator
			for (InputPort inputPort : op.getInputPorts().getAllPorts()) {
				try {
					IOObject ioObject = inputPort.getDataOrNull(IOObject.class);
					if (ioObject instanceof ExampleSet) {
						ExampleSet exampleSet = (ExampleSet) ioObject;
						logInputVolume(op, inputPort, exampleSet.size(), exampleSet.getAttributes().allSize());
					}
				} catch (UserError e) {
					// cannot log volume
				}
			}
		}

		@Override
		public void processFinishedOperator(Process process, Operator op) {
			// log the output volumes of the operator
			for (OutputPort outputPort : op.getOutputPorts().getAllPorts()) {
				try {
					IOObject ioObject = outputPort.getDataOrNull(IOObject.class);
					if (ioObject instanceof ExampleSet) {
						ExampleSet exampleSet = (ExampleSet) ioObject;
						logOutputVolume(op, outputPort, exampleSet.size(), exampleSet.getAttributes().allSize());
					}
				} catch (UserError e) {
					// cannot log volume
				}

			}
			// log the memory volume used
			logMemory();
		}

		@Override
		public void processEnded(Process process) {
			// not needed
		}
	};

	private final Map<Key, Long> counts = new HashMap<>();

	/** flag whether the rowLimit was already exceeded during this session */
	private boolean rowLimitExceeded;

	public static ActionStatisticsCollector getInstance() {
		return INSTANCE;
	}

	protected void start() {
		if (RapidMiner.getExecutionMode().isHeadless()) {
			return;
		}

		long eventMask = AWTEvent.MOUSE_EVENT_MASK;
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

			@Override
			public void eventDispatched(AWTEvent e) {
				if (e.getID() == MouseEvent.MOUSE_RELEASED) {
					final MouseEvent me = (MouseEvent) e;
					Component component = me.getComponent();
					logAction(component);
				}
			}

		}, eventMask);

		RapidMinerGUI.getMainFrame().getDockingDesktop().addDockableStateChangeListener(new DockableStateChangeListener() {

			@Override
			public void dockableStateChanged(DockableStateChangeEvent e) {
				log(TYPE_DOCKABLE, e.getNewState().getDockable().getDockKey().getKey(),
						e.getNewState().getLocation().toString());
			}
		});
		log(TYPE_CONSTANT, "start", null);
	}

	private void logAction(Object component) {
		if (component == null) {
			return;
		}
		if (component instanceof AbstractButton) {
			AbstractButton button = (AbstractButton) component;
			Action action = button.getAction();
			// Only log ResourceActions. Otherwise, we would also log recent files, including file
			// names, etc.
			if (action instanceof ResourceAction) {
				String actionCommand = button.getActionCommand();
				if (actionCommand != null) {
					if (button instanceof JToggleButton || button instanceof JCheckBox) {
						log(TYPE_ACTION, actionCommand, button.isSelected() ? "deselected" : "selected");
					} else {
						log(TYPE_ACTION, actionCommand, "clicked");
					}
				}
			}
		} else if (component instanceof AbstractLinkButton) {
			AbstractLinkButton button = (AbstractLinkButton) component;
			Action action = button.getAction();
			// Only log ResourceActions
			if (action instanceof ResourceAction) {
				log(TYPE_ACTION, ((ResourceAction) action).getKey(), "clicked");
			}
		}
	}

	/**
	 * Logs the operator execution event and adds the {@link ProcessListener} logging the operator
	 * volumes.
	 *
	 * @param process
	 *            the started process
	 */
	public void logExecution(Process process) {
		if (process == null) {
			return;
		}
		// add listener for operator port volume logging
		process.getRootOperator().addProcessListener(operatorVolumeListener);
		List<Operator> allInnerOperators = process.getRootOperator().getAllInnerOperators();
		for (Operator op : allInnerOperators) {
			log(TYPE_OPERATOR, op.getOperatorDescription().getKey(), OPERATOR_EVENT_EXECUTION);
		}
	}

	/**
	 * Logs the execution time for all operators in the process and removes the
	 * {@link ProcessListener} logging the operator volumes.
	 *
	 * @param process
	 *            the finished process
	 */
	public void logExecutionFinished(Process process) {
		if (process == null) {
			return;
		}
		// remove listener for operator port volume logging
		process.getRootOperator().removeProcessListener(operatorVolumeListener);
		Collection<Operator> allInnerOperators = process.getAllOperators();
		for (Operator op : allInnerOperators) {
			// only log if the operator finished
			if (!op.isDirty()) {
				// retrieve execution time stored with the operator
				double executionTime = (double) op.getValue("execution-time").getValue();
				logOperatorExecutionTime(op, (long) executionTime);
			}
		}
	}

	/**
	 * Logs that the user exceeded the row limit and schedules a transmission soon.
	 */
	public void logRowLimitExceeded() {
		log(ActionStatisticsCollector.TYPE_ROW_LIMIT, ActionStatisticsCollector.VALUE_ROW_LIMIT_EXCEEDED,
				ActionStatisticsCollector.ARG_ROW_LIMIT_CHECK);
		if (!rowLimitExceeded) {
			rowLimitExceeded = true;
			UsageStatistics.getInstance().scheduleTransmissionSoon();
		}
	}

	public void logCtaRuleTriggered(String ruleID, String result) {
		log(ActionStatisticsCollector.VALUE_RULE_TRIGGERED, ruleID, result);
		UsageStatistics.getInstance().scheduleTransmissionSoon();
	}

	/**
	 * Logs the volume for the operator input port. Logs the columns, rows and cells (rows *
	 * columns) and for each their sum, min, max and count.
	 *
	 * @param operator
	 *            the operator the input port belongs to
	 * @param port
	 *            the input port for which to log the volume
	 * @param rows
	 *            the rows of the example set at the port
	 * @param columns
	 *            the columns of the example set at the port
	 */
	private void logInputVolume(Operator operator, InputPort port, int rows, int columns) {
		logVolume(TYPE_INPUT_VOLUME, operator, port, rows, columns);
	}

	/**
	 * Logs the volume for the operator output port. Logs the columns, rows and cells (rows *
	 * columns) and for each their sum, min, max and count.
	 *
	 * @param operator
	 *            the operator the output port belongs to
	 * @param port
	 *            the output port for which to log the volume
	 * @param rows
	 *            the rows of the example set at the port
	 * @param columns
	 *            the columns of the example set at the port
	 */
	private void logOutputVolume(Operator operator, OutputPort port, int rows, int columns) {
		logVolume(TYPE_OUTPUT_VOLUME, operator, port, rows, columns);
	}

	public void log(Operator op, String event) {
		if (op == null) {
			return;
		}
		log(TYPE_OPERATOR, op.getOperatorDescription().getKey(), event);
	}

	public void log(String type, String value, String arg) {
		log(type, value, arg, 1);
	}

	/**
	 * Logs the executionTime for the operator. Adjusts the sum, min, max and count of the execution
	 * times logged before.
	 *
	 * @param operator
	 *            the operator to log
	 * @param executionTime
	 *            the execution time (in milliseconds) to log
	 */
	private void logOperatorExecutionTime(Operator operator, long executionTime) {
		logCountSumMinMax(TYPE_OPERATOR, operator.getOperatorDescription().getKey(), OPERATOR_RUNTIME, executionTime);
	}

	/**
	 * Logs sum, max and count of the total memory currently used.
	 */
	private void logMemory() {
		long totalSize = Runtime.getRuntime().totalMemory() / BYTE_TO_MB;
		log(TYPE_MEMORY, MEMORY_USED, MEMORY_ARG + COUNT_INDICATOR);
		log(TYPE_MEMORY, MEMORY_USED, MEMORY_ARG, totalSize);
		logMax(TYPE_MEMORY, MEMORY_USED, MEMORY_ARG, totalSize);
	}

	/**
	 * Logs the volume for an operator port. Logs the columns, rows and cells and for each their
	 * sum, min, max and count.
	 */
	private void logVolume(String type, Operator operator, Port port, int rows, int columns) {
		String value = operator.getOperatorDescription().getKey() + "." + port.getName();
		logCountSumMinMax(type, value, VOLUME_ROWS, rows);
		logCountSumMinMax(type, value, VOLUME_COLUMNS, columns);
		logCountSumMinMax(type, value, VOLUMNE_CELLS, (long) columns * rows);
	}

	/**
	 * For the key given by type, value and arg logs the amount, its minimum and maximum and how
	 * often a amount was logged.
	 */
	private void logCountSumMinMax(String type, String value, String arg, long amount) {
		log(type, value, arg + COUNT_INDICATOR);
		log(type, value, arg, amount);
		logMin(type, value, arg, amount);
		logMax(type, value, arg, amount);
	}

	private void log(String type, String value, String arg, long count) {
		Key key = new Key(type, value, arg);
		CtaEventAggregator.INSTANCE.log(key, count);
		synchronized (counts) {
			Long oldAggregate = counts.get(key);
			if (oldAggregate == null) {
				oldAggregate = 0l;
			}
			counts.put(key, oldAggregate + count);
		}
	}

	/**
	 * Logs the minimum amount that was logged for (type, value, arg) under (type, value, arg_MIN).
	 */
	private void logMin(String type, String value, String arg, long amount) {
		Key key = new Key(type, value, arg + MINIMUM_INDICATOR);
		synchronized (counts) {
			Long oldMin = counts.get(key);
			if (oldMin == null) {
				oldMin = amount;
			}
			counts.put(key, Math.min(oldMin, amount));
		}
	}

	/**
	 * Logs the maximum amount that was logged for (type, value, arg) under (type, value, arg_MAX).
	 */
	private void logMax(String type, String value, String arg, long amount) {
		Key key = new Key(type, value, arg + MAXIMUM_INDICATOR);
		synchronized (counts) {
			Long oldMax = counts.get(key);
			if (oldMax == null) {
				oldMax = amount;
			}
			counts.put(key, Math.max(oldMax, amount));
		}
	}

	private Map<Key, Long> runningTimers = new HashMap<>();

	public void startTimer(String type, String value, String arg) {
		runningTimers.put(new Key(type, value, arg), System.currentTimeMillis());
	}

	public void stopTimer(String type, String value, String arg) {
		Long startTime = runningTimers.remove(new Key(type, value, arg));
		if (startTime != null) {
			log(type, value, arg, System.currentTimeMillis() - startTime);
		}
	}

	protected Element getXML(Document doc) {
		synchronized (counts) {
			Element root = doc.createElement(XML_TAG);
			doc.getDocumentElement().appendChild(root);
			for (Entry<Key, Long> entry : counts.entrySet()) {
				Element actionElement = doc.createElement(TYPE_ACTION);
				Key key = entry.getKey();
				Long count = entry.getValue();
				XMLTools.addTag(actionElement, "type", key.type);
				XMLTools.addTag(actionElement, "value", key.value);
				if (key.arg != null) {
					XMLTools.addTag(actionElement, "arg", key.arg);
				}
				XMLTools.addTag(actionElement, "count", String.valueOf(count));
				root.appendChild(actionElement);
			}
			root.setAttribute("os-name", System.getProperty("os.name"));
			root.setAttribute("os-version", System.getProperty("os.version"));
			return root;
		}
	}

	protected void load(Element element) throws XMLException {
		synchronized (counts) {
			counts.clear();
			NodeList actionElements = element.getElementsByTagName(TYPE_ACTION);
			for (int i = 0; i < actionElements.getLength(); i++) {
				Element actionElement = (Element) actionElements.item(i);
				Key key = new Key(XMLTools.getTagContents(actionElement, "type"),
						XMLTools.getTagContents(actionElement, "value"), XMLTools.getTagContents(actionElement, "arg"));
				counts.put(key, XMLTools.getTagContentsAsLong(actionElement, "count"));
			}
		}
	}

	public void clear() {
		synchronized (counts) {
			counts.clear();
		}
	}

	/** Returns a copy of the current stats. */
	public Map<Key, Long> getCounts() {
		return new HashMap<>(counts);
	}

	public long getCount(String type, String value, String arg) {
		Long count = counts.get(new Key(type, value, arg));
		return count != null ? count : 0;
	}

}
