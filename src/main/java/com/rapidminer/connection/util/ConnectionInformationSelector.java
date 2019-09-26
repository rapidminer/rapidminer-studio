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
package com.rapidminer.connection.util;

import static com.rapidminer.connection.util.ConnectionI18N.getTypeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.InvalidRepositoryEntryError;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.PortUserError;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.ParameterError;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeConnectionLocation;
import com.rapidminer.parameter.conditions.PortConnectedCondition;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryEntryNotFoundException;
import com.rapidminer.repository.RepositoryEntryWrongTypeException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;


/**
 * Helper class that can handle an handler's {@link ConnectionInformation} through a passthrough port/parameter combination.
 * Instances can also set up default transformation rules.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionInformationSelector {

	public static final String PARAMETER_CONNECTION_ENTRY = "connection_entry";

	private static final String PARAMETER_DESC_PREFIX = "gui.label.connection.operator_parameter.";
	private static final String CONNECTION_MISMATCHED_TYPE = "connection.mismatched_type";

	private InputPort input;
	private OutputPort output;
	private ParameterHandler handler;
	private String conType;

	/**
	 * Minimal constructor with related Operator and connection type. Will create an input/output port pair named "connection".
	 */
	public ConnectionInformationSelector(Operator operator, String conType) {
		this(operator.getInputPorts().createPassThroughPort("connection"), operator.getOutputPorts().createPassThroughPort("connection"), operator, conType);
	}

	/**
	 * Sets up an instance with the given handler, port pair (if any) and connection type.
	 *
	 * @param input
	 * 		the input port; can be {@code null}
	 * @param output
	 * 		the output port; can be {@code null}
	 * @param handler
	 * 		the handler that this instance is coupled with; must not be {@code null}
	 * @param conType
	 * 		the connection type as handled by its corresponding {@link com.rapidminer.connection.ConnectionHandler}
	 */
	public ConnectionInformationSelector(InputPort input, OutputPort output, ParameterHandler handler, String conType) {
		this.input = input;
		this.output = output;
		this.handler = handler;
		this.conType = conType;
	}

	/**
	 * Returns the parameter type's key of this selector. Subclasses might override this.
	 *
	 * @return always {@value #PARAMETER_CONNECTION_ENTRY}
	 * @since 9.4.1
	 */
	public String getParameterKey() {
		return PARAMETER_CONNECTION_ENTRY;
	}

	/**
	 * Creates default transformation rules if {@link #input} is not {@code null}. This will add a {@link SimplePrecondition}
	 * on the input port, and adds a
	 * {@link com.rapidminer.operator.ports.metadata.MDTransformer#addPassThroughRule passthrough rule}.
	 */
	public void makeDefaultPortTransformation() {
		if (input != null) {
			input.addPrecondition(new SimplePrecondition(input, new ConnectionInformationMetaData(), false) {

				@Override
				protected boolean isMandatory() {
					return portMandatory();
				}
			});
		} else {
			return;
		}
		if (output != null && handler instanceof Operator) {
			((Operator) handler).getTransformer().addPassThroughRule(input, output);
		}
	}

	/**
	 * @return whether or not the input port is mandatory. Will return {@code false} if the parameter is set
	 */
	protected boolean portMandatory() {
		return !handler.getParameters().getParameterType(getParameterKey()).isHidden() && !handler.getParameters().isSet(getParameterKey());
	}

	/** Get the input port. Might return {@code null}. */
	public InputPort getInput() {
		return input;
	}

	/** Get the output port. Might return {@code null}. */
	public OutputPort getOutput() {
		return output;
	}

	/** Get the associated {@link ParameterHandler}. Can be an {@link Operator} */
	public ParameterHandler getHandler() {
		return handler;
	}

	/** Get the allowed connection type. Can be {@code null}, allowing any connection type */
	public String getConnectionType() {
		return conType;
	}

	/**
	 * Checks whether a connection is specified through the port or parameter. Does not test for validity of parameter.
	 */
	public boolean isConnectionSpecified() {
		if (input == null || !input.isConnected()) {
			return handler.getParameters().isSet(getParameterKey());
		}
		return input.getMetaData() instanceof ConnectionInformationMetaData;
	}

	/**
	 * Get the meta data of the connection if it is properly specified. Otherwise just returns a generic {@link ConnectionInformationMetaData}.
	 * Will try to either get matching meta data from the input port or from the parameter specified repository location.
	 */
	public ConnectionInformationMetaData getMetaData() {
		try {
			return getMetaDataOrThrow();
		} catch (RepositoryException e) {
			return new ConnectionInformationMetaData();
		}
	}

	/**
	 * Get the meta data of the connection if it is properly specified. Otherwise returns {@code null} for
	 * not or wrongly connected inputs, as well as for an unset or unused {@link #getParameterKey()} parameter.
	 * Will throw repository related exceptions in case of misconfigured parameters.
	 *
	 * @return the properly specified meta data or {@code null}
	 * @throws RepositoryException
	 * 		if a repository related problem occurs
	 */
	public ConnectionInformationMetaData getMetaDataOrThrow() throws RepositoryException {
		if (input != null && input.isConnected()) {
			MetaData md = input.getMetaData();
			if (md instanceof ConnectionInformationMetaData) {
				return (ConnectionInformationMetaData) md;
			}
			// wrong kind of MD or no MD => taken care of by precondition
			return null;
		}
		if (handler.getParameters().getParameterType(getParameterKey()).isHidden()) {
			return null;
		}
		if (!handler.getParameters().isSet(getParameterKey())) {
			// parameter not set => but is mandatory
			return null;
		}
		RepositoryLocation location;
		try {
			location = getRepoLocationFromParameter();
		} catch (UserError e) {
			throw new RepositoryException(e.getMessage());
		}
		Entry entry = location.locateEntry();
		if (entry == null) {
			throw new RepositoryEntryNotFoundException(location);
		}
		if (!(entry instanceof ConnectionEntry)) {
			throw new RepositoryEntryWrongTypeException(location, ConnectionEntry.TYPE_NAME, entry.getType());
		}
		MetaData md = ((ConnectionEntry) entry).retrieveMetaData();
		if (!(md instanceof ConnectionInformationMetaData)) {
			throw  new RepositoryEntryWrongTypeException(location, ConnectionEntry.TYPE_NAME,
					md == null ? null : md.getObjectClass().getSimpleName());
		}
		ConnectionInformationMetaData metaData = (ConnectionInformationMetaData) md;
		Annotations annotations = metaData.getAnnotations();
		if (annotations == null) {
			annotations = new Annotations();
			metaData.setAnnotations(annotations);
		}
		annotations.setAnnotation(Annotations.KEY_SOURCE, entry.getLocation().toString());
		return metaData;
	}

	/**
	 * Returns the selected {@link ConnectionInformation} if any. Will prefer the input port over the parameter.
	 * Will throw a {@link UserError} if any error occurs, i.e. if the data is not present or does not match.
	 *
	 * @return a connection information, never {@code null}
	 * @throws UserError
	 * 		if an error occurs
	 */
	public ConnectionInformation getConnection() throws UserError {
		ConnectionInformationContainerIOObject container;
		boolean connectionFromPort = input != null && input.isConnected();
		if (connectionFromPort) {
			container = input.getDataOrNull(ConnectionInformationContainerIOObject.class);
			if (container == null) {
				// no data (yet)? => infer repo location from meta data
				RepositoryLocation location = getRepoLocationFromInputMD(input);
				if (location != null) {
					container = extractConnectionFromLocation(location);
				}
			}
		} else {
			if (!handler.getParameters().isSet(getParameterKey())) {
				throw new UserError(null, "connection.no_connection");
			}
			RepositoryLocation location = getRepoLocationFromParameter();
			container = extractConnectionFromLocation(location);
		}
		if (container == null) {
			throw new UserError(null, "connection.no_container");
		}
		// don't use the original
		ConnectionInformation ci = container.getConnectionInformation().copy();

		if (conType != null) {
			String actualType = ci.getConfiguration().getType();
			if (!conType.equals(actualType)) {
				if (connectionFromPort) {
					throw new PortUserError(input, CONNECTION_MISMATCHED_TYPE,
							getTypeName(conType), getTypeName(actualType));
				} else {
					throw new ParameterError(null, CONNECTION_MISMATCHED_TYPE,
							getParameterKey(), getTypeName(conType), getTypeName(actualType));
				}
			}
		}
		return ci;
	}

	/**
	 * Checks that this {@link ConnectionInformationSelector} has a correctly defined connection at hand,
	 * if that is indicated by a connected port or the parameter {@link #getParameterKey()}.
	 * Will return a {@link ProcessSetupError} if one of the following holds:
	 * <ul>
	 * 		<li>A repository error occurred when trying to retrieve the information</li>
	 * 		<li>No handler was registered for the {@link #conType} specified here, if that type is not {@code null}</li>
	 * 		<li>The provided connection type and the specified connection type don't match</li>
	 * </ul>
	 * The last case can be separated into two subcases; if the connection is provided by the {@link InputPort},
	 * then the returned error is a {@link SimpleMetaDataError}. If the connection is provided by the parameter,
	 * the error is an {@link InvalidRepositoryEntryError}.
	 *
	 * @param operator
	 * 		the operator to check on
	 * @return {@code null} if no errors occurred, otherwise an error as specified above
	 * @see #getMetaDataOrThrow()
	 */
	public ProcessSetupError checkConnectionTypeMatch(Operator operator) {
		ConnectionInformationMetaData metaData;
		List<ParameterSettingQuickFix> parameterSetting = Collections.singletonList(new ParameterSettingQuickFix(operator, getParameterKey()));
		try {
			metaData = getMetaDataOrThrow();
			if (metaData == null) {
				return null;
			}
		} catch (RepositoryException e) {
			String errorKey = e.getMessage();
			boolean isAbsolute = errorKey != null;
			if (!isAbsolute) {
				errorKey = "connection.repository_error";
			}
			return new SimpleProcessSetupError(Severity.ERROR, operator.getPortOwner(),
						parameterSetting, isAbsolute, errorKey);
		}
		String wantedType = getConnectionType();
		if (wantedType == null) {
			return null;
		}
		if (!ConnectionHandlerRegistry.getInstance().isTypeKnown(wantedType)) {
			return new SimpleProcessSetupError(Severity.ERROR, operator.getPortOwner(), "connection.no_handler_registered");
		}
		String foundType = metaData.getConnectionType();
		if (foundType == null || wantedType.equals(foundType)) {
			return null;
		}
		String errorKey = CONNECTION_MISMATCHED_TYPE;
		String wantedTypeName = getTypeName(wantedType);
		String foundTypeName = getTypeName(foundType);
		Port conInput = getInput();
		boolean connectionFromPort = conInput != null && conInput.isConnected();
		if (connectionFromPort) {
			return new SimpleMetaDataError(Severity.ERROR, conInput, Collections.emptyList(), errorKey, wantedTypeName, foundTypeName);
		} else {
			return new SimpleProcessSetupError(Severity.ERROR, operator.getPortOwner(),
					 parameterSetting, errorKey, wantedTypeName, foundTypeName);
		}
	}

	/** Passes matching data through if both input and output ports exist. */
	public void passDataThrough() {
		if (input != null && output != null) {
			IOObject data = null;
			try {
				data = input.getDataOrNull(ConnectionInformationContainerIOObject.class);
			} catch (UserError userError) {
				// ignore; do nothing
			}
			output.deliver(data);
		}
	}

	/** Passes matching data through as a copy if both input and output ports exist. */
	public void passCloneThrough() {
		if (input != null && output != null) {
			IOObject data = null;
			try {
				data = input.getDataOrNull(ConnectionInformationContainerIOObject.class);
			} catch (UserError userError) {
				// ignore; do nothing
			}
			output.deliver(data == null ? null : data.copy());
		}
	}

	/**
	 * Extracts a repository location from the input ports {@link MetaData} if possible.
	 * For this to work, the input port needs to be connected and have correct {@link MetaData}
	 * of type {@link ConnectionInformationMetaData} that are annotated with {@value Annotations#KEY_SOURCE}.
	 * This annotation needs to be a valid {@link RepositoryLocation} which will then be returned.
	 * Otherwise this method returns {@code null}.
	 *
	 * @param input
	 * 		the input port to check for meta data; must not be {@code null}
	 * @return a valid repository location or {@code null}
	 * @see com.rapidminer.operator.io.RepositorySource#getGeneratedMetaData() RepositorySource.getGeneratedMetaData()
	 */
	private RepositoryLocation getRepoLocationFromInputMD(InputPort input) {
		MetaData md;
		try {
			md = input.getMetaData(ConnectionInformationMetaData.class);
			if (md != null && md.getAnnotations() != null) {
				String source = md.getAnnotations().getAnnotation(Annotations.KEY_SOURCE);
				if (source != null) {
					return new RepositoryLocation(source);
				}
			}
		} catch (IncompatibleMDClassException | MalformedRepositoryLocationException e) {
			// ignore
		}
		return null;
	}

	/**
	 * Extracts a {@link ConnectionInformationContainerIOObject} from the given {@link RepositoryLocation} if possible.
	 * If the associated {@link Entry} is not a {@link ConnectionEntry}, no (valid) data is stored there or a {@link RepositoryException}
	 * occurs, this method will tjrow a corresponding {@link UserError}.
	 *
	 * @param location
	 * 		the repository location to check/extract from; must not be {@code null}
	 * @return a valid connection container, never {@code null}
	 * @throws UserError
	 * 		if an error occurs
	 */
	private ConnectionInformationContainerIOObject extractConnectionFromLocation(RepositoryLocation location) throws UserError {
		ConnectionInformationContainerIOObject container;
		try {
			Entry entry = location.locateEntry();
			if (!(entry instanceof ConnectionEntry)) {
				throw new UserError(null, "connection.wrong_entry_type");
			}
			IOObject data = ((ConnectionEntry) entry).retrieveData(null);
			if (!(data instanceof ConnectionInformationContainerIOObject)) {
				throw new UserError(null, "connection.wrong_entry_data");
			}
			container = (ConnectionInformationContainerIOObject) data;
		} catch (RepositoryException e) {
			throw new UserError(null, e, "connection.repository_error", location.getName());
		}
		return container;
	}

	/** Resolves the repository location. Will make a distinction between an operater and a simple parameter handler */
	private RepositoryLocation getRepoLocationFromParameter() throws UserError {
		return RepositoryLocation.getRepositoryLocation(handler.getParameterAsString(getParameterKey()),
				handler instanceof Operator ? (Operator) handler : null);
	}

	/**
	 * Creates and returns the list of parameters associated with the given selector.
	 * By default this contains only one parameter of type {@link ParameterTypeConnectionLocation} that will be hidden while
	 * the input port is connected.
	 */
	public static List<ParameterType> createParameterTypes(ConnectionInformationSelector cis) {
		ArrayList<ParameterType> types = new ArrayList<>();
		ParameterType type = new ParameterTypeConnectionLocation(cis.getParameterKey(),
				createConnectionEntryDescription(PARAMETER_CONNECTION_ENTRY, cis.conType,
						"Select a connection from a repository"), cis.conType);
		if (cis.input != null) {
			type.registerDependencyCondition(new PortConnectedCondition(cis.handler, () -> cis.input, true, false));
		}
		types.add(type);
		return types;
	}

	/**
	 * Creates an {@link MDTransformationRule} that checks if a {@link ConnectionInformation} is provided either through
	 * the input port or as a parameter of the given provider. If any error occurs, adds a {@link ProcessSetupError}
	 * to either the port or operator.
	 *
	 * @param provider
	 * 		the connection selection provider; should also be an {@link Operator}
	 * @return the meta data transformation rule
	 * @see #checkConnectionTypeMatch(Operator)
	 * @since 9.4.1
	 */
	public static MDTransformationRule makeConnectionCheckTransformation(ConnectionSelectionProvider provider) {
		if (!(provider instanceof Operator)) {
			return () -> {};
		}
		Operator operator = (Operator) provider;
		return () -> {
			ConnectionInformationSelector selector = provider.getConnectionSelector();
			if (selector == null) {
				return;
			}
			ProcessSetupError error = selector.checkConnectionTypeMatch(operator);
			if (error == null) {
				return;
			}
			if (error instanceof MetaDataError) {
				InputPort input = selector.getInput();
				if (input != null) {
					input.addError((MetaDataError) error);
					return;
				}
			}
			operator.addError(error);
		};
	}

	/**
	 * Create an i18n compliant description if possible. Looks up the i18n key
	 * {@value #PARAMETER_DESC_PREFIX}{@code {key}.[any|type].desc}, and uses the i18n name of the connection type
	 * if possible.
	 *
	 * @param key
	 * 		the parameter key
	 * @param conType
	 * 		the connection type
	 * @param defaultDescription
	 * 		the description to be used if no i18n is available
	 * @return the i18n or default description
	 */
	private static String createConnectionEntryDescription(String key, String conType, String defaultDescription) {
		String conTypeName = "";
		String typeKey = ".any";
		if (conType != null && ConnectionHandlerRegistry.getInstance().isTypeKnown(conType)) {
			conTypeName = getTypeName(conType);
			typeKey = ".type";
		}
		String description = I18N.getGUIMessageOrNull(PARAMETER_DESC_PREFIX + key + typeKey + ".desc", conTypeName);
		return description != null ? description : defaultDescription;
	}
}
