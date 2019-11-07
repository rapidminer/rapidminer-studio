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
package com.rapidminer.operator;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.groovy.GroovyBugError;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPortExtender;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.TextType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.plugin.Plugin;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;


/**
 * <p>
 * This operator can be used to execute arbitrary Groovy scripts. This basically means that analysts
 * can write their own operators directly within the process by specifiying Java code and / or a
 * Groovy script which will be interpreted and executed during process runtime. For a complete
 * reference of Groovy scripting please refer to http://groovy.codehaus.org/.
 * </p>
 *
 * <p>
 * In addition to the usual scripting code elements from Groovy, the RapidMiner scripting operator
 * defines some special scripting elements:
 * </p>
 * <ul>
 * <li>If you use the standard <em>imports</em>, all important types like Example, ExampleSet,
 * Attribute, Operator etc. as well as the most important Java types like collections etc. are
 * automatically imported and can directly be used within the script. Hence, there is no need for
 * importing them in your script. However, you can of course import any other class you want and use
 * this in your script.</li>
 * <li>The <em>current operator</em> (the scripting operator for which you define the script) is
 * referenced by <code>operator</code>.<br />
 * Example: <code>operator.log("text")</code></li>
 * <li>All <em>operator methods</em> like <code>log</code> (see above), accessing the input or the
 * complete process can directly be used by writing a preceding <code>operator</code>.<br />
 * Example: <code>operator.getProcess()</code></li>
 * <li><em>Input of the operator</em> can be retrieved via the input method getInput(Class) of the
 * surrounding operator.<br />
 * Example: <code>ExampleSet exampleSet = operator.getInput(ExampleSet.class)</code></li>
 * <li>You can <em>iterate over examples</em> with the following construct:<br />
 * <code>for (Example example : exampleSet) { ... }</code></li>
 * <li>You can <em>retrieve example values</em> with the shortcut<br />
 * <code>String value = example[&quot;attribute_name&quot;];</code> or <br />
 * <code>double value = example[&quot;attribute_name&quot;];</code></li>
 * <li>You can <em>set example values</em> with
 * <code>example[&quot;attribute_name&quot;] = &quot;value&quot;;</code> or <br />
 * <code>example[&quot;attribute_name&quot;] = 5.7;</code></li>
 * </ul>
 *
 * <p>
 * <em>Note:</em> Scripts written for this operator may access Java code. Scripts may hence become
 * incompatible in future releases of RapidMiner.
 * </p>
 *
 * <p>
 * <em>Note:</em> As of RapidMiner Studio 7.5, Execute Script is now capable of executing many
 * scripts in parallel by implementing script caching. Before that, each execution parsed its script
 * again which was done on a global lock.
 * </p>
 *
 * @author Simon Fischer, Ingo Mierswa, Marco Boeck
 */
public class ScriptingOperator extends Operator {

	/**
	 * Binding delegator capable of multi-threaded access. Using a regular binding on a script which
	 * is run concurrently would result in the last set binding to be used.
	 *
	 * @author Marco Boeck
	 * @since 7.5
	 */
	private static class ConcurrentBindingDelegator extends Binding {

		private final ThreadLocal<Binding> binding = ThreadLocal.withInitial(Binding::new);

		@Override
		public Object getVariable(String name) {
			return binding.get().getVariable(name);
		}

		@Override
		public void setVariable(String name, Object value) {
			binding.get().setVariable(name, value);
		}

		@Override
		public boolean hasVariable(String name) {
			return binding.get().hasVariable(name);
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Map getVariables() {
			return binding.get().getVariables();
		}

		@Override
		public Object getProperty(String property) {
			return binding.get().getProperty(property);
		}

		@Override
		public void setProperty(String property, Object newValue) {
			binding.get().setProperty(property, newValue);
		}

	}

	private InputPortExtender inExtender = new InputPortExtender("input", getInputPorts());
	private OutputPortExtender outExtender = new OutputPortExtender("output", getOutputPorts());

	public static final String PARAMETER_SCRIPT = "script";

	public static final String GROOVY_DOMAIN = "/groovyscript";

	public static final String PARAMETER_STANDARD_IMPORTS = "standard_imports";

	/** the max number of entries in the script cache */
	private static final int MAX_CACHE_SIZE = 500;

	/**
	 * this map contains lock objects for each script. This is necessary because we only want to
	 * block a particular script which has not yet been parsed, but not other scripts. If there was
	 * only synchronization on a static object, all script execution would be blocked JVM-wide
	 * (think background process execution or RM Server) until parsing is finished.
	 */
	private static final Map<String, Object> LOCK_MAP = Collections
			.synchronizedMap(new LinkedHashMap<String, Object>(MAX_CACHE_SIZE + 1, 0.75f, true) {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
					return size() > MAX_CACHE_SIZE;
				}
			});

	/**
	 * this map contains the parsed scripts. This greatly speeds up operator execution, especially
	 * in loops. Will drop the oldest scripts that have not been used if the max cache size is
	 * exceeded.
	 */
	private static final Map<String, Script> SCRIPT_CACHE = Collections
			.synchronizedMap(new LinkedHashMap<String, Script>(MAX_CACHE_SIZE + 1, 0.75f, true) {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean removeEldestEntry(Map.Entry<String, Script> eldest) {
					return size() > MAX_CACHE_SIZE;
				}
			});

