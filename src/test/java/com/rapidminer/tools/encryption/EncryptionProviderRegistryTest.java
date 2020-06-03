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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionException;


/**
 * Tests for the {@link EncryptionProviderRegistry}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class EncryptionProviderRegistryTest {

	private static final String INPUT = "this is going to be encrypted!";


	@BeforeClass
	public static void init() {
		EncryptionProvider.initialize();
	}

	@Test
	public void testContextRegisteringSymmetric() throws GeneralSecurityException, IOException {
		EncryptionProviderSymmetric customProvider = new EncryptionProviderBuilder().withContext("custom").buildSymmetricProvider();
		try {
			customProvider.encryptString("This should fail".toCharArray());
			Assert.fail("The context does not exist, but encryption did not fail");
		} catch (EncryptionContextNotFound e) {
			// all good
		} catch (EncryptionException e) {
			Assert.fail("Context unknown, but failed with different exception " + e.getMessage());
			e.printStackTrace();
		}

		EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(createKeysetHandle(), "custom", EncryptionType.SYMMETRIC, false);

		byte[] encryptedBytes = customProvider.encryptString(INPUT.toCharArray());
		Assert.assertEquals("Decrypted string was not identical to original", new String(customProvider.decryptString(encryptedBytes)), INPUT);

		EncryptionProviderRegistry.INSTANCE.removeKeysetHandle("custom", EncryptionType.SYMMETRIC);

		try {
			customProvider.encryptString("This should fail".toCharArray());
			Assert.fail("The context was unregistered, but encryption did not fail");
		} catch (EncryptionContextNotFound e) {
			// all good
		} catch (EncryptionException e) {
			Assert.fail("Context unknown, but failed with different exception " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testInvalidContext() throws GeneralSecurityException, IOException {
		try {
			EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(createKeysetHandle(), "   ", EncryptionType.SYMMETRIC, false);
			Assert.fail("Registered context of only whitespaces, should have been forbidden!");
		} catch (IllegalArgumentException e) {
			// expected, all good
		}

		try {
			EncryptionProviderRegistry.INSTANCE.registerKeysetForContext(createKeysetHandle(), null, EncryptionType.SYMMETRIC, false);
			Assert.fail("Registered null context, should have been forbidden!");
		} catch (IllegalArgumentException e) {
			// expected, all good
		}
	}

	private KeysetHandle createKeysetHandle() throws GeneralSecurityException {
		return KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM);
	}

}
