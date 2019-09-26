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
package com.rapidminer.connection.gui.model;


import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConfigurationParameterGroup;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.configuration.PlaceholderParameter;
import com.rapidminer.connection.configuration.PlaceholderParameterImpl;
import com.rapidminer.connection.util.GenericHandlerRegistry;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;


/**
 * Utility class to convert between {@link ConnectionModel}s and {@link ConnectionInformation} objects.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public final class ConnectionModelConverter {

	/**
	 * Prevent utility class instantiation.
	 */
	private ConnectionModelConverter() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Creates a ConnectionModel from a ConnectionInformation
	 *
	 * @param connection
	 * 		the {@link ConnectionInformation} object
	 * @param location
	 * 		the {@link RepositoryLocation} of the connection
	 * @param editable
	 *        {@code true} if the connection is editable
	 * @return a connection model containing the information of the connection
	 */
	public static ConnectionModel fromConnection(ConnectionInformation connection, RepositoryLocation location, boolean editable) {
		List<ValueProviderModel> valueProviderModels = connection.getConfiguration().getValueProviders().stream().map(ValueProviderModelConverter::toModel).collect(Collectors.toList());
		ConnectionModel conn = new ConnectionModel(connection, location, editable, valueProviderModels);
		conn.setDescription(connection.getConfiguration().getDescription());
		conn.setTags(new ArrayList<>(connection.getConfiguration().getTags()));
		// use new (empty) connection to capture all potential new parameters
		ConnectionInformation newConnection = null;
		try {
			ConnectionHandler handler = ConnectionHandlerRegistry.getInstance().getHandler(connection.getConfiguration().getType());
			newConnection = handler.createNewConnectionInformation("emptyCI");
		} catch (GenericHandlerRegistry.MissingHandlerException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.gui.model.ConnectionModelConverter.creating_empty_connection_failed", connection.getConfiguration().getType());
		}
		if (newConnection != null) {
			addParameters(newConnection, conn);
		}
		// now overwrite with values of existing connection
		addParameters(connection, conn);
		for (PlaceholderParameter p : connection.getConfiguration().getPlaceholders()) {
			conn.addOrSetPlaceholder(p.getGroup(), p.getName(), p.getValue(), p.isEncrypted(), p.getInjectorName(), p.isEnabled());
		}
		conn.setLibraryFiles(connection.getLibraryFiles());
		conn.setOtherFiles(connection.getOtherFiles());
		return conn;
	}

	/**
	 * Applies the connection model on the connection
	 *
	 * @param connection
	 * 		the connection
	 * @param model
	 * 		the connection model
	 * @return a new connection with the updated information
	 */
	public static ConnectionInformation applyConnectionModel(ConnectionInformation connection, ConnectionModel model) {
		ConnectionConfigurationBuilder config;
		try {
			// We have to clone to keep the id field
			config = new ConnectionConfigurationBuilder(connection.getConfiguration());
		} catch (IOException e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.gui.model.ConnectionModelConverter.cloning_connection_configuration_failed", e);
			config = new ConnectionConfigurationBuilder(model.getType(), model.getName());
		}
		config.withKeys(toMap(model.getParameterGroups()));
		List<ValueProvider> valueProviders = model.valueProvidersProperty().stream()
				.filter(vp -> model.getParameterGroups().stream()
						.anyMatch(group -> group.getParameters().stream()
								.anyMatch(param -> vp.getName().equals(param.getInjectorName())))).map(ValueProviderModelConverter::toValueProvider)
				.collect(Collectors.toList());
		config.withValueProviders(valueProviders);
		config.withTags(new ArrayList<>(model.getTags()));
		config.withDescription(model.getDescription());
		config.withPlaceholders(toList(model.getPlaceholders()));

		ConnectionInformationBuilder connectionBuilder;
		try {
			connectionBuilder = new ConnectionInformationBuilder(connection).updateConnectionConfiguration(config.build());
		} catch (IOException e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.gui.model.ConnectionModelConverter.cloning_connection_failed", e);
			connectionBuilder = new ConnectionInformationBuilder(config.build())
					.withLibraryFiles(connection.getLibraryFiles())
					.withOtherFiles(connection.getOtherFiles())
					.withAnnotations(connection.getAnnotations())
					.withStatistics(connection.getStatistics());
		}

		try {
			connectionBuilder.withOtherFiles(model.getOtherFiles());
		} catch (IllegalArgumentException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.gui.model.ConnectionModelConverter.removed_nonexisting_paths", e);
			model.getOtherFiles().removeIf(Files::notExists);
			connectionBuilder.withOtherFiles(model.getOtherFiles());
		}

		try {
			connectionBuilder.withLibraryFiles(model.getLibraryFiles());
		} catch (IllegalArgumentException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.gui.model.ConnectionModelConverter.removed_nonexisting_paths", e);
			model.getLibraryFiles().removeIf(Files::notExists);
			connectionBuilder.withLibraryFiles(model.getLibraryFiles());
		}

		ConnectionInformation result = connectionBuilder.build();
		// We compare with the object that contains the updated statistics, since they don't modify the connection
		if (!result.equals(connection)) {
			result.getStatistics().updateChange();
		}

		return result;
	}

	/**
	 * Returns a temporary configuration object for the parameter
	 *
	 * @param parameter
	 * 		the parameter
	 * @return a temporary configuration object
	 */
	public static ConnectionInformation getConnection(ConnectionParameterModel parameter) {
		return parameter.getConnection().asConnectionInformation();
	}

	/**
	 * Returns a temporary configuration object for the model.
	 *
	 * @param model
	 * 		the connection model
	 * @return a temporary configuration object
	 */
	public static ConnectionInformation getConnection(ConnectionModel model) {
		return model.asConnectionInformation();
	}

	/**
	 * Converts a list of group models into a Map of group name to configuration parameter
	 *
	 * @param groupModels
	 * 		the list of parameter group models
	 * @return a map of group name to list of configuration parameter
	 */
	static Map<String, List<ConfigurationParameter>> toMap(List<ConnectionParameterGroupModel> groupModels) {
		LinkedHashMap<String, List<ConfigurationParameter>> result = new LinkedHashMap<>();
		for (ConnectionParameterGroupModel group : groupModels) {
			String groupName = group.getName();
			List<ConfigurationParameter> parameters = new ArrayList<>();
			for (ConnectionParameterModel parameter : group.getParameters()) {
				if (StringUtils.trimToNull(parameter.getName()) != null) {
					parameters.add(new ConfigurationParameterImpl(parameter.getName(), parameter.getValue(), parameter.isEncrypted(), parameter.getInjectorName(), parameter.isEnabled()));
				}
			}
			//We can't add empty groups
			if (!parameters.isEmpty()) {
				result.put(groupName, parameters);
			}
		}
		return result;
	}

	/**
	 * Converts a list of PlaceholderParameterModel into a list of AdvancedConfigurationParameter
	 *
	 * @param groupModels
	 * 		the parameter group models
	 * @return advanced configuration parameter
	 */
	static List<PlaceholderParameter> toList(List<PlaceholderParameterModel> groupModels) {
		List<PlaceholderParameter> result = new ArrayList<>();
		for (PlaceholderParameterModel parameter : groupModels) {
			result.add(new PlaceholderParameterImpl(parameter.getName(), parameter.getValue(), parameter.getGroupName(), parameter.isEncrypted(), parameter.getInjectorName(), parameter.isEnabled()));
		}
		return result;
	}

	/**
	 * Adds all {@link ConfigurationParameter} of the given CI to the given model.
	 *
	 * @param connection
	 * 		the CI
	 * @param model
	 * 		the model
	 */
	private static void addParameters(ConnectionInformation connection, ConnectionModel model) {
		for (ConfigurationParameterGroup group : connection.getConfiguration().getKeys()) {
			ConnectionParameterGroupModel groupModel = model.getOrCreateParameterGroup(group.getGroup());
			for (ConfigurationParameter p : group.getParameters()) {
				groupModel.addOrSetParameter(p.getName(), p.getValue(), p.isEncrypted(), p.getInjectorName(), p.isEnabled());
			}
		}
	}
}
