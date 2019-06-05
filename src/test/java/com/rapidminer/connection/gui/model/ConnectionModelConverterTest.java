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
package com.rapidminer.connection.gui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;
import com.rapidminer.connection.valueprovider.handler.MacroValueProviderHandler;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.resource.ResourceConnectionEntry;
import com.rapidminer.repository.resource.ResourceFolderTest;
import com.rapidminer.repository.resource.ResourceRepository;


/**
 * Test for the {@link ConnectionModelConverter}
 *
 * @since 9.3.0
 * @author Jonas Wilms-Pfau
 */
public class ConnectionModelConverterTest {

	private final boolean isEditable = false;

	private final String groupName = RandomStringUtils.randomAlphabetic(10);
	private final String name = RandomStringUtils.randomAlphabetic(10);
	private final String advancedGroupName = RandomStringUtils.randomAlphabetic(10);
	private final String value = RandomStringUtils.randomAlphabetic(10);
	private final boolean isEncrypted = RandomUtils.nextBoolean();
	private final String injectorName = RandomStringUtils.randomAlphabetic(10);
	private final boolean isEnabled = RandomUtils.nextBoolean();
	private static final List<ValueProvider> valueProviders = new ArrayList<>();

	static {
		valueProviders.add(
				new ValueProviderImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), Arrays.asList(
						new ValueProviderParameter[]{new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean())
						})));

		valueProviders.add(new ValueProviderImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), Arrays.asList(
				new ValueProviderParameter[]{new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean())
				})));
		valueProviders.add(new ValueProviderImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), Arrays.asList(
				new ValueProviderParameter[]{new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean())
				})));
		valueProviders.add(new ValueProviderImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), Arrays.asList(
				new ValueProviderParameter[]{new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean())
				})));
		valueProviders.add(new ValueProviderImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), Arrays.asList(
				new ValueProviderParameter[]{new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean()),
						new ValueProviderParameterImpl(RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(10), RandomUtils.nextBoolean(), RandomUtils.nextBoolean())
				})));
	}

	public ConnectionModelConverterTest() throws MalformedRepositoryLocationException {
		// only here for the exception
	}

	private static ConnectionInformation connection;
	private static RepositoryLocation location;

	@BeforeClass
	public static void registerRepository() throws RepositoryException {
		ResourceRepository resourceRepo = new ResourceRepository("test", "resourcerepositorytest") {
			@Override
			public boolean supportsConnections() {
				return true;
			}
		};
		Entry entry = resourceRepo.locate(Folder.CONNECTION_FOLDER_NAME+'/'+ ResourceFolderTest.TEST_CON_NAME);
		location = entry.getLocation();
		connection = ((ConnectionInformationContainerIOObject) ((ResourceConnectionEntry) entry).retrieveData(null)).getConnectionInformation();
	}

	@Test
	public void testFromConnection() throws RepositoryException {
		ConnectionModel model = ConnectionModelConverter.fromConnection(connection, location, true);
		Assert.assertEquals(model.getType(), connection.getConfiguration().getType());
		Assert.assertEquals(model.getName(), connection.getConfiguration().getName());
		Assert.assertEquals(model.getTags(), connection.getConfiguration().getTags());
		Assert.assertEquals(model.getLibraryFiles(), connection.getLibraryFiles());
		Assert.assertEquals(model.getOtherFiles(), connection.getOtherFiles());
		Assert.assertEquals(model.getLocation(), location);
	}

	@Test
	public void testApplyConnectionModel() throws RepositoryException {
		ConnectionModel model = ConnectionModelConverter.fromConnection(connection, location, true);
		ConnectionInformation same = ConnectionModelConverter.applyConnectionModel(connection, model);
		Assert.assertEquals(connection.getConfiguration().getPlaceholders(), ConnectionModelConverter.toList(model.getPlaceholders()));
		Assert.assertEquals(connection.getConfiguration().getKeyMap(), ConnectionModelConverter.toMap(model.getParameterGroups()));
		Assert.assertEquals(connection, same);
		List<String> tags = Arrays.asList("one", "two");
		model.setTags(tags);
		String description = "The description";
		model.setDescription(description);
		ConnectionInformation different = ConnectionModelConverter.applyConnectionModel(connection, model);
		Assert.assertNotEquals(different, connection);
		Assert.assertEquals(tags, different.getConfiguration().getTags());
		Assert.assertEquals(tags, model.getTags());
		Assert.assertEquals(description, model.getDescription());
		Assert.assertEquals(description, different.getConfiguration().getDescription());

		// Test clone
		ConnectionModel copy = model.copyDataOnly();
		Assert.assertEquals(model.getDescription(), copy.getDescription());
		copy.setDescription("other description");
		Assert.assertNotEquals(model.getDescription(), copy.getDescription());
	}

	@Test
	public void testConnectionModelApply() {
		ConnectionModel model = new ConnectionModel(null, location, isEditable, valueProviders.stream().map(ValueProviderModelConverter::toModel).collect(Collectors.toList()));
		ConnectionModel clone = new ConnectionModel(model);
		Assert.assertEquals(location, clone.getLocation());
		clone.addOrSetParameter(groupName, name, "other" + value, !isEncrypted, injectorName, !isEnabled);
		clone.addOrSetParameter(groupName, name, value, isEncrypted, injectorName, isEnabled);
		clone.addOrSetPlaceholder(advancedGroupName, name, "other" + value, !isEncrypted, injectorName, !isEnabled);
		clone.addOrSetPlaceholder(advancedGroupName, name, value, isEncrypted, injectorName, isEnabled);
		for (ValueProvider valueProvider : valueProviders) {
			Assert.assertTrue(clone.getValueProviders().stream().noneMatch(vp -> vp == valueProvider));
			Assert.assertTrue(clone.getValueProviders().stream().anyMatch(vp -> ValueProviderModelConverter.toValueProvider(vp).equals(valueProvider)));
		}
		clone.setValueProviders(Collections.singletonList(ValueProviderModelConverter.toModel(MacroValueProviderHandler.getInstance().createNewProvider("name"))));
		ConnectionModel clone2 = clone.copyDataOnly();
		Assert.assertFalse(clone.removePlaceholder(groupName, name));
		Assert.assertTrue(clone.removePlaceholder(advancedGroupName, name));
		Assert.assertFalse(clone.removeParameter(advancedGroupName, name));
		Assert.assertTrue(clone.removeParameter(groupName, name));
		Assert.assertEquals(isEnabled, clone2.getPlaceholders().get(0).isEnabled());
		Assert.assertEquals(isEncrypted, clone2.getPlaceholders().get(0).isEncrypted());
		Assert.assertTrue(clone2.getPlaceholders().get(0).isInjected());
		clone.valueProvidersProperty().remove(0);
		Assert.assertEquals(0, clone.getValueProviders().size());
		Assert.assertEquals(1, clone2.getValueProviders().size());
		Assert.assertEquals(0, clone.getPlaceholders().size());
		Assert.assertEquals(1, clone2.getPlaceholders().size());
		ConnectionInformation ci = ConnectionModelConverter.applyConnectionModel(connection, clone2);
		ConnectionModelConverter.fromConnection(ci, location, isEditable);
		ConnectionInformation ci2 = ConnectionModelConverter.applyConnectionModel(connection, clone);
		ConnectionModelConverter.fromConnection(ci2, location, isEditable);
	}
}