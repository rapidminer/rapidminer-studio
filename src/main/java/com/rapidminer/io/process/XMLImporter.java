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
package com.rapidminer.io.process;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.VersionNumber.VersionNumberException;
import com.rapidminer.io.process.rules.AbstractGenericParseRule;
import com.rapidminer.io.process.rules.ChangeParameterValueRule;
import com.rapidminer.io.process.rules.DeleteAfterAutoWireRule;
import com.rapidminer.io.process.rules.DeleteUnnecessaryOperatorChainRule;
import com.rapidminer.io.process.rules.ExcelCellAddressParseRule;
import com.rapidminer.io.process.rules.ExchangeSubprocessesRule;
import com.rapidminer.io.process.rules.OperatorEnablerRepairRule;
import com.rapidminer.io.process.rules.ParseRule;
import com.rapidminer.io.process.rules.PassthroughShortcutRule;
import com.rapidminer.io.process.rules.RenamePlotterParametersRule;
import com.rapidminer.io.process.rules.ReplaceIOMultiplierRule;
import com.rapidminer.io.process.rules.ReplaceOperatorRule;
import com.rapidminer.io.process.rules.ReplaceParameterRule;
import com.rapidminer.io.process.rules.SetParameterRule;
import com.rapidminer.io.process.rules.SetRoleByNameRule;
import com.rapidminer.io.process.rules.SwitchListEntriesRule;
import com.rapidminer.io.process.rules.WireAllOperators;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.DummyOperator;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.ListDescription;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.UnknownParameterInformation;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Class that parses an XML DOM into an {@link Operator}.
 *
 * @author Simon Fischer
 *
 */
public class XMLImporter {

	public static final VersionNumber VERSION_RM_5 = new VersionNumber(5, 0);
	public static final VersionNumber VERSION_RM_6 = new VersionNumber(6, 0);
	public static final VersionNumber CURRENT_VERSION = VERSION_RM_6;

	private static final Set<String> IRRELEVANT_PARAMETERS = new HashSet<String>();

	static {
		IRRELEVANT_PARAMETERS.add("read_database.data_set_meta_data_information");
	}

	/**
	 * Encoding in which process files are written. UTF-8 is guaranteed to exist on any JVM, see
	 * javadoc of {@link Charset}.
	 */
	public static final Charset PROCESS_FILE_CHARSET = StandardCharsets.UTF_8;

	private static List<ParseRule> PARSE_RULES = new LinkedList<ParseRule>();
	private static HashMap<String, List<ParseRule>> OPERATOR_KEY_RULES_MAP = new HashMap<String, List<ParseRule>>();

	/** Reads the parse rules from parserules.xml */
	public static void init() {
		URL rulesResource = XMLImporter.class.getResource("/com/rapidminer/resources/parserules.xml");
		if (rulesResource != null) {
			// registering the core rules without name prefix
			importParseRules(rulesResource, null);
		} else {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.io.process.XMLImporter.cannot_find_default_parse_rules");
		}
	}

	/**
	 * This method adds the parse rules from the given resource to the import rule set. The operator
	 * name prefix describes the operators coming from plugins. The core operators do not have any
	 * name prefix, while the plugin operators are registered using <plugin>:<operatorname>
	 */
	public static void importParseRules(URL rulesResource, Plugin prover) {
		if (rulesResource == null) {
			throw new NullPointerException("Parserules resource must not be null.");
		} else {

			String operatorNamePrefix = "";
			if (prover != null) {
				operatorNamePrefix = prover.getPrefix() + ":";
			}

			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.io.process.XMLImporter.reading_parse_rules",
					rulesResource);
			try {
				Document doc = XMLTools.createDocumentBuilder().parse(rulesResource.openStream());
				if (!doc.getDocumentElement().getTagName().equals("parserules")) {
					LogService.getRoot().log(Level.SEVERE, "com.rapidminer.io.process.XMLImporter.xml_document_start_error",
							rulesResource);
				} else {
					NodeList operatorElements = doc.getDocumentElement().getChildNodes();
					for (int i = 0; i < operatorElements.getLength(); i++) {
						if (operatorElements.item(i) instanceof Element) {
							Element operatorElement = (Element) operatorElements.item(i);
							String operatorTypeName = operatorElement.getNodeName();

							NodeList ruleElements = operatorElement.getChildNodes();
							for (int j = 0; j < ruleElements.getLength(); j++) {
								if (ruleElements.item(j) instanceof Element) {
									PARSE_RULES.add(constructRuleFromElement(operatorNamePrefix + operatorTypeName,
											(Element) ruleElements.item(j)));
								}
							}
						}
					}
					LogService.getRoot().log(Level.FINE, "com.rapidminer.io.process.XMLImporter.found_rules",
							PARSE_RULES.size());
				}
			} catch (Exception e) {
				LogService.getRoot()
						.log(Level.SEVERE,
								I18N.getMessage(LogService.getRoot().getResourceBundle(),
										"com.rapidminer.io.process.XMLImporter.error_reading_parse_rules", rulesResource, e),
						e);
			}
		}
	}

	public static ParseRule constructRuleFromElement(String operatorTypeName, Element element) throws XMLException {
		ParseRule rule = null;
		AbstractGenericParseRule genericRule = null;
		if (element.getTagName().equals("replaceParameter")) {
			rule = new ReplaceParameterRule(operatorTypeName, element);
		} else if (element.getTagName().equals("deleteAfterAutowire")) {
			rule = new DeleteAfterAutoWireRule(operatorTypeName, element);
		} else if (element.getTagName().equals("setParameter")) {
			rule = new SetParameterRule(operatorTypeName, element);
		} else if (element.getTagName().equals("replaceParameterValue")) {
			rule = new ChangeParameterValueRule(operatorTypeName, element);
		} else if (element.getTagName().equals("replaceOperator")) {
			rule = new ReplaceOperatorRule(operatorTypeName, element);
		} else if (element.getTagName().equals("exchangeSubprocesses")) {
			rule = new ExchangeSubprocessesRule(operatorTypeName, element);
		} else if (element.getTagName().equals("wireSubprocess")) {
			rule = new WireAllOperators(operatorTypeName, element);
		} else if (element.getTagName().equals("switchListEntries")) {
			rule = new SwitchListEntriesRule(operatorTypeName, element);
		} else if (element.getTagName().equals("renamePlotterParameters")) {
			rule = new RenamePlotterParametersRule(operatorTypeName, element);
		} else if (element.getTagName().equals("replaceRoleParameter")) {
			rule = new SetRoleByNameRule(operatorTypeName, element);
		} else if (element.getTagName().equals("replaceByCellAddress")) {
			rule = new ExcelCellAddressParseRule(operatorTypeName, element);

			/*
			 * General rules. Will take care of theirselves.
			 */
		} else if (element.getTagName().equals("deleteUnnecessaryOperatorChain")) {
			genericRule = new DeleteUnnecessaryOperatorChainRule();
		} else if (element.getTagName().equals("passthroughShortcut")) {
			genericRule = new PassthroughShortcutRule();
		} else if (element.getTagName().equals("replaceIOMultiplier")) {
			genericRule = new ReplaceIOMultiplierRule();
		} else if (element.getTagName().equals("repairOperatorEnabler")) {
			genericRule = new OperatorEnablerRepairRule();
		} else {
			throw new XMLException("Unknown rule tag: <" + element.getTagName() + ">");
		}

		if (rule != null) {
			// registering rules applying to one single operator.
			List<ParseRule> rules = OPERATOR_KEY_RULES_MAP.get(operatorTypeName);
			if (rules == null) {
				rules = new LinkedList<ParseRule>();
				OPERATOR_KEY_RULES_MAP.put(operatorTypeName, rules);
			}
			rules.add(rule);
		}

		if (genericRule != null) {
			for (String applicableOperatorKeys : genericRule.getApplicableOperatorKeys()) {
				// registering rules applying to one single operator.
				List<ParseRule> rules = OPERATOR_KEY_RULES_MAP.get(applicableOperatorKeys);
				if (rules == null) {
					rules = new LinkedList<ParseRule>();
					OPERATOR_KEY_RULES_MAP.put(applicableOperatorKeys, rules);
				}
				rules.add(genericRule);
			}
		}
		return rule;
	}

	private final List<Runnable> jobsAfterAutoWire = new LinkedList<Runnable>();
	private final List<Runnable> jobsAfterTreeConstruction = new LinkedList<Runnable>();

	private boolean mustAutoConnect = false;

	private int total;
	private final ProgressListener progressListener;
	private int created = 0;

	private final StringBuilder messages = new StringBuilder();

	private boolean operatorAsDirectChildrenDeprecatedReported = false;

	/** Creates a new importer that reports progress to the given listener. */
	public XMLImporter(ProgressListener listener) {
		progressListener = listener;
	}

	/**
	 * This constructor will simply ignore the version. It will always use the one of RapidMiner.
	 */
	@Deprecated
	public XMLImporter(ProgressListener listener, int version) {
		this(listener);
	}

	public void addMessage(String msg) {
		LogService.getRoot().info(msg);
		messages.append("<li>");
		messages.append(msg);
		messages.append("</li>");
	}

	public void parse(Document doc, Process process, List<UnknownParameterInformation> unknownParameters)
			throws XMLException {
		parse(doc.getDocumentElement(), process, unknownParameters);
	}

	/**
	 * This method will parsen a process that has been stored as a child to the given parent element
	 * using the {@link XMLExporter#exportProcess(Element, Process)} with the same parentElement.
	 */
	public Process parse(Element parentElement, List<UnknownParameterInformation> unknownParameters) throws XMLException {
		Element processElement = XMLTools.getUniqueChildElement(parentElement, XMLExporter.ELEMENT_PROCESS);
		Process process = new Process();
		parse(processElement, process, unknownParameters);
		return process;
	}

	private void parse(Element processElement, Process process, List<UnknownParameterInformation> unknownParameters)
			throws XMLException {
		// find version number
		VersionNumber processVersion = parseVersion(processElement);

		// search root operator: it's either the process tag or an operator itself
		Element rootOperatorElement = processElement;
		if (!processElement.getTagName().equals("operator")) {
			rootOperatorElement = XMLTools.getChildTag(processElement, "operator", false);
		}

		ProcessRootOperator rootOperator = parseRootOperator(rootOperatorElement, processVersion, process,
				unknownParameters);
		process.setRootOperator(rootOperator);

		// Process context
		Collection<Element> contextElements = XMLTools.getChildElements(processElement, "context");
		switch (contextElements.size()) {
			case 0:
				break;
			case 1:
				parseContext(contextElements.iterator().next(), process);
				break;
			default:
				addMessage("&lt;process&gt; can have at most one &lt;context&gt; tag.");
				break;
		}

		// Annotations
		Collection<Element> annotationsElems = XMLTools.getChildElements(processElement, Annotations.ANNOTATIONS_TAG_NAME);
		switch (annotationsElems.size()) {
			case 0:
				break;
			case 1:
				process.getAnnotations().parseXML(annotationsElems.iterator().next());
				break;
			default:
				addMessage("&lt;process&gt; can have at most one &lt;annotations&gt; tag.");
				break;
		}

		if (hasMessage()) {
			process.setImportMessage(getMessage());
		}
	}

	private ProcessRootOperator parseRootOperator(Element rootOperatorElement, VersionNumber processFileVersion,
			Process process, List<UnknownParameterInformation> unknownParameters) throws XMLException {
		Operator rootOp = parseOperator(rootOperatorElement, processFileVersion, process, unknownParameters);

		for (Runnable runnable : jobsAfterTreeConstruction) {
			runnable.run();
		}

		if (mustAutoConnect) {
			if (rootOp instanceof OperatorChain) {
				try {
					((OperatorChain) rootOp).getSubprocess(0).autoWire(CompatibilityLevel.PRE_VERSION_5, true, true);
					addMessage(
							"As of version 5.0, RapidMiner processes define an explicit data flow. This data flow has been constructed automatically.");
				} catch (Exception e) {
					addMessage(
							"As of version 5.0, RapidMiner processes define an explicit data flow. This data flow could not be constructed automatically: "
									+ e);
					// LogService.getRoot().log(Level.WARNING, "Cannot autowire: " + e, e);
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.io.process.XMLImporter.autowire_error", e), e);
				}
			}
		}
		for (Runnable runnable : jobsAfterAutoWire) {
			runnable.run();
		}
		if (rootOp instanceof ProcessRootOperator) {
			return (ProcessRootOperator) rootOp;
		} else {
			throw new XMLException("Outermost operator must be of type 'Process' (<operator class=\"Process\">)");
		}
	}

	private void parseProcess(Element element, ExecutionUnit executionUnit, VersionNumber processFileVersion,
			Process process, List<UnknownParameterInformation> unknownParameterInformation) throws XMLException {
		assert "process".equals(element.getTagName());

		if (element.hasAttribute("expanded")) {
			String expansionString = element.getAttribute("expanded");
			if ("no".equals(expansionString) || "false".equals(expansionString)) {
				executionUnit.setExpanded(false);
			} else if ("yes".equals(expansionString) || "true".equals(expansionString)) {
				executionUnit.setExpanded(true);
			} else {
				throw new XMLException("Expansion mode `" + expansionString + "` is not defined!");
			}
		}

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element opElement = (Element) child;
				if ("operator".equals(opElement.getTagName())) {
					parseOperator(opElement, executionUnit, processFileVersion, process, unknownParameterInformation);
				} else if ("connect".equals(opElement.getTagName())) {
					parseConnection(opElement, executionUnit);
				} else if ("portSpacing".equals(opElement.getTagName())) {
					// ignore, parsed by ProcessRenderer
				} else if ("description".equals(opElement.getTagName())) {
					// ignore, parsed by GUIProcessXMLFilter
				} else if ("background".equals(opElement.getTagName())) {
					// ignore, parsed by GUIProcessXMLFilter
				} else {
					addMessage(
							"<em class=\"error\">ExecutionUnit must only contain <operator> tags as children. Ignoring unknown tag <code>&lt;"
									+ opElement.getTagName() + "&gt;</code>.</em>");
				}
			}
		}

		ProcessXMLFilterRegistry.fireExecutionUnitImported(executionUnit, element);
	}

	private void parseConnection(Element connectionElement, ExecutionUnit executionUnit) throws XMLException {
		final OutputPorts outputPorts;
		if (connectionElement.hasAttribute("from_op")) {
			String fromOp = connectionElement.getAttribute("from_op");
			Operator from = executionUnit.getOperatorByName(fromOp);
			if (from == null) {
				addMessage("<em class=\"error\">Unknown operator " + fromOp + " referenced in <code>from_op</code>.</em>");
				return;
			}
			outputPorts = from.getOutputPorts();
		} else {
			outputPorts = executionUnit.getInnerSources();
		}
		String fromPort = connectionElement.getAttribute("from_port");
		OutputPort out = outputPorts.getPortByName(fromPort);
		if (out == null) {
			addMessage("<em class=\"error\">The output port <var>" + fromPort + "</var> is unknown at operator <var>"
					+ outputPorts.getOwner().getName() + "</var>.</em>");
			return;
		}

		final InputPorts inputPorts;
		if (connectionElement.hasAttribute("to_op")) {
			String toOp = connectionElement.getAttribute("to_op");
			Operator to = executionUnit.getOperatorByName(toOp);
			if (to == null) {
				addMessage("<em class=\"error\">Unknown operator " + toOp + " referenced in <code>to_op</code>.</em>");
				return;
			}
			inputPorts = to.getInputPorts();
		} else {
			inputPorts = executionUnit.getInnerSinks();
		}
		String toPort = connectionElement.getAttribute("to_port");
		InputPort in = inputPorts.getPortByName(toPort);
		if (in == null) {
			addMessage("<em class=\"error\">The input port <var>" + toPort + "</var> is unknown at operator <var>"
					+ inputPorts.getOwner().getName() + "</var>.</em>");
			return;
		}
		try {
			out.connectTo(in);
		} catch (PortException e) {
			addMessage("<em class=\"error\">Faild to connect ports: " + e.getMessage() + "</var>.</em>");
		}
	}

	public Operator parseOperator(Element opElement, VersionNumber processVersion, Process process,
			List<UnknownParameterInformation> unknownParameterInformation) throws XMLException {
		total = opElement.getElementsByTagName("operator").getLength();
		Operator operator = parseOperator(opElement, null, processVersion, process, unknownParameterInformation);
		unlockPorts(operator);
		return operator;
	}

	private Operator parseOperator(Element opElement, ExecutionUnit addToProcess, VersionNumber processFileVersion,
			Process process, List<UnknownParameterInformation> unknownParameterInformation) throws XMLException {
		assert "operator".equals(opElement.getTagName());
		String className = opElement.getAttribute("class");
		String replacement = OperatorService.getReplacementForDeprecatedClass(className);

		if (replacement != null) {
			addMessage("Deprecated operator '<code>" + className + "</code>' was replaced by '<code>" + replacement
					+ "</code>'.");
			className = replacement;
		}
		OperatorDescription opDescr = OperatorService.getOperatorDescription(className);
		if (opDescr == null) {
			OperatorDescription[] operatorDescriptions = OperatorService.getOperatorDescriptions(DummyOperator.class);
			if (operatorDescriptions.length == 1) {
				opDescr = operatorDescriptions[0];
				if (className.indexOf(':') == -1) {
					addMessage("<em class=\"error\">The operator class '" + className + "' is unknown.</em>");
				} else {
					addMessage("<em class=\"error\">The operator class '" + className
							+ "' is unknown. Possibly you must install a plugin for operators of group '"
							+ className.substring(0, className.indexOf(':')) + "'.</em>");
				}
			} else {
				throw new XMLException("Unknown operator class: '" + className + "'!");
			}
		}
		Operator operator;
		try {
			operator = opDescr.createOperatorInstance();
			if (operator instanceof DummyOperator) {
				((DummyOperator) operator).setReplaces(className);
			}
			ProcessXMLFilterRegistry.fireOperatorImported(operator, opElement);
			created++;
			if (progressListener != null && total > 0) {
				progressListener.setCompleted(100 * created / total);
			}
		} catch (OperatorCreationException e) {
			throw new XMLException("Cannot create operator: " + e.getMessage(), e);
		}
		operator.rename(opElement.getAttribute("name"));
		String versionString = opElement.getAttribute("compatibility");

		OperatorVersion[] incompatibleVersionChanges = operator.getIncompatibleVersionChanges();
		assert incompatibleVersionChanges != null;

		// sort to be sure to have an array of ascending order
		Arrays.sort(incompatibleVersionChanges);

		OperatorVersion opVersion = null;

		/*
		 * Here we are searching if there has been any change since the last save. If no, we use the
		 * latest version to save it again. If yes, use the earliest incompatible version.
		 */
		OperatorVersion latestOperatorVersion = OperatorVersion.getLatestVersion(opDescr);
		if (versionString != null && !versionString.isEmpty()) {
			try {
				opVersion = new OperatorVersion(versionString);
			} catch (VersionNumberException e) {
				addMessage(
						"Failed to parse version string '" + versionString + "' for operator " + operator.getName() + ".");
				// fall back to 5.0 on malformed version string
				opVersion = new OperatorVersion(5, 0, 0);
			}
		}
		if (opVersion == null) {
			// fall back version for missing version string
			opVersion = getEnclosingVersion(process);
		}

		OperatorVersion nextIncompatibility = null;
		for (int i = incompatibleVersionChanges.length - 1; i >= 0; i--) {
			if (opVersion.isAtMost(incompatibleVersionChanges[i])) {
				nextIncompatibility = incompatibleVersionChanges[i];
			} else {
				break;
			}
		}

		if (nextIncompatibility == null) {
			opVersion = latestOperatorVersion;
		} else {
			opVersion = nextIncompatibility;
		}
		operator.setCompatibilityLevel(opVersion);
		if (operator instanceof ProcessRootOperator && process != null) {
			// add root operator to process to enable fallback version
			process.setRootOperator((ProcessRootOperator) operator);
		}

		/*
		 * Check if the selected operator version does not equal the latest version (hence an
		 * older/incompatible version was loaded)
		 */
		if (incompatibleVersionChanges.length > 0 && opVersion.compareTo(latestOperatorVersion) != 0) {
			addMessage("Operator '" + operator.getName() + "' was created with version '" + opVersion
					+ "'. The operator's behaviour has changed as of version '" + latestOperatorVersion
					+ "' and can be adapted to the latest version in the parameter panel.");
		}

		if (opElement.hasAttribute("breakpoints")) {
			String breakpointString = opElement.getAttribute("breakpoints");
			boolean ok = false;
			if (breakpointString.equals("both")) {
				operator.setBreakpoint(BreakpointListener.BREAKPOINT_BEFORE, true);
				operator.setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, true);
				ok = true;
			}
			for (int i = 0; i < BreakpointListener.BREAKPOINT_POS_NAME.length; i++) {
				if (breakpointString.indexOf(BreakpointListener.BREAKPOINT_POS_NAME[i]) >= 0) {
					operator.setBreakpoint(i, true);
					ok = true;
				}
			}
			if (!ok) {
				throw new XMLException("Breakpoint `" + breakpointString + "` is not defined!");
			}
		}

		if (opElement.hasAttribute("activated")) {
			String activationString = opElement.getAttribute("activated");
			if (activationString.equals("no") || activationString.equals("false")) {
				operator.setEnabled(false);
			} else if (activationString.equals("yes") || activationString.equals("true")) {
				operator.setEnabled(true);
			} else {
				throw new XMLException("Activation mode `" + activationString + "` is not defined!");
			}
		}

		if (opElement.hasAttribute("expanded")) {
			String expansionString = opElement.getAttribute("expanded");
			if ("no".equals(expansionString) || "false".equals(expansionString)) {
				operator.setExpanded(false);
			} else if ("yes".equals(expansionString) || "true".equals(expansionString)) {
				operator.setExpanded(true);
			} else {
				throw new XMLException("Expansion mode `" + expansionString + "` is not defined!");
			}
		}

		if (addToProcess != null) {
			addToProcess.addOperator(operator);
		}

		// parameters and inner operators
		NodeList innerTags = opElement.getChildNodes();
		for (int i = 0; i < innerTags.getLength(); i++) {
			Node node = innerTags.item(i);
			if (node instanceof Element) {
				Element inner = (Element) node;
				if (inner.getTagName().toLowerCase().equals("parameter")) {
					String[] parameter = parseParameter(inner);
					boolean knownType = operator.getParameters().setParameter(parameter[0], parameter[1]);
					if (!knownType) {
						if (relevantParameter(className, parameter[0])) {
							LogService.getRoot().log(Level.INFO,
									"com.rapidminer.io.process.XMLImporter.attribute_not_found_unknown", new Object[] {
											parameter[0], operator.getName(), operator.getOperatorDescription().getName() });
						}
					}
				} else if (inner.getTagName().toLowerCase().equals("list")) {
					final String key = inner.getAttribute("key");
					ParameterType type = operator.getParameters().getParameterType(key);
					if (type == null) {
						if (relevantParameter(className, key)) {
							LogService.getRoot().log(Level.INFO,
									"com.rapidminer.io.process.XMLImporter.attribute_not_found_list",
									new Object[] { key, operator.getName(), operator.getOperatorDescription().getName() });
						}
					} else {
						if (!(type instanceof ParameterTypeList)) {
							addMessage("The parameter '" + type.getKey() + "' is a " + type.getClass().getSimpleName()
									+ ", but a list was found.");
							type = null;
						} else {
							ListDescription listDescription = parseParameterList(inner, (ParameterTypeList) type);
							final String listString = ParameterTypeList.transformList2String(listDescription.getList());
							boolean knownType = operator.getParameters().setParameter(listDescription.getKey(), listString);
							if (!knownType) {
								if (relevantParameter(className, listDescription.getKey())) {
									LogService.getRoot().log(Level.INFO,
											"com.rapidminer.io.process.XMLImporter.attribute_not_found_unknown",
											new Object[] { listDescription.getKey(), operator.getName(),
													operator.getOperatorDescription().getName() });
								}
							}
						}
					}
				} else if (inner.getTagName().toLowerCase().equals("enumeration")) {
					final String key = inner.getAttribute("key");
					ParameterType type = operator.getParameters().getParameterType(key);
					if (type == null) {
						if (relevantParameter(className, key)) {
							LogService.getRoot().log(Level.INFO,
									"com.rapidminer.io.process.XMLImporter.attribute_not_found_enum",
									new Object[] { key, operator.getName(), operator.getOperatorDescription().getName() });
						}
					} else {
						if (!(type instanceof ParameterTypeEnumeration)) {
							addMessage("The parameter '" + type.getKey() + "' is a " + type.getClass().getSimpleName()
									+ ", but an enumeration was found.");
							type = null;
						}
						final List<String> parsed = parseParameterEnumeration(inner, (ParameterTypeEnumeration) type);
						boolean knownType = operator.getParameters().setParameter(key,
								ParameterTypeEnumeration.transformEnumeration2String(parsed));
						if (!knownType) {
							if (relevantParameter(className, key)) {
								LogService.getRoot().log(Level.INFO,
										"com.rapidminer.io.process.XMLImporter.attribute_not_found_unknown", new Object[] {
												key, operator.getName(), operator.getOperatorDescription().getName() });
							}
						}
					}
				} else if (inner.getTagName().toLowerCase().equals("operator")
						|| inner.getTagName().toLowerCase().equals("process")) {
					if (!(operator instanceof OperatorChain)) {
						addMessage("<em class=\"error\">Operator '<class>" + operator.getOperatorDescription().getName()
								+ "</class>' may not have children. Ignoring.");
					}
					// otherwise, we do the parsing later
				} else if (inner.getTagName().toLowerCase().equals("description")) {
					// ignore, parsed by GUIProcessXMLFilter
				} else {
					addMessage("<em class=\"error\">Ignoring unknown inner tag for <code>&gt;operator&lt;</code>: <code>&lt;"
							+ inner.getTagName() + "&gt;</code>.");
				}
			}
		}

		if (operator instanceof OperatorChain) {
			OperatorChain nop = (OperatorChain) operator;
			NodeList children = opElement.getChildNodes();
			int subprocessIndex = 0;
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Element) {
					Element childProcessElement = (Element) child;
					if ("process".equals(childProcessElement.getTagName())
							|| "operator".equals(childProcessElement.getTagName())) {
						if (subprocessIndex >= nop.getNumberOfSubprocesses() && !nop.areSubprocessesExtendable()) {
							addMessage("<em class=\"error\">Cannot add child " + childProcessElement.getAttribute("name")
									+ "</var>.</em> Operator <code>" + nop.getOperatorDescription().getName()
									+ "</code> has only " + nop.getNumberOfSubprocesses() + " subprocesses.");
						} else {
							if (subprocessIndex >= nop.getNumberOfSubprocesses()) {
								// we know nop.areSubprocessesExtendable()==true now
								nop.addSubprocess(subprocessIndex);
							}
							if ("process".equals(childProcessElement.getTagName())) {
								ExecutionUnit subprocess = nop.getSubprocess(subprocessIndex);
								subprocessIndex++;
								parseProcess(childProcessElement, subprocess, processFileVersion, process,
										unknownParameterInformation);
							} else if ("operator".equals(childProcessElement.getTagName())) {
								if (processFileVersion.compareTo(VERSION_RM_5) >= 0) {
									addMessage(
											"<em class=\"error\"><code>&lt;operator&gt;</code> as children of <code>&lt;operator&gt</code> is deprecated syntax. From version 5.0 on, use <code>&lt;process&gt;</code> as children.</em>");
								} else {
									if (!operatorAsDirectChildrenDeprecatedReported) {
										addMessage(
												"<code>&lt;operator&gt;</code> as children of <code>&lt;operator&gt</code> is deprecated syntax. From version 5.0 on, use <code>&lt;process&gt;</code> as children.");
										operatorAsDirectChildrenDeprecatedReported = true;
									}
									final ExecutionUnit subprocess = nop.getSubprocess(subprocessIndex);
									if (subprocessIndex <= nop.getNumberOfSubprocesses() - 2
											|| nop.areSubprocessesExtendable()) {
										subprocessIndex++;
									}
									parseOperator(childProcessElement, subprocess, processFileVersion, process,
											unknownParameterInformation);
									mustAutoConnect = true;
								}
							}
						}
					}
				}
			}
		}

		/**
		 * Apply all parse rules that are applicable for this operator.
		 */
		String key = operator.getOperatorDescription().getKey();
		List<ParseRule> list = OPERATOR_KEY_RULES_MAP.get(key);
		if (list != null) {
			for (ParseRule rule : list) {
				String msg = rule.apply(operator, processFileVersion, this);
				if (msg != null) {
					process.setProcessConverted(true);
					addMessage(msg);
				}
			}
		}
		return operator;
	}

	/**
	 * Returns the best default version for an operator. Will use the root operator or current
	 * RapidMiner version (in this order). The given process can be null.
	 *
	 * @param process
	 *            the process
	 *
	 * @return the best matching enclosing version
	 * @since 7.6
	 */
	private OperatorVersion getEnclosingVersion(Process process) {
		if (process != null) {
			Operator root = process.getRootOperator();
			if (root != null && root.getCompatibilityLevel() != null) {
				return root.getCompatibilityLevel();
			}
		}
		return OperatorVersion.asNewOperatorVersion(RapidMiner.getVersion());
	}

	private boolean relevantParameter(String opName, String paramName) {
		return !(IRRELEVANT_PARAMETERS.contains(opName + "." + paramName) || IRRELEVANT_PARAMETERS.contains(paramName));
	}

	private List<String> parseList(Element parent, String childName) {
		List<String> result = new LinkedList<String>();
		NodeList childNodes = parent.getElementsByTagName(childName);
		switch (childNodes.getLength()) {
			case 0:
				addMessage("Missing &lt;" + childName + "&gt; tag in context.");
				break;
			case 1:
				NodeList locationNodes = ((Element) childNodes.item(0)).getElementsByTagName("location");
				for (int i = 0; i < locationNodes.getLength(); i++) {
					result.add(locationNodes.item(i).getTextContent());
				}
				break;
			default:
				addMessage("&lt;context&gt; can have at most one &lt;" + childName + "&gt; tag.");
				break;
		}
		return result;
	}

	private void parseContext(Element element, Process process) {
		ProcessContext context = process.getContext();
		context.setInputRepositoryLocations(parseList(element, "input"));
		context.setOutputRepositoryLocations(parseList(element, "output"));
		NodeList childNodes = element.getElementsByTagName("macros");
		switch (childNodes.getLength()) {
			case 0:
				addMessage("Missing &lt;macros&gt; tag in context.");
				break;
			case 1:
				NodeList locationNodes = ((Element) childNodes.item(0)).getElementsByTagName("macro");
				for (int i = 0; i < locationNodes.getLength(); i++) {
					Element macroElem = (Element) locationNodes.item(i);
					context.addMacro(new Pair<String, String>(XMLTools.getTagContents(macroElem, "key"),
							XMLTools.getTagContents(macroElem, "value")));
				}
				break;
			default:
				addMessage("&lt;context&gt; can have at most one &lt;macros&gt; tag.");
				break;
		}
	}

	private ListDescription parseParameterList(Element list, ParameterTypeList type) throws XMLException {
		// TODO: type is unused here. Do we have to use type.transformNewValue for children?
		ParameterType keyType = type.getKeyType();
		ParameterType valueType = type.getValueType();

		List<String[]> values = new LinkedList<String[]>();
		NodeList children = list.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				Element inner = (Element) node;
				if (inner.getTagName().toLowerCase().equals("parameter")) {
					String key = inner.getAttribute("key");
					String value = inner.getAttribute("value");
					final String transformedKey = keyType.transformNewValue(key);
					final String transformedValue = valueType.transformNewValue(value);
					values.add(new String[] { transformedKey, transformedValue });
				} else {
					addMessage("<em class=\"error\">Ilegal inner tag for <code>&lt;list&gt;</code>: <code>&lt;"
							+ inner.getTagName() + "&gt;</code>.</em>");
					return new ListDescription(list.getAttribute("key"), Collections.<String[]> emptyList());
				}
			}
		}
		return new ListDescription(list.getAttribute("key"), values);
	}

	private List<String> parseParameterEnumeration(Element list, ParameterTypeEnumeration type) throws XMLException {
		List<String> values = new LinkedList<String>();
		NodeList children = list.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node instanceof Element) {
				Element inner = (Element) node;
				if (inner.getTagName().toLowerCase().equals("parameter")) {
					values.add(type.getValueType().transformNewValue(inner.getAttribute("value")));
				} else {
					addMessage("<em class=\"error\">Ilegal inner tag for <code>&lt;enumeration&gt;</code>: <code>&lt;"
							+ inner.getTagName() + "&gt;</code>.</em>");
					return new LinkedList<String>();
				}
			}
		}
		return values;
	}

	private String[] parseParameter(Element parameter) {
		return new String[] { parameter.getAttribute("key"), parameter.getAttribute("value") };
	}

	private VersionNumber parseVersion(Element element) {
		if (element.hasAttribute("version")) {
			try {
				VersionNumber version = new VersionNumber(element.getAttribute("version"));
				return version;
			} catch (NumberFormatException e) {
				addMessage("<em class=\"error\">The version " + element.getAttribute("version")
						+ " is not a legal RapidMiner version, assuming 4.0.</em>");
				return new VersionNumber(4, 0);
			}
		}
		addMessage("<em class=\"error\">Found no version information, assuming 4.0.</em>");
		return new VersionNumber(4, 0);
	}

	private void unlockPorts(Operator operator) {
		operator.getInputPorts().unlockPortExtenders();
		operator.getOutputPorts().unlockPortExtenders();
		if (operator instanceof OperatorChain) {
			for (ExecutionUnit unit : ((OperatorChain) operator).getSubprocesses()) {
				unit.getInnerSinks().unlockPortExtenders();
				unit.getInnerSources().unlockPortExtenders();
				for (Operator child : unit.getOperators()) {
					unlockPorts(child);
				}
			}
		}
	}

	public void doAfterAutoWire(Runnable runnable) {
		jobsAfterAutoWire.add(runnable);
	}

	public void doAfterTreeConstruction(Runnable runnable) {
		jobsAfterTreeConstruction.add(runnable);
	}

	private boolean hasMessage() {
		return messages.length() > 0;
	}

	private String getMessage() {
		return "<html><body><h3>Importing process produced the following messages:</h3><ol>" + messages.toString()
				+ "</ol></body></html>";
	}
}
