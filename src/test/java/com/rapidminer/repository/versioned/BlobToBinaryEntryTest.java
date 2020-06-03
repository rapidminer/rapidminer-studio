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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.TestUtils;
import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundDrawDecorator;
import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundImage;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.license.AlreadyRegisteredException;
import com.rapidminer.license.InvalidProductException;
import com.rapidminer.license.location.LicenseLoadingException;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Test that legacy blob entries can be copied to new filesystem repos and continue to be usable (even though they will
 * not have a suffix).
 */
public class BlobToBinaryEntryTest {

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final String ICON_NAME = "image";
	private static final String ICON_NAME_PNG = "image.png";
	private static final String PROCESS_NAME = "p";
	private static final String DUPLICATE_AUTO_NAME = " - 2";
	private static final String PNG_SUFFIX = ".png";

	private Path repo1;
	private Path repo2;
	private LocalRepository legacyLocalRepo;
	private FilesystemRepositoryAdapter newLocalRepo;


	@BeforeClass
	public static void init() throws AlreadyRegisteredException, LicenseLoadingException, InvalidProductException, IllegalAccessException, OperatorCreationException {
		TestUtils.INSTANCE.minimalProcessUsageSetup();
	}

	@Before
	public void createTestRepos() throws IOException, RepositoryException {
		repo1 = Files.createTempDirectory(UUID.randomUUID().toString());
		repo2 = Files.createTempDirectory(UUID.randomUUID().toString());

		legacyLocalRepo = new LocalRepository("Legacy", repo1.toFile());
		newLocalRepo = (FilesystemRepositoryAdapter) FilesystemRepositoryFactory.createRepository("New", repo2, EncryptionProvider.DEFAULT_CONTEXT);

		RepositoryManager.getInstance(null).addRepository(legacyLocalRepo);
		RepositoryManager.getInstance(null).addRepository(newLocalRepo);
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(repo1.toFile());
		FileUtils.deleteDirectory(repo2.toFile());

		RepositoryManager.getInstance(null).removeRepository(legacyLocalRepo);
		RepositoryManager.getInstance(null).removeRepository(newLocalRepo);
	}

	@Test
	public void testCopyBlobToBinary() throws RepositoryException, IOException, InterruptedException, XMLException {
		ImageIcon icon = SwingTools.createIcon("48/ok.png");
		ProcessRendererModel model = new ProcessRendererModel();
		Process p = new Process();
		ProcessBackgroundImage backgroundImage = new ProcessBackgroundImage(-1, -1, -1, -1, ICON_NAME, p.getRootOperator().getSubprocesses().get(0));
		model.setBackgroundImage(backgroundImage);

		BlobEntry imageEntryLegacy = legacyLocalRepo.createBlobEntry(ICON_NAME);
		try (OutputStream output = imageEntryLegacy.openOutputStream("image/png")) {
			ImageIO.write(toBufferedImage(icon.getImage()), "png", output);
		}
		Path diskPathLegacy = repo1.resolve(ICON_NAME + BlobEntry.BLOB_SUFFIX);
		Assert.assertTrue("diskPathLegacy should have existed but did not", Files.exists(diskPathLegacy));
		Assert.assertTrue("diskPathLegacy should have been larger than 0 bytes", Files.size(diskPathLegacy) > 0);
		ProcessEntry processEntryLegacy = legacyLocalRepo.createProcessEntry(PROCESS_NAME, p.getRootOperator().getXML(false));
		Path diskPathProcessLegacy = repo1.resolve(PROCESS_NAME + ProcessEntry.RMP_SUFFIX);
		Assert.assertTrue("diskPathProcessLegacy should have existed but did not", Files.exists(diskPathProcessLegacy));
		Assert.assertTrue("diskPathProcessLegacy should have been larger than 0 bytes", Files.size(diskPathProcessLegacy) > 0);

		RepositoryManager.getInstance(null).copy(imageEntryLegacy.getLocation(), newLocalRepo, null);
		RepositoryManager.getInstance(null).copy(processEntryLegacy.getLocation(), newLocalRepo, null);
		BlobEntry oldImageEntry = RepositoryManager.getInstance(null).locateData(legacyLocalRepo, ICON_NAME, BlobEntry.class, false, true);
		BinaryEntry imageEntryNew = RepositoryManager.getInstance(null).locateData(newLocalRepo, ICON_NAME, BinaryEntry.class, false, true);
		ProcessEntry processEntryNew = RepositoryManager.getInstance(null).locateData(newLocalRepo, PROCESS_NAME, ProcessEntry.class, false, true);
		Assert.assertNotNull("oldImageEntry was null!", oldImageEntry);
		Assert.assertNotNull("imageEntryNew was null!", imageEntryNew);
		Assert.assertNotNull("processEntryNew was null!", processEntryNew);
		Path diskPathNew = repo2.resolve(ICON_NAME);
		Assert.assertTrue("diskPathNew should have existed but did not", Files.exists(diskPathNew));
		Assert.assertTrue("diskPathNew should have been larger than 0 bytes", Files.size(diskPathNew) > 0);

		// make sure both processes have exactly the same XML as they use a relative bg image path
		String xmlOld = processEntryLegacy.retrieveXML();
		String xmlNew = processEntryNew.retrieveXML();
		Assert.assertEquals("Process XML was different but should have been identical!", xmlOld, xmlNew);

		Process legacyProcess = new Process(xmlOld);
		legacyProcess.setProcessLocation(new RepositoryProcessLocation(processEntryLegacy.getLocation()));
		// now draw process with both bg images and ensure that there is no difference
		model.setProcess(legacyProcess, true, false);
		model.setDisplayedChain(legacyProcess.getRootOperator());
		model.setProcesses(legacyProcess.getRootOperator().getSubprocesses());
		model.setProcessSize(legacyProcess.getRootOperator().getSubprocesses().get(0), new Dimension(WIDTH, HEIGHT));
		ProcessDrawer drawer = new ProcessDrawer(model, false);

		// blob bg image
		CountDownLatch drawLatch = new CountDownLatch(1);
		// wait until bg image has loaded
		backgroundImage = new ProcessBackgroundImage(-1, -1, -1, -1, ICON_NAME, legacyProcess.getRootOperator().getSubprocesses().get(0));
		model.setBackgroundImage(backgroundImage);
		backgroundImage.getImage(((RepositoryProcessLocation)legacyProcess.getProcessLocation()).getRepositoryLocation().parent().getAbsoluteLocation(), thread -> drawLatch.countDown());
		if (!drawLatch.await(5, TimeUnit.SECONDS)) {
			Assert.fail("Waited 5 seconds for loading background image from legacy repo, did not load in time");
		}

		drawer.addDecorator(new ProcessBackgroundDrawDecorator(null), RenderPhase.BACKGROUND);
		BufferedImage processImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = processImage.createGraphics();
		drawer.draw(g2, true);
		g2.dispose();

		byte[] byteArrayLegacy;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(processImage, "png", baos);
			byteArrayLegacy = baos.toByteArray();
		}
		Assert.assertTrue("byte array of process image of process with legacy background image was empty!", byteArrayLegacy.length > 0);

