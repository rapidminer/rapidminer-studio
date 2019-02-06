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
package com.rapidminer.operator.ports;

import java.util.Collections;
import java.util.List;

import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 * Is thrown if {@link Port#getMetaData(Class)} is called and the available meta data is not
 * compatible with the provided desired class.
 * 
 * @author Nils Woehler
 * 
 */
public class IncompatibleMDClassException extends Exception implements MetaDataError {

	private static final long serialVersionUID = 1L;
	private final PortOwner owner;
	private final Port port;

	public IncompatibleMDClassException(PortOwner owner, Port port) {
		this.owner = owner;
		this.port = port;
	}

	@Override
	public PortOwner getOwner() {
		return owner;
	}

	@Override
	public List<? extends QuickFix> getQuickFixes() {
		return Collections.emptyList();
	}

	@Override
	public Severity getSeverity() {
		return Severity.ERROR;
	}

	@Override
	public Port getPort() {
		return port;
	}

}
