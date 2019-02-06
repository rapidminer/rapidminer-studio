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
package com.rapidminer.tools.cipher;

import java.security.Key;

import javax.crypto.SecretKey;


/**
 * The {@link CipherKeyProvider} handles creation, loading and storing of cipher {@link Key}s for
 * the {@link KeyGeneratorTool}.
 *
 * @author Nils Woehler
 * @since 6.2.0
 *
 */
public interface CipherKeyProvider {

	/**
	 * Generates a new random {@link Key} for the specified key length and algorithm.
	 *
	 * @param keyLength
	 *            the length of the new generated key
	 * @param algorithm
	 *            the algorithm that should be used to generate the key
	 * @return The new random key.
	 * @throws KeyGenerationException
	 *             in case the key generation fails
	 */
	SecretKey createKey(int keyLength, String algorithm) throws KeyGenerationException;

	/**
	 * Stores a key to the {@link CipherKeyProvider}s storage location
	 *
	 * @param key
	 *            The key to store to the {@link CipherKeyProvider}s storage location
	 * @throws KeyStoringException
	 *             in case the storing does not work
	 */
	void storeKey(Key key) throws KeyStoringException;

	/**
	 * Loads the cipher key from the {@link CipherKeyProvider}s storage location.
	 *
	 * @return the loaded cipher key
	 * @throws KeyLoadingException
	 *             in case an error occurs when loading the cipher key
	 */
	Key loadKey() throws KeyLoadingException;

}
