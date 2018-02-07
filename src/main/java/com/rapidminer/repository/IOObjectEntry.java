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
package com.rapidminer.repository;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.ProgressListener;


/**
 * @author Simon Fischer
 */
public interface IOObjectEntry extends DataEntry {

	public static final String TYPE_NAME = "data";

	public IOObject retrieveData(ProgressListener l) throws RepositoryException;

	public MetaData retrieveMetaData() throws RepositoryException;

	/**
	 * This method returns the class of the stored object or null, if it is not an object known to
	 * this RapidMiner Client.
	 */
	public Class<? extends IOObject> getObjectClass();

	/** Stores data in this entry. */
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException;

}
