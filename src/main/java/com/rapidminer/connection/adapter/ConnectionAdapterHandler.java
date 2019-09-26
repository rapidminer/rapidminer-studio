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
package com.rapidminer.connection.adapter;

import static com.rapidminer.connection.util.ConnectionInformationSelector.createParameterTypes;
import static com.rapidminer.tools.FunctionWithThrowable.suppress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.Process;
import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.legacy.ConversionException;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.connection.util.GenericHandler;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.TestResult.ResultType;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.SimpleListBasedParameterHandler;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.NonEqualStringCondition;
import com.rapidminer.parameter.conditions.PortConnectedCondition;
import com.rapidminer.repository.RepositoryAccessor;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.config.AbstractConfigurator;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurableConnectionHandler;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;
import com.rapidminer.tools.config.TestConfigurableAction;
import com.rapidminer.tools.config.actions.ActionResult;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * {@link ConnectionHandler} which can convert an existing {@link ConnectionAdapter} into a
 * {@link ConnectionInformation} and back.
 * <p>
 * This class also provides helper methods to centralize the integration with {@link ConnectionInformationSelector},
 * {@link ParameterType ParameterTypes} and {@link ConnectionAdapter ConnectionAdapters}.
 * <ul>
 *     <li>{@link #registerHandler(ConnectionAdapterHandler) Register a handler for an adapter creator}</li>
 *     <li>{@link #getHandler(String) Get the appropriate handler for a given type}</li>
 *     <li>{@link #getConnectionParameters(Operator, String, ParameterTypeConfigurable)
 *     Get connection/configurable related parameters and register a selector if possible}</li>
 *     <li>{@link #getAdapter(Operator, String, String) Get the adapter specified in an operator}</li>
 * </ul>
 * <p>
 * Subclasses should check if they need to override specific methods
 * <ul>
 *     <li><strong>{@link #parametersByGroup()}</strong>: Specify which parameters will go in which group;
 *     by default, all parameters specified by {@link #getParameterTypes(ParameterHandler)} will be grouped
 *     in the {@value #BASE_GROUP} group</li>
 *     <li><strong>{@link #getAdditionalActions()}</strong>: Additional actions such as clearing the cache or configuring
 *     an environment should be returned here. By default, this is an empty map</li>
 *     <li><strong>{@link #initialize()}</strong>: If additional setup is necessary for the handler,
 *     do it in this method. If you do override this method, you also need to override {@link #isInitialized()}.
 *     By default the handler is initialized with a noop.</li>
 *     <li><strong>{@link #validate(ConnectionInformation)}</strong> and <strong>{@link #test(TestExecutionContext)}</strong>:
 *     If you need to do specialty checks, you can override the validation and testing methods. By default, they rely on
 *     the relation between the parameters provided by {@link #getParameterTypes(ParameterHandler)} and
 *     {@link ConnectionAdapter#getTestAction()} respectively.</li>
 * </ul>
 *
 * @author Jan Czogalla, Gisa Meier
 * @see AbstractConfigurator
 * @see <a href="https://kb.rapidminer.com/display/RMSTUDIO/Converting+old+Configurables+to+New+Connection+Management">
 *     Confluence article for converting configurables to new connection management</a>
 * @since 9.3
 */
public abstract class ConnectionAdapterHandler<T extends ConnectionAdapter>
		extends AbstractConfigurator<T> implements ConfigurableConnectionHandler<T> {

	/** The group key for the default parameter group */
	public static final String BASE_GROUP = "basic";

	//region parameter related constants
	/** Operator parameter key for the selection between old configurables and new connection locations */
	public static final String PARAMETER_CONNECTION_SOURCE = "connection_source";

	/** Indicator to use old configurables for {@link #PARAMETER_CONNECTION_SOURCE} */
	public static final String PREDEFINED_MODE = "predefined";
	/** Indicator to use new connection location for {@link #PARAMETER_CONNECTION_SOURCE} */
	public static final String REPOSITORY_MODE = "repository";
	/** Dropdown list for {@link #PARAMETER_CONNECTION_SOURCE} */
	private static final String[] CONNECTION_SOURCE_MODES = {PREDEFINED_MODE, REPOSITORY_MODE};
	//endregion

	/** Map of all registered {@link ConnectionHandler ConnectionHandlers} that are based on {@link ConnectionAdapterHandler} */
	private static final Map<String, ConnectionAdapterHandler> handlerMap = new HashMap<>();

	/** The namespace of this handler, might be {@code null} */
	private final String namespace;

	/**
	 * Constructor for subclasses; the namespace is optional, but if provided, must not be empty
	 *
	 * @param namespace {@code null} or a non-empty string
	 */
	protected ConnectionAdapterHandler(String namespace) {
		if (namespace != null) {
			this.namespace = ValidationUtil.requireNonEmptyString(namespace, "namespace");
		} else {
			this.namespace = null;
		}
	}

	//region implementation of ConnectionHandler
	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public String getType() {
		if (namespace == null) {
			return getTypeId();
		}
		return namespace + ':' + getTypeId();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This handler uses {@link #create(String, Map)} to create a new {@link ConnectionAdapter} and then converts it.
	 *
	 * @return a new connection or {@code null} if an error occurs
	 * @see #convert(ConnectionAdapter)
	 */
	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		try {
			Map<String, String> defaultValues = getParameterTypes(null).stream()
					.filter(p -> p.getDefaultValueAsString() != null && !p.getDefaultValueAsString().isEmpty())
					.collect(Collectors.toMap(ParameterType::getKey, ParameterType::getDefaultValueAsString));
			return convert(create(name, defaultValues));
		} catch (ConfigurationException | ConversionException e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getErrorMessage("com.rapidminer.connection.adapter.creation_error", name, e.getMessage()), e);
			return null;
		}
	}

	/** @see #getConfigurableClass() */
	@Override
	public boolean canConvert(Object oldConnectionObject) {
		return getConfigurableClass().isInstance(oldConnectionObject);
	}

	/**
	 * Converts a {@link ConnectionAdapter} to a {@link ConnectionInformation}.
	 *
	 * @param oldConnectionObject
	 * 		the adapter to be converted; must not be {@code null}
	 * @see #getParameterTypes(ParameterHandler)
	 * @see #parametersByGroup()
	 */
	@Override
	public ConnectionInformation convert(T oldConnectionObject) throws ConversionException {
		if (!canConvert(oldConnectionObject)) {
			throw new ConversionException("Can not convert " + oldConnectionObject);
		}
		// find encrypted parameter keys
		Map<String, ParameterType> encryptedKeys = getParameterTypes(null).stream()
				.filter(p -> p instanceof ParameterTypePassword)
				.collect(Collectors.toMap(ParameterType::getKey, p -> p));

		// build the new connection based on all possible parameters and the configurables actual parameter values
		Map<String, String> parameters = oldConnectionObject.getParameters();
		ConnectionConfigurationBuilder builder = new ConnectionConfigurationBuilder(oldConnectionObject.getName(), getType());
		for (Entry<String, List<String>> entry : parametersByGroup().entrySet()) {
			List<ConfigurationParameter> cps = new ArrayList<>();
			for (String p : entry.getValue()) {
				boolean isEncrypted = encryptedKeys.containsKey(p);
				String value = parameters.get(p);
				if (isEncrypted) {
					value = encryptedKeys.get(p).transformNewValue(value);
				}
				cps.add(new ConfigurationParameterImpl(p, value, isEncrypted));
			}
			builder.withKeys(entry.getKey(), cps);
		}

		return new ConnectionInformationBuilder(builder.build()).build();
	}

	/** @see ConnectionAdapter#getActions() */
	@Override
	public Map<String, Runnable> getAdditionalActions() {
		return Collections.emptyMap();
	}

	/** Does nothing by default */
	@Override
	public void initialize() {
		// noop
	}
	//endregion

	//region adapter methods
	/**
	 * Gets the {@link ConnectionAdapter} that corresponds to the given {@link ConnectionInformation} and {@link Operator}.
	 * Uses the {@link ValueProviderHandlerRegistry#injectValues(ConnectionInformation, Operator, boolean) injection mechanism}
	 * to create a fully functional adapter and {@link #validate(Function, Function) validates} the adapter before
	 * returning it. If the validation fails, will throw a {@link ConnectionAdapterException} with the given operator,
	 * connection information's name and type, as well as the {@link ValidationResult}.
	 *
	 * @param connection
	 * 		the connection; must not be {@code null}
	 * @param operator
	 * 		the operator for context; may be {@code null}
	 * @return the adapter, never {@code null}
	 * @throws ConnectionAdapterException
	 * 		if a {@link #validate(Function, Function) validation} on the injected values fails
	 * @throws ConfigurationException
	 * 		if an error occurs
	 * @see ValueProviderHandlerRegistry#injectValues(ConnectionInformation, Operator, boolean)
	 * @see #create(String, Map)
	 */
	public T getAdapter(ConnectionInformation connection, Operator operator) throws ConnectionAdapterException, ConfigurationException {
		ConnectionConfiguration configuration = ValidationUtil.requireNonNull(connection, "connection").getConfiguration();
		Map<String, ConfigurationParameter> keyMap = configuration.getKeyMap();

		Map<String, String> valueMap = ValueProviderHandlerRegistry.getInstance().injectValues(connection, operator, false);
		ValidationResult validation = validate(valueMap::get, keyMap::get);
		if (validation.getType() == ResultType.FAILURE) {
			throw new ConnectionAdapterException(operator, configuration, validation);
		}
		// remove all null values and get rid of group prefix
		Map<String, String> adapterMap = valueMap.entrySet().stream().filter(e ->  e.getValue() != null)
				.collect(Collectors.toMap(e -> e.getKey().substring(e.getKey().indexOf('.') + 1), Entry::getValue));

		return create(configuration.getName(), adapterMap);
	}

	/**
	 * Returns a map of group keys to parameter keys. The combined keys of all groups have to contain
	 * all keys that can be derived from {@link #getParameterTypes(ParameterHandler)}.
	 *
	 * @return a singleton map, bundling all parameters in the {@value #BASE_GROUP} group by default
	 */
	public Map<String, List<String>> parametersByGroup() {
		return Collections.singletonMap(BASE_GROUP, getParameterTypes(null)
				.stream().map(ParameterType::getKey).collect(Collectors.toList()));
	}

	/**
	 * Validates the given {@link ConnectionInformation} based on the {@link ParameterType ParameterTypes}
	 * retrieved from {@link #getParameterTypes(ParameterHandler)}.
	 * <p>
	 * Parameters that are not optional according to all parameters set will be checked if they are set.
	 * If they are not set, that parameter is reported as {@link ValidationResult#I18N_KEY_VALUE_MISSING missing}.
	 * A parameter is defined as set if a) its value is not null or b) it is
	 * {@link ConfigurationParameter#isInjected() injected}
	 *
	 * @see #validate(Function, Function)
	 */
	@Override
	public ValidationResult validate(ConnectionInformation information) {
		if (information == null) {
			return ValidationResult.nullable();
		}
		Map<String, ConfigurationParameter> keyMap = information.getConfiguration().getKeyMap();
		Function<String, ConfigurationParameter> configParameters = keyMap::get;
		return validate(configParameters.andThen(p -> p.isInjected() ? p.getInjectorName() : p.getValue()), configParameters);
	}

	/**
	 * Tests if the given object is configured correctly. This might fail if injection is involved, since some
	 * {@link com.rapidminer.connection.valueprovider.ValueProvider ValueProviders} might require an
	 * {@link Operator Operator} for context. Relies on {@link #validate(Function, Function)} and
	 * {@link ConnectionAdapter#getTestAction()}.
	 *
	 * @see ValueProviderHandlerRegistry#injectValues(ConnectionInformation, Operator, boolean)
	 */
	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
		if (testContext == null || testContext.getSubject() == null) {
			return TestResult.nullable();
		}
		ConnectionInformation connection = testContext.getSubject();
		ConnectionConfiguration configuration = connection.getConfiguration();
		T adapter;
		try {
			adapter = getAdapter(connection, null);
		} catch (ConfigurationException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.adapter.conversion_failed",
					new Object[] {configuration.getName(), getType(), e.getLocalizedMessage()});
			return TestResult.failure(TestResult.I18N_KEY_FAILED, e.getLocalizedMessage());
		} catch (ConnectionAdapterException e) {
			return TestResult.failure(TestResult.I18N_KEY_INJECTION_FAILURE,
					e.getValidation().getParameterErrorMessages(), e.getLocalizedMessage());
		}

		TestConfigurableAction testAction = adapter.getTestAction();
		if (testAction == null) {
			return new TestResult(ResultType.NOT_SUPPORTED, TestResult.I18N_KEY_NOT_IMPLEMENTED, null);
		}
		ActionResult testResult = testAction.doWork();
		switch (testResult.getResult()) {
			case SUCCESS:
				return TestResult.success(TestResult.I18N_KEY_SUCCESS);
			case FAILURE:
				String errorMessage = testResult.getMessage();
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.adapter.connection_failed",
						new Object[] {configuration.getName(), getType(), errorMessage});
				return TestResult.failure(TestResult.I18N_KEY_FAILED, errorMessage);
			case NONE:
			default:
				return TestResult.nullable();
		}
	}

	/**
	 * Validates the given actual parameters using both the {@link ParameterType ParameterTypes} retrieved from
	 * {@link #getParameterTypes(ParameterHandler)} as well as the provided
	 * {@link ConfigurationParameter ConfigurationParameters}. All parameter keys that can be retrieved through
	 * {@link #parametersByGroup()} will be tested.
	 * <p>
	 * Parameters that are not optional according to {@link ParameterType#isOptional()} with the given values
	 * will be checked as follows:
	 * <ul>
	 * 		<li>Parameters that have no {@link ConfigurationParameter} or are not set according to
	 * 		{@link ValidationUtil#isValueSet(ConfigurationParameter)} are marked as
	 * 		{@value ValidationResult#I18N_KEY_VALUE_MISSING}</li>
	 * 		<li>Parameters that pass the validation util, are <strong>not</strong>, but still {@code null},
	 * 		are marked as {@value ValidationResult#I18N_KEY_VALUE_MISSING_PLACEHOLDER}</li>
	 * 		<li>Parameters that pass the validation util, are {@link ConfigurationParameter#isInjected() injected},
	 * 		but still {@code null}, are marked as {@value ValidationResult#I18N_KEY_VALUE_NOT_INJECTABLE}</li>
	 * </ul>
	 *
	 * If either of the above is true for any amount of parameters, a {@link ValidationResult} of type
	 * {@link ResultType#FAILURE} will be returned. Otherwise will return one of type {@link ResultType#SUCCESS}.
	 *
	 * @param parameterValues
	 * 		a function that maps full parameter keys to their respective values; must not be {@code null}
	 * @param configParameters
	 * 		a function that maps full parameter keys to their respective {@link ConfigurationParameter};
	 * 		must not be {@code null}
	 * @return a {@link ValidationResult} of either success or failure,
	 * 		never {@code null} or of type {@link ResultType#NONE}
	 */
	protected ValidationResult validate(Function<String, String> parameterValues, Function<String, ConfigurationParameter> configParameters) {
		// set up parameter handler with the given values
		ParameterHandler paramHandler = new SimpleListBasedParameterHandler() {
			@Override
			public List<ParameterType> getParameterTypes() {
				return ConnectionAdapterHandler.this.getParameterTypes(this);
			}
		};
		// this would potentially override values if there are parameters with the same name in different groups
		// configurables by default do not have this problem, since their parameter types are all put together and
		// should not have duplicate keys in the first place
		parametersByGroup().forEach((group, names) -> names.forEach(name ->
			paramHandler.setParameter(name, parameterValues.apply(group + '.' + name))));
		Parameters parameters = paramHandler.getParameters();

		// go through parameters and collect errors
		Map<String, String> errorMap = new HashMap<>();
		parametersByGroup().forEach((group, names) -> names.forEach(name -> {
			ParameterType parameterType = parameters.getParameterType(name);
			// ignore irrelevant parameters
			if (parameterType == null || parameterType.isOptional() || parameterType.isHidden()) {
				return;
			}
			String fullKey = group + '.' + name;
			ConfigurationParameter parameter = configParameters.apply(fullKey);
			// parameter does not exist in connection or is neither set nor injected
			if (parameter == null || !ValidationUtil.isValueSet(parameter)) {
				errorMap.put(fullKey, ValidationResult.I18N_KEY_VALUE_MISSING);
				return;
			}

			if (parameters.getParameterOrNull(name) != null) {
				return;
			}

			String errorString;
			if (parameter.isInjected()) {
				// parameter is marked as injected but has no value
				errorString = ValidationResult.I18N_KEY_VALUE_NOT_INJECTABLE;
			} else {
				// parameter has placeholders that resolve to null
				errorString = ValidationResult.I18N_KEY_VALUE_MISSING_PLACEHOLDER;
			}
			errorMap.put(fullKey, errorString);
		}));

		if (!errorMap.isEmpty()) {
			return ValidationResult.failure(ValidationResult.I18N_KEY_FAILURE, errorMap);
		}
		return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
	}

	/**
	 * Validates the given {@link ConnectionInformation} based on the {@link ParameterType ParameterTypes}
	 * retrieved from {@link #getParameterTypes(ParameterHandler)} and the given predicate.
	 * <p>
	 * Parameters that are not optional according to all parameters set will be checked with the predicate.
	 * If they fail the predicate, that parameter is reported with the provided {@code failureKey}.
	 *
	 * @param information
	 * 		the connection
	 * @param validation
	 * 		the predicate to determine if a {@link ConfigurationParameter} passes or not
	 * @param failureKey
	 * 		the failure key to use if a parameter fails the validation
	 * @see ConnectionHandler#validate(Object) ConnectionHandler.validate
	 */
	protected ValidationResult validate(ConnectionInformation information,
										Predicate<ConfigurationParameter> validation, String failureKey) {
		if (information == null || information.getConfiguration() == null) {
			return ValidationResult.nullable();
		}
		ParameterHandler paramHandler = new SimpleListBasedParameterHandler() {
			@Override
			public List<ParameterType> getParameterTypes() {
				return ConnectionAdapterHandler.this.getParameterTypes(this);
			}
		};
		Map<String, List<String>> parametersByGroup = parametersByGroup();
		ConnectionConfiguration configuration = information.getConfiguration();
		parametersByGroup.forEach((group, names) -> names.forEach(name ->
				paramHandler.setParameter(name, configuration.getValue(group + '.' + name))));

		Parameters parameters = paramHandler.getParameters();
		Map<String, String> errorMap = new HashMap<>();
		parametersByGroup.forEach((group, names) -> names.forEach(name -> {
			if (parameters.getParameterType(name).isOptional()) {
				return;
			}
			String fullKey = group + '.' + name;
			ConfigurationParameter parameter = configuration.getParameter(fullKey);
			if (!validation.test(parameter)) {
				errorMap.put(fullKey, failureKey);
			}
		}));
		if (!errorMap.isEmpty()) {
			return ValidationResult.failure(ValidationResult.I18N_KEY_FAILURE, errorMap);
		}
		return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
	}
	//endregion

	//region static methods to register and retrieve a handler
	/**
	 * Registers the given {@link ConnectionAdapterHandler} and makes it available through {@link #getHandler(String)}.
	 * Registers the handler with the {@link ConnectionHandlerRegistry} as well as the {@link ConfigurationManager}.
	 *
	 * @param handler
	 * 		the handler; must not be {@code null} or have an empty {@link #getTypeId() type}
	 * @param <T>
	 * 		the subtype of {@link ConnectionAdapter} that the {@link ConnectionAdapterHandler} can create
	 * @see ConnectionHandlerRegistry#registerHandler(GenericHandler)
	 */
	public static synchronized <T extends ConnectionAdapter> void registerHandler(ConnectionAdapterHandler<T> handler) {
		String typeId = ValidationUtil.requireNonNull(handler, "handler").getTypeId();
		ConnectionAdapterHandler<T> registeredhandler = getHandler(typeId, false);
		if (registeredhandler != null) {
			Level severity = Level.INFO;
			String messageKey = "com.rapidminer.connection.adapter.handler_already_registered";
			if (registeredhandler.getClass() != handler.getClass()) {
				severity = Level.WARNING;
				messageKey += ".mismatch";
			}
			LogService.getRoot().log(severity, messageKey, new Object[]{typeId, registeredhandler.getClass(), handler.getClass()});
			// check classes?
			return;
		}
		handlerMap.put(typeId, handler);
		handler.initialize();
		// this is now centralized here, so when we decide to completely remove this, we can just do it here
		ConfigurationManager.getInstance().register(handler);
		ConnectionHandlerRegistry.getInstance().registerHandler(handler);
	}

	/**
	 * Returns the handler for the given type ID. This supports both the old {@link #getTypeId()}
	 * as well as the new {@link #getType()} style types. Might return {@code null} if no such handler is available.
	 * <p>
	 * For a handler to be available, it must have been registered using {@link #registerHandler(ConnectionAdapterHandler)}.
	 *
	 * @param typeId
	 * 		the type of the handler; must not be {@code null}; is of the form {@code [namespace:]type}
	 * @param <T>
	 * 		the expected subtype of {@link ConnectionAdapter}
	 * @return the registered handler for this type or {@code null}
	 */
	public static synchronized <T extends ConnectionAdapter> ConnectionAdapterHandler<T> getHandler(String typeId) {
		return getHandler(typeId, true);
	}

	/**
	 * Returns a list with the needed {@link ParameterType ParameterTypes} for the given operator to support connections
	 * of the given type. If the operator does not implement {@link ConnectionSelectionProvider} or no handler for
	 * that type was registered, returns a list with only the given parameter.
	 * <p>
	 * Otherwise the returned list contains the following parameters (in that order)
	 * <ol>
	 *     <li>A {@link ParameterTypeStringCategory dropdown} with {@link #CONNECTION_SOURCE_MODES} to choose the old ro new format</li>
	 *     <li>The {@link ParameterTypeConfigurable given parameter}</li>
	 *     <li>The {@link com.rapidminer.parameter.ParameterTypeConnectionLocation new parameter} for {@link ConnectionInformation}</li>
	 * </ol>
	 * The second and third parameters depend on the selection of the first. Furthermore, a {@link ConnectionInformationSelector}
	 * is installed in the operator (if not already present), which also creates a throughput port for connections.
	 * The input port from that selector takes precedence over all other parameters.
	 * <p>
	 * <strong>Note:</strong> If you want to install the {@link ConnectionInformationSelector} yourself, you have to do
	 * so before this call, best in the constructor of your operator. Reasons for doing this might be
	 * <ul>
	 *     <li>No throughput port wanted</li>
	 *     <li>Positioning of throughput ports</li>
	 *     <li>Subclassing the selector</li>
	 * </ul>
	 *
	 * @param operator
	 * 		the operator these parameters will belong too
	 * @param typeId
	 * 		the type ID for the adapter; must be neither {@code null} nor empty
	 * @param oldParameter
	 * 		the parameter for the {@link Configurable}; must not be{@code null}
	 * @return the list of parameters, never {@code null}
	 * @see ConnectionInformationSelector
	 * @see ConnectionSelectionProvider
	 */
	public static List<ParameterType> getConnectionParameters(Operator operator, String typeId, ParameterTypeConfigurable oldParameter) {
		ValidationUtil.requireNonNull(oldParameter, "configurable parameter type");

		List<ParameterType> parameters = new ArrayList<>(3);
		parameters.add(oldParameter);
		// check if there actually is a handler; if not, just return the normal parameter
		ConnectionAdapterHandler handler = getHandler(typeId);
		if (handler == null || !(operator instanceof ConnectionSelectionProvider)) {
			oldParameter.setOptional(false);
			return parameters;
		}
		// dropdown to select between old and new mode; default depends on compatibility level
		ParameterTypeStringCategory connectionSource = new ParameterTypeStringCategory(PARAMETER_CONNECTION_SOURCE,
				"select where to look for connections", CONNECTION_SOURCE_MODES, REPOSITORY_MODE, false) {
			@Override
			public Object getDefaultValue() {
				if (operator.getCompatibilityLevel().isAtMost(ConnectionHandlerRegistry.BEFORE_NEW_CONNECTION_MANAGEMENT)) {
					return PREDEFINED_MODE;
				}
				return super.getDefaultValue();
			}
		};
		connectionSource.setExpert(false);
		parameters.add(0, connectionSource);

		// add condition to old parameter and make optional
		oldParameter.registerDependencyCondition(new EqualStringCondition(operator, PARAMETER_CONNECTION_SOURCE, true, PREDEFINED_MODE));
		oldParameter.setOptional(true);

		// install selector if not present
		ConnectionSelectionProvider provider = (ConnectionSelectionProvider) operator;
		ConnectionInformationSelector cis = provider.getConnectionSelector();
		if (cis == null) {
			cis = new ConnectionInformationSelector(operator, handler.getType());
			provider.setConnectionSelector(cis);
			cis.makeDefaultPortTransformation();
			operator.getTransformer().addRule(() -> Optional.ofNullable(provider.getConnectionSelector())
					.filter(sel -> sel.getInput() == null || !sel.getInput().isConnected())
					.flatMap(suppress(sel -> operator.getParameter(PARAMETER_CONNECTION_SOURCE)).andThen(o -> Optional.of(o == null ? PREDEFINED_MODE : o)))
					.filter(PREDEFINED_MODE::equals).map(s -> new SimpleProcessSetupError(Severity.WARNING, operator.getPortOwner(), "connection.deprecated"))
					.ifPresent(operator::addError));
		}
		if (cis.getInput() != null) {
			connectionSource.registerDependencyCondition(
					new PortConnectedCondition(operator, cis::getInput, false, false));
		}
		// get parameters from selector and add dependency condition
		List<ParameterType> cisTypes = createParameterTypes(cis);
		NonEqualStringCondition repoModeCondition = new NonEqualStringCondition(operator, PARAMETER_CONNECTION_SOURCE, true, PREDEFINED_MODE);
		cisTypes.stream().peek(p -> p.registerDependencyCondition(repoModeCondition)).forEach(parameters::add);

		return parameters;
	}

	/**
	 * Creates a {@link MDTransformationRule} to check the connection for the given operator. The rule can add
	 * {@link SimpleProcessSetupError SimpleProcessSetupErrors} if there is no handler for the specific type or
	 * the selected connection does not fit the wanted type.
	 *
	 * @param operator
	 * 		the operator to add the rule to; must not be {@code null}
	 */
	public static <O extends Operator & ConnectionSelectionProvider> MDTransformationRule createProcessSetupRule(O operator) {
		return ConnectionInformationSelector.makeConnectionCheckTransformation(operator);
	}

	/**
	 * Same as {@link #getAdapter(Operator, String, String, RepositoryAccessor)
	 * getAdapter(operator, oldParameterKey, oldTypeID, operator.getProcess().getRepositoryAccessor())}.
	 * If the operator is not tied to a process, will use null as the accessor.
	 */
	public static <T extends Configurable> T getAdapter(Operator operator, String oldParameterKey, String oldTypeID)
			throws ConfigurationException, UserError {
		Process process = operator.getProcess();
		RepositoryAccessor accessor = process == null ? null : process.getRepositoryAccessor();
		return getAdapter(operator, oldParameterKey, oldTypeID, accessor);
	}

	/**
	 * Looks up the adapter for the given operator. Will resolve where to find it as follows
	 * <ol>
	 *     <li>If the {@link ConnectionInformationSelector#getInput() input port} exists and connected will take
	 *     a {@link ConnectionInformation} from there</li>
	 *     <li>If the {@value #REPOSITORY_MODE} is selected for {@value #PARAMETER_CONNECTION_SOURCE}, uses the parameter
	 *     {@link ConnectionInformationSelector#getParameterKey()} to find the {@link ConnectionInformation} </li>
	 *     <li>If the {@value #PREDEFINED_MODE} is selected for {@value #PARAMETER_CONNECTION_SOURCE}, uses the parameter
	 *     {@code oldParameterKey} to find the {@link ConfigurationManager#lookup(String, String, RepositoryAccessor) adapter}</li>
	 * </ol>
	 *
	 * @param operator
	 * 		the operator to check on; must not be {@code null}
	 * @param oldParameterKey
	 * 		the old parameter's key
	 * @param oldTypeID
	 * 		the type ID, not fully qualified
	 * @param accessor
	 * 		the repository accessor; can be {@code null}
	 * @param <T>
	 * 		the expected subtype of {@link Configurable}
	 * @return the adapter, never {@code null}
	 * @throws ConnectionAdapterException
	 * 		if a {@link #validate(Function, Function) validation} fails in {@link #getAdapter(ConnectionInformation, Operator)}
	 * @throws UserError
	 * 		if other errors occur
	 * @throws ConfigurationException
	 * 		if an error occurred related to the adapter
	 * @see ConfigurationManager#lookup(String, String, RepositoryAccessor)
	 * @see ConnectionInformationSelector#getConnection()
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Configurable> T getAdapter(Operator operator, String oldParameterKey, String oldTypeID, RepositoryAccessor accessor)
			throws UserError, ConfigurationException {
		String connectionSource;
		if (ValidationUtil.requireNonNull(operator, "operator").isParameterSet(PARAMETER_CONNECTION_SOURCE)) {
			connectionSource = operator.getParameter(PARAMETER_CONNECTION_SOURCE);
		} else {
			connectionSource = null;
		}
		boolean inputConnected = false;
		ConnectionInformationSelector cis = null;
		if (operator instanceof ConnectionSelectionProvider) {
			cis = ((ConnectionSelectionProvider) operator).getConnectionSelector();
			inputConnected = cis != null && cis.getInput() != null && cis.getInput().isConnected();
		}

		if (!inputConnected && (cis == null || connectionSource == null || PREDEFINED_MODE.equals(connectionSource))) {
			String configurableName = operator.getParameter(oldParameterKey);
			T oldConnection = (T) ConfigurationManager.getInstance().lookup(oldTypeID, configurableName, accessor);
			ActionStatisticsCollector.INSTANCE.logOldConnection(operator, oldTypeID);
			operator.logWarning(I18N.getErrorMessage("process.error.connection.deprecated"));
			return oldConnection;
		} else {
			// make sure data is propagated as otherwise each operator would need to do it himself
			cis.passDataThrough();
			ConnectionInformation connection = cis.getConnection();
			ConnectionAdapterHandler<ConnectionAdapter> handler = ConnectionAdapterHandler.getHandler(cis.getConnectionType());
			if (handler == null) {
				throw new UserError(operator, "connection.adapter.no_handler_registered", oldTypeID);
			}
			T newConnection = (T) handler.getAdapter(connection, operator);
			ActionStatisticsCollector.INSTANCE.logNewConnection(operator, connection);
			return newConnection;
		}
	}

	/**
	 * Returns the handler for the given type ID. This supports both the {@link #getTypeId()} as well as
	 * the {@link #getType()} style types. Might return {@code null} if no such handler is available.
	 * <p>
	 * For a handler to be available, it must have been registered using {@link #registerHandler(ConnectionAdapterHandler)}.
	 *
	 * @param <T>
	 * 		the expected subtype of {@link ConnectionAdapter}
	 * @param typeId
	 * 		the type of the handler; must not be {@code null}; is of the form {@code [namespace:]type}
	 * @param logNullHandler
	 * 		whether or not to log when no handler was registered
	 * @return the registered handler for this type or {@code null}
	 */
	@SuppressWarnings("unchecked")
	private static synchronized <T extends ConnectionAdapter> ConnectionAdapterHandler<T> getHandler(String typeId, boolean logNullHandler) {
		int separator = ValidationUtil.requireNonEmptyString(typeId, "type ID").indexOf(':');
		if (separator >= 0) {
			typeId = typeId.substring(separator + 1);
		}
		ConnectionAdapterHandler handler = handlerMap.get(typeId);
		if (handler == null && logNullHandler) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.adapter.no_handler_registered", typeId);
		}
		return (ConnectionAdapterHandler<T>) handler;
	}
	//endregion

}
