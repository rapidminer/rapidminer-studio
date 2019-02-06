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
package com.rapidminer.gui.tools;

import java.util.Collection;
import java.util.HashSet;

import com.rapidminer.operator.IOPublishToAppOperator;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.io.CSVExampleSetWriter;
import com.rapidminer.operator.io.ExcelExampleSetWriter;
import com.rapidminer.operator.io.RepositoryStorer;
import com.rapidminer.operator.io.ResultWriter;
import com.rapidminer.operator.io.SpecialFormatExampleSetWriter;
import com.rapidminer.operator.nio.file.WriteFileOperator;


/**
 * This class can be used to register operators which should not trigger a no result connected
 * warning if they are the only operators in the root process.
 *
 * @author David Arnu
 * @since 6.5.0
 *
 */
public final class ResultWarningPreventionRegistry {

	private static final Collection<Class<? extends Operator>> REGISTERED_OPERATORS = new HashSet<>();

	static {
		// writer operators
		REGISTERED_OPERATORS.add(ExcelExampleSetWriter.class);
		REGISTERED_OPERATORS.add(CSVExampleSetWriter.class);
		REGISTERED_OPERATORS.add(SpecialFormatExampleSetWriter.class);
		REGISTERED_OPERATORS.add(WriteFileOperator.class);
		REGISTERED_OPERATORS.add(ResultWriter.class);

		// Store
		REGISTERED_OPERATORS.add(RepositoryStorer.class);

		// publish to app
		REGISTERED_OPERATORS.add(IOPublishToAppOperator.class);

	}

	/**
	 * Private constructor which throws if called.
	 */
	private ResultWarningPreventionRegistry() {
		throw new UnsupportedOperationException("Static registry");
	}

	/**
	 * Checks if the provided operator suppresses the "no result connected" warning error bubble
	 *
	 * @param operator
	 *            the operator in question
	 * @return {@code false} if the operator does not suppress the warning, {@code true} otherwise
	 */
	public static boolean isResultWarningSuppressed(Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}
		return REGISTERED_OPERATORS.contains(operator.getClass());
	}

	/**
	 * Adds an additional Operator class to the list of Operator classes which don't provoke a
	 * warning bubble as the last executed child operator of {@link ProcessRootOperator}.
	 *
	 * @param opClass
	 */
	public static void addOperatorClass(Class<? extends Operator> opClass) {
		if (opClass == null) {
			throw new IllegalArgumentException("opClass must not be null!");
		}
		REGISTERED_OPERATORS.add(opClass);
	}

}
