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
package com.rapidminer.repository;

/**
 * An entry that can store processes.
 * 
 * @author Simon Fischer
 * 
 */
public interface ProcessEntry extends DataEntry {

	String TYPE_NAME = "process";
	String RMP_SUFFIX = ".rmp";

	@Override
	default String getType() {
		return TYPE_NAME;
	}

	/**
	 * Retrieves the XML of the process stored here.
	 * <strong>Important:</strong> The XML may contain secret values, which probably have been encrypted with an
	 * encryption context! See
	 * {@link com.rapidminer.tools.encryption.EncryptionProvider} and {@link Repository#getEncryptionContext()} for
	 * reference. Loading it with a wrong encryption context will cause the decryption of those secret values to fail!
	 *
	 * @throws RepositoryException if loading goes wrong
	 */
	String retrieveXML() throws RepositoryException;

	/**
	 * Stores the XML of the process here.
	 * <strong>Important:</strong> Make sure that the XML has secret values encrypted with the proper encryption context! See
	 * {@link com.rapidminer.tools.encryption.EncryptionProvider} and {@link Repository#getEncryptionContext()} for
	 * reference. Otherwise, loading it again later will fail!
	 *
	 * @param xml the XML generated with the correct encryption context for this repository
	 * @throws RepositoryException if storing goes wrong
	 */
	void storeXML(String xml) throws RepositoryException;

}
