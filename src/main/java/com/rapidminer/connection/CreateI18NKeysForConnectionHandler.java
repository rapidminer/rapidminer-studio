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
package com.rapidminer.connection;

import static com.rapidminer.connection.util.ConnectionI18N.GROUP_PREFIX;
import static com.rapidminer.connection.util.ConnectionI18N.ICON_SUFFIX;
import static com.rapidminer.connection.util.ConnectionI18N.KEY_DELIMITER;
import static com.rapidminer.connection.util.ConnectionI18N.LABEL_SUFFIX;
import static com.rapidminer.connection.util.ConnectionI18N.PARAMETER_PREFIX;
import static com.rapidminer.connection.util.ConnectionI18N.TIP_SUFFIX;
import static com.rapidminer.connection.util.ConnectionI18N.TYPE_PREFIX;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import com.rapidminer.connection.adapter.ConnectionAdapter;
import com.rapidminer.connection.adapter.ConnectionAdapterHandler;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;


/**
 * Helper class to prepare the {@code GUI{Extension}.properties} file. Provides the following functionality:
 * <ul>
 *     <li>{@link #appendOrReplaceConnectionKeys(Runnable, String, Path, BiFunction) appendOrReplaceConnectionKeys}:
 *     Main method to add missing keys regarding connections and prefilling them where possible</li>
 *     <li>{@link #getDefaultGUIPropertyPath(String)}: Method to get the default path of the
 *     {@code GUI{Extension}.properties} file; resolves to
 *     {@code {project}/src/main/resources/com/rapidminer/resources/i18n/GUI{infix}.properties}
 *     and makes it absolute</li>
 *     <li>{@link #connectionAdaptionHandlerDefaultValues(String)}: Method to get the default values for a
 *     {@link ConnectionAdapterHandler}; this will probably resolve most of the new keys already</li>
 * </ul>
 *
 * @author Jan Czogalla
 * @see com.rapidminer.connection.util.ConnectionI18N ConnectionI18N
 * @since 9.3
 */
public class CreateI18NKeysForConnectionHandler {

	private static final String HEADER_LINE = "####################";
	private static final String CONNECTION_HEADER = "## Connections     #";

	private CreateI18NKeysForConnectionHandler() {}

