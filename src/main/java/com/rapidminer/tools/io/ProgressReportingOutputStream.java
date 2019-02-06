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
import java.io.OutputStream;


/**
 * Stream reporting the amount of written bytes to ProgressListener
 * 
 * @author Simon Fischer
 * */
public class ProgressReportingOutputStream extends OutputStream {

	private OutputStream parent;

	private ProgressListener listener;
	private int showMin;
	private int showMax;
	private int streamLength;

	private int readBytes = 0;
	private int lastReportedValue = Integer.MIN_VALUE;

	public ProgressReportingOutputStream(OutputStream out, ProgressListener listener, int minProgress, int maxProgress,
			int streamLength) {
		this.parent = out;
		this.listener = listener;
		this.showMin = minProgress;
		this.showMax = maxProgress;
		this.streamLength = streamLength;
	}

	private void report(int increment) {
		if (increment > 0) {
			readBytes += increment;
			int completed = showMin + (showMax - showMin) * readBytes / streamLength;
			if (completed != lastReportedValue) {
				listener.setCompleted(completed);
				listener.setMessage(Tools.formatBytes(readBytes) + "/" + Tools.formatBytes(streamLength));
				lastReportedValue = completed;
			}
		}
	}

	@Override
	public void write(int b) throws IOException {
		parent.write(b);
		report(1);
	}

	@Override
	public void write(byte[] b) throws IOException {
		int chunkSize = 4096;
		for (int offset = 0; offset < b.length; offset += chunkSize) {
			int chunkLength = Math.min(chunkSize, b.length - offset);
			write(b, offset, chunkLength);
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		parent.write(b, off, len);
		report(len);
	}

	@Override
	public void flush() throws IOException {
		parent.flush();
	}

	@Override
	public void close() throws IOException {
		parent.close();
	}
}
