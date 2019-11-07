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
package com.rapidminer.tools.plugin;

import java.awt.Frame;
import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.Policy;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.RapidMinerVersion;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.properties.SettingsItem;
import com.rapidminer.gui.properties.SettingsItems;
import com.rapidminer.gui.properties.SettingsXmlHandler;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.safemode.SafeMode;
import com.rapidminer.gui.tools.SplashScreen;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.dialogs.AboutBox;
import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.I18N.SettingsType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.ResourceSource;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * <p>
 * The class for RapidMiner plugins. This class is used to encapsulate the .jar file which must be
 * in the <code>lib/plugins</code> subdirectory of RapidMiner. Provides methods for plugin checks,
 * operator registering, and getting information about the plugin.
 * </p>
 * <p>
 * Plugin dependencies must be defined in the form <br />
 * plugin_name1 (plugin_version1) # ... # plugin_nameM (plugin_versionM) < /br> of the manifest
 * parameter <code>Plugin-Dependencies</code>. You must define both the name and the version of the
 * desired plugins and separate them with &quot;#&quot;.
 * </p>
 *
 * @author Simon Fischer, Ingo Mierswa, Nils Woehler, Adrian Wilke
 */
public class Plugin {

	/**
	 * The name for the manifest entry RapidMiner-Type which can be used to indicate that a jar file
	 * is a RapidMiner plugin.
	 */
	public static final String RAPIDMINER_TYPE = "RapidMiner-Type";

	/**
	 * The value for the manifest entry RapidMiner-Type which indicates that a jar file is a
	 * RapidMiner plugin.
	 */
	public static final String RAPIDMINER_TYPE_PLUGIN = "RapidMiner_Extension";

	/** @since 9.0.0 */
	public static final String PROPERTY_PLUGINS_WHITELIST = "rapidminer.extensions.whitelist";
	/** @since 9.0.0 */
	public static final String WHITELIST_NONE = "none";
	/** @since 9.0.0 */
	public static final String WHITELIST_SHIPPED = "shipped";
	/** @since 9.0.0 */
	private static final String PACKAGED_IDS = "advanced_file_connectors,concurrency,jdbc_connectors,legacy,productivity,professional,remote_repository,blending,utility,browser,html5_charts";
	/** @since 9.0.0 */
	private static final Set<String> PACKAGED_EXTENSIONS;
	/** @since 9.0.0 */
	private static final String SHIPPED_IDS = "model_simulator,process_scheduling,social_media,cloud_connectivity,h2o,dataeditor,model_deployment_management,operator_recommender,time_series";
	/** @since 9.0.0 */
	private static final Set<String> SHIPPED_EXTENSIONS;
	private static final String PACKAGEID_RAPIDMINER = "rapidminer-studio-6";
	static {
		String rmxPrefix = "rmx_";
		Set<String> packagedExtensions = Arrays.stream(PACKAGED_IDS.split(",")).map(rmxPrefix::concat).collect(Collectors.toSet());
		Set<String> shippedExtensions = Arrays.stream(SHIPPED_IDS.split(",")).map(rmxPrefix::concat).collect(Collectors.toCollection(HashSet::new));
		shippedExtensions.addAll(packagedExtensions);
		PACKAGED_EXTENSIONS = Collections.unmodifiableSet(packagedExtensions);
		SHIPPED_EXTENSIONS = Collections.unmodifiableSet(shippedExtensions);
	}

	private static final ClassLoader MAJOR_CLASS_LOADER;

	static {
		try {
			MAJOR_CLASS_LOADER = AccessController.doPrivileged((PrivilegedExceptionAction<ClassLoader>) AllPluginsClassLoader::new);
		} catch (PrivilegedActionException e) {
			throw new RuntimeException("Cannot create major class loader: " + e.getMessage(), e);
		}
	}

	/** The folder where bundled server extensions are stored at */
	private static Set<String> additionalExtensionDirs = new HashSet<>();

	/**
	 * The jar archive of the plugin which must be placed in the <code>lib/plugins</code>
	 * subdirectory of RapidMiner.
	 */
	private final JarFile archive;

	/** The file for this plugin. */
	private final File file;

	/** The resource source for this plugin */
	private ResourceSource resourceSource;

	/** The class loader based on the plugin file. */
	private PluginClassLoader classLoader;

	/** The name of the plugin. */
	private String name;

	/** The version of the plugin. */
	private String version;

	/** The vendor of the plugin. */
	private String vendor;

	/** The url for this plugin (in WWW). */
	private String url;

	/** The RapidMiner version which is needed for this plugin. */
	private String requiredRapidMinerVersion = "0.0.000";

	/** The plugins and their versions which are needed for this plugin. */
	private final List<Dependency> pluginDependencies = new LinkedList<>();

	private String extensionId;

	private String pluginInitClassName;

	private String pluginResourceObjects;

	private String pluginResourceOperators;

	private String pluginParseRules;

	private String pluginGroupDescriptions;

	private String pluginErrorDescriptions;

	private String pluginUserErrorDescriptions;

	private String pluginGUIDescriptions;

	private String pluginSettingsDescriptions;

	private String pluginSettingsStructure;

	private String prefix;

	private boolean disabled = false;

	private ResourceBundle settingsRessourceBundle;

	private Boolean useExtensionTreeRoot = null;

	private static final Comparator<Plugin> PLUGIN_COMPARATOR = (p1, p2) -> {
		if (p1 == null && p2 == null) {
			return 0;
		}

		if (p1 == null || p2 == null) {
			return p1 == null ? 1 : -1;
		}

		if (p1.getName() == null && p2.getName() == null) {
			return 0;
		}

		if (p1.getName() == null || p2.getName() == null) {
			return p1.getName() == null ? 1 : -1;
		}

		// if both plugins have the same name then check the version
		if (p1.getName().equals(p2.getName())) {
			if (p1.getVersion() == null && p2.getVersion() == null) {
				return 0;
			}
			if (p1.getVersion() == null || p2.getVersion() == null) {
				return p1.getVersion() == null ? 1 : -1;
			}

			return p1.getVersion().compareTo(p2.getVersion());
		}

		return p1.getName().compareTo(p2.getName());
	};

	/**
	 * An ordered collection of plugins sorted by the dependency order. IMPORTANT: This collection
	 * does not respect failures during initialization, so it might contain more plugins than
	 * {@link #ALL_PLUGINS}.
	 */
	private static final Collection<Plugin> PLUGIN_INITIALIZATION_ORDER = new ArrayList<>();

	/** An ordered set of all plugins sorted lexically based on the plugin name. */
	private static final Collection<Plugin> ALL_PLUGINS = new TreeSet<>(PLUGIN_COMPARATOR);

	/** Set of plugins that failed to load. */
	private static final Set<Plugin> INCOMPATIBLE_PLUGINS = new HashSet<>();

	/**
	 * The map of blacklisted plugins that should not be loaded. The key is the extension id. The
	 * value can be {@code null} or a pair of version numbers. The pair of version numbers specifies
	 * the range of forbidden version numbers [from - up to]. These version numbers can be
	 * {@code null} indicating no upper or lower bound.
	 */
	private static final Map<String, Pair<VersionNumber, VersionNumber>> PLUGIN_BLACKLIST = new HashMap<>();

