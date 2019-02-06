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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.logging.Level;

import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;


/**
 * An abstract implementation for InputPorts. It handles meta data and meta data changes changes,
 * preconditions and the connection to the source output port.
 *
 * @author Nils Woehler
 *
 */
public abstract class AbstractInputPort extends AbstractPort implements InputPort {

	private final Collection<MetaDataChangeListener> metaDataChangeListeners = new LinkedList<>();

	private final Collection<Precondition> preconditions = new LinkedList<>();

	private MetaData metaData;

	private MetaData realMetaData;

	/** The port to which this port is connected. */
	private OutputPort sourceOutputPort;

	protected AbstractInputPort(Ports<? extends Port> owner, String name, boolean simulatesStack) {
		super(owner, name, simulatesStack);
	}

	@Override
	public void clear(int clearFlags) {
		super.clear(clearFlags);
		if ((clearFlags & CLEAR_REAL_METADATA) > 0) {
			realMetaData = null;
			informListenersOfChange(null);
		}
		if ((clearFlags & CLEAR_METADATA) > 0) {
			this.metaData = null;
			informListenersOfChange(null);
		}
	}

	@Override
	public void receiveMD(MetaData metaData) {
		this.metaData = metaData;
		informListenersOfChange(metaData);
	}

	@Override
	public MetaData getMetaData() {
		if (realMetaData != null) {
			return realMetaData;
		} else {
			return metaData;
		}
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

	public void connect(OutputPort outputPort) {
		this.sourceOutputPort = outputPort;
		fireUpdate(this);
	}

	@Override
	public OutputPort getSource() {
		return sourceOutputPort;
	}

	@Override
	public void checkPreconditions() {
		MetaData metaData = getMetaData();
		for (Precondition precondition : preconditions) {
			try {
				precondition.check(metaData);
			} catch (Exception e) {
				getPorts().getOwner().getOperator().getLogger()
						.log(Level.WARNING, "Error checking preconditions at " + getSpec() + ": " + e, e);
				this.addError(new SimpleMetaDataError(Severity.WARNING, this, "exception_checking_precondition", e
						.toString()));
			}
		}
	}

	@Override
	public String getPreconditionDescription() {
		StringBuilder buf = new StringBuilder();
		buf.append(getName());
		buf.append(": ");
		for (Precondition precondition : preconditions) {
			buf.append(precondition.getDescription());
		}
		return buf.toString();
	}

	@Override
	public boolean isInputCompatible(MetaData input, CompatibilityLevel level) {
		for (Precondition precondition : preconditions) {
			if (!precondition.isCompatible(input, level)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getDescription() {
		StringBuilder b = new StringBuilder();
		boolean first = true;
		for (Precondition precondition : preconditions) {
			if (!first) {
				b.append(", ");
			} else {
				first = false;
			}
			b.append(precondition.getDescription());
		}
		return b.toString();
	}

	@Override
	public boolean isConnected() {
		return sourceOutputPort != null;
	}

	@Override
	public void addPrecondition(Precondition precondition) {
		preconditions.add(precondition);

	}

	@Override
	public Collection<Precondition> getAllPreconditions() {
		return Collections.unmodifiableCollection(preconditions);
	}

	@Override
	public synchronized void registerMetaDataChangeListener(MetaDataChangeListener listener) {
		metaDataChangeListeners.add(listener);
	}

	@Override
	public synchronized void removeMetaDataChangeListener(MetaDataChangeListener listener) {
		metaDataChangeListeners.remove(listener);
	}

	protected synchronized void informListenersOfChange(MetaData metaData) {
		for (MetaDataChangeListener listener : metaDataChangeListeners) {
			listener.informMetaDataChanged(metaData);
		}
	}

	public MetaData getRealMetaData() {
		return realMetaData;
	}

	public void setRealMetaData(MetaData realMetaData) {
		this.realMetaData = realMetaData;
	}

}
