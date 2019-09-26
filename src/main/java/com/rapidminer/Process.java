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
package com.rapidminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import javax.swing.event.EventListenerList;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.core.license.LicenseViolationException;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.license.violation.LicenseViolation;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.DebugMode;
import com.rapidminer.operator.DummyOperator;
import com.rapidminer.operator.ExecutionMode;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectMap;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.PortUserError;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.execution.FlowData;
import com.rapidminer.operator.execution.ProcessFlowFilter;
import com.rapidminer.operator.nio.file.RepositoryBlobObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.report.ReportStream;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryAccessor;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.studio.internal.ProcessFlowFilterRegistry;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.ResultService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.WrapperLoggingHandler;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.XMLParserException;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * <p>
 * This class was introduced to avoid confusing handling of operator maps and other stuff when a new
 * process definition is created. It is also necessary for file name resolving and breakpoint
 * handling.
 * </p>
 *
 * <p>
 * If you want to use RapidMiner from your own application the best way is often to create a process
 * definition from scratch (by adding the complete operator tree to the process' root operator) or
 * from a file (for example created with the GUI beforehand) and start it by invoking the
 * {@link #run()} method.
 * </p>
 *
 * <p>
 * Observers can listen to changes of the associated file, repository location, and context.
 * </p>
 * TODO: Add reasonable class comment
 *
 * @author Ingo Mierswa
 */
public class Process extends AbstractObservable<Process> implements Cloneable {

	public static final int PROCESS_STATE_UNKNOWN = -1;
	public static final int PROCESS_STATE_STOPPED = 0;
	public static final int PROCESS_STATE_PAUSED = 1;
	public static final int PROCESS_STATE_RUNNING = 2;

	/** The root operator of the process. */
	private ProcessRootOperator rootOperator = null;

	/** This is the operator which is currently applied. */
	private Operator currentOperator;

	/**
	 * The process might be connected to this file or repository location which is then used to
	 * resolve relative file names which might be defined as parameters.
	 */
	private ProcessLocation processLocation;

	/**
	 * Indicates if the original process file has been changed by import rules. If this happens,
	 * overwriting will destroy the backward compatibility. This flag indicates that this would
	 * happen during saving.
	 */
	private boolean isProcessConverted = false;

	/**
	 * Indicates how deeply nested the current process is. The original process itself has a depth
	 * of {@code 0}. If that process spawns a new one via an Execute Process operator, the depth of
	 * the new one will be {@code 1}. If the new process also contains an Execute Process operator,
	 * the depth will be {@code 2} and so on. Used to prevent {@link StackOverflowError} when
	 * recursion is too deep (mostly to prevent accidents).
	 */
	private int nestingDepth = 0;

	/**
	 * This list contains all unknown parameter information which existed during the loading of the
	 * process.
	 */
	private final List<UnknownParameterInformation> unknownParameterInformation = new LinkedList<>();

	/** The listeners for breakpoints. */
	private final List<BreakpointListener> breakpointListeners = Collections.synchronizedList(new LinkedList<>());

	/** The list of filters called between each operator */
	private final CopyOnWriteArrayList<ProcessFlowFilter> processFlowFilters = new CopyOnWriteArrayList<>();

	/** The listeners for logging (data tables). */
	private final List<LoggingListener> loggingListeners = Collections.synchronizedList(new LinkedList<>());

	private final List<ProcessStateListener> processStateListeners = Collections.synchronizedList(new LinkedList<>());

	/** The macro handler can be used to replace (user defined) macro strings. */
	private final MacroHandler macroHandler = new MacroHandler(this);

	/**
	 * This map holds the names of all operators in the process. Operators are automatically
	 * registered during adding and unregistered after removal.
	 */
	private Map<String, Operator> operatorNameMap = new HashMap<>();

	/**
	 * Maps names of ProcessLog operators to Objects, that these Operators use for collecting
	 * statistics (objects of type {@link DataTable}).
	 */
	private final Map<String, DataTable> dataTableMap = new HashMap<>();

	/**
	 * Maps names of report streams to reportStream objects
	 */
	private final Map<String, ReportStream> reportStreamMap = new HashMap<>();

	/**
	 * Stores IOObjects according to a specified name for the runtime of the process.
	 */
	private final Map<String, IOObject> storageMap = new HashMap<>();

	/**
	 * Stores IOObjects according to a specified name for a long-term scope like the session of
	 * RapidMiner or a RapidMiner Server app session
	 */
	private IOObjectMap ioObjectCache = RapidMiner.getGlobalIOObjectCache();

	/** Indicates the current process state. */
	private int processState = PROCESS_STATE_STOPPED;

	/** Indicates whether operators should be executed always or only when dirty. */
	private transient ExecutionMode executionMode = ExecutionMode.ALWAYS;

	/** Indicates whether we are updating meta data. */
	private transient DebugMode debugMode = DebugMode.DEBUG_OFF;

	private final transient Logger logger = makeLogger();

	/** @deprecated Use {@link #getLogger()} */
	@Deprecated
	private final transient LoggingHandler logService = new WrapperLoggingHandler(logger);

	private ProcessContext context = new ProcessContext();

	/** Message generated during import by {@link XMLImporter}. */
	private String importMessage;

	private final Annotations annotations = new Annotations();

	private RepositoryAccessor repositoryAccessor;

	/**
	 * Indicates whether the {@link IOContainer} returned by {@link #run()} might contain
	 * <code>null</code> values for empty results.
	 */
	private boolean omitNullResults = true;

	// -------------------
	// Constructors
	// -------------------

	/** Constructs an process consisting only of a SimpleOperatorChain. */
	public Process() {
		try {
			ProcessRootOperator root = OperatorService.createOperator(ProcessRootOperator.class);
			root.rename(root.getOperatorDescription().getName());
			setRootOperator(root);
		} catch (Exception e) {
			throw new RuntimeException("Cannot initialize root operator of the process: " + e.getMessage(), e);
		}
		initContext();
	}

	public Process(final File file) throws IOException, XMLException {
		this(file, null);
	}

	/**
	 * Creates a new process from the given process file. This might have been created with the GUI
	 * beforehand.
	 */
	public Process(final File file, final ProgressListener progressListener) throws IOException, XMLException {
		this.processLocation = new FileProcessLocation(file);
		initContext();
		try (FileInputStream fis = new FileInputStream(file); Reader in = new InputStreamReader(fis, "UTF-8")) {
			readProcess(in, progressListener);
		}
	}

	/**
	 * Creates a new process from the given XML copying state information not covered by the XML
	 * from the parameter process.
	 */
	public Process(final String xml, final Process process) throws IOException, XMLException {
		this(xml);
		this.processLocation = process.processLocation;
	}

	/** Reads an process configuration from an XML String. */
	public Process(final String xmlString) throws IOException, XMLException {
		initContext();
		StringReader in = new StringReader(xmlString);
		readProcess(in);
		in.close();
	}

	/** Reads an process configuration from the given reader. */
	public Process(final Reader in) throws IOException, XMLException {
		initContext();
		readProcess(in);
	}

	/** Reads an process configuration from the given stream. */
	public Process(final InputStream in) throws IOException, XMLException {
		initContext();
		readProcess(new InputStreamReader(in, XMLImporter.PROCESS_FILE_CHARSET));
	}

	/** Reads an process configuration from the given URL. */
	public Process(final URL url) throws IOException, XMLException {
		initContext();
		try (Reader in = new InputStreamReader(WebServiceTools.openStreamFromURL(url), getEncoding(null))) {
			readProcess(in);
		}

	}

	protected Logger makeLogger() {
		return Logger.getLogger(Process.class.getName());
	}

	private void initContext() {
		getContext().addObserver(delegatingContextObserver, false);
	}

	/**
	 * Clone constructor. Makes a deep clone of the operator tree and the process file. The same
	 * applies for the operatorNameMap. The breakpoint listeners are copied by reference and all
	 * other fields are initialized like for a fresh process.
	 */
	private Process(final Process other) {
		this();
		setRootOperator((ProcessRootOperator) other.rootOperator.cloneOperator(other.rootOperator.getName(), false));
		this.currentOperator = null;
		if (other.processLocation != null) {
			this.processLocation = other.processLocation;
		} else {
			this.processLocation = null;
		}
	}

	private void initLogging(final int logVerbosity) {
		if (logVerbosity >= 0) {
			logger.setLevel(WrapperLoggingHandler.LEVELS[logVerbosity]);
		} else {
			logger.setLevel(Level.INFO);
		}
	}

	@Override
	public Object clone() {
		return new Process(this);
	}

	/**
	 * @deprecated Use {@link #setProcessState(int)} instead
	 */
	@Deprecated
	public synchronized void setExperimentState(final int state) {
		setProcessState(state);
	}

	private void setProcessState(final int state) {
		int oldState = this.processState;
		this.processState = state;
		fireProcessStateChanged(oldState, state);
	}

	/**
	 * @deprecated Use {@link #getProcessState()} instead
	 */
	@Deprecated
	public synchronized int getExperimentState() {
		return getProcessState();
	}

	public int getProcessState() {
		return this.processState;
	}

	// -------------------------
	// User initiated state changes
	// ---------------------------
	/** Adds the given process state listener. */
	public void addProcessStateListener(ProcessStateListener processStateListener) {
		this.processStateListeners.add(processStateListener);
	}

	/** Removes the given process state listener. */
	public void removeProcessStateListener(ProcessStateListener processStateListener) {
		this.processStateListeners.remove(processStateListener);
	}

	private void fireProcessStateChanged(int stateBefore, int newState) {
		// sanity check
		if (stateBefore == newState) {
			return;
		}
		List<ProcessStateListener> listeners;
		synchronized (processStateListeners) {
			listeners = new LinkedList<>(processStateListeners);
		}
		for (ProcessStateListener listener : listeners) {
			switch (newState) {
				case PROCESS_STATE_PAUSED:
					listener.paused(this);
					break;
				case PROCESS_STATE_STOPPED:
					listener.stopped(this);
					break;
				default:
					if (stateBefore == PROCESS_STATE_STOPPED) {
						listener.started(this);
					} else {
						listener.resumed(this);
					}
					break;
			}
		}

	}

	// -------------------------
	// Logging
	// -------------------------

	/**
	 * @deprecated use {@link #getLogger()} instead
	 */
	@Deprecated
	public LoggingHandler getLog() {
		return this.logService;
	}

	public Logger getLogger() {
		return this.logger;
	}

	// -------------------------
	// Macro Handler
	// -------------------------

	/** Returns the macro handler. */
	public MacroHandler getMacroHandler() {
		return this.macroHandler;
	}

	/** Clears all macros. */
	public void clearMacros() {
		this.getMacroHandler().clear();
	}

	// -------------------------
	// IOObject Storage
	// -------------------------

	/** Stores the object with the given name. */
	public void store(final String name, final IOObject object) {
		this.storageMap.put(name, object);
	}

	/** Retrieves the stored object. */
	public IOObject retrieve(final String name, final boolean remove) {
		if (remove) {
			return this.storageMap.remove(name);
		} else {
			return this.storageMap.get(name);
		}
	}

	/** Clears all macros. */
	public void clearStorage() {
		this.storageMap.clear();
	}

	// -------------------------
	// State storage
	// -------------------------

	/**
	 * Injects another {@link IOObject} cache (to remember and recall {@link IOObject}s during a
	 * long-term scope)
	 *
	 * If {@link #ioObjectCache} is null, the setter does not have any effect
	 */
	public void setIOObjectCache(IOObjectMap ioObjectCache) {
		if (ioObjectCache != null) {
			this.ioObjectCache = ioObjectCache;
		}
	}

	/**
	 * Returns the {@link IOObject} cache (to remember and recall {@link IOObject}s during a
	 * long-term scope), designed to be manipulated by operators in the process
	 *
	 * @return the IOObjectCache of the process
	 */
	public IOObjectMap getIOObjectCache() {
		return ioObjectCache;
	}

	// -------------------------
	// Data Tables (Logging)
	// -------------------------

	/** Adds the given logging listener. */
	public void addLoggingListener(final LoggingListener loggingListener) {
		this.loggingListeners.add(loggingListener);
	}

	/** Removes the given logging listener. */
	public void removeLoggingListener(final LoggingListener loggingListener) {
		this.loggingListeners.remove(loggingListener);
	}

	/** Returns true if a data table object with the given name exists. */
	public boolean dataTableExists(final String name) {
		return dataTableMap.get(name) != null;
	}

	/**
	 * Adds the given data table.
	 */
	public void addDataTable(final DataTable table) {
		dataTableMap.put(table.getName(), table);
		synchronized (loggingListeners) {
			for (LoggingListener listener : loggingListeners) {
				listener.addDataTable(table);
			}
		}
	}

	/** Clears a single data table, i.e. removes all entries. */
	public void clearDataTable(final String name) {
		DataTable table = getDataTable(name);
		if (table instanceof SimpleDataTable) {
			((SimpleDataTable) table).clear();
		}
	}

	/** Deletes a single data table. */
	public void deleteDataTable(final String name) {
		if (dataTableExists(name)) {
			DataTable table = dataTableMap.remove(name);
			synchronized (loggingListeners) {
				for (LoggingListener listener : loggingListeners) {
					listener.removeDataTable(table);
				}
			}
		}
	}

	/**
	 * Returns the data table associated with the given name. If the name was not used yet, an empty
	 * DataTable object is created with the given columnNames.
	 */
	public DataTable getDataTable(final String name) {
		return dataTableMap.get(name);
	}

	/** Returns all data tables. */
	public Collection<DataTable> getDataTables() {
		return dataTableMap.values();
	}

	/** Removes all data tables before running a new process. */
	private void clearDataTables() {
		dataTableMap.clear();
	}

	// ------------------------------
	// Report Streams
	// ------------------------------

	/**
	 * This method adds a new report stream with the given name
	 */
	public void addReportStream(final ReportStream stream) {
		reportStreamMap.put(stream.getName(), stream);
	}

	/**
	 * Returns the reportStream with given name
	 */
	public ReportStream getReportStream(final String name) {
		if (name == null || name.length() == 0) {
			if (reportStreamMap.size() == 1) {
				return reportStreamMap.values().iterator().next();
			} else {
				return null;
			}
		} else {
			return reportStreamMap.get(name);
		}
	}

	/**
	 * Removes this reportStream from process. This report Stream will not be notified about new
	 * report items.
	 *
	 * @param name
	 *            of the report stream given in the ReportGenerator operator
	 */
	public void removeReportStream(final String name) {
		reportStreamMap.remove(name);
	}

	public void clearReportStreams() {
		reportStreamMap.clear();
	}

	// ----------------------
	// Operator Handling
	// ----------------------

	/** Sets the current root operator. This might lead to a new registering of operator names. */
	public void setRootOperator(final ProcessRootOperator root) {
		if (this.rootOperator != null) {
			this.rootOperator.removeObserver(delegatingOperatorObserver);
		}
		this.rootOperator = root;
		this.rootOperator.addObserver(delegatingOperatorObserver, false);
		this.operatorNameMap.clear();
		this.rootOperator.setProcess(this);
	}

	/** Delivers the current root operator. */
	public ProcessRootOperator getRootOperator() {
		return rootOperator;
	}

	/** Returns the operator with the given name. */
	public Operator getOperator(final String name) {
		return operatorNameMap.get(name);
	}

	/** Returns the operator that is currently being executed. */
	public Operator getCurrentOperator() {
		return currentOperator;
	}

	/** Returns a Collection view of all operators. */
	public Collection<Operator> getAllOperators() {
		List<Operator> result = rootOperator.getAllInnerOperators();
		result.add(0, rootOperator);
		return result;
	}

	/** Returns a Set view of all operator names (i.e. Strings). */
	public Collection<String> getAllOperatorNames() {
		return getAllOperators().stream().map(Operator::getName).collect(Collectors.toList());
	}

	/** Sets the operator that is currently being executed. */
	public void setCurrentOperator(final Operator operator) {
		this.currentOperator = operator;
	}

	// -------------------------------------
	// start, stop, resume, breakpoints
	// -------------------------------------

	/** We synchronize on this object to wait and resume operation. */
	private final Object breakpointLock = new Object();

	/** Pauses the process at a breakpoint. */
	public void pause(final Operator operator, final IOContainer iocontainer, final int breakpointType) {
		setProcessState(PROCESS_STATE_PAUSED);
		fireBreakpointEvent(operator, iocontainer, breakpointType);
		while (getProcessState() == Process.PROCESS_STATE_PAUSED) {
			synchronized (breakpointLock) {
				try {
					breakpointLock.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/** Resumes the process after it has been paused. */
	public void resume() {
		setProcessState(PROCESS_STATE_RUNNING);
		synchronized (breakpointLock) {
			breakpointLock.notifyAll();
		}
		fireResumeEvent();
	}

	/** Stops the process as soon as possible. */
	public void stop() {
		this.setProcessState(PROCESS_STATE_STOPPED);
		synchronized (breakpointLock) {
			breakpointLock.notifyAll();
		}
	}

	/** Stops the process as soon as possible. */
	public void pause() {
		this.setProcessState(PROCESS_STATE_PAUSED);
	}

	/** Returns true iff the process should be stopped. */
	public boolean shouldStop() {
		return getProcessState() == PROCESS_STATE_STOPPED;
	}

	/** Returns true iff the process should be stopped. */
	public boolean shouldPause() {
		return getProcessState() == PROCESS_STATE_PAUSED;
	}

	// --------------------
	// Filters between operators handling
	// --------------------

	/**
	 * Add a new {@link ProcessFlowFilter} to this process. The filter will be called directly
	 * before and after each operator. Refer to {@link ProcessFlowFilter} for more information.
	 * <p>
	 * If the given filter instance is already registered, it will not be added a second time.
	 * </p>
	 *
	 * @param filter
	 *            the filter instance to add
	 */
	public void addProcessFlowFilter(ProcessFlowFilter filter) {
		if (filter == null) {
			throw new IllegalArgumentException("filter must not be null!");
		}
		processFlowFilters.addIfAbsent(filter);
	}

	/**
	 * Remove a {@link ProcessFlowFilter} from this process. Does nothing if the filter is unknown.
	 *
	 * @param filter
	 *            the filter instance to remove
	 */
	public void removeProcessFlowFilter(ProcessFlowFilter filter) {
		if (filter == null) {
			throw new IllegalArgumentException("filter must not be null!");
		}
		processFlowFilters.remove(filter);
	}

	/**
	 * Notifies all registered {@link ProcessFlowFilter}s that the next operator in the process is
	 * about to be executed.
	 *
	 * @param previousOperator
	 *            the previous operator; may be {@code null} for the first operator in a subprocess
	 * @param nextOperator
	 *            the next operator to be called, never {@code null}
	 * @param input
	 *            the list of all input data for the next operator. If {@code null}, an empty list
	 *            will be used
	 */
	public void fireProcessFlowBeforeOperator(Operator previousOperator, Operator nextOperator, List<FlowData> input)
			throws OperatorException {
		if (nextOperator == null) {
			throw new IllegalArgumentException("nextOperator must not be null!");
		}
		if (input == null) {
			input = Collections.emptyList();
		}
		for (ProcessFlowFilter filter : processFlowFilters) {
			try {
				filter.preOperator(previousOperator, nextOperator, input);
			} catch (OperatorException oe) {
				throw oe;
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "com.rapidminer.Process.process_flow_filter_failed", e);
			}
		}
	}

	/**
	 * Notifies all registered {@link ProcessFlowFilter}s that an operator in the process has just
	 * finished execution.
	 *
	 * @param previousOperator
	 *            the operator which just finished, never {@code null}
	 * @param nextOperator
	 *            the next operator to be called; may be {@code null} if this was the last operator
	 *            in a subprocess
	 * @param output
	 *            the list of all output data from the previous operator. If {@code null}, an empty
	 *            list will be used
	 */
	public void fireProcessFlowAfterOperator(Operator previousOperator, Operator nextOperator, List<FlowData> output)
			throws OperatorException {
		if (previousOperator == null) {
			throw new IllegalArgumentException("previousOperator must not be null!");
		}
		if (output == null) {
			output = Collections.emptyList();
		}
		for (ProcessFlowFilter filter : processFlowFilters) {
			try {
				filter.postOperator(previousOperator, nextOperator, output);
			} catch (OperatorException oe) {
				throw oe;
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "com.rapidminer.Process.process_flow_filter_failed", e);
			}
		}
	}

	/**
	 * Copies the registered {@link ProcessFlowFilter}s of this process to the given process
	 * instance.
	 *
	 * @param otherProcess
	 *            the process who should get all process flow listeners which are registered to this
	 *            process instance
	 */
	public void copyProcessFlowListenersToOtherProcess(Process otherProcess) {
		if (otherProcess == null) {
			throw new IllegalArgumentException("otherProcess must not be null!");
		}
		for (ProcessFlowFilter filter : processFlowFilters) {
			otherProcess.addProcessFlowFilter(filter);
		}
	}

	// --------------------
	// Breakpoint Handling
	// --------------------

	/** Adds a breakpoint listener. */
	public void addBreakpointListener(final BreakpointListener listener) {
		breakpointListeners.add(listener);
	}

	/** Removes a breakpoint listener. */
	public void removeBreakpointListener(final BreakpointListener listener) {
		breakpointListeners.remove(listener);
	}

	/** Fires the event that the process was paused. */
	private void fireBreakpointEvent(final Operator operator, final IOContainer ioContainer, final int location) {
		LinkedList<BreakpointListener> l;
		synchronized (breakpointListeners) {
			l = new LinkedList<>(breakpointListeners);
		}
		for (BreakpointListener listener : l) {
			listener.breakpointReached(this, operator, ioContainer, location);
		}
	}

	/** Fires the event that the process was resumed. */
	public void fireResumeEvent() {
		LinkedList<BreakpointListener> l;
		synchronized (breakpointListeners) {
			l = new LinkedList<>(breakpointListeners);
		}
		for (BreakpointListener listener : l) {
			listener.resume();
		}
	}

	// -----------------
	// Checks
	// -----------------

	/**
	 * Delivers the information about unknown parameter types which occurred during process creation
	 * (from streams or files).
	 */
	public List<UnknownParameterInformation> getUnknownParameters() {
		return this.unknownParameterInformation;
	}

	/**
	 * Clears the information about unknown parameter types which occurred during process creation
	 * (from streams or files).
	 */
	public void clearUnknownParameters() {
		this.unknownParameterInformation.clear();
	}

	/**
	 * Checks for correct number of inner operators, properties, and io.
	 *
	 * @deprecated Use {@link #checkProcess(IOContainer)} instead
	 */
	@Deprecated
	public boolean checkExperiment(final IOContainer inputContainer) {
		return checkProcess(inputContainer);
	}

	/** Checks for correct number of inner operators, properties, and io. */
	public boolean checkProcess(final IOContainer inputContainer) {
		rootOperator.checkAll();
		return true;
	}

	// ------------------
	// Running
	// ------------------

	/**
	 * This method initializes the process, the operators, and the services and must be invoked at
	 * the beginning of run. It also resets all apply counts.
	 */
	private final void prepareRun(final int logVerbosity) throws OperatorException {
		initLogging(logVerbosity);

		setProcessState(PROCESS_STATE_RUNNING);
		getLogger().fine("Initialising process setup.");

		RandomGenerator.init(this);
		ResultService.init(this);

		clearDataTables();
		clearReportStreams();
		clearMacros();
		clearStorage();
		if (getExecutionMode() != ExecutionMode.ONLY_DIRTY) {
			getRootOperator().clear(Port.CLEAR_DATA);
		}
		AttributeFactory.resetNameCounters();

		getLogger().fine("Process initialised.");

		// add process start macro value here already to have it available for root parameters
		// can be overwritten if it is passed to the run() method via the macro map
		getMacroHandler().addMacro(MacroHandler.PROCESS_START,
				MacroHandler.DATE_FORMAT.get().format(new Date(System.currentTimeMillis())));
	}

	/**
	 * Checks whether input data was specified in the process context. Will return true if at least
	 * one input port has specified data.
	 *
	 * @param firstPort
	 *            The first port to check
	 * @return true if at least one input port has specified data.
	 */
	private boolean checkForInitialData(int firstPort) {
		for (int i = firstPort; i < context.getInputRepositoryLocations().size(); i++) {
			String location = context.getInputRepositoryLocations().get(i);
			if (location == null || location.length() == 0) {
				continue;
			}
			if (i >= rootOperator.getSubprocess(0).getInnerSources().getNumberOfPorts()) {
				break;
			}
			return true;
		}
		return false;
	}

	/**
	 * Loads results from the repository if specified in the {@link ProcessContext}.
	 *
	 * @param firstPort
	 *            Specifies the first port which is read from the ProcessContext. This enables the
	 *            possibility to skip ports for which input is already specified via the input
	 *            parameter of the run() method.
	 */
	protected void loadInitialData(final int firstPort) throws UserError {
		loadInitialData(firstPort, null);
	}

	/**
	 * Loads results from the repository if specified in the {@link ProcessContext}. Will also show
	 * the progress of loading if a {@link ProgressListener} is specified.
	 *
	 * @param firstPort
	 *            Specifies the first port which is read from the ProcessContext. This enables the
	 *            possibility to skip ports for which input is already specified via the input
	 *            parameter of the run() method.
	 * @param progressListener
	 *            The progress listener for loading the data. Can be null.
	 */
	protected void loadInitialData(final int firstPort, ProgressListener progressListener) throws UserError {
		ProcessContext context = getContext();
		if (context.getInputRepositoryLocations().isEmpty()) {
			if (progressListener != null) {
				progressListener.complete();
			}
			return;
		}
		if (progressListener != null) {
			progressListener.setTotal(context.getInputRepositoryLocations().size() - firstPort - 1);
			progressListener.setCompleted(0);
		}
		getLogger()
				.info("Loading initial data" + (firstPort > 0 ? " (starting at port " + (firstPort + 1) + ")" : "") + ".");
		for (int i = firstPort; i < context.getInputRepositoryLocations().size(); i++) {
			if (shouldStop()) {
				return;
			}
			String location = context.getInputRepositoryLocations().get(i);
			if (location == null || location.length() == 0) {
				getLogger().fine("Input #" + (i + 1) + " not specified.");
				if (progressListener != null) {
					progressListener.setCompleted(i - firstPort + 1);
				}
				continue;
			}
			if (i >= rootOperator.getSubprocess(0).getInnerSources().getNumberOfPorts()) {
				getLogger().warning("No input port available for process input #" + (i + 1) + ": " + location);
				int rest = context.getInputRepositoryLocations().size() - i - 1;
				if (rest != 0) {
					getLogger().warning("Aborting loading " + rest + " more process input(s)");
				}
				if (progressListener != null) {
					progressListener.complete();
				}
				break;
			}
			OutputPort port = rootOperator.getSubprocess(0).getInnerSources().getPortByIndex(i);
			RepositoryLocation loc;
			try {
				loc = resolveRepositoryLocation(location);
			} catch (MalformedRepositoryLocationException | UserError e1) {
				if (progressListener != null) {
					progressListener.complete();
				}
				throw new PortUserError(port, 325, e1.getMessage());
			}
			try {
				Entry entry = loc.locateEntry();
				if (entry == null) {
					if (progressListener != null) {
						progressListener.complete();
					}
					throw new PortUserError(port, 312, loc, "Entry does not exist.");
				}
				if (entry instanceof IOObjectEntry) {
					getLogger().info("Assigning " + loc + " to input port " + port.getSpec() + ".");
					// only deliver the data if the port is really connected
					if (port.isConnected()) {
						port.deliver(((IOObjectEntry) entry).retrieveData(null));
					}
				} else if (entry instanceof BlobEntry) {
					getLogger().info("Assigning " + loc + " to input port " + port.getSpec() + ".");
					// only deliver the data if the port is really connected
					if (port.isConnected()) {
						port.deliver(new RepositoryBlobObject(loc));
					}
				} else {
					getLogger().info("Cannot assigning " + loc + " to input port " + port.getSpec()
							+ ": Repository location does not reference an IOObject entry.");
					if (progressListener != null) {
						progressListener.complete();
					}
					throw new PortUserError(port, 312, loc, "Not an IOObject entry.");
				}
				if (progressListener != null) {
					progressListener.setCompleted(i - firstPort + 1);
				}
			} catch (RepositoryException e) {
				if (progressListener != null) {
					progressListener.complete();
				}
				throw new PortUserError(port, 312, loc, e.getMessage());
			}
		}
	}

	/** Stores the results in the repository if specified in the {@link ProcessContext}. */
	protected void saveResults() throws UserError {
		ProcessContext context = getContext();
		if (context.getOutputRepositoryLocations().isEmpty()) {
			return;
		}
		getLogger().info("Saving results.");
		for (int i = 0; i < context.getOutputRepositoryLocations().size(); i++) {
			String locationStr = context.getOutputRepositoryLocations().get(i);
			if (locationStr == null || locationStr.length() == 0) {
				getLogger().fine("Output #" + (i + 1) + " not specified.");
			} else {
				if (i >= rootOperator.getSubprocess(0).getInnerSinks().getNumberOfPorts()) {
					getLogger().warning("No output port corresponding to process output #" + (i + 1) + ": " + locationStr);
				} else {
					InputPort port = rootOperator.getSubprocess(0).getInnerSinks().getPortByIndex(i);
					RepositoryLocation location;
					try {
						location = rootOperator.getProcess().resolveRepositoryLocation(locationStr);
					} catch (MalformedRepositoryLocationException | UserError e) {
						throw new PortUserError(port, 325, e.getMessage());
					}
					IOObject data = port.getDataOrNull(IOObject.class);
					if (data == null) {
						getLogger().warning(
								"Nothing to store at " + location + ": No results produced at " + port.getSpec() + ".");
					} else {
						try {
							RepositoryAccessor repositoryAccessor = getRepositoryAccessor();
							location.setAccessor(repositoryAccessor);
							RepositoryManager.getInstance(repositoryAccessor).store(data, location, rootOperator);

						} catch (RepositoryException e) {
							throw new PortUserError(port, 315, location, e.getMessage());
						}
					}
				}
			}
		}
	}

	public void applyContextMacros() {
		for (Pair<String, String> macro : context.getMacros()) {
			getLogger().fine("Defining context macro: " + macro.getFirst() + " = " + macro.getSecond() + ".");
			getMacroHandler().addMacro(macro.getFirst(), macro.getSecond());
		}
	}

	/** Starts the process with no input. */
	public final IOContainer run() throws OperatorException {
		return run(new IOContainer());
	}

	/** Starts the process with the given log verbosity. */
	public final IOContainer run(final int logVerbosity) throws OperatorException {
		return run(new IOContainer(), logVerbosity);
	}

	/** Starts the process with the given input. */
	public final IOContainer run(final IOContainer input) throws OperatorException {
		return run(input, LogService.UNKNOWN_LEVEL);
	}

	/** Starts the process with the given input. The process uses the given log verbosity. */
	public final IOContainer run(final IOContainer input, final int logVerbosity) throws OperatorException {
		return run(input, logVerbosity, null);
	}

	/**
	 * Starts the process with the given input. The process uses a default log verbosity. The
	 * boolean flag indicates if some static initializations should be cleaned before the process is
	 * started. This should usually be true but it might be useful to set this to false if, for
	 * example, several process runs uses the same object visualizer which would have been cleaned
	 * otherwise.
	 */
	@Deprecated
	public final IOContainer run(final IOContainer input, final boolean unused) throws OperatorException {
		return run(input, LogService.UNKNOWN_LEVEL);
	}

	/**
	 * Starts the process with the given input. The process uses the given log verbosity. The
	 * boolean flag indicates if some static initializations should be cleaned before the process is
	 * started. This should usually be true but it might be useful to set this to false if, for
	 * example, several process runs uses the same object visualizer which would have been cleaned
	 * otherwise.
	 */
	@Deprecated
	public final IOContainer run(final IOContainer input, final int logVerbosity, final boolean cleanUp)
			throws OperatorException {
		return run(input, logVerbosity, null);
	}

	/**
	 * Starts the process with the given input. The process uses the given log verbosity. The
	 * boolean flag indicates if some static initializations should be cleaned before the process is
	 * started. This should usually be true but it might be useful to set this to false if, for
	 * example, several process runs uses the same object visualizer which would have been cleaned
	 * otherwise.
	 *
	 * Since the macros are cleaned then as well it is not possible to set macros to a process but
	 * with the given macroMap of this method.
	 */
	@Deprecated
	public final IOContainer run(final IOContainer input, final int logVerbosity, final boolean cleanUp,
			final Map<String, String> macroMap) throws OperatorException {
		return run(input, logVerbosity, macroMap);

	}

	public final IOContainer run(final IOContainer input, final int logVerbosity, final Map<String, String> macroMap)
			throws OperatorException {
		return run(input, logVerbosity, macroMap, true);
	}

	/**
	 * Starts the process with the given input. The process uses the given log verbosity.
	 *
	 * If input is not null, it is delivered to the input ports of the process. If it is null or
	 * empty, the input is read instead from the locations specified in the {@link ProcessContext}.
	 *
	 * If input contains less IOObjects than are specified in the context, the remaining ones are
	 * read according to the context.
	 *
	 * @param storeOutput
	 *            Specifies if the output of the process should be saved. This is useful, if you
	 *            embed a process using the Execute Process operator, and do not want to store the
	 *            output as specified by the process context.
	 */
	public final IOContainer run(final IOContainer input, int logVerbosity, final Map<String, String> macroMap,
			final boolean storeOutput) throws OperatorException {
		ActionStatisticsCollector.getInstance().logExecutionStarted(this);
		try {
			// make sure the process flow filter is registered
			ProcessFlowFilter filter = ProcessFlowFilterRegistry.INSTANCE.getProcessFlowFilter();
			if (filter != null && !processFlowFilters.contains(filter)) {
				addProcessFlowFilter(filter);
			}

			// make sure licensing constraints are not violated
			// iterate over all operators in the process
			for (Operator op : rootOperator.getAllInnerOperators()) {
				// we only care about enabled operators
				if (op.isEnabled()) {

					// Check for annotations that constrain access to the current operator
					List<LicenseViolation> licenseViolations = ProductConstraintManager.INSTANCE.checkAnnotationViolations(op,
							true);
					if (!licenseViolations.isEmpty()) {
						throw new LicenseViolationException(op, licenseViolations);
					}

					// Check if the given operator is blacklisted
					if (OperatorService.isOperatorBlacklisted(op.getOperatorDescription().getKey())) {
						throw new UserError(op, "operator_blacklisted");
					}

					// as a side effect mark all enabled operators as dirty
					// so it is clear which ones have already been executed
					op.makeDirty();
				}
			}

			int myVerbosity = rootOperator.getParameterAsInt(ProcessRootOperator.PARAMETER_LOGVERBOSITY);
			if (logVerbosity == LogService.UNKNOWN_LEVEL) {
				logVerbosity = LogService.OFF;
			}
			logVerbosity = Math.min(logVerbosity, myVerbosity);

			prepareRun(logVerbosity);

			// apply macros
			applyContextMacros();
			if (macroMap != null) {
				for (Map.Entry<String, String> entry : macroMap.entrySet()) {
					getMacroHandler().addMacro(entry.getKey(), entry.getValue());
				}
			}

			Handler logHandler = generateLogHandler();
			if (logHandler != null) {
				getLogger().addHandler(logHandler);
			}

			long start = System.currentTimeMillis();

			rootOperator.processStarts();

			final int firstInput = input != null ? input.getIOObjects().length : 0;
			if (checkForInitialData(firstInput)) {
				// load data as specified in process context
				ProgressThread pt = new ProgressThread("load_context_data", false) {

					@Override
					public void run() {
						try {
							loadInitialData(firstInput, getProgressListener());
							setLastInitException(null);
						} catch (ProgressThreadStoppedException ptse) {
							// do nothing, it's checked below (pt.isCancelled)
						} catch (Exception e) {
							setLastInitException(e);
						}
					}
				};
				pt.setShowDialogTimerDelay(5000);
				pt.setStartDialogShowTimer(true);

				pt.startAndWait();
				if (lastInitException != null) {
					Throwable e = lastInitException;
					lastInitException = null;
					finishProcess(logHandler);
					OperatorException oe;
					if (e instanceof OperatorException) {
						oe = (OperatorException) e;
					} else {
						oe = new OperatorException("context_problem_other", e, e.getMessage());
					}
					throw oe;
				}
				if (pt.isCancelled() || shouldStop()) {
					finishProcess(logHandler);
					throw new ProcessStoppedException();
				}
			}
			return execute(input, storeOutput, logHandler, start);
		} catch (Exception e) {
			ActionStatisticsCollector.getInstance().logExecutionException(this, e);

			throw e;
		}
	}

	private IOContainer execute(IOContainer input, boolean storeOutput, Handler logHandler, long start) throws OperatorException {
		// fetching process name for logging
		final String name;
		if (getProcessLocation() != null) {
			name = getProcessLocation().toString();
			getLogger().log(Level.INFO, () -> "Process " + name + " starts");
		} else {
			name = null;
			getLogger().log(Level.INFO, "Process starts");
		}
		getLogger().log(Level.FINE, () -> "Process:" + Tools.getLineSeparator() + getRootOperator().createProcessTree(3));

		try {
			ActionStatisticsCollector.getInstance().logExecution(this);

			IOContainer result;
			// RA-2105: prevent pooled process execution for web apps
			if (rootOperator.getUserData("WEBAPP_EXECUTION") != null) {
				result = executeRoot(input, storeOutput);
			} else {
				result = executeRootInPool(input, storeOutput);
			}
			long end = System.currentTimeMillis();

			getLogger().log(Level.FINE, () -> "Process:" + Tools.getLineSeparator() + getRootOperator().createProcessTree(3));
			if (name != null) {
				getLogger().log(Level.INFO, () -> "Process " + name + " finished successfully after " + Tools.formatDuration(end - start));
			} else {
				getLogger().log(Level.INFO, () -> "Process finished successfully after " + Tools.formatDuration(end - start));
			}

			ActionStatisticsCollector.getInstance().logExecutionSuccess(this);

			return result;
		} catch (ProcessStoppedException e) {
			Operator op = getOperator(e.getOperatorName());
			ActionStatisticsCollector.getInstance().log(op, ActionStatisticsCollector.OPERATOR_EVENT_STOPPED);
			throw e;
		} catch (UserError e) {
			ActionStatisticsCollector.getInstance().log(getCurrentOperator(), ActionStatisticsCollector.OPERATOR_EVENT_FAILURE);
			ActionStatisticsCollector.getInstance().log(e.getOperator(), ActionStatisticsCollector.OPERATOR_EVENT_USER_ERROR);
			throw e;
		} catch (OperatorException e) {
			ActionStatisticsCollector.getInstance().log(getCurrentOperator(), ActionStatisticsCollector.OPERATOR_EVENT_FAILURE);
			ActionStatisticsCollector.getInstance().log(getCurrentOperator(), ActionStatisticsCollector.OPERATOR_EVENT_OPERATOR_EXCEPTION);
			throw e;
		} finally {
			finishProcess(logHandler);
		}
	}

	private IOContainer executeRootInPool(IOContainer input, boolean storeOutput) throws OperatorException {
		IOContainer result;
		try {
			RandomGenerator.stash(this);
			List<IOContainer> containers = Resources.getConcurrencyContext(rootOperator)
					.call(Collections.singletonList(() -> {
						RandomGenerator.restore(this);
						return executeRoot(input, storeOutput);
					}));
			result = containers.get(0);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			//all other checked exceptions must come from called method executeRoot
			throw (OperatorException) e.getCause();
		} catch (ExecutionStoppedException e) {
			throw new ProcessStoppedException();
		}
		return result;
	}

	private IOContainer executeRoot(IOContainer input, boolean storeOutput) throws OperatorException {
		if (input != null) {
			rootOperator.deliverInput(Arrays.asList(input.getIOObjects()));
		}
		rootOperator.execute();
		rootOperator.checkForStop();
		if (storeOutput) {
			saveResults();
		}
		return rootOperator.getResults(isOmittingNullResults());
	}

	/**
	 * Sets up the {@link Handler}} for the executed process.
	 *
	 * @throws UndefinedParameterError
	 */
	private Handler generateLogHandler() throws UndefinedParameterError {
		String logFilename = rootOperator.getParameter(ProcessRootOperator.PARAMETER_LOGFILE);
		Handler logHandler = null;
		if (logFilename != null) {
			try {
				logHandler = new FileHandler(logFilename);
				logHandler.setFormatter(new SimpleFormatter());
				logHandler.setLevel(Level.ALL);
				getLogger().log(Level.CONFIG, () -> "Logging process to file " + logFilename);
			} catch (Exception e) {
				getLogger().warning("Cannot create log file '" + logFilename + "': " + e);
			}
		}
		return logHandler;
	}

	/** The last thrown exception during context loading */
	private Exception lastInitException;

	/**
	 * Sets the last thrown exception that occurred during context data loading. This is necessary,
	 * since the {@link ProgressThread#startAndWait()} method does not throw exceptions from the run
	 * method of the {@link ProgressThread}.
	 *
	 * @param e
	 *            the exception that was thrown
	 */
	private void setLastInitException(Exception e) {
		this.lastInitException = e;
	}

	/**
	 * Finishes the process and cleans up everything, including GUI.
	 *
	 * @since 7.4
	 */
	private void finishProcess(Handler logHandler) {
		stop();
		tearDown();
		if (logHandler != null) {
			getLogger().removeHandler(logHandler);
			logHandler.close();
		}
		ActionStatisticsCollector.getInstance().logExecutionFinished(this);
	}

	/** This method is invoked after a process has finished. */
	private void tearDown() {
		try {
			rootOperator.processFinished();
		} catch (OperatorException e) {
			getLogger().log(Level.WARNING, "Problem during finishing the process: " + e.getMessage(), e);
		}

		// clean up
		// clearMacros();
		clearReportStreams();
		clearStorage();
		clearUnknownParameters();
		ResultService.close();
	}

	// ----------------------
	// Process IO
	// ----------------------

	public static Charset getEncoding(String encoding) {
		if (encoding == null) {
			encoding = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEFAULT_ENCODING);
			if (encoding == null || encoding.trim().length() == 0) {
				encoding = RapidMiner.SYSTEM_ENCODING_NAME;
			}
		}

		Charset result = null;
		if (RapidMiner.SYSTEM_ENCODING_NAME.equals(encoding)) {
			result = Charset.defaultCharset();
		} else {
			try {
				result = Charset.forName(encoding);
			} catch (IllegalArgumentException e) {
				result = Charset.defaultCharset();
			}
		}
		return result;
	}

	/** Saves the process to the process file. */
	public void save() throws IOException {
		try {
			Process.checkIfSavable(this);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
		if (processLocation != null) {
			this.isProcessConverted = false;
			processLocation.store(this, null);
		} else {
			throw new IOException("No process location is specified.");
		}
	}

	/** Saves the process to the given process file. */
	public void save(final File file) throws IOException {
		try {
			Process.checkIfSavable(this);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
		new FileProcessLocation(file).store(this, null);
	}

	/**
	 * Resolves the given filename against the directory containing the process file.
	 */
	public File resolveFileName(final String name) {
		File absolute = new File(name);
		if (absolute.isAbsolute()) {
			return absolute;
		}
		if (processLocation instanceof FileProcessLocation) {
			File processFile = ((FileProcessLocation) processLocation).getFile();
			return Tools.getFile(processFile.getParentFile(), name);
		} else {
			String homeName = System.getProperty("user.home");
			if (homeName != null) {
				File file = new File(new File(homeName), name);
				getLogger().warning("Process not attached to a file. Resolving against user directory: '" + file + "'.");
				return file;
			} else {
				getLogger().warning("Process not attached to a file. Trying absolute filename '" + name + "'.");
				return new File(name);
			}
		}

	}

	/** Reads the process setup from the given input stream. */
	public void readProcess(final Reader in) throws XMLException, IOException {
		readProcess(in, null);
	}

	public void readProcess(final Reader in, final ProgressListener progressListener) throws XMLException, IOException {
		Map<String, Operator> nameMapBackup = operatorNameMap;
		operatorNameMap = new HashMap<>(); // no invocation of clear (see below)

		if (progressListener != null) {
			progressListener.setTotal(120);
			progressListener.setCompleted(0);
		}
		try {
			Document document = XMLTools.createDocumentBuilder().parse(new InputSource(in));
			if (progressListener != null) {
				progressListener.setCompleted(20);
			}
			unknownParameterInformation.clear();
			XMLImporter xmlImporter = new XMLImporter(progressListener);
			xmlImporter.parse(document, this, unknownParameterInformation);

			nameMapBackup = operatorNameMap;
			rootOperator.clear(Port.CLEAR_ALL);
		} catch (XMLParserException e) {
			throw new XMLException(e.toString(), e);
		} catch (SAXException e) {
			throw new XMLException("Cannot parse document: " + e.getMessage(), e);
		} finally {
			operatorNameMap = nameMapBackup; // if everything went fine -->
			// map = new map, if not -->
			// map = old map (backup)
			if (progressListener != null) {
				progressListener.complete();
			}
		}
	}

	/**
	 * Returns a &quot;name (i)&quot; if name is already in use. This new name should then be used
	 * as operator name.
	 */
	public String registerName(final String name, final Operator operator) {
		String newName = ProcessTools.getNewName(operatorNameMap.keySet(), name);
		operatorNameMap.put(newName, operator);
		return newName;
	}

	/** This method is used for unregistering a name from the operator name map. */
	public void unregisterName(final String name) {
		operatorNameMap.remove(name);
	}

	public void notifyRenaming(final String oldName, final String newName) {
		rootOperator.notifyRenaming(oldName, newName);
	}

	/**
	 * This method is called when the operator given by {@code oldName} (and {@code oldOp} if it is not {@code null})
	 * was replaced with the operator described by {@code newName} and {@code newOp}.
	 * This will inform the {@link ProcessRootOperator} of the replacing.
	 *
	 * @param oldName
	 * 		the name of the old operator
	 * @param oldOp
	 * 		the old operator; can be {@code null}
	 * @param newName
	 * 		the name of the new operator
	 * @param newOp
	 * 		the new operator; must not be {@code null}
	 * @see Operator#notifyReplacing(String, Operator, String, Operator)
	 * @since 9.3
	 */
	public void notifyReplacing(String oldName, Operator oldOp, String newName, Operator newOp) {
		rootOperator.notifyReplacing(oldName, oldOp, newName, newOp);
	}

	@Override
	public String toString() {
		if (rootOperator == null) {
			return "empty process";
		} else {
			return "Process:" + Tools.getLineSeparator() + rootOperator.getXML(true);
		}
	}

	private final EventListenerList processSetupListeners = new EventListenerList();

	/** Delegates any changes in the ProcessContext to the root operator. */
	private final Observer<ProcessContext> delegatingContextObserver = (observable, arg) -> fireUpdate();
	private final Observer<Operator> delegatingOperatorObserver = (observable, arg) -> fireUpdate();

	public void addProcessSetupListener(final ProcessSetupListener listener) {
		processSetupListeners.add(ProcessSetupListener.class, listener);
	}

	public void removeProcessSetupListener(final ProcessSetupListener listener) {
		processSetupListeners.remove(ProcessSetupListener.class, listener);
	}

	public void fireOperatorAdded(final Operator operator) {
		for (ProcessSetupListener l : processSetupListeners.getListeners(ProcessSetupListener.class)) {
			l.operatorAdded(operator);
		}
	}

	public void fireOperatorChanged(final Operator operator) {
		for (ProcessSetupListener l : processSetupListeners.getListeners(ProcessSetupListener.class)) {
			l.operatorChanged(operator);
		}
	}

	public void fireOperatorRemoved(final Operator operator, final int oldIndex, final int oldIndexAmongEnabled) {
		for (ProcessSetupListener l : processSetupListeners.getListeners(ProcessSetupListener.class)) {
			l.operatorRemoved(operator, oldIndex, oldIndexAmongEnabled);
		}
	}

	public void fireExecutionOrderChanged(final ExecutionUnit unit) {
		for (ProcessSetupListener l : processSetupListeners.getListeners(ProcessSetupListener.class)) {
			l.executionOrderChanged(unit);
		}
	}

	public ExecutionMode getExecutionMode() {
		return executionMode;
	}

	public void setExecutionMode(final ExecutionMode mode) {
		this.executionMode = mode;
	}

	public DebugMode getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(final DebugMode mode) {
		this.debugMode = mode;
		if (mode == DebugMode.DEBUG_OFF) {
			getRootOperator().clear(Port.CLEAR_REAL_METADATA);
		}
	}

	/** Resolves a repository location relative to {@link #getRepositoryLocation()}. */
	public RepositoryLocation resolveRepositoryLocation(final String loc)
			throws UserError, MalformedRepositoryLocationException {
		if (RepositoryLocation.isAbsolute(loc)) {
			RepositoryLocation repositoryLocation = new RepositoryLocation(loc);
			repositoryLocation.setAccessor(getRepositoryAccessor());
			return repositoryLocation;
		}
		RepositoryLocation repositoryLocation = getRepositoryLocation();
		if (repositoryLocation != null) {
			RepositoryLocation repositoryLocation2 = new RepositoryLocation(repositoryLocation.parent(), loc);
			repositoryLocation2.setAccessor(getRepositoryAccessor());
			return repositoryLocation2;
		} else {
			throw new UserError(null, 317, loc);
		}
	}

	/** Turns loc into a repository location relative to {@link #getRepositoryLocation()}. */
	public String makeRelativeRepositoryLocation(final RepositoryLocation loc) {
		RepositoryLocation repositoryLocation = getRepositoryLocation();
		if (repositoryLocation != null) {
			return loc.makeRelative(repositoryLocation.parent());
		} else {
			return loc.getAbsoluteLocation();
		}
	}

	public void setContext(final ProcessContext context) {
		if (this.context != null) {
			this.context.removeObserver(delegatingContextObserver);
		}
		this.context = context;
		this.context.addObserver(delegatingContextObserver, false);
		fireUpdate();
	}

	public ProcessContext getContext() {
		return context;
	}

	public void setImportMessage(final String importMessage) {
		this.importMessage = importMessage;
	}

	/**
	 * This returns true if the process has been imported and ImportRules have been applied during
	 * importing. Since the backward compatibility is lost on save, one can warn by retrieving this
	 * value.
	 */
	public boolean isProcessConverted() {
		return isProcessConverted;
	}

	/**
	 * This sets whether the process is converted.
	 */
	public void setProcessConverted(final boolean isProcessConverted) {
		this.isProcessConverted = isProcessConverted;
	}

	/**
	 * Returns some user readable messages generated during import by {@link XMLImporter}.
	 */
	public String getImportMessage() {
		return importMessage;
	}

	// process location (file/repository)

	/**
	 * Returns if the process has a valid save location.
	 *
	 * @return {@code true} iff either a file location is defined or a repository location is defined AND the repository
	 * is not read-only; {@code false} otherwise
	 */
	public boolean hasSaveDestination() {
		if (processLocation == null) {
			return false;
		}
		if (processLocation instanceof RepositoryProcessLocation) {
			RepositoryProcessLocation repoProcLoc = (RepositoryProcessLocation) processLocation;
			try {
				return !repoProcLoc.getRepositoryLocation().getRepository().isReadOnly();
			} catch (RepositoryException e) {
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * Returns the current process file.
	 *
	 * @deprecated Use {@link #getProcessFile()} instead
	 */
	@Deprecated
	public File getExperimentFile() {
		return getProcessFile();
	}

	/**
	 * Returns the current process file.
	 *
	 * @deprecated Use {@link #getProcessLocation()}
	 */
	@Deprecated
	public File getProcessFile() {
		if (processLocation instanceof FileProcessLocation) {
			return ((FileProcessLocation) processLocation).getFile();
		} else {
			return null;
		}
	}

	/**
	 * Sets the process file. This file might be used for resolving relative filenames.
	 *
	 * @deprecated Please use {@link #setProcessFile(File)} instead.
	 */
	@Deprecated
	public void setExperimentFile(final File file) {
		setProcessLocation(new FileProcessLocation(file));
	}

	/** Sets the process file. This file might be used for resolving relative filenames. */
	public void setProcessFile(final File file) {
		setProcessLocation(new FileProcessLocation(file));
	}

	public void setProcessLocation(final ProcessLocation processLocation) {
		// keep process file version if same file, otherwise overwrite
		if (this.processLocation != null && !this.processLocation.equals(processLocation)) {
			this.isProcessConverted = false;
			getLogger().info("Decoupling process from location " + this.processLocation
					+ ". Process is now associated with file " + processLocation + ".");
		}
		this.processLocation = processLocation;
		fireUpdate();
	}

	public ProcessLocation getProcessLocation() {
		return this.processLocation;
	}

	public RepositoryLocation getRepositoryLocation() {
		if (processLocation instanceof RepositoryProcessLocation) {
			return ((RepositoryProcessLocation) processLocation).getRepositoryLocation();
		} else {
			return null;
		}
	}

	/**
	 * @return if <code>false</code> is returned, the {@link IOContainer} returned by {@link #run()}
	 *         will contain <code>null</code> results instead of just omitting them.
	 */
	public boolean isOmittingNullResults() {
		return omitNullResults;
	}

	/**
	 * If set to <code>false</code> the {@link IOContainer} returned by {@link #run()} will contain
	 * <code>null</code> results instead of omitting them. By default this is <code>true</code>.
	 */
	public void setOmitNullResults(boolean omitNullResults) {
		this.omitNullResults = omitNullResults;
	}

	/**
	 * Can be called by GUI components if visual representation or any other state not known to the
	 * process itself has changed.
	 */
	public void updateNotify() {
		fireUpdate(this);
	}

	public RepositoryAccessor getRepositoryAccessor() {
		return repositoryAccessor;
	}

	public void setRepositoryAccessor(final RepositoryAccessor repositoryAccessor) {
		this.repositoryAccessor = repositoryAccessor;
	}

	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Indicates how deeply nested the current process is. The original process itself has a depth
	 * of {@code 0}. If that process spawns a new one via an Execute Process operator, the depth of
	 * the new one will be {@code 1}. If the new process also contains an Execute Process operator,
	 * the depth will be {@code 2} and so on.
	 *
	 * @return the nesting depth of the current process
	 */
	public int getDepth() {
		return nestingDepth;
	}

	/**
	 * Sets the nesting depth of this process. See {@link #getDepth()} for details.
	 *
	 * @param depth
	 *            the new nesting depth
	 */
	public void setDepth(int depth) {
		this.nestingDepth = depth;
	}

	/**
	 * Checks weather the process can be saved as is. Throws an Exception if the Process should not
	 * be saved.
	 **/
	public static void checkIfSavable(final Process process) throws Exception {
		for (Operator operator : process.getAllOperators()) {
			if (operator instanceof DummyOperator) {
				throw new Exception(
						"The process contains dummy operators. Remove all dummy operators or install all missing extensions in order to save the process.");
			}
		}
	}

	/**
	 * Checks if breakpoints are present in this process.
	 *
	 * @return {@code true}  if a breakpoint is present. {@code false}  otherwise
	 * @author Joao Pedro Pinheiro
	 * @since 8.2.0
	 */
	public boolean hasBreakpoints() {
		for (Operator op: getAllOperators()) {
			if (op.hasBreakpoint()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Removes all breakpoints from the current process
	 *
	 * @author Joao Pedro Pinheiro
	 * @since 8.2.0
	 */
	public void removeAllBreakpoints() {
		for (Operator op: getAllOperators()) {
			op.setBreakpoint(BreakpointListener.BREAKPOINT_BEFORE, false);
			op.setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, false);
		}
	}

}
