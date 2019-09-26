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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.ProcessListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.operator.ports.SinglePortExtender;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.AboveOperatorVersionCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.parameter.conditions.NonEqualTypeCondition;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ListenerTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;
import com.rapidminer.tools.mail.connection.MailConnectionUtilities;


/**
 * Each process must contain exactly one operator of this class and it must be the root operator of
 * the process. The only purpose of this operator is to provide some parameters that have global
 * relevance.
 *
 * @author Ingo Mierswa
 */
public final class ProcessRootOperator extends OperatorChain implements ConnectionSelectionProvider {

	private static final OperatorVersion OPERATOR_REPLACE_MACROS_CAUSES_ERROR_ON_UNDEFINED = new OperatorVersion(6, 0, 2);

	/** The property name for &quot;The default random seed (-1: random random seed).&quot; */
	public static final String PROPERTY_RAPIDMINER_GENERAL_RANDOMSEED = "rapidminer.general.randomseed";

	public static final String PARAMETER_ENCODING = "encoding";

	public static final String PARAMETER_LOGVERBOSITY = "logverbosity";

	public static final String PARAMETER_LOGFILE = "logfile";

	public static final String PARAMETER_RESULTFILE = "resultfile";

	public static final String PARAMETER_TEMP_DIR = "temp_dir";

	public static final String PARAMETER_DELETE_TEMP_FILES = "delete_temp_files";

	public static final String PARAMETER_RANDOM_SEED = "random_seed";

	public static final String PARAMETER_SEND_MAIL = "send_mail";

	public static final String[] PARAMETER_SEND_MAIL_OPTIONS = { "always", "never", "for_long_processes" };
	public static final int PARAMETER_SEND_MAIL_ALWAYS = 0;
	public static final int PARAMETER_SEND_MAIL_NEVER = 1;
	public static final int PARAMETER_SEND_MAIL_FOR_LONG = 2;

	public static final String PARAMETER_PROCESS_DURATION_FOR_MAIL = "process_duration_for_mail";

	public static final String PARAMETER_NOTIFICATION_EMAIL = "notification_email";

	static {
		ParameterService.registerParameter(new ParameterTypeInt(PROPERTY_RAPIDMINER_GENERAL_RANDOMSEED,
				I18N.getSettingsMessage(PROPERTY_RAPIDMINER_GENERAL_RANDOMSEED, I18N.SettingsType.DESCRIPTION), -1,
				Integer.MAX_VALUE, RandomGenerator.DEFAULT_SEED));
	}

	/** The list of listeners for process events. */
	private final List<ProcessListener> listenerList = new LinkedList<>();

	/** The process which is connected to this process operator. */
	private Process process;

	/** @since 9.4.1 */
	private final ConnectionInformationSelector connectionSelector;

	private final SinglePortExtender<InputPort> resultPortExtender = new SinglePortExtender<>("result", getSubprocess(0)
			.getInnerSinks());

	private final OutputPortExtender processInputExtender = new OutputPortExtender("input", getSubprocess(0)
			.getInnerSources());

