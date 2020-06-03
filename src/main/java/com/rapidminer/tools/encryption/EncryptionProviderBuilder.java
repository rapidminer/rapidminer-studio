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

import org.apache.commons.lang.StringUtils;


/**
 * A builder for creating {@link EncryptionProvider}s. By default, will use the {@link
 * EncryptionProvider#DEFAULT_CONTEXT}. See {@link #withContext(String)} on how to set a context or even create an
 * encryption provider that does NOT encrypt anything at all.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class EncryptionProviderBuilder {

	private String context = EncryptionProvider.DEFAULT_CONTEXT;


	/**
	 * Set the context key which is used to determine which encryption key is used.
	 * <p>
	 * Optional, will use the local default context if not specified.
	 * </p>
	 *
	 * @param context the context, must only contain alphanumeric characters plus either whitespaces, {@code _}, {@code
	 *                -}, or {@code %}. If {@code null}, will return a special encryption provider which does NOT
	 *                encrypt at all!
	 * @return this builder
	 */
	public EncryptionProviderBuilder withContext(String context) {
		if (context != null) {
			if (StringUtils.stripToNull(context) == null) {
				throw new IllegalArgumentException("context must not be empty!");
			}
			if (EncryptionProviderRegistry.INVALID_CONTEXT_PATTERN.matcher(context).find()) {
				throw new IllegalArgumentException("context must only contain alphanumeric characters plus whitespaces, '_', '%', or '-'!, but was " + context);
			}
		}

		this.context = context;
		return this;
	}

	/**
	 * Creates the {@link EncryptionProviderSymmetric} instance with the given settings.
	 *
	 * @return the symmetric encryption provider for encrypting/decrypting byte arrays or strings, never {@code null}
	 */
	public EncryptionProviderSymmetric buildSymmetricProvider() {
		if (context == null) {
			return EncryptionProviderSymmetric.PROVIDER_WITHOUT_ENCRYPTION;
		}

		return new EncryptionProviderSymmetric(context);
	}

	/**
	 * Creates the {@link EncryptionProviderStreaming} instance with the given settings.
	 *
	 * @return the streaming encryption provider for encrypting/decrypting streams, never {@code null}
	 */
	public EncryptionProviderStreaming buildStreamingProvider() {
		if (context == null) {
			return EncryptionProviderStreaming.PROVIDER_WITHOUT_ENCRYPTION;
		}

		return new EncryptionProviderStreaming(context);
	}
}
