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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.tools.encryption.exceptions.EncryptionContextNotFound;
import com.rapidminer.tools.encryption.exceptions.EncryptionException;
import com.rapidminer.tools.encryption.exceptions.EncryptionFailedException;


/**
 * Tests for the {@link EncryptionProviderStreaming}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class EncryptionProviderStreamingTest {

	private static final String INPUT = "this is going to be encrypted!";
	private static final String ASSOCIATED_DATA = "RapidMiner";


	@BeforeClass
	public static void init() {
		EncryptionProvider.initialize();
	}

	@Test
	public void testWithoutContext() throws EncryptionException, IOException {
		EncryptionProviderStreaming provider = new EncryptionProviderBuilder().buildStreamingProvider();
		String decrypted = createStringFromEncryptedBytesViaStream(createEncryptedBytesViaStream(INPUT, null, provider), null, provider);
		Assert.assertEquals("Decrypted string was not identical to original", decrypted, INPUT);
	}

	@Test
	public void testDefaultContext() throws EncryptionException, IOException {
		EncryptionProviderStreaming providerImplicit = new EncryptionProviderBuilder().buildStreamingProvider();
		EncryptionProviderStreaming providerExplicit = new EncryptionProviderBuilder().withContext(EncryptionProvider.DEFAULT_CONTEXT).buildStreamingProvider();
		String decryptedImplicit = createStringFromEncryptedBytesViaStream(createEncryptedBytesViaStream(INPUT, null, providerImplicit), null, providerImplicit);
		String decryptedExplicit = createStringFromEncryptedBytesViaStream(createEncryptedBytesViaStream(INPUT, null, providerExplicit), null, providerExplicit);
		Assert.assertEquals("Decrypted string was not identical to original", decryptedImplicit, decryptedExplicit);
	}

	@Test
	public void testDifferentEncryptionForSameInput() throws EncryptionException, IOException {
		EncryptionProviderStreaming provider1 = new EncryptionProviderBuilder().buildStreamingProvider();
		EncryptionProviderStreaming provider2 = new EncryptionProviderBuilder().buildStreamingProvider();
		byte[] encryptedBytes1 = createEncryptedBytesViaStream(INPUT, null, provider1);
		byte[] encryptedBytes2 = createEncryptedBytesViaStream(INPUT, null, provider2);
		Assert.assertEquals("Decrypted string by provider1 was not identical to original", createStringFromEncryptedBytesViaStream(encryptedBytes1, null, provider1), INPUT);
		Assert.assertEquals("Decrypted string by provider2 was not identical to original", createStringFromEncryptedBytesViaStream(encryptedBytes2, null, provider2), INPUT);
		Assert.assertFalse("Encrypted data by different provider instances was identical but should not have been", Arrays.equals(encryptedBytes1, encryptedBytes2));
	}

	@Test
	public void testNullContext() throws EncryptionException, IOException {
		EncryptionProviderStreaming nullProvider = new EncryptionProviderBuilder().withContext(null).buildStreamingProvider();
		byte[] inputBytes = INPUT.getBytes(StandardCharsets.UTF_8);
		byte[] outputBytes = createEncryptedBytesViaStream(INPUT, null, nullProvider);
		Assert.assertArrayEquals("Input and output bytes are different despite null encryption context", inputBytes, outputBytes);
	}

	@Test
	public void testUnknownContext() {
		EncryptionProviderStreaming unknownProvider = new EncryptionProviderBuilder().withContext("---unknown---").buildStreamingProvider();
		try {
			createEncryptedBytesViaStream("This should fail", null, unknownProvider);
			Assert.fail("The context does not exist, but encryption did not fail");
		} catch (EncryptionContextNotFound e) {
			// all good
		} catch (Exception e) {
			Assert.fail("Context unknown, but failed with different exception " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Test
	public void testWithAssociatedData() throws EncryptionException, IOException {
		EncryptionProviderStreaming provider = new EncryptionProviderBuilder().buildStreamingProvider();
		byte[] encryptedBytes = createEncryptedBytesViaStream(INPUT, ASSOCIATED_DATA.getBytes(StandardCharsets.UTF_8), provider);
		Assert.assertEquals("Decrypted string was not identical to original", createStringFromEncryptedBytesViaStream(encryptedBytes, ASSOCIATED_DATA.getBytes(StandardCharsets.UTF_8), provider), INPUT);
		try {
			Assert.assertNotEquals("Decrypted string identical to original despite not having associated data", createStringFromEncryptedBytesViaStream(encryptedBytes, null, provider), INPUT);
			Assert.fail("Decryption should have failed!");
		} catch (EncryptionFailedException e) {
			// expected
		}
	}

	private byte[] createEncryptedBytesViaStream(String input, byte[] associatedData, EncryptionProviderStreaming provider) throws IOException, EncryptionException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		OutputStream encryptedOut = provider.getEncryptionStream(bout, associatedData);
		byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
		encryptedOut.write(inputBytes, 0, inputBytes.length);
		encryptedOut.close();
		return bout.toByteArray();
	}

	private String createStringFromEncryptedBytesViaStream(byte[] encrypted, byte[] associatedData, EncryptionProviderStreaming provider) throws IOException, EncryptionException {
		ByteArrayInputStream bin = new ByteArrayInputStream(encrypted);
		InputStream decryptedIn = provider.getDecryptionStream(bin, associatedData);
		try {
			return IOUtils.toString(decryptedIn, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new EncryptionFailedException(e);
		}
	}
}