	/**
	 * Uses the given {@code propertyPath} as a basis to fill in the new connection related keys.
	 * The method works as follows:
	 * <ol>
	 * 		<li>If the {@code initializer} is not {@code null}, run it</li>
	 * 		<li>If the {@code defaultValues} is {@code null}, use a {@link BiFunction} that returns an empty string</li>
	 * 		<li>Find all registered {@link ConnectionHandler ConnectionHandlers} that have {@code namespace} as a prefix;
	 * 		if none are found, return {@code null}</li>
	 * 		<li>For each handler, create all relevant i18n keys for the type, all parameter groups, and each parameter;
	 * 		they will be sorted by group name and the parameters will also be sorted alphabetically inside each group</li>
	 * 		<li>Load the existing properties from {@code propertyPath} and use them together with {@code defaultValues}
	 * 		to initialize all keys where possible</li>
	 * 		<li>Create a backup of {@code propertyPath} and {@link #writeProperties(String, Map, Path, Path) write}
	 * 		all keys to a new path that will then be returned</li>
	 * </ol>
	 *
	 * @param initializer
	 * 		an initializer if necessary; can be {@code null}; can be used to register all relevant
	 *        {@link ConnectionHandler ConnectionHandlers}
	 * @param namespace
	 * 		the namespace of the relevant {@link ConnectionHandler ConnectionHandlers}; must not be {@code null} or empty
	 * @param propertyPath
	 * 		the path to the file to take as a basis; must not be {@code null} and must exist
	 * @param defaultValues
	 * 		a {@link BiFunction} that provides a default value; can be {@code null}
	 * @return the path of the new file with the combined keys from the original file and the (sorted) connection keys
	 * or {@code null} if no relevant handlers were found
	 * @throws IOException
	 * 		if an I/O error occurs
	 */
	public static Path appendOrReplaceConnectionKeys(Runnable initializer, String namespace, Path propertyPath,
													 BiFunction<String, Properties, String> defaultValues) throws IOException {
		if (initializer != null) {
			initializer.run();
		}

		BiFunction<String, Properties, String> defaultValueProvider;
		if (defaultValues == null) {
			defaultValueProvider = (key, props) -> "";
		} else {
			defaultValueProvider = defaultValues;
		}

		ConnectionHandlerRegistry handlerRegistry = ConnectionHandlerRegistry.getInstance();
		List<ConnectionInformation> connections = handlerRegistry.getAllTypes().stream()
				.filter(type -> type.startsWith(namespace + ':'))
				.map(handlerRegistry::getHandler).map(h -> h.createNewConnectionInformation("test"))
				.collect(Collectors.toList());
		if (connections.isEmpty()) {
			return null;
		}

		List<String> typeSuffixes = Arrays.asList(LABEL_SUFFIX, TIP_SUFFIX, ICON_SUFFIX);
		List<String> groupSuffixes = Arrays.asList(LABEL_SUFFIX, TIP_SUFFIX, ICON_SUFFIX);
		List<String> parameterSuffixes = Arrays.asList(LABEL_SUFFIX, TIP_SUFFIX);
		SortedMap<String, SortedMap<String, SortedSet<String>>> connectionI18NProperties = new TreeMap<>();

		// Create all i18n keys for type, groups and parameters
		for (ConnectionInformation connection : connections) {
			ConnectionConfiguration configuration = connection.getConfiguration();
			String type = configuration.getType();
			TreeMap<String, SortedSet<String>> typeKeys = new TreeMap<>();
			// store type keys as first group
			typeKeys.put("", typeSuffixes.stream().map(suffix -> getKeyForType(type, suffix))
					.collect(Collectors.toCollection(TreeSet::new)));

			// collect group keys and parameter keys per group
			configuration.getKeys().forEach(group -> {
				SortedSet<String> groupKeys = new TreeSet<>();
				String groupKey = group.getGroup();
				groupSuffixes.stream().map(suffix -> getKeyForGroup(type, groupKey, suffix)).forEach(groupKeys::add);
				parameterSuffixes.stream().flatMap(suffix -> group.getParameters().stream()
						.map(cp -> getKeyForParameter(type, groupKey, cp.getName(), suffix))).forEach(groupKeys::add);
				if (!groupKey.isEmpty()) {
					typeKeys.put(groupKey, groupKeys);
				}
			});
			if (!typeKeys.isEmpty()) {
				connectionI18NProperties.put(type, typeKeys);
			}
		}

		Properties properties = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(propertyPath)) {
			properties.load(reader);
		}

		// find default values from properties and the given bifunction
		Map<String, List<List<String>>> connectionPropertyValues = new LinkedHashMap<>();
		connectionI18NProperties.forEach((type, typeKeys) -> typeKeys.forEach((group, groupKeys) -> {
			String typeKey = getKeyForType(type, LABEL_SUFFIX);
			String typeName = properties.getProperty(typeKey,
					defaultValueProvider.andThen(n -> n.isEmpty() ? typeKey : n).apply(typeKey, properties));
			List<String> groupKeyValues = groupKeys.stream()
					.map(key -> key + " = " + properties.getProperty(key, defaultValueProvider.apply(key, properties)))
					.collect(Collectors.toList());
			connectionPropertyValues.computeIfAbsent(typeName, x -> new ArrayList<>()).add(groupKeyValues);
		}));

		Path backupPath = Paths.get(propertyPath.toString() + ".bak");
		Files.copy(propertyPath, backupPath, REPLACE_EXISTING);
		Path newPath = Paths.get(propertyPath.toString() + ".new");

		writeProperties(namespace, connectionPropertyValues, backupPath, newPath);
		return newPath;
	}

	/**
	 * Creates and returns an absolute {@link Path} that resolves to
	 * {@code {projectPath}/src/main/resources/com/rapidminer/resources/i18n/GUI{infix}.properties}
	 *
	 * @param infix
	 * 		the infix for the property file name; usually the camel cased namespace of an extension
	 * @return the absolute path
	 */
	public static Path getDefaultGUIPropertyPath(String infix) {
		String propertyFileName = "src/main/resources/com/rapidminer/resources/i18n/GUI" + infix + ".properties";
		return Paths.get(propertyFileName).toAbsolutePath();
	}

	/**
	 * Returns a {@link BiFunction} that can extract default values for {@link ConnectionAdapterHandler} based
	 * connection keys. Will resolve i18n keys as follows; unresolved keys will result in an empty string return value:
	 * <ul>
	 *     <li>Icons and tips for <strong>groups</strong> will not be resolved and just return the empty string</li>
	 *     <li>Labels for <strong>groups</strong> and <strong>parameters</strong> will be capitalized and spaced,
	 *     i.e. {@code parameter_key} will be transformed to "Parameter key"</li>
	 *     <li>Tips for <strong>parameters</strong> will be resolved through {@link ParameterType#getDescription()}
	 *     from the appropriate parameter type obtained from
	 *     {@link ConnectionAdapterHandler#getParameterTypes(ParameterHandler)}; it then is put through the original
	 *     properties to make sure that if description was based on i18n before, it will be resolved properly</li>
	 *     <li>Keys for <strong>types</strong> will be resolved by looking up the values of
	 *     {@link ConnectionAdapterHandler#getIconName()}, {@link ConnectionAdapterHandler#getName()} and
	 *     {@link ConnectionAdapterHandler#getDescription()} in the original properties for the
	 *     icon, label and tip respectively; if the name/label contains a "Connection" suffix, that will be removed</li>
	 * </ul>
	 * <strong>Note:</strong> This method might be removed in the future.
	 *
	 * @param namespace
	 * 		the namespace prefix of all affected handlers
	 * @return the default value function
	 */
	public static BiFunction<String, Properties, String> connectionAdaptionHandlerDefaultValues(String namespace) {
		return (i18nKey, properties) -> connectionAdaptionHandlerDefaultValues(namespace, i18nKey, properties);
	}

	/**
	 * Creates the i18n key for the given connection type and suffix
	 *
	 * @param type
	 * 		the type of the connection whose key should be created
	 * @param suffix
	 * 		the suffix; one of "icon", "label" or "tip"
	 * @return the i18n key
	 */
	private static String getKeyForType(String type, String suffix) {
		return getKeyFor(type, TYPE_PREFIX, suffix);
	}

	/**
	 * Creates the i18n key for the given connection type, group and suffix
	 *
	 * @param type
	 * 		the type of the connection
	 * @param group
	 * 		the group whose key should be created
	 * @param suffix
	 * 		the suffix; one of "icon", "label" or "tip"
	 * @return the i18n key
	 */
	private static String getKeyForGroup(String type, String group, String suffix) {
		return getKeyFor(type, GROUP_PREFIX, group, suffix);
	}

	/**
	 * Creates the i18n key for the given connection type, group, parameter and suffix
	 *
	 * @param type
	 * 		the type of the connection
	 * @param group
	 * 		the group of the parameter
	 * @param parameter
	 * 		the parameter whose key should be created
	 * @param suffix
	 * 		the suffix; one of "label" or "tip"
	 * @return the i18n key
	 */
	private static String getKeyForParameter(String type, String group, String parameter, String suffix) {
		return getKeyFor(type, PARAMETER_PREFIX, group, parameter, suffix);
	}

	/**
	 * Creates the i18n key for the given connection type and other pieces. The first piece is the actual needed prefix
	 * (one of {@value com.rapidminer.connection.util.ConnectionI18N#TYPE_PREFIX},
	 * {@value com.rapidminer.connection.util.ConnectionI18N#GROUP_PREFIX} or
	 * {@value com.rapidminer.connection.util.ConnectionI18N#PARAMETER_PREFIX}), the other pieces describe the infix
	 * (group/parameter and actual name/key) as well as the suffix.
	 *
	 * @param type
	 * 		the type of the connection
	 * @param pieces
	 * 		the different needed pieces
	 * @return the i18n key
	 */
	private static String getKeyFor(String type, String... pieces) {
		type = type.replace(':', '.');
		return String.join(KEY_DELIMITER, (String[]) ArrayUtils.add(pieces, 1, type));
	}

	/**
	 * Writes all properties to the given {@code newPath} after reading in {@code backupPath}. Adds all connection
	 * related keys in one block which might be appended to the end of the file if it did not exist before. Will keep
	 * an existing block in the same space and expand on it. Also will remove all keys that are outside of that block.
	 *
	 * @param namespace
	 * 		the namespace of the affected handlers
	 * @param connectionPropertyValues
	 * 		the key/value pair lines, in blocks and sorted
	 * @param backupPath
	 * 		the original file to expand on
	 * @param newPath
	 * 		the new file to write to
	 * @throws IOException
	 * 		if an I/O error occurs
	 * @see #writeProperties(BufferedWriter, Map)
	 */
	private static void writeProperties(String namespace, Map<String, List<List<String>>> connectionPropertyValues,
										Path backupPath, Path newPath) throws IOException {
		String[] prefixes = Stream.of(TYPE_PREFIX, GROUP_PREFIX, PARAMETER_PREFIX)
				.map(p -> p + '.' + namespace).toArray(String[]::new);
		try (BufferedReader reader = Files.newBufferedReader(backupPath);
			 BufferedWriter writer = Files.newBufferedWriter(newPath, CREATE, TRUNCATE_EXISTING)) {
			int status = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				switch (status) {
					case 0:
						if (StringUtils.startsWithAny(line, prefixes)) {
							// skip connection keys that are not collected in the proper area
							// they will be added again later
							break;
						}
						writer.write(line);
						writer.newLine();
						if (line.equals(CONNECTION_HEADER)) {
							status = 1;
						}
						break;
					case 1:
						writer.write(line);
						writer.newLine();
						writeProperties(writer, connectionPropertyValues);
						while ((line = reader.readLine()) != null && !line.startsWith("##")) {
							//skip existing connection entries
						}
						if (line != null) {
							writer.newLine();
							writer.write(line);
							writer.newLine();
						}
						status = 2;
						break;
					case 2:
						writer.write(line);
						writer.newLine();
						break;
					default:
						break;
				}
			}
			if (status == 0) {
				// no connections so far, append it all
				writer.newLine();
				writer.write(HEADER_LINE);
				writer.newLine();
				writer.write(CONNECTION_HEADER);
				writer.newLine();
				writer.write(HEADER_LINE);
				writer.newLine();
				writeProperties(writer, connectionPropertyValues);
			}
		}
	}

	/**
	 * Writes the actual connection i18n keys to the file as one block. Each type has a leading comment in the form of
	 * {@code # {type name or type key}}, followed by blocks of the type and its groups.
	 *
	 * @param writer
	 * 		the file writer
	 * @param connectionPropertyValues
	 * 		the i18n keys
	 * @throws IOException
	 * 		if an I/O error occurs
	 */
	private static void writeProperties(BufferedWriter writer, Map<String, List<List<String>>> connectionPropertyValues) throws IOException {
		for (Entry<String, List<List<String>>> entry : connectionPropertyValues.entrySet()) {
			writer.newLine();
			writer.write("# " + entry.getKey());
			for (List<String> groups : entry.getValue()) {
				writer.newLine();
				for (String group : groups) {
					writer.write(group);
					writer.newLine();
				}
			}
		}
	}

	/**
	 * Returns a {@link BiFunction} that can extract default values for {@link ConnectionAdapterHandler} based
	 * connection keys. Will resolve i18n keys as follows; unresolved keys will result in an empty string return value:
	 * <ul>
	 *     <li>Icons and tips for <strong>groups</strong> will not be resolved and just return the empty string</li>
	 *     <li>Labels for <strong>groups</strong> and <strong>parameters</strong> will be capitalized and spaced,
	 *     i.e. {@code parameter_key} will be transformed to "Parameter key"</li>
	 *     <li>Tips for <strong>parameters</strong> will be resolved through {@link ParameterType#getDescription()}
	 *     from the appropriate parameter type obtained from
	 *     {@link ConnectionAdapterHandler#getParameterTypes(ParameterHandler)}; it then is put through the original
	 *     properties to make sure that if description was based on i18n before, it will be resolved properly</li>
	 *     <li>Keys for <strong>types</strong> will be resolved by looking up the values of
	 *     {@link ConnectionAdapterHandler#getIconName()}, {@link ConnectionAdapterHandler#getName()} and
	 *     {@link ConnectionAdapterHandler#getDescription()} in the original properties for the
	 *     icon, label and tip respectively; if the name/label contains a "Connection" suffix, that will be removed</li>
	 * </ul>
	 *
	 * @param namespace
	 * 		the namespace prefix of all affected handlers
	 * @param i18nKey
	 * 		the i18n key to be resolved
	 * @param properties
	 * 		the original properties
	 * @return the default value
	 */
	private static String connectionAdaptionHandlerDefaultValues(String namespace, String i18nKey, Properties properties) {
		if (i18nKey.startsWith(GROUP_PREFIX)) {
			if (i18nKey.endsWith(LABEL_SUFFIX)) {
				return capitalizeName(i18nKey);
			}
			return "";
		}
		if (i18nKey.startsWith(PARAMETER_PREFIX)) {
			if (i18nKey.endsWith(LABEL_SUFFIX)) {
				return capitalizeName(i18nKey);
			}
			if (i18nKey.endsWith(TIP_SUFFIX)) {
				String type = extractType(i18nKey, namespace);
				ConnectionAdapterHandler<ConnectionAdapter> handler = ConnectionAdapterHandler.getHandler(type);
				if (handler == null) {
					return "";
				}
				String paramKey = extractName(i18nKey);
				return handler.getParameterTypes(null).stream()
						.filter(pt -> pt.getKey().equals(paramKey)).findFirst()
						.map(ParameterType::getDescription).map(desc -> properties.getProperty(desc, desc))
						.orElse("");
			}
		}
		if (i18nKey.startsWith(TYPE_PREFIX)) {
			ConnectionAdapterHandler<ConnectionAdapter> handler = ConnectionAdapterHandler.getHandler(extractType(i18nKey, namespace));
			if (handler == null) {
				return "";
			}
			if (i18nKey.endsWith(LABEL_SUFFIX)) {
				return properties.getProperty(handler.getName(), "").replace(" Connection", "");
			} else if (i18nKey.endsWith(ICON_SUFFIX)) {
				return properties.getProperty(handler.getIconName(), "");
			} else if (i18nKey.endsWith(TIP_SUFFIX)) {
				return properties.getProperty(handler.getDescription(), "");
			}
		}
		return "";
	}

	/**
	 * Turns a parameter key into a capitalized version to be used as a name,
	 * e.g. {@code parameter_key} is turned into "Parameter key"
	 *
	 * @param i18nKey
	 * 		the i18n key of the parameter
	 * @return the capitalized name
	 */
	private static String capitalizeName(String i18nKey) {
		return WordUtils.capitalize(extractName(i18nKey).replace('_', ' '));
	}

	/**
	 * Extracts the name part from the given i18n key; this is defined as the second to last segment
	 * when splitting by '.'.
	 *
	 * @param i18nKey
	 * 		the i18n key
	 * @return the actual name part
	 */
	private static String extractName(String i18nKey) {
		int end = i18nKey.lastIndexOf('.');
		int start = i18nKey.lastIndexOf('.', end - 1);
		return i18nKey.substring(start + 1, end);
	}

	/**
	 * Extracts the type part from the given i18n key; this is defined as the segment after {@code namespace}
	 * when splitting by '.'.
	 *
	 * @param i18nKey
	 * 		the i18n key
	 * @return the actual type part
	 */
	private static String extractType(String i18nKey, String namespace) {
		int namespacePos = i18nKey.indexOf(namespace);
		if (namespacePos == -1) {
			return "";
		}
		int start = namespacePos + namespace.length() + 1;
		return i18nKey.substring(start, i18nKey.indexOf('.', start));
	}
}
