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
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionNotInitializedException;


/**
 * Base abstract encryption provider. Create specific instances by using the builder {@link
 * EncryptionProviderBuilder}.
 * <p>
 * Encryption providers are based on <a href=https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md>Google
 * Tink</a>.
 * </p>
 * <p>
 * To convert the encrypted byte array into a String and back, use {@link #encodeToBase64(byte[])} and {@link
 * #decodeFromBase64(String)}.
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public abstract class EncryptionProvider {

	/**
	 * the default encryption context which can be used to symmetrically encrypt data which is only used locally on the
	 * client machine. Will use AES-GCM 256.
	 */
	public static final String DEFAULT_CONTEXT = "default-local-context";
	private static AtomicBoolean initialized = new AtomicBoolean(false);

	protected final String context;
	protected final EncryptionType encryptionType;


	protected EncryptionProvider(String context, EncryptionType encryptionType) {
		this.context = context;
		this.encryptionType = encryptionType;
	}

	/**
	 * Encodes the given byte array as a Base64 string.
	 *
	 * @param bytes the input bytes, must not be {@code null}
	 * @return the Base64 encoded string, never {@code null}
	 */
	public String encodeToBase64(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Decodes the given Base64 string to a byte array.
	 *
	 * @param base64 the Base64 encoded string, must not be {@code null}
	 * @return the byte array, never {@code null}
	 */
	public byte[] decodeFromBase64(String base64) {
		return Base64.getDecoder().decode(base64);
	}

	/**
	 * Gets the keyset handle or throws. Requires internal permissions!
	 *
	 * @return the handle, or {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the given context does not have a keyset handle registered
	 * @throws EncryptionNotInitializedException if the encryption was not successfully initialized
	 */
	protected KeysetHandle getKeysetHandle() throws EncryptionContextNotFound, EncryptionNotInitializedException {
		if (!initialized.get()) {
			throw new EncryptionNotInitializedException();
		}
		if (context == null) {
			return null;
		}
		Tools.requireInternalPermission();

		KeysetHandle keysetHandle = EncryptionProviderRegistry.INSTANCE.getKeysetHandle(context, encryptionType);
		if (keysetHandle == null) {
			throw new EncryptionContextNotFound(context);
		}

		return keysetHandle;
	}

	/**
	 * Initialize the encryption providers. Calling multiple times has no effect.
	 */
	public static synchronized void initialize() {
		if (initialized.get()) {
			return;
		}

		try {
			AeadConfig.register();
			StreamingAeadConfig.register();

			// load stored keysets
			for (EncryptionFileUtils.KeysetHandleContainer container : EncryptionFileUtils.INSTANCE.loadAllKeysetHandlesFromDisk()) {
				EncryptionProviderRegistry.INSTANCE.addKeysetHandle(container.getKeysetHandle(), container.getContext(), container.getEncryptionType());
			}


			// init default keyset handles if necessary
			KeysetHandle defaultSymmetricKeysetHandle = EncryptionProviderRegistry.INSTANCE.getKeysetHandle(DEFAULT_CONTEXT, EncryptionType.SYMMETRIC);
			if (defaultSymmetricKeysetHandle == null) {
				EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM), DEFAULT_CONTEXT, EncryptionType.SYMMETRIC, true);
			}
			KeysetHandle defaultStreamingKeysetHandle = EncryptionProviderRegistry.INSTANCE.getKeysetHandle(DEFAULT_CONTEXT, EncryptionType.STREAMING);
			if (defaultStreamingKeysetHandle == null) {
				EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(KeysetHandle.generateNew(StreamingAeadKeyTemplates.AES256_GCM_HKDF_4KB), DEFAULT_CONTEXT, EncryptionType.STREAMING, true);
			}
			initialized.set(true);
		} catch (GeneralSecurityException | IOException e) {
			LogService.getRoot().log(Level.SEVERE, "Failed to initialize encryption providers!", e);
		}
	}

}
