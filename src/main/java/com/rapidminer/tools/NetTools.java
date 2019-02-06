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
package com.rapidminer.tools;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Level;

import com.rapidminer.gui.tools.SwingTools;


/**
 * Class providing new protocols special for RapidMiner. Currently it supports the icon:// protocol,
 * that will use the given path to load the icon using new URL on
 * {@link SwingTools#getIconPath(String)}.
 *
 * @author Sebastian Land
 *
 */
public class NetTools {

	private static final String ICON_PROTOCOL = "icon";
	private static final String RESOURCE_PROTOCOL = "resource";
	private static final String DYNAMIC_ICON_PROTOCOL = "dynicon";
	private static boolean initialized = false;
	private static URLStreamHandlerFactory existingUrlHandlerFactory;

	public static void init() {
		if (!initialized) {
			// we need to work around a stupid assumption by a certain JDBC driver (Amazon Redshift) that thinks he is a standalone application and sets the factory
			try {
				Field urlHandlerFactoryField = URL.class.getDeclaredField("factory");
				urlHandlerFactoryField.setAccessible(true);
				existingUrlHandlerFactory = (URLStreamHandlerFactory) urlHandlerFactoryField.get(null);
				// now set value of factory to null so that our own factory can be properly set afterwards
				urlHandlerFactoryField.set(null, null);
			} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
				// this should never happen unless Java changes something in that class
				// We don't stop in this case however, because unless you have a stupid JDBC driver installed, this causes no further problems
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.NetTools.cannot_access_URL_factory", e);
			}

			// set our own factory now which will ask a potentially previously registered factory if we do nothing with a protocol
			URL.setURLStreamHandlerFactory(protocol -> {
				// try our own handlers
				if (ICON_PROTOCOL.equals(protocol)) {
					return new URLStreamHandler() {

						@Override
						protected URLConnection openConnection(URL u) throws IOException {
							URL resource = Tools.getResource("icons" + u.getPath());
							if (resource != null) {
								URLConnection conn = resource.openConnection();
								WebServiceTools.setURLConnectionDefaults(conn);
								return conn;
							}
							throw new IOException("Icon not found.");
						}
					};
				} else if (RESOURCE_PROTOCOL.equals(protocol)) {
					return new URLStreamHandler() {

						@Override
						protected URLConnection openConnection(URL u) throws IOException {
							URL resource = Tools.getResource(u.getPath().substring(1, u.getPath().length()));
							if (resource != null) {
								URLConnection conn = resource.openConnection();
								WebServiceTools.setURLConnectionDefaults(conn);
								return conn;
							}
							throw new IOException("Resource not found.");

						}
					};
				} else if (DYNAMIC_ICON_PROTOCOL.equals(protocol)) {
					return new DynamicIconUrlStreamHandler();
				}

				// try the factory that was registered by some dumb JDBC driver, if something is returned, use it
				if (existingUrlHandlerFactory != null) {
					URLStreamHandler handler = existingUrlHandlerFactory.createURLStreamHandler(protocol);
					if (handler != null) {
						return handler;
					}
				}

				// nothing to do with the protocol, let Java handle it (e.g. http)
				return null;
			});
			initialized = true;
		}
	}

}
