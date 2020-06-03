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

import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionFailedException;
import com.rapidminer.tools.encryption.exceptions.EncryptionNotInitializedException;


/**
 * An encryption provider for safe, symmetrical encryption in RapidMiner. Create instances by using the builder {@link
 * EncryptionProviderBuilder}.
 * <p>
 * Encryption providers are based on <a href="https://github.com/google/tink/blob/master/docs/PRIMITIVES.md#authenticated-encryption-with-associated-data">Google
 * Tink AEAD</a>.
 * </p>
 * <p>
 * To convert the encrypted byte array into a String and back, use {@link #encodeToBase64(byte[])} and {@link
 * #decodeFromBase64(String)}.
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public final class EncryptionProviderSymmetric extends EncryptionProvider {

	/**
	 * special encryption provider that only copies the input/output byte/char array and does no encryption/decryption whatsoever
	 */
	static final EncryptionProviderSymmetric PROVIDER_WITHOUT_ENCRYPTION = new EncryptionProviderSymmetric(null);


	EncryptionProviderSymmetric(String context) {
		super(context, EncryptionType.SYMMETRIC);
	}

	/**
	 * Encrypt the given char array. This is used to encrypt strings, without actually creating an in-memory string at
	 * any point in time for security reasons. Note that your char array is expected to be encoded via {@link
	 * java.nio.charset.StandardCharsets#UTF_8}!
	 * <p>Note: Encrypting the same input multiple times will result in a different encrypted value each time! This is
	 * a
	 * security feature, not a bug! Decrypting all those different values will of course always produce data identical
	 * to the input.</p>
	 *
	 * @param toEncrypt the char array to encrypt. It is expected to be encoded via {@link
	 *                  java.nio.charset.StandardCharsets#UTF_8}!
	 * @return the encrypted bytes, never {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the given context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 * @see #decryptString(byte[])
	 * @see Tools#convertCharArrayToByteArray(char[])
	 */
	public byte[] encryptString(char[] toEncrypt) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return encryptString(toEncrypt, null);
	}

	/**
	 * Encrypt the given char array. This is used to encrypt strings, without actually creating an in-memory string at
	 * any point in time for security reasons. Note that your char array is expected to be encoded via {@link
	 * java.nio.charset.StandardCharsets#UTF_8}!
	 * <p>Note: Encrypting the same input multiple times will result in a different encrypted value each time! This is
	 * a security feature, not a bug! Decrypting all those different values will of course always produce data identical
	 * to the input.</p>
	 *
	 * @param toEncrypt      the char array to encrypt. It is expected to be encoded via {@link
	 *                       java.nio.charset.StandardCharsets#UTF_8}!
	 * @param associatedData the byte array of associated data. Can be {@code null}, in which case no associated data is
	 *                       used to encrypt.
	 * @return the encrypted bytes, never {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the given context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 * @see #decryptString(byte[])
	 * @see Tools#convertCharArrayToByteArray(char[])
	 */
	public byte[] encryptString(char[] toEncrypt, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return encrypt(Tools.convertCharArrayToByteArray(toEncrypt), associatedData);
	}

	/**
	 * Encrypt the byte array.
	 * <p>Note: Encrypting the same input multiple times will result in a different encrypted value each time! This is a
	 * security feature, not a bug! Decrypting all those different values will of course always produce data identical
	 * to the input.</p>
	 *
	 * @param toEncrypt the byte array to encrypt
	 * @return the encrypted bytes, never {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public byte[] encrypt(byte[] toEncrypt) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return encrypt(toEncrypt, null);
	}

	/**
	 * Encrypt the byte array. <p>Note: Encrypting the same input multiple times will result in a different encrypted
	 * value each time! This is a security feature, not a bug! Decrypting all those different values will of course
	 * always produce data identical to the input.</p>
	 *
	 * @param toEncrypt      the byte array to encrypt
	 * @param associatedData the byte array of associated data. Can be {@code null}, in which case no associated data is
	 *                       used to encrypt.
	 * @return the encrypted bytes, never {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public byte[] encrypt(byte[] toEncrypt, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		KeysetHandle keysetHandle = getKeysetHandle();

		// no encryption
		if (keysetHandle == null) {
			return Arrays.copyOf(toEncrypt, toEncrypt.length);
		}

		try {
			Aead aead = keysetHandle.getPrimitive(Aead.class);
			return aead.encrypt(toEncrypt, associatedData != null ? associatedData : new byte[0]);
		} catch (GeneralSecurityException e) {
			throw new EncryptionFailedException(e);
		}
	}

	/**
	 * Decrypt the given char array. This is used to decrypt strings, without actually creating an in-memory string at
	 * any point in time for security reasons. Note that your char array is expected to be encoded via {@link
	 * java.nio.charset.StandardCharsets#UTF_8}!
	 *
	 * @param encrypted the byte array which contains the encrypted data. It is expected that the decrypted result is a
	 *                  string encoded via {@link java.nio.charset.StandardCharsets#UTF_8}!
	 * @return the decrypted string as a char array, encoded via {@link java.nio.charset.StandardCharsets#UTF_8}, never
	 * {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the given context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 * @see #encryptString(char[])
	 * @see Tools#convertByteArrayToCharArray(byte[]) (char[])
	 */
	public char[] decryptString(byte[] encrypted) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return decryptString(encrypted, null);
	}

	/**
	 * Decrypt the given char array. This is used to decrypt strings, without actually creating an in-memory string at
	 * any point in time for security reasons. Note that your char array is expected to be encoded via {@link
	 * java.nio.charset.StandardCharsets#UTF_8}!
	 *
	 * @param encrypted      the byte array which contains the encrypted data. It is expected that the decrypted result
	 *                       is a string encoded via {@link java.nio.charset.StandardCharsets#UTF_8}!
	 * @param associatedData the byte array of associated data. Can be {@code null}, in which case no associated data is
	 *                       used to decrypt.
	 * @return the decrypted string as a char array, encoded via {@link java.nio.charset.StandardCharsets#UTF_8}, never
	 * {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the given context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 * @see #encryptString(char[])
	 * @see Tools#convertByteArrayToCharArray(byte[]) (char[])
	 */
	public char[] decryptString(byte[] encrypted, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return Tools.convertByteArrayToCharArray(decrypt(encrypted, associatedData));
	}

	/**
	 * Decrypt the encrypted byte array. If the encrypted data was a char array (see {@link #encryptString(char[])}, you
	 * can get the char array back by calling {@link Tools#convertByteArrayToCharArray(byte[])}.
	 *
	 * @param encrypted the byte array which contains the encrypted data
	 * @return the decrypted bytes, never {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public byte[] decrypt(byte[] encrypted) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return decrypt(encrypted, null);
	}

	/**
	 * Decrypt the encrypted byte array. If the encrypted data was a char array (see {@link #encryptString(char[])}, you
	 * can get the char array back by calling {@link Tools#convertByteArrayToCharArray(byte[])}.
	 *
	 * @param encrypted      the byte array which contains the encrypted data
	 * @param associatedData the byte array of associated data. Can be {@code null}, in which case no associated data is
	 *                       used to decrypt.
	 * @return the decrypted bytes, never {@code null}
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public byte[] decrypt(byte[] encrypted, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		KeysetHandle keysetHandle = getKeysetHandle();

		// no encryption
		if (keysetHandle == null) {
			return Arrays.copyOf(encrypted, encrypted.length);
		}

		try {
			Aead aead = keysetHandle.getPrimitive(Aead.class);
			return aead.decrypt(encrypted, associatedData != null ? associatedData : new byte[0]);
		} catch (GeneralSecurityException e) {
			throw new EncryptionFailedException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Overridden to make sure this method succeeds for this encryption provider, even if used by extensions.
	 * </p>
	 */
	@Override
	protected final KeysetHandle getKeysetHandle() throws EncryptionContextNotFound, EncryptionNotInitializedException {
		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<KeysetHandle>) super::getKeysetHandle);
		} catch (PrivilegedActionException e) {
			Exception cause = e.getException();
			if (cause instanceof EncryptionContextNotFound) {
				throw (EncryptionContextNotFound) cause;
			} else if (cause instanceof EncryptionNotInitializedException) {
				throw (EncryptionNotInitializedException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		}
	}
}
