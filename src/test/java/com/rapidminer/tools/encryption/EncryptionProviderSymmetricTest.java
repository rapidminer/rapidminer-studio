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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.tools.Tools;
import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionException;
import com.rapidminer.tools.encryption.exceptions.EncryptionFailedException;


/**
 * Tests for the {@link EncryptionProviderSymmetric}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class EncryptionProviderSymmetricTest {

	private static final String INPUT = "this is going to be encrypted!";
	private static final String ASSOCIATED_DATA = "RapidMiner";


	@BeforeClass
	public static void init() {
		EncryptionProvider.initialize();
	}

	@Test
	public void testWithoutContext() throws EncryptionException {
		EncryptionProviderSymmetric provider = new EncryptionProviderBuilder().buildSymmetricProvider();
		byte[] encryptedBytes = provider.encryptString(INPUT.toCharArray());
		Assert.assertEquals("Decrypted string was not identical to original", new String(provider.decryptString(encryptedBytes)), INPUT);
	}

	@Test
	public void testDefaultContext() throws EncryptionException {
		EncryptionProviderSymmetric providerImplicit = new EncryptionProviderBuilder().buildSymmetricProvider();
		EncryptionProviderSymmetric providerExplicit = new EncryptionProviderBuilder().withContext(EncryptionProvider.DEFAULT_CONTEXT).buildSymmetricProvider();
		byte[] encryptedBytesImplicit = providerImplicit.encryptString(INPUT.toCharArray());
		byte[] encryptedBytesExplicit = providerExplicit.encryptString(INPUT.toCharArray());
		Assert.assertEquals("Decrypted string was not identical to original", new String(providerImplicit.decryptString(encryptedBytesImplicit)), new String(providerExplicit.decryptString(encryptedBytesExplicit)));
	}

	@Test
	public void testDifferentEncryptionForSameInput() throws EncryptionException {
		EncryptionProviderSymmetric provider1 = new EncryptionProviderBuilder().buildSymmetricProvider();
		EncryptionProviderSymmetric provider2 = new EncryptionProviderBuilder().buildSymmetricProvider();
		byte[] encryptedBytes1 = provider1.encryptString(INPUT.toCharArray());
		byte[] encryptedBytes2 = provider2.encryptString(INPUT.toCharArray());
		Assert.assertEquals("Decrypted string by provider1 was not identical to original", new String(provider1.decryptString(encryptedBytes1)), INPUT);
		Assert.assertEquals("Decrypted string by provider2 was not identical to original", new String(provider2.decryptString(encryptedBytes2)), INPUT);
		Assert.assertFalse("Encrypted data by different provider instances was identical but should not have been", Arrays.equals(encryptedBytes1, encryptedBytes2));
	}

	@Test
	public void testNullContext() throws EncryptionException {
		EncryptionProviderSymmetric nullProvider = new EncryptionProviderBuilder().withContext(null).buildSymmetricProvider();
		byte[] inputBytes = INPUT.getBytes(StandardCharsets.UTF_8);
		byte[] outputBytes = nullProvider.encryptString(Tools.convertByteArrayToCharArray(inputBytes));
		Assert.assertArrayEquals("Input and output bytes are different despite null encryption context", inputBytes, outputBytes);
	}

	@Test
	public void testEmptyContextInBuilder() {
		try {
			new EncryptionProviderBuilder().withContext("  ").buildSymmetricProvider();
			Assert.fail("Context was set to whitespace only, should have been forbidden but was fine");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testUnknownContext() {
		EncryptionProviderSymmetric unknownProvider = new EncryptionProviderBuilder().withContext("---unknown---").buildSymmetricProvider();
		try {
			unknownProvider.encryptString("This should fail".toCharArray());
			Assert.fail("The context does not exist, but encryption did not fail");
		} catch (EncryptionContextNotFound e) {
			// all good
		} catch (EncryptionException e) {
			Assert.fail("Context unknown, but failed with different exception " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testWithAssociatedData() throws EncryptionException {
		EncryptionProviderSymmetric provider = new EncryptionProviderBuilder().buildSymmetricProvider();
		byte[] encryptedBytes = provider.encryptString(INPUT.toCharArray(), ASSOCIATED_DATA.getBytes(StandardCharsets.UTF_8));
		Assert.assertEquals("Decrypted string was not identical to original", new String(provider.decryptString(encryptedBytes, ASSOCIATED_DATA.getBytes(StandardCharsets.UTF_8))), INPUT);
		try {
			Assert.assertNotEquals("Decrypted string identical to original despite not having associated data", new String(provider.decryptString(encryptedBytes, null)), INPUT);
			Assert.fail("Decryption should have failed!");
		} catch (EncryptionFailedException e) {
			// expected
		}
	}
}
