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
package com.rapidminer.tools.usagestats;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
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
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.AbstractLinkButton;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
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

	private static final String TYPE_CONSTANT = "rapidminer";
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

	public static final String OPERATOR_EVENT_EXECUTION = "EXECUTE";
	public static final String OPERATOR_EVENT_STOPPED = "STOPPED";
	public static final String OPERATOR_EVENT_FAILURE = "FAILURE";
	public static final String OPERATOR_EVENT_USER_ERROR = "USER_ERROR";
	public static final String OPERATOR_EVENT_OPERATOR_EXCEPTION = "OPERATOR_EXCEPTION";
	public static final String OPERATOR_EVENT_RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION";

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

	private final Map<Key, Long> counts = new HashMap<>();

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

	public void logExecution(Process process) {
		if (process == null) {
			return;
		}
		List<Operator> allInnerOperators = process.getRootOperator().getAllInnerOperators();
		for (Operator op : allInnerOperators) {
			log(TYPE_OPERATOR, op.getOperatorDescription().getKey(), OPERATOR_EVENT_EXECUTION);
		}
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

	private void log(String type, String value, String arg, long count) {
		Key key = new Key(type, value, arg);
		synchronized (counts) {
			Long oldAggregate = counts.get(key);
			if (oldAggregate == null) {
				oldAggregate = 0l;
			}
			counts.put(key, oldAggregate + count);
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
