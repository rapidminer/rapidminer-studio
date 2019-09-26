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
package com.rapidminer.parameter;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.rapidminer.operator.Operator;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Tools;


/**
 * This class is a collection of the parameter values of a single operator. Instances of
 * <code>Parameters</code> are created with respect to the declared list of
 * <code>ParameterTypes</code> of an operator. If parameters are set using the
 * <code>setParameter()</code> method and the value exceeds the range, it is automatically
 * corrected. If parameters are queried that are not set, their default value is returned. <br/>
 * Upon setting the parameters, observers are notified, where the argument to the
 * {@link Observer#update()} method will be the key of the changed parameter.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class Parameters extends AbstractObservable<String> implements Cloneable, Iterable<String> {

	public static final char PAIR_SEPARATOR = '\u241D';

	public static final char RECORD_SEPARATOR = '\u241E';

	/** Maps parameter keys (i.e. Strings) to their value (Objects). */
	private final Map<String, String> keyToValueMap = new LinkedHashMap<String, String>();

	/** Maps parameter keys (i.e. Strings) to their <code>ParameterType</code>. */
	private final Map<String, ParameterType> keyToTypeMap = new LinkedHashMap<String, ParameterType>();

	/** Creates an empty parameters object without any parameter types. */
	public Parameters() {}

	/**
	 * Constructs an instance of <code>Parameters</code> for the given list of
	 * <code>ParameterTypes</code>. The list might be empty but not null.
	 */
	public Parameters(List<ParameterType> parameterTypes) {
		for (ParameterType type : parameterTypes) {
			addParameterType(type);
		}
	}

	/**
	 * Returns a list of <tt>ParameterTypes</tt> describing the parameters of this operator. This
	 * list will be generated during construction time of the Operator.
	 */
	public Collection<ParameterType> getParameterTypes() {
		return keyToTypeMap.values();
	}

	public void addParameterType(ParameterType type) {
		keyToTypeMap.put(type.getKey(), type);
	}

	/** Performs a deep clone on this parameters object. */
	@Override
	public Object clone() {
		Parameters clone = new Parameters();

		clone.keyToValueMap.putAll(keyToValueMap);
		clone.keyToTypeMap.putAll(keyToTypeMap);

		return clone;
	}

	@Override
	public Iterator<String> iterator() {
		return keyToTypeMap.keySet().iterator();
	}

	/** Returns the type of the parameter with the given type. */
	public ParameterType getParameterType(String key) {
		return keyToTypeMap.get(key);
	}

	/**
	 * Sets the parameter for the given key after performing a range-check. This method returns true
	 * if the type was known and false if no parameter type was defined for this key.
	 */
	public boolean setParameter(String key, String value) {
		ParameterType parameterType = keyToTypeMap.get(key);
		if (value == null) {
			keyToValueMap.remove(key);
		} else {
			if (parameterType != null) {
				value = parameterType.transformNewValue(value);
			}
			keyToValueMap.put(key, value);
		}
		fireUpdate(key);
		return parameterType != null;
	}

	/**
	 * Sets the parameter without performing a range and type check.
	 * 
	 * @deprecated Please use the method {@link #setParameter(String, String)} instead
	 */
	@Deprecated
	public void setParameterWithoutCheck(String key, String value) {
		setParameter(key, value);
	}

	/**
	 * Returns the value of the given parameter. If it was not yet set, the default value is set now
	 * and a log message is issued. If the <code>ParameterType</code> does not provide a default
	 * value, this may result in an error message. In subsequent calls of this method, the parameter
	 * will be set. An OperatorException (UserError) will be thrown if a non-optional parameter was
	 * not set.
	 */
	public String getParameter(String key) throws UndefinedParameterError {
		if (keyToValueMap.containsKey(key)) {
			return keyToValueMap.get(key);
		} else {
			ParameterType type = keyToTypeMap.get(key);
			if (type == null) {
				return null;
			}
			Object defaultValue = type.getDefaultValue();
			if ((defaultValue == null) && !type.isOptional()) {
				// LogService.getRoot().fine("Parameter '" + key +
				// "' is not set and has no default value.");//
				// +Arrays.toString(Thread.currentThread().getStackTrace()));
				LogService.getRoot().log(Level.FINE,
						"com.rapidminer.parameter.Parameters.parameter_not_set_no_default_value", key);
				throw new UndefinedParameterError(key);
			} else {
				// LogService.getRoot().finer("Parameter '" + key + "' is not set. Using default ('"
				// + type.toString(defaultValue) + "').");
				LogService.getRoot().log(Level.FINER, "com.rapidminer.parameter.Parameters.parameter_not_set_using_default",
						new Object[] { key, type.toString(defaultValue) });

			}
			if (defaultValue == null) {
				return null;
			} else {
				return type.toString(defaultValue);
			}
		}
	}

	/**
	 * Returns the value of the parameter as specified by the process definition file (without
	 * substituting default values etc.)
	 */
	public String getParameterAsSpecified(String key) {
		return keyToValueMap.get(key);
	}

	/** As {@link #getParameter(String)}, but returns null rather than throwing an exception. */
	public String getParameterOrNull(String key) {
		if (keyToValueMap.containsKey(key)) {
			return keyToValueMap.get(key);
		} else {
			ParameterType type = keyToTypeMap.get(key);
			if (type == null) {
				return null;
			}
			Object value = type.getDefaultValue();
			if ((value == null) && !type.isOptional()) {
				// LogService.getRoot().finer("Parameter '" + key + "' is not set. Using null.");
				LogService.getRoot().log(Level.FINER, "com.rapidminer.parameter.Parameters.parameter_not_set_using_null",
						key);
				return null;
			} else {
				// LogService.getRoot().finer("Parameter '" + key + "' is not set. Using default ('"
				// + type.toString(value) + "').");
				LogService.getRoot().log(Level.FINER, "com.rapidminer.parameter.Parameters.parameter_not_set_using_default",
						new Object[] { key, type.toString(value) });
			}
			if (value == null) {
				return null;
			} else {
				return type.toString(value);
			}
		}
	}

	/**
	 * Returns a set view of all parameter keys defined by parameter types.
	 */
	public Set<String> getKeys() {
		return keyToTypeMap.keySet();
	}

	/**
	 * This method returns the keys of all defined values. This might be disjunct with the keys
	 * defined in the keyToType map, because the operator might contain parameter values, which do
	 * not correspond to any parameter type. This is used for example during import, when parameters
	 * are renamed.
	 */
	public Set<String> getDefinedKeys() {
		return keyToValueMap.keySet();
	}

	/**
	 * Returns true if the given parameters are not null and are the same like this parameters.
	 */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Parameters)) {
			return false;
		} else {
			Parameters p = (Parameters) o;
			return p.keyToValueMap.equals(this.keyToValueMap);
		}
	}

	@Override
	public int hashCode() {
		return keyToValueMap.hashCode();
	}

	/** Appends the elements describing these Parameters to the given element. */
	public void appendXML(Element toElement, boolean hideDefault, Document doc) {
		for (String key : keyToTypeMap.keySet()) {
			String value = keyToValueMap.get(key);
			ParameterType type = keyToTypeMap.get(key);
			Element paramElement;
			if (type != null) {
				paramElement = type.getXML(key, value, hideDefault, doc);
			} else {
				paramElement = doc.createElement("parameter");
				paramElement.setAttribute("key", key);
				paramElement.setAttribute("value", value.toString());
			}
			if (paramElement != null) {
				toElement.appendChild(paramElement);
			}
		}
	}

	/**
	 * Writes a portion of the xml configuration file specifying the parameters that differ from
	 * their default value.
	 * 
	 * @deprecated Use the DOM version of this method (
	 *             {@link #appendXML(Element, boolean, Document)}).
	 */
	@Deprecated
	public String getXML(String indent, boolean hideDefault) {
		StringBuffer result = new StringBuffer();
		Iterator<String> i = keyToTypeMap.keySet().iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = keyToValueMap.get(key);
			ParameterType type = keyToTypeMap.get(key);
			if (type != null) {
				result.append(type.getXML(indent, key, value, hideDefault));
			} else {
				result.append(indent + "<parameter key=\"" + key + "\"\tvalue=\"" + value.toString() + "\"/>"
						+ Tools.getLineSeparator());
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		return this.keyToValueMap.toString();
	}

	@Deprecated
	/**
	 * This method has been moved to ParameterTypeList
	 */
	public static String transformList2String(List<String[]> parameterList) {
		return ParameterTypeList.transformList2String(parameterList);
	}

	@Deprecated
	/**
	 * This method has been moved to ParameterTypeList
	 */
	public static List<String[]> transformString2List(String listString) {
		return ParameterTypeList.transformString2List(listString);
	}

	/** Notify all set parameters of the operator renaming */
	public void notifyRenaming(String oldName, String newName) {
		if (!Objects.equals(oldName, newName)) {
			notifyRenameReplace((t, v) -> t.notifyOperatorRenaming(oldName, newName, v));
		}
	}

	/**
	 * This method is called when the operator given by {@code oldName} (and {@code oldOp} if it is not {@code null})
	 * was replaced with the operator described by {@code newName} and {@code newOp}.
	 * This will inform all set {@link ParameterType parameters} of the replacing.
	 *
	 * @param oldName
	 * 		the name of the old operator
	 * @param oldOp
	 * 		the old operator; can be {@code null}
	 * @param newName
	 * 		the name of the new operator
	 * @param newOp
	 * 		the new operator; must not be {@code null}
	 * @see ParameterType#notifyOperatorReplacing(String, Operator, String, Operator, String)
	 * @since 9.3
	 */
	public void notifyReplacing(String oldName, Operator oldOp, String newName, Operator newOp) {
		notifyRenameReplace((t, v) -> t.notifyOperatorReplacing(oldName, oldOp, newName, newOp, v));
	}

	/** @since 9.3 */
	private void notifyRenameReplace(BiFunction<ParameterType, String, String> replacer) {
		for (Entry<String, String> entry : keyToValueMap.entrySet()) {
			ParameterType type = keyToTypeMap.get(entry.getKey());
			if (type != null && entry.getValue() != null) {
				entry.setValue(replacer.apply(type, entry.getValue()));
			}
		}
		keyToValueMap.values().removeIf(Objects::isNull);
	}

	/** Renames a parameter, e.g. during importing old XML process files. */
	public void renameParameter(String oldAttributeName, String newAttributeName) {
		String value = keyToValueMap.get(oldAttributeName);
		if (value != null) {
			keyToValueMap.remove(oldAttributeName);
			keyToValueMap.put(newAttributeName, value);
		}
	}

	/**
	 * Returns true if the parameter is set or has a default value.
	 * 
	 * @see Parameters#isSpecified(String)
	 */
	public boolean isSet(String parameterKey) {
		if (keyToValueMap.containsKey(parameterKey)) {
			return true;
		} else {
			// check for default if we have a type registered for this key
			ParameterType type = keyToTypeMap.get(parameterKey);
			if (type == null) {
				return false;
			}
			return type.getDefaultValue() != null;
		}
	}

	/**
	 * Returns true iff the parameter value was explicitly set (as opposed to {@link #isSet(String)}
	 * which also takes into account a possible default value.
	 */
	public boolean isSpecified(String key) {
		return keyToValueMap.containsKey(key);
	}

	public void copyFrom(Parameters parameters) {
		this.keyToValueMap.putAll(parameters.keyToValueMap);
		fireUpdate();
	}

	/**
	 * Clears the parameters map and adds all given parameters. This will not reset the already
	 * registered types so you have to keep track that you don't set parameters not defined for this
	 * operator!
	 * 
	 * @param parameters
	 */
	public void setAll(Parameters parameters) {
		keyToValueMap.clear();
		keyToValueMap.putAll(parameters.keyToValueMap);
		fireUpdate();
	}

	/**
	 * Adds all Parameters to the parameters map.
	 * 
	 * @param parameters
	 */
	public void addAll(Parameters parameters) {
		keyToValueMap.putAll(parameters.keyToValueMap);
		fireUpdate();
	}
}