	public ScriptingOperator(OperatorDescription description) {
		super(description);
		inExtender.start();
		outExtender.start();
	}

	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		try {
			String scriptWithoutReplacedMacros = getParameters().getParameter(PARAMETER_SCRIPT);
			int startIndex = scriptWithoutReplacedMacros.indexOf(Operator.MACRO_STRING_START);
			int endIndex = scriptWithoutReplacedMacros.indexOf(Operator.MACRO_STRING_END, startIndex);
			if (startIndex > -1 && endIndex > -1) {
				addError(new SimpleProcessSetupError(Severity.WARNING, getPortOwner(), "script_has_macro"));
			}
		} catch (UndefinedParameterError e) {
			// should not happen - ignore
		}
	}

	@Override
	public void doWork() throws OperatorException {
		String script = getParameterAsString(PARAMETER_SCRIPT);
		if (getParameterAsBoolean(PARAMETER_STANDARD_IMPORTS)) {
			StringBuilder imports = new StringBuilder();
			imports.append("import com.rapidminer.example.*;\n");
			imports.append("import com.rapidminer.example.set.*;\n");
			imports.append("import com.rapidminer.example.table.*;\n");
			imports.append("import com.rapidminer.operator.*;\n");
			imports.append("import com.rapidminer.tools.Tools;\n");
			imports.append("import java.util.*;\n");

			script = imports.toString() + script;
		}

		List<IOObject> input = inExtender.getData(IOObject.class, false);
		try {
			convertIOTables(input);
		} catch (BeltConverter.ConversionException e) {
			throw new UserError(this, "scriptingOperator.custom_columns", e.getColumnName(), e.getType().customTypeID());
		}
		Object result;
		try {
			// cache access is synchronized on a per-script basis to prevent Execute Script
			// inside a loop to start many parsings at the same time
			Object lock;
			synchronized (LOCK_MAP) {
				lock = LOCK_MAP.computeIfAbsent(script, s -> new Object());
			}

			Script cachedScript;
			synchronized (lock) {
				cachedScript = SCRIPT_CACHE.get(script);
				if (cachedScript == null) {
					// use the delegator which is capable of handling multi-threaded access as
					// binding
					GroovyShell shell = new GroovyShell(Plugin.getMajorClassLoader(), new ConcurrentBindingDelegator());
					GroovyCodeSource codeSource = new GroovyCodeSource(script, "customScript", GROOVY_DOMAIN);
					codeSource.setCachable(false);
					cachedScript = shell.parse(codeSource);
					SCRIPT_CACHE.put(script, cachedScript);
				}
			}

			// even though we cache the script, we need to use a new binding for each execution to
			// avoid multiple concurrent scripts running on the same/editing the same binding
			cachedScript.getBinding().setVariable("input", input);
			cachedScript.getBinding().setVariable("operator", this);

			// run the script via the delegator
			result = cachedScript.run();
		} catch (SecurityException e) {
			throw new UserError(this, e, "scriptingOperator_security", e.getMessage());
		} catch (GroovyBugError e) {
			if (e.getCause() instanceof SecurityException) {
				throw new UserError(this, e.getCause(), "scriptingOperator_security", e.getCause().getMessage());
			} else {
				throw new UserError(this, e, 945, "Groovy", e);
			}
		} catch (Throwable e) {
			throw new UserError(this, e, 945, "Groovy", e, ExceptionUtils.getStackTrace(e));
		}

		if (result instanceof Object[]) {
			outExtender.deliver(Arrays.asList((IOObject[]) result));
		} else if (result instanceof List) {
			List<IOObject> results = new LinkedList<>();
			for (Object single : (List<?>) result) {
				if (single instanceof IOObject) {
					results.add((IOObject) single);
				} else {
					getLogger().warning("Unknown result type: " + single);
				}
			}
			outExtender.deliver(results);
		} else {
			if (result != null) {
				if (result instanceof IOObject) {
					outExtender.deliver(Collections.singletonList((IOObject) result));
				} else {
					getLogger().warning("Unknown result: " + result.getClass() + ": " + result);
				}
			}
		}
	}

	/**
	 * Since the script does unchecked casts to {@link com.rapidminer.example.ExampleSet}s, we need to convert belt
	 * tables here. Later, when more operators return belt tables, we should introduce a compatibility level for this.
	 *
	 * @throws BeltConverter.ConversionException
	 * 		if a table cannot be converted because it contains custom columns
	 */
	private void convertIOTables(List<IOObject> input) {
		ConcurrencyContext concurrencyContext = Resources.getConcurrencyContext(this);
		for (int i = 0; i < input.size(); i++) {
			IOObject object = input.get(i);
			if (object instanceof IOTable) {
				input.set(i, BeltConverter.convert((IOTable) object, concurrencyContext));
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeText(PARAMETER_SCRIPT, "The script to execute.", TextType.GROOVY, false);
		type.setExpert(false);
		type.setPrimary(true);
		type.setDefaultValue("/* \n" + " * You can use both Java and Groovy syntax in this script.\n"
				+ " * \n * Note that you have access to the following two predefined variables:\n"
				+ " * 1) input (an array of all input data)\n"
				+ " * 2) operator (the operator instance which is running this script)\n" + " */\n" + "\n"
				+ "// Take first input data and treat it as generic IOObject\n"
				+ "// Alternatively, you could treat it as an ExampleSet if it is one:\n"
				+ "// ExampleSet inputData = input[0];\n" + "IOObject inputData = input[0];\n" + "\n\n"
				+ "// You can add any code here\n" + "\n" + "\n"
				+ "// This line returns the first input as the first output\n" + "return inputData;");
		types.add(type);
		types.add(new ParameterTypeBoolean(PARAMETER_STANDARD_IMPORTS,
				"Indicates if standard imports for examples and attributes etc. should be automatically generated.", true));
		return types;
	}
}
