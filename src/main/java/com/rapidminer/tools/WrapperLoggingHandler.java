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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Forwards log messages to a {@link Logger}
 * 
 * @author Simon Fischer
 * */
public class WrapperLoggingHandler implements LoggingHandler {

	public static final Level[] LEVELS = { Level.ALL, Level.FINER, Level.FINE,

	Level.INFO, Level.INFO, Level.WARNING, Level.SEVERE, Level.SEVERE, Level.SEVERE, Level.OFF };

	private Logger logger;

	public WrapperLoggingHandler() {}

	public WrapperLoggingHandler(Logger logger) {
		this.logger = logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void log(String message, int level) {
		logger.log(LEVELS[level], message);
	}

	@Override
	public void log(String message) {
		logger.info(message);
	}

	@Override
	public void logError(String message) {
		logger.severe(message);
	}

	@Override
	public void logNote(String message) {
		logger.fine(message);
	}

	@Override
	public void logWarning(String message) {
		logger.warning(message);
	}
}