		// binary entry bg image
		Process newProcess = new Process(xmlNew);
		newProcess.setProcessLocation(new RepositoryProcessLocation(processEntryNew.getLocation()));
		model.setProcess(newProcess, true, false);
		model.setDisplayedChain(newProcess.getRootOperator());
		model.setProcesses(newProcess.getRootOperator().getSubprocesses());
		model.setProcessSize(newProcess.getRootOperator().getSubprocesses().get(0), new Dimension(WIDTH, HEIGHT));

		// load bg image
		CountDownLatch drawLatchNew = new CountDownLatch(1);
		// wait until bg image has loaded
		backgroundImage = new ProcessBackgroundImage(-1, -1, -1, -1, ICON_NAME, newProcess.getRootOperator().getSubprocesses().get(0));
		model.setBackgroundImage(backgroundImage);
		backgroundImage.getImage(((RepositoryProcessLocation)newProcess.getProcessLocation()).getRepositoryLocation().parent().getAbsoluteLocation(), thread -> drawLatchNew.countDown());
		if (!drawLatchNew.await(5, TimeUnit.SECONDS)) {
			Assert.fail("Waited 5 seconds for loading background image from new repo, did not load in time");
		}

		processImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		g2 = processImage.createGraphics();
		drawer.draw(g2, true);
		g2.dispose();

		byte[] byteArrayNew;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(processImage, "png", baos);
			byteArrayNew = baos.toByteArray();
		}
		Assert.assertTrue("byte array of process image of process with new background image was empty!", byteArrayNew.length > 0);
		Assert.assertArrayEquals("byte representation of images of rendered processes was different between legacy and new background image", byteArrayLegacy, byteArrayNew);
	}

	@Test
	public void testCopyBlobToBinaryDuplicate() throws RepositoryException, IOException, InterruptedException, XMLException {
		ImageIcon icon = SwingTools.createIcon("48/ok.png");
		BlobEntry imageEntryLegacy = legacyLocalRepo.createBlobEntry(ICON_NAME);
		try (OutputStream output = imageEntryLegacy.openOutputStream("image/png")) {
			ImageIO.write(toBufferedImage(icon.getImage()), "png", output);
		}
		BlobEntry imageEntryLegacyPng = legacyLocalRepo.createBlobEntry(ICON_NAME_PNG);
		try (OutputStream output = imageEntryLegacyPng.openOutputStream("image/png")) {
			ImageIO.write(toBufferedImage(icon.getImage()), "png", output);
		}
		Path diskPathLegacy = repo1.resolve(ICON_NAME + BlobEntry.BLOB_SUFFIX);
		Assert.assertTrue("diskPathLegacy should have existed but did not", Files.exists(diskPathLegacy));
		Assert.assertTrue("diskPathLegacy should have been larger than 0 bytes", Files.size(diskPathLegacy) > 0);
		Path diskPathLegacyPng = repo1.resolve(ICON_NAME_PNG + BlobEntry.BLOB_SUFFIX);
		Assert.assertTrue("diskPathLegacyPng should have existed but did not", Files.exists(diskPathLegacyPng));
		Assert.assertTrue("diskPathLegacyPng should have been larger than 0 bytes", Files.size(diskPathLegacyPng) > 0);

		RepositoryManager.getInstance(null).copy(imageEntryLegacy.getLocation(), newLocalRepo, null);
		RepositoryManager.getInstance(null).copy(imageEntryLegacy.getLocation(), newLocalRepo, null);
		BinaryEntry imageEntryNew1 = RepositoryManager.getInstance(null).locateData(newLocalRepo, ICON_NAME, BinaryEntry.class, false, true);
		BinaryEntry imageEntryNew2 = RepositoryManager.getInstance(null).locateData(newLocalRepo, ICON_NAME + DUPLICATE_AUTO_NAME, BinaryEntry.class, false, true);
		Assert.assertNotNull("imageEntryNew1 was null!", imageEntryNew1);
		Assert.assertNotNull("imageEntryNew2 was null!", imageEntryNew2);
		Path diskPathNew1 = repo2.resolve(ICON_NAME);
		Assert.assertTrue("diskPathNew should have existed but did not", Files.exists(diskPathNew1));
		Assert.assertTrue("diskPathNew should have been larger than 0 bytes", Files.size(diskPathNew1) > 0);

		Path diskPathNew2 = repo2.resolve(ICON_NAME + DUPLICATE_AUTO_NAME);
		Assert.assertTrue("diskPathNew should have existed but did not", Files.exists(diskPathNew2));
		Assert.assertTrue("diskPathNew should have been larger than 0 bytes", Files.size(diskPathNew2) > 0);

		RepositoryManager.getInstance(null).copy(imageEntryLegacyPng.getLocation(), newLocalRepo, null);
		RepositoryManager.getInstance(null).copy(imageEntryLegacyPng.getLocation(), newLocalRepo, null);
		BinaryEntry imageEntryNewPng1 = RepositoryManager.getInstance(null).locateData(newLocalRepo, ICON_NAME_PNG, BinaryEntry.class, false, true);
		BinaryEntry imageEntryNewPng2 = RepositoryManager.getInstance(null).locateData(newLocalRepo, ICON_NAME + DUPLICATE_AUTO_NAME + PNG_SUFFIX, BinaryEntry.class, false, true);
		Assert.assertNotNull("imageEntryNewPng1 was null!", imageEntryNewPng1);
		Assert.assertNotNull("imageEntryNewPng2 was null!", imageEntryNewPng2);
		Path diskPathNewPng1 = repo2.resolve(ICON_NAME_PNG);
		Assert.assertTrue("diskPathNewPng1 should have existed but did not", Files.exists(diskPathNewPng1));
		Assert.assertTrue("diskPathNewPng1 should have been larger than 0 bytes", Files.size(diskPathNewPng1) > 0);

		Path diskPathNewPng2 = repo2.resolve(ICON_NAME + DUPLICATE_AUTO_NAME + PNG_SUFFIX);
		Assert.assertTrue("diskPathNewPng2 should have existed but did not", Files.exists(diskPathNewPng2));
		Assert.assertTrue("diskPathNewPng2 should have been larger than 0 bytes", Files.size(diskPathNewPng2) > 0);
	}

	@Test
	public void testCopyBinaryToBlob() throws RepositoryException, IOException {
		ImageIcon icon = SwingTools.createIcon("48/ok.png");
		BinaryEntry imageEntry = newLocalRepo.createBinaryEntry(ICON_NAME + PNG_SUFFIX);
		try (OutputStream output = imageEntry.openOutputStream()) {
			ImageIO.write(toBufferedImage(icon.getImage()), "png", output);
		}
		Path diskPathNew = repo2.resolve(ICON_NAME + PNG_SUFFIX);
		Assert.assertTrue("diskPathNew should have existed but did not", Files.exists(diskPathNew));
		Assert.assertTrue("diskPathNew should have been larger than 0 bytes", Files.size(diskPathNew) > 0);

		try {
			RepositoryManager.getInstance(null).copy(imageEntry.getLocation(), legacyLocalRepo, null);
			Assert.fail("Copying binary entry to legacy repo should have failed but worked!");
		} catch (RepositoryException e) {
			if (!e.getMessage().contains("Cannot copy entry")) {
				throw e;
			}
			// all good, expected
		}
	}


	private static BufferedImage toBufferedImage(Image img) {
		BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2 = bufferedImage.createGraphics();
		g2.drawImage(img, 0, 0, null);
		g2.dispose();

		return bufferedImage;
	}
}
