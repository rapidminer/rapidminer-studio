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
package com.rapidminer.repository.versioned;

/**
 * Flag interface signalling new repositories based on the new VersionedRepository project. This is the non-versioned variant.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface NewFilesystemRepository {

	/**
	 * Gets the encryption context key that is used by the repository for encrypting files in it. See {@link
	 * com.rapidminer.tools.encryption.EncryptionProvider}.
	 *
	 * @return the encryption context key or {@code null}, in which case no encryption will be used (and e.g. passwords
	 * would be stored as-is, i.e. unencrypted)
	 */
	String getEncryptionContext();
}
