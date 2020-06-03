/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.operator.ports;

import java.util.Collection;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 * Hidden interface to keep raw type of {@link Port} usable.
 * See <a href=https://stackoverflow.com/questions/17181780/compiler-error-on-java-generic-interface-with-a-list-method>StackOverflow</a>
 * for details on raw type type erasure.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
interface PortBase {
	/**
	 * Returns the meta data of the desired class or throws an UserError if available meta data
	 * cannot be cast to the desired class. If no meta data is present at all, <code>null</code> is
	 * returned.
	 *
	 */
	<T extends MetaData> T getMetaData(Class<T> desiredClass) throws IncompatibleMDClassException;

	/**
	 * This method returns the object of the desired class or throws an UserError if no object is
	 * present or cannot be casted to the desiredClass.
	 *
	 * @throws UserError
	 * 		if data is missing or of wrong class.
	 */
	<T extends IOObject> T getData(Class<T> desiredClass) throws UserError;

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the
	 * connected {@link OutputPort}
	 *
	 * @throws UserError
	 *             If data is not of the requested type.
	 */
	<T extends IOObject> T getDataOrNull(Class<T> desiredClass) throws UserError;

	/**
	 * This method returns the object of the desired class or {@link null} if no object is
	 * present or it cannot be cast or converted to the desiredClass. Never throws an exception.
	 *
	 * @return the data cast or converted to the desired class or {@code null}
	 * @since 9.4
	 */
	default <T extends IOObject> T getDataAsOrNull(Class<T> desiredClass){
		// default method for compatibility, overwritten by {@link AbstractPort}
		try {
			return getDataOrNull(desiredClass);
		} catch (UserError userError) {
			return null;
		}
	}

	/** Returns the set of errors added since the last clear errors. */
	Collection<MetaDataError> getErrors();

	/** Returns a sorted list of all quick fixes applicable for this port. */
	List<QuickFix> collectQuickFixes();

	/**
	 * Same as {@link #getData()}.
	 *
	 * @throws UserError
	 *             if data is missing.
	 * @deprecated use {@link #getData(Class)}
	 */
	@Deprecated
	<T extends IOObject> T getData() throws OperatorException;

	/**
	 * Returns the last object delivered to the connected {@link InputPort} or received from the
	 * connected {@link OutputPort}.
	 *
	 * @throws UserError
	 *             If data is not of the requested type.
	 * @deprecated call {@link #getDataOrNull(Class)}
	 */
	@Deprecated
	<T extends IOObject> T getDataOrNull() throws UserError;
}