	/** Creates a new process operator without reference to an process. */
	public ProcessRootOperator(OperatorDescription description) {
		this(description, null);
		getTransformer().addRuleAtBeginning(new MDTransformationRule() {

			@Override
			public void transformMD() {
				if (getProcess() == null) {
					// can happen during loading
					return;
				}
				ProcessContext context = getProcess().getContext();
				if (getProcess().getProcessState() == Process.PROCESS_STATE_STOPPED) {
					// We apply macros only if process is stopped so we dont break the process
					// in case we have a meta data propagation while process runs (in which
					// case it should be disabled anyway
					getProcess().applyContextMacros();
				}
				for (int i = 0; i < context.getInputRepositoryLocations().size(); i++) {
					String location = context.getInputRepositoryLocations().get(i);
					if (location != null && location.length() > 0) {
						if (i < getSubprocess(0).getInnerSources().getNumberOfPorts()) {
							OutputPort port = getSubprocess(0).getInnerSources().getPortByIndex(i);
							RepositoryLocation loc;
							try {
								loc = getProcess().resolveRepositoryLocation(location);
							} catch (Exception e1) {
								addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
										"repository_access_error", location, e1.toString()));
								return;
							}
							try {
								Entry entry = loc.locateEntry();
								if (entry == null) {
									addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
											"repository_location_does_not_exist", location));
								} else if (entry instanceof IOObjectEntry) {
									port.deliverMD(((IOObjectEntry) entry).retrieveMetaData());
								} else if (entry instanceof BlobEntry) {
									port.deliverMD(new MetaData(FileObject.class));
								} else {
									addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
											"repository_location_wrong_type", location, entry.getType(), "IOObject"));
								}
							} catch (RepositoryException e) {
								addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
										"repository_access_error", location, e.getMessage()));
							}
						}
					}
				}

			}
		});
	}

	/** Creates a new process operator which directly references to the given process. */
	public ProcessRootOperator(OperatorDescription description, Process process) {
		super(description, "Main Process");
		connectionSelector = setupConnectionSelector();
		resultPortExtender.start();
		processInputExtender.start();
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));

		addValue(new ValueDouble("memory", "The current memory usage.") {

			@Override
			public double getDoubleValue() {
				return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			}
		});
		setProcess(process);
		rename("Root");
	}

	/**
	 * Creates a {@link ConnectionInformationSelector} with parameter key {@link MailConnectionHandler#getParameterKey()}
	 * from {@link MailConnectionHandler#SEND}
	 *
	 * @since 9.4.1
	 */
	private ConnectionInformationSelector setupConnectionSelector() {
		return new ConnectionInformationSelector(null, null, ProcessRootOperator.this, MailConnectionHandler.SEND.getType()) {

			/** @return {@link MailConnectionHandler#getParameterKey()} from {@link MailConnectionHandler#SEND} */
			@Override
			public String getParameterKey() {
				return MailConnectionHandler.SEND.getParameterKey();
			}
		};
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		try {
			if (getParameterAsInt(PARAMETER_SEND_MAIL) == PARAMETER_SEND_MAIL_NEVER) {
				return;
			}
		} catch (UndefinedParameterError undefinedParameterError) {
			return;
		}
		if (connectionSelector == null) {
			return;
		}
		if (connectionSelector.isConnectionSpecified()) {
			ProcessSetupError error = connectionSelector.checkConnectionTypeMatch(this);
			if (error != null) {
				addError(error);
			}
			return;
		}
		addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(),
				Collections.singletonList(new ParameterSettingQuickFix(this, connectionSelector.getParameterKey())),
				"connection.mail.not_specified"));
	}

	public void deliverInput(List<IOObject> inputs) {
		processInputExtender.deliver(inputs);
	}

	public void deliverInputMD(List<MetaData> inputMD) {
		processInputExtender.deliverMetaData(inputMD);
	}

	/** Sets the process. */
	public void setProcess(Process process) {
		this.process = process;
		registerOperator(this.process);
	}

	/**
	 * Returns the process of this operator if available. Overwrites the method from the superclass.
	 */
	@Override
	public Process getProcess() {
		return process;
	}

	/** Adds an process listener to the list of listeners. */
	public void addProcessListener(ProcessListener l) {
		listenerList.add(l);
	}

	/** Removes an process listener from the list of listeners. */
	public void removeProcessListener(ProcessListener l) {
		listenerList.remove(l);
	}

	private List<ProcessListener> getListenerListCopy() {
		if (listenerList.isEmpty()) {
			return Collections.emptyList();
		} else {
			return new LinkedList<>(listenerList);
		}
	}

	/**
	 * Called at the beginning of the process. Notifies all listeners and the children operators
	 * (super method).
	 */
	@Override
	public void processStarts() throws OperatorException {
		ListenerTools.informAllAndThrow(x -> super.processStarts(), getListenerListCopy(), l -> l.processStarts(process));
	}

	/** Counts the step and notifies all process listeners. */
	public void processStartedOperator(Operator op) {
		ListenerTools.informAllAndThrow(getListenerListCopy(), (Consumer<ProcessListener>) l -> l.processStartedOperator(process, op));
	}

	/** Counts the step and notifies all process listeners. */
	public void processFinishedOperator(Operator op) {
		ListenerTools.informAllAndThrow(getListenerListCopy(), (Consumer<ProcessListener>) l -> l.processFinishedOperator(process, op));
	}

	/**
	 * Called at the end of the process. Notifies all listeners and the children operators (super
	 * method).
	 */
	@Override
	public void processFinished() throws OperatorException {
		ListenerTools.informAllAndThrow(x-> super.processFinished(), getListenerListCopy(), l -> l.processEnded(process));
	}

	/** @since 9.4.1 */
	@Override
	public ConnectionInformationSelector getConnectionSelector() {
		return connectionSelector;
	}

	/** @since 9.4.1 */
	@Override
	public void setConnectionSelector(ConnectionInformationSelector selector) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	 * This method can be used to send an email after the process has finished. Currently only a
	 * working sendmail server is supported.
	 */
	public void sendEmail(IOContainer results, Throwable e) throws UndefinedParameterError {
		int sendEmail = getParameterAsInt(PARAMETER_SEND_MAIL);
		if (sendEmail == PARAMETER_SEND_MAIL_NEVER) {
			return;
		} else if (sendEmail == PARAMETER_SEND_MAIL_FOR_LONG) {
			long minTimeToSendEmail = getParameterAsInt(PARAMETER_PROCESS_DURATION_FOR_MAIL) * 60 * 1000;
			if (System.currentTimeMillis() - getStartTime() < minTimeToSendEmail) {
				return;
			}
		}

		String email = getParameterAsString(PARAMETER_NOTIFICATION_EMAIL);
		ProgressThread mailThread = new ProgressThread("process.send_result_mail") {

			@Override
			public void run() {
				sendEmailAsync(results, e, email);
			}
		};
		mailThread.setIndeterminate(true);
		mailThread.start();
	}

	/**
	 * Sends the result email, called from a {@link ProgressThread}.
	 *
	 * @since 9.4.1
	 * @see MailConnectionUtilities#sendEmail(Operator, String, String, String, java.util.Map)
	 */
	private void sendEmailAsync(IOContainer results, Throwable e, String email) {
		if (email == null) {
			return;
		}
		getLogger().info("Sending notification email to '" + email + "'");

		String name = email;
		int at = name.indexOf('@');
		if (at >= 0) {
			name = name.substring(0, at);
		}

		String operatorName = ProcessRootOperator.this.getName();
		String subject = "Process " + operatorName + " finished";
		StringBuilder content = new StringBuilder("Hello " + name + "," + Tools.getLineSeparator()
				+ Tools.getLineSeparator());
		content.append("I'm sending you a notification message on your process '")
				.append(getProcess().getProcessLocation()).append("'.").append(Tools.getLineSeparator());

		// File logFile = getLog().getLogFile();
		// if (logFile != null) {
		// content.append("Logfile is file://" + logFile.getAbsolutePath() +
		// Tools.getLineSeparator() + Tools.getLineSeparator());
		// }

		if (e != null) {
			content.append("Process failed: ").append(e.toString());
			subject = "Process " + operatorName + " failed";
		}

		if (results != null) {
			content.append(Tools.getLineSeparator()).append(Tools.getLineSeparator()).append("Results:");
			ResultObject result;
			int i = 0;
			while (i < results.size()) {
				try {
					result = results.get(ResultObject.class, i);
					content.append(Tools.getLineSeparator()).append(Tools.getLineSeparator())
							.append(Tools.getLineSeparator()).append(result.toResultString());
					i++;
				} catch (MissingIOObjectException exc) {
					break;
				}
			}
		}
		try {
			MailConnectionUtilities.sendEmail(ProcessRootOperator.this, email, subject, content.toString(), null);
		} catch (MailNotSentException exception) {
			getLogger().log(Level.WARNING, "process.send_mail_failed", new UserError(
					ProcessRootOperator.this, exception.getCause(), exception.getErrorKey(),
					exception.getArguments()).getMessage());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_LOGVERBOSITY, "Log verbosity level.",
				LogService.LOG_VERBOSITY_NAMES, LogService.INIT);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeFile(PARAMETER_LOGFILE, "File to write logging information to.", "log", true);
		type.setExpert(false);
		types.add(type);
		types.add(new ParameterTypeFile(PARAMETER_RESULTFILE, "File to write inputs of the ResultWriter operators to.",
				"res", true));
		int seed = RandomGenerator.DEFAULT_SEED;
		String seedProperty = ParameterService.getParameterValue(PROPERTY_RAPIDMINER_GENERAL_RANDOMSEED);
		try {
			if (seedProperty != null) {
				seed = Integer.parseInt(seedProperty);
			}
		} catch (NumberFormatException e) {
			logWarning("Bad integer in property 'rapidminer.general.randomseed', using default seed ("
					+ RandomGenerator.DEFAULT_SEED + ").");
		}
		types.add(new ParameterTypeInt(PARAMETER_RANDOM_SEED,
				"Global random seed for random generators (-1 for initialization by system time).", Integer.MIN_VALUE,
				Integer.MAX_VALUE, seed));

		types.add(new ParameterTypeCategory(PARAMETER_SEND_MAIL, "Send email upon completion of the proces.",
				PARAMETER_SEND_MAIL_OPTIONS, PARAMETER_SEND_MAIL_NEVER));
		NonEqualTypeCondition sendMailCondition = new NonEqualTypeCondition(this, PARAMETER_SEND_MAIL,
				PARAMETER_SEND_MAIL_OPTIONS, true, PARAMETER_SEND_MAIL_NEVER);
		if (connectionSelector != null) {
			AboveOperatorVersionCondition compCondition = new AboveOperatorVersionCondition(this, MailConnectionUtilities.BEFORE_EMAIL_CONNECTION);
			List<ParameterType> cisTypes = ConnectionInformationSelector.createParameterTypes(connectionSelector);
			types.addAll(cisTypes);
			cisTypes.forEach(p -> p.registerDependencyCondition(sendMailCondition));
			cisTypes.forEach(p -> p.registerDependencyCondition(compCondition));
		}
		ParameterType parameterRecepient = new ParameterTypeString(PARAMETER_NOTIFICATION_EMAIL,
				"Email address for the notification mail.",
				ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_RECIPIENT));
		parameterRecepient.registerDependencyCondition(sendMailCondition);
		types.add(parameterRecepient);

		int defaultTime;
		try {
			defaultTime = Integer.parseInt(ParameterService
					.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_DEFAULT_PROCESS_DURATION_FOR_MAIL));
		} catch (NumberFormatException e) {
			defaultTime = 30;
		}
		ParameterType parameterTimeMail = new ParameterTypeInt(PARAMETER_PROCESS_DURATION_FOR_MAIL,
				"Minimum process duration to send emails (in minutes).", 0, Integer.MAX_VALUE, defaultTime);
		parameterTimeMail.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SEND_MAIL,
				PARAMETER_SEND_MAIL_OPTIONS, true, PARAMETER_SEND_MAIL_FOR_LONG));
		types.add(parameterTimeMail);

		types.addAll(Encoding.getParameterTypes(this));
		return types;
	}

	/**
	 * Convenience backport method to get the results of a process.
	 *
	 * @param omitNullResults
	 *            if set to <code>false</code> the returned {@link IOContainer} will contain
	 *            <code>null</code> values for empty results instead of omitting them.
	 */
	public IOContainer getResults(boolean omitNullResults) {
		InputPorts innerSinks = getSubprocess(0).getInnerSinks();
		return innerSinks.createIOContainer(false, omitNullResults);
	}

	/**
	 * Convenience backport method to get the results of a process. This method will omit empty
	 * result values instead of returning them as <code>null</code> value.
	 */
	public IOContainer getResults() {
		return getSubprocess(0).getInnerSinks().createIOContainer(false, true);
	}

	/** Returns the meta data delivered to the output ports. */
	public List<MetaData> getResultMetaData() {
		LinkedList<MetaData> result = new LinkedList<>();
		for (InputPort resultPort : getSubprocess(0).getInnerSinks().getAllPorts()) {
			result.add(resultPort.getMetaData());
		}
		while (!result.isEmpty() && result.getLast() == null) {
			result.removeLast();
		}
		return result;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		OperatorVersion[] updatedVersions = Arrays.copyOf(old, old.length + 2);
		updatedVersions[old.length] = OPERATOR_REPLACE_MACROS_CAUSES_ERROR_ON_UNDEFINED;
		updatedVersions[old.length + 1] = MailConnectionUtilities.BEFORE_EMAIL_CONNECTION;
		return updatedVersions;
	}

}
