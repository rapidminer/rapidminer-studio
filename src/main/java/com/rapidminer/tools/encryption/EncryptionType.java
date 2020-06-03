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
package com.rapidminer.tools.encryption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.crypto.tink.KeysetHandle;


/**
 * The encryption type, e.g. simple symmetric, streaming, etc.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public enum EncryptionType {

	/**
	 * Symmetric encryption via AEAD, see <a href="https://github.com/google/tink/blob/master/docs/PRIMITIVES.md#authenticated-encryption-with-associated-data">Google
	 * Tink AEAD</a>
	 */
	SYMMETRIC,

	/**
	 * Symmetric encryption via AEAD, see <a href="https://github.com/google/tink/blob/master/docs/PRIMITIVES.md#streaming-authenticated-encryption-with-associated-data">Google
	 * Tink Streaming AEAD</a>
	 */
	STREAMING;


	private final Map<String, KeysetHandle> keysetContextMap = new ConcurrentHashMap<>();

	/**
	 * @return the concurrent hash map mapping string context to a keyset handle, never {@code null}
	 */
	Map<String, KeysetHandle> getKeysetContextMap() {
		return keysetContextMap;
	}
}
