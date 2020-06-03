/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.tools.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.ValidationUtil;

/**
 * A wrapper {@link OutputStream} that takes a {@link Runnable} to execute on close.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class NotifyingOutputStreamWrapper extends OutputStream {

	private final OutputStream stream;
	private final Runnable onClose;
	private final boolean onEDT;

	private final AtomicBoolean isClosed = new AtomicBoolean();

	/**
	 * Same as {@link #NotifyingOutputStreamWrapper(OutputStream, Runnable, boolean)
	 * NotifyingOutputStreamWrapper(stream, onClose, false)}
	 */
	public NotifyingOutputStreamWrapper(OutputStream stream, Runnable onClose) {
		this(stream, onClose, false);
	}

	/**
	 * Creates a wrapper based on the given {@link OutputStream} that will execute the provided Runnable on {@link #close()}.
	 * The parameter {@code onEDT} indicates if the runnable needs to be executed on the EDT.
	 *
	 * @param stream
	 * 		the stream to wrap, must not be {@code null}
	 * @param onClose
	 * 		the runnable to execute, must not be {@code null}
	 * @param onEDT
	 * 		whether it is necessary to execute the runnable on the EDT
	 */
	public NotifyingOutputStreamWrapper(OutputStream stream, Runnable onClose, boolean onEDT) {
		this.stream = ValidationUtil.requireNonNull(stream, "stream");
		this.onClose = ValidationUtil.requireNonNull(onClose, "onClose");
		this.onEDT = onEDT;
	}

	@Override
	public void write(int b) throws IOException {
		stream.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		stream.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		stream.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		stream.flush();
	}

	/**
	 * Closes the wrapped {@link #stream} and executes the runnable {@link #onClose} in a finally block and on the EDT
	 * if so indicated by {@link #onEDT}.
	 * <p>
	 * Successive calls of this method have no effect
	 */
	@Override
	public void close() throws IOException {
		if (!isClosed.compareAndSet(false, true)) {
			return;
		}
		try {
			stream.close();
		} finally {
			if (onEDT) {
				SwingTools.invokeLater(onClose);
			} else {
				onClose.run();
			}
		}
	}
}
