/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

import org.junit.Test;


/**
 * This test copies & pastes the {@link com.rapidminer.tools.WebServiceTools#clearAuthCache} code as it would swallow
 * any exceptions. The purpose of this test is to make sure the sun classes currently in use will still be available in
 * future, OpenJDK Java builds.
 *
 * @author Marco Boeck
 * @since 9.1
 */
public class AuthCacheClearingTest {

	@Test
	public void testWebServiceToolsClearAuthCache() {
		// remove this test once WebServiceTools#clearAuthCache() no longer uses the code below
		try {
			Class<?> authCacheValueClass = Class.forName("sun.net.www.protocol.http.AuthCacheValue");
			Class<?> authCacheClass = Class.forName("sun.net.www.protocol.http.AuthCache");
			Class<?> authCacheImplClass = Class.forName("sun.net.www.protocol.http.AuthCacheImpl");
			Constructor<?> authCacheImplConstructor = authCacheImplClass.getConstructor();
			Method setAuthCacheMethod = authCacheValueClass.getMethod("setAuthCache", authCacheClass);
			setAuthCacheMethod.invoke(null, authCacheImplConstructor.newInstance());
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Clearing Auth Cache failed! " + t.getMessage());
		}
	}
}