	static {
		// incompatible initialization code
		PLUGIN_BLACKLIST.put("rmx_parallel", null);

		// every version smaller or equal 7.1.1
		final Pair<VersionNumber, VersionNumber> upToRm711 = new Pair<>(null, new VersionNumber(7, 1, 1));

		// bundled extensions using the old license schema
		PLUGIN_BLACKLIST.put("rmx_advanced_file_connectors", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_jdbc_connectors", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_legacy", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_productivity", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_remote_repository", upToRm711);

		// packaged extensions using the old license schema
		PLUGIN_BLACKLIST.put("rmx_data_editor", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_process_scheduling", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_social_media", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_cloud_connectivity", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_cloud_execution", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_operator_recommender", upToRm711);

		// non-packaged extensions using the old license schema
		PLUGIN_BLACKLIST.put("rmx_mozenda", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_nosql", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_pmml", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_qlik", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_solr", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_splunk", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_tableau_table_writer", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_text", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_web", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_r_scripting", upToRm711);
		PLUGIN_BLACKLIST.put("rmx_python_scripting", upToRm711);

		// Radoop must be at least version 8.1.0 due to certain features being broken in older Radoop versions due to Server 8.x architectural changes
		PLUGIN_BLACKLIST.put("rmx_radoop", new Pair<>(null, new VersionNumber(8, 0, 99)));

		// RapidLabs / 3rd party extensions causing problems since Studio 7.2
		PLUGIN_BLACKLIST.put("rmx_rapidprom", new Pair<>(null, new VersionNumber(3, 0, 7)));
		// yes the rmx_rmx_ prefix is correct...
		PLUGIN_BLACKLIST.put("rmx_rmx_toolkit", new Pair<>(null, new VersionNumber(1, 0, 0)));
		PLUGIN_BLACKLIST.put("rmx_ida", new Pair<>(null, new VersionNumber(5, 1, 0)));

		// Block beta version usage of the TurboPrep/AutoModel extension
		PLUGIN_BLACKLIST.put("rmx_model_simulator", new Pair<>(null, new VersionNumber(9, 0, 0, "BETA4")));

		// Google dependencies of earlier In-Database Processing extension and newer Cloud
		// Connectivity may collide
		PLUGIN_BLACKLIST.put("rmx_in_database_processing", new Pair<>(null, new VersionNumber(9, 1, 0)));
	}

	/**
	 * The set of white listed plugins if specified by the {@value PROPERTY_PLUGINS_WHITELIST} admin property.
	 *
	 * @since 9.0.0
	 */
	private static Set<String> pluginWhitelist = null;

	/**
	 * Boolean to indicate that shipped extension jars are allowed. Specified by the special admin keyword {@value #WHITELIST_SHIPPED}.
	 *
	 * @since 9.0.0
	 */
	private static boolean allowShippedExtensions = true;

	/** map of all plugin loading times */
	private static final Map<String, Long> LOADING_TIMES = new ConcurrentHashMap<>();

	/**
	 * amount of time in ms a plugin has to load before its loading time will be displayed as
	 * WARNING instead of INFO log level
	 */
	private static final int LOADING_THRESHOLD = 10_000;

	/** Creates a new plugin based on the plugin .jar file. */
	public Plugin(File file) throws IOException {
		this.file = file;
		this.archive = new JarFile(this.file);
		this.classLoader = makeInitialClassloader();
		fetchMetaData();
		this.resourceSource = new ResourceSource(this.classLoader);
		Tools.setResourceSourceForPlugin(getExtensionId(), resourceSource);
		fetchPluginData();
		this.classLoader.setPluginKey(getExtensionId());

		if (!RapidMiner.getExecutionMode().isHeadless()) {
			RapidMiner.getSplashScreen().addExtension(this);
		}
	}

	/**
	 * This method will create an initial class loader that is only used to access the manifest.
	 * After the manifest is read, a new class loader will be constructed from all dependencies.
	 */
	private PluginClassLoader makeInitialClassloader() {
		URL url;
		try {
			url = this.file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Cannot make classloader for plugin: " + e, e);
		}
		return new PluginClassLoader(new URL[] { url });
	}

	/**
	 * This method will build the final class loader for this plugin that contains all class loaders
	 * of all plugins this plugin depends on.
	 *
	 * This must be called after all plugins have been initially loaded.
	 */
	public void buildFinalClassLoader() {
		// add URLs of plugins this plugin depends on
		for (Dependency dependency : this.pluginDependencies) {
			final Plugin other = getPluginByExtensionId(dependency.getPluginExtensionId());
			classLoader.addDependency(other);
		}

	}

	/** Returns the name of the plugin. */
	public String getName() {
		return name;
	}

	/** Returns the version of this plugin. */
	public String getVersion() {
		return version;
	}

	/** Returns the necessary RapidMiner version. */
	public VersionNumber getNecessaryRapidMinerVersion() {
		return new VersionNumber(requiredRapidMinerVersion);
	}

	/**
	 * Returns the class name of the plugin init class
	 */
	public String getPluginInitClassName() {
		return pluginInitClassName;
	}

	public String getPluginParseRules() {
		return pluginParseRules;
	}

	public String getPluginGroupDescriptions() {
		return pluginGroupDescriptions;
	}

	public String getPluginErrorDescriptions() {
		return pluginErrorDescriptions;
	}

	public String getPluginUserErrorDescriptions() {
		return pluginUserErrorDescriptions;
	}

	public String getPluginGUIDescriptions() {
		return pluginGUIDescriptions;
	}

	public String getPluginSettingsDescriptions() {
		return pluginSettingsDescriptions;
	}

	public String getPluginSettingsStructure() {
		return pluginSettingsStructure;
	}

	/**
	 * Returns the resource identifier of the xml file specifying the operators
	 */
	public String getPluginResourceOperators() {
		return pluginResourceOperators;
	}

	/**
	 * Returns the resource identifier of the IO Object descriptions.
	 */
	public String getPluginResourceObjects() {
		return pluginResourceObjects;
	}

	/** Returns the plugin dependencies of this plugin. */
	public List<Dependency> getPluginDependencies() {
		return pluginDependencies;
	}

	/**
	 * Returns the class loader of this plugin. This class loader should be used in cases where
	 * Class.forName(...) should be used, e.g. for implementation finding in all classes (including
	 * the core and the plugins).
	 */
	public PluginClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * Returns the class loader of this plugin. This class loader should be used in cases where
	 * Class.forName(...) should find a class explicitly defined in this plugin jar.
	 */
	public ClassLoader getOriginalClassLoader() {
		try {
			final URL url = new URL("file", null, this.file.getAbsolutePath());
			return AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {

				@Override
				public ClassLoader run() throws Exception {
					return new URLClassLoader(new URL[] { url }, Plugin.class.getClassLoader());
				}
			});

		} catch (IOException | PrivilegedActionException e) {
			return null;
		}
	}

	/** Checks the RapidMiner version and plugin dependencies. */
	private boolean checkDependencies(Plugin plugin, Collection<Plugin> plugins) {
		if (RapidMiner.getVersion().compareTo(getNecessaryRapidMinerVersion()) < 0) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.plugin.Plugin.registring_operators_error_rm_version",
					new Object[] { plugin.getName(), plugin.getNecessaryRapidMinerVersion(), RapidMiner.getVersion() });
			return false;
		}
		// other extensions
		Iterator<Dependency> i = pluginDependencies.iterator();
		while (i.hasNext()) {
			Dependency dependency = i.next();
			if (!dependency.isFulfilled(plugins)) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.plugin.Plugin.registring_operators_error_ext_missing",
						new Object[] { plugin.getName(), dependency.getPluginExtensionId(), dependency.getPluginVersion() });
				return false;
			}
		}
		// all ok
		return true;
	}

	/** Collects all meta data of the plugin from the manifest file. */
	private void fetchMetaData() throws IOException {
		try {
			java.util.jar.Attributes atts = archive.getManifest().getMainAttributes();

			name = getValue(atts, "Implementation-Title");
			if (name == null) {
				name = archive.getName();
			}
			version = getValue(atts, "Implementation-Version");
			if (version == null) {
				version = "";
			}

			url = getValue(atts, "Implementation-URL");
			vendor = getValue(atts, "Implementation-Vendor");
			prefix = getValue(atts, "Namespace");
			extensionId = getValue(atts, "Extension-ID");
			pluginInitClassName = getValue(atts, "Initialization-Class");
			requiredRapidMinerVersion = getValue(atts, "RapidMiner-Version");
			String dependencies = getValue(atts, "Plugin-Dependencies");
			if (dependencies == null) {
				dependencies = "";
			}
			addDependencies(dependencies);

			RapidMiner.splashMessage("loading_plugin", name);
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			} else {
				throw new IOException(e.getMessage(), e);
			}
		}
	}

	private void fetchPluginData() throws IOException {
		try {
			java.util.jar.Attributes atts = archive.getManifest().getMainAttributes();

			pluginResourceObjects = getDescriptorResource("IOObject-Descriptor", false, false, atts);
			pluginResourceOperators = getDescriptorResource("Operator-Descriptor", false, true, atts);
			pluginParseRules = getDescriptorResource("ParseRule-Descriptor", false, false, atts);
			pluginGroupDescriptions = getDescriptorResource("Group-Descriptor", false, false, atts);

			pluginErrorDescriptions = getDescriptorResource("Error-Descriptor", false, true, atts);
			pluginUserErrorDescriptions = getDescriptorResource("UserError-Descriptor", false, true, atts);
			pluginGUIDescriptions = getDescriptorResource("GUI-Descriptor", false, true, atts);
			pluginSettingsDescriptions = getDescriptorResource("Settings-Descriptor", false, true, atts);
			pluginSettingsStructure = getDescriptorResource("SettingsStructure-Descriptor", false, false, atts);
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw e;
			} else {
				throw new IOException(e.getMessage(), e);
			}
		}
	}

	private String getValue(java.util.jar.Attributes atts, String key) {
		String result = atts.getValue(key);
		if (result == null) {
			return null;
		} else {
			result = result.trim();
			if (result.isEmpty()) {
				return null;
			} else {
				return result;
			}
		}

	}

	private String getDescriptorResource(String typeName, boolean mandatory, boolean isBundle, java.util.jar.Attributes atts)
			throws IOException {
		String value = getValue(atts, typeName);
		if (value == null) {
			if (mandatory) {
				throw new IOException("Manifest attribute '" + typeName + "' is not defined.");
			} else {
				return null;
			}
		} else {
			if (isBundle) {
				return toResourceBundleIdentifier(value);
			} else {
				return toResourceIdentifier(value);
			}
		}
	}

	private String toResourceBundleIdentifier(String value) {
		if (value.startsWith("/")) {
			value = value.substring(1);
		}
		if (value.endsWith(".properties")) {
			value = value.substring(0, value.length() - 11);
		}
		return value;
	}

	/**
	 * Removes leading slash if present.
	 */
	private String toResourceIdentifier(String value) {
		if (value.startsWith("/")) {
			value = value.substring(1);
		}
		return value;
	}

	/** Register plugin dependencies. */
	private void addDependencies(String dependencies) {
		pluginDependencies.addAll(Dependency.parse(dependencies));
	}

	public void registerOperators() {
		if (disabled) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.registring_operators_error",
					getName());
		}
		InputStream in = null;
		// trying normal plugins
		if (pluginResourceOperators != null) {
			URL operatorsURL = this.classLoader.getResource(pluginResourceOperators);
			if (operatorsURL == null) {
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.tools.plugin.Plugin.operators_description_not_existing",
						new Object[] { pluginResourceOperators, archive.getName() });
				return;
			} else {
				// register operators
				try {
					in = operatorsURL.openStream();
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING,
							I18N.getMessage(LogService.getRoot().getResourceBundle(),
									"com.rapidminer.tools.plugin.Plugin.operator_descriptor_reading_error", operatorsURL,
									archive.getName()),
							e);
					return;
				}
			}
		} else if (pluginInitClassName != null) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.plugin.Plugin.operator_descriptor_not_specified",
					new Object[] { getName(), pluginInitClassName });

			// if no operators.xml found: Try via PluginInit method getOperatorStream()
			try {
				// important: here the combined class loader has to be used
				Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getClassLoader());
				Method registerOperatorMethod = pluginInitator.getMethod("getOperatorStream", ClassLoader.class);
				in = (InputStream) registerOperatorMethod.invoke(null, getClassLoader());
			} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException
					| IllegalAccessException | InvocationTargetException e) {
				// ignore
			}
		}
		if (in != null) {
			OperatorService.registerOperators(archive.getName(), in, this.classLoader, this);
		} else {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.operator_descriptor_not_defined",
					getName());
		}
	}

	/**
	 * Register all things delivered with this plugin.
	 *
	 * @throws PluginException
	 */
	public void registerDescriptions() throws PluginException {
		// make sure to not accidentally find resources from dependencies
		this.classLoader.setIgnoreDependencyClassloaders(true);

		// registering parse rules
		if (pluginParseRules != null) {
			URL resource = this.classLoader.getResource(pluginParseRules);
			if (resource != null) {
				XMLImporter.importParseRules(resource, this);
			} else {
				throw new PluginException(
						"Cannot find parse rules '" + pluginParseRules + "' for plugin " + getName() + ".");
			}
		}

		// registering settings for internationalization
		if (pluginErrorDescriptions != null) {
			I18N.registerErrorBundle(
					ResourceBundle.getBundle(pluginErrorDescriptions, Locale.getDefault(), this.classLoader));
		}
		if (pluginGUIDescriptions != null) {
			I18N.registerGUIBundle(ResourceBundle.getBundle(pluginGUIDescriptions, Locale.getDefault(), this.classLoader));
		}
		if (pluginUserErrorDescriptions != null) {
			I18N.registerUserErrorMessagesBundle(
					ResourceBundle.getBundle(pluginUserErrorDescriptions, Locale.getDefault(), this.classLoader));
		}
		if (pluginSettingsDescriptions != null) {
			settingsRessourceBundle = ResourceBundle.getBundle(pluginSettingsDescriptions, Locale.getDefault(),
					this.classLoader);
			I18N.registerSettingsBundle(settingsRessourceBundle);
		}

		// Do only register renderers and process renderer colors if not in headless mode
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			// registering renderers
			if (pluginResourceObjects != null) {
				URL resource = this.classLoader.getResource(pluginResourceObjects);
				if (resource != null) {
					RendererService.init(name, resource, this.classLoader);
				} else {
					throw new PluginException("Cannot find io object descriptor '" + pluginResourceObjects + "' for plugin "
							+ getName() + ".");
				}
			}

			// registering colors
			if (pluginGroupDescriptions != null) {
				ProcessDrawUtils.registerAdditionalObjectColors(pluginGroupDescriptions, name, classLoader, this);
				ProcessDrawUtils.registerAdditionalGroupColors(pluginGroupDescriptions, name, classLoader, this);
			}
		}

		this.classLoader.setIgnoreDependencyClassloaders(false);
	}

	/** Creates the about box for this plugin. */
	public AboutBox createAboutBox(Frame owner) {
		ClassLoader simpleClassLoader = makeInitialClassloader();
		String about = "";
		try {
			URL url = simpleClassLoader.getResource("META-INF/ABOUT.NFO");
			if (url != null) {
				about = Tools.readTextFile(new InputStreamReader(url.openStream()));
			}
		} catch (Exception e) {
			I18N.getMessage(ResourceBundle.getBundle("com.rapidminer.resources.i18n.LogMessages"),
					"com.rapidminer.tools.I18N.plugin_warning1", Level.WARNING, getName(), e);
		}
		Image productLogo = null;
		try (InputStream imageIn = simpleClassLoader.getResourceAsStream("META-INF/icon.png")) {
			productLogo = ImageIO.read(imageIn);
		} catch (Exception e) {
			// LogService.getRoot().log(Level.WARNING, "Error reading icon.png for plugin " +
			// getName(), e);
			I18N.getMessage(ResourceBundle.getBundle("com.rapidminer.resources.i18n.LogMessages"),
					"com.rapidminer.tools.I18N.plugin_warning2", Level.WARNING, getName(), e);
		}
		return new AboutBox(owner, name, version, "Vendor: " + (vendor != null ? vendor : "unknown"), url, about, true,
				productLogo);
	}

	/**
	 * Scans the directory for jar files and calls {@link #registerPlugins(List, boolean)} on the
	 * list of files.
	 */
	private static void findAndRegisterPlugins(File pluginDir, boolean showWarningForNonPluginJars,
			boolean overwritePluginsWithHigherVersions) {
		List<File> files = new LinkedList<>();
		if (pluginDir == null) {
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.tools.plugin.Plugin.findandregisterplugins_called_with_null_directory");
			return;
		}
		if (!(pluginDir.exists() && pluginDir.isDirectory())) {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.plugin_dir_not_existing", pluginDir);
		} else {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.scanning_for_plugins", pluginDir);
			files.addAll(Arrays.asList(pluginDir.listFiles((dir, name) -> name.endsWith(".jar"))));
		}
		registerPlugins(files, showWarningForNonPluginJars, overwritePluginsWithHigherVersions);
	}

	/**
	 * Makes {@link Plugin} s from all files and adds them to {@link #ALL_PLUGINS}. After all
	 * Plugins are loaded, they must be assigend their final class loader.
	 */
	private static void registerPlugins(List<File> files, boolean showWarningForNonPluginJars,
			boolean overwritePluginsWithHigherVersions) {
		List<Plugin> newPlugins = new LinkedList<>();
		for (File file : files) {
			try (JarFile jarFile = new JarFile(file)) {
				Manifest manifest = jarFile.getManifest();
				Attributes attributes = manifest.getMainAttributes();
				if (RAPIDMINER_TYPE_PLUGIN.equals(attributes.getValue(RAPIDMINER_TYPE))) {
					final Plugin plugin = new Plugin(file);
					final Plugin conflict = getPluginByExtensionId(plugin.getExtensionId(), newPlugins);
					if (conflict == null) {
						newPlugins.add(plugin);
					} else {
						resolveVersionConflict(plugin, conflict, newPlugins);
					}
				} else if (showWarningForNonPluginJars) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.plugin.Plugin.jar_file_does_not_contain_entry",
							new Object[]{jarFile.getName(), RAPIDMINER_TYPE});
				}
			} catch (Throwable e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.plugin.Plugin.plugin_loading_error", file, e.getMessage()), e);
			}
		}
		for (Plugin newPlugin : newPlugins) {
			LogService.getRoot().log(Level.INFO, "Register plugin: " + newPlugin.getName());
			Plugin oldPlugin = getPluginByExtensionId(newPlugin.getExtensionId(), ALL_PLUGINS);
			if (oldPlugin == null) {
				ALL_PLUGINS.add(newPlugin);
			} else {
				if (overwritePluginsWithHigherVersions) {
					ALL_PLUGINS.remove(oldPlugin);
					ALL_PLUGINS.add(newPlugin);
				} else {
					resolveVersionConflict(newPlugin, oldPlugin, ALL_PLUGINS);
				}
			}
		}
	}

	/**
	 * Resolves an extension version conflict by comparing both extension versions. If the
	 * conflicting extension has a lower version than the new extension version the conflicting
	 * extension is removed from the provided list and the new extension is added to it.
	 *
	 * @param newExtension
	 *            the newly loaded extension
	 * @param conflictingExtension
	 *            the already registered extension with the same extension ID
	 * @param plugins
	 *            the collection from which the conflicting extension should be removed if its
	 *            version is lower than the version of the new extension
	 */
	private static void resolveVersionConflict(Plugin newExtension, Plugin conflictingExtension,
			Collection<Plugin> plugins) {

		// keep extension with higher version number
		VersionNumber newVersion = new VersionNumber(newExtension.getVersion());
		VersionNumber conflictVersion = new VersionNumber(conflictingExtension.getVersion());
		VersionNumber higherNumber = conflictVersion;
		if (newVersion.compareTo(conflictVersion) > 0) {
			if (isExtensionVersionAllowed(newExtension, newVersion)) {
				plugins.remove(conflictingExtension);
				Tools.setResourceSourceForPlugin(newExtension.getExtensionId(), newExtension.getResourceSource());
				plugins.add(newExtension);
				higherNumber = newVersion;
			} else {
				Tools.setResourceSourceForPlugin(conflictingExtension.getExtensionId(), conflictingExtension.getResourceSource());
			}
		}

		LogService.getRoot().log(Level.WARNING,
				"com.rapidminer.tools.plugin.Plugin.duplicate_plugin_definition_higher_version",
				new Object[] { newExtension.getName(), newExtension.file, conflictingExtension.file,
						higherNumber.getShortLongVersion() });
	}

	@Override
	public String toString() {
		return name + " " + version + " (" + archive.getName() + ") depending on " + pluginDependencies;
	}

	/**
	 * Checks if the version of the extension with id extensionId is blacklisted or if the extension is not whitelisted.
	 *
	 * @param extensionId
	 *            the id of the extension to check
	 * @param version
	 *            the version to check
	 * @return {@code true} if the extension version is blacklisted or not whitelisted
	 */
	public static boolean isExtensionVersionBlacklisted(String extensionId, VersionNumber version) {
		if (!isExtensionWhitelisted(extensionId)) {
			return true;
		}
		if (PLUGIN_BLACKLIST.containsKey(extensionId)) {
			Pair<VersionNumber, VersionNumber> versionRange = PLUGIN_BLACKLIST.get(extensionId);
			return versionRange == null || (versionRange.getSecond() == null || version.isAtMost(versionRange.getSecond()))
					&& (versionRange.getFirst() == null || version.isAtLeast(versionRange.getFirst()));
		}
		return false;
	}

	/**
	 * Checks if the extension with id extensionID is whitelisted.
	 *
	 * @param extensionId
	 * 		the id of the extension to check
	 * @return {@code true} if the extension is allowed.
	 * @see #isExtensionVersionAllowed(Plugin, VersionNumber)
	 * @since 9.0.0
	 */
	public static boolean isExtensionWhitelisted(String extensionId) {
		return isExtensionWhitelisted(null, extensionId);
	}

	/**
	 * Checks if the extension is whitelisted.
	 *
	 * @param extension
	 * 		the extension to check
	 * @return {@code true} if the extension is allowed.
	 * @see #isExtensionVersionAllowed(Plugin, VersionNumber)
	 * @since 9.0.0
	 */
	public static boolean isExtensionWhitelisted(Plugin extension) {
		return isExtensionWhitelisted(extension, extension.getExtensionId());
	}

	/**
	 * Checks if the extension or an extension with the given extension ID is whitelisted. If the specified {@link Plugin}
	 * is not {@code null}, the plugin will also be checked for being signed, depending on whether it's a shipped extension.
	 *
	 * @param extension
	 * 		the extension to check, can be {@code null}
	 * @param extensionId
	 * 		the extension ID to check, must match the extension if present
	 * @return {@code true} if the extension is allowed.
	 * @since 9.0.0
	 * @see #initializePluginWhiteList()
	 */
	private static boolean isExtensionWhitelisted(Plugin extension, String extensionId) {
		// exclude non matching entries
		if (extension != null && !extension.getExtensionId().equals(extensionId)) {
			return false;
		}
		// no white list => no constraints
		if (pluginWhitelist == null) {
			return true;
		}
		// always allow packaged extensions; allow shipped extensions if either allowed in general or listed
		// otherwise allow listed extensions; check all shipped extensions if they are given as a Plugin object
		return PACKAGED_EXTENSIONS.contains(extensionId)
				|| SHIPPED_EXTENSIONS.contains(extensionId) && (allowShippedExtensions || pluginWhitelist.contains(extensionId))
				? extension == null || extension.isSigned() : pluginWhitelist.contains(extensionId);
	}

	/**
	 * Adds the amount of milliseconds elapsed since the given start time to the already logged
	 * amount of time the specified extension took to load. Times can be accessed from the
	 * {@link #LOADING_TIMES} map.
	 *
	 * @param id
	 *            the id of the extension
	 * @param start
	 *            the starting time of this recording in milliseconds since 1970
	 */
	private static void recordLoadingTime(String id, long start) {
		long end = System.currentTimeMillis();
		Long time = LOADING_TIMES.get(id);
		if (time == null) {
			time = 0L;
		}
		time += end - start;
		LOADING_TIMES.put(id, time);
	}

	/**
	 * Finds all plugins in lib/plugins directory and initializes them.
	 */
	private static void registerAllPluginDescriptions() {
		Iterator<Plugin> i = ALL_PLUGINS.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (!plugin.checkDependencies(plugin, ALL_PLUGINS)) {
				plugin.disabled = true;
				i.remove();
				INCOMPATIBLE_PLUGINS.add(plugin);
			}
		}

		if (!ALL_PLUGINS.isEmpty()) {
			i = ALL_PLUGINS.iterator();
			while (i.hasNext()) {
				Plugin plugin = i.next();
				try {
					long start = System.currentTimeMillis();
					plugin.registerDescriptions();
					recordLoadingTime(plugin.getExtensionId(), start);
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.plugin_initializing_error", e), e);
					i.remove();
					plugin.disabled = true;
					INCOMPATIBLE_PLUGINS.add(plugin);
				}
			}
		}
	}

	/**
	 * Checks whether the given combination of extension and its version is allowed.
	 * For now, this only checks that packaged extensions do not have a higher Studio core version dependency than the current Studio core.
	 *
	 * @param extension
	 * 		the extension in question
	 * @param extensionVersion
	 * 		the extension version to check
	 * @return {@code true} if the extension version would work with the current Studio core; {@code false} otherwise
	 */
	private static boolean isExtensionVersionAllowed(final Plugin extension, final VersionNumber extensionVersion) {
		// packaged extension required Studio core version can only be as high as current Studio core version
		RapidMinerVersion coreVersion = new RapidMinerVersion();
		boolean allowed = extension.getNecessaryRapidMinerVersion().isAtMost(coreVersion);
		if (!allowed) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.plugin_studio_core_version_too_high",
					new Object[] { extension.getName(), extensionVersion.getShortLongVersion(), extension.getNecessaryRapidMinerVersion().getShortLongVersion() });
		}

		return allowed;
	}

	/**
	 * Reads the {@value PROPERTY_PLUGINS_WHITELIST} admin properties and updates the white list.
	 * Packaged extensions (with IDs found in {@link #PACKAGED_EXTENSIONS} are always allowed.
	 * Shipped extensions (with IDs found in {@link #SHIPPED_EXTENSIONS} are most often allowed and include packaged extensions.
	 * <p><strong>Note:</strong> Shipped extensions (if provided as {@link Plugin}, not as id) will be checked for signage.
	 * <p>An absent or empty parameter means that all extensions are allowed. Empty means the empty string, only white spaces
	 * or only whitespaces separated by commas.
	 * <p>The keyword {@value #WHITELIST_NONE} can be used to indicate that only packaged extensions are allowed.
	 * This overrides any other value in the property
	 * <p>The keyword {@value #WHITELIST_SHIPPED} can be used to indicate that shipped (and also packaged extensions are allowed.
	 * Other extensions are allowed, if they are specified in the property
	 * <p>If only extension IDs are specified, the packaged extensions and those specified are allowed, excluding shipped extensions
	 * that are not packaged.
	 *
	 * @since 9.0.0
	 * @see #isExtensionWhitelisted(Plugin, String)
	 */
	private static synchronized void initializePluginWhiteList() {
		String whitelistProperty = ParameterService.getParameterValue(PROPERTY_PLUGINS_WHITELIST);
		if (whitelistProperty == null || whitelistProperty.trim().isEmpty()) {
			pluginWhitelist = null;
			allowShippedExtensions = true;
			return;
		}

		Set<String> newWhitelist = new HashSet<>();
		boolean newShippedState = false;
		String[] whitelistEntries = whitelistProperty.split(",");
		for (String whitelistEntry : whitelistEntries) {
			whitelistEntry = whitelistEntry.trim();
			if (whitelistEntry.isEmpty()) {
				continue;
			}
			if (WHITELIST_NONE.equals(whitelistEntry)) {
				pluginWhitelist = Collections.emptySet();
				allowShippedExtensions = false;
				return;
			}
			if (WHITELIST_SHIPPED.equals(whitelistEntry)) {
				newShippedState = true;
			} else {
				newWhitelist.add(whitelistEntry);
			}
		}
		allowShippedExtensions = newWhitelist.isEmpty() || newShippedState;
		if (newWhitelist.isEmpty()) {
			pluginWhitelist = newShippedState ? Collections.emptySet() : null;
		} else {
			pluginWhitelist = Collections.unmodifiableSet(newWhitelist);
		}
	}

	/** Checks if Studio update should be prohibited */
	private static void updateStudioUpdatePolicy() {
		if (Boolean.parseBoolean(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_DISALLOW_STUDIO_UPDATE))) {
			PLUGIN_BLACKLIST.put(PACKAGEID_RAPIDMINER, null);
		} else {
			PLUGIN_BLACKLIST.remove(PACKAGEID_RAPIDMINER);
		}
	}

	/**
	 * Removes the blacklisted and non-whitelisted plugins from the list of all plugins and adds them to the
	 * incompatible plugins.
	 */
	private static void filterBlacklistedPlugins() {
		Iterator<Plugin> i = ALL_PLUGINS.iterator();
		while (i.hasNext()) {
			Plugin plugin = i.next();
			if (plugin.isIncompatible()) {
				plugin.disabled = true;
				i.remove();
				INCOMPATIBLE_PLUGINS.add(plugin);
			}
		}
	}

	/**
	 * Checks if the plugin is marked as incompatible by the {@link #PLUGIN_BLACKLIST} or is not allowed by the {@link #pluginWhitelist}.
	 *
	 * @return whether the plugin is incompatible
	 */
	private final boolean isIncompatible() {
		if (!isExtensionWhitelisted(this)) {
			return true;
		}
		if (PLUGIN_BLACKLIST.containsKey(getExtensionId())) {
			Pair<VersionNumber, VersionNumber> forbiddenRange = PLUGIN_BLACKLIST.get(getExtensionId());
			if (forbiddenRange == null) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.incompatible_extension",
						new Object[] { getName() });
				return true;
			}
			VersionNumber startVersion = forbiddenRange.getFirst();
			VersionNumber endVersion = forbiddenRange.getSecond();
			VersionNumber currentVersion = new VersionNumber(getVersion());
			if ((startVersion != null && currentVersion.isAtLeast(startVersion) || startVersion == null)
					&& (endVersion != null && currentVersion.isAtMost(endVersion) || endVersion == null)) {

				if (startVersion != null && endVersion != null) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.plugin.Plugin.incompatible_extension_version",
							new Object[] { getName(), startVersion, endVersion, currentVersion });
				} else if (startVersion != null) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.plugin.Plugin.incompatible_extension_version_above",
							new Object[] { getName(), startVersion, currentVersion });
				} else if (endVersion != null) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.plugin.Plugin.incompatible_extension_version_below",
							new Object[] { getName(), endVersion, currentVersion });
				} else {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.incompatible_extension",
							new Object[] { getName() });
				}
				return true;
			}

		}
		return false;
	}

	/**
	 * Returns whether this extension is signed or not.
	 * This will return {@code false} if there is no init class defined or can not be loaded, there is no certificate present
	 * or the certificate is not sufficient.
	 *
	 * @return {@code true} if the extension is properly signed, {@code false} otherwise
	 * @since 9.0.0
	 */
	public boolean isSigned() {
		if (pluginInitClassName == null) {
			return false;
		}
		try {
			Policy policy = Policy.getPolicy();
			if (!(policy instanceof PluginSandboxPolicy)) {
				return false;
			}
			Class<?> initClass = Class.forName(pluginInitClassName, false, classLoader);
			ProtectionDomain protectionDomain = initClass.getProtectionDomain();
			Certificate[] certificates = protectionDomain.getCodeSource().getCertificates();
			if (certificates == null || certificates.length == 0) {
				return false;
			}
			Enumeration<Permission> elements = policy.getPermissions(protectionDomain).elements();
			while (elements.hasMoreElements()) {
				if (elements.nextElement() instanceof AllPermission) {
					return true;
				}
			}
		} catch (ClassNotFoundException | SecurityException e) {
			return false;
		}
		return false;
	}

	/**
	 * This method will check all needed dependencies of all currently registered plugin files and
	 * will build the final class loaders for the extensions containing all dependencies.
	 */
	public static void finalizePluginLoading() {
		// building final class loader with all dependent extensions
		LinkedList<Plugin> queue = new LinkedList<>(ALL_PLUGINS);
		HashSet<Plugin> initialized = new HashSet<>();
		// now initialized every extension that's dependencies are fulfilled as long as we find
		// another per round
		boolean found = false;
		while (found || !queue.isEmpty() && initialized.isEmpty()) {
			found = false;
			Iterator<Plugin> iterator = queue.iterator();
			while (iterator.hasNext()) {
				Plugin plugin = iterator.next();
				boolean dependenciesMet = true;
				long start = System.currentTimeMillis();
				for (Dependency dependency : plugin.pluginDependencies) {
					Plugin dependencyPlugin = getPluginByExtensionId(dependency.getPluginExtensionId());
					if (dependencyPlugin == null) {
						// if we cannot find dependency plugin: Don't load this one, instead remove
						// it and post error
						ALL_PLUGINS.remove(plugin);
						INCOMPATIBLE_PLUGINS.add(plugin);
						iterator.remove();
						LogService.getRoot().log(Level.SEVERE, "com.rapidminer.tools.plugin.Plugin.loading_extension_error",
								new Object[] { plugin.extensionId, dependency.getPluginExtensionId() });
						found = true;
						dependenciesMet = false;
						break; // break this loop: Nothing to check
					} else {
						dependenciesMet &= initialized.contains(dependencyPlugin);
					}
				}

				// if we have all dependencies met: Load final class loader
				if (dependenciesMet) {
					plugin.buildFinalClassLoader();
					initialized.add(plugin);
					iterator.remove();

					// then we have one more extension that is initialized, next round might find
					// more
					found = true;

					// remember the initialization order globally
					PLUGIN_INITIALIZATION_ORDER.add(plugin);
				}
				recordLoadingTime(plugin.getExtensionId(), start);
			}

		}
	}

	/**
	 * Registers all operators from the plugins previously found by a call of
	 * registerAllPluginDescriptions
	 */
	public static void registerAllPluginOperators() {
		for (Plugin plugin : ALL_PLUGINS) {
			long start = System.currentTimeMillis();
			plugin.registerOperators();
			recordLoadingTime(plugin.getExtensionId(), start);
		}
	}

	/** Returns a class loader which is able to load all classes (core _and_ all plugins). */
	public static ClassLoader getMajorClassLoader() {
		return MAJOR_CLASS_LOADER;
	}

	/** Returns a sorted collection of all plugins. */
	public static Collection<Plugin> getAllPlugins() {
		return ALL_PLUGINS;
	}

	/**
	 * Returns unmodifiable list of plugins that failed to load.
	 *
	 * @return the list of plugins
	 */
	public static Collection<Plugin> getIncompatiblePlugins() {
		return Collections.unmodifiableCollection(INCOMPATIBLE_PLUGINS);
	}

	/** Returns the plugin with the given extension id. */
	public static Plugin getPluginByExtensionId(String name) {
		return getPluginByExtensionId(name, ALL_PLUGINS);
	}

	/** Returns the plugin with the given extension id. */
	private static Plugin getPluginByExtensionId(String name, Collection<Plugin> plugins) {
		for (Plugin plugin : plugins) {
			if (name.equals(plugin.getExtensionId())) {
				return plugin;
			}
		}
		return null;
	}

	/**
	 * This method will try to invoke the method void initGui(MainFrame) of PluginInit class of
	 * every plugin.
	 */
	public static void initPluginGuis(MainFrame mainframe) {
		callPluginInitMethods("initGui", new Class[] { MainFrame.class }, new Object[] { mainframe }, false);
	}

	/**
	 * This method will try to invoke the public static method initPlugin() of the class
	 * com.rapidminer.PluginInit for arbitrary initializations of the plugins. It is called directly
	 * after registering the plugins.
	 */
	public static void initPlugins() {
		callPluginInitMethods("initPlugin", new Class[] {}, new Object[] {}, false);
	}

	public static void initPluginUpdateManager() {
		callPluginInitMethods("initPluginManager", new Class[] {}, new Object[] {}, false);
	}

	public static void initFinalChecks() {
		callPluginInitMethods("initFinalChecks", new Class[] {}, new Object[] {}, false);
	}

	public static void initPluginTests() {
		callPluginInitMethods("initPluginTests", new Class[] {}, new Object[] {}, false);
	}

	/**
	 * Finds the given object's {@link Plugin} if possible. Returns {@code null} for objects whose classes were
	 * not loaded through a {@link PluginClassLoader}.
	 *
	 * @param o
	 * 		the object to check
	 * @return the plugin associated with the given object or {@code null} if it was not loaded through a plugin
	 * @since 9.3
	 */
	public static Plugin getPluginForObject(Object o) {
		if (o == null) {
			return null;
		}
		return getPluginForClass(o.getClass());
	}

	/**
	 * Finds the given class' {@link Plugin} if possible. Returns {@code null} for classes that were
	 * not loaded through a {@link PluginClassLoader}.
	 *
	 * @param c
	 * 		the class to check
	 * @return the plugin associated with the given object or {@code null} if it was not loaded through a plugin
	 * @since 9.3
	 */
	public static Plugin getPluginForClass(Class<?> c) {
		ClassLoader cl = c.getClassLoader();
		if (cl instanceof PluginClassLoader) {
			return getPluginByExtensionId(((PluginClassLoader) cl).getPluginKey());
		}
		return null;
	}

	private static void callPluginInitMethods(String methodName, Class<?>[] arguments, Object[] argumentValues,
			boolean useOriginalJarClassLoader) {
		for (Plugin plugin : PLUGIN_INITIALIZATION_ORDER) {
			if (!ALL_PLUGINS.contains(plugin)) {
				// plugin may be removed in the meantime,
				// so skip the initialization
				continue;
			}
			if (!plugin.checkDependencies(plugin, ALL_PLUGINS)) {
				ALL_PLUGINS.remove(plugin);
				INCOMPATIBLE_PLUGINS.add(plugin);
				continue;
			}

			long start = System.currentTimeMillis();
			if (!plugin.callInitMethod(methodName, arguments, argumentValues, useOriginalJarClassLoader)) {
				ALL_PLUGINS.remove(plugin);
				INCOMPATIBLE_PLUGINS.add(plugin);
			}
			recordLoadingTime(plugin.getExtensionId(), start);
		}
	}

	/**
	 * @return true if everything went well, false if a fatal error occurred. The plugin should be
	 *         unregistered in this case.
	 */
	private boolean callInitMethod(String methodName, Class<?>[] arguments, Object[] argumentValues,
			boolean useOriginalJarClassLoader) {
		if (pluginInitClassName == null) {
			return true;
		}
		try {
			ClassLoader classLoader;
			if (useOriginalJarClassLoader) {
				classLoader = getOriginalClassLoader();
			} else {
				classLoader = getClassLoader();
			}
			Class<?> pluginInitator = Class.forName(pluginInitClassName, false, classLoader);
			Method initMethod;
			try {
				initMethod = pluginInitator.getMethod(methodName, arguments);
			} catch (NoSuchMethodException e) {
				return true;
			}
			initMethod.invoke(null, argumentValues);
			return true;
		} catch (Throwable e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.plugin_initializer_error", pluginInitClassName, methodName,
							getName(), e.getMessage()),
					e);
			return false;
		}
	}

	public static void initPluginSplashTexts(SplashScreen splashScreen) {
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			callPluginInitMethods("initSplashTexts", new Class[] { SplashScreen.class }, new Object[] { splashScreen },
					false);
		}
	}

	public static void initAboutTexts(Properties properties) {
		callPluginInitMethods("initAboutTexts", new Class[] { Properties.class }, new Object[] { properties }, false);
	}

	public boolean showAboutBox() {
		if (pluginInitClassName == null) {
			return true;
		}
		try {
			Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getClassLoader());
			Method initGuiMethod = pluginInitator.getMethod("showAboutBox");
			Boolean showAboutBox = (Boolean) initGuiMethod.invoke(null);
			return showAboutBox.booleanValue();
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
		}
		return true;
	}

	/**
	 * Defines whether the extension is using the "extensions.EXTENSION_NAME" folder as tree root.
	 *
	 * @return {@code true} by default
	 */
	public synchronized boolean useExtensionTreeRoot() {
		if (pluginInitClassName == null) {
			return true;
		}

		// lookup only once
		if (useExtensionTreeRoot == null) {
			// store old value and ensure that the dependency classloaders are not ignored
			boolean oldValue = this.classLoader.isIgnoreDependencyClassloaders();
			this.classLoader.setIgnoreDependencyClassloaders(false);
			try {
				Class<?> pluginInitator = Class.forName(pluginInitClassName, false, getClassLoader());
				Method initGuiMethod = pluginInitator.getMethod("useExtensionTreeRoot");
				useExtensionTreeRoot = (Boolean) initGuiMethod.invoke(null);
			} catch (Throwable e) {
				useExtensionTreeRoot = Boolean.TRUE;
			}
			// restore setting for ignoring dependency classloaders
			this.classLoader.setIgnoreDependencyClassloaders(oldValue);
		}

		// return cached value
		return useExtensionTreeRoot.booleanValue();
	}

	/**
	 * Initializes all plugins if {@link RapidMiner#PROPERTY_RAPIDMINER_INIT_PLUGINS} is set.
	 * Plugins are searched for in the directory specified by
	 * {@link RapidMiner#PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION} or, if this is not set, in the
	 * RapidMiner/lib/plugins directory.
	 */
	public static void initAll() {
		// only load managed extensions if execution modes indicates
		if (RapidMiner.getExecutionMode().isLoadingManagedExtensions()) {
			ManagedExtension.init();
		}

		String loadPluginsString = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS);
		boolean loadPlugins = Tools.booleanValue(loadPluginsString, true);
		SafeMode safeMode = RapidMinerGUI.getSafeMode();
		boolean isSafeMode = false;
		if (safeMode != null) {
			isSafeMode = safeMode.isSafeMode();
		}
		if (loadPlugins && !isSafeMode) {

			// Check for Web start extension directory and load extensions from Web start extension
			// directory if it exists.
			File webstartPluginDir;
			if (RapidMiner.getExecutionMode() == ExecutionMode.WEBSTART) {
				webstartPluginDir = updateWebstartPluginsCache();
			} else {
				webstartPluginDir = null;
			}

			if (webstartPluginDir != null) {
				findAndRegisterPlugins(webstartPluginDir, true, false);
			}

			// Check if an extension directory is specified in the preferences and load extensions
			// from there (if it exists).
			File pluginDir = null;
			String pluginDirString = ParameterService
					.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION);
			if (pluginDirString != null && !pluginDirString.trim().isEmpty()) {
				pluginDir = new File(pluginDirString);
			} else if (loadFromUserConfigFolder()) {
				// update preferences if preferences property is empty
				// and point it to ~/.RapidMiner/extensions
				pluginDir = FileSystemService.getUserConfigFile("extensions");
				if (!pluginDir.isDirectory() && !pluginDir.mkdirs()) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.tools.plugin.Plugin.could_not_create_user_home_extension_directory");
				}
				ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION,
						pluginDir.getAbsolutePath());
			}
			if (pluginDir != null) {
				findAndRegisterPlugins(pluginDir, true, false);
			}

			// Check for additional extension directories and load extensions from there (if the
			// directory exists).
			for (String additionalExtensionDir : additionalExtensionDirs) {
				File extensionDir = new File(additionalExtensionDir);
				if (extensionDir.isDirectory()) {
					findAndRegisterPlugins(extensionDir, true, false);
				}
			}

			// Check for managed extensions and register them
			registerPlugins(ManagedExtension.getActivePluginJars(), true, false);

			// Check global folder for extensions and register them
			// CAUTION: This extensions overwrite extensions from other folders, even if they have a
			// lower version number.
			// Otherwise plugins that are too new for the running studio version could not work.
			// After this registerPlugins or findAndRegisterPlugins must not be called anymore.
			if (loadFromGlobalFolder()) {
				try {
					// Load globally installed extensions if RAPIDMINER_HOME is specified
					File globalPluginDir = getPluginLocation();
					if (globalPluginDir != null) {
						findAndRegisterPlugins(globalPluginDir, true, true);
					}
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.no_properties_set",
							new Object[] { PlatformUtilities.PROPERTY_RAPIDMINER_HOME });
				}
			}

			initializePluginWhiteList();
			updateStudioUpdatePolicy();
			ParameterService.registerParameterChangeListener(new ParameterChangeListener() {
				@Override
				public void informParameterChanged(String key, String value) {
					if (key.equals(PROPERTY_PLUGINS_WHITELIST)) {
						initializePluginWhiteList();
					} else if (key.equals(RapidMinerGUI.PROPERTY_RAPIDMINER_DISALLOW_STUDIO_UPDATE)) {
						updateStudioUpdatePolicy();
					}
				}

				@Override
				public void informParameterSaved() {
					// ignore
				}
			});
			filterBlacklistedPlugins();
			finalizePluginLoading();
			registerAllPluginDescriptions();
			loadAllSettingsStructures();
			initPlugins();
			updateAllSettingsDescriptions();
		} else {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.plugin.Plugin.plugins_skipped");
		}

		// log extension loading times
		List<Entry<String, Long>> sortedLoadingTimes = new LinkedList<>(LOADING_TIMES.entrySet());
		// sort from fastest to slowest
		sortedLoadingTimes.sort(Comparator.comparing(Entry::getValue));
		for (Entry<String, Long> entry : sortedLoadingTimes) {
			Plugin plugin = getPluginByExtensionId(entry.getKey());
			String loadingTime = String.valueOf(entry.getValue()) + "ms";
			Level logLevel = Level.INFO;
			if (entry.getValue() > LOADING_THRESHOLD) {
				loadingTime = Tools.formatDuration(entry.getValue());
				logLevel = Level.WARNING;
			}

			String identifier;
			String value;
			if (plugin != null) {
				LogService.getRoot().log(logLevel, "com.rapidminer.tools.plugin.Plugin.loading_time",
						new Object[] { plugin.getName(), loadingTime });
				identifier = plugin.getExtensionId() + ActionStatisticsCollector.ARG_SPACER + plugin.getVersion();
				value = ActionStatisticsCollector.VALUE_EXTENSION_INITIALIZATION;
			} else {
				LogService.getRoot().log(logLevel, "com.rapidminer.tools.plugin.Plugin.loading_time_failure",
						new Object[] { entry.getKey(), loadingTime });
				identifier = entry.getKey();
				value = ActionStatisticsCollector.VALUE_EXTENSION_INITIALIZATION_FAILED;
			}

			ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_CONSTANT, value, identifier);
		}
	}

	/**
	 * @return {@code false} if the {@link ExecutionMode} of RapidMiner is embedded, web
	 *         (server/applet) or test. {@code true} for UI, COMMAND_LINE or UNKNOWN.
	 */
	private static boolean loadFromUserConfigFolder() {
		switch (RapidMiner.getExecutionMode()) {
			case APPLET:
			case APPSERVER:
			case EMBEDDED_WITHOUT_UI:
			case EMBEDDED_AS_APPLET:
			case EMBEDDED_WITH_UI:
			case TEST:
				return false;
			case WEBSTART:
			case COMMAND_LINE:
			case UI:
			case UNKNOWN:
			default:
				return true;
		}
	}

	/**
	 * @return {@code false} if the {@link ExecutionMode} of RapidMiner is embedded or web
	 *         (server/applet). {@code true} for UI, COMMAND_LINE or UNKNOWN.
	 */
	private static boolean loadFromGlobalFolder() {
		switch (RapidMiner.getExecutionMode()) {
			case APPLET:
			case APPSERVER:
			case EMBEDDED_WITHOUT_UI:
			case EMBEDDED_AS_APPLET:
			case EMBEDDED_WITH_UI:
				return false;
			case TEST:
			case WEBSTART:
			case COMMAND_LINE:
			case UI:
			case UNKNOWN:
			default:
				return true;
		}
	}

	/** Calls {@link #loadSettingsStructure()} for all extensions, which are not disabled. */
	private static void loadAllSettingsStructures() {
		for (Iterator<Plugin> iterator = getAllPlugins().iterator(); iterator.hasNext();) {
			Plugin plugin = iterator.next();
			if (plugin != null && !plugin.disabled) {
				long start = System.currentTimeMillis();
				plugin.loadSettingsStructure();
				recordLoadingTime(plugin.getExtensionId(), start);
			}
		}
	}

	/**
	 * Checks, if a XML file with information about the settings of a plugin is provided. If such a
	 * XML file is found, it is parsed and the settings items are added to the {@link SettingsItems}
	 * container. The settings can be displayed and changed via the 'RapidMiner Studio Preferences'
	 * dialog.
	 */
	private void loadSettingsStructure() {
		// XML file has to be provided by extension
		if (getPluginSettingsStructure() != null) {

			// Locate XML resource
			URL settingsXML = getClassLoader().getResource(getPluginSettingsStructure());

			// XML file has to be found
			if (settingsXML != null) {
				try {
					// Parse XML
					Map<String, SettingsItem> map = new SettingsXmlHandler().parse(settingsXML.toURI());

					// Add to settings items
					SettingsItems settingsItems = SettingsItems.INSTANCE;
					Iterator<Entry<String, SettingsItem>> iterator = map.entrySet().iterator();
					while (iterator.hasNext()) {
						Entry<String, SettingsItem> entry = iterator.next();
						if (!settingsItems.containsKey(entry.getKey())) {
							settingsItems.put(entry.getKey(), entry.getValue());
						}
					}
				} catch (ParserConfigurationException | SAXException | IOException | URISyntaxException e) {
					LogService.getRoot().log(Level.WARNING, "Could not parse XML settings file: "
							+ SettingsXmlHandler.SETTINGS_XML_FILE + " of extension " + getName() + " " + getVersion());
					// Must not throw an exception, as the settings work without the structure
					// inside XML.
				}
			}

		}

	}

	/** Calls {@link #updateSettingsDescriptions()} for all extensions, which are not disabled. */
	private static void updateAllSettingsDescriptions() {
		for (Iterator<Plugin> iterator = getAllPlugins().iterator(); iterator.hasNext();) {
			Plugin plugin = iterator.next();
			if (plugin != null && !plugin.disabled) {
				long start = System.currentTimeMillis();
				plugin.updateSettingsDescriptions();
				recordLoadingTime(plugin.getExtensionId(), start);
			}
		}
	}

	/**
	 * Updates descriptions of {@link ParameterType} objects, which are defined in the i18n
	 * settings.
	 */
	private void updateSettingsDescriptions() {
		if (settingsRessourceBundle != null) {
			for (String settingsKey : settingsRessourceBundle.keySet()) {
				if (settingsKey.endsWith(SettingsType.DESCRIPTION.toString())) {

					// Extract key of ParameterType by removing suffix of description
					String parameterTypeKey = settingsKey.substring(0,
							settingsKey.length() - SettingsType.DESCRIPTION.toString().length());

					ParameterType parameterType = ParameterService.getParameterType(parameterTypeKey);
					if (parameterType != null) {
						String description = I18N.getSettingsMessage(parameterTypeKey, SettingsType.DESCRIPTION);

						// Only update description, if it is set in i18n
						if (!description.startsWith(settingsKey)) {
							parameterType.setDescription(description);
						}
					}
				}
			}
		}
	}

	/** Updates plugins from the server and returns a cache directory containing the jar files. */
	private static File updateWebstartPluginsCache() {
		// We hash the home URL to a directory name, so we don't have special characters.
		final String homeUrl = System.getProperty(RapidMiner.PROPERTY_HOME_REPOSITORY_URL);
		String dirName;
		try {
			final byte[] md5hash = MessageDigest.getInstance("MD5").digest(homeUrl.getBytes());
			dirName = DatatypeConverter.printBase64Binary(md5hash);
		} catch (NoSuchAlgorithmException e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.tools.plugin.Plugin.hashing_remote_url_error", e), e);
			return null;
		}

		File cacheDir = new File(ManagedExtension.getUserExtensionsDir(), dirName);
		cacheDir.mkdirs();
		File readmeFile = new File(cacheDir, "README.txt");
		try {
			Tools.writeTextFile(readmeFile,
					"This directory contains plugins downloaded from RapidMiner Server instance \n" + "  " + homeUrl + ".\n"
							+ "These plugins are only used if RapidMiner is started via WebStart from this \n"
							+ "server. You can delete the directory if you no longer need the cached plugins.");
		} catch (IOException e1) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.tools.plugin.Plugin.creating_file_error", readmeFile, e1), e1);
		}

		Document pluginsDoc;
		try {
			URL pluginsListUrl = new URL(homeUrl + "/RAWS/dependencies/resources.xml");
			pluginsDoc = XMLTools.parse(pluginsListUrl.openStream());
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.tools.plugin.Plugin.loading_extensions_list_error", e), e);
			return null;
		}

		Set<File> cachedFiles = new HashSet<>();
		NodeList pluginElements = pluginsDoc.getElementsByTagName("extension");
		boolean errorOccurred = false;
		for (int i = 0; i < pluginElements.getLength(); i++) {
			Element pluginElem = (Element) pluginElements.item(i);
			String pluginName = pluginElem.getTextContent();
			String pluginVersion = pluginElem.getAttribute("version");
			File pluginFile = new File(cacheDir, pluginName + "-" + pluginVersion + ".jar");
			cachedFiles.add(pluginFile);
			if (pluginFile.exists()) {
				LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.extension_found_cache_exists",
						pluginName);
			} else {
				LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.extension_found_downloading",
						pluginName);
				try {
					URL pluginUrl = new URL(homeUrl + "/RAWS/dependencies/plugins/" + pluginName);
					try (InputStream pluginURLStream = WebServiceTools.openStreamFromURL(pluginUrl);
							FileOutputStream pluginFileStream = new FileOutputStream(pluginFile)) {
						Tools.copyStreamSynchronously(pluginURLStream, pluginFileStream, true);
					}
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.plugin.Plugin.downloading_extension_error", e), e);
					errorOccurred = true; // Don't clear unknown files in this case.
				}
			}
		}
		// clear out of date cache files unless error occurred
		if (!errorOccurred) {
			for (File file : cacheDir.listFiles()) {
				if (file.getName().equals("README.txt")) {
					continue;
				}
				if (!cachedFiles.contains(file)) {
					LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.plugin.Plugin.deleting_obsolete_file",
							file);
					file.delete();
				}
			}
		}
		return cacheDir;
	}

	/** Specifies whether plugins should be initialized on startup. */
	public static void setInitPlugins(boolean init) {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS, Boolean.toString(init));
	}

	/** Specifies the main directory to scan for extensions. */
	public static void setPluginLocation(String directory) {
		ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_INIT_PLUGINS_LOCATION, directory);
	}

	/**
	 * Adds a directory to scan for RapidMiner extensions when initializing the RapidMiner
	 * extensions.
	 *
	 * @param directory
	 *            the absolute path to the directory which contains the RapidMiner extensions
	 */
	public static void addAdditionalExtensionDir(String directory) {
		additionalExtensionDirs.add(directory);
	}

	/**
	 * Returns the prefix to be used in the operator keys (namespace). This is also used for the
	 * Wiki URL.
	 */
	public String getPrefix() {
		return this.prefix;
	}

	public JarFile getArchive() {
		return archive;
	}

	public File getFile() {
		return file;
	}

	public String getExtensionId() {
		return extensionId;
	}

	/**
	 * @return the directory where globally installed extension files are expected.
	 */
	public static File getPluginLocation() throws IOException {
		return FileSystemService.getLibraryFile("plugins");
	}

	/**
	 * This returns the Icon of the extension or null if not present.
	 */
	public ImageIcon getExtensionIcon() {
		URL iconURL = classLoader.findResource("META-INF/icon.png");
		if (iconURL != null) {
			return new ImageIcon(iconURL);
		}
		return null;
	}

	/**
	 * <strong>Experimental method.</strong> Registers this plugin at runtime.
	 */
	public void reregister() {
		getAllPlugins().add(this);
		try {
			registerDescriptions();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.register_desc_runtime_failed",
					new Object[] { this.getName(), e.getMessage() });
		}
		try {
			registerOperators();
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.plugin.Plugin.register_operators_runtime_failed",
					new Object[] { this.getName(), e.getMessage() });
		}
	}

	/**
	 * <strong>Experimental method.</strong> Finishes the initializing of this plugin.
	 */
	public void finishReregister() {
		buildFinalClassLoader();
		callInitMethod("initPlugin", new Class[] {}, new Object[] {}, false);
		callInitMethod("initGui", new Class[] { MainFrame.class }, new Object[] { RapidMinerGUI.getMainFrame() }, false);
		callInitMethod("initFinalChecks", new Class[] {}, new Object[] {}, false);
		callInitMethod("initPluginManager", new Class[] {}, new Object[] {}, false);
	}

	/**
	 * <strong>Experimental method.</strong> Unregisters this plugin, all of its {@link Operator}s,
	 * and calls tearDown() and optionally tearDownGUI(MainFrame) on the
	 * {@link #pluginInitClassName}. Finally, removes the plugin from {@link #ALL_PLUGINS}.
	 */
	public void tearDown() {
		OperatorService.unregisterAll(this);
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			callInitMethod("tearDownGUI", new Class[] { MainFrame.class }, new Object[] { RapidMinerGUI.getMainFrame() },
					false);
		}
		callInitMethod("tearDown", new Class[0], new Object[0], false);
		try {
			classLoader.close();
		} catch (IOException e) {
			// files could not be closed
		}
		ALL_PLUGINS.remove(this);
	}

	/**
	 * Returns the resource source of this plugin.
	 *
	 * @return the source, never {@code null}
	 * @since 9.0.0
	 */
	public ResourceSource getResourceSource() {
		return resourceSource;
	}
}
