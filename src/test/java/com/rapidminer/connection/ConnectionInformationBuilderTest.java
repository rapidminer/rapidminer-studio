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

import static com.rapidminer.connection.configuration.ConnectionConfigurationBuilderTest.getDefaultConfiguration;
import static com.rapidminer.connection.configuration.ConnectionConfigurationBuilderTest.getLargeConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.configuration.ConnectionResources;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Testing the {@link ConnectionInformationBuilder}.
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ConnectionInformationBuilderTest {

	private static final String CONFIGURATION_TYPE_TEST = "test";

	@Test
	public void cloneConstructor() throws IOException {
		ConnectionInformation ci = getConnectionInformation("configuration test");

		ConnectionInformation clone = new ConnectionInformationBuilder(ci).build();
		assertEquals("The clone is not equal to the original connection", ci, clone);
		assertNotSame("The clone should be a new object!", ci, clone);
	}

	@Test
	public void buildNotAllowedWithoutStatistics() {
		try {
			new ConnectionInformationBuilder(getDefaultConfiguration()).withStatistics(null).build();
		} catch (IllegalArgumentException iae) {
			assertEquals("Missing value for \"statistics\" not allowed", iae.getMessage());
		}
	}

	@Test
	public void canOrNotBeUpdated() throws IOException {
		ConnectionInformationBuilder cibCanUpdate = new ConnectionInformationBuilder(getConnectionInformation("test update"));
		ConnectionInformationBuilder cibCanNotUpdate = new ConnectionInformationBuilder(getDefaultConfiguration());
		final ConnectionConfiguration updateConfig = new ConnectionConfigurationBuilder("update it", CONFIGURATION_TYPE_TEST).build();
		cibCanUpdate.updateConnectionConfiguration(updateConfig);
		final ConnectionInformation canBuild = cibCanUpdate.build();
		assertEquals("Expecting the updated configuration to be used", updateConfig, canBuild.getConfiguration());

		try {
			cibCanNotUpdate.updateConnectionConfiguration(updateConfig);
		} catch (IllegalArgumentException iae) {
			assertEquals("Cannot update a new Connection Information object", iae.getMessage());
		}
	}

	@Test
	public void checkMetadata() {
		assertNull(ConnectionInformationSerializer.INSTANCE.getMetaData(null));
		ConnectionInformation ci = getConnectionInformation("checkMetadata");
		final ConnectionInformationMetaData metaData = ConnectionInformationSerializer.INSTANCE.getMetaData(ci);
		final Object o = read(metaData, "configuration");
		assertEquals(ci.getConfiguration(), o);
	}

	@Test
	public void loadConfig() throws IOException {
		assertNull(ConnectionInformationSerializer.INSTANCE.loadConfiguration(null, null));
	}

	@Test
	public void readSerializedConfiguration() throws IOException, URISyntaxException {
		List<Path> libfiles = new ArrayList<>();
		final Path alibfile = new File(ConnectionResources.EMPTY_JAR.toURI()).toPath();
		libfiles.add(ConnectionInformationFileUtils.addFileInternally(alibfile.getFileName().toString(), ConnectionResources.EMPTY_JAR.openStream(), null));
		List<Path> otherfiles = new ArrayList<>();
		final Path anOtherFile = new File(ConnectionResources.ENCODING_TEST_RESOURCE.toURI()).toPath();
		otherfiles.add(
				ConnectionInformationFileUtils.addFileInternally(anOtherFile.getFileName().toString(), ConnectionResources.ENCODING_TEST_RESOURCE.openStream(), null));
		Annotations annotations = new Annotations();
		annotations.setAnnotation("key", "value");
		annotations.setAnnotation("around", "the world");
		annotations.setAnnotation("mui", "loco");
		ConnectionInformation ci = new ConnectionInformationBuilder(getLargeConfiguration()).withLibraryFiles(libfiles).withOtherFiles(otherfiles).withAnnotations(annotations).build();

		// we need to store the original CI to get matching paths for the contained files
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ConnectionInformationSerializer.INSTANCE.serialize(ci, baos, EncryptionProvider.DEFAULT_CONTEXT);
		final ConnectionInformation ciReloaded = ConnectionInformationSerializer.INSTANCE.loadConnection(new ByteArrayInputStream(baos.toByteArray()), null, EncryptionProvider.DEFAULT_CONTEXT);

		assertEquals("The serialized and afterwards deserialized ConnectionInformation should be equal to the original object, but it is not", ci, ciReloaded);
	}

	static Object read(Object object, String field) {
		try {
			final Field configurationField = object.getClass().getDeclaredField(field);
			configurationField.setAccessible(true);
			return configurationField.get(object);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected ConnectionInformation getConnectionInformation(String name) {
		ConnectionConfigurationBuilder configBuilder = new ConnectionConfigurationBuilder(name, CONFIGURATION_TYPE_TEST);
		List<Path> libfiles = null;
		List<Path> otherfiles = null;
		Annotations annotations = null;
		return new ConnectionInformationBuilder(configBuilder.build()).withLibraryFiles(libfiles).withOtherFiles(otherfiles).withAnnotations(annotations).build();
	}
}