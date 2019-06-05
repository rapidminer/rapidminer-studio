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
package com.rapidminer.tools.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.tools.FileSystemService;


/**
 * Test the ManagedExtension class.
 * Execute in any sequential order.
 *
 * @author Andreas Timm
 * @since 8.2
 */
@FixMethodOrder(MethodSorters.DEFAULT)
public class ManagedExtensionTest {

	private static File tmpRMuserDir;
	private static String originalUserHome;

	@Before
	public void setup() throws IOException {
		originalUserHome = System.getProperty("user.home");

		File tmpDir = File.createTempFile("managedextension", "test");
		tmpDir.delete();
		tmpDir.mkdir();
		tmpRMuserDir = new File(tmpDir, FileSystemService.RAPIDMINER_USER_FOLDER);
		tmpRMuserDir.mkdir();
		System.setProperty("user.home", tmpDir.getAbsolutePath());
		File managedDir = new File(tmpRMuserDir, "managed");

		managedDir.mkdir();
		File extensionXml = new File(managedDir, "extensions.xml");

		URL resource = ManagedExtensionTest.class.getResource("extensions.xml");
		try (OutputStream fos = new FileOutputStream(extensionXml)) {
			copy(resource.openStream(), fos);
		}

		ManagedExtension.init();
	}

	@AfterClass
	public static void cleanUp() {
		System.setProperty("user.home", originalUserHome);
	}

	@Test
	public void testInit() throws IOException {
		ManagedExtensionTestImpl[] expectedExtensions = new ManagedExtensionTestImpl[5];
		expectedExtensions[0] = new ManagedExtensionTestImpl("rmx_test", "Process Testing", true, "RM_EULA", new VersionNumber(7, 2, 0), new VersionNumber[]{new VersionNumber(7, 2, 0)});
		expectedExtensions[1] = new ManagedExtensionTestImpl("rmx_text", "Text Processing", true, "RM_EULA", new VersionNumber(7, 5, 0), new VersionNumber[]{new VersionNumber(7, 5, 0), new VersionNumber(8, 1, 0)});
		expectedExtensions[2] = new ManagedExtensionTestImpl("rmx_web", "Web Mining", true, "RM_EULA", new VersionNumber(7, 3, 0), new VersionNumber[]{new VersionNumber(7, 3, 0)});
		// the next one has a malformed installed version number
		expectedExtensions[3] = new ManagedExtensionTestImpl("rmx_w00t", "W00t Mining", false, "w00t5t0ck", new VersionNumber(7, 3, 0), null);
		// the next has no selected version
		expectedExtensions[4] = new ManagedExtensionTestImpl("rmx_none_selected_use_last", "none selected use last", false, "none", new VersionNumber(8, 0, 1), new VersionNumber[]{new VersionNumber(6, 7, 99), new VersionNumber(7, 13, 20), new VersionNumber(8, 0, 1)});

		Assert.assertEquals("ManagedExtension init() should load all the configured extensions", expectedExtensions.length, ManagedExtension.getAll().size());
		Assert.assertEquals("There are no jar artifacts available so the ManagedExtension should show no available Plugin jars", 0, ManagedExtension.getActivePluginJars().size());

		for (ManagedExtensionTestImpl expectedExtension : expectedExtensions) {
			ManagedExtension managedExtension = ManagedExtension.get(expectedExtension.id);
			Assert.assertNotNull("Did not find ManagedExtension for '" + expectedExtension.id + "'", managedExtension);
			Assert.assertEquals(expectedExtension.id, managedExtension.getPackageId());
			Assert.assertEquals("The ManagedExtension '" + managedExtension.getPackageId() + "' is using a different selected version than expected", expectedExtension.selectedVNr, managedExtension.getSelectedVersion());
			if (expectedExtension.installedVNrs == null) {
				Assert.assertTrue("The installed version should be empty", managedExtension.getInstalledVersions().length == 0);
			} else {
				Assert.assertEquals("The ManagedExtension '" + managedExtension.getPackageId() + "' contains more installed versions than expected", expectedExtension.installedVNrs.length, managedExtension.getInstalledVersions().length);
				List<VersionNumber> installedVersionsList = Arrays.asList(managedExtension.getInstalledVersions());
				for (VersionNumber installedVNr : expectedExtension.installedVNrs) {
					Assert.assertTrue("The expected VersionNumber " + installedVNr + " is missing in the ManagedExtension " + managedExtension.getPackageId(), installedVersionsList.contains(installedVNr));
				}
			}
			Assert.assertEquals("ManagedExtension should be " + (expectedExtension.active ? "active" : "inactive"), expectedExtension.active, managedExtension.isActive());
		}
	}

	@Test
	public void alterSettings() {
		ManagedExtension.init();

		ManagedExtension[] allExtensions = ManagedExtension.getAll().toArray(new ManagedExtension[0]);
		for (ManagedExtension managedExtension : allExtensions) {
			ManagedExtension.remove(managedExtension.getPackageId());
		}

		String newPackageId = "new";
		String newPackageName = "test package name";
		ManagedExtension.getOrCreate(newPackageId, newPackageName, "FFA");
		Assert.assertEquals("There should only be the new test extension", 1, ManagedExtension.getAll().size());
		// TODO this is really ugly, the actual state leaves a misconfigured extensions.xml, got to fix this here...
		ManagedExtension newManagedExtension = ManagedExtension.get(newPackageId);
		newManagedExtension.setActive(true);
		newManagedExtension.addAndSelectVersion("7.0.0");
		ManagedExtension.saveConfiguration();
		// TODO now it works m(

		ManagedExtension.remove(newPackageId);
		Assert.assertEquals("There should be no extensions", 0, ManagedExtension.getAll().size());

		ManagedExtension.init();
		System.out.println(ManagedExtension.getUserExtensionsDir().getAbsolutePath());
		Assert.assertEquals("There should only be the new test extension", 1, ManagedExtension.getAll().size());
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

	private class ManagedExtensionTestImpl {

		private String id;
		private String name;
		private boolean active;
		private String license;
		private VersionNumber selectedVNr;
		private VersionNumber[] installedVNrs;

		private ManagedExtensionTestImpl(String id, String name, boolean active, String license, VersionNumber selectedVNr, VersionNumber[] installedVNrs) {
			this.id = id;
			this.name = name;
			this.active = active;
			this.license = license;
			this.selectedVNr = selectedVNr;
			this.installedVNrs = installedVNrs;
		}
	}

}
