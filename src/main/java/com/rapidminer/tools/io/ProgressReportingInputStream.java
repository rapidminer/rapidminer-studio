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
package com.rapidminer.tools.io;

import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;

import java.io.IOException;
import java.io.InputStream;


/**
 * Stream reporting the amount of read bytes to ProgressListener
 * 
 * @author Simon Fischer
 * */
public class ProgressReportingInputStream extends InputStream {

	private InputStream parent;

	private ProgressListener listener;
	private int showMin;
	private int showMax;
	private long streamLength;

	private long readBytes = 0;
	private int lastReportedValue = Integer.MIN_VALUE;

	/**
	 * @deprecated Use
	 *             {@link #ProgressReportingInputStream(InputStream, ProgressListener, int, int, long)}
	 *             instead
	 */
	@Deprecated
	public ProgressReportingInputStream(InputStream in, ProgressListener listener, int minProgress, int maxProgress,
			int streamLength) {
		this(in, listener, minProgress, maxProgress, (long) streamLength);
	}

	public ProgressReportingInputStream(InputStream in, ProgressListener listener, int minProgress, int maxProgress,
			long streamLength) {
		this.parent = in;
		this.listener = listener;
		this.showMin = minProgress;
		this.showMax = maxProgress;
		this.streamLength = streamLength;
	}

	@Override
	public int read() throws IOException {
		int b = parent.read();
		report(1);
		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = parent.read(b, off, len);
		report(result);
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int result = parent.read(b);
		report(result);
		return result;
	}

	private void report(int increment) {
		if (increment > 0) {
			readBytes += increment;
			int completed = (int) (showMin + (((long) showMax - (long) showMin) * readBytes) / streamLength);
			if (completed != lastReportedValue) {
				if (listener != null) {
					listener.setCompleted(completed);
					listener.setMessage(Tools.formatBytes(readBytes) + "/" + Tools.formatBytes(streamLength));
				}
				lastReportedValue = completed;
			}
		}
	}

}
