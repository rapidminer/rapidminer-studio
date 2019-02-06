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
 * Marker exception to indicate a (user) disconnect from repository action was triggered while a
 * {@link com.rapidminer.repository.internal.remote.RemoteRepository RemoteRepository} was still in progress of loading.
 *
 * @author Jan Czogalla
 * @since 8.2.1
 */
public class DisconnectedWhileLoadingRepositoryException extends RepositoryException {

	public DisconnectedWhileLoadingRepositoryException() {
		super("Disconnected while still connecting");
	}
}
