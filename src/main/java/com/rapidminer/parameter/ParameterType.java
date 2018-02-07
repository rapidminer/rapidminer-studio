/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.parameter;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;


/**
 * A ParameterType holds information about type, range, and default value of a parameter. Lists of
 * ParameterTypes are provided by operators.
 * <p>
 * Sensitive information: <br/>
 * If your parameter is expected to never contain sensitive data, e.g. the number of iterations for
 * an algorithm, overwrite {@link #isSensitive()} and and return {@code false}. This method is used
 * to determine how parameters are handled in certain places, e.g. if their value is replaced before
 * uploading the process for operator recommendations. The default implementation always returns
 * {@code true}.
 * </p>
 *
 * @author Ingo Mierswa, Simon Fischer
 * @see com.rapidminer.operator.Operator#getParameterTypes()
 */
public abstract class ParameterType implements Comparable<ParameterType>, Serializable {

	private static final long serialVersionUID = 5296461242851710130L;

	public static final String ELEMENT_PARAMETER_TYPE = "ParameterType";

	private static final String ELEMENT_DESCRIPTION = "Description";

	private static final String ELEMENT_CONDITIONS = "Conditions";

	private static final String ATTRIBUTE_EXPERT = "is-expert";

	private static final String ATTRIBUTE_DEPRECATED = "is-deprecated";

	private static final String ATTRIBUTE_HIDDEN = "is-hidden";

	private static final String ATTRIBUTE_OPTIONAL = "is-optional";

	private static final String ATTRIBUTE_SHOW_RANGE = "is-range-shown";

	private static final String ATTRIBUTE_KEY = "key";

	private static final String ATTRIBUTE_CONDITION_CLASS = "condition-class";

	private static final String ATTRIBUTE_CLASS = "class";

	/** The key of this parameter. */
	private String key;

	/** The documentation. Used as tooltip text... */
	private String description;

	/**
	 * Indicates if this is a parameter only viewable in expert mode. Mandatory parameters are
	 * always viewable. The default value is true.
	 */
	private boolean expert = true;

	/**
	 * Indicates if this parameter is hidden and is not shown in the GUI. May be used in conjunction
	 * with a configuration wizard which lets the user configure the parameter.
	 */
	private boolean isHidden = false;

	/** Indicates if the range should be displayed. */
	private boolean showRange = true;

	/**
	 * Indicates if this parameter is optional unless a dependency condition made it mandatory.
	 */
	private boolean isOptional = true;

	/**
	 * Indicates that this parameter is deprecated and remains only for compatibility reasons during
	 * loading of older processes. It should neither be shown nor documented.
	 */
	private boolean isDeprecated = false;

	/**
	 * This collection assembles all conditions to be met to show this parameter within the gui.
	 */
	private final Collection<ParameterCondition> conditions = new LinkedList<>();

	/**
	 * This is the inversed constructor to {@link #getDefinitionAsXML(Document)}. It will reload all
	 * settings of this {@link ParameterType} from the given XML Element. Subclasses MUST implement
	 * this constructor, since it is called by reflection.
	 *
	 * @throws XMLException
	 */
	public ParameterType(Element element) throws XMLException {
		loadDefinitionFromXML(element);
	}

	/** Creates a new ParameterType. */
	public ParameterType(String key, String description) {
		this.key = key;
		this.description = description;
	}

	public abstract Element getXML(String key, String value, boolean hideDefault, Document doc);

	/** Returns a human readable description of the range. */
	public abstract String getRange();

	/** Returns a value that can be used if the parameter is not set. */
	public abstract Object getDefaultValue();

	/**
	 * Returns the correct string representation of the default value. If the default is undefined,
	 * it returns null.
	 */
	public String getDefaultValueAsString() {
		return toString(getDefaultValue());
	}

	/** Sets the default value. */
	public abstract void setDefaultValue(Object defaultValue);

