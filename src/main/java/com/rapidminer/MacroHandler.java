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
package com.rapidminer;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.logging.Level;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.Value;
import com.rapidminer.parameter.UndefinedMacroError;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * This class can be used to store macros for an process which can be defined by the operator
 * {@link com.rapidminer.operator.MacroDefinitionOperator}. It also defines some standard macros
 * like the process path or file name.
 *
 * @author Ingo Mierswa
 */
public class MacroHandler extends Observable {

	public static final String PROCESS_NAME = "process_name";
	public static final String PROCESS_FILE = "process_file";
	public static final String PROCESS_PATH = "process_path";
	public static final String PROCESS_START = "process_start";
	public static final String TEMP_DIR = "tempdir";
	/** java.io.tmpdir without trailing File.separator */
	private static final String TEMP_DIR_PATH;

	/**
	 * Remaining problem is that predefined macros that are overridden by custom macros are
	 * evaluated first. The result is the predefined value.
	 */
	private static final String[] ALL_PREDEFINED_MACROS = { PROCESS_NAME, PROCESS_FILE, PROCESS_PATH, PROCESS_START, "a",
			"execution_count", "b", "c", "n", "operator_name", "t", "p[]", "v[]", TEMP_DIR };

	/** all predefined macros that do not depend on an operator except for v[] */
	private static final Set<String> PREDEFINED_OPERATOR_INDEPENDENT_MACROS = new HashSet<>(Arrays.asList(
			PROCESS_NAME, PROCESS_FILE, PROCESS_PATH, PROCESS_START, Operator.STRING_EXPANSION_MACRO_TIME, TEMP_DIR));

	/** all predefined macros that depend on an operator except for p[] */
	private static final Set<String> PREDEFINED_OPERATOR_DEPENDENT_MACROS = new HashSet<>(
			Arrays.asList(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY,
					Operator.STRING_EXPANSION_MACRO_OPERATORNAME_USER_FRIENDLY, Operator.STRING_EXPANSION_MACRO_OPERATORNAME,
					Operator.STRING_EXPANSION_MACRO_OPERATORCLASS, Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES,
					Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_PLUS_ONE));

	private static final String[] ALL_USER_FRIENDLY_PREDEFINED_MACROS = { PROCESS_NAME, PROCESS_FILE, PROCESS_PATH,
			PROCESS_START, Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY,
			Operator.STRING_EXPANSION_MACRO_OPERATORNAME_USER_FRIENDLY };

	private static final OperatorVersion THROW_ERROR_ON_UNDEFINED_MACRO = new OperatorVersion(6, 0, 3);

	/**
	 * This HashSet contains the keys of legacy macros which will be replaced while string
	 * expansion. CAUTION: Do NOT add any new content to this set.
	 */
	private static final HashSet<String> LEGACY_STRING_EXPANSION_MACRO_KEYS = new HashSet<>();

	static {
		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_OPERATORNAME);
		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_OPERATORCLASS);
		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES);
		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_PLUS_ONE);
		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_TIME);
		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_PERCENT_SIGN);

		LEGACY_STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_SHIFTED
				+ Operator.STRING_EXPANSION_MACRO_PARAMETER_START);
		LEGACY_STRING_EXPANSION_MACRO_KEYS
				.add(Operator.STRING_EXPANSION_MACRO_OPERATORVALUE + Operator.STRING_EXPANSION_MACRO_PARAMETER_START);
		String tmpDir = System.getProperty("java.io.tmpdir");
		try {
			// Remove trailing {@link java.io.File#separator}, resolve old 8.3 DOS filenames
			tmpDir = new File(tmpDir).getCanonicalPath();
		} catch (Exception e) {
			String internalTemp = FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_TEMP_FULL).toAbsolutePath().toString();
			LogService.log(LogService.getRoot(), Level.SEVERE, e, "com.rapidminer.MacroHandler.invalid_temp_dir", "" + tmpDir, internalTemp);
			tmpDir = internalTemp;
		}
		TEMP_DIR_PATH = tmpDir;
	}

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	/**
	 * Used for formatting the %{process_start} and current time %{t} macro
	 */
	public static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
		// clone because getDateInstance uses an internal pool which can return the same
		// instance for multiple threads
		return new SimpleDateFormat("yyyy_MM_dd-a_KK_mm_ss");
	});

	/**
	 * This HashSet contains the keys of macros which will be replaced while string expansion. Each
	 * macro item might have an arbitrary length.
	 */
	private static final HashSet<String> STRING_EXPANSION_MACRO_KEYS = new HashSet<>();

	static {
		STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_OPERATORNAME_USER_FRIENDLY);
		STRING_EXPANSION_MACRO_KEYS.add(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY);
	}

	private final Process process;

	private final Map<String, String> macroMap = new HashMap<>();

	private final Object LOCK = new Object();

	public MacroHandler(Process process) {
		this.process = process;
	}

	public void clear() {
		setChanged();
		synchronized (LOCK) {
			macroMap.clear();
		}
		notifyObservers(this);
	}

	public Iterator<String> getDefinedMacroNames() {
		synchronized (LOCK) {
			return new HashMap<>(macroMap).keySet().iterator();
		}
	}

	/**
	 * @return an array with the names of all user-friendly predefined macros available in
	 *         RapidMiner
	 */
	public String[] getAllGraphicallySupportedPredefinedMacros() {
		return ALL_USER_FRIENDLY_PREDEFINED_MACROS;
	}

	/**
	 * @return an array with the names of ALL predefined macros available in RapidMiner
	 */
	public String[] getAllPredefinedMacros() {
		return ALL_PREDEFINED_MACROS;
	}

	/**
	 * Adds a macro to this MacroHandler. If a macro with this name is already present, it will be
	 * overwritten.
	 *
	 * @param macro
	 *            The name of the macro.
	 * @param value
	 *            The new value of the macro.
	 */
	public void addMacro(String macro, String value) {
		if (macro != null && !macro.isEmpty()) {
			setChanged();
			synchronized (LOCK) {
				macroMap.put(macro, value);
			}
			notifyObservers(this);
		}
	}

	public void removeMacro(String macro) {
		setChanged();
		synchronized (LOCK) {
			macroMap.remove(macro);
		}
		notifyObservers(this);
	}

	/**
	 * Checks whether a provided macro was set.
	 *
	 * @param macro
	 *            the macro key
	 * @param operator
	 *            the operator that can be used to resolve the macro
	 * @return <code>true</code> in case it was set, <code>false</code> otherwise
	 */
	public boolean isMacroSet(String macro, Operator operator) {
		synchronized (LOCK) {
			if (macroMap.containsKey(macro) || PREDEFINED_OPERATOR_INDEPENDENT_MACROS.contains(macro)) {
				return true;
			}
		}
		return operator != null && PREDEFINED_OPERATOR_DEPENDENT_MACROS.contains(macro);

	}

	/**
	 * Resolves the macros "process_name", "process_file", "process_path", "t" and user defined
	 * macros.
	 */
	public String getMacro(String macro) {
		if (PREDEFINED_OPERATOR_INDEPENDENT_MACROS.contains(macro)) {
			switch (macro) {
				case PROCESS_NAME:
					ProcessLocation processLocation = process.getProcessLocation();
					if (processLocation == null) {
						return null;
					}
					if (processLocation instanceof FileProcessLocation) {
						return processLocation.getShortName().substring(0, processLocation.getShortName().lastIndexOf("."));
					}
					return processLocation.getShortName();
				case PROCESS_FILE:
					return process.getProcessLocation() != null ? process.getProcessLocation().getShortName() : null;
				case PROCESS_PATH:
					return process.getProcessLocation() != null ? process.getProcessLocation().toString() : null;
				case PROCESS_START:
					return macroMap.containsKey(macro) ? macroMap.get(macro)
							: DATE_FORMAT.get().format(new Date(process.getRootOperator().getStartTime()));
				case Operator.STRING_EXPANSION_MACRO_TIME:
					return DATE_FORMAT.get().format(new Date());
				case TEMP_DIR:
					return TEMP_DIR_PATH;
				default:
					return null;
			}
		}
		return this.macroMap.get(macro);
	}

	/**
	 * Resolves the macro.
	 *
	 * <p>
	 * Resolves following predefined macros:
	 * </p>
	 * <ul>
	 * <li><b>process_name</b> with the name of the process</li>
	 * <li><b>process_file</b> with the file name of the process</li>
	 * <li><b>process_path</b> with the path to the process</li>
	 * <li><b>t</b> with the current system date and time</li>
	 * </ul>
	 * <p>
	 * Resolves following predefined macros if operator is non-null:
	 * </p>
	 * <ul>
	 * <li><b>n</b> or <b>operator_name</b> with the name of this operator</li>
	 * <li><b>c</b> with the class of this operator</li>
	 * <li><b>a</b> or <b>execution_count</b> with the number of times the operator was applied</li>
	 * <li><b>b</b> with the number of times the operator was applied plus one</li>
	 * </ul>
	 * <p>
	 * Resolves user defined macros.
	 * </p>
	 *
	 * @param macro
	 *            the macro to resolve
	 * @param operator
	 *            the operator to use for resolving, may be {@code null}
	 * @return the macro value
	 */
	public String getMacro(String macro, Operator operator) {
		if (operator != null) {
			String value = resolveUnshiftedOperatorMacros(macro, operator);
			if (value != null) {
				return value;
			}
		}
		return getMacro(macro);
	}

	@Override
	public String toString() {
		return this.macroMap.toString();
	}

	/**
	 * This method replaces all Macros in a given String through their real values and returns a the
	 * String with replaced Macros. If the CompabililtyLevel of the RootOperator is lower than
	 * 6.0.3, undefined macros will be ignored.
	 *
	 * @param parameterValue
	 *            the whole ParameterType value String
	 * @return the complete parameter value with replaced Macros
	 * @throws UndefinedMacroError
	 *             this error will be thrown if the CompatibilityLevel of the RootOperator is at least
	 *             6.0.3 and a macro is undefined
	 */
	public String resolveMacros(String parameterKey, String parameterValue) throws UndefinedMacroError {
		int startIndex = parameterValue.indexOf(Operator.MACRO_STRING_START);
		if (startIndex == -1) {
			return parameterValue;
		}
		StringBuilder result = new StringBuilder();
		while (startIndex >= 0) {
			result.append(parameterValue, 0, startIndex);
			int endIndex = parameterValue.indexOf(Operator.MACRO_STRING_END, startIndex + 2);
			if (endIndex == -1) {
				return parameterValue;
			}
			String macroString = parameterValue.substring(startIndex + 2, endIndex);
			// check whether macroString is a predefined macro which will be resolved at String
			// expansion
			if (STRING_EXPANSION_MACRO_KEYS.contains(macroString) || LEGACY_STRING_EXPANSION_MACRO_KEYS
					.contains(macroString.length() > 1 ? macroString.substring(0, 2) : macroString)) {
				// skip macro because it will be replaced during the string expansion
				result.append(Operator.MACRO_STRING_START).append(macroString).append(Operator.MACRO_STRING_END);
			} else {
				// resolve macro
				String macroValue = this.getMacro(macroString);
				if (macroValue != null) {
					result.append(macroValue);
				} else {
					if (this.process.getRootOperator().getCompatibilityLevel().isAtLeast(THROW_ERROR_ON_UNDEFINED_MACRO)) {
						throw new UndefinedMacroError(parameterKey, macroString);
					} else {
						result.append(Operator.MACRO_STRING_START).append(macroString).append(Operator.MACRO_STRING_END);
					}
				}
			}
			parameterValue = parameterValue.substring(endIndex + 1);
			startIndex = parameterValue.indexOf(Operator.MACRO_STRING_START);
		}
		result.append(parameterValue);
		return result.toString();
	}

	/**
	 * <p>
	 * Replaces following predefined macros:
	 * </p>
	 * <ul>
	 * <li><b>%{n}</b> or <b>%{operator_name}</b> with the name of this operator</li>
	 * <li><b>%{c}</b> with the class of this operator</li>
	 * <li><b>%{t}</b> with the current system date and time
	 * <li><b>%{a}</b> or <b>%{execution_count}</b> with the number of times the operator was
	 * applied</li>
	 * <li><b>%{b}</b> with the number of times the operator was applied plus one (a shortcut for
	 * %{p[1]})</li>
	 * <li><b>%{p[number]}</b> with the number of times the operator was applied plus number</li>
	 * <li><b>%{v[OperatorName.ValueName]}</b> with the value &quot;ValueName&quot; of the operator
	 * &quot;OperatorName&quot;</li>
	 * <li><b>%{%}</b> with %</li>
	 * </ul>
	 *
	 * @return The String with resolved predefined macros. Returns {@code null} in case provided
	 *         parameter str is {@code null}.
	 */
	public String resolvePredefinedMacros(String str, Operator operator) throws UndefinedParameterError {
		if (str == null) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		int totalStart = 0;
		int start;
		while ((start = str.indexOf(Operator.MACRO_STRING_START, totalStart)) >= 0) {
			result.append(str, totalStart, start);
			int end = str.indexOf(Operator.MACRO_STRING_END, start);
			if (end == -1) {
				return str;
			}
			if (end >= start) {
				String command = str.substring(start + 2, end);
				String unshiftedOperatorMacroResult = resolveUnshiftedOperatorMacros(command, operator);
				if (unshiftedOperatorMacroResult != null) {
					result.append(unshiftedOperatorMacroResult);
				} else if (command.startsWith(Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_SHIFTED
						+ Operator.STRING_EXPANSION_MACRO_PARAMETER_START)) {
					int openNumberIndex = command.indexOf(Operator.STRING_EXPANSION_MACRO_PARAMETER_START);
					int closeNumberIndex = command.indexOf(Operator.STRING_EXPANSION_MACRO_PARAMETER_END, openNumberIndex);
					if (closeNumberIndex < 0 || closeNumberIndex <= openNumberIndex + 1) {
						throw new UndefinedMacroError(operator, "predefinedMacro_shiftedExecutionCounter_format", "");
					}
					String numberString = command.substring(openNumberIndex + 1, closeNumberIndex);
					int number;
					try {
						number = Integer.parseInt(numberString);
					} catch (NumberFormatException e) {
						throw new UndefinedMacroError(operator, "946", numberString);
					}
					result.append(operator.getApplyCount() + number);
				} else if (Operator.STRING_EXPANSION_MACRO_TIME.equals(command)) {
					result.append(DATE_FORMAT.get().format(new Date()));
				} else if (command.startsWith(
						Operator.STRING_EXPANSION_MACRO_OPERATORVALUE + Operator.STRING_EXPANSION_MACRO_PARAMETER_START)) {
					int openNumberIndex = command.indexOf(Operator.STRING_EXPANSION_MACRO_PARAMETER_START);
					int closeNumberIndex = command.indexOf(Operator.STRING_EXPANSION_MACRO_PARAMETER_END, openNumberIndex);
					if (closeNumberIndex < 0 || closeNumberIndex <= openNumberIndex + 1) {
						throw new UndefinedMacroError(operator, "predefinedMacro_OperatorValue_format", "");
					}
					String operatorValueString = command.substring(openNumberIndex + 1, closeNumberIndex);
					String[] operatorValuePair = operatorValueString.split("\\.");
					if (operatorValuePair.length != 2) {
						throw new UndefinedMacroError(operator, "predefinedMacro_OperatorValue_format", "");
					}
					Operator op = process.getOperator(operatorValuePair[0]);
					if (op == null) {
						throw new UndefinedMacroError(operator, "predefinedMacro_OperatorValue_wrongOperator",
								operatorValuePair[0]);
					}
					Value value = op.getValue(operatorValuePair[1]);
					if (value == null) {
						throw new UndefinedMacroError(operator, "predefinedMacro_OperatorValue_noValue",
								operatorValuePair[1]);
					} else {
						if (value.isNominal()) {
							Object valueObject = value.getValue();
							if (valueObject != null) {
								result.append(valueObject.toString());
							} else {
								throw new UndefinedMacroError(operator, "predefinedMacro_OperatorValue_noValue",
										operatorValuePair[1]);
							}
						} else {
							double doubleValue = (Double) value.getValue();
							if (!Double.isNaN(doubleValue)) {
								result.append(Tools.formatIntegerIfPossible(doubleValue));
							} else {
								operator.logError("Value '" + operatorValuePair[1] + "' of the operator '"
										+ operatorValuePair[0] + "' not found!");
							}
						}
					}
				} else if (Operator.STRING_EXPANSION_MACRO_PERCENT_SIGN.equals(command)) {
					result.append('%');
				} else {
					result.append(command);
				}
			} else {
				end = start + 2;
				result.append(Operator.MACRO_STRING_START);
			}
			totalStart = end + 1;
		}
		result.append(str.substring(totalStart));
		return result.toString();
	}

	/**
	 * <p>
	 * Resolves following predefined macros:
	 * </p>
	 * <ul>
	 * <li><b>n</b> or <b>operator_name</b> with the name of this operator</li>
	 * <li><b>c</b> with the class of this operator</li>
	 * <li><b>a</b> or <b>execution_count</b> with the number of times the operator was applied</li>
	 * <li><b>b</b> with the number of times the operator was applied plus one</li>
	 * </ul>
	 *
	 * Does not resolve p[].
	 */
	private String resolveUnshiftedOperatorMacros(String command, Operator operator) {
		if (Operator.STRING_EXPANSION_MACRO_OPERATORNAME.equals(command)
				|| Operator.STRING_EXPANSION_MACRO_OPERATORNAME_USER_FRIENDLY.equals(command)) {
			return operator.getName();
		} else if (Operator.STRING_EXPANSION_MACRO_OPERATORCLASS.equals(command)) {
			return operator.getClass().getName();
		} else if (Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES.equals(command)
				|| Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_USER_FRIENDLY.equals(command)) {
			return operator.getApplyCount() + "";
		} else if (Operator.STRING_EXPANSION_MACRO_NUMBER_APPLIED_TIMES_PLUS_ONE.equals(command)) {
			return operator.getApplyCount() + 1 + "";
		} else {
			return null;
		}
	}

}
