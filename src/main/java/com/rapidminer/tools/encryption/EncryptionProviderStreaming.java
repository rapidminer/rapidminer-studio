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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionFailedException;
import com.rapidminer.tools.encryption.exceptions.EncryptionNotInitializedException;


/**
 * An encryption provider for safe, streaming encryption in RapidMiner. Create instances by using the builder {@link
 * EncryptionProviderBuilder}.
 * <p>
 * This streaming encryption provider is based on <a href="https://github.com/google/tink/blob/master/docs/PRIMITIVES.md#streaming-authenticated-encryption-with-associated-data">Google
 * Tink Streaming AEAD</a>.
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public final class EncryptionProviderStreaming extends EncryptionProvider {

	/**
	 * special streaming encryption provider that only forwards the input/output data and does no encryption/decryption whatsoever
	 */
	static final EncryptionProviderStreaming PROVIDER_WITHOUT_ENCRYPTION = new EncryptionProviderStreaming(null);


	EncryptionProviderStreaming(String context) {
		super(context, EncryptionType.STREAMING);
	}

	/**
	 * Provides an {@link OutputStream} to which data can be written, which is then automatically encrypted on the
	 * fly. Note that the writing has to happen in one go, you cannot close the stream and resume at a later point in
	 * time.
	 * <strong>Important:</strong> Take care to call {@link OutputStream#close()} once you are done!
	 *
	 * @param encryptionTarget the output stream to which the encrypted data will be written
	 * @return the output stream in which plaintext data can be written which will automatically be encrypted and written to
	 * the given target stream
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public OutputStream getEncryptionStream(OutputStream encryptionTarget) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return getEncryptionStream(encryptionTarget, null);
	}

	/**
	 * Provides an {@link OutputStream} to which data can be written, which is then automatically encrypted on the
	 * fly. Note that the writing has to happen in one go, you cannot close the stream and resume at a later point in
	 * time.
	 * <strong>Important:</strong> Take care to call {@link OutputStream#close()} once you are done!
	 *
	 * @param encryptionTarget the output stream to which the encrypted data will be written
	 * @param associatedData   the byte array of associated data. Can be {@code null}, in which case no associated data
	 *                         is used to encrypt.
	 * @return the output stream in which plaintext data can be written which will automatically be encrypted and written to
	 * the given target stream
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public OutputStream getEncryptionStream(OutputStream encryptionTarget, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		KeysetHandle keysetHandle = getKeysetHandle();

		// no encryption
		if (keysetHandle == null) {
			return encryptionTarget;
		}

		try {
			StreamingAead streamingAead = keysetHandle.getPrimitive(StreamingAead.class);
			return streamingAead.newEncryptingStream(encryptionTarget, associatedData != null ? associatedData : new byte[0]);
		} catch (GeneralSecurityException | IOException e) {
			throw new EncryptionFailedException(e);
		}
	}

	/**
	 * Provides a {@link WritableByteChannel} to which data can be written, which is then automatically encrypted on the
	 * fly. Note that the writing has to happen in one go, you cannot close the channel and resume at a later point in
	 * time.
	 * <strong>Important:</strong> Take care to call {@link WritableByteChannel#close()} once you are done!
	 *
	 * @param encryptionTarget the byte channel to which the encrypted data will be written
	 * @return the channel in which plaintext data can be written which will automatically be encrypted and written to
	 * the given target channel
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public WritableByteChannel getEncryptionChannel(WritableByteChannel encryptionTarget) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return getEncryptionChannel(encryptionTarget, null);
	}

	/**
	 * Provides a {@link WritableByteChannel} to which data can be written, which is then automatically encrypted on the
	 * fly. Note that the writing has to happen in one go, you cannot close the channel and resume at a later point in
	 * time.
	 * <strong>Important:</strong> Take care to call {@link WritableByteChannel#close()} once you are done!
	 *
	 * @param encryptionTarget the byte channel to which the encrypted data will be written
	 * @param associatedData   the byte array of associated data. Can be {@code null}, in which case no associated data
	 *                         is used to encrypt.
	 * @return the channel in which plaintext data can be written which will automatically be encrypted and written to
	 * the given target channel
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the encryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public WritableByteChannel getEncryptionChannel(WritableByteChannel encryptionTarget, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		KeysetHandle keysetHandle = getKeysetHandle();

		// no encryption
		if (keysetHandle == null) {
			return encryptionTarget;
		}

		try {
			StreamingAead streamingAead = keysetHandle.getPrimitive(StreamingAead.class);
			return streamingAead.newEncryptingChannel(encryptionTarget, associatedData != null ? associatedData : new byte[0]);
		} catch (GeneralSecurityException | IOException e) {
			throw new EncryptionFailedException(e);
		}
	}

	/**
	 * Provides an {@link InputStream} from which data can be read in plaintext which is decrypted on-the-fly from the
	 * specified encryptedStream.
	 * <p>Attention: If the data cannot be decrypted (or the associated data is wrong), reading from the returned input
	 * stream will result in an IOException!</p>
	 *
	 * @param encryptedStream the input stream from which the encrypted data can be read
	 * @return the stream from which the plaintext data can be read
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public InputStream getDecryptionStream(InputStream encryptedStream) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return getDecryptionStream(encryptedStream, null);
	}

	/**
	 * Provides an {@link InputStream} from which data can be read in plaintext which is decrypted on-the-fly from the
	 * specified encryptedStream.
	 * <p>Attention: If the data cannot be decrypted (or the associated data is wrong), reading from the returned input
	 * stream will result in an IOException!</p>
	 *
	 * @param encryptedStream the input stream from which the encrypted data can be read
	 * @param associatedData  the byte array of associated data. Can be {@code null}, in which case no associated data
	 *                        is used to decrypt.
	 * @return the stream from which the plaintext data can be read
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public InputStream getDecryptionStream(InputStream encryptedStream, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		KeysetHandle keysetHandle = getKeysetHandle();

		// no encryption
		if (keysetHandle == null) {
			return encryptedStream;
		}

		try {
			StreamingAead streamingAead = keysetHandle.getPrimitive(StreamingAead.class);
			return streamingAead.newDecryptingStream(encryptedStream, associatedData != null ? associatedData : new byte[0]);
		} catch (GeneralSecurityException | IOException e) {
			throw new EncryptionFailedException(e);
		}
	}

	/**
	 * Provides a {@link ReadableByteChannel} from which data can be read in plaintext which is decrypted on-the-fly
	 * from the specified encryptedChannel.
	 * <p>Attention: If the data cannot be decrypted (or the associated data is wrong), reading from the returned
	 * channel
	 * will result in an IOException!</p>
	 *
	 * @param encryptedChannel the byte channel from which the encrypted data can be read
	 * @return the channel from which the plaintext data can be read
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public ReadableByteChannel getDecryptionChannel(ReadableByteChannel encryptedChannel) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		return getDecryptionChannel(encryptedChannel, null);
	}

	/**
	 * Provides a {@link ReadableByteChannel} from which data can be read in plaintext which is decrypted on-the-fly
	 * from the specified encryptedChannel.
	 * <p>Attention: If the data cannot be decrypted (or the associated data is wrong), reading from the returned
	 * channel
	 * will result in an IOException!</p>
	 *
	 * @param encryptedChannel the byte channel from which the encrypted data can be read
	 * @param associatedData   the byte array of associated data. Can be {@code null}, in which case no associated data
	 *                         is used to decrypt.
	 * @return the channel from which the plaintext data can be read
	 * @throws EncryptionNotInitializedException if the encryption is not available due to an unexpected error
	 * @throws EncryptionContextNotFound         if the specified context has no encryption key registered to it
	 * @throws EncryptionFailedException         if the decryption fails. See {@link EncryptionFailedException#getCause()}
	 *                                           for details
	 */
	public ReadableByteChannel getDecryptionChannel(ReadableByteChannel encryptedChannel, byte[] associatedData) throws EncryptionContextNotFound, EncryptionFailedException, EncryptionNotInitializedException {
		KeysetHandle keysetHandle = getKeysetHandle();

		// no encryption
		if (keysetHandle == null) {
			return encryptedChannel;
		}

		try {
			StreamingAead streamingAead = keysetHandle.getPrimitive(StreamingAead.class);
			return streamingAead.newDecryptingChannel(encryptedChannel, associatedData != null ? associatedData : new byte[0]);
		} catch (GeneralSecurityException | IOException e) {
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
	protected KeysetHandle getKeysetHandle() throws EncryptionContextNotFound, EncryptionNotInitializedException {
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