	/**
	 * Returns true if the values of this parameter type are numerical, i.e. might be parsed by
	 * {@link Double#parseDouble(String)}. Otherwise false should be returned. This method might be
	 * used by parameter logging operators.
	 */
	public abstract boolean isNumerical();

	/**
	 * Writes an xml representation of the given key-value pair.
	 *
	 * @deprecated Use the DOM version of this method. At the moment, we cannot delete it, because
	 *             {@link Parameters#equals(Object)} and {@link Parameters#hashCode()} rely on it.
	 */
	@Deprecated
	public abstract String getXML(String indent, String key, String value, boolean hideDefault);

	public boolean showRange() {
		return showRange;
	}

	public void setShowRange(boolean showRange) {
		this.showRange = showRange;
	}

	/**
	 * This method will be invoked by the Parameters after a parameter was set. The default
	 * implementation is empty but subclasses might override this method, e.g. for a decryption of
	 * passwords.
	 */
	public String transformNewValue(String value) {
		return value;
	}

	/**
	 * Returns true if this parameter can only be seen in expert mode. The default implementation
	 * returns true if the parameter is optional. It is ensured that an non-optional parameter is
	 * never expert!
	 */
	public boolean isExpert() {
		return expert && isOptional;
	}

	/**
	 * Sets if this parameter can be seen in expert mode (true) or beginner mode (false).
	 *
	 */
	public void setExpert(boolean expert) {
		this.expert = expert;
	}

	/**
	 * Returns true if this parameter is hidden or not all dependency conditions are fulfilled. Then
	 * the parameter will not be shown in the GUI. The default implementation returns true which
	 * should be the normal case.
	 *
	 * Please note that this method cannot be accessed during getParameterTypes() method
	 * invocations, because it relies on getting the Parameters object, which is then not created.
	 */
	public boolean isHidden() {
		boolean conditionsMet = true;
		for (ParameterCondition condition : conditions) {
			conditionsMet &= condition.dependencyMet();
		}
		return isDeprecated || isHidden || !conditionsMet;
	}

	public Collection<ParameterCondition> getConditions() {
		return Collections.unmodifiableCollection(conditions);
	}

	/**
	 * Sets if this parameter is hidden (value true) and will not be shown in the GUI.
	 */
	public void setHidden(boolean hidden) {
		this.isHidden = hidden;
	}

	/**
	 * This returns whether this parameter is deprecated.
	 */
	public boolean isDeprecated() {
		return this.isDeprecated;
	}

	/**
	 * This method indicates that this parameter is deprecated and isn't used anymore beside from
	 * loading old process files.
	 */
	public void setDeprecated() {
		this.isDeprecated = true;
	}

	/**
	 * This sets if the parameter is optional or must be entered. If it is not optional, it may not
	 * be an expert parameter and the expert status will be ignored!
	 */
	public final void setOptional(boolean isOptional) {
		this.isOptional = isOptional;
	}

	/** Registers the given dependency condition. */
	public void registerDependencyCondition(ParameterCondition condition) {
		this.conditions.add(condition);
	}

	public Collection<ParameterCondition> getDependencyConditions() {
		return this.conditions;
	}

	/**
	 * Returns true if this parameter is optional. The default implementation returns true. Please
	 * note that this method cannot be accessed during {@link Operator#getParameterTypes()} method
	 * invocations, because it relies on getting the Parameters object, which is then not created.
	 *
	 */
	public final boolean isOptional() {
		if (isOptional) {
			// if parameter is optional per default: check conditions
			boolean becomeMandatory = false;
			for (ParameterCondition condition : conditions) {
				if (condition.dependencyMet()) {
					becomeMandatory |= condition.becomeMandatory();
				} else {
					return true;
				}
			}
			return !becomeMandatory;
		}
		// otherwise it is mandatory even without dependency
		return false;
	}

