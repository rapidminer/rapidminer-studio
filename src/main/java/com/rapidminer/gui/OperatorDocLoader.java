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
package com.rapidminer.gui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;
import com.rapidminer.tools.documentation.ExampleProcess;


/**
 * This class loads the operator's descriptions either live from the internet wiki or from the
 * resources. The latter requires that a custom Bot which gets all operator description sites from
 * the MediaWiki was executed during build time. Those html files retrieved there for an operator
 * must have been parsed and saved in the resources folder in "doc/namespace/operatorname". If the
 * user does not have an internet connection all operators are loaded from this resource. If the
 * user does have an internet connection the operators are loaded directly from the RapidWiki site.
 * The operator description is shown in the RapidMiner Help Window.
 *
 * @author Miguel Buescher, Sebastian Land, Marcel Seifert
 *
 */
public class OperatorDocLoader {

	public static final String DEFAULT_IOOBJECT_ICON_NAME = "question_blue.png";
	/**
	 * The documentation cache. It is used to cache documentations after reading them for the first
	 * time.
	 */
	private static Map<String, String> DOC_CACHE = new HashMap<>(100);

	/**
	 * Gets the operator documentation as an HTML string.
	 *
	 * The documentation sources will be used in the following order: 1. Documentation Cache 2.
	 * Single-XML documentation format (for each operator) 3. OperatorsDoc.xml file documentation
	 * format
	 *
	 * Previously uncached documentations will be cached after reading for a better response time.
	 * If something goes wrong, a log message will be triggered.
	 *
	 * @param operator
	 *            The operator from that to get the documentation.
	 * @return HTML string of the operator documentation
	 */
	static String getDocumentation(Operator operator) {
		String html = null;
		if (operator != null) {

			// load cache
			String key = operator.getOperatorDescription().getKey();
			if (DOC_CACHE.containsKey(key)) {
				return DOC_CACHE.get(key);
			}

			// load XML
			URL resourceURL = OperatorDocumentationBrowser.getDocResourcePath(operator);
			if (resourceURL != null) {
				try (InputStream xmlStream = WebServiceTools.openStreamFromURL(resourceURL)) {
					Source xmlSource = new StreamSource(xmlStream);
					if (xmlSource != null) {
						html = OperatorDocToHtmlConverter.applyXSLTTransformation(xmlSource);
						if (html != null) {
							html = html.replace("xmlns:rmdoc=\"com.rapidminer.gui.OperatorDocumentationBrowser\"", " ");
						}
					}
				} catch (IOException | TransformerException e) {
					LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.OperatorDocLoader.xml_error", e);
				}
			}

			// load operatorsDoc
			if (html == null) {
				html = makeOperatorDocumentation(operator);
				if (html == null) {
					LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.OperatorDocLoader.operatorsdoc_error");
				}
			}

			// write cache
			if (html != null) {
				DOC_CACHE.put(key, html);
				return html;
			}
		}
		LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.OperatorDocLoader.operator_not_found");
		return getErrorText(operator);
	}

	/**
	 * This generates the operator documentation from the operatorsDoc
	 */
	private static String makeOperatorDocumentation(Operator displayedOperator) {
		OperatorDescription descr = displayedOperator.getOperatorDescription();
		StringBuilder buf = new StringBuilder(2048);
		buf.append("<html><body><table><tr><td>");

		String iconName = "icons/24/" + displayedOperator.getOperatorDescription().getIconName();
		URL resource = Tools.getResource(iconName);
		if (resource != null) {
			buf.append("<img src=\"");
			buf.append(resource);
			buf.append("\" class=\"HeadIcon\"/> ");
		}

		buf.append("<td valign=\"middle\" align=\"left\"> <h2 class=\"firstHeading\" id=\"firstHeading\">");
		buf.append(descr.getName());

		buf.append("<span class=\"packageName\"><br/>");
		buf.append(descr.getProviderName());

		buf.append("</span></h2></td></tr></table><div style=\"border-top: 1px solid #bbbbbb\">");
		buf.append(OperatorDocToHtmlConverter.getTagHtmlForDescription(descr));

		buf.append("<h4>Synopsis</h4><p>");
		buf.append(descr.getShortDescription());
		buf.append("</p></p><br/><h4>Description</h4>");
		String descriptionText = descr.getLongDescriptionHTML();
		if (descriptionText != null) {
			if (!descriptionText.trim().startsWith("<p>")) {
				buf.append("<p>");
			}
			buf.append(descriptionText);
			if (!descriptionText.trim().endsWith("</p>")) {
				buf.append("</p>");
			}
			buf.append("<br/>");
		}
		appendPortsToDocumentation(displayedOperator.getInputPorts(), "Input", buf);
		appendPortsToDocumentation(displayedOperator.getOutputPorts(), "Output", buf);

		Parameters parameters = displayedOperator.getParameters();
		if (parameters.getKeys().size() > 0) {
			buf.append("<h4 class=\"parametersHeading\">Parameters</h4><table class=\"parametersTable\">");
			for (String key : parameters.getKeys()) {
				ParameterType type = parameters.getParameterType(key);
				if (type == null) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.OperatorDocLoader.unkwown_parameter_key",
							new Object[] { displayedOperator.getName(), key });
					continue;
				}
				buf.append("<tr><td><b>");

				buf.append(makeParameterHeader(type));
				buf.append("</b>");

				if (type.isOptional()) {
					buf.append(" (optional)");
				}

				buf.append("</td></tr><tr><td>");
				buf.append(type.getDescription());
				buf.append("</td></tr><tr><td class=\"parameterDetailsCell\"><span class=\"parameterDetails\">");

				String parameterType = OperatorDocToHtmlConverter.getParameterType(descr.getKey(), key);
				buf.append("<b>Type: </b> <i>");
				buf.append(parameterType);
				buf.append("</i>");

				if (parameterType.equals(OperatorDocToHtmlConverter.REAL_LABEL)
						|| parameterType.equals(OperatorDocToHtmlConverter.INTEGER_LABEL)
						|| parameterType.equals(OperatorDocToHtmlConverter.LONG_LABEL)
						|| parameterType.equals(OperatorDocToHtmlConverter.SELECTION_LABEL)) {
					buf.append("<br/><b>Range: </b> <i>");
					buf.append(OperatorDocToHtmlConverter.getParameterRange(descr.getKey(), key));
					buf.append("</i>");
				}

				String parameterDefault = OperatorDocToHtmlConverter.getParameterDefault(descr.getKey(), key);
				if (!parameterDefault.trim().isEmpty()) {
					buf.append("<br/><b>Default: </b> <i>");
					buf.append(OperatorDocToHtmlConverter.getParameterDefault(descr.getKey(), key));
					buf.append("</i>");
				}

				buf.append("</span></td></tr>");
			}
			buf.append("</table>");
		}

		if (!descr.getOperatorDocumentation().getExamples().isEmpty()) {
			buf.append("<h4>Examples</h4><ul>");
			int i = 0;
			for (ExampleProcess exampleProcess : descr.getOperatorDocumentation().getExamples()) {
				buf.append("<li>");
				buf.append(exampleProcess.getComment());
				buf.append(makeExampleFooter(i));
				buf.append("</li>");
				i++;
			}
			buf.append("</ul>");
		}

		buf.append("</div></body></html>");
		return buf.toString();
	}

	private static Object makeExampleFooter(int exampleIndex) {
		return "<br/><a href=\"show_example_" + exampleIndex + "\">Show example process</a>.";
	}

	private static void appendPortsToDocumentation(Ports<? extends Port> ports, String title, StringBuilder buf) {
		if (ports.getNumberOfPorts() > 0) {
			buf.append("<h4>" + title + "</h4><table border=\"0\" cellspacing=\"0\"><tr><td>");
			for (Port port : ports.getAllPorts()) {
				buf.append("<table><tr>");
				buf.append("<td class=\"lilIcon\">");
				Class<? extends IOObject> typeClass = null;
				if (port instanceof InputPort) {
					// Input Port
					InputPort inputPort = (InputPort) port;
					List<Precondition> preconditions = new LinkedList<Precondition>(inputPort.getAllPreconditions());
					if (preconditions.size() > 0) {
						MetaData metaData = preconditions.get(0).getExpectedMetaData();
						if (metaData != null) {
							typeClass = metaData.getObjectClass();
						}
					}
				}
				String imgSrc = getIconNameForType(typeClass);
				buf.append("<img src=\"" + imgSrc + "\" class=\"typeIcon\" />");
				buf.append("</td><td>");
				buf.append("<b>" + port.getName() + "</b>");
				if (typeClass != null) {
					buf.append("<i>" + OperatorDocToHtmlConverter.getTypeNameForType(typeClass) + "</i>");
				}
				buf.append("</td>");
				buf.append("</tr>");
				buf.append("</table></td></tr>");
			}
			buf.append("</table><br/>");
		}
	}

	private static String makeParameterHeader(ParameterType type) {
		return type.getKey().replace('_', ' ');
	}

	/**
	 *
	 * Searches for a class with the given name and returns the path of the resource.
	 *
	 * @param clazz
	 *            the class as Class.
	 * @return the path of the resource of the corresponding icon.
	 */
	private static String getIconNameForType(Class<? extends IOObject> clazz) {
		String path = null;
		String iconName;
		if (clazz == null) {
			iconName = DEFAULT_IOOBJECT_ICON_NAME;
		} else {
			iconName = RendererService.getIconName(clazz);
		}
		try {
			path = SwingTools.getIconPath("24/" + iconName);
		} catch (Exception e) {
			LogService.getRoot().finer("Error retrieving icon for type '" + clazz + "'! Reason: " + e.getLocalizedMessage());
		}
		return path;

	}

	/**
	 * Generates a HTML string which contains an error text for the case that an operator
	 * documentation was not found.
	 *
	 * @param operator
	 *            The operator which documentation not was found
	 * @return An error HTML which says, that no documentation for the operator was found
	 */
	private static String getErrorText(Operator operator) {
		String opName;
		if (operator != null) {
			opName = operator.getName();
		} else {
			opName = "unnamed";
		}
		StringBuilder builder = new StringBuilder();
		builder.append(
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\"><head><table cellpadding=0 cellspacing=0><tr><td><img src=\"");
		builder.append(SwingTools.getIconPath("48/bug_error.png"));
		builder.append("\"/></td><td width=\"5\"></td><td>");
		builder.append(I18N.getErrorMessage("documentation.could_not_find", opName));
		builder.append("</td></tr></table></head></html>");
		return builder.toString();
	}

}
