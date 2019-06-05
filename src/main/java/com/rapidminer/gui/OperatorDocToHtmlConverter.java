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

import static com.rapidminer.gui.OperatorDocLoader.DEFAULT_IOOBJECT_ICON_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Level;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeLong;
import com.rapidminer.parameter.ParameterTypeNumber;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;


/**
 * This class handles the conversion of the operator documentation XML to an HTML page.
 *
 * @author Philipp Kersting, Marco Boeck, Marcel Seifert
 *
 */
public class OperatorDocToHtmlConverter {

	private static final TransformerFactory XSLT_TRANSFORMER_FACTORY = TransformerFactory.newInstance();
	public static final String STYLESHEET_RESOURCE = "/com/rapidminer/resources/documentationview.xslt";
	private static final byte[] XSLT_CONTENT;
	private static final int MAX_CATEROGIES_DISPLAYED_IN_HELP = 10;

	static final String INTEGER_LABEL = I18N.getGUILabel("attribute_type.integer");
	static final String LONG_LABEL = I18N.getGUILabel("attribute_type.long");
	static final String REAL_LABEL = I18N.getGUILabel("attribute_type.real");
	static final String SELECTION_LABEL = I18N.getGUILabel("attribute_type.selection");
	private static final String STRING_LABEL = I18N.getGUILabel("attribute_type.string");
	private static final String LIST_LABEL = I18N.getGUILabel("attribute_type.list");
	private static final String ENUMERATION_LABEL = I18N.getGUILabel("attribute_type.enumeration");
	private static final String BOOLEAN_LABEL = I18N.getGUILabel("attribute_type.boolean");
	private static final String OTHER_LABEL = I18N.getGUILabel("attribute_type.other");

	/**
	 * This icon will be shown as a replacement for operators without an icon. These are usually
	 * deprecated operators.
	 */
	private static final String REPLACEMENT_FOR_OPERATORS_WITHOUT_ICON = "symbol_questionmark.png";

