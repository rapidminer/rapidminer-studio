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
package com.rapidminer.gui.tools;

import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This is the basic class for progress monitoring.
 * 
 * @author Santhosh Kumar, Ingo Mierswa
 */
public class ProgressMonitor {

	private int total;
	private int current = -1;
	private boolean indeterminate;
	private int waitingTime = 100;
	private String status;

	private Vector<ChangeListener> listeners = new Vector<ChangeListener>();
	private ChangeEvent changeEvent = new ChangeEvent(this);

	public ProgressMonitor(int total, boolean indeterminate, int waitingTime) {
		this.total = total;
		this.indeterminate = indeterminate;
		this.waitingTime = waitingTime;
	}

	public ProgressMonitor(int total, boolean indeterminate) {
		this.total = total;
		this.indeterminate = indeterminate;
	}

	public int getTotal() {
		return total;
	}

	public void start(String status) {
		if (current != -1) {
			throw new IllegalStateException("not started yet");
		}
		this.status = status;
		current = 0;
		fireChangeEvent();
	}

	public int getWaitingTime() {
		return waitingTime;
	}

	public int getCurrent() {
		return current;
	}

	public String getStatus() {
		return status;
	}

	public boolean isIndeterminate() {
		return indeterminate;
	}

	public void setCurrent(String status, int current) {
		if (current == -1) {
			throw new IllegalStateException("not started yet");
		}
		this.current = current;
		if (status != null) {
			this.status = status;
		}
		fireChangeEvent();
	}

	public synchronized void addChangeListener(ChangeListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeChangeListener(ChangeListener listener) {
		listeners.remove(listener);
	}

	@SuppressWarnings("unchecked")
	private void fireChangeEvent() {
		// copy necessary against concurrent modification exceptions
		Vector<ChangeListener> targets;
		synchronized (this) {
			targets = (Vector<ChangeListener>) listeners.clone();
		}
		for (ChangeListener listener : targets) {
			listener.stateChanged(changeEvent);
		}
	}
}
