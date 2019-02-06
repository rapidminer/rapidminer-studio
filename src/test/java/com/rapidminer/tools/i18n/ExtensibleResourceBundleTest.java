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
package com.rapidminer.tools.i18n;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.tools.ExtensibleResourceBundle;


/**
 * Tests the behavior of the ExtensibleResourceBundle class
 * <p>
 * The tests are very limited and don't contain hierarchical ResourceBundles
 *
 * @author Jonas Wilms-Pfau
 * @since 9.1.0
 */
public class ExtensibleResourceBundleTest {

	/**
	 * Remembers the original Locale before the test
	 */
	private static Locale locale;

	/**
	 * Bundle that can hold a single key-value pair, no language hierarchy
	 */
	private static final class DummyBundle extends ResourceBundle {
		private String key;
		private String value;
		private Locale locale;

		public DummyBundle(String key, String value, String language) {
			this.key = key;
			this.value = value;
			this.locale = Locale.forLanguageTag(language);
		}

		@Override
		protected Object handleGetObject(String key) {
			if (key.equals(this.key)) {
				return value;
			} else {
				return null;
			}
		}

		@Override
		public Enumeration<String> getKeys() {
			return new Enumeration<String>() {
				private boolean found = false;

				@Override
				public boolean hasMoreElements() {
					return !found;
				}

				@Override
				public String nextElement() {
					found = true;
					return key;
				}
			};
		}

		@Override
		public Locale getLocale() {
			return locale;
		}
	}

	@BeforeClass
	public static void retrieveLocale() {
		locale = Locale.getDefault();
	}

	@AfterClass
	public static void resetLocale() {
		Locale.setDefault(locale);
	}

	@Test
	public void testOrder() {
		Locale.setDefault(Locale.forLanguageTag("de"));
		String key = "foobar";
		String expected = "second";
		ExtensibleResourceBundle test = new ExtensibleResourceBundle(new DummyBundle(key, "root", ""));
		// first registered german language
		test.addResourceBundle(new DummyBundle(key, expected, "de"));
		test.addResourceBundle(new DummyBundle(key, "third", ""));
		test.addResourceBundle(new DummyBundle(key, "fourth", "de"));
		Assert.assertEquals(expected, test.getString(key));
	}

	@Test
	public void testOrderWithCountry() {
		Locale.setDefault(Locale.forLanguageTag("de-AT"));
		String key = "foobar";
		String expected = "second";
		ExtensibleResourceBundle test = new ExtensibleResourceBundle(new DummyBundle(key, "root", ""));
		test.addResourceBundle(new DummyBundle(key, "first", "de"));
		test.addResourceBundle(new DummyBundle("bla", "bla", "de-AT"));
		// first registered german austrian bundle that matches the key
		test.addResourceBundle(new DummyBundle(key, expected, "de-AT"));
		test.addResourceBundle(new DummyBundle(key, "third", "de-DE"));
		test.addResourceBundle(new DummyBundle(key, "fourth", "de-AT"));
		Assert.assertEquals(expected, test.getString(key));
	}

	@Test
	public void testIcon() {
		Locale.setDefault(Locale.forLanguageTag("de"));
		String key = "rapidminer.icon";
		String expected = "rapidminer.png";
		ExtensibleResourceBundle test = new ExtensibleResourceBundle(new DummyBundle(key, expected, ""));
		test.addResourceBundle(new DummyBundle(key, "evil.tiff", "de"));
		test.addResourceBundle(new DummyBundle(key, "ugly.jpg", "de"));
		Assert.assertEquals(expected, test.getString(key));
	}

	@Test
	public void testNewIcon() {
		Locale.setDefault(Locale.forLanguageTag("de"));
		String key = "new.icon";
		String expected = "extension.png";
		ExtensibleResourceBundle test = new ExtensibleResourceBundle(new DummyBundle("rapidminer.icon", "rapidminer.png", ""));
		test.addResourceBundle(new DummyBundle(key, expected, "de"));
		test.addResourceBundle(new DummyBundle(key, "evil.tiff", "de"));
		Assert.assertEquals(expected, test.getString(key));
	}


	@Test
	public void testFallback() {
		Locale.setDefault(Locale.forLanguageTag("ja"));
		String key = "YES";
		String expected = "yes";
		ExtensibleResourceBundle test = new ExtensibleResourceBundle(new DummyBundle(key, expected, ""));
		test.addResourceBundle(new DummyBundle(key, "oui", ""));
		test.addResourceBundle(new DummyBundle(key, "ja", ""));
		test.addResourceBundle(new DummyBundle(key, "si", ""));
		Assert.assertEquals(expected, test.getString(key));
	}

}
