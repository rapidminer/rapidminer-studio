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
package com.rapidminer.gui.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.cipher.KeyGenerationException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * @author Andreas Timm
 * @since 8.1
 */
public class WalletTest {

	private static final String SECRETS_XML = "secrets.xml";
	private static final String CREDENTIALS_XML = "credentials.xml";
	private static File tmpRMUserDir;
	private static String originalUserHome;
	private static String secretsXmlContentBefore;

	@BeforeClass
	public static void setup() throws KeyGenerationException, IOException {
		originalUserHome = System.getProperty("user.home");

		File tmpDir = File.createTempFile("wallet", "test");
		tmpDir.delete();
		tmpDir.mkdir();
		tmpRMUserDir = new File(tmpDir, FileSystemService.RAPIDMINER_USER_FOLDER);
		tmpRMUserDir.mkdir();
		System.setProperty("user.home", tmpDir.getAbsolutePath());

		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);

		// init old encryption framework
		if (!CipherTools.isKeyAvailable()) {
			KeyGeneratorTool.storeKey(KeyGeneratorTool.createSecretKey().getEncoded(), new File(tmpRMUserDir, "cipher.key").toPath());
		}
		// init new (9.7+) encryption framework
		EncryptionProvider.initialize();

		URL resource = WalletTest.class.getResource(SECRETS_XML);
		File newSecretsXml = new File(tmpRMUserDir, SECRETS_XML);
		OutputStream fos = new FileOutputStream(newSecretsXml);
		Tools.copyStreamSynchronously(resource.openStream(), fos, true);

		secretsXmlContentBefore = readFile(new File(tmpRMUserDir, SECRETS_XML).getAbsolutePath());
	}

	@AfterClass
	public static void cleanUp() {
		System.setProperty("user.home", originalUserHome);
	}

	@Test
	public void testReadSecureStorage() throws IOException {
		//expecting migration while loading so the content should have changed
		String secretsXmlContentAfter = readFile(new File(tmpRMUserDir, CREDENTIALS_XML).getAbsolutePath());
		Assert.assertNotEquals(secretsXmlContentBefore, secretsXmlContentAfter);
		Wallet.getInstance().readCache();
		Assert.assertEquals(5,  Wallet.getInstance().getKeys().size());
	}

	@Test
	public void testCloning() {
		Wallet w1 = Wallet.getInstance();
		Wallet w2 = new Wallet(w1);
		Assert.assertEquals(w1.size(), w2.size());

		String key = w2.getKeys().get(0);
		w2.registerCredentials(key + "_TEST", new UserCredential(w2.getEntry(key)));
		Assert.assertEquals(w2.size(), w1.size() + 1);

		UserCredential entry1 = w1.getEntry(key);
		UserCredential entry2 = w2.getEntry(key);
		Assert.assertEquals(entry1.getURL(), entry2.getURL());
		Assert.assertEquals(entry1.getUsername(), entry2.getUsername());
		Assert.assertArrayEquals(entry1.getPassword(), entry2.getPassword());

		entry2.getPassword()[0] = 'x';
		Assert.assertNotEquals(Arrays.toString(entry1.getPassword()), Arrays.toString(entry2.getPassword()));

		w2.registerCredentials(w2.extractIdFromKey(key), new UserCredential(entry1.getURL(), "user", "secure".toCharArray()));
		entry2 = w2.getEntry(key);
		// url is encoded in the key. UserCredentials are overridden.
		Assert.assertEquals(entry1.getURL(), entry2.getURL());
		Assert.assertNotEquals(entry1.getUsername(), entry2.getUsername());
		Assert.assertNotEquals(Arrays.toString(entry1.getPassword()), Arrays.toString(entry2.getPassword()));

		w1.removeEntry(key);
		Assert.assertNotNull(w2.getEntry(key));
		Assert.assertEquals(w2.size(), w1.size() + 2);
	}

	private static String readFile(String path) throws IOException {
		return Tools.readTextFile(Files.newInputStream(Paths.get(path)));
	}

}