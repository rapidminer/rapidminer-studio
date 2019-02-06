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
package com.rapidminer.gui.tools.logging;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.rapidminer.gui.tools.ScaledImageIcon;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.I18N;


/**
 * The abstract model for a log in the {@link LogViewer}.
 * <p>
 * <strong>Note: </strong>Do not extend this directly, but rather extend
 * {@link AbstractPushLogModel} and {@link AbstractPullLogModel}.
 * </p>
 *
 * @author Sabrina Kirstein, Marco Boeck, Marcel Michel
 */
public abstract class AbstractLogModel extends AbstractObservable<List<LogEntry>> implements LogModel {

	private static final Object LOCK = new Object();

	/**
	 * This code is in Public Domain. Please retain the author annotation.
	 *
	 * @author Isak du Preez (isak at du-preez dot com, www.du-preez.com)
	 */
	private class CircularArrayList<E> extends AbstractList<E> implements RandomAccess {

		private final int n; // buffer length
		private final List<E> buf; // a List implementing RandomAccess
		private int head = 0;
		private int tail = 0;

		public CircularArrayList(int capacity) {
			n = capacity + 1;
			buf = new ArrayList<E>(Collections.nCopies(n, (E) null));
		}

		@SuppressWarnings("unused")
		public int capacity() {
			return n - 1;
		}

		private int wrapIndex(int i) {
			int m = i % n;
			if (m < 0) { // java modulus can be negative
				m += n;
			}
			return m;
		}

		// This method is O(n) but will never be called if the
		// CircularArrayList is used in its typical/intended role.
		private void shiftBlock(int startIndex, int endIndex) {
			assert endIndex > startIndex;
			for (int i = endIndex - 1; i >= startIndex; i--) {
				set(i + 1, get(i));
			}
		}

		@Override
		public int size() {
			return tail - head + (tail < head ? n : 0);
		}

		@Override
		public E get(int i) {
			if (i < 0 || i >= size()) {
				throw new IndexOutOfBoundsException();
			}
			return buf.get(wrapIndex(head + i));
		}

		@Override
		public E set(int i, E e) {
			if (i < 0 || i >= size()) {
				throw new IndexOutOfBoundsException();
			}
			return buf.set(wrapIndex(head + i), e);
		}

		@Override
		public void add(int i, E e) {
			int s = size();
			if (s == n - 1) {
				throw new IllegalStateException("Cannot add element." + " CircularArrayList is filled to capacity.");
			}
			if (i < 0 || i > s) {
				throw new IndexOutOfBoundsException();
			}
			tail = wrapIndex(tail + 1);
			if (i < s) {
				shiftBlock(i, s);
			}
			set(i, e);
		}

		@Override
		public E remove(int i) {
			int s = size();
			if (i < 0 || i >= s) {
				throw new IndexOutOfBoundsException();
			}
			E e = get(i);
			if (i > 0) {
				shiftBlock(0, i);
			}
			head = wrapIndex(head + 1);
			return e;
		}
	}

	/** the min and max size of an icon (if set) */
	private static final int ICON_SIZE = 16;

	/**
	 * the max size of log entries, if this size is exceeded the first elements will be dismissed
	 */
	public static final int DEFAULT_MAX_LOG_ENTRIES = 100_000;

	/** icon which is used if a log specifies no own icon */
	private static final ImageIcon FALLBACK_ICON = SwingTools
			.createIcon("16/" + I18N.getGUIMessage("gui.logging.fallback.icon"));

	/** log list */
	private List<LogEntry> logs;

	/** Icon */
	private Icon modelIcon;

	/** Unique Name */
	private String modelName;

	/** Log Mode */
	private LogMode logMode;

	/** is closable ? */
	private boolean isClosable;

	/** Log Level of the model */
	private Level logLevel;

	/**
	 * Creates a new log model with max {@value #DEFAULT_MAX_LOG_ENTRIES} log entries. If the size
	 * is exceeded old entries will be overwritten.
	 *
	 * @param modelIcon
	 *            can be <code>null</code>. If not <code>null</code>, must be 16x16 pixel
	 * @param modelName
	 *            cannot be <code>null</code> or empty. Must not exceed
	 *            {@link LogModel#MAX_NAME_LENGTH} characters
	 * @param logMode
	 *            see {@link LogMode#PULL} and {@link LogMode#PUSH}
	 * @param isClosable
	 *            if <code>true</code>, the user can close the log via a button in the GUI
	 */
	public AbstractLogModel(Icon modelIcon, String modelName, LogMode logMode, boolean isClosable) {
		this(modelIcon, modelName, logMode, isClosable, DEFAULT_MAX_LOG_ENTRIES);
	}

