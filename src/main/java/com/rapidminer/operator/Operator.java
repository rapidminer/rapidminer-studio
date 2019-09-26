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
package com.rapidminer.operator;

import static com.rapidminer.repository.RepositoryLocation.REPOSITORY_PREFIX;
import static com.rapidminer.repository.RepositoryLocation.SEPARATOR;
import static com.rapidminer.repository.RepositoryLocation.getRepositoryLocation;
import static com.rapidminer.repository.RepositoryLocation.isConnectionPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.BreakpointListener;
import com.rapidminer.MacroHandler;
import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.belt.execution.ExecutionAbortedException;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.wizards.ConfigurationListener;
import com.rapidminer.gui.wizards.PreviewListener;
import com.rapidminer.io.process.XMLExporter;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.annotation.ResourceConsumer;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.ports.DeliveringPortManager;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.impl.InputPortsImpl;
import com.rapidminer.operator.ports.impl.OutputPortsImpl;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MDTransformer;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.ports.quickfix.RelativizeRepositoryLocationQuickfix;
import com.rapidminer.operator.preprocessing.filter.AbstractDateDataProcessing;
import com.rapidminer.parameter.CombinedParameterType;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDate;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeInnerOperator;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedMacroError;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.DelegatingObserver;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.WrapperLoggingHandler;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.io.Encoding;
import com.rapidminer.tools.math.StringToMatrixConverter;
import com.rapidminer.tools.patterns.Visitor;


/**
 * <p>
 * An operator accepts an array of input objects and generates an array of output objects that can
 * be processed by other operators. Both must implement the IOObject interface. This is the
 * superclass which must be extended by all RapidMiner operators. Please refer to the RapidMiner
 * tutorial for a detailed description how to implement your operator.
 * </p>
 *
 * <p>
 * As default, operators consume their input by using it. This is often a useful behavior especially
 * in complex processes. For example, a learning operator consumes an example set to produce a model
 * and so does a cross validation to produce a performance value of the learning method. To receive
 * the input {@link IOObject} of a certain class simply use {@link #getInput(Class class)}. This
 * method delivers the first object of the desired class which is in the input of this operator. The
 * delivered object is consumed afterwards and thus is removed from input. If the operator alters
 * this object, it should return the altered object as output again. Therefore, you have to add the
 * object to the output array which is delivered by the {@link #apply()} method of the operator. You
 * also have to declare it in {@link #getOutputClasses()}. All input objects which are not used by
 * your operator will be automatically passed to the next operators.
 * </p>
 *
 * <p>
 * In some cases it would be useful if the user can define if the input object should be consumed or
 * not. For example, a validation chain like cross validation should estimate the performance but
 * should also be able to return the example set which is then used to learn the overall model.
 * Operators can change the default behavior for input consumption and a parameter will be
 * automatically defined and queried. The default behavior is defined in the method
 * {@link #getInputDescription(Class cls)} and should be overridden in these cases. Please note that
 * input objects with a changed input description must not be defined in {@link #getOutputClasses()}
 * and must not be returned at the end of apply. Both is automatically done with respect to the
 * value of the automatically created parameter. Please refer to the Javadoc comments of this method
 * for further explanations.
 * </p>
 *
 * @see com.rapidminer.operator.OperatorChain
 *
 * @author Ralf Klinkenberg, Ingo Mierswa, Simon Fischer, Marius Helf
 */
