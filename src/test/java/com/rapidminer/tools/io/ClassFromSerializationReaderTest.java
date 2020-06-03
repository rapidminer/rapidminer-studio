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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;


/**
 * Tests for {@link ClassFromSerializationReader}
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class ClassFromSerializationReaderTest {

	private static class MinimumSerializable implements Serializable {}

	@Test
	public void testNormalObject() throws IOException {
		Serializable serializable = new MinimumSerializable();
		File temp = File.createTempFile("serialize", ".object");
		temp.deleteOnExit();
		try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(temp))) {
			outputStream.writeObject(serializable);
		}
		try (FileInputStream inputStream = new FileInputStream(temp)) {
			Class<?> readClass = ClassFromSerializationReader.readClass(inputStream, Object.class);
			assertEquals(MinimumSerializable.class, readClass);
		}
		try (FileInputStream inputStream = new FileInputStream(temp)) {
			Class<?> readClass = ClassFromSerializationReader.readClass(inputStream, Serializable.class);
			assertEquals(MinimumSerializable.class, readClass);
		}
	}

	@Test
	public void testWrongSuperClass() throws IOException {
		Serializable serializable = new MinimumSerializable();
		File temp = File.createTempFile("serialize", ".object");
		temp.deleteOnExit();
		try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(temp))) {
			outputStream.writeObject(serializable);
		}
		try (FileInputStream inputStream = new FileInputStream(temp)) {
			Class<?> readClass = ClassFromSerializationReader.readClass(inputStream, String.class);
			assertNull(readClass);
		}
	}

	@Test
	public void testUnrelatedFile() throws IOException {
		Serializable serializable = new MinimumSerializable();
		File temp = File.createTempFile("test", ".txt");
		temp.deleteOnExit();
		Files.write(temp.toPath(), "This is a test".getBytes(StandardCharsets.UTF_8));
		try (FileInputStream inputStream = new FileInputStream(temp)) {
			Class<?> readClass = ClassFromSerializationReader.readClass(inputStream, Object.class);
			assertNull(readClass);
		}
	}
}