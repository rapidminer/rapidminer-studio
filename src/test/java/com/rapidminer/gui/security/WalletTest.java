/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.cipher.CipherTools;
import com.rapidminer.tools.cipher.KeyGenerationException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;


/**
 * @author Andreas Timm
 * @since 8.1
 */
public class WalletTest {

	private static File tmpRMuserDir;
	private static String originalUserHome;
	private static String secretsXmlContentBefore;

	@BeforeClass
	public static void setup() throws KeyGenerationException, IOException {
		originalUserHome = System.getProperty("user.home");

		File tmpDir = File.createTempFile("wallet", "test");
		tmpDir.delete();
		tmpDir.mkdir();
		tmpRMuserDir = new File(tmpDir, FileSystemService.RAPIDMINER_USER_FOLDER);
		tmpRMuserDir.mkdir();
		System.setProperty("user.home", tmpDir.getAbsolutePath());

		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);

		if (!CipherTools.isKeyAvailable()) {
			KeyGeneratorTool.storeKey(KeyGeneratorTool.createSecretKey().getEncoded(), new File(tmpRMuserDir, "cipher.key").toPath());
		}

		URL resource = WalletTest.class.getResource("secrets.xml");
		File newSecretsxml = new File(tmpRMuserDir, "secrets.xml");
		OutputStream fos = new FileOutputStream(newSecretsxml);
		copy(resource.openStream(), fos);
		fos.close();

		secretsXmlContentBefore = readFile(new File(tmpRMuserDir, "secrets.xml").getAbsolutePath(), Charset.defaultCharset());
	}

	@AfterClass
	public static void cleanUp() {
		System.setProperty("user.home", originalUserHome);
	}

	@Test
	public void testReadSecureStorage() throws IOException, JAXBException, TransformerException {
		//expecting migration while loading so the content should have changed
		String secretsXmlContentAfter = readFile(new File(tmpRMuserDir, "secrets.xml").getAbsolutePath(), Charset.defaultCharset());
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

	static String readFile(String path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static long copy(InputStream source, OutputStream sink)
			throws IOException {
		long nread = 0L;
		byte[] buf = new byte[1024];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
			nread += n;
		}
		return nread;
	}
}