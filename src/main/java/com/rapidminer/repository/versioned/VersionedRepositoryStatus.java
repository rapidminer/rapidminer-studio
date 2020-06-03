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

/**
 * Contains status information for a versioned repository. This information is static and does not query the Git server
 * itself, it is updated when a query is triggered by the user, though.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface VersionedRepositoryStatus {

	/**
	 * The name of the current branch.
	 *
	 * @return the name, never {@code null}
	 */
	String getCurrentBranch();

	/**
	 * If the status has been initialized.
	 *
	 * @return if {@code true}, at least one update has happened, if {@code false} we have not yet talked to the Git
	 * repo at all
	 */
	boolean isInitialized();

	/**
	 * If the status is initialized AND there has been no error getting the last state, the repository is treated as
	 * connected.
	 *
	 * @return {@code true} if {@link #isInitialized()} and if no error has happened for getting the most recent status
	 * update; {@code false} otherwise
	 */
	boolean isConnected();

	/**
	 * Find out if the remote origin exists. It may have been deleted.
	 *
	 * @return {@code true} if the currently set remote origin exists; {@code false} otherwise
	 */
	boolean isRemoteOriginExisting();

	/**
	 * Find out if the remote tracking branch exists. It may have been deleted for one reason or another remotely.
	 *
	 * @return {@code true} if the currently set remote tracking branch exists; {@code false} otherwise
	 */
	boolean isRemoteTrackingBranchExisting();

	/**
	 * This method covers a special case: When a versioned repository is created, it is assigned an encryption key. This
	 * key cannot change, as otherwise it would break decrypting items in the history as well as requiring migrations on
	 * each client. However, this state can still happen, albeit indirectly: If the versioned repository is deleted on
	 * RapidMiner AI Hub, and then re-created under the same name again (but with a different encryption key), the
	 * aforementioned problems would occur. Therefore, Studio checks whether the expected encryption context (see {@link
	 * com.rapidminer.tools.encryption.EncryptionProvider}) is the same as the current encryption context, based
	 * on the latest AI Hub information. If they differ, this method will indicate that.
	 * <p>
	 * <strong>Attention:</strong> Once this happens, ALL GIT INTERACTIONS ARE DISABLED FOREVER! Otherwise we would
	 * corrupt the history, have merge problems, etc. The only way out of this scenario is to connect with a new
	 * repository in Studio to the versioned repository. Then you can copy the files over from old to new via the Studio
	 * UI, and finally delete this repository.
	 * </p>
	 *
	 * @return {@code true} if everything is fine (expected and actual encryption context are identical); {@code false}
	 * if encryption context has changed between expected and actual
	 */
	boolean isEncryptionContextKnown();

	/**
	 * Shortcut for finding out if there are any remote or local changes.
	 *
	 * @return {@code true} if neither local nor remote changes exist and local and remote branch are identical; {@code
	 * false} otherwise
	 */
	boolean isUpToDate();

	/**
	 * Gets how many commits this local branch is behind the remote HEAD.
	 *
	 * @return the number of commits this branch is behind compared to the remote
	 */
	int getNumberOfCommitsBehind();

	/**
	 * Gets how many commits this local branch is ahead of the remote HEAD. This can for example happen if committing
	 * worked, but the push failed due to no connection or similar.
	 *
	 * @return the number of commits this branch is ahead compared to the remote
	 */
	int getNumberOfCommitsAhead();

	/**
	 * Gets how many local files have been changed compared to the latest state of the local HEAD. Includes added
	 * files, modified files, and removed files.
	 *
	 * @return the number of locally changed files that have not yet been committed + pushed to remote
	 */
	int getNumberOfLocalChanges();


	/**
	 * Register a {@link StatusListener} to get notified whenever the status changes.
	 *
	 * @param listener the listener, must not be {@code null}
	 */
	void addStatusListener(StatusListener listener);

	/**
	 * Removes the given {@link StatusListener}. If it was not registered, does nothing.
	 *
	 * @param listener the listener, must not be {@code null}
	 */
	void removeStatusListener(StatusListener listener);
}
