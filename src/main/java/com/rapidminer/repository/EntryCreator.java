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
package com.rapidminer.repository;

/**
 * A functional interface to streamline {@link DataEntry} creation in repositories
 *
 * @param <L>
 * 		the type of the location
 * @param <E>
 * 		the subtype of {@link DataEntry} that will be created
 * @param <F>
 * 		the subtype of {@link Folder} that the new entry will live in
 * @param <R>
 * 		the subtype of {@link Repository} that the new entry will live in
 * @author Jan Czogalla
 * @since 9.3
 */
@FunctionalInterface
public interface EntryCreator<L, E extends Entry, F extends Folder, R extends Repository> {

	/**
	 * Create a new entry with the given location information, parent folder and enclosing repository.
	 *
	 * @param location
	 * 		the location information needed for the new entry
	 * @param folder
	 * 		the containing folder for the new entry
	 * @param repository
	 * 		the repository the new entry will reside in
	 * @return the new entry, never {@code null}
	 * @throws RepositoryException
	 * 		if an error occurs
	 */
	E create(L location, F folder, R repository) throws RepositoryException;

	/** An empty creator, always returning {@code null}. */
	EntryCreator<?, ?, ?, ?> NULL_CREATOR = (l, f, r) -> null;

	/**
	 * Returns a creator that always creates a {@code null} {@link Entry}.
	 *
	 * <p>This example illustrates the type-safe way to obtain a {@code null} creator:
	 * <pre>
	 *     EntryCreator&lt;L, E, F, R&gt; nulLCreator = EntryCreator.nullCreator();
	 * </pre>
	 *
	 * @param <C>
	 * 		subclass of the {@link EntryCreator} to be returned
	 * @return a creator that always creates a {@code null} {@link Entry}.
	 * @see #NULL_CREATOR
	 */
	@SuppressWarnings("unchecked")
	static <C extends EntryCreator<?, ?, ? ,?>> C nullCreator() {
		return (C) NULL_CREATOR;
	}
}
