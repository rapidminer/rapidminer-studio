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
package com.rapidminer.operator;

import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.quickfix.QuickFix;

import java.util.List;


/**
 * An error in the process setup that can be registered with the operator in which it appears using
 * {@link Operator#addError(ProcessSetupError)}. Beyond specifying its message, an error may also
 * provide a collections of quick fixes that solve this problem.
 * 
 * @author Simon Fischer
 * 
 */
public interface ProcessSetupError {

	/** Severity levels of ProcessSetupErrors. */
	public enum Severity {
		/**
		 * This indicates that the corresponding message is just for information
		 */
		INFORMATION,
		/**
		 * This is an indicator of wrong experiment setup, but the process may run nevertheless.
		 */
		WARNING,
		/** Process will definitely (well, say, most certainly) not run. */
		ERROR
	}

	/** Returns the human readable, formatted message. */
	public String getMessage();

	/**
	 * Returns the owner of the port that should be displayed by the GUI to fix the error.
	 */
	public PortOwner getOwner();

	/** If possible, return a list of fixes for the error. */
	public List<? extends QuickFix> getQuickFixes();

	/** Returns the severity of the error. */
	public Severity getSeverity();
}