	/**
	 * Checks whether the parameter is configured to be optional without looking at the parameter
	 * conditions. This method can be invoked during {@link Operator#getParameterTypes()} method
	 * invocations as it does not check parameter conditions. It does not reflect the actual state
	 * though as parameter conditions might change an optional parameter to become mandatory.
	 *
	 * @return whether the parameter is optional without checking the parameter conditions
	 */
	public final boolean isOptionalWithoutConditions() {
		return isOptional;
	}

	/** Sets the key. */
	public void setKey(String key) {
		this.key = key;
	}

	/** Returns the key. */
	public String getKey() {
		return key;
	}

	/** Returns a short description. */
	public String getDescription() {
		return description;
	}

	/** Sets the short description. */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * States whether a given parameter type implementation may contain sensitive information or
	 * not. Sensitive information are obvious things like passwords, files, OAuth tokens, or
	 * database connections. However less obvious things like SQL queries can also contain sensitive
	 * information.
	 *
	 * @return always {@code true}
	 */
	public boolean isSensitive() {
		return true;
	}

	/**
	 * This method gives a hook for the parameter type to react on a renaming of an operator. It
	 * must return the correctly modified String value. The default implementation does nothing.
	 */
	public String notifyOperatorRenaming(String oldOperatorName, String newOperatorName, String parameterValue) {
		return parameterValue;
	}

	/** Returns a string representation of this value. */
	public String toString(Object value) {
		if (value == null) {
			return "";
		} else {
			return value.toString();
		}
	}

	public String toXMLString(Object value) {
		return Tools.escapeXML(toString(value));
	}

	@Override
	public String toString() {
		return key + " (" + description + ")";
	}

	/**
	 * Can be called in order to report an illegal parameter value which is encountered during
	 * <tt>checkValue()</tt>.
	 */
	public void illegalValue(Object illegal, Object corrected) {
		LogService.getRoot().log(Level.WARNING, "com.rapidminer.parameter.ParameterType.illegal_value_for_parameter",
				new Object[] { illegal, key, corrected.toString() });
	}

	@Override
	public int compareTo(ParameterType o) {
			/* ParameterTypes are compared by key. */
			return this.key.compareTo(o.key);
		}

	/**
	 * This method operates on the internal string representation of parameter values and replaces
	 * macro expressions of the form %{macroName}.
	 */
	public abstract String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError;

	/**
	 * This method replaces predefined macro values. It is called right after
	 * {@link #substituteMacros(String, MacroHandler)}.
	 * <p>
	 * Override this method in case a custom parameter type should not replace predefined macros on
	 * parameter value fetching.
	 *
	 * @param parameterValue
	 *            the parameter value which is the result of
	 *            {@link #substituteMacros(String, MacroHandler)}
	 * @param operator
	 *            the calling operator. Must not be <code>null</code>.
	 * @return the parameter string with replaced predefined macros
	 * @throws UndefinedParameterError
	 *             in case a predefined macro is malformed
	 */
	public String substitutePredefinedMacros(String parameterValue, Operator operator) throws UndefinedParameterError {
		return operator.getProcess().getMacroHandler().resolvePredefinedMacros(parameterValue, operator);
	}

	/**
	 * This method will write the definition of this {@link ParameterType} into the {@link Element}
	 * that is returned. This XML representation can be used to load the {@link ParameterType} later
	 * on again using the static {@link #createType(Element)} method.
	 */
	public final Element getDefinitionAsXML(Document document) {
		Element typeElement = document.createElement(ELEMENT_PARAMETER_TYPE);
		// class name for reconstruction
		typeElement.setAttribute(ATTRIBUTE_CLASS, this.getClass().getCanonicalName());

		// simple properties
		typeElement.setAttribute(ATTRIBUTE_KEY, key);
		typeElement.setAttribute(ATTRIBUTE_EXPERT, expert + "");
		typeElement.setAttribute(ATTRIBUTE_HIDDEN, isHidden + "");
		typeElement.setAttribute(ATTRIBUTE_DEPRECATED, isDeprecated + "");
		typeElement.setAttribute(ATTRIBUTE_SHOW_RANGE, showRange + "");
		typeElement.setAttribute(ATTRIBUTE_OPTIONAL, isOptional + "");

		// description
		XMLTools.addTag(typeElement, ELEMENT_DESCRIPTION, description);

		// conditions
		Element conditionsElement = XMLTools.addTag(typeElement, ELEMENT_CONDITIONS);
		for (ParameterCondition condition : conditions) {
			Element conditionElement = condition.getDefinitionAsXML(document);

			// setting class name for reconstruction
			conditionElement.setAttribute(ATTRIBUTE_CONDITION_CLASS, condition.getClass().getName());
			conditionsElement.appendChild(conditionElement);
		}

		writeDefinitionToXML(typeElement);

		return typeElement;
	}

