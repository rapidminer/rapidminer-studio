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
package com.rapidminer.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.RapidMiner;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.processeditor.search.OperatorGlobalSearch;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.OperatorCreationHook;
import com.rapidminer.search.GlobalSearchable;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.documentation.OperatorDocumentation;
import com.rapidminer.tools.documentation.XMLOperatorDocBundle;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.rapidminer.tools.plugin.Plugin;


/**
 * This class maintains all registered operators in the current context. There exists a listener
 * concept that will alert all listeners, if new operators are added or removed.
 *
 * It provides convenience methods for creating new {@link Operator}s. See the description of the
 * {@link #createOperator(Class)} method. Please mind that {@link Operator}s that are built from an
 * {@link GenericOperatorFactory} cannot be constructed with this method. Please use
 * {@link #createOperator(String)} method instead, that can be passed the {@link Operator}'s key.
 *
 *
 * <p>
 * This class also reads the xml definitions of the RapidMiner Studio Core and Extension operators.
 * These descriptions are entries in a XML file like OperatorsCore.xml.
 * </p>
 *
 * @author Ingo Mierswa, Simon Fischer, Sebastian Land
 */
public class OperatorService {

	/**
	 * The interface for all Listener to the {@link OperatorService}.
	 *
	 * @author Sebastian Land
	 */
	public static interface OperatorServiceListener {

		/**
		 * This will be called if an operator is registered.
		 *
		 * ATTENTION!!! You must ensure that bundle might be null!
		 */
		public void operatorRegistered(OperatorDescription description, OperatorDocBundle bundle);

		/**
		 * This method will be called if an operator is removed.
		 */
		public void operatorUnregistered(OperatorDescription description);
	}

	public static final String RAPID_MINER_CORE_PREFIX = "RapidMiner Studio Core";
	public static final String RAPID_MINER_CORE_NAMESPACE = "core";

	private static final String OPERATORS_XML = "OperatorsCore.xml";

	public static final String OPERATOR_BLACKLIST_KEY = "rapidminer.operator.blacklist";

	private static Set<String> operatorBlacklist = Collections.emptySet();

	private static final LinkedList<WeakReference<OperatorServiceListener>> listeners = new LinkedList<>();
	private static final List<OperatorCreationHook> operatorCreationHooks = new LinkedList<>();
	/**
	 * Maps operator keys as defined in the OperatorsCore.xml to operator descriptions.
	 */
	private static final Map<String, OperatorDescription> KEYS_TO_DESCRIPTIONS = new HashMap<>();

	/** Set of all Operator classes registered. */
	private static final Set<Class<? extends Operator>> REGISTERED_OPERATOR_CLASSES = new HashSet<>();

	/** The Map for all IO objects (maps short names on classes). */
	private static final Map<String, Class<? extends IOObject>> IO_OBJECT_NAME_MAP = new TreeMap<>();

	/** Maps deprecated operator names to new names. */
	private static final Map<String, String> DEPRECATION_MAP = new HashMap<>();

	private static final GroupTreeRoot groupTreeRoot = new GroupTreeRoot();

	private static GlobalSearchable operatorSearchable;

	/** Updates the operator blacklist when it changes */
	private static final ParameterChangeListener UPDATE_BLACKLIST_LISTENER = new ParameterChangeListener() {

		@Override
		public void informParameterSaved() {
			// not necessary
		}

		@Override
		public void informParameterChanged(String key, String value) {
			if (OPERATOR_BLACKLIST_KEY.equals(key)) {
				updateBlacklist();
			}
		}
	};

	/** Updates the operator descriptions when the license changes */
	private static final LicenseManagerListener ACTIVE_LICENSE_CHANGED = new LicenseManagerListener() {

		@Override
		public <S, C> void handleLicenseEvent(final LicenseEvent<S, C> event) {
			if (event.getType() == LicenseEvent.LicenseEventType.ACTIVE_LICENSE_CHANGED) {
				refreshOperatorDescriptions();
			}
		}
	};


