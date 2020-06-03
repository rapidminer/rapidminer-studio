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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.rapidminer.tools.ValidationUtil;

/**
 * A {@link FileVisitor} that deletes regular files if a {@link Predicate} was given and
 * deletes empty directories bottom up.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class EmptyDirCleaner implements FileVisitor<Path> {

	private final Predicate<Path> deleteFile;

	/** Constructor to only clean empty directories */
	public EmptyDirCleaner() {
		this(p -> false);
	}

	/** Constructor to delete certain regular files and empty directories */
	public EmptyDirCleaner(Predicate<Path> deleteFile) {
		this.deleteFile = ValidationUtil.requireNonNull(deleteFile, "deleteFile predicate");
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		// noop
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (deleteFile.test(file)) {
			try {
				Files.delete(file);
			} catch (IOException e) {
				// ignore
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		// noop
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		// do nothing if an exception was thrown before
		if (exc != null) {
			return FileVisitResult.CONTINUE;
		}
		// do nothing if still files/subfolders are present
		try (Stream<Path> stream = Files.list(dir)) {
			if (stream.findFirst().isPresent()) {
				return FileVisitResult.CONTINUE;
			}
		}
		// try to delete dir
		try {
			Files.delete(dir);
		} catch (IOException e) {
			// ignore
		}
		return FileVisitResult.CONTINUE;
	}
}
