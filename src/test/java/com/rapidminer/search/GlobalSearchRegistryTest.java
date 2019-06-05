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
package com.rapidminer.search;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.search.event.GlobalSearchManagerEventHandler;
import com.rapidminer.search.event.GlobalSearchRegistryEventListener;


/**
 * Testing methods provided in the class {@link GlobalSearchRegistry}
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class GlobalSearchRegistryTest {

	private static final String NONEXISTENTCAT = "foo";
	private static final String ACATEGORY = "testcat";

	private static GlobalSearchRegistryEventListener searchRegistryEventListener;

	@BeforeClass
	public static void setup() {
		GlobalSearchIndexer.INSTANCE.initialize();

		searchRegistryEventListener = Mockito.mock(GlobalSearchRegistryEventListener.class);
		GlobalSearchRegistry.INSTANCE.addEventListener(searchRegistryEventListener);

		GlobalSearchManager searchManagerMock = Mockito.mock(GlobalSearchManager.class);
		Mockito.when(searchManagerMock.getSearchCategoryId()).thenReturn(ACATEGORY);
		Mockito.when(searchManagerMock.getSearchManagerEventHandler()).thenReturn(Mockito.mock(GlobalSearchManagerEventHandler.class));
		GlobalSearchCategory category = new GlobalSearchCategory(searchManagerMock);
		Mockito.when(searchManagerMock.getSearchCategory()).thenReturn(category);
		GlobalSearchable globalSearchable = createGlobalSearchable(searchManagerMock);
		GlobalSearchRegistry.INSTANCE.registerSearchCategory(globalSearchable);

		Mockito.verify(searchRegistryEventListener, Mockito.times(1)).searchCategoryRegistrationChanged(Mockito.any(), Mockito.any());
	}

	@Test(expected = NullPointerException.class)
	public void getCategoryNull() {
		GlobalSearchRegistry.INSTANCE.getSearchCategoryById(null);
	}

	@Test
	public void getNonexistentCategory() {
		GlobalSearchCategory searchCategoryById = GlobalSearchRegistry.INSTANCE.getSearchCategoryById(NONEXISTENTCAT);
		Assert.assertNull(NONEXISTENTCAT + " is not an existing category name but searching it resulted in " + searchCategoryById, searchCategoryById);
		Assert.assertFalse(GlobalSearchRegistry.INSTANCE.isSearchCategoryRegistered(NONEXISTENTCAT));
	}

	@Test
	public void registerNullAsSearchable() {
		try {
			GlobalSearchRegistry.INSTANCE.registerSearchCategory(null);
			Assert.fail("Successfully registered null as GlobalSearchable");
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Exception message changed", "searchable must not be null!", iae.getMessage());
		}
	}

	@Test
	public void registerSearchableWithoutSearchManager() {
		try {
			GlobalSearchable searchable = createGlobalSearchable(null);

			GlobalSearchRegistry.INSTANCE.registerSearchCategory(searchable);
			Assert.fail("Successfully registered a GlobalSearchable without a GlobalSearchManager");
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Exception message changed", "searchable must not return null for the GlobalSearchManager!", iae.getMessage());
		}
	}

	@Test
	public void registerSearchableWithSearchManagerWithoutDefaultSearchFields() {
		try {
			GlobalSearchManager searchManagerMock = Mockito.mock(GlobalSearchManager.class);
			Mockito.when(searchManagerMock.getSearchCategoryId()).thenReturn(NONEXISTENTCAT);
			Mockito.when(searchManagerMock.getAdditionalDefaultSearchFields()).thenReturn(null);
			GlobalSearchCategory category = new GlobalSearchCategory(searchManagerMock);
			Mockito.when(searchManagerMock.getSearchCategory()).thenReturn(category);
			GlobalSearchable searchable = createGlobalSearchable(searchManagerMock);

			GlobalSearchRegistry.INSTANCE.registerSearchCategory(searchable);
			Assert.fail("Successfully registered a GlobalSearchable with a GlobalSearchManager without defaultSearchFields");
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Exception message changed", "getAdditionalDefaultSearchFields() must not return null!", iae.getMessage());
		}
	}


	@Test
	public void registerSearchableWithSearchManagerWithoutCategoryId() {
		try {
			GlobalSearchManager searchManagerMock = Mockito.mock(GlobalSearchManager.class);
			Mockito.when(searchManagerMock.getSearchCategoryId()).thenReturn(null);
			Mockito.when(searchManagerMock.getAdditionalDefaultSearchFields()).thenReturn(null);
			GlobalSearchable searchable = createGlobalSearchable(searchManagerMock);

			GlobalSearchRegistry.INSTANCE.registerSearchCategory(searchable);
			Assert.fail("Successfully registered a GlobalSearchable with a GlobalSearchManager without searchCategoryId");
		} catch (IllegalArgumentException iae) {
			Assert.assertEquals("Exception message changed", "getAdditionalDefaultSearchFields() must not return null!", iae.getMessage());
		}
	}

	@Test
	public void checkExistingCategories() {
		List<GlobalSearchCategory> allSearchCategories = GlobalSearchRegistry.INSTANCE.getAllSearchCategories();
		Assert.assertEquals("Only one search category was registered successfully and should therefore be available", 1, allSearchCategories.size());
		Assert.assertEquals("The only known category should be " + ACATEGORY, ACATEGORY, allSearchCategories.get(0).getCategoryId());
		Assert.assertTrue(ACATEGORY + " should be known", GlobalSearchRegistry.INSTANCE.isSearchCategoryRegistered(ACATEGORY));
		Assert.assertNotNull(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(ACATEGORY));
	}

	@Test
	public void checkEventlistenerObjects() {
		Exception e = null;
		try {
			GlobalSearchRegistry.INSTANCE.addEventListener(null);
		} catch (Exception caught) {
			e = caught;
		}
		Assert.assertTrue(e instanceof IllegalArgumentException);
		Assert.assertEquals("Resulting error message for null as added event listener differs", "listener must not be null!", e.getMessage());

		e = null;
		try {
			GlobalSearchRegistry.INSTANCE.removeEventListener(null);
		} catch (Exception caught) {
			e = caught;
		}
		Assert.assertTrue(e instanceof IllegalArgumentException);
		Assert.assertEquals("Resulting error message for null as removed event listener differs", "listener must not be null!", e.getMessage());
	}

	@AfterClass
	public static void teardown() {
		GlobalSearchManager searchManagerMock = Mockito.mock(GlobalSearchManager.class);
		Mockito.when(searchManagerMock.getSearchCategoryId()).thenReturn(ACATEGORY);
		GlobalSearchRegistry.INSTANCE.unregisterSearchCategory(createGlobalSearchable(searchManagerMock));
		Assert.assertFalse(GlobalSearchRegistry.INSTANCE.isSearchCategoryRegistered(ACATEGORY));
		Assert.assertNull(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(ACATEGORY));

		Mockito.verify(searchRegistryEventListener, Mockito.times(2)).searchCategoryRegistrationChanged(Mockito.any(), Mockito.any());
		GlobalSearchRegistry.INSTANCE.removeEventListener(searchRegistryEventListener);

	}

	private static GlobalSearchable createGlobalSearchable(GlobalSearchManager searchManager) {
		return () -> searchManager;
	}

}
