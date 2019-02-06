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
package com.rapidminer.tools.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.FunctionInput.Category;


/**
 * A {@link Resolver} for macros using a {@link MacroHandler}.
 *
 * @author Gisa Schaefer
 * @since 6.5.0
 */
public class MacroResolver implements Resolver {

	private final MacroHandler macroHandler;
	private final Operator operator;

	private static final String KEY_MACROS = I18N.getGUIMessage("gui.dialog.function_input.macros");

	/**
	 * Creates a {@link MacroResolver} that uses the given macroHandler.
	 *
	 * @param macroHandler
	 *            the macro handler to resolve macros
	 */
	public MacroResolver(MacroHandler macroHandler) {
		this(macroHandler, null);
	}

	/**
	 * * Creates a {@link MacroResolver} that uses the given macroHandler and operator.
	 * 
	 * @param macroHandler
	 *            the macro handler to resolve macros
	 * @param operator
	 *            the operator used for resolving operator dependent predefined macros
	 */
	public MacroResolver(MacroHandler macroHandler, Operator operator) {
		this.macroHandler = macroHandler;
		this.operator = operator;
	}

	@Override
	public Collection<FunctionInput> getAllVariables() {
		List<FunctionInput> functionInputs = new ArrayList<>();

		String[] predefinedMacros = macroHandler.getAllGraphicallySupportedPredefinedMacros();
		for (String macro : predefinedMacros) {
			functionInputs.add(new FunctionInput(Category.SCOPE, KEY_MACROS, macro, Ontology.NOMINAL, null, false));
		}

		Iterator<String> macros = macroHandler.getDefinedMacroNames();
		while (macros.hasNext()) {
			functionInputs.add(new FunctionInput(Category.SCOPE, KEY_MACROS, macros.next(), Ontology.NOMINAL, null, true));
		}

		return functionInputs;
	}

	@Override
	public ExpressionType getVariableType(String variableName) {
		if (!macroHandler.isMacroSet(variableName, operator)) {
			return null;
		}
		return ExpressionType.STRING;
	}

	@Override
	public String getStringValue(String variableName) {
		return macroHandler.getMacro(variableName, operator);
	}

	@Override
	public double getDoubleValue(String variableName) {
		throw new IllegalStateException("Macros are always Strings!");
	}

	@Override
	public boolean getBooleanValue(String variableName) {
		throw new IllegalStateException("Macros are always Strings!");
	}

	@Override
	public Date getDateValue(String variableName) {
		throw new IllegalStateException("Macros are always Strings!");
	}

}