	static {
		byte[] content = new byte[0];
		try {
			content = Tools.readInputStream(OperatorDocToHtmlConverter.class.getResourceAsStream(STYLESHEET_RESOURCE));
		} catch (IOException e) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.gui.OperatorDocToHtmlConverter.error_loading_xslt", e);
		}
		XSLT_CONTENT = content;
	}

	/**
	 * Applies the documentation XSLT to the specified XML source and returns the transformed HTML
	 * as {@link String}.
	 *
	 * @param xmlSource
	 *            the source containing the documentation XML
	 * @return the transformed documentation XML as HTML
	 * @throws TransformerException
	 *             in case the transformation goes wrong
	 * @since 6.4.0
	 */
	public static String applyXSLTTransformation(Source xmlSource) throws TransformerException {
		StringWriter buffer = new StringWriter();
		Source xsltSource = new StreamSource(new ByteArrayInputStream(XSLT_CONTENT));
		Transformer trans = XSLT_TRANSFORMER_FACTORY.newTransformer(xsltSource);
		trans.transform(xmlSource, new StreamResult(buffer));
		return buffer.toString();
	}

	/**
	 * Replaces underscores in the given {@link String} with a blank.
	 *
	 * @param string
	 * @return
	 */
	public static String insertBlanks(String string) {
		return string.replace('_', ' ');
	}

	/**
	 * Returns the name of a type in exchange for its class' name.
	 *
	 * @param type
	 *            the class' name as String
	 * @return the short name of the class as String
	 */
	@SuppressWarnings("unchecked")
	public static String getTypeNameForType(String type) {
		if (type == null || type.isEmpty()) {
			return "";
		} else {
			try {
				Class<? extends IOObject> typeClass;
				typeClass = (Class<? extends IOObject>) Class.forName(type);
				return getTypeNameForType(typeClass);
			} catch (ClassNotFoundException e) {
				LogService.getRoot().log(Level.FINER,
						"com.rapidminer.gui.OperatorDocToHtmlConverter.getTypeNameForTypeFromString",
						new String[] { type, e.getLocalizedMessage() });
				return "";
			}
		}
	}

	static String getTypeNameForType(Class<? extends IOObject> typeClass) {
		String typeName;
		if (typeClass != null) {
			typeName = RendererService.getName(typeClass);
			if (typeName != null && !typeName.isEmpty()) {
				typeName = " (" + typeName + ")";
			} else {
				LogService.getRoot().log(Level.FINER,
						"com.rapidminer.gui.OperatorDocToHtmlConverter.getTypeNameForTypeFromClass_no_name", typeClass);
				typeName = "";
			}
		} else {
			LogService.getRoot().log(Level.FINER,
					"com.rapidminer.gui.OperatorDocToHtmlConverter.getTypeNameForTypeFromClass_null", typeClass);
			typeName = "";
		}
		return typeName;
	}

	/**
	 * Returns the path to the icon that belongs to the given operator key.
	 *
	 * @param operatorKey
	 *            the key of the operator
	 * @return the path to icon for the given operator key
	 */
	public static String getIconNameForOperator(String operatorKey) {
		if (operatorKey == null) {
			LogService.getRoot().finer("Tried to retrieve icon name for null operatorKey!");
			return null;
		}
		// operator keys in the documentation begin with "operator.", so remove that
		int index = operatorKey.indexOf(".");
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		OperatorDescription operatorDescription = OperatorService.getOperatorDescription(operatorKey);
		if (operatorDescription == null) {
			LogService.getRoot().finer("Tried to retrieve icon name for null operator with key " + operatorKey);
			return null;
		}
		String iconName = operatorDescription.getIconName();
		if (iconName != null) {
			return SwingTools.getIconPath("24/" + iconName);
		} else {
			// Operator has no icon
			return SwingTools.getIconPath("24/" + REPLACEMENT_FOR_OPERATORS_WITHOUT_ICON);
		}
	}

	public static String getIconNameForOperatorSmall(String operatorKey) {
		if (operatorKey == null) {
			LogService.getRoot().finer("Tried to retrieve icon name for null operatorKey!");
			return null;
		}
		// operator keys in the documentation begin with "operator.", so remove that
		int index = operatorKey.indexOf(".");
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		OperatorDescription operatorDescription = OperatorService.getOperatorDescription(operatorKey);
		if (operatorDescription == null) {
			LogService.getRoot().finer("Tried to retrieve icon name for null operator with key " + operatorKey);
			return null;
		}
		return SwingTools.getIconPath("16/" + operatorDescription.getIconName());
	}

	public static String getOperatorNameForKey(String operatorKey) {
		OperatorDescription operatorDescription = OperatorService.getOperatorDescription(operatorKey);
		if (operatorDescription != null) {
			return operatorDescription.getName();
		} else {
			return null;
		}
	}

	public static String getPluginNameForOperator(String operatorKey) {
		OperatorDescription operatorDescription;
		int index = operatorKey.indexOf(".");
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		operatorDescription = OperatorService.getOperatorDescription(operatorKey);
		if (operatorDescription == null) {
			LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
					operatorKey);
			return null;
		}
		return operatorDescription.getProviderName();
	}

	/**
	 * Gets the {@link ParameterType} of the given operator key and parameter name as an i18n
	 * string. This is used if no type is specified in the documentation xml.
	 *
	 * @param operatorKey
	 *            The key of the operator
	 * @param parameterName
	 *            The name of the parameter
	 * @return An i18n string containing the type, if one can be found. Empty string else.
	 */
	public static String getParameterType(String operatorKey, String parameterName) {
		Operator operator = null;
		int index = operatorKey.indexOf(".");
		// remove operator group if existent
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		try {
			OperatorDescription description = OperatorService.getOperatorDescription(operatorKey);
			if (description == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return "";
			}
			operator = description.createOperatorInstance();
			if (operator == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return "";
			}

			Parameters parameters = operator.getParameters();
			if (parameters != null) {
				ParameterType type = parameters.getParameterType(parameterName);
				if (type != null) {
					if (type instanceof ParameterTypeNumber) {
						ParameterTypeNumber numberType = (ParameterTypeNumber) type;

						if (numberType instanceof ParameterTypeInt) {
							return INTEGER_LABEL;
						} else if (numberType instanceof ParameterTypeLong) {
							return LONG_LABEL;
						} else if (numberType instanceof ParameterTypeDouble) {
							return REAL_LABEL;
						}
					} else if (type instanceof ParameterTypeCategory || type instanceof ParameterTypeStringCategory) {
						return SELECTION_LABEL;
					} else if (type instanceof ParameterTypeString) {
						return STRING_LABEL;
					} else if (type instanceof ParameterTypeList) {
						return LIST_LABEL;
					} else if (type instanceof ParameterTypeEnumeration) {
						return ENUMERATION_LABEL;
					} else if (type instanceof ParameterTypeBoolean) {
						return BOOLEAN_LABEL;
					} else {
						return OTHER_LABEL;
					}
				}
			}
			return "";
		} catch (OperatorCreationException e) {
			LogService.getRoot().finer("Tried to retrieve plugin name for null operator with key " + operatorKey);
			return "";
		}

	}

	/**
	 * Gets the ParameterRange of the given operator key and parameter name as a string. This is
	 * used if no range is specified in the documentation xml.
	 *
	 * @param operatorKey
	 *            The key of the operator
	 * @param parameterName
	 *            The name of the parameter
	 * @return A string containing the range, if one can be found. Empty string else.
	 */
	public static String getParameterRange(String operatorKey, String parameterName) {
		Operator operator = null;
		int index = operatorKey.indexOf(".");
		// remove operator group if existent
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		try {
			OperatorDescription description = OperatorService.getOperatorDescription(operatorKey);
			if (description == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return "";
			}
			operator = description.createOperatorInstance();
			if (operator == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return "";
			}

			Parameters parameters = operator.getParameters();
			if (parameters != null) {
				ParameterType type = parameters.getParameterType(parameterName);
				if (type != null) {
					if (type instanceof ParameterTypeNumber) {
						ParameterTypeNumber numberType = (ParameterTypeNumber) type;
						StringBuilder range = new StringBuilder();
						if (numberType instanceof ParameterTypeInt) {
							int min = ((ParameterTypeInt) numberType).getMinValueInt();
							int max = ((ParameterTypeInt) numberType).getMaxValueInt();

							if (min == -Integer.MAX_VALUE || min == Integer.MIN_VALUE) {
								range.append("-\u221E");
							} else {
								range.append(min);
							}
							range.append(" - ");
							if (max == Integer.MAX_VALUE) {
								range.append("+\u221E");
							} else {
								range.append(max);
							}
						} else if (numberType instanceof ParameterTypeLong) {
							long min = ((ParameterTypeLong) numberType).getMinValuelong();
							long max = ((ParameterTypeLong) numberType).getMaxValuelong();

							if (min == -Long.MAX_VALUE || min == Long.MIN_VALUE) {
								range.append("-\u221E");
							} else {
								range.append(min);
							}
							range.append(" - ");
							if (max == Long.MAX_VALUE) {
								range.append("+\u221E");
							} else {
								range.append(max);
							}
						} else if (numberType instanceof ParameterTypeDouble) {
							double min = numberType.getMinValue();
							double max = numberType.getMaxValue();

							if (min == Double.NEGATIVE_INFINITY) {
								range.append("-\u221E");
							} else {
								range.append(min);
							}
							range.append(" - ");
							if (max == Double.POSITIVE_INFINITY) {
								range.append("+\u221E");
							} else {
								range.append(max);
							}
						}
						return range.toString();
					} else if (type instanceof ParameterTypeCategory) {
						ParameterTypeCategory categoryType = (ParameterTypeCategory) type;
						int number = categoryType.getNumberOfCategories();

						StringBuilder values = new StringBuilder();
						for (int i = 0; i < number && i < MAX_CATEROGIES_DISPLAYED_IN_HELP; i++) {
							if (i > 0) {
								values.append(", ");
							}
							values.append(categoryType.getCategory(i));
						}
						if (number > MAX_CATEROGIES_DISPLAYED_IN_HELP) {
							values.append(", ...");
						}
						return values.toString();
					} else if (type instanceof ParameterTypeStringCategory) {
						ParameterTypeStringCategory stringCategoryType = (ParameterTypeStringCategory) type;
						String[] caterogies = stringCategoryType.getValues();
						int number = caterogies.length;

						StringBuffer values = new StringBuffer();
						for (int i = 0; i < number && i < MAX_CATEROGIES_DISPLAYED_IN_HELP; i++) {
							if (i > 0) {
								values.append(", ");
							}
							values.append(caterogies[i]);
						}
						if (number > MAX_CATEROGIES_DISPLAYED_IN_HELP) {
							values.append(", ...");
						}
						return values.toString();
					} else {
						return type.getRange();
					}

				}
			}
			return "";
		} catch (OperatorCreationException e) {
			LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
					operatorKey);
			return "";
		}
	}

	/**
	 * Gets the default value of the given operator key and parameter name as a string. This is used
	 * if no default value is specified in the documentation xml.
	 *
	 * @param operatorKey
	 *            The key of the operator
	 * @param parameterName
	 *            The name of the parameter
	 * @return A string containing the default value, if one can be found. Empty string else.
	 */
	public static String getParameterDefault(String operatorKey, String parameterName) {
		Operator operator = null;
		int index = operatorKey.indexOf(".");
		// remove operator group if existent
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		try {
			OperatorDescription description = OperatorService.getOperatorDescription(operatorKey);
			if (description == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return "";
			}
			operator = description.createOperatorInstance();
			if (operator == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return "";
			}

			Parameters parameters = operator.getParameters();
			if (parameters != null) {
				ParameterType type = parameters.getParameterType(parameterName);
				if (type != null) {
					if (type.getDefaultValueAsString() != null && !type.getDefaultValueAsString().trim().isEmpty()) {
						return type.getDefaultValueAsString();
					}
				}
			}
			return "";
		} catch (OperatorCreationException e) {
			LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
					operatorKey);
			return "";
		}
	}

	/**
	 * Returns if the given parameter is optional.
	 *
	 * @param operatorKey
	 *            The key of the operator
	 * @param parameterName
	 *            The name of the parameter
	 * @return {@code true} if the parameter is optional. {@code false} if the parameter is not
	 *         optional or if no parameter exists for the given name and operator.
	 */
	public static boolean isParameterOptional(String operatorKey, String parameterName) {
		Operator operator = null;
		int index = operatorKey.indexOf(".");
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		try {
			OperatorDescription description = OperatorService.getOperatorDescription(operatorKey);
			if (description == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return false;
			}
			operator = description.createOperatorInstance();
			if (operator == null) {
				LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
						operatorKey);
				return false;
			}

			Parameters parameters = operator.getParameters();
			if (parameters != null) {
				ParameterType type = parameters.getParameterType(parameterName);
				if (type != null) {
					return type.isOptional();
				}
			}
			return false;
		} catch (OperatorCreationException e) {
			LogService.getRoot().log(Level.FINER, "com.rapidminer.gui.OperatorDocToHtmlConverter.null_operator",
					operatorKey);
			return false;
		}
	}

	/**
	 * Searches for a class with the given name and returns the path of the resource. Used for the
	 * images of the ports' data types.
	 *
	 * @param type
	 *            the class' name as String
	 * @return the path of the resource of the corresponding icon.
	 */
	@SuppressWarnings("unchecked")
	public static String getIconNameForType(String type) {
		String iconName = DEFAULT_IOOBJECT_ICON_NAME;
		if (type != null && !type.isEmpty()) {
			Class<? extends IOObject> typeClass;
			try {
				typeClass = (Class<? extends IOObject>) Class.forName(type);
				iconName = RendererService.getIconName(typeClass);
				if (iconName == null) {
					iconName = DEFAULT_IOOBJECT_ICON_NAME;
				}
			} catch (ClassNotFoundException e) {
				LogService.getRoot().finer("Failed to lookup class '" + type + "'. Reason: " + e.getLocalizedMessage());
			}
		}

		return SwingTools.getIconPath("24/" + iconName);
	}
	
	public static String getTagHtmlForKey(String operatorKey) {
		// operator keys in the documentation begin with "operator.", so remove that
		int index = operatorKey.indexOf(".");
		if (index != -1) {
			operatorKey = operatorKey.substring(index + 1);
		}
		OperatorDescription operatorDescription = OperatorService.getOperatorDescription(operatorKey);
		return getTagHtmlForDescription(operatorDescription);
	}

	static String getTagHtmlForDescription(OperatorDescription operatorDescription) {
		if (operatorDescription != null) {
			List<String> tags = operatorDescription.getTags();
			if(tags != null && !tags.isEmpty()){
				StringBuilder builder = new StringBuilder();
				builder.append("<p class=\"tags\">Tags:");
				for(int i=0; i<tags.size(); i++){
					String tag = tags.get(i);
					builder.append(" <a class=\"tags\" href=\"tag:" + tag + "\">" + tag + "</a>");
					if(i != tags.size() - 1){
						builder.append(",");
					}
				}
				builder.append("</p>");
				return builder.toString();
			}
		}
		
		// no tags found
		return "";
	}
}