public abstract class Operator extends AbstractObservable<Operator>
		implements ConfigurationListener, PreviewListener, LoggingHandler, ParameterHandler, ResourceConsumer {

	private static final boolean CPU_TIME_SUPPORTED = ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported();

	public static final OperatorVersion[] EMPTY_OPERATOR_VERSIONS_ARRAY = new OperatorVersion[0];

	private static final OperatorVersion THROW_ERROR_ON_UNDEFINED_MACRO = new OperatorVersion(6, 0, 3);

	/**
	 * indicates the start of a macro
	 */
	public static final String MACRO_STRING_START = "%{";
	/**
	 * indicates the end of a macro
	 */
	public static final String MACRO_STRING_END = "}";
	/**
	 * indicates the the start of a string expansion parameter
	 */
	public static final String STRING_EXPANSION_MACRO_PARAMETER_START = "[";
	/**
	 * indicates the the end of a string expansion parameter
	 */
	public static final String STRING_EXPANSION_MACRO_PARAMETER_END = "]";
	/**
	 * string expansion key which will be replaced with the current system date and time
	 */
	public static final String STRING_EXPANSION_MACRO_TIME = "t";
	/**
	 * string expansion key which will be replaced with the specified value of the operator with the
	 * specified name.
	 */
	public static final String STRING_EXPANSION_MACRO_OPERATORVALUE = "v";
	/**
	 * string expansion key which will be replaced with the name of the operator.
	 */
	public static final String STRING_EXPANSION_MACRO_OPERATORNAME = "n";
	/**
	 * string expansion key which will be replaced with the name of the operator.
	 */
	public static final String STRING_EXPANSION_MACRO_OPERATORNAME_USER_FRIENDLY = "operator_name";
	/**
	 * string expansion key which will be replaced with the class of the operator.
	 */
	public static final String STRING_EXPANSION_MACRO_OPERATORCLASS = "c";
	/**
	 * string expansion key which will be replaced with the number of times the operator was
	 * applied.
	 */
	public static final String STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES = "a";
	/**
	 * string expansion key which will be replaced with the number of times the operator was
	 * applied.
	 */
	public static final String STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY = "execution_count";
	/**
	 * string expansion key which will be replaced with the number of times the operator was applied
	 * plus one (a shortcut for %{p[1]}).
	 */
	public static final String STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_PLUS_ONE = "b";
	/**
	 * string expansion key which will be replaced with the number of times the operator was applied
	 * plus the specified number.
	 */
	public static final String STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_SHIFTED = "p";
	/**
	 * string expansion key which will be replaced with %.
	 */
	public static final String STRING_EXPANSION_MACRO_PERCENT_SIGN = "%";

	/** Indicates if before / within / after this operator a breakpoint is set. */
	private boolean breakPoint[] = new boolean[BreakpointListener.BREAKPOINT_POS_NAME.length];

	/** Indicates if this operator is enabled. */
	private boolean enabled = true;

	/** Indicates if the tree node is expanded (only operator chains). */
	private boolean expanded = true;

	/** Name of the operators (for logging). */
	private String name;

	/** Number of times the operator was applied. */
	private AtomicInteger applyCount = new AtomicInteger();

	/** May differ from {@link #applyCount} if parallellized. */
	private int applyCountAtLastExecution = -1;

	/** System time when execution started. */
	private long startTime;

	/** Cpu time when execution started. */
	private long startCpuTime;

	/** System time when execution finished. */
	private long endTime;

	/** Cpu time when execution finished. */
	private long endCpuTime;

	/** System time when the current loop of execution started. */
	private long loopStartTime;

	/** Parameters for this Operator. */
	private Parameters parameters = null;

	/**
	 * The values for this operator. The current value of a Value can be asked by the
	 * ProcessLogOperator.
	 */
	private final Map<String, Value> valueMap = new TreeMap<>();

	/**
	 * Container for user data.
	 */
	private Map<String, UserData<Object>> userData;

	/**
	 * Lock object for user data container.
	 */
	private final Object userDataLock = new Object();

	/**
	 * The list which stores the errors of this operator (parameter not set, wrong children number,
	 * wrong IO).
	 */
	private List<ProcessSetupError> errorList = Collections.synchronizedList(new LinkedList<ProcessSetupError>());

	/**
	 * The operator description of this operator (icon, classname, description, ...).
	 */
	private OperatorDescription operatorDescription = null;

	/** Signals whether the output must be re-generated. */
	private boolean dirty = true;

	/**
	 * Indicates whether {@link #propagateDirtyness()} was called after the last call to
	 * {@link #makeDirty()}.
	 */
	private boolean dirtynessWasPropagated = false;

	private transient final Logger logger = Logger.getLogger(Operator.class.getName());

	private transient final LoggingHandler logService = new WrapperLoggingHandler(logger);

	private boolean isRunning = false;

	private boolean shouldStopStandaloneExecution = false;

	private OperatorVersion compatibilityLevel;

	/**
	 * The {@link OperatorProgress} used to track progress during the execution of the operator.
	 */
	private final OperatorProgress operatorProgress = new OperatorProgress(this);

	// -------------------- INITIALISATION --------------------

	/**
	 * <p>
	 * Creates an unnamed operator. Subclasses must pass the given description object to this
	 * super-constructor (i.e. invoking super(OperatorDescription)). They might also add additional
	 * values for process logging.
	 * </p>
	 * <p>
	 * NOTE: the preferred way for operator creation is using one of the factory methods of
	 * {@link com.rapidminer.tools.OperatorService}.
	 * </p>
	 */
	public Operator(OperatorDescription description) {
		this.operatorDescription = description;
		this.parameters = null;
		this.name = operatorDescription.getOperatorDocumentation().getShortName();

		inputPorts = createInputPorts(portOwner);
		outputPorts = createOutputPorts(portOwner);
		inputPorts.addObserver(delegatingPortObserver, false);
		outputPorts.addObserver(delegatingPortObserver, false);
		makeDirtyOnUpdate(inputPorts);

		addValue(new ValueDouble("applycount", "The number of times the operator was applied.", false) {

			@Override
			public double getDoubleValue() {
				return applyCountAtLastExecution;
			}
		});
		addValue(new ValueDouble("time", "The time elapsed since this operator started.", false) {

			@Override
			public double getDoubleValue() {
				return System.currentTimeMillis() - startTime;
			}
		});
		addValue(new ValueDouble("cpu-time", "The cpu time elapsed since this operator started.", false) {

			@Override
			public double getDoubleValue() {
				return getThreadCpuTime() - startCpuTime;
			}
		});
		addValue(new ValueDouble("execution-time", "The execution time of this operator.", false) {

			@Override
			public double getDoubleValue() {
				return endTime - startTime;
			}
		});
		addValue(new ValueDouble("cpu-execution-time", "The cpu execution time of this operator.", false) {

			@Override
			public double getDoubleValue() {
				return endCpuTime - startCpuTime;
			}
		});
		addValue(new ValueDouble("looptime", "The time elapsed since the current loop started.", false) {

			@Override
			public double getDoubleValue() {
				return System.currentTimeMillis() - loopStartTime;
			}
		});
	}

	/**
	 * Observes the given {@link Observable} and sets this operators dirty flag to true upon any
	 * update.
	 */
	@SuppressWarnings("unchecked")
	protected void makeDirtyOnUpdate(Observable<? extends Object> observable) {
		observable.addObserver(dirtyObserver, false);
	}

	/** Returns the operator description of this operator. */
	public final OperatorDescription getOperatorDescription() {
		return operatorDescription;
	}

	/**
	 * Returns the &quot;class name&quot; of this operator from the operator description of the
	 * operator. This is the name which is defined in the operator.xml file.
	 */
	public final String getOperatorClassName() {
		return operatorDescription.getName();
	}

	/**
	 * Returns the experiment (process) of this operator by asking the parent operator. If the
	 * operator itself and all of its parents are not part of an process, this method will return
	 * null. Please note that some operators (e.g. ProcessLog) must be part of an process in order
	 * to work properly.
	 *
	 * @deprecated Please use {@link #getProcess()} instead
	 */
	@Deprecated
	public Process getExperiment() {
		return getProcess();
	}

	/**
	 * Returns the process of this operator by asking the parent operator. If the operator itself
	 * and all of its parents are not part of an process, this method will return null. Please note
	 * that some operators (e.g. ProcessLog) must be part of an process in order to work properly.
	 */
	@Override
	public Process getProcess() {
		Operator parent = getParent();
		if (parent == null) {
			return null;
		} else {
			return parent.getProcess();
		}
	}

	/**
	 * Returns the logging of the process if this operator is part of an process and the global
	 * logging service otherwise.
	 */
	public LoggingHandler getLog() {
		return logService;
	}

	public Logger getLogger() {
		if (getProcess() == null) {
			return logger;
		} else {
			return getProcess().getLogger();
		}
	}

	@Override
	public void log(String message, int level) {
		getLog().log(message, level);
	}

	@Override
	public void log(String message) {
		getLogger().fine(getName() + ": " + message);
	}

	@Override
	public void logNote(String message) {
		getLog().log(getName() + ": " + message, LogService.NOTE);
	}

	@Override
	public void logWarning(String message) {
		getLog().log(getName() + ": " + message, LogService.WARNING);
	}

	@Override
	public void logError(String message) {
		getLog().log(getName() + ": " + message, LogService.ERROR);
	}

	// --------------------------------------------------------------------------------

	/** Returns the name of the operator. */
	public final String getName() {
		return this.name;
	}

	/**
	 * This method simply sets the name to the given one. Please note that it is not checked if the
	 * name was already used in the process. Please use the method {@link #rename(String)} for usual
	 * renaming.
	 */
	private final void setName(String newName) {
		this.name = newName;
	}

	/**
	 * This method unregisters the old name if this operator is already part of a {@link Process}.
	 * Afterwards, the new name is set and registered in the process. Please note that the name
	 * might be changed during registering in order to ensure that each operator name is unique in
	 * its process. The new name will be returned.
	 */
	public final String rename(String newName) {
		Process process = getProcess();
		if (process != null) {
			process.unregisterName(this.name);
			String oldName = this.name;
			this.name = process.registerName(newName, this);
			process.notifyRenaming(oldName, this.name);
		} else {
			this.name = newName;
		}
		fireUpdate(this);
		return this.name;
	}

	/**
	 * Sets the user specified comment for this operator.
	 *
	 * @deprecated use
	 *             {@link com.rapidminer.io.process.GUIProcessXMLFilter#addOperatorAnnotation(com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation)}
	 *             instead! Calling this method will do nothing anymore.
	 */
	@Deprecated
	public void setUserDescription(String description) {
		getLogger().log(Level.WARNING,
				"Setting comments directly on operators has been deprecated. See JavaDoc for details.");
	}

	/**
	 * Looks up {@link UserData} entries. Returns null if key is unknown.
	 *
	 * @param key
	 * 		The key of the user data.
	 * @return The user data.
	 */
	public UserData<Object> getUserData(String key) {
		synchronized (this.userDataLock) {
			if (this.userData == null) {
				return null;
			} else {
				return this.userData.get(key);
			}
		}
	}

	/**
	 * Stores arbitrary {@link UserData}.
	 *
	 * @param key
	 *            The key to used to identify the data.
	 * @param data
	 *            The user data.
	 */
	public void setUserData(String key, UserData<Object> data) {
		synchronized (this.userDataLock) {
			if (this.userData == null) {
				this.userData = new TreeMap<>();
			}
			this.userData.put(key, data);
		}
	}

	/**
	 * The user specified comment for this operator.
	 *
	 * @deprecated use
	 *             {@link com.rapidminer.io.process.GUIProcessXMLFilter#lookupOperatorAnnotations(Operator)}
	 *             instead! This method will always return {@code null}.
	 */
	@Deprecated
	public String getUserDescription() {
		getLogger().log(Level.WARNING,
				"Getting comments directly from operators has been deprecated. See JavaDoc for details.");
		return null;
	}

	/**
	 * Returns null if this operator is not deprecated. This implementation returns the return value
	 * of OperatorDescription.getDeprecationInfo() which is usually null. If a non-null value is
	 * returned this should describe a a workaround for a user. In this case the workaround is
	 * displayed during the validation of the process.
	 *
	 * @deprecated Use getOperatorDescription().getDeprecationInfo()
	 */
	@Deprecated
	public final String getDeprecationInfo() {
		return this.operatorDescription.getDeprecationInfo();
	}

	public void removeAndKeepConnections(List<Operator> keepConnectionsTo) {

		getInputPorts().disconnectAllBut(keepConnectionsTo);
		getOutputPorts().disconnectAllBut(keepConnectionsTo);

		Process process = getProcess();
		if (enclosingExecutionUnit != null) {
			enclosingExecutionUnit.removeOperator(this);
		}
		if (process != null) {
			unregisterOperator(process);
		}
	}

	/** Removes this operator from its parent. */
	public void remove() {
		removeAndKeepConnections(null);
	}

	/**
	 * This methods was used in older RapidMiner version for registering the operator in the process
	 * and to ensure that all operator names are unique. This is now automatically done during
	 * operator adding and therefore this method is now deprecated.
	 *
	 * @deprecated No longer necessary since the registering / unregistering will be performed
	 *             during operator adding
	 */
	@Deprecated
	public void register(Process process, String name) {}

	/**
	 * Registers this operator in the given process. Please note that this might change the name of
	 * the operator.
	 */
	protected void registerOperator(Process process) {
		if (process != null) {
			setName(process.registerName(getName(), this));
		}
	}

	/** Deletes this operator removing it from the name map of the process. */
	protected void unregisterOperator(Process process) {
		process.unregisterName(name);
	}

	/** Sets the activation mode. Inactive operators do not perform their action. */
	public void setEnabled(boolean enabled) {
		if (this.enabled != enabled) {
			this.enabled = enabled;
			fireUpdate(this);
		}
	}

	/** Sets the expansion mode which indicates if this operator is drawn expanded or not. */
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	/** Returns true if this operator should be painted expanded. */
	public boolean isExpanded() {
		return expanded;
	}

	/**
	 * Returns true if this operator is enabled. No longer takes parent enabled status into account.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * This method must return true if the operator performs parallel execution of child operators
	 * and false otherwise.
	 */
	public boolean isParallel() {
		return false;
	}

	/** Returns the number of times this operator was already applied. */
	public int getApplyCount() {
		return applyCountAtLastExecution;
	}

	// --------------------------------------------------------------------------------

	/**
	 * Performs a deep clone on the most parts of this operator. The breakpointThread is empty (as
	 * it is in initialization). The parent will be clone in the method of OperatorChain overwriting
	 * this one. The in- and output containers are only cloned by reference copying. Use this method
	 * only if you are sure what you are doing.
	 *
	 * @param name
	 *            This parameter is not longer used.
	 */
	public Operator cloneOperator(String name, boolean forParallelExecution) {
		Operator clone;
		try {
			clone = operatorDescription.createOperatorInstance();
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Can not create clone of operator '" + getName() + "': " + e, e);
			throw new RuntimeException("Can not create clone of operator '" + getName(), e);
		}
		clone.setName(getName());
		clone.breakPoint = new boolean[]{breakPoint[0], breakPoint[1]};
		clone.enabled = enabled;
		clone.expanded = expanded;

		// copy user data entries
		if (this.userData != null) {
			this.userData.forEach((k, data) -> {if (data != null) clone.setUserData(k, data.copyUserData(clone));});
		}

		if (forParallelExecution) {
			clone.applyCount = this.applyCount;
		} else {
			clone.applyCount = new AtomicInteger();
		}
		clone.startTime = startTime;
		clone.startCpuTime = startCpuTime;
		clone.endTime = endTime;
		clone.endCpuTime = endCpuTime;
		clone.loopStartTime = loopStartTime;
		clone.getParameters().copyFrom(this.getParameters());
		clone.compatibilityLevel = compatibilityLevel;
		clone.errorList.addAll(errorList);
		return clone;
	}

	// --------------------- Apply ---------------------

	/**
	 * Implement this method in subclasses.
	 *
	 * @deprecated use doWork()
	 */
	@Deprecated
	public IOObject[] apply() throws OperatorException {
		throw new UnsupportedOperationException("apply() is deprecated. Implement doWork().");
	}

	/**
	 * Performs the actual work of the operator and must be implemented by subclasses. Replaces the
	 * old method <code>apply()</code>.
	 */
	public void doWork() throws OperatorException {}

	// -------------------- Nesting --------------------

	/**
	 * Returns the classes that are needed as input. May be null or an empty (no desired input). As
	 * default, all delivered input objects are consumed and must be also delivered as output in
	 * both {@link #getOutputClasses()} and {@link #apply()} if this is necessary. This default
	 * behavior can be changed by overriding {@link #getInputDescription(Class)}. Subclasses which
	 * implement this method should not make use of parameters since this method is invoked by
	 * getParameterTypes(). Therefore, parameters are not fully available at this point of time and
	 * this might lead to exceptions. Please use InputDescriptions instead.
	 *
	 * @deprecated create input ports instead
	 */
	@Deprecated
	public Class<?>[] getInputClasses() {
		return new Class[0];
	}

	/**
	 * <p>
	 * Returns the classes that are guaranteed to be returned by <tt>apply()</tt> as additional
	 * output. Please note that input objects which should not be consumed must also be defined by
	 * this method (e.g. an example set which is changed but not consumed in the case of a
	 * preprocessing operator must be defined in both, the methods {@link #getInputClasses()} and
	 * {@link #getOutputClasses()}). The default behavior for input consumation is defined by
	 * {@link #getInputDescription(Class)} and can be changed by overwriting this method. Objects
	 * which are not consumed (defined by changing the implementation in
	 * {@link #getInputDescription(Class)}) must not be defined as additional output in this method.
	 * </p>
	 *
	 * <p>
	 * May deliver null or an empy array (no additional output is produced or guaranteed). Must
	 * return the class array of delivered output objects otherwise.
	 * </p>
	 *
	 * @deprecated create output ports
	 */
	@Deprecated
	public Class<?>[] getOutputClasses() {
		return new Class[0];
	}

	/**
	 * Returns the classes that are needed as input. Returns the result of
	 * {@link #getInputClasses()}.
	 */
	protected final Class<?>[] getDesiredInputClasses() {
		Class<?>[] inputClasses = getInputClasses();
		if (inputClasses == null) {
			return new Class[0];
		} else {
			return inputClasses;
		}
	}

	/**
	 * Returns the classes that are guaranteed to be returned by <tt>apply()</tt>. These are all
	 * input classes which are not consumed and all guaranteed additional output classes.
	 */
	protected final Class<?>[] getDeliveredOutputClasses() {
		List<Class<?>> result = new LinkedList<>();
		Class<?>[] inputClasses = getDesiredInputClasses();
		for (Class<?> inputClasse : inputClasses) {
			InputDescription description = getInputDescription(inputClasse);
			if (description.showParameter() && getParameterAsBoolean(description.getParameterName())
					|| description.getKeepDefault()) {
				result.add(inputClasse);
			}
		}
		Class<?>[] additionalOutput = getOutputClasses();
		if (additionalOutput != null) {
			for (Class<?> element : additionalOutput) {
				result.add(element);
			}
		}
		Class<?>[] resultArray = new Class[result.size()];
		result.toArray(resultArray);
		return resultArray;
	}

	/**
	 * The default implementation returns an input description that consumes the input IOObject
	 * without a user parameter. Subclasses may override this method to allow other input handling
	 * behaviors.
	 *
	 * @deprecated
	 */
	@Deprecated
	protected InputDescription getInputDescription(Class<?> inputClass) {
		return new InputDescription(inputClass);
	}

	/**
	 * If you find the <tt>getInputClasses()</tt> and <tt>getOuputClasses()</tt> methods for some
	 * reason not useful, you may override this method. Otherwise it returns a default IODescription
	 * containing the classes returned by the first.
	 *
	 * @deprecated As of version 5.0, this method is no longer necessary.
	 */
	@Deprecated
	protected IODescription getIODescription() {
		return new DefaultIODescription(getDesiredInputClasses(), getDeliveredOutputClasses());
	}

	/**
	 * Subclasses will throw an exception if something isn't ok. Returns the output that this
	 * operator returns when provided with the given input.
	 *
	 * @deprecated As of version 5.0, this method is no longer necessary.
	 */
	@Deprecated
	public Class<?>[] checkIO(Class<?>[] input) throws IllegalInputException, WrongNumberOfInnerOperatorsException {
		if (isEnabled()) {
			return getIODescription().getOutputClasses(input, this);
		} else {
			return input;
		}
	}

	/**
	 * This method is invoked during the validation checks. It is invoked as a last check. The
	 * default implementation does nothing. Subclasses might want to override this method to perform
	 * some specialized checks, e.g. if an inner operator is of a specific class.
	 */
	protected void performAdditionalChecks() {}

	/**
	 * Will count an error if a non optional property has no default value and is not defined by
	 * user. Returns the total number of errors.
	 */
	public int checkProperties() {
		if (!isEnabled()) {
			return 0;
		}
		int errorCount = 0;
		for (ParameterType type : getParameters().getParameterTypes()) {
			boolean optional = type.isOptional();
			boolean hidden = type.isHidden();
			String key = type.getKey();
			if (!optional && !hidden) {
				boolean parameterSet = getParameters().isSet(key);
				boolean isUndefined = false;
				try {
					isUndefined = parameterSet
							&& (type instanceof ParameterTypeAttribute || type instanceof CombinedParameterType)
							&& "".equals(getParameter(key));
				} catch (UndefinedParameterError e) {
					// ignore
				}
				if (!parameterSet || isUndefined) {
					addError(new UndefinedParameterSetupError(this, key));
					errorCount++;
				}
			}
			if (type instanceof ParameterTypeRepositoryLocation) {
				String value = getParameters().getParameterOrNull(key);
				if (value == null || value.isEmpty() || ((ParameterTypeRepositoryLocation) type).isAllowAbsoluteEntries()
						|| value.charAt(0) != SEPARATOR && !value.startsWith(REPOSITORY_PREFIX)
						|| isConnectionPath(value) && !value.startsWith(REPOSITORY_PREFIX))  {
					continue;
				}
				String errorKey = null;
				Severity severity = null;
				List<? extends QuickFix> quickfix = Collections.singletonList(
						new RelativizeRepositoryLocationQuickfix(this, key, value));
				if (!value.startsWith(REPOSITORY_PREFIX)) {
					errorKey = "absolute_repository_location";
					severity = Severity.ERROR;
					if (getProcess().getRepositoryLocation() == null) {
						quickfix = Collections.emptyList();
					}
					errorCount++;
				} else if (!value.startsWith(REPOSITORY_PREFIX + RepositoryManager.SAMPLE_REPOSITORY_NAME)) {
					errorKey = "accessing_repository_by_name";
					severity = Severity.WARNING;
					RepositoryLocation processLocation = getProcess().getRepositoryLocation();
					if (processLocation == null || !value.startsWith(REPOSITORY_PREFIX + processLocation.getRepositoryName())) {
						quickfix = Collections.emptyList();
					}
				}
				if (errorKey != null) {
					addError(new SimpleProcessSetupError(severity, getPortOwner(), quickfix, errorKey,
							key.replace('_', ' '), value));
				}
			} else if (type instanceof ParameterTypeDateFormat) {
				Locale locale = Locale.getDefault();
				try {
					int localeIndex;
					localeIndex = getParameterAsInt(AbstractDataResultSetReader.PARAMETER_LOCALE);
					if (localeIndex >= 0 && localeIndex < AbstractDateDataProcessing.availableLocales.size()) {
						locale = AbstractDateDataProcessing.availableLocales.get(localeIndex);
					}
				} catch (UndefinedParameterError e) {
					// ignore and use default locale
				}
				try {
					ParameterTypeDateFormat.createCheckedDateFormat(this, locale, true);
				} catch (UserError userError) {
					// will not happen because of isSetup
				}

			} else if (!optional && !hidden && type instanceof ParameterTypeDate) {
				String value = getParameters().getParameterOrNull(key);
				if (value != null && !ParameterTypeDate.isValidDate(value)) {
					addError(new SimpleProcessSetupError(Severity.WARNING, portOwner, "invalid_date_format",
							key.replace('_', ' '), value));
				}
			}
		}
		return errorCount;
	}

	/**
	 * Will count the number of deprecated operators, i.e. the operators which
	 * {@link #getDeprecationInfo()} method does not return null. Returns the total number of
	 * deprecations.
	 */
	public int checkDeprecations() {
		String deprecationString = getOperatorDescription().getDeprecationInfo();
		int deprecationCount = 0;
		if (deprecationString != null) {
			addError(new SimpleProcessSetupError(Severity.WARNING, portOwner, "deprecation",
					new Object[]{getOperatorDescription().getName(), deprecationString}));
			deprecationCount = 1;
		}
		return deprecationCount;
	}

	/**
	 * @deprecated use {@link #execute()}
	 */
	@Deprecated
	public final IOContainer apply(IOContainer input) throws OperatorException {
		throw new UnsupportedOperationException("apply(IOContainer) is deprecated. Use execute()!");
	}

	/**
	 * Applies the operator. Don't override this method, but {@link #doWork()}
	 */
	@SuppressWarnings("unchecked")
	public final void execute() throws OperatorException {
		Process process = getProcess();
		if (process == null) {
			getLogger().fine("Process of operator " + this.getName()
					+ " is not set, probably not registered! Trying to use this operator (should work in most cases anyway)...");
		}

		if (process != null && process.getExecutionMode() == ExecutionMode.ONLY_DIRTY && !isDirty()) {
			return;
		}

		if (getOperatorDescription().getDeprecationInfo() != null && applyCount.get() == 0) {
			getLogger().warning("Deprecation warning for " + getOperatorDescription().getName() + ": "
					+ getOperatorDescription().getDeprecationInfo());
		}

		getOutputPorts().clear(Port.CLEAR_DATA);

		if (isEnabled()) {
			// check for stop
			checkForStop(process);

			applyCountAtLastExecution = applyCount.incrementAndGet();
			startTime = loopStartTime = System.currentTimeMillis();
			startCpuTime = getThreadCpuTime();
			if (process != null) {
				process.setCurrentOperator(this);
				process.getRootOperator().processStartedOperator(this);
			}

			if (breakPoint[BreakpointListener.BREAKPOINT_BEFORE]) {
				processBreakpoint(getInputPorts().createIOContainer(true), BreakpointListener.BREAKPOINT_BEFORE);
			}

			for (InputPort inputPort : getInputPorts().getAllPorts()) {
				IOObject ioObject = inputPort.getDataOrNull(IOObject.class);
				if (ioObject != null) {
					ioObject.setLoggingHandler(getLog());
				}
			}

			getLogger().fine("Starting application " + applyCount + " of operator " + getName());
			// logging?
			if (getLogger().isLoggable(WrapperLoggingHandler.LEVELS[LogService.IO])) {
				StringBuilder builder = new StringBuilder();
				builder.append(getName());
				builder.append(" called ");
				builder.append(Tools.ordinalNumber(applyCount.get()));
				builder.append(" time with input:");
				formatIO(getInputPorts(), builder);
				getLogger().log(WrapperLoggingHandler.LEVELS[LogService.IO], builder.toString());
			}
			// reset progress listener to default value
			getProgress().setTotal(OperatorProgress.NO_PROGRESS);

			getOutputPorts().clear(Port.CLEAR_DATA);
			try {
				isRunning = true;
				fireUpdate();
				doWork();
				getLogger().fine("Completed application " + applyCount.get() + " of operator " + getName());
			} catch (ProcessStoppedRuntimeException | ExecutionAbortedException e) {
				// Convert unchecked exception to checked exception (unchecked exception might be
				// thrown from places where no checked exceptions are possible, e.g. thread pools).
				throw new ProcessStoppedException(this);
			} catch (RuntimeException e) {
				if (!(e instanceof OperatorRuntimeException)) {
					throw e;
				}
				OperatorException operatorException = ((OperatorRuntimeException) e).toOperatorException();
				if (!(operatorException instanceof UserError)) {
					throw operatorException;
				}
				UserError userError = (UserError) operatorException;
				if (userError.getOperator() == null) {
					userError.setOperator(this);
				}
				throw userError;
			} catch (UserError e) {
				// TODO: ensuring that operator is removed if it abnormally terminates but is not
				// removed if child operator terminates abnormally
				if (e.getOperator() == null) {
					e.setOperator(this);
				}
				throw e;
			} finally {
				isRunning = false;
				endTime = System.currentTimeMillis();
				endCpuTime = getThreadCpuTime();
				// set source to the output
				for (OutputPort outputPort : getOutputPorts().getAllPorts()) {
					IOObject ioObject = outputPort.getDataOrNull(IOObject.class);
					if (ioObject != null && ioObject.getSource() == null) {
						ioObject.setSource(getName());
						if (ioObject instanceof IOObjectCollection) {
							for (IOObject ioo : ((IOObjectCollection<IOObject>) ioObject).getObjects()) {
								DeliveringPortManager.setLastDeliveringPort(ioo, outputPort);
								if (ioo.getSource() == null) {
									ioo.setSource(getName());
								}
							}
						}
						ioObject.setLoggingHandler(null);
					}
				}
			}

			// logging?
			if (getLogger().isLoggable(WrapperLoggingHandler.LEVELS[LogService.IO])) {
				StringBuilder builder = new StringBuilder(getName());
				builder.append(" returned with output:");
				formatIO(getOutputPorts(), builder);
				getLogger().log(WrapperLoggingHandler.LEVELS[LogService.IO], builder.toString());
			}
			getLogger().finest(getName() + ": execution time was " + (System.currentTimeMillis() - startTime) + " ms");

			//
			if (process != null) {
				process.getRootOperator().processFinishedOperator(this);
			}

			setNotDirty();
			if (breakPoint[BreakpointListener.BREAKPOINT_AFTER]) {
				processBreakpoint(getOutputPorts().createIOContainer(true), BreakpointListener.BREAKPOINT_AFTER);
			}
		} else {
			// TODO: Apply pass through rules if operator is disabled
		}
	}

	private void formatIO(Ports<? extends Port> ports, StringBuilder builder) {
		for (Port port : ports.getAllPorts()) {
			builder.append("\n  ");
			builder.append(port.getName());
			IOObject data = port.getRawData();
			builder.append(data == null ? "-/-" : data.toString());
		}
	}

	/**
	 * This method should be called within long running loops of an operator to check if the user
	 * has canceled the execution in the mean while. This then will throw a
	 * {@link ProcessStoppedException} to cancel the execution.
	 */
	public final void checkForStop() throws ProcessStoppedException {
		if (getParent() != null) {
			checkForStop(getParent().getProcess());
		} else {
			checkForStop(getProcess());
		}
	}

	private final void checkForStop(Process process) throws ProcessStoppedException {
		if (process != null && process.shouldStop()) {
			stop();
			return;
		}
		if (process != null && process.shouldPause()) {
			getLogger().info("Process interrupted in " + getName());
			processBreakpoint(null, BreakpointListener.BREAKPOINT_AFTER);
		}
		if (process == null && shouldStopStandaloneExecution) {
			stop();
			return;
		}
	}

	/**
	 * This method will cause the execution of this operator to stop at the next call of
	 * checkForStop() in the executing thread. When this will be depends on the operator. Some
	 * operations like huge matrix inversions cannot be aborted prematurely at all. A
	 * ProcessStoppedException will be thrown in case of stopping, so prepare to catch it when
	 * executing the operator. Please keep in mind, that this method will have an effect only if the
	 * operator is executed without a process context directly from the API.
	 */
	public final void shouldStopStandaloneExecution() throws ProcessStoppedException {
		if (getProcess() == null) {
			this.shouldStopStandaloneExecution = true;
		}
	}

	private final void stop() throws ProcessStoppedException {
		getLogger().info(getName() + ": Process stopped.");
		throw new ProcessStoppedException(this);
	}

	/**
	 * This method should only be called by the command line breakpoint listener to resume the
	 * process after a breakpoint.
	 *
	 * @deprecated Use {@link Process#resume()}
	 */
	@Deprecated
	public final void resume() {
		getProcess().resume();
	}

	private void processBreakpoint(IOContainer container, int breakpointType) throws ProcessStoppedException {
		getLogger().info(getName() + ": Breakpoint reached.");
		Process process = getProcess();
		if (process.shouldStop()) {
			stop();
		}
		process.pause(this, container, breakpointType);
	}

	/**
	 * Indicates how additional output should be added to the IOContainer. Usually the additional
	 * output should be preprended to the input container but some operators, especially operator
	 * chains might override this method in order add only the additional output instead of the
	 * complete IOContainer. This prevents doubling the IOObjects e.g. for SimpleOperatorChains. The
	 * default implementation returns false.
	 *
	 * @deprecated
	 */
	@Deprecated
	public boolean getAddOnlyAdditionalOutput() {
		return false;
	}

	/**
	 * Returns an IOObject of class cls. The object is removed from the input IOContainer if the
	 * input description defines this behavior (default).
	 *
	 * @deprecated Use input ports
	 */
	@Deprecated
	protected <T extends IOObject> T getInput(Class<T> cls) throws MissingIOObjectException {
		return getInput(cls, 0);
	}

	/**
	 * Returns the nr-th IOObject of class cls. The object is removed from the input IOContainer if
	 * the input description defines this behavior (default).
	 *
	 * @deprecated use the input ports directly
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	protected <T extends IOObject> T getInput(Class<T> cls, int nr) throws MissingIOObjectException {
		int successCount = 0;
		for (InputPort inputPort : getInputPorts().getAllPorts()) {
			IOObject input = inputPort.getAnyDataOrNull();
			if (input != null && cls.isAssignableFrom(input.getClass())) {
				if (successCount == nr) {
					return (T) input;
				}
				successCount++;
			}
		}
		throw new MissingIOObjectException(cls);
	}

	/**
	 * Returns true if this operator has an input object of the desired class. The object will not
	 * be removed by using this method.
	 *
	 * @deprecated use the input ports directly
	 */
	@Deprecated
	protected boolean hasInput(Class<? extends IOObject> cls) {
		try {
			getInput(cls);
			return true;
		} catch (MissingIOObjectException e) {
			return false;
		}
	}

	/**
	 * Returns the complete input. Operators should usually not directly use this method but should
	 * use {@link #getInput(Class)}. However, some operator chains must handle their inner input and
	 * have to use the IOContainer directly.
	 *
	 * @deprecated Use {@link #getInputPorts()}
	 */
	@Deprecated
	protected final IOContainer getInput() {
		throw new UnsupportedOperationException("getInput() is deprecated. Use the input ports.");
	}

	/**
	 * ATTENTION: Use this method only if you are ABSOLUTELY sure what you are doing! This method
	 * might be useful for some meta optimization operators but wrong usage can cause serious
	 * errors.
	 *
	 * @deprecated use the ports
	 */
	@Deprecated
	protected void setInput(IOContainer input) {
		throw new UnsupportedOperationException("setInput() is deprecated. Use the input ports.");
	}

	/** Called when the process starts. Resets all counters. */
	public void processStarts() throws OperatorException {
		applyCount.set(0);
		applyCountAtLastExecution = 0;
	}

	/**
	 * Called at the end of the process. The default implementation does nothing.
	 */
	public void processFinished() throws OperatorException {}

	/**
	 * Sets or clears a breakpoint at the given position.
	 *
	 * @param position
	 *            One out of BREAKPOINT_BEFORE and BREAKPOINT_AFTER
	 */
	public void setBreakpoint(int position, boolean on) {
		breakPoint[position] = on;
		fireUpdate(this);
	}

	/**
	 * Returns true iff this operator has a breakpoint at any possible position.
	 */
	public boolean hasBreakpoint() {
		return hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE) || hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER);
	}

	/**
	 * Returns true iff a breakpoint is set at the given position
	 *
	 * @param position
	 *            One out of BREAKPOINT_BEFORE and BREAKPOINT_AFTER
	 */
	public boolean hasBreakpoint(int position) {
		return breakPoint[position];
	}

	/**
	 * Should be called if this operator performs a loop (for the loop time resetting used for Value
	 * creation used by DataTables). This method also invokes {@link #checkForStop()}.
	 */
	public void inApplyLoop() throws ProcessStoppedException {
		loopStartTime = System.currentTimeMillis();
		checkForStop();
	}

	/** Adds an implementation of Value. */
	public void addValue(Value value) {
		valueMap.put(value.getKey(), value);
	}

	/** Returns the value of the Value with the given key. */
	public final Value getValue(String key) {
		return valueMap.get(key);
	}

	/** Returns all Values sorted by key. */
	public Collection<Value> getValues() {
		return valueMap.values();
	}

	// -------------------- parameter wrapper --------------------

	/**
	 * Returns a collection of all parameters of this operator. If the parameters object has not
	 * been created yet, it will now be created. Creation had to be moved out of constructor for
	 * meta data handling in subclasses needing a port.
	 */
	@Override
	public Parameters getParameters() {
		if (parameters == null) {
			// if not loaded already: do now
			parameters = new Parameters(getParameterTypes());
			parameters.addObserver(delegatingParameterObserver, false);

			makeDirtyOnUpdate(parameters);
		}
		return parameters;
	}

	/**
	 * Returns the the first primary {@link ParameterType} of this operator that is not hidden
	 * if it exists. May return {@code null}.
	 *
	 * @return the primary parameter or {@code null}
	 * @since 8.2
	 */
	public ParameterType getPrimaryParameter() {
		for (ParameterType param : getParameters().getParameterTypes()) {
			if (param.isPrimary() && !param.isHidden()) {
				return param;
			}
		}
		return null;
	}

	@Override
	public ParameterHandler getParameterHandler() {
		return this;
	}

	/**
	 * Sets all parameters of this operator. The given parameters are not allowed to be null and
	 * must correspond to the parameter types defined by this operator.
	 */
	@Override
	public void setParameters(Parameters parameters) {
		if (this.parameters != parameters) {
			if (this.parameters != null) {
				this.parameters.removeObserver(delegatingParameterObserver);
				this.parameters.removeObserver(dirtyObserver);
			}
			if (parameters != null) {
				parameters.addObserver(delegatingParameterObserver, false);
				makeDirtyOnUpdate(parameters);
			}
		}
		this.parameters = parameters;
	}

	/**
	 * Sets the given single parameter to the Parameters object of this operator. For parameter list
	 * the method {@link #setListParameter(String, List)} should be used.
	 */
	@Override
	public void setParameter(String key, String value) {
		getParameters().setParameter(key, value);
	}

	/**
	 * Sets the given parameter list to the Parameters object of this operator. For single
	 * parameters the method {@link #setParameter(String, String)} should be used.
	 */
	@Override
	public void setListParameter(String key, List<String[]> list) {
		getParameters().setParameter(key, ParameterTypeList.transformList2String(list));
	}

	public void setPairParameter(String key, String firstValue, String secondValue) {
		getParameters().setParameter(key, ParameterTypeTupel.transformTupel2String(firstValue, secondValue));
	}

	/**
	 * Returns a single parameter retrieved from the {@link Parameters} of this Operator.
	 */
	@Override
	public String getParameter(String key) throws UndefinedParameterError {
		try {
			return replaceMacros(getParameters().getParameter(key), key);
		} catch (UndefinedParameterError e) {
			e.setOperator(this);
			throw e;
		}
	}

	/**
	 * Returns true iff the parameter with the given name is set. If no parameters object has been
	 * created yet, false is returned. This can be used to break initialization loops.
	 */
	@Override
	public boolean isParameterSet(String key) {
		return getParameters().isSet(key);
	}

	/** Returns a single named parameter and casts it to String. */
	@Override
	public String getParameterAsString(String key) throws UndefinedParameterError {
		return getParameter(key);
	}

	/** Returns a single named parameter and casts it to char. */
	@Override
	public char getParameterAsChar(String key) throws UndefinedParameterError {
		String parameterValue = getParameter(key);
		if (parameterValue.length() > 0) {
			return parameterValue.charAt(0);
		}
		return 0;
	}

	/** Returns a single named parameter and casts it to int. */
	@Override
	public int getParameterAsInt(String key) throws UndefinedParameterError {
		return getParameterAsIntNumber(key, Integer::valueOf, Integer::intValue);
	}

	/** Returns a single named parameter and casts it to long. */
	@Override
	public long getParameterAsLong(String key) throws UndefinedParameterError {
		return getParameterAsIntNumber(key, Long::valueOf, Long::valueOf);
	}

	private <N extends Number> N getParameterAsIntNumber(String key,
														 Function<String, N> transformer,
														 Function<Integer, N> caster) throws UndefinedParameterError{
		ParameterType type = this.getParameters().getParameterType(key);
		String value = getParameter(key);
		if (type instanceof ParameterTypeCategory) {
			try {
				return transformer.apply(value);
			} catch (NumberFormatException e) {
				ParameterTypeCategory categoryType = (ParameterTypeCategory) type;
				return caster.apply(categoryType.getIndex(value));
			}
		}
		try {
			return transformer.apply(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, this, "Expected long but found '" + value + "'.");
		}
	}

	/** Returns a single named parameter and casts it to double. */
	@Override
	public double getParameterAsDouble(String key) throws UndefinedParameterError {
		String value = getParameter(key);
		if (value == null) {
			throw new UndefinedParameterError(key, this);
		}
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			throw new UndefinedParameterError(key, this, "Expected real number but found '" + value + "'.");
		}
	}

	/**
	 * Returns a single named parameter and casts it to boolean. This method never throws an
	 * exception since there are no non-optional boolean parameters.
	 */
	@Override
	public boolean getParameterAsBoolean(String key) {
		try {
			return Boolean.valueOf(getParameter(key));
		} catch (UndefinedParameterError e) {
		}
		return false; // cannot happen
	}

	/**
	 * Returns a single named parameter and casts it to List. The list returned by this method
	 * contains the user defined key-value pairs. Each element is a String array of length 2. The
	 * first element is the key, the second the parameter value. The caller have to perform the
	 * casts to the correct types himself.
	 */
	@Override
	public List<String[]> getParameterList(String key) throws UndefinedParameterError {
		return ParameterTypeList.transformString2List(getParameter(key));
	}

	/**
	 * Returns a Pair of Strings, the Strings are in the order of type definition of the subtypes.
	 */
	@Override
	public String[] getParameterTupel(String key) throws UndefinedParameterError {
		return ParameterTypeTupel.transformString2Tupel(getParameter(key));
	}

	/** Returns a single named parameter and casts it to Color. */
	@Override
	public java.awt.Color getParameterAsColor(String key) throws UndefinedParameterError {
		return com.rapidminer.parameter.ParameterTypeColor.string2Color(getParameter(key));
	}

	/**
	 * Returns a single named parameter and tries to handle it as URL. If this works, this method
	 * creates an input stream from this URL and delivers it. If not, this method tries to cast the
	 * parameter value to a file. This file is already resolved against the process definition file.
	 * If the parameter name defines a non-optional parameter which is not set and has no default
	 * value, a UndefinedParameterError will be thrown. If the parameter is optional and was not set
	 * this method returns null. Operators should always use this method instead of directly using
	 * the method {@link Process#resolveFileName(String)}.
	 *
	 * @throws IOException
	 * @throws UserError
	 */
	@Override
	public InputStream getParameterAsInputStream(String key) throws IOException, UserError {
		String urlString = getParameter(key);
		if (urlString == null) {
			return null;
		}

		try {
			URL url = new URL(urlString);
			InputStream stream = WebServiceTools.openStreamFromURL(url);
			return stream;
		} catch (MalformedURLException e) {
			// URL did not work? Try as file...
			File file = getParameterAsFile(key);
			if (file != null) {
				return new FileInputStream(file);
			} else {
				return null;
			}
		}
	}

	/**
	 * Returns a single named parameter and casts it to File. This file is already resolved against
	 * the process definition file but missing directories will not be created. If the parameter
	 * name defines a non-optional parameter which is not set and has no default value, a
	 * UndefinedParameterError will be thrown. If the parameter is optional and was not set this
	 * method returns null. Operators should always use this method instead of directly using the
	 * method {@link Process#resolveFileName(String)}.
	 */
	@Override
	public java.io.File getParameterAsFile(String key) throws UserError {
		return getParameterAsFile(key, false);
	}

	/**
	 * Returns a single named parameter and casts it to File. This file is already resolved against
	 * the process definition file and missing directories will be created. If the parameter name
	 * defines a non-optional parameter which is not set and has no default value, a
	 * UndefinedParameterError will be thrown. If the parameter is optional and was not set this
	 * method returns null. Operators should always use this method instead of directly using the
	 * method {@link Process#resolveFileName(String)}.
	 *
	 * @throws UserError
	 */
	@Override
	public File getParameterAsFile(String key, boolean createMissingDirectories) throws UserError {
		String fileName = getParameter(key);
		if (fileName == null) {
			return null;
		}

		Process process = getProcess();
		File result;
		if (process != null) {
			result = process.resolveFileName(fileName);
		} else {
			getLogger().fine(getName() + " is not attached to a process. Trying '" + fileName + "' as absolute filename.");
			result = new File(fileName);
		}

		if (createMissingDirectories) {
			File parent = result.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				throw new UserError(null, "io.dir_creation_fail", parent.getAbsolutePath());
			}
		}
		return result;
	}

	/**
	 * This method returns the parameter identified by key as a RepositoryLocation. For this the
	 * string is resolved against this operators process location in the Repository.
	 */
	public RepositoryLocation getParameterAsRepositoryLocation(String key) throws UserError {
		String loc = getParameter(key);
		return getRepositoryLocation(loc, this);
	}

	/** Returns a single named parameter and casts it to a double matrix. */
	@Override
	public double[][] getParameterAsMatrix(String key) throws UndefinedParameterError {
		String matrixLine = getParameter(key);
		try {
			return StringToMatrixConverter.parseMatlabString(matrixLine);
		} catch (OperatorException e) {
			throw new UndefinedParameterError(e.getMessage(), this);
		}
	}

	/**
	 * Replaces existing macros in the given value string by the macro values defined for the
	 * process. Please note that this is basically only supported for string type parameter values.
	 *
	 * This method replaces the predefined macros like %{process_name}, %{process_file}, and
	 * %{process_path} and tries to replace macros surrounded by &quot;%{&quot; and &quot;}&quot;
	 * with help of the {@link MacroHandler} of the {@link Process}. These macros might have been
	 * defined with help of a {@link MacroDefinitionOperator}.
	 *
	 * @param parameterKey
	 *            the key of the parameter type the macro is looked up for. If the referenced
	 *            parameterType is {@code null} the value-string will be directly passed to the
	 *            MacrosHandler
	 *
	 * @throws UndefinedParameterError
	 *             will be thrown if value contains a not defined macro
	 */
	private String replaceMacros(String value, String parameterKey) throws UndefinedParameterError {
		if (value == null || getProcess() == null) {
			return value;
		}
		String returnValue = value;
		try {
			ParameterType parameterType = getParameters().getParameterType(parameterKey);
			if (parameterType == null) {
				returnValue = getProcess().getMacroHandler().resolveMacros(parameterKey, value);
				returnValue = getProcess().getMacroHandler().resolvePredefinedMacros(returnValue, this);
			} else {
				returnValue = parameterType.substituteMacros(value, getProcess().getMacroHandler());
				returnValue = parameterType.substitutePredefinedMacros(returnValue, this);
			}
		} catch (UndefinedMacroError e) {
			if (getProcess().getRootOperator().getCompatibilityLevel().isAtLeast(THROW_ERROR_ON_UNDEFINED_MACRO)) {
				// throw the error so that the user recognizes that the macro was undefined
				UndefinedMacroError macroError = e;
				macroError.setOperator(this);
				throw macroError;
			}
		} catch (RuntimeException e) {
			LogService.getRoot().log(Level.SEVERE, "Error resolving Macro values", e);
			throw e;
		}
		return returnValue;
	}

	/**
	 * Returns a list of <tt>ParameterTypes</tt> describing the parameters of this operator. The
	 * default implementation returns an empty list if no input objects can be retained and special
	 * parameters for those input objects which can be prevented from being consumed.
	 *
	 * ATTENTION! This will create new parameterTypes. For calling already existing parameter types
	 * use getParameters().getParameterTypes();
	 */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		Class<?>[] inputClasses = getDesiredInputClasses();
		for (Class<?> inputClasse : inputClasses) {
			InputDescription description = getInputDescription(inputClasse);
			if (description.showParameter()) {
				types.add(new ParameterTypeBoolean(description.getParameterName(),
						"Indicates if this input object should also be returned as output.", description.getKeepDefault()));
			}
		}
		return types;
	}

	/**
	 * Returns the parameter type with the given name. Will return null if this operator does not
	 * have a parameter with the given name.
	 */
	public ParameterType getParameterType(String name) {
		Iterator<ParameterType> i = getParameters().getParameterTypes().iterator();
		while (i.hasNext()) {
			ParameterType current = i.next();
			if (current.getKey().equals(name)) {
				return current;
			}
		}
		return null;
	}

	/**
	 * Writes the XML representation of this operator.
	 *
	 * @deprecated indent is not considered any more. Use {@link #writeXML(Writer, boolean)}
	 */
	@Deprecated
	public void writeXML(Writer out, String indent, boolean hideDefault) throws IOException {
		writeXML(out, hideDefault);
	}

	/**
	 * This will report this operator with all its parameter settings to the given writer as XML.
	 */
	public void writeXML(Writer out, boolean hideDefault) throws IOException {
		try {
			XMLTools.stream(new XMLExporter().exportProcess(this, hideDefault), new StreamResult(out),
					XMLImporter.PROCESS_FILE_CHARSET);
		} catch (XMLException e) {
			throw new IOException("Cannot create process XML: " + e, e);
		}
	}

	/**
	 * This returns this operator with all its parameter settings as a {@link Document}
	 */
	public Document getDOMRepresentation() throws IOException {
		return new XMLExporter().exportProcess(this, false);
	}

	/**
	 * @deprecated indent is not used any more. Use {@link #getXML(boolean)}.
	 */
	@Deprecated
	public String getXML(String indent, boolean hideDefault) {
		return getXML(hideDefault);
	}

	/** Same as getXML(hideDefault, false). */
	public String getXML(boolean hideDefault) {
		return getXML(hideDefault, false);
	}

	/**
	 * Returns the XML representation of this operator.
	 *
	 * @param hideDefault
	 *            if true, default parameters will be ignored when creating the xml representation
	 * @param onlyCoreElements
	 *            if true, GUI and other additional information will be ignored.
	 */
	public String getXML(boolean hideDefault, boolean onlyCoreElements) {
		try {
			return XMLTools.toString(new XMLExporter(onlyCoreElements).exportProcess(this, hideDefault));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.operator.Operator.generating_xml_process_error", e), e);
			return e.toString();
		}
	}

	public static Operator createFromXML(Element element, Process targetProcess,
										 List<UnknownParameterInformation> unknownParameterInformation) throws XMLException {
		return createFromXML(element, targetProcess, unknownParameterInformation, null);
	}

	/**
	 * This will create an operator by interpreting the given XML element as being generated from
	 * the current RapidMiner version. No Import Rules will be applied to adapt it to version
	 * changes.
	 */
	public static Operator createFromXML(Element element, Process process,
										 List<UnknownParameterInformation> unknownParameterInformation, ProgressListener l) throws XMLException {
		XMLImporter importer = new XMLImporter(l);
		return importer.parseOperator(element, XMLImporter.CURRENT_VERSION, process, unknownParameterInformation);
	}

	/**
	 * This will create an operator from a XML element describing this operator. The given version
	 * will be passed to the XMLImporter to enable the handling of this element as if it would have
	 * been created from this version. See {@link XMLImporter#VERSION_RM_5} for details.
	 */
	public static Operator createFromXML(Element element, Process process,
										 List<UnknownParameterInformation> unknownParameterInformation, ProgressListener progressListener,
										 VersionNumber originatingVersion) throws XMLException {
		XMLImporter importer = new XMLImporter(progressListener);
		return importer.parseOperator(element, originatingVersion, process, unknownParameterInformation);
	}

	/**
	 * Clears the list of errors.
	 *
	 * @see #addError(String)
	 */
	public void clearErrorList() {
		clear(Port.CLEAR_META_DATA_ERRORS);
	}

	private final void checkOperator() {
		if (!isEnabled()) {
			return;
		}
		checkProperties();
		checkDeprecations();
		performAdditionalChecks();
	}

	/**
	 * Clears all errors, checks the operator and its children and propagates meta data, propgatates
	 * dirtyness and sorts execution order.
	 */
	public void checkAll() {
		getRoot().clear(Port.CLEAR_METADATA | Port.CLEAR_ALL_ERRORS);
		if (isEnabled()) {
			checkOperator();
			getRoot().transformMetaData();
			propagateDirtyness();
		}
		updateExecutionOrder();
	}

	/** As check all, but does not check the meta data for performance reasons. */
	public void checkAllExcludingMetaData() {
		getRoot().clear(Port.CLEAR_METADATA | Port.CLEAR_SIMPLE_ERRORS);
		if (isEnabled()) {
			checkOperator();
			propagateDirtyness();
		}
		updateExecutionOrder();
	}

	public void updateExecutionOrder() {}

	public void addError(ProcessSetupError error) {
		errorList.add(error);
	}

	/**
	 * Adds an error message.
	 *
	 * @deprecated Use {@link #addError(ProcessSetupError)}
	 */
	@Deprecated
	public void addError(final String message) {
		errorList.add(new ProcessSetupError() {

			@Override
			public String getMessage() {
				return message;
			}

			@Override
			public PortOwner getOwner() {
				return portOwner;
			}

			@Override
			public List<QuickFix> getQuickFixes() {
				return Collections.emptyList();
			}

			@Override
			public Severity getSeverity() {
				return Severity.ERROR;
			}
		});
	}

	/**
	 * Adds a warning message to the error list.
	 *
	 * @deprecated Use {@link #addError(ProcessSetupError)} *
	 */
	@Deprecated
	public void addWarning(final String message) {
		errorList.add(new ProcessSetupError() {

			@Override
			public String getMessage() {
				return message;
			}

			@Override
			public PortOwner getOwner() {
				return portOwner;
			}

			@Override
			public List<QuickFix> getQuickFixes() {
				return Collections.emptyList();
			}

			@Override
			public Severity getSeverity() {
				return Severity.WARNING;
			}
		});
	}

	/**
	 * Returns a List of Strings containing error messages.
	 *
	 * @see #addError(String)
	 */
	public List<ProcessSetupError> getErrorList() {
		List<ProcessSetupError> errors = new LinkedList<>();
		collectErrors(errors);
		return errors;
	}

	protected void collectErrors(List<ProcessSetupError> errors) {
		errors.addAll(errorList);
		for (Port port : getInputPorts().getAllPorts()) {
			Collection<MetaDataError> portErrors = port.getErrors();
			if (portErrors != null) {
				try {
					errors.addAll(portErrors);
				} catch (NullPointerException e) {
					// TODO: Can it be avoided to have NullPointerExceptions here when an error is
					// created
					// just after the operator has been inserted?
				}
			}
		}
		for (Port port : getOutputPorts().getAllPorts()) {
			Collection<MetaDataError> portErrors = port.getErrors();
			if (portErrors != null) {
				errors.addAll(port.getErrors());
			}
		}
	}

	/** Returns the system time when the operator was started. */
	public long getStartTime() {
		return startTime;
	}

	/** Returns the name. */
	@Override
	public String toString() {
		String type = null;
		if (getOperatorDescription() != null) {
			type = getOperatorClassName();
		} else {
			type = getClass().getName();
		}
		return (breakPoint[0] || breakPoint[1] ? "* " : "") + name + " (" + type + ")";
	}

	/**
	 * Returns this operator's name and class.
	 *
	 * @deprecated Use {@link #createProcessTree(int)} instead
	 */
	@Deprecated
	public String createExperimentTree(int indent) {
		return createProcessTree(indent);
	}

	/** Returns this operator's name and class. */
	public String createProcessTree(int indent) {
		return createProcessTree(indent, "", "", null, null);
	}

	/**
	 * Returns this operator's name and class.
	 *
	 * @deprecated Use {@link #createMarkedProcessTree(int, String, Operator)} instead
	 */
	@Deprecated
	public String createMarkedExperimentTree(int indent, String mark, Operator markOperator) {
		return createMarkedProcessTree(indent, mark, markOperator);
	}

	/** Returns this operator's name and class. */
	public String createMarkedProcessTree(int indent, String mark, Operator markOperator) {
		return createProcessTree(indent, "", "", markOperator, mark);
	}

	/**
	 * Returns this operator's name and class.
	 *
	 * @deprecated Use {@link #createProcessTree(int, String, String, Operator, String)} instead
	 */
	@Deprecated
	protected String createExperimentTree(int indent, String selfPrefix, String childPrefix, Operator markOperator,
										  String mark) {
		return createProcessTree(indent, selfPrefix, childPrefix, markOperator, mark);
	}

	/** Returns this operator's name and class. */
	protected String createProcessTree(int indent, String selfPrefix, String childPrefix, Operator markOperator,
									   String mark) {
		return createProcessTreeEntry(indent, selfPrefix, childPrefix, markOperator, mark);
	}

	/** Returns this operator's name and class in a list. */
	protected List<String> createProcessTreeList(int indent, String selfPrefix, String childPrefix, Operator markOperator,
												 String mark) {
		List<String> processTreeList = new LinkedList<>();
		processTreeList.add(createProcessTreeEntry(indent, selfPrefix, childPrefix, markOperator, mark));
		return processTreeList;
	}

	private String createProcessTreeEntry(int indent, String selfPrefix, String childPrefix, Operator markOperator,
										  String mark) {
		if (markOperator != null && getName().equals(markOperator.getName())) {
			return Tools.indent(indent - mark.length()) + mark + selfPrefix + getName() + "[" + applyCount + "]" + " ("
					+ getOperatorClassName() + ")";
		} else {
			return Tools.indent(indent) + selfPrefix + getName() + "[" + applyCount + "]" + " (" + getOperatorClassName()
					+ ")";
		}
	}

	/**
	 * Returns the encoding if defined by the root operator if this operator is part of a process or
	 * the standard encoding defined via the system property. If both is not possible or if the
	 * defined encoding name is 'SYSTEM', the default encoding of the underlying operating system is
	 * returned.
	 *
	 * @deprecated This method is rubbish. Use the {@link Encoding} to add a custom encoding
	 *             parameter to this operator.
	 */
	@Deprecated
	public final Charset getEncoding() {
		Process process = getProcess();
		if (process != null) {
			if (process.getRootOperator().isParameterSet(ProcessRootOperator.PARAMETER_ENCODING)) {
				try {
					return Process.getEncoding(
							process.getRootOperator().getParameterAsString(ProcessRootOperator.PARAMETER_ENCODING));
				} catch (UndefinedParameterError e) {
					// cannot happen
					return Process.getEncoding(null);
				}
			} else {
				return Process.getEncoding(null);
			}
		} else {
			return Process.getEncoding(null);
		}
	}

	public boolean isDebugMode() {
		String debugProperty = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE);
		return Tools.booleanValue(debugProperty, false);
	}

	// / SIMONS NEUERUNGEN
	private final PortOwner portOwner = new PortOwner() {

		@Override
		public OperatorChain getPortHandler() {
			return getParent();
		}

		@Override
		public String getName() {
			return Operator.this.getName();
		}

		@Override
		public Operator getOperator() {
			return Operator.this;
		}

		@Override
		public ExecutionUnit getConnectionContext() {
			return getExecutionUnit();
		}
	};

	private final InputPorts inputPorts;
	private final OutputPorts outputPorts;
	private final MDTransformer transformer = new MDTransformer(this);
	private final Observer<Port> delegatingPortObserver = new DelegatingObserver<>(this, this);
	private final Observer<String> delegatingParameterObserver = new DelegatingObserver<>(this, this);
	/** Sets the dirty flag on any update. */
	@SuppressWarnings("rawtypes")
	private final Observer dirtyObserver = (observable, arg) -> makeDirty();
	private ExecutionUnit enclosingExecutionUnit;

	/**
	 * Returns the operator containing the enclosing process or null if this is the root operator.
	 */
	public final OperatorChain getParent() {
		if (enclosingExecutionUnit != null) {
			return enclosingExecutionUnit.getEnclosingOperator();
		} else {
			return null;
		}
	}

	/**
	 * This method returns the {@link InputPorts} object that gives access to all defined
	 * {@link InputPort}s of this operator. This object can be used to create a new
	 * {@link InputPort} for an operator using one of the {@link InputPorts#createPort(String)}
	 * methods.
	 */
	public final InputPorts getInputPorts() {
		return inputPorts;
	}

	/**
	 * This method returns the {@link OutputPorts} object that gives access to all defined
	 * {@link OutputPort}s of this operator. This object can be used to create a new
	 * {@link OutputPort} for an operator using one of the {@link OutputPorts#createPort(String)}
	 * methods.
	 */
	public final OutputPorts getOutputPorts() {
		return outputPorts;
	}

	/**
	 * This method returns an {@link InputPorts} object for port initialization. Useful for adding
	 * an arbitrary implementation (e.g. changing port creation & (dis)connection behavior,
	 * optionally by customized {@link InputPort} instances) by overriding this method.
	 *
	 * @param portOwner
	 *            The owner of the ports.
	 * @return The {@link InputPorts} instance, never {@code null}.
	 * @since 7.3.0
	 */
	protected InputPorts createInputPorts(PortOwner portOwner) {
		return new InputPortsImpl(portOwner);
	}

	/**
	 * This method returns an {@link OutputPorts} object for port initialization. Useful for adding
	 * an arbitrary implementation (e.g. changing port creation & (dis)connection behavior,
	 * optionally by customized {@link OutputPort} instances) by overriding this method.
	 *
	 * @param portOwner
	 *            The owner of the ports.
	 * @return The {@link OutputPorts} instance, never {@code null}.
	 * @since 7.3.0
	 */
	protected OutputPorts createOutputPorts(PortOwner portOwner) {
		return new OutputPortsImpl(portOwner);
	}

	/**
	 * This method returns the {@link MDTransformer} object of this operator. This object will
	 * process all meta data of all ports of this operator according to the rules registered to it.
	 * This method can be used to get the transformer and register new Rules for
	 * MetaDataTransformation for the ports using the
	 * {@link MDTransformer#addRule(com.rapidminer.operator.ports.metadata.MDTransformationRule)}
	 * method or one of it's more specialized sisters.
	 */
	public final MDTransformer getTransformer() {
		return transformer;
	}

	/** Returns the ExecutionUnit that contains this operator. */
	public final ExecutionUnit getExecutionUnit() {
		return enclosingExecutionUnit;
	}

	final protected void setEnclosingProcess(ExecutionUnit parent) {
		if (parent != null && this.enclosingExecutionUnit != null) {
			throw new IllegalStateException("Parent already set.");
		}
		this.enclosingExecutionUnit = parent;
	}

	/** Clears output and input ports. */
	public void clear(int clearFlags) {
		if ((clearFlags & Port.CLEAR_SIMPLE_ERRORS) > 0) {
			errorList.clear();
		}
		getInputPorts().clear(clearFlags);
		getOutputPorts().clear(clearFlags);
	}

	/**
	 * Assumes that all preconditions are satisfied. This method is useful to query an operator for
	 * its output, given that it was wired correctly.
	 */
	public void assumePreconditionsSatisfied() {
		for (InputPort inputPort : getInputPorts().getAllPorts()) {
			for (Precondition precondition : inputPort.getAllPreconditions()) {
				precondition.assumeSatisfied();
			}
		}
	}

	/**
	 * This method will disconnect all ports from as well the input ports as well as the
	 * outputports.
	 */
	public void disconnectPorts() {
		for (OutputPort port : getOutputPorts().getAllPorts()) {
			if (port.isConnected()) {
				port.disconnect();
			}
		}
		for (InputPort port : getInputPorts().getAllPorts()) {
			if (port.isConnected()) {
				port.getSource().disconnect();
			}
		}
	}

	/**
	 * If this method is called for perform the meta data transformation on this operator. It needs
	 * the meta data on the input Ports to be already calculated.
	 */
	public void transformMetaData() {
		clear(Port.CLEAR_META_DATA_ERRORS);
		if (!isEnabled()) {
			return;
		}
		getInputPorts().checkPreconditions();
		getTransformer().transformMetaData();
	}

	/**
	 * By default, all ports will be auto-connected by
	 * {@link ExecutionUnit#autoWire(CompatibilityLevel, boolean, boolean)}. Optional outputs were
	 * handled up to version 4.4 by parameters. From 5.0 on, optional outputs are computed iff the
	 * corresponding port is connected. For backward compatibility, operators can check if we should
	 * auto-connect a port by overriding this method (e.g. by checking a deprecated parameter).
	 * TODO: Remove in later versions
	 */
	public boolean shouldAutoConnect(OutputPort outputPort) {
		return true;
	}

	/**
	 * @see #shouldAutoConnect(OutputPort)
	 */
	public boolean shouldAutoConnect(InputPort inputPort) {
		return true;
	}

	/**
	 * Returns the first ancestor that does not have a parent. Note that this is not necessarily a
	 * ProcessRootOperator!
	 */
	public Operator getRoot() {
		OperatorChain parent = getParent();
		if (parent == null) {
			return this;
		} else {
			return parent.getRoot();
		}
	}

	/**
	 * This method is called when the operator with "oldName" is renamed to "newName". It provides a
	 * hook, when this operator's parameter are depending on operator names. The
	 * {@link ParameterTypeInnerOperator} is an example for such an dependency. This way it is
	 * possible to change the parameter's according to the renaming.
	 */
	public void notifyRenaming(String oldName, String newName) {
		getParameters().notifyRenaming(oldName, newName);
	}

	/**
	 * This method is called when the operator given by {@code oldName} (and {@code oldOp} if it is not {@code null})
	 * was replaced with the operator described by {@code newName} and {@code newOp}.
	 * This will inform the {@link Parameters} of the replacing.
	 *
	 * @param oldName
	 * 		the name of the old operator
	 * @param oldOp
	 * 		the old operator; can be {@code null}
	 * @param newName
	 * 		the name of the new operator
	 * @param newOp
	 * 		the new operator; must not be {@code null}
	 * @see Parameters#notifyReplacing(String, Operator, String, Operator)
	 * @since 9.3
	 */
	public void notifyReplacing(String oldName, Operator oldOp, String newName, Operator newOp) {
		getParameters().notifyReplacing(oldName, oldOp, newName, newOp);
	}

	@Override
	protected void fireUpdate(Operator operator) {
		super.fireUpdate(operator);
		if (getProcess() != null) {
			getProcess().fireOperatorChanged(this);
		}
	}

	/**
	 * This method will flag this operator's results as dirty. Currently unused feature.
	 */
	public void makeDirty() {
		if (!dirty) {
			this.dirty = true;
			if (getProcess().getDebugMode() == DebugMode.COLLECT_METADATA_AFTER_EXECUTION) {
				clear(Port.CLEAR_REAL_METADATA);
			}
			dirtynessWasPropagated = false;
			fireUpdate();
		}
	}

	protected void propagateDirtyness() {
		if (isDirty() && !dirtynessWasPropagated) {
			dirtynessWasPropagated = true;
			for (OutputPort port : getOutputPorts().getAllPorts()) {
				if (port.isConnected()) {
					Operator operator = port.getDestination().getPorts().getOwner().getOperator();
					operator.makeDirty();
					operator.propagateDirtyness();
				}
			}
		}
	}

	private void setNotDirty() {
		this.dirty = false;
		fireUpdate();
	}

	/**
	 * Returns whether the results on the output ports of this operator are dirty. This is the case
	 * when the results depend on old parameter settings or old data from an input port, whose
	 * connected output port is flaged as dirty.
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * This returns the number of breakpoints: 0, 1 or 2.
	 */
	public int getNumberOfBreakpoints() {
		int num = 0;
		for (boolean bp : breakPoint) {
			if (bp) {
				num++;
			}
		}
		return num;
	}

	/**
	 * Returns true if this operator contains at least one {@link InputPort} which accepts an input
	 * of the given class (loose checking).
	 */
	public boolean acceptsInput(Class<? extends IOObject> inputClass) {
		MetaData metaData = new MetaData(inputClass);
		for (InputPort inPort : getInputPorts().getAllPorts()) {
			if (inPort.isInputCompatible(metaData, CompatibilityLevel.PRE_VERSION_5)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this operator contains at least one {@link OutputPort} provided that its
	 * input ports are satisfied.
	 */
	@SuppressWarnings("deprecation")
	public boolean producesOutput(Class<? extends IOObject> outputClass) {
		assumePreconditionsSatisfied();
		transformMetaData();
		for (OutputPort outPort : getOutputPorts().getAllPorts()) {
			if (outPort.getMetaData() != null && outputClass.isAssignableFrom(outPort.getMetaData().getObjectClass())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This returns the {@link PortOwner} of this operator. See {@link PortOwner} for more details.
	 */
	public PortOwner getPortOwner() {
		return portOwner;
	}

	/**
	 * This method is called before auto-wiring an operator. Operators can reorder outputs in order
	 * to influence how subsequent operators are wired. This is only necessary for legacy operators
	 * like IOConsumer or IOSelector. Don't override this method for new operators.
	 */
	protected LinkedList<OutputPort> preAutoWire(LinkedList<OutputPort> readyOutputs) throws OperatorException {
		return readyOutputs;
	}

	/**
	 * Releases of any resources held by this operator due since its execution. In particular,
	 * removes all hard references to IOObjects stored at the ports.
	 */
	public void freeMemory() {
		getInputPorts().freeMemory();
		getOutputPorts().freeMemory();
	}

	/**
	 * Looks up an operator with the given name in the containing process.
	 *
	 * TODO: This method is slow since it scans operators several times. Simply looking at the
	 * {@link Process#operatorNameMap} does not work for parallel execution, however.
	 */
	protected Operator lookupOperator(String operatorName) {
		if (getName().equals(operatorName)) {
			return this;
		}
		ExecutionUnit executionUnit = getExecutionUnit();
		if (executionUnit == null) {
			return null;
		}
		for (Operator sibling : executionUnit.getAllInnerOperators()) {
			if (sibling.getName().equals(operatorName)) {
				return sibling;
			}
		}

		OperatorChain parent = getParent();
		if (parent != null) {
			return parent.lookupOperator(operatorName);
		} else {
			return null;
		}
	}

	/**
	 * The {@link OperatorProgress} should be initialized when starting the operator execution by
	 * setting the total amount of progress (which is {@link OperatorProgress#NO_PROGRESS} by
	 * default) by calling {@link OperatorProgress#setTotal(int)}. Afterwards the progress can be
	 * reported by calling {@link OperatorProgress#setCompleted(int)}. The progress will be reset
	 * before the operator is being executed.
	 *
	 * @return the {@link OperatorProgress} to report progress during operator execution.
	 * @since 7.0.0
	 */
	public final OperatorProgress getProgress() {
		return operatorProgress;
	}

	/**
	 * Returns if this operators {@link #execute()} method is currently executed.
	 */
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Returns if this operator should currently show progress animation. This method can be
	 * overridden to provide unique animation display criteria. By default it checks if the operator
	 * is running. Please note that
	 * {@link com.rapidminer.gui.animation.OperatorAnimationProcessListener#processFinishedOperator}
	 * also depends on this method.
	 */
	public boolean isAnimating() {
		return isRunning();
	}

	/**
	 * @see OperatorVersion
	 */
	public void setCompatibilityLevel(OperatorVersion compatibilityLevel) {
		this.compatibilityLevel = compatibilityLevel;
		fireUpdate();
	}

	/**
	 * @see OperatorVersion
	 */
	public OperatorVersion getCompatibilityLevel() {
		if (compatibilityLevel == null) {
			compatibilityLevel = OperatorVersion.getLatestVersion(this.getOperatorDescription());
		}
		return compatibilityLevel;
	}

	/**
	 * Returns the versions of an operator <strong>after which its behavior incompatibly
	 * changed</strong> in random order. Only the versions after which the new behavior was
	 * introduced are returned. See comment of {@link OperatorVersion} for details.
	 */
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return EMPTY_OPERATOR_VERSIONS_ARRAY;
	}

	// Resource consumption estimation

	/**
	 * Subclasses can override this method if they are able to estimate the consumed resources (CPU
	 * time and memory), based on their input. The default implementation returns null.
	 */
	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return null;
	}

	/** Visitor pattern visiting all operators in subprocesses and the operator itself. */
	public void walk(Visitor<Operator> visitor) {
		visitor.visit(this);
	}

	/** Returns the current CPU time if supported, or the current system time otherwise. */
	private static long getThreadCpuTime() {
		return CPU_TIME_SUPPORTED ? ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId())
				: System.currentTimeMillis() * 1000000l;
	}

}
