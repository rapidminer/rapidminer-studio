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
package com.rapidminer.operator.nio.file.compression;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.tools.Tools;


/**
 * @author Marius Helf
 *
 */
public class ZipFileObject extends ArchiveFileObject {

	/**
	 * The data stream which compresses all data which is written to it.
	 *
	 * The compressed data is then piped to the {@link #archiveDataStream}.
	 */
	private ZipOutputStream zipOutputStream;
	private boolean finished = false;

	public ZipFileObject() throws OperatorException {
		super();
		zipOutputStream = new ZipOutputStream(getArchiveDataStream());
	}

	public ZipFileObject(BufferType bufferType) throws OperatorException {
		super(bufferType);
		zipOutputStream = new ZipOutputStream(getArchiveDataStream());
	}

	/**
	 * Flushes all output buffers.
	 */
	@Override
	protected void flush() throws IOException {
		zipOutputStream.flush();
		zipOutputStream.finish();
		finished = true;
		super.flush();
	}

	private int getZipCompressionLevel(FuzzyCompressionLevel compressionLevel) {
		switch (compressionLevel) {
			case BEST:
				return Deflater.BEST_COMPRESSION;
			case FASTEST:
				return Deflater.BEST_SPEED;
			case NONE:
				return Deflater.NO_COMPRESSION;
			case DEFAULT:
			default:
				return Deflater.DEFAULT_COMPRESSION;
		}
	}

	/**
	 * @param fileObject
	 * @param directory
	 * @throws IOException
	 * @throws OperatorException
	 */
	@Override
	public void addEntry(FileObject fileObject, String directory, FuzzyCompressionLevel localCompressionLevel)
			throws OperatorException {
		addEntry(fileObject, directory, getZipCompressionLevel(localCompressionLevel));
	}

	private void addEntry(FileObject fileObject, String directory, int zipCompressionLevel) throws OperatorException {
		if (finished) {
			throw new UserError(null, "zip_file_object.cannot_add_entry_after_finish");
		}

		if (directory == null) {
			directory = "";
		}
		// remove trailing slashes
		directory = directory.replaceAll("\\\\", "/");
		directory = directory.replaceAll("[\\\\|/]+$", "");

		String filename = fileObject.getFilename();
		if (filename == null) {
			throw new UserError(null, "archive_file.undefined_filename");
		}

		if (!directory.isEmpty()) {
			filename = directory + "/" + filename;
		}

		zipOutputStream.setLevel(zipCompressionLevel);

		InputStream fileStream = fileObject.openStream();
		try {
			try {
				zipOutputStream.putNextEntry(new ZipEntry(filename));
				Tools.copyStreamSynchronously(fileStream, zipOutputStream, false);
				zipOutputStream.closeEntry();
			} catch (ZipException e) {
				throw new UserError(null, "zip_file_object.zip_error_while_adding", e.getLocalizedMessage());
			} catch (IOException e) {
				throw new OperatorException("archive_file.stream_error", e, new Object[0]);
			}
		} finally {
			try {
				fileStream.close();
			} catch (IOException e) {
				throw new OperatorException("zipfile.stream_error", e, new Object[0]);
			}
		}
	}

	@Override
	public String getName() {
		return "Zip File";
	}

	/**
	 * Returns the length of the Zip file. If the file does not exist, a temporary file is created
	 * via a call of {@link #getFile()}.
	 */
	@Override
	public long getLength() throws OperatorException {
		return getFile().length();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.rapidminer.operator.nio.file.compression.ArchiveFileObject#getSupportedCompressionLevels
	 * ()
	 */
	@Override
	public Set<FuzzyCompressionLevel> getSupportedCompressionLevels() {
		HashSet<FuzzyCompressionLevel> supportedLevels = new HashSet<>();
		supportedLevels.add(FuzzyCompressionLevel.BEST);
		supportedLevels.add(FuzzyCompressionLevel.FASTEST);
		supportedLevels.add(FuzzyCompressionLevel.NONE);
		supportedLevels.add(FuzzyCompressionLevel.DEFAULT);
		return supportedLevels;
	}

	public boolean supportsComppressionLevel(FuzzyCompressionLevel compressionLevel) {
		return getSupportedCompressionLevels().contains(compressionLevel);
	}
}
