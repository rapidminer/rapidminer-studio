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
package com.rapidminer.connection.valueprovider.handler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.TestResult.ResultType;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.tools.ValidationUtil;

/**
 * Simple base implementation for {@link ValueProviderHandler}. Subclasses only have to implement
 * {@link #injectValues(ValueProvider, java.util.Map, com.rapidminer.operator.Operator, com.rapidminer.connection.ConnectionInformation)
 * injectValues(ValueProvider, Map, Operator, ConnectionConfiguration)},
 * but it is advised to also implement the {@link #test(TestExecutionContext)} method.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public abstract class BaseValueProviderHandler implements ValueProviderHandler {

	private static final ObjectWriter writer;
	private static final ObjectReader reader;

	static {
		ObjectMapper mapper = ConnectionInformationSerializer.getRemoteObjectMapper();
		JavaType listOfVPP = mapper.getTypeFactory().constructCollectionType(List.class, ValueProviderParameter.class);
		writer = mapper.writerWithType(listOfVPP);
		reader = mapper.reader(listOfVPP);
	}

	private final List<ValueProviderParameter> parameters;
	private final String type;


	/** Simple handler with no parameters */
	protected BaseValueProviderHandler(String type) {
		this.type = ValidationUtil.requireNonEmptyString(type, "type");
		this.parameters = Collections.emptyList();
	}

	/** Handler with parameters */
	protected BaseValueProviderHandler(String type, List<ValueProviderParameter> parameters) {
		this.type = ValidationUtil.requireNonEmptyString(type, "type");
		this.parameters = ValidationUtil.requireNonEmptyList(parameters, "parameters");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Does nothing by default.
	 */
	@Override
	public void initialize() {
		// noop
	}

	/** @return {@code true} by default */
	@Override
	public boolean isInitialized() {
		return true;
	}

	/** @return {@code true} if parameters is not empty */
	@Override
	public boolean isConfigurable() {
		return !parameters.isEmpty();
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public ValidationResult validate(ValueProvider object) {
		return validate(object, null);
	}

	@Override
	public ValidationResult validate(ValueProvider object, ConnectionInformation information) {
		if (object == null) {
			return ValidationResult.nullable();
		}
		return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
	}

	@Override
	public TestResult test(TestExecutionContext<ValueProvider> object) {
		if (object == null || object.getSubject() == null) {
			return TestResult.nullable();
		}
		return new TestResult(ResultType.NOT_SUPPORTED, TestResult.I18N_KEY_NOT_IMPLEMENTED, null);
	}

	@Override
	public List<ValueProviderParameter> getParameters()	{
		if (parameters.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			// create a deep copy of the parameter list (including potential default values) using Jackson
			// a new value provider will start with such a deep copy, see base implementation of createNewProvider(String)
			return reader.readValue(writer.writeValueAsBytes(parameters));
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public ValueProvider createNewProvider(String name) {
		return new ValueProviderImpl(name, getType(), getParameters());
	}
}