	/**
	 * Subclasses must store all their properties inside the typeElement and must be able to reload
	 * it from their using the constructor (Operator operator, Element element). This constructor is
	 * called via reflection. This method should be abstract, but in order to keep the class
	 * compatible with existing extensions, this only throws an unsupported exception.
	 */
	protected void writeDefinitionToXML(Element typeElement) {
		throw new UnsupportedOperationException("The Subclass " + this.getClass().getCanonicalName()
				+ " must override the method getDefinitionAsXML(Element) of the super type "
				+ ParameterType.class.getCanonicalName());
	}

	private void loadDefinitionFromXML(Element typeElement) throws XMLException {
		// simple properties
		key = typeElement.getAttribute(ATTRIBUTE_KEY);
		expert = Boolean.parseBoolean(typeElement.getAttribute(ATTRIBUTE_EXPERT));
		isHidden = Boolean.parseBoolean(typeElement.getAttribute(ATTRIBUTE_HIDDEN));
		isDeprecated = Boolean.parseBoolean(typeElement.getAttribute(ATTRIBUTE_DEPRECATED));
		isOptional = Boolean.parseBoolean(typeElement.getAttribute(ATTRIBUTE_OPTIONAL));
		showRange = Boolean.parseBoolean(typeElement.getAttribute(ATTRIBUTE_SHOW_RANGE));

		// description
		description = XMLTools.getTagContents(typeElement, ELEMENT_DESCRIPTION);

		// conditions
		try {
			Collection<Element> conditionElements = XMLTools.getChildElements(XMLTools.getChildElement(typeElement,
					ELEMENT_CONDITIONS, true));
			for (Element conditionElement : conditionElements) {
				String className = conditionElement.getAttribute(ATTRIBUTE_CONDITION_CLASS);
				Class<?> conditionClass = Class.forName(className);
				Constructor<?> constructor = conditionClass.getConstructor(Element.class);
				conditions.add((ParameterCondition) constructor.newInstance(conditionElement));
			}
		} catch (ClassNotFoundException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		} catch (IllegalArgumentException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		} catch (InstantiationException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		} catch (IllegalAccessException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		} catch (InvocationTargetException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		} catch (SecurityException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		} catch (NoSuchMethodException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CONDITION_CLASS, e);
		}
	}

	/**
	 * This creates the ParameterType defined by the given element for the given operator.
	 *
	 * @throws XMLException
	 */
	public static ParameterType createType(Element element) throws XMLException {
		String className = element.getAttribute(ATTRIBUTE_CLASS);
		try {
			Class<?> typeClass = Class.forName(className);
			Constructor<?> constructor = typeClass.getConstructor(Element.class);
			Object type = constructor.newInstance(element);
			return (ParameterType) type;
		} catch (ClassNotFoundException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (SecurityException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (NoSuchMethodException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (IllegalArgumentException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (InstantiationException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (IllegalAccessException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (InvocationTargetException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		} catch (ClassCastException e) {
			throw new XMLException("Illegal value for attribute " + ATTRIBUTE_CLASS, e);
		}
	}
}
