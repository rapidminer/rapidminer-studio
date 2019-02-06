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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.logging.Level;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;


/**
 * This class can be used to generate a new key and store it in the user directory. Please note that
 * by default existing keys will be overwritten by objects of this class. That means that passwords
 * stored with &quot;old&quot; keys can no longer be decrypted.
 *
 * Note that the class provides methods to override the default key storage location. Furthermore,
 * it is possible to suppress the storage of keys completely. These methods are implemented thread
 * safe to allow for scenarios in which the encryption key has to be exchanged (in separate
 * threads).
 *
 * @author Ingo Mierswa, Michael Knopf, Nils Woehler
 */
public class KeyGeneratorTool {

	/** Length of the triple DES key. */
	private static final int KEY_LENGTH = 168;

	/** Use Java's build in triple DES implementation. */
	private static final String GENERATOR_TYPE = "DESede";

	/** The cipher key provider */
	private static CipherKeyProvider cipherKeyProvider = new FileCipherKeyProvider();

	/** The encryption key */
	private static ThreadLocal<Key> localKey = new ThreadLocal<Key>() {

		@Override
		public Key initialValue() {
			return null;
		}
	};

	/**
	 * Controls whether a newly created key should be save to disk or not. This allow for the
	 * encryption with key other than the user key by calling the {@link KeyGeneratorTool} from
	 * another thread.
	 */
	private static ThreadLocal<Boolean> saveKeyToDisk = new ThreadLocal<Boolean>() {

		@Override
		public Boolean initialValue() {
			return new Boolean(true);
		}
	};

	/**
	 * Generates a new random key.
	 *
	 * @return The new random key.
	 * @throws KeyGenerationException
	 */
	public static SecretKey createSecretKey() throws KeyGenerationException {
		return cipherKeyProvider.createKey(KEY_LENGTH, GENERATOR_TYPE);
	}

	/**
	 * Generates and stores a new random key. Key storage can be suppressed using
	 * {@link KeyGeneratorTool#setSaveKeyToDisk(boolean)}.
	 *
	 * @throws KeyGenerationException
	 */
	public static void createAndStoreKey() throws KeyGenerationException {

		if (!RapidMiner.getExecutionMode().canAccessFilesystem()) {
			LogService.getRoot().log(Level.CONFIG, "com.rapidminer.tools.cipher.KeyGeneratorTool.skip_key_generation",
					RapidMiner.getExecutionMode());
			return;
		}

		// actual generation
		SecretKey key = cipherKeyProvider.createKey(KEY_LENGTH, GENERATOR_TYPE);
		if (key != null) {
			localKey.set(key);
		}

		// save key to disk
		if (saveKeyToDisk.get()) {
			Exception writeException = null;
			try {
				cipherKeyProvider.storeKey(key);
			} catch (Exception e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.tools.cipher.KeyGeneratorTool.generating_key_error", e), e);
				writeException = e;
			}

			if (writeException != null) {
				throw new KeyGenerationException("Cannot store key: " + writeException.getMessage());
			}
		}
	}

	/**
	 * Stores the specified {@link Key} bytes[] in the given file.
	 *
	 * @param rawKey
	 * @param keyPath
	 * @throws KeyGenerationException
	 */
	public static void storeKey(byte[] rawKey, Path keyPath) throws IOException {
		Files.deleteIfExists(keyPath);
		try (FileOutputStream fos = new FileOutputStream(keyPath.toFile());
				ObjectOutputStream out = new ObjectOutputStream(fos)) {
			out.writeInt(rawKey.length);
			out.write(rawKey);
		}
	}

	/**
	 * Return the user encryption key stored on disk. The key is only loaded once per session.
	 *
	 * @return The user encryption key.
	 * @throws IOException
	 */
	public static Key getUserKey() throws IOException {
		if (localKey.get() == null) {
			try {
				localKey.set(cipherKeyProvider.loadKey());
			} catch (KeyLoadingException e) {
				throw new IOException(e.getLocalizedMessage());
			}
		}
		return localKey.get();
	}

	/**
	 * Sets the new user encryption key for the <strong>current thread</strong>.
	 * <p>
	 * <strong>THIS KEY IS NEVER STORED ON THE FILESYSTEM!</strong>
	 * </p>
	 *
	 * @param newKey
	 *            the new key
	 */
	public static void setUserKey(Key newKey) {
		localKey.set(newKey);
	}

	/**
	 * Converts a raw key into a {@link SecretKeySpec}.
	 *
	 * @param rawKey
	 *            The raw key.
	 * @return
	 */
	public static SecretKeySpec makeKey(final byte[] rawKey) {
		return new SecretKeySpec(rawKey, GENERATOR_TYPE);
	}

	/**
	 * Controls whether newly generated keys should be stored permanently.
	 *
	 * @param saveKey
	 *            whether the new key should be stored to disk
	 */
	public static void setSaveKeyToDisk(boolean saveKey) {
		saveKeyToDisk.set(saveKey);
	}

	/**
	 * Sets the {@link CipherKeyProvider} for the {@link KeyGeneratorTool} utility class
	 *
	 * @param keyProvider
	 *            the {@link CipherKeyProvider} to be used
	 */
	public static void setCipherKeyProvider(CipherKeyProvider keyProvider) {
		cipherKeyProvider = keyProvider;
	}
}
