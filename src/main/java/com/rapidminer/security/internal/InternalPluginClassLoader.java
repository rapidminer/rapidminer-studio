/**
 * Copyright (C) 2001-2016 RapidMiner GmbH
 */
package com.rapidminer.security.internal;

import java.net.URL;

import com.rapidminer.tools.plugin.PluginClassLoader;


/**
 * This classloader extends a {@link PluginClassLoader}. It offers no additional functionality but
 * should always be used when we create classloaders to dynamically load code from special internal
 * extensions which need to load unsigned library jars from arbitrary places (e.g. Radoop).
 * <p>
 * Note that the parent will check for {@code checkCreateClassLoader} permission so only our signed
 * extensions and our own code can use it.
 * </p>
 *
 * @author Marco Boeck
 * @since 7.2
 */
public class InternalPluginClassLoader extends PluginClassLoader {

	/**
	 * @see PluginClassLoader#PluginClassLoader(URL[])
	 */
	public InternalPluginClassLoader(URL[] urls) {
		super(urls);
	}
}
