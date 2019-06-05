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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.tools.ParameterService;

/**
 * Test some methods of the {@link Plugin} class.
 *
 * @author Jan Czogalla
 * @since 9.0.0
 */
public class PluginTest {

	private static final String PACKAGED_ID = "rmx_professional";
	private static final String SHIPPED_ID = "rmx_dataeditor";
	private static final String OTHER_ID = "rmx_other";
	private static final String[] IDS = {PACKAGED_ID, SHIPPED_ID, OTHER_ID};

	private static Plugin[] plugins;

	private static Method privateWhitelistCheck;


	@BeforeClass
	public static void setup() throws NoSuchMethodException {
		Plugin unsignedPackaged = createMock(PACKAGED_ID, false);
		Plugin signedPackaged = createMock(PACKAGED_ID, true);
		Plugin unsignedShipped = createMock(SHIPPED_ID, false);
		Plugin signedShipped = createMock(SHIPPED_ID, true);
		Plugin other = createMock(OTHER_ID, false);
		plugins = new Plugin[]{unsignedPackaged, signedPackaged, unsignedShipped, signedShipped, other};
		privateWhitelistCheck = Plugin.class.getDeclaredMethod("isExtensionWhitelisted", Plugin.class, String.class);
		privateWhitelistCheck.setAccessible(true);
		// init whitelist first time
		Plugin.initAll();
	}

	/** Create a simple {@link Plugin} mock with the given ID and signed status */
	private static Plugin createMock(String packagedId, boolean signed) {
		Plugin plugin = Mockito.mock(Plugin.class);
		Mockito.when(plugin.getExtensionId()).thenReturn(packagedId);
		Mockito.when(plugin.isSigned()).thenReturn(signed);
		return plugin;
	}

	/** Test for mismatching {@link Plugin Plugins}/IDs; using reflection */
	@Test
	public void isExtensionWhiteListedIDMismatchTest() throws InvocationTargetException, IllegalAccessException {
		if (privateWhitelistCheck == null) {
			return;
		}
		for (String id : IDS) {
			for (Plugin plugin : plugins) {
				if (plugin.getExtensionId().equals(id)) {
					continue;
				}
				Assert.assertFalse("Extension mismatch not detected: " + plugin.getExtensionId() + "/" + id,
						(Boolean) privateWhitelistCheck.invoke(null, plugin, id));
			}
		}
	}

	/** Check null, empty and whitespace-only parameter; null parameter relies on unset parameter in the beginning */
	@Test
	public void isExtensionWhiteListedNoRestrictionsTest() {
		String[] allAllowedVariances = {null, "", " "};
		for (String allAllowed : allAllowedVariances) {
			testAdminParameter(allAllowed, id -> true, p -> true);
		}
	}

	/** Check the keyword {@value Plugin#WHITELIST_NONE} that overrides any other config */
	@Test
	public void isExtensionWhiteListedNoneTest() {
		String none = Plugin.WHITELIST_NONE;
		String[] addons = {"," + Plugin.WHITELIST_SHIPPED, "," + OTHER_ID};
		Predicate<String> packagedIDTest = id -> id.equals(PACKAGED_ID);
		Predicate<Plugin> packaedPluginTest = p -> p.getExtensionId().equals(PACKAGED_ID) && p.isSigned();
		testAdminParameter(none, packagedIDTest, packaedPluginTest);
		testAdminParameter(none + addons[0], packagedIDTest, packaedPluginTest);
		testAdminParameter(none + addons[1], packagedIDTest, packaedPluginTest);
		testAdminParameter(none + addons[0] + addons[1], packagedIDTest, packaedPluginTest);
	}

	/** Check the keyword {@value Plugin#WHITELIST_SHIPPED} alone and together with another non-shipped ID */
	@Test
	public void isExtensionWhiteListedShippedTest() {
		String shipped = Plugin.WHITELIST_SHIPPED;
		testAdminParameter(shipped, id -> !id.equals(OTHER_ID), p -> !p.getExtensionId().equals(OTHER_ID) && p.isSigned());
		testAdminParameter(shipped + "," + OTHER_ID, id -> true, p -> p.getExtensionId().equals(OTHER_ID) || p.isSigned());
	}

	/** Check parameter with no keywords; also special cases where shipped or packaged IDs are in the list */
	@Test
	public void isExtensionWhiteListedListTest() {
		testAdminParameter(OTHER_ID, id -> !id.equals(SHIPPED_ID), p -> p.getExtensionId().equals(OTHER_ID) || p.getExtensionId().equals(PACKAGED_ID) && p.isSigned());
		testAdminParameter(SHIPPED_ID, id -> !id.equals(OTHER_ID), p -> !p.getExtensionId().equals(OTHER_ID) && p.isSigned());
		testAdminParameter(PACKAGED_ID, id -> id.equals(PACKAGED_ID), p -> p.getExtensionId().equals(PACKAGED_ID) && p.isSigned());
		testAdminParameter(SHIPPED_ID + "," + OTHER_ID, id -> true, p -> p.getExtensionId().equals(OTHER_ID) || p.isSigned());
	}

	/**
	 * Test a certain configuration for the property {@value Plugin#PROPERTY_PLUGINS_WHITELIST}. Will set the parameter
	 * via the {@link ParameterService} and then run tests on all the different IDs and {@link Plugin} mocks, with expected
	 * values specified by the predicates {@code idTest} and {@code pluginTest}.
	 */
	private void testAdminParameter(String parameter, Predicate<String> idTest, Predicate<Plugin> pluginTest) {
		// (re)init whitelist
		if (parameter != null) {
			ParameterService.setParameterValue(Plugin.PROPERTY_PLUGINS_WHITELIST, parameter);
		} else if (ParameterService.getParameterValue(Plugin.PROPERTY_PLUGINS_WHITELIST) != null){
			// abort if the parameter should be unset but is not
			return;
		}
		for (String id : IDS) {
			Assert.assertEquals("Problem testing id " + id + " for parameter \"" + parameter + "\"",
					idTest.test(id), Plugin.isExtensionWhitelisted(id));
		}
		for (Plugin p : plugins) {
			Assert.assertEquals("Problem testing plugin " + p.getExtensionId() + " for parameter \"" + parameter + "\"",
					pluginTest.test(p), Plugin.isExtensionWhitelisted(p));
		}
	}
}