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
package com.rapidminer.storage.hdf5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;


/**
 * Tests the exceptions that happen when giving a broken file to the  {@link Hdf5ExampleSetReader}.
 *
 * @author Gisa Meier
 */
public class ReadExceptionTests {

	private static final String THIS_FOLDER = "src/test/java/com/rapidminer/storage/hdf5/testfiles";

	@Test(expected = HdfReaderException.class)
	public void readAttributeMissing() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER,"no-columns.hdf5"));
		} catch (HdfReaderException e){
			assertEquals(HdfReaderException.Reason.MISSING_ATTRIBUTE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readIllegalType() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get( THIS_FOLDER, "string-columns.hdf5"));
		} catch (HdfReaderException e){
			assertEquals(HdfReaderException.Reason.ILLEGAL_TYPE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readNegativeColumns() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get( THIS_FOLDER, "negative-columns.hdf5"));
		} catch (HdfReaderException e){
			assertEquals(HdfReaderException.Reason.INCONSISTENT_FILE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readTooManyColumns() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get( THIS_FOLDER, "too-many-columns.hdf5"));
		} catch (HdfReaderException e){
			assertEquals(HdfReaderException.Reason.INCONSISTENT_FILE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readTooManyRows() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get( THIS_FOLDER, "too-many-rows.hdf5"));
		} catch (HdfReaderException e){
			assertEquals(HdfReaderException.Reason.INCONSISTENT_FILE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readNoDictionary() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "no-dictionary.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.INCONSISTENT_FILE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readWrongType() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "wrong-type.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.ILLEGAL_TYPE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readNoType() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "no-type.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.MISSING_ATTRIBUTE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readWrongTypeType() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "wrong-type-type.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.ILLEGAL_TYPE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readDoubleValueDictionary() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "double-value-dictionary.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.DICTIONARY_NOT_UNIQUE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readChunkedDictionary() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "chunked-dictionary.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.NON_CONTIGUOUS, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readNonStringDictionary() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "non-string-dictionary.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.UNSUPPORTED_TYPE, e.getReason());
			throw e;
		}
	}

	@Test(expected = HdfReaderException.class)
	public void readWrongMissingType() throws IOException {
		try {
			Hdf5ExampleSetReader.read(Paths.get(THIS_FOLDER, "wrong-type-datetime.hdf5"));
		} catch (HdfReaderException e) {
			assertEquals(HdfReaderException.Reason.UNSUPPORTED_TYPE, e.getReason());
			throw e;
		}
	}

	@Test
	public void noMetaDataFromPython() throws IOException {
		assertNull(Hdf5ExampleSetReader.readMetaData(Paths.get(THIS_FOLDER, "not-from-studio.hdf5")));
	}
}
