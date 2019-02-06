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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;


/**
 * The default {@link CipherKeyProvider} for RapidMiner Studio. It reads the Cipher key from a file
 * called "cipher.key" which is stored within the RapidMiner user folder.
 *
 * @author Nils Woehler
 * @since 6.2.0
 */
public class FileCipherKeyProvider implements CipherKeyProvider {

	/** Default file name used to store the cipher. */
	private static final String DEFAULTKEY_FILE_NAME = "cipher.key";

	@Override
	public SecretKey loadKey() throws KeyLoadingException {
		// try to load key from file
		File keyFile = new File(FileSystemService.getUserRapidMinerDir(), DEFAULTKEY_FILE_NAME);
		try (FileInputStream fis = new FileInputStream(keyFile); ObjectInputStream in = new ObjectInputStream(fis)) {
			int length = in.readInt();
			byte[] rawKey = new byte[length];
			int actualLength = in.read(rawKey);
			if (length != actualLength) {
				throw new IOException("Cannot read key file (unexpected length)");
			}
			return KeyGeneratorTool.makeKey(rawKey);
		} catch (IOException e) {
			// catch to log the problem, then throw again to indicate error
			Level logLevel = RapidMiner.getExecutionMode().canAccessFilesystem() ? Level.WARNING : Level.CONFIG;
			LogService.getRoot().log(logLevel, "com.rapidminer.tools.cipher.KeyGeneratorTool.read_key_error",
					e.getMessage());
			throw new KeyLoadingException("Cannot retrieve key: " + e.getMessage());
		}
	}

	@Override
	public SecretKey createKey(int keyLength, String algorithm) throws KeyGenerationException {
		KeyGenerator keyGenerator = null;
		try {
			keyGenerator = KeyGenerator.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new KeyGenerationException("Cannot generate key, generation algorithm not known.");
		}

		keyGenerator.init(keyLength, new SecureRandom());

		// actual key generation
		return keyGenerator.generateKey();
	}

	@Override
	public void storeKey(Key key) throws KeyStoringException {
		Path keyPath = FileSystemService.getUserRapidMinerDir().toPath().resolve(DEFAULTKEY_FILE_NAME);
		try {
			KeyGeneratorTool.storeKey(key.getEncoded(), keyPath);
		} catch (IOException e) {
			throw new KeyStoringException("Could not store new cipher key", e);
		}
	}

}
