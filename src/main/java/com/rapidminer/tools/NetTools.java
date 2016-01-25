/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

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

	protected static final String ICON_PROTOCOL = "icon";
	protected static final String RESOURCE_PROTOCOL = "resource";
	protected static final String DYNAMIC_ICON_PROTOCOL = "dynicon";

	private static boolean initialized = false;

	public static void init() {
		if (!initialized) {
			URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

				@Override
				public URLStreamHandler createURLStreamHandler(String protocol) {
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
					return null;
				}
			});
			initialized = true;
		}
	}
}
