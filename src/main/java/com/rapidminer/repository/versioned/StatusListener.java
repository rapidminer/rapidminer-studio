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
package com.rapidminer.repository.versioned;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


/**
 * A listener to get notified about changes to the {@link VersionedRepositoryStatus}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface StatusListener {

	/**
	 * The versioning operation that took place.
	 */
	enum VersioningOperation {
		PULL,

		PUSH,

		REVERT_TO_COMMIT,

		RESET_HARD
	}

	/**
	 * Fired when something in the {@link com.rapidminer.git.GitStatus} changes. This listener is expected to return
	 * quickly. If processing takes longer than a few ms, spawn a new thread instead of blocking the caller.
	 *
	 * @param repo      the repo for which the status changed
	 * @param newStatus the current status after the change
	 */
	void statusChanged(NewVersionedRepository repo, VersionedRepositoryStatus newStatus);

	/**
	 * When a versioning operation on this repository was started. Note that multiple operations can be started, but the
	 * main logic for each operation will be gated in a {@link com.rapidminer.gui.tools.ProgressThread} so that only a
	 * single instance of an operation can run at the same time. This listener is expected to return immediately!
	 * <p>
	 * <strong>DO NOT CALL {@link Future#get()} OR ANY OTHER
	 * BLOCKING CALL OF THE FUTURE IN THIS THREAD, SPAWN A NEW THREAD TO WAIT FOR RESULTS!</strong>
	 * </p>
	 *
	 * @param repo      the repo for which the status changed
	 * @param operation the versioning operation that was started
	 * @param future    the future that gets notified if a versioning operation was successful or not. Will be {@code
	 *                  true} if the operation was successful; {@code false} if the operation could not be done due to
	 *                  preconditions not satisfied. Note that getting the value of the future will throw a {@link
	 *                  java.util.concurrent.CancellationException} if the operation was aborted by user or an {@link
	 *                  java.util.concurrent.ExecutionException} if the operation failed due to an error.
	 */
	void versioningOperationWasStarted(NewVersionedRepository repo, VersioningOperation operation, CompletableFuture<Boolean> future);
}
