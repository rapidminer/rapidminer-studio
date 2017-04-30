/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

import com.rapidminer.io.Base64;
//
// import sun.misc.BASE64Decoder;
// import sun.misc.BASE64Encoder;


/**
 * This class can be used to encrypt and decrypt given strings based on a key generated by the user.
 * Please note that classes using this tool class should first ensure that a user key is available
 * by invoking the method isKeyAvailable().
 * 
 * @author Ingo Mierswa, Marco Boeck
 */
@SuppressWarnings("deprecation")
public class CipherTools {

	/** algorithm used to encrypt/decrypt */
	private static final String CIPHER_TYPE = "DESede";

	/** String used to flag new encoded passwords */
	private static final String FLAG_NEW_ENCODE = "_";

	/** the minimum length of string to encode to Base64 before 6.0.003 */
	private static final int FORMER_MINLENGTH_TO_ENCODE = 4;

	/**
	 * Returns whether an encryption {@link Key} is available or not.
	 * 
	 * @return <code>true</code> if a cipher {@link Key} is available; <code>false</code> otherwise
	 */
	public static boolean isKeyAvailable() {
		try {
			KeyGeneratorTool.getUserKey();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Encrypt the given {@link String} with the current {@link KeyGeneratorTool#getUserKey()} and
	 * encode the result in Base64.
	 * 
	 * @param text
	 * @return
	 * @throws CipherException
	 */
	public static String encrypt(String text) throws CipherException {
		Key key = null;
		try {
			key = KeyGeneratorTool.getUserKey();
		} catch (IOException e) {
			throw new CipherException("Cannot retrieve key, probably no one was created: " + e.getMessage());
		}
		return encrypt(text, key);
	}

	/**
	 * Encrypt the given {@link String} with the specified {@link Key} and encode the result in
	 * Base64.
	 * 
	 * @param text
	 * @param key
	 * @return
	 * @throws CipherException
	 */
	public static String encrypt(String text, Key key) throws CipherException {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
			cipher.init(Cipher.ENCRYPT_MODE, key);

			byte[] outputBytes = cipher.doFinal(text.getBytes());

			String base64;
			if (text.length() < FORMER_MINLENGTH_TO_ENCODE) {
				// use new way to encode strings with less than 4 characters which was impossible
				// before 6.0.003
				base64 = DatatypeConverter.printBase64Binary(outputBytes) + FLAG_NEW_ENCODE;
			} else {
				// use old way for full backwards compatibility
				base64 = Base64.encodeBytes(outputBytes);
			}
			return base64;
		} catch (NoSuchAlgorithmException e) {
			throw new CipherException("Failed to encrypt text: " + e.getMessage());
		} catch (NoSuchPaddingException e) {
			throw new CipherException("Failed to encrypt text: " + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new CipherException("Failed to encrypt text: " + e.getMessage());
		} catch (IllegalBlockSizeException e) {
			throw new CipherException("Failed to encrypt text: " + e.getMessage());
		} catch (BadPaddingException e) {
			throw new CipherException("Failed to encrypt text: " + e.getMessage());
		}
	}

	/**
	 * Decrypt the given Base64 encoded and encrypted {@link String} with the current
	 * {@link KeyGeneratorTool#getUserKey()}.
	 * 
	 * @param text
	 * @return
	 * @throws CipherException
	 */
	public static String decrypt(String text) throws CipherException {
		Key key = null;
		try {
			key = KeyGeneratorTool.getUserKey();
		} catch (IOException e) {
			throw new CipherException("Cannot retrieve key, probably no one was created: " + e.getMessage());
		}
		return decrypt(text, key);
	}

	/**
	 * Decrypt the given Base64 encoded and encrypted {@link String} with the specified {@link Key}.
	 * 
	 * @param text
	 * @param key
	 * @return
	 * @throws CipherException
	 */
	public static String decrypt(String text, Key key) throws CipherException {
		try {
			byte[] encrypted;
			if (text.endsWith(FLAG_NEW_ENCODE)) {
				// encrypted strings with less than 4 characters which have been encrypted with
				// version 6.0.003 onwards use are encoded via DataTypeConverter instead
				encrypted = DatatypeConverter.parseBase64Binary(text.subSequence(0, text.length() - 1).toString());
			} else {
				encrypted = Base64.decode(text);
			}

			Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
			cipher.init(Cipher.DECRYPT_MODE, key);

			byte[] outputBytes = cipher.doFinal(encrypted);
			String ret = new String(outputBytes);
			return ret;
		} catch (NoSuchAlgorithmException e) {
			throw new CipherException("Failed to decrypt text: " + e.getMessage());
		} catch (NoSuchPaddingException e) {
			throw new CipherException("Failed to decrypt text: " + e.getMessage());
		} catch (IOException e) {
			throw new CipherException("Failed to decrypt text: " + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new CipherException("Failed to decrypt text: " + e.getMessage());
		} catch (IllegalBlockSizeException e) {
			throw new CipherException("Failed to decrypt text: " + e.getMessage());
		} catch (BadPaddingException e) {
			throw new CipherException("Failed to decrypt text: " + e.getMessage());
		}
	}

}
