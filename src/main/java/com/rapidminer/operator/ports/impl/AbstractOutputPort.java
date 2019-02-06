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
package com.rapidminer.operator.ports.impl;

import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.MetaData;


/**
 * An abstract output port implementation. Only the
 * {@link #deliver(com.rapidminer.operator.IOObject)} method is missing.
 *
 * @author Nils Woehler
 *
 */
public abstract class AbstractOutputPort extends AbstractPort implements OutputPort {

	private InputPort connectedTo;

	private MetaData metaData;

	private MetaData realMetaData;

	protected AbstractOutputPort(Ports<? extends Port> owner, String name, boolean simulatesStack) {
		super(owner, name, simulatesStack);
	}

	@Override
	public void deliverMD(MetaData md) {
		this.metaData = md;
		if (connectedTo != null) {
			this.connectedTo.receiveMD(md);
		}
	}

	@Override
	public MetaData getMetaData() {
		if (realMetaData != null) {
			return realMetaData;
		} else {
			return metaData;
		}
	}

	@Override
	public InputPort getDestination() {
		return connectedTo;
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public boolean isConnected() {
		return connectedTo != null;
	}

	@Override
	public boolean shouldAutoConnect() {
		return getPorts().getOwner().getOperator().shouldAutoConnect(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends MetaData> T getMetaData(Class<T> desiredClass) throws IncompatibleMDClassException {
		if (realMetaData != null) {
			checkDesiredClass(realMetaData, desiredClass);
			return (T) realMetaData;
		} else {
			if (metaData != null) {
				checkDesiredClass(metaData, desiredClass);
			}
			return (T) metaData;
		}
	}

	@Override
	public void clear(int clearFlags) {
		super.clear(clearFlags);
		if ((clearFlags & CLEAR_REAL_METADATA) > 0) {
			realMetaData = null;
		}
		if ((clearFlags & CLEAR_METADATA) > 0) {
			metaData = null;
		}
	}

	protected void assertConnected() throws PortException {
		if (this.connectedTo == null) {
			throw new PortException(this, "Not connected.");
		}
	}

	/*
	 * private void assertDisconnected() throws PortException { if (this.connectedTo != null) {
	 * throw new PortException(this, "Already connected."); } }
	 */

	@Override
	public void connectTo(InputPort inputPort) throws PortException {
		if (this.connectedTo == inputPort) {
			return;
		}
		if (inputPort.getPorts().getOwner().getConnectionContext() != this.getPorts().getOwner().getConnectionContext()) {
			throw new PortException("Cannot connect " + getSpec() + " to " + inputPort.getSpec()
					+ ": ports must be in the same subprocess, but are in "
					+ this.getPorts().getOwner().getConnectionContext().getName() + " and "
					+ inputPort.getPorts().getOwner().getConnectionContext().getName() + ".");
		}
		boolean destConnected = inputPort.isConnected();
		boolean sourceConnected = this.isConnected();
		boolean bothConnected = destConnected && sourceConnected;
		boolean connected = destConnected || sourceConnected;
		if (connected) {
			if (bothConnected) {
				throw new CannotConnectPortException(this, inputPort, this.getDestination(), inputPort.getSource());
			} else if (sourceConnected) {
				throw new CannotConnectPortException(this, inputPort, this.getDestination());
			} else {
				throw new CannotConnectPortException(this, inputPort, inputPort.getSource());
			}
		}
		this.connectedTo = inputPort;
		((AbstractInputPort) inputPort).connect(this);
		fireUpdate(this);
	}

	@Override
	public void disconnect() throws PortException {
		assertConnected();
		((AbstractInputPort) this.connectedTo).connect(null);
		this.connectedTo.receive(null);
		this.connectedTo.receiveMD(null);
		this.connectedTo = null;
		fireUpdate(this);
	}

	/**
	 * @return the realMetaData
	 */
	protected MetaData getRealMetaData() {
		return this.realMetaData;
	}

	/**
	 * @param realMetaData
	 *            the realMetaData to set
	 */
	protected void setRealMetaData(MetaData realMetaData) {
		this.realMetaData = realMetaData;
	}

}