	public static void init() {
		// this serves 2 purposes:
		// 1: Add Global Search capabilities for operators by registering OperatorServiceListener
		// 2: Keep reference so that weak listener reference is not killed via GC
		operatorSearchable = new OperatorGlobalSearch();

		URL mainOperators = getMainOperators();
		if (mainOperators == null) {
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.tools.OperatorService.main_operator_descripton_file_not_found",
					new Object[] { Tools.RESOURCE_PREFIX, OPERATORS_XML });
		} else {
			registerOperators(mainOperators, null, null);
		}

		// additional operators from starting parameter
		String additionalOperators = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL);
		// check whether the bug that System.getProperty(key) returns key instead of null appears
		if (additionalOperators != null
				&& additionalOperators.contains(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL)) {
			return;
		}
		if (additionalOperators != null && !additionalOperators.isEmpty()) {
			if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
				LogService.getRoot().log(Level.CONFIG,
						"com.rapidminer.tools.OperatorService.execution_mode_does_not_permitting_accessing_file_system",
						new Object[] { RapidMiner.getExecutionMode(), additionalOperators });
			} else {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.OperatorService.loading_additional_operators",
						new Object[] { RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL, additionalOperators });
				String[] additionalOperatorFileNames = additionalOperators.split(File.pathSeparator);
				for (String additionalOperatorFileName : additionalOperatorFileNames) {
					File additionalOperatorFile = new File(additionalOperatorFileName);
					if (additionalOperatorFile.exists()) {
						try (FileInputStream in = new FileInputStream(additionalOperatorFile)) {
							OperatorService.registerOperators(additionalOperatorFile.getPath(), in, null);
						} catch (IOException e) {
							LogService.getRoot().log(Level.SEVERE,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.tools.OperatorService.reading_additional_operator_file_error",
											additionalOperatorFile),
									e);
						}
					} else {
						LogService.getRoot().log(Level.SEVERE,
								"com.rapidminer.tools.OperatorService.operator_description_file_not_found",
								additionalOperatorFileName);
					}
				}
			}
		}

		// loading operators from plugins
		Plugin.registerAllPluginOperators();

		// add parent folder as operator tag
		addParentFolderOperatorTags();

		LogService.getRoot().log(Level.FINE,
				"com.rapidminer.tools.OperatorService.number_of_registered_operator_classes_and_descriptions",
				new Object[] { REGISTERED_OPERATOR_CLASSES.size(), KEYS_TO_DESCRIPTIONS.size(), DEPRECATION_MAP.size() });

		updateBlacklist();
		ParameterService.removeParameterChangeListener(UPDATE_BLACKLIST_LISTENER);
		ParameterService.registerParameterChangeListener(UPDATE_BLACKLIST_LISTENER);
		ProductConstraintManager.INSTANCE.removeLicenseManagerListener(ACTIVE_LICENSE_CHANGED);
		ProductConstraintManager.INSTANCE.registerLicenseManagerListener(ACTIVE_LICENSE_CHANGED);
	}

	private static void updateBlacklist() {
		String blacklistString = ParameterService.getParameterValue(OPERATOR_BLACKLIST_KEY);
		if (blacklistString != null) {
			Set<String> newOperatorBlacklist = new HashSet<>(Arrays.asList(blacklistString.trim().split("\\s*,\\s*")));

			//Calculate the Symmetric Difference between the sets
			Set<String> symmetricDifference = new HashSet<>(operatorBlacklist);
			symmetricDifference.addAll(newOperatorBlacklist);
			Set<String> tmp = new HashSet<>(operatorBlacklist);
			tmp.retainAll(newOperatorBlacklist);
			symmetricDifference.removeAll(tmp);

			operatorBlacklist = newOperatorBlacklist;

			//Update the operators whose status changed
			for (String blacklistedKey: symmetricDifference) {
				OperatorDescription blacklistedOperator = OperatorService.getOperatorDescription(blacklistedKey);
				if (blacklistedOperator != null) {
					blacklistedOperator.setIconName(blacklistedOperator.getIconName());
				}
			}
		}
	}

	/**
	 * Adds parent folder name of each operator to the operator tags.
	 */
	private static void addParentFolderOperatorTags() {
		for (String operatorKey : getOperatorKeys()) {
			OperatorDescription operatorDescription = getOperatorDescription(operatorKey);
			if (operatorDescription == null) {
				continue;
			}
			if (ProcessRootOperator.class.equals(operatorDescription.getOperatorClass())) {
				// no tags for the root process
				continue;
			}
			GroupTree subTree = groupTreeRoot.findGroup(operatorDescription.getGroup());
			if (subTree == null) {
				continue;
			}
			String groupName = subTree.getName();
			if (groupName != null && !groupName.trim().isEmpty()) {
				if (!operatorDescription.getTags().contains(groupName.trim())) {
					OperatorDocumentation operatorDocumentation = operatorDescription.getOperatorDocumentation();
					ArrayList<String> updatedTags = new ArrayList<>(operatorDocumentation.getTags().size() + 1);
					updatedTags.addAll(operatorDocumentation.getTags());
					updatedTags.add(groupName.trim());
					operatorDocumentation.setTags(updatedTags);
				}
			}
		}
	}

	/**
	 * Refreshes all operator descriptions including the reloading of operator icons.
	 */
	public static void refreshOperatorDescriptions() {
		for (OperatorDescription desc : KEYS_TO_DESCRIPTIONS.values()) {
			desc.refresh();
		}
	}

	public static void registerOperators(URL operatorsXML, ClassLoader classLoader, Plugin plugin) {
		InputStream inputStream = null;
		try {
			if (operatorsXML != null) {
				inputStream = operatorsXML.openStream();
			}
		} catch (IOException e) {
			// LogService.getRoot().log(Level.WARNING,
			// "Cannot open stream to operator description file " + operatorsXML + ": " + e, e);
			LogService.getRoot().log(Level.WARNING,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.OperatorService.opening_stream_to_operator_description_file_error",
							operatorsXML, e),
					e);
			return;
		}
		registerOperators(OPERATORS_XML, inputStream, null, plugin);
	}

	/**
	 * Registers all operators from a given XML input stream. Closes the stream.
	 */
	public static void registerOperators(String name, InputStream operatorsXML, ClassLoader classLoader) {
		registerOperators(name, operatorsXML, classLoader, null);
	}

	public static void registerOperators(String name, InputStream operatorsXML, ClassLoader classLoader, Plugin provider) {
		// register operators
		if (classLoader == null) {
			classLoader = OperatorService.class.getClassLoader();
		}
		// LogService.getRoot().config("Loading operators from '" + name + "'.");
		LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.OperatorService.loading_operators", name);
		String version = null;
		Document document = null;
		try {
			document = XMLTools.createDocumentBuilder().parse(operatorsXML);
			if (!document.getDocumentElement().getTagName().toLowerCase().equals("operators")) {
				LogService.getRoot().log(Level.SEVERE,
						"com.rapidminer.tools.OperatorService.operator_description_file_outermost_tag", name);
				return;
			}
			version = document.getDocumentElement().getAttribute("version");
			if (version.startsWith("5.") || version.startsWith("6.") || version.startsWith("7.")) {
				parseOperators(document, classLoader, provider);
			} else {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.OperatorService.operator_description_file_wrong_version", name, version));
			}
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE,
					I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.OperatorService.operator_description_file_reading_error", name,
							e.getMessage()),
					e);
			return;
		} finally {
			try {
				operatorsXML.close();
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.OperatorService.error_closing_stream", e.getMessage()), e);
			}
		}
	}

	private static void parseOperators(Document document, ClassLoader classLoader, Plugin provider)
			throws XMLException, OperatorCreationException {
		String docBundle = document.getDocumentElement().getAttribute("docbundle");
		OperatorDocBundle bundle;
		if (docBundle == null || docBundle.isEmpty()) {
			bundle = null;
			String providerName;
			if (provider == null) {
				providerName = "RapidMiner core";
			} else {
				providerName = provider.getName();
			}
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.OperatorService.operators_no_attached_documention",
					providerName);
		} else {
			bundle = XMLOperatorDocBundle.load(classLoader, docBundle);
		}

		GroupTree root = groupTreeRoot;

		/*
		 * Check whether the operators should be placed in the Extensions top level folder
		 */
		if (provider != null && provider.useExtensionTreeRoot()) {
			// get or create the Extensions group
			root = groupTreeRoot.getOrCreateSubGroup(OperatorDescription.EXTENSIONS_GROUP_IDENTIFIER, bundle);

			// get or create the Extension name group
			root = root.getOrCreateSubGroup(provider.getName(), bundle);
		}

		parseOperators(root, document.getDocumentElement(), classLoader, provider, bundle);
	}

	private static void parseOperators(GroupTree currentGroup, Element groupElement, ClassLoader classLoader,
			Plugin provider, OperatorDocBundle bundle) throws XMLException, OperatorCreationException {
		NodeList children = groupElement.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElement = (Element) child;
				if (childElement.getTagName().equals("group")) {
					String name = childElement.getAttribute("key");
					String icon = XMLTools.getTagContents(childElement, "icon");
					GroupTree newTree;
					if (name != null && !name.isEmpty()) {
						newTree = currentGroup.getOrCreateSubGroup(name, bundle);
					} else {
						newTree = currentGroup;
					}
					if (icon != null && icon.length() > 0) {
						newTree.setIconName(icon);
					} else {
						if (newTree.getIconName() == null || newTree.getIconName().length() == 0) {
							newTree.setIconName(currentGroup.getIconName());
						}
					}
					parseOperators(newTree, childElement, classLoader, provider, bundle);
				} else if (childElement.getTagName().equals("operator")) {
					String extensionId = "RapidMiner Core";
					if (provider != null) {
						extensionId = provider.getExtensionId();
					}
					try {
						String groupKey = currentGroup.getFullyQualifiedKey();
						OperatorDescription desc = new OperatorDescription(groupKey, childElement, classLoader, provider,
								bundle);
						desc.setUseExtensionTreeRoot(provider != null && provider.useExtensionTreeRoot());
						registerOperator(desc, bundle);
						if (desc.getIconName() == null && currentGroup.getIconName() != null) {
							desc.setIconName(currentGroup.getIconName());
						}
						if (desc.getReplacedKeys() != null) {
							for (String replaces : desc.getReplacedKeys()) {
								DEPRECATION_MAP.put(replaces, desc.getKey());
							}
						}
					} catch (ClassNotFoundException e) {
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.OperatorService.operator_description_creating_error",
										extensionId, XMLTools.getTagContents(childElement, "key", false)),
								e);
					} catch (NoClassDefFoundError e) {
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.OperatorService.operator_description_creating_error",
										extensionId, XMLTools.getTagContents(childElement, "key", false)),
								e);
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.OperatorService.operator_registering_error", extensionId,
										XMLTools.getTagContents(childElement, "key", false)),
								e);
					} catch (AbstractMethodError e) {
						LogService.getRoot().log(Level.WARNING,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.OperatorService.operator_registering_error", extensionId,
										XMLTools.getTagContents(childElement, "key", false)),
								e);
					} catch (Throwable e) {
						// Yes, this is evil. However, it is the only way we can prevent errors due
						// to
						// incompatible RapidMiner / extension updates
						LogService.getRoot().log(Level.SEVERE,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.tools.OperatorService.operator_registering_error", extensionId,
										XMLTools.getTagContents(childElement, "key", false)),
								e);
					}
				} else if (childElement.getTagName().equals("factory")) {
					String factoryClassName = childElement.getTextContent().trim();
					if (factoryClassName == null || factoryClassName.isEmpty()) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.tools.OperatorService.malformed_operator_descriptor_factory");
					} else {
						Class<?> factoryClass = null;
						try {
							factoryClass = Class.forName(factoryClassName, true, classLoader);
						} catch (Throwable e) {
							// LogService.getRoot().warning("Operator factory class '" +
							// factoryClassName + "' not found!");
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.tools.OperatorService.operator_factory_class_not_found",
											factoryClassName),
									e);
						}
						if (factoryClass != null) {
							if (GenericOperatorFactory.class.isAssignableFrom(factoryClass)) {
								GenericOperatorFactory factory = null;
								try {
									factory = (GenericOperatorFactory) factoryClass.newInstance();
								} catch (Exception e) {
									LogService.getRoot().log(Level.WARNING,
											"com.rapidminer.tools.OperatorService.operator_instantiating_error",
											factoryClass.getName());
								} catch (Throwable e) {
									// Yes, this is evil. However, it is the only way we can prevent
									// errors due to
									// incompatible RapidMiner / extension updates
									LogService.getRoot().log(Level.SEVERE,
											I18N.getMessage(LogService.getRoot().getResourceBundle(),
													"com.rapidminer.tools.OperatorService.failed_to_register_oprator", e),
											e);
								}
								LogService.getRoot().log(Level.CONFIG,
										"com.rapidminer.tools.OperatorService.creating_operators_from_factory",
										factoryClassName);
								try {
									if (factory != null) {
										factory.registerOperators(classLoader, provider);
									}
								} catch (Exception e) {
									LogService.getRoot().log(Level.WARNING,
											I18N.getMessage(LogService.getRoot().getResourceBundle(),
													"com.rapidminer.tools.OperatorService.error_registering_oprators_from",
													factoryClass.getName(), e),
											e);
								} catch (Throwable e) {
									// Yes, this is evil. However, it is the only way we can prevent
									// errors due to
									// incompatible RapidMiner / extension updates
									LogService.getRoot().log(Level.SEVERE,
											I18N.getMessage(LogService.getRoot().getResourceBundle(),
													"com.rapidminer.tools.OperatorService.failed_to_register_oprator", e),
											e);
								}
							} else {
								LogService.getRoot().log(Level.WARNING,
										"com.rapidminer.tools.OperatorService.malformed_operator_descriptor_subclasses",
										factoryClassName);
							}
						}
					}
				} else if (childElement.getTagName().equals("icon")) {
					// why do we ignore this?
				} else {
					throw new XMLException("Illegal tag in operator descrioption file: " + childElement.getTagName());
				}
			}
		}
	}

	/**
	 * This method does the same as {@link #registerOperator(OperatorDescription,
	 * OperatorDocBundle))}, but without an {@link OperatorDocBundle} groups will not have a name or
	 * icon. This method remains for compatibility of older extensions.
	 */
	@Deprecated
	public static void registerOperator(OperatorDescription description) throws OperatorCreationException {
		registerOperator(description, null);
	}

	/**
	 * Registers the given operator description. Please note that two different descriptions must
	 * not have the same name. Otherwise the second description overwrite the first in the
	 * description map.
	 *
	 * If there's no icon defined for the given {@link OperatorDescription}, the group icon will be
	 * set here.
	 *
	 * @param bundle
	 *            might be null. If existing will be used for GroupCreation / Icon settings
	 * @throws OperatorCreationException
	 */
	public static void registerOperator(OperatorDescription description, OperatorDocBundle bundle)
			throws OperatorCreationException {
		// check if this operator was not registered earlier
		OperatorDescription oldDescription = KEYS_TO_DESCRIPTIONS.get(description.getKey());
		if (oldDescription != null) {
			// LogService.getRoot().warning("Operator key '" + description.getKey() +
			// "' was already registered for class " +
			// oldDescription.getOperatorClass().getName() + ". Overwriting with " +
			// description.getOperatorClass() + ".");
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.OperatorService.operator_key_already_registered",
					new Object[] { description.getKey(), oldDescription.getOperatorClass().getName(),
							description.getOperatorClass() });
		}

		// check if icon already was set.
		if (!description.isIconDefined()) {
			description.setIconName(groupTreeRoot.findOrCreateGroup(description.getGroup(), bundle).getIconName());
		}

		// register in maps
		KEYS_TO_DESCRIPTIONS.put(description.getKey(), description);
		REGISTERED_OPERATOR_CLASSES.add(description.getOperatorClass());

		// TODO: Check if still necessary.
		Operator currentOperator = description.createOperatorInstance();
		currentOperator.assumePreconditionsSatisfied();
		currentOperator.transformMetaData();
		checkIOObjects(currentOperator.getInputPorts());
		checkIOObjects(currentOperator.getOutputPorts());

		// inform listener
		invokeOperatorRegisteredListener(description, bundle);
	}

	/**
	 * This method can be used to dynamically remove Operators from the number of defined operators.
	 */
	public static void unregisterOperator(OperatorDescription description) {
		KEYS_TO_DESCRIPTIONS.remove(description.getKey());
		REGISTERED_OPERATOR_CLASSES.remove(description.getOperatorClass());

		// inform all listener including GroupTree
		invokeOperatorUnregisteredListener(description);
	}

	/**
	 * <strong>Experimental method.</strong> Unregisters all operators for the given Plugin.
	 */
	public static void unregisterAll(Plugin plugin) {
		for (String opKey : new HashSet<>(OperatorService.getOperatorKeys())) {
			OperatorDescription desc = OperatorService.getOperatorDescription(opKey);
			if (desc.getProvider() == plugin) {
				unregisterOperator(desc);
			}
		}
	}

	/**
	 * Checks if the classes generated by these ports are already registered and registers them if
	 * not.
	 */
	private static void checkIOObjects(Ports<? extends Port> ports) {
		List<Class<? extends IOObject>> result = new LinkedList<>();
		for (Port port : ports.getAllPorts()) {
			try {
				if (port.getMetaData(MetaData.class) != null) {
					result.add(port.getMetaData(MetaData.class).getObjectClass());
				}
			} catch (IncompatibleMDClassException e) {
				// cannot happen since MetaData is the base class for all meta data
			}
		}
		registerIOObjects(result);
	}

	/** Checks if the given classes are already registered and adds them if not. */
	public static void registerIOObjects(Collection<Class<? extends IOObject>> objects) {
		for (Class<? extends IOObject> currentClass : objects) {
			String current = currentClass.getName();
			IO_OBJECT_NAME_MAP.put(current.substring(current.lastIndexOf(".") + 1), currentClass);
		}
	}

	/** Returns a sorted set of all short IO object names. */
	public static Set<String> getIOObjectsNames() {
		// TODO: Check if this can be replaced!
		// return RendererService.getAllRenderableObjectNames();
		return IO_OBJECT_NAME_MAP.keySet();
	}

	/** Returns the class for the short name of an IO object. */
	public static Class<? extends IOObject> getIOObjectClass(String name) {
		// TODO: CHECK
		// assert (IO_OBJECT_NAME_MAP.get(name).equals(RendererService.getClass(name)));
		//
		// return RendererService.getClass(name);
		return IO_OBJECT_NAME_MAP.get(name);
	}

	/**
	 * Returns a collection of all operator keys. Use {@link #getOperatorKeys()} instead.
	 */
	@Deprecated
	public static Set<String> getOperatorNames() {
		return KEYS_TO_DESCRIPTIONS.keySet();
	}

	/**
	 * Returns a collection of all operator keys.
	 */
	public static Set<String> getOperatorKeys() {
		return KEYS_TO_DESCRIPTIONS.keySet();
	}

	/**
	 * Returns the group hierarchy of all operators. This will automatically reflect changes on the
	 * registered Operators. You might register as listener to {@link OperatorService} in order to
	 * receive registration or unregistration eventsevents
	 */
	public static GroupTree getGroups() {
		return groupTreeRoot;
	}

	// ================================================================================
	// Operator Factory Methods
	// ================================================================================

	/**
	 * Returns the operator descriptions for the operators which uses the given class. Performs a
	 * linear search through all operator descriptions.
	 */
	public static OperatorDescription[] getOperatorDescriptions(Class<?> clazz) {
		if (clazz == null) {
			return new OperatorDescription[0];
		}
		List<OperatorDescription> result = new ArrayList<>(1);
		for (OperatorDescription current : KEYS_TO_DESCRIPTIONS.values()) {
			if (current.getOperatorClass().equals(clazz)) {
				result.add(current);
			}
		}
		return result.toArray(new OperatorDescription[result.size()]);
	}

	/**
	 * Returns the operator description for a given key from the operators.xml file, e.g.
	 * &quot;read_csv&quot; for the Read CSV operator.
	 */
	public static OperatorDescription getOperatorDescription(String key) {
		return KEYS_TO_DESCRIPTIONS.get(key);
	}

	/**
	 * Use this method to create an operator from the given class name (from operator description
	 * file operators.xml, not from the Java class name). For most operators, is is recommended to
	 * use the method {@link #createOperator(Class)} which can be checked during compile time. This
	 * is, however, not possible for some generic operators like the Weka operators. In that case,
	 * you have to use this method with the argument from the operators.xml file, e.g.
	 * <tt>createOperator(&quot;J48&quot;)</tt> for a J48 decision tree learner.
	 */
	public static Operator createOperator(String typeName) throws OperatorCreationException {
		OperatorDescription description = getOperatorDescription(typeName);
		if (description == null) {
			throw new OperatorCreationException(OperatorCreationException.NO_DESCRIPTION_ERROR, typeName, null);
		}
		return createOperator(description);
	}

	/** Use this method to create an operator of a given description object. */
	public static Operator createOperator(OperatorDescription description) throws OperatorCreationException {
		return description.createOperatorInstance();
	}

	/**
	 * <p>
	 * Use this method to create an operator from an operator class. This is the only method which
	 * ensures operator existence checks during compile time (and not during runtime) and the usage
	 * of this method is therefore the recommended way for operator creation.
	 * </p>
	 *
	 * <p>
	 * It is, however, not possible to create some generic operators with this method (this mainly
	 * applies to the Weka operators). Please use the method {@link #createOperator(String)} for
	 * those generic operators.
	 * </p>
	 *
	 * <p>
	 * If you try to create a generic operator with this method, the OperatorDescription will not be
	 * unique for the given class and an OperatorCreationException is thrown.
	 * </p>
	 *
	 * <p>
	 * Please note that is is not necessary to cast the operator to the desired class.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Operator> T createOperator(Class<T> clazz) throws OperatorCreationException {
		OperatorDescription[] descriptions = getOperatorDescriptions(clazz);
		if (descriptions.length == 0) {
			throw new OperatorCreationException(OperatorCreationException.NO_DESCRIPTION_ERROR, clazz.getName(), null);
		} else if (descriptions.length > 1) {
			List<OperatorDescription> nonDeprecated = new LinkedList<>();
			for (OperatorDescription od : descriptions) {
				if (od.getDeprecationInfo() == null) {
					nonDeprecated.add(od);
				}
			}
			if (nonDeprecated.size() > 1) {
				throw new OperatorCreationException(OperatorCreationException.NO_UNIQUE_DESCRIPTION_ERROR, clazz.getName(),
						null);
			}
			return (T) nonDeprecated.get(0).createOperatorInstance();
		} else {
			return (T) descriptions[0].createOperatorInstance();
		}
	}

	/**
	 * Returns a replacement if the given operator class is deprecated, and null otherwise. The
	 * deprecated Key is the key with that this operator was used in RapidMiner 4.x
	 */
	public static String getReplacementForDeprecatedClass(String deprecatedKey) {
		return DEPRECATION_MAP.get(deprecatedKey);
	}

	/** Specifies a list of files to be loaded as operator descriptors. */
	public static void setAdditionalOperatorDescriptors(String... files) {
		if (files == null || files.length == 0) {
			System.clearProperty(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL);
			return;
		}
		StringBuffer buf = new StringBuffer();
		boolean first = true;
		for (String file : files) {
			if (!first) {
				buf.append(File.pathSeparator);
			} else {
				first = false;
			}
			buf.append(file);
		}
		System.setProperty(RapidMiner.PROPERTY_RAPIDMINER_OPERATORS_ADDITIONAL, buf.toString());
	}

	/** Returns the main operator description file (XML). */
	private static URL getMainOperators() {
		String resource;
		String operatorsXML = System.getProperty(RapidMiner.PROPERTY_RAPIDMINER_INIT_OPERATORS);
		if (operatorsXML != null) {
			resource = operatorsXML;
			// LogService.getRoot().config("Main operator descriptor overrideen by system property.
			// Using "
			// + operatorsXML + ".");
			LogService.getRoot().log(Level.CONFIG,
					"com.rapidminer.tools.OperatorService.main_operator_descriptor_overrideen", operatorsXML);
		} else {
			resource = "/" + Tools.RESOURCE_PREFIX + OPERATORS_XML;
		}
		return OperatorService.class.getResource(resource);
	}

	/*
	 * OperatorCreationHooks
	 */

	/**
	 * This method adds an {@link OperatorCreationHook} that will be informed whenever an
	 * {@link Operator} instance is created.
	 */
	public static void addOperatorCreationHook(OperatorCreationHook operatorCreationHook) {
		operatorCreationHooks.add(operatorCreationHook);
	}

	/**
	 * This method must be called by each {@link OperatorDescription#createOperatorInstance()} call.
	 * It is used for example for statistics purpose.
	 */
	public static void invokeCreationHooks(Operator operator) {
		for (OperatorCreationHook hook : operatorCreationHooks) {
			try {
				hook.operatorCreated(operator);
			} catch (RuntimeException e) {
				// LogService.getRoot().log(Level.WARNING, "Error in operator creation hook: " + e,
				// e);
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.OperatorService.error_in_operation_creation_hook", e), e);
			}
		}
	}

	/*
	 * Listener to changes in available Operators
	 */
	/**
	 * This method can be used to add an listener to the OperatorService that is informed whenever
	 * the set of available operators changes. Internally WeakReferences are used so that there's no
	 * need to deregister listeners.
	 */
	public static void addOperatorServiceListener(OperatorServiceListener listener) {
		listeners.add(new WeakReference<>(listener));
	}

	/**
	 * Checks if any blacklisted operators exist
	 *
	 * @return {@code true} if the blacklisted operators exist
	 * @since 9.0.0
	 */
	public static boolean hasBlacklistedOperators(){
		return !operatorBlacklist.isEmpty();
	}

	/**
	 * Checks if the operator with the given key is blacklisted.
	 * @param opKey
	 * @return {@code true} if the operator is blacklisted, or {@code false} if it is not blacklisted.
	 * @since 9.0.0
	 */
	public static boolean isOperatorBlacklisted(String opKey){
		return operatorBlacklist.contains(opKey);
	}

	/**
	 * This method will inform all listeners of a change in the available operators.
	 */
	private static void invokeOperatorRegisteredListener(OperatorDescription description, OperatorDocBundle bundle) {
		List<WeakReference<OperatorServiceListener>> listenersCopy = new LinkedList<>(listeners);
		for (WeakReference<OperatorServiceListener> listenerRef : listenersCopy) {
			OperatorServiceListener operatorServiceListener = listenerRef.get();
			if (operatorServiceListener != null) {
				operatorServiceListener.operatorRegistered(description, bundle);
			}
		}
		Iterator<WeakReference<OperatorServiceListener>> iterator = listenersCopy.iterator();
		while (iterator.hasNext()) {
			OperatorServiceListener operatorServiceListener = iterator.next().get();
			if (operatorServiceListener == null) {
				iterator.remove();
			}
		}

	}

	/**
	 * This method will inform all listeners of a change in the available operators.
	 */
	private static void invokeOperatorUnregisteredListener(OperatorDescription description) {
		List<WeakReference<OperatorServiceListener>> listenersCopy = new LinkedList<>(listeners);
		for (WeakReference<OperatorServiceListener> listenerRef : listenersCopy) {
			OperatorServiceListener operatorServiceListener = listenerRef.get();
			if (operatorServiceListener != null) {
				operatorServiceListener.operatorUnregistered(description);
			}
		}
		Iterator<WeakReference<OperatorServiceListener>> iterator = listenersCopy.iterator();
		while (iterator.hasNext()) {
			OperatorServiceListener operatorServiceListener = iterator.next().get();
			if (operatorServiceListener == null) {
				iterator.remove();
			}
		}
	}

}
