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

import java.lang.reflect.Method;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.StreamHandler;


/**
 * The regular {@link ConsoleHandler} fails to call super.close() which prevents
 * {@link Formatter#getTail(java.util.logging.Handler)} to be written propertly. We solve this by
 * reflectively calling the private method {@link StreamHandler#flushAndClose}.
 * 
 * @author Simon Fischer
 * 
 */
public class FixedConsoleHandler extends ConsoleHandler {

	@Override
	public void close() {
		super.close();

		Method flushAndClose;
		try {
			flushAndClose = StreamHandler.class.getDeclaredMethod("flushAndClose", new Class[0]);
			flushAndClose.setAccessible(true);
			flushAndClose.invoke(this);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

}
