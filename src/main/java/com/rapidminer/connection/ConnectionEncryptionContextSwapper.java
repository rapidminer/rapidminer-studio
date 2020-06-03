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
package com.rapidminer.connection;


import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Helper class to swap an encryption context (see {@link com.rapidminer.tools.encryption.EncryptionProvider}) via
 * try-with-resources and then put it back at the end of the try regardless of errors. Can for example be used in
 * combination with {@link ThreadLocal}.
 * <p>
 * Usage: try (ConnectionEncryptionContextSwapper context = {@link ConnectionEncryptionContextSwapper#withEncryptionContext(String,
 * Supplier, Consumer)}) { ... }
 * <p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public final class ConnectionEncryptionContextSwapper implements AutoCloseable {

	private final String encryptionContextBefore;
	private final String encryptionContextNew;
	private Consumer<String> encryptionContextSetter;


	private ConnectionEncryptionContextSwapper(String encryptionContextBefore, String encryptionContextNew, Consumer<String> encryptionContextSetter) {
		this.encryptionContextBefore = encryptionContextBefore;
		this.encryptionContextNew = encryptionContextNew;
		this.encryptionContextSetter = encryptionContextSetter;

		encryptionContextSetter.accept(encryptionContextNew);
	}

	/**
	 * Get the current encryption context.
	 *
	 * @return the encryption context, can be {@code null}
	 */
	public String getEncryptionContext() {
		return this.encryptionContextNew;
	}

	@Override
	public void close() {
		encryptionContextSetter.accept(encryptionContextBefore);
		encryptionContextSetter = null;
	}

	/**
	 * Create a new ConnectionEncryptionContextSwapper instance.
	 *
	 * @param encryptionContext            the encryption context that should be set within the try-with-resources
	 *                                     block, can be {@code null}
	 * @param getPreviousEncryptionContext the supplier for the previous encryption context, must not be {@code null}
	 * @param encryptionContextSetter      the consumer which will be called to both set the encryption context within
	 *                                     the try-block, as well as to restore the previous encryption context, must
	 *                                     not be {@code null}
	 * @return the instance, never {@code null}
	 */
	public static ConnectionEncryptionContextSwapper withEncryptionContext(String encryptionContext, Supplier<String> getPreviousEncryptionContext, Consumer<String> encryptionContextSetter) {
		if (getPreviousEncryptionContext == null) {
			throw new IllegalArgumentException("getPreviousEncryptionContext must not be null!");
		}
		if (encryptionContextSetter == null) {
			throw new IllegalArgumentException("encryptionContextSetter must not be null!");
		}

		return new ConnectionEncryptionContextSwapper(getPreviousEncryptionContext.get(), encryptionContext, encryptionContextSetter);
	}
}
