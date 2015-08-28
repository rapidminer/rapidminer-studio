/**
 * Copyright (c) 2007-2014 The LIBLINEAR Project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither name of copyright holders nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.bwaldvogel.liblinear;

import java.io.File;


public class InvalidInputDataException extends Exception {

	private static final long serialVersionUID = 2945131732407207308L;

	private final int _line;

	private File _file;

	public InvalidInputDataException(String message, File file, int line) {
		super(message);
		_file = file;
		_line = line;
	}

	public InvalidInputDataException(String message, String filename, int line) {
		this(message, new File(filename), line);
	}

	public InvalidInputDataException(String message, File file, int lineNr, Exception cause) {
		super(message, cause);
		_file = file;
		_line = lineNr;
	}

	public InvalidInputDataException(String message, String filename, int lineNr, Exception cause) {
		this(message, new File(filename), lineNr, cause);
	}

	public File getFile() {
		return _file;
	}

	/**
	 * This methods returns the path of the file. The method name might be misleading.
	 *
	 * @deprecated use {@link #getFile()} instead
	 */
	@Deprecated
	public String getFilename() {
		return _file.getPath();
	}

	public int getLine() {
		return _line;
	}

	@Override
	public String toString() {
		return super.toString() + " (" + _file + ":" + _line + ")";
	}

}
