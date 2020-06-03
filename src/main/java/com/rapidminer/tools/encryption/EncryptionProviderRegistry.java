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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.rapidminer.tools.Tools;


/**
 * <p>
 * Internal API, do not use! Use {@link EncryptionProviderBuilder} for actually using encryption.
 * </p>
 * The registry for adding encryption providers for given contexts and {@link EncryptionType EncryptionTypes} for
 * encrypting/decrypting.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public enum EncryptionProviderRegistry {

	INSTANCE;


	/** this pattern finds characters that are neither alphanumeric, whitespaces, nor '_', '%', or '-' */
	static final Pattern INVALID_CONTEXT_PATTERN = Pattern.compile("[^a-zA-Z0-9% _-]");


	/**
	 * <p>
	 * Internal method, not public API. Do not call!
	 * </p>
	 * Registers the given keyset handle for the given encryption context. Also persists it on disk, so on next Studio
	 * start, it will be automatically available. The context is just a string key, but for each context key only a
	 * single keyset can be defined. Registering a second one will overwrite the first one!
	 * <p>
	 * Requires {@link com.rapidminer.security.PluginSandboxPolicy#RAPIDMINER_INTERNAL_PERMISSION internal
	 * permissions}!
	 * </p>
	 *
	 * @param keysetJSON     the keyset handle as JSON
	 * @param context        the context key, must only contain alphanumeric characters, whitespaces, {@code _}, {@code
	 *                       -}, or {@code %}
	 * @param encryptionType the encryption type the keyset handle is for
	 * @param persist        if {@code true} the keyset handle will be persisted on disk so on next Studio start it will
	 *                       be automatically available; if {@code false} the keyset handle will only be available in
	 *                       this Studio session
	 * @return {@code true} if this registration overwrote an existing keyset; {@code false} if there was no keyset
	 * defined for the given context key yet
	 * @throws GeneralSecurityException if registering the keyset handle goes wrong semantically
	 * @throws IOException              if registering the keyset handle goes wrong technically
	 * @see <a href=https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md>Google Tink</a>
	 * @see #unregisterKeysetForContext(String, EncryptionType)
	 */
	public boolean registerKeysetForContext(String keysetJSON, String context, EncryptionType encryptionType, boolean persist) throws GeneralSecurityException, IOException {
		Tools.requireInternalPermission();
		if (StringUtils.stripToNull(keysetJSON) == null) {
			throw new IllegalArgumentException("keysetJSON must not be null or empty!");
		}
		if (encryptionType == null) {
			throw new IllegalArgumentException("encryptionType must not be null!");
		}
		checkContext(context);

		return registerKeysetForContext(CleartextKeysetHandle.read(JsonKeysetReader.withString(keysetJSON)), context, encryptionType, persist);
	}

	/**
	 * Registers the given keyset handle for the given encryption context. Also persists it on disk if desired, so on
	 * next Studio start, it will be automatically available. The context is just a string key, but for each context key
	 * only a single keyset can be defined. Registering a second one will overwrite the first one!
	 * <p>
	 * Requires {@link com.rapidminer.security.PluginSandboxPolicy#RAPIDMINER_INTERNAL_PERMISSION internal
	 * permissions}!
	 * </p>
	 *
	 * @param keysetHandle   the keyset handle
	 * @param context        the context key, must only contain alphanumeric characters plus whitespaces, {@code _},
	 *                       {@code -}, or {@code %}
	 * @param encryptionType the {@link EncryptionType} the keyset handle is for
	 * @param persist        if {@code true} the keyset handle will be persisted on disk so on next Studio start it will
	 *                       be automatically available; if {@code false} the keyset handle will only be available in
	 *                       this Studio session
	 * @return {@code true} if this registration overwrote an existing keyset; {@code false} if there was no keyset
	 * defined for the given context key yet
	 * @throws IOException if registering the keyset handle goes wrong technically
	 * @see <a href=https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md>Google Tink</a>
	 * @see #unregisterKeysetForContext(String, EncryptionType)
	 */
	public boolean registerKeysetForContext(KeysetHandle keysetHandle, String context, EncryptionType encryptionType, boolean persist) throws IOException {
		Tools.requireInternalPermission();
		if (keysetHandle == null) {
			throw new IllegalArgumentException("keysetHandle must not be null!");
		}
		if (encryptionType == null) {
			throw new IllegalArgumentException("encryptionType must not be null!");
		}
		checkContext(context);

		// persist it on disk if desired
		if (persist) {
			EncryptionFileUtils.INSTANCE.storeOnDisk(keysetHandle, context, encryptionType);
		}

		return addKeysetHandle(keysetHandle, context, encryptionType);
	}

	/**
	 * Unregisters the keyset for the given encryption context. Also removes it from disk (if it was persisted), so on
	 * next Studio start, it will no longer be automatically available. If the context is not known, nothing happens.
	 * <p>
	 * Requires {@link com.rapidminer.security.PluginSandboxPolicy#RAPIDMINER_INTERNAL_PERMISSION internal
	 * permissions}!
	 * </p>
	 *
	 * @param context        the context key, must only contain alphanumeric characters, whitespaces, {@code _}, {@code
	 *                       -}, or {@code %}
	 * @param encryptionType the {@link EncryptionType} the keyset handle was for
	 * @return {@code true} if a context was removed; {@code false} if the context was not known
	 * @throws IOException if unregistering the keyset handle goes wrong technically
	 */
	public boolean unregisterKeysetForContext(String context, EncryptionType encryptionType) throws IOException {
		Tools.requireInternalPermission();
		if (encryptionType == null) {
			throw new IllegalArgumentException("encryptionType must not be null!");
		}
		checkContext(context);

		// remove from on disk
		EncryptionFileUtils.INSTANCE.deleteFromDisk(context, encryptionType);

		return removeKeysetHandle(context, encryptionType);
	}

	/**
	 * Add the keyset handle to the registry. Does not persist it on disk.
	 *
	 * @param keysetHandle   the handle
	 * @param context        the context
	 * @param encryptionType the {@link EncryptionType} the keyset handle is for
	 * @return {@code true} if this addition overwrote an existing keyset; {@code false} if there was no keyset
	 */
	boolean addKeysetHandle(KeysetHandle keysetHandle, String context, EncryptionType encryptionType) {
		return encryptionType.getKeysetContextMap().put(context, keysetHandle) != null;
	}

	/**
	 * Remove the keyset handle from the registry. Does not remove it from disk.
	 *
	 * @param context        the context
	 * @param encryptionType the {@link EncryptionType} the keyset handle was for
	 * @return {@code true} if a context was removed; {@code false} if the context was not known
	 */
	boolean removeKeysetHandle(String context, EncryptionType encryptionType) {
		return encryptionType.getKeysetContextMap().remove(context) != null;
	}

	/**
	 * Get the keyset handle for the given context.
	 *
	 * @param context        the context key, must only contain alphanumeric characters, whitespaces, {@code _}, {@code
	 *                       -}, or {@code %}
	 * @param encryptionType the {@link EncryptionType} the keyset handle is for
	 * @return the keyset handle or {@code null} if the given context has no registered handle
	 */
	KeysetHandle getKeysetHandle(String context, EncryptionType encryptionType) {
		if (encryptionType == null) {
			throw new IllegalArgumentException("encryptionType must not be null!");
		}
		checkContext(context);

		return encryptionType.getKeysetContextMap().get(context);
	}

	/**
	 * Checks the context to make sure it contains only alpha-numeric characters plus whitespaces, '_', '%', or '-'.
	 *
	 * @throws IllegalArgumentException if context is invalid
	 */
	private void checkContext(String context) {
		if (StringUtils.stripToNull(context) == null) {
			throw new IllegalArgumentException("context must not be null or empty!");
		}
		if (INVALID_CONTEXT_PATTERN.matcher(context).find()) {
			throw new IllegalArgumentException("context must only contain alphanumeric characters plus whitespaces, '_', '%', or '-'!, but was " + context);
		}
	}
}