	/**
	 * Creates a new log model with the defined size of log entries. If the size is exceeded old
	 * entries will be overwritten.
	 *
	 * @param modelIcon
	 *            can be <code>null</code>. If not <code>null</code>, must be 16x16 pixel
	 * @param modelName
	 *            cannot be <code>null</code> or empty. Must not exceed
	 *            {@link LogModel#MAX_NAME_LENGTH} characters
	 * @param logMode
	 *            see {@link LogMode#PULL} and {@link LogMode#PUSH}
	 * @param isClosable
	 *            if <code>true</code>, the user can close the log via a button in the GUI
	 * @param maxLogEntries
	 *            the maximum size of log entries
	 */
	public AbstractLogModel(Icon modelIcon, String modelName, LogMode logMode, boolean isClosable, int maxLogEntries) {
		if (modelName == null || "".equals(modelName.trim())) {
			throw new IllegalArgumentException("modelName must not be null or empty!");
		}
		if (logMode == null) {
			throw new IllegalArgumentException("logMode must not be null!");
		}
		// enforce max length of name to avoid ugly GUI
		if (modelName.length() > MAX_NAME_LENGTH) {
			throw new IllegalArgumentException("modelName must not exeeed " + MAX_NAME_LENGTH + " characters!");
		}
		// enforce correct icon size (if icon is set)
		if (modelIcon != null) {
			if (!(modelIcon instanceof ScaledImageIcon) && modelIcon.getIconHeight() != ICON_SIZE
					|| modelIcon.getIconWidth() != ICON_SIZE) {
				throw new IllegalArgumentException("if modelIcon is not null it must be 16x16 pixel!");
			}
			this.modelIcon = modelIcon;
		} else {
			this.modelIcon = FALLBACK_ICON;
		}

		// this is important as we read the entries in reverse order in the LogViewer
		// with a LinkedList the performance would become abysmal for huge logs
		// where ArrayList takes constant time
		logs = new CircularArrayList<>(maxLogEntries);
		this.modelName = modelName;
		this.logMode = logMode;
		this.isClosable = isClosable;
		this.logLevel = Level.INFO;
	}

	@Override
	public void addLogEntries(List<LogEntry> logEntries) {
		if (logEntries != null) {
			synchronized (LOCK) {
				int diff = logs.size() + logEntries.size() - DEFAULT_MAX_LOG_ENTRIES;
				while (diff > 0) {
					logs.remove(0);
					diff--;
				}
				logs.addAll(logEntries);
				fireUpdate(logEntries);
			}
		}
	}

	@Override
	public void clearLog() {
		synchronized (LOCK) {
			logs.clear();
			fireUpdate();
		}
	}

	@Override
	public List<LogEntry> getLogEntries() {
		return Collections.unmodifiableList(logs);
	}

	@Override
	public Icon getIcon() {
		return modelIcon;
	}

	@Override
	public String getName() {
		return modelName;
	}

	@Override
	public LogMode getLogMode() {
		return logMode;
	}

	@Override
	public Level getLogLevel() {
		return logLevel;
	}

	@Override
	public void setLogLevel(Level level) {
		this.logLevel = level;
	}

	@Override
	public boolean isClosable() {
		return isClosable;
	}

	@Override
	public boolean equals(Object anObject) {
		if (this == anObject) {
			return true;
		}

		if (anObject instanceof LogModel) {
			LogModel anotherModel = (LogModel) anObject;
			if (!getName().equals(anotherModel.getName())) {
				return false;
			}
			if (getLogEntries().size() != anotherModel.getLogEntries().size()) {
				return false;
			}
			if (getLogMode() != anotherModel.getLogMode()) {
				return false;
			}
			if (getLogLevel() == null && anotherModel.getLogLevel() != null
					|| getLogLevel() != null && anotherModel.getLogLevel() == null) {
				return false;
			}
			if (!getLogLevel().equals(anotherModel.getLogLevel())) {
				return false;
			}
			for (int i = 0; i < getLogEntries().size(); i++) {
				// size is identical as we have checked earlier
				if (!getLogEntries().get(i).equals(anotherModel.getLogEntries().get(i))) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getLogEntries().hashCode();
		result = prime * result + getName().hashCode();
		if (getLogLevel() != null) {
			result = prime * result + getLogLevel().hashCode();
		}
		result = prime * result + getLogMode().hashCode();
		return result;
	}

	@Override
	public String toString() {
		String logLevel = getLogLevel() != null ? getLogLevel().toString() : "INFO";
		return getName() + " - " + logLevel + " (" + getLogEntries().size() + " entries)";
	}
}
