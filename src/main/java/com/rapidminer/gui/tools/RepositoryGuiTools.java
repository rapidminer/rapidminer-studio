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
package com.rapidminer.gui.tools;

import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryLocationChooser;


/**
 * A class that presents helper methods for interacting with the repository.
 *
 * @author Sebastian Land
 */
public class RepositoryGuiTools {

	/**
	 * This method will return an {@link Entry} if the given location is a valid repository
	 * location. If the entry already exists, the user will be asked if it can be overwritten. Null
	 * is returned if the user denies to overwrite the existing file.
	 */
	public static BlobEntry createBlobEntry(String loc) {
		try {
			RepositoryLocation location = new RepositoryLocation(loc);
			Entry entry = location.locateEntry();
			if (entry != null) {
				if (entry instanceof BlobEntry) {
					if (SwingTools.showConfirmDialog("overwrite_entry_different_type", ConfirmDialog.YES_NO_OPTION,
							entry.getLocation()) == ConfirmDialog.YES_OPTION) {
						return (BlobEntry) entry;
					}
				} else {
					if (SwingTools.showConfirmDialog("overwrite_entry", ConfirmDialog.YES_NO_OPTION,
							entry.getLocation()) == ConfirmDialog.YES_OPTION) {
						Folder folder = entry.getContainingFolder();
						String name = entry.getName();
						entry.delete();
						return folder.createBlobEntry(name);
					}
				}
			}
			return RepositoryManager.getInstance(null).getOrCreateBlob(location);
		} catch (MalformedRepositoryLocationException e) {
			SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
		}
		return null;
	}

	/**
	 * This method will show an Repository Entry chooser and returns the entry if the user has
	 * chosen an unused entry or confirmed to overwrite the existing. Otherwise null is returned.
	 */
	public static BlobEntry selectBlobEntryForStoring() {
		String selectEntry = RepositoryLocationChooser.selectEntry(null, null, true);
		return createBlobEntry(selectEntry);
	}

	/**
	 * This method will show an Repository Entry chooser and lets the user choose an entry. This
	 * entry must be of the type {@link BlobEntry}, otherwise an error message is shown. If the user
	 * aborts the selection, null is returned.
	 *
	 * TODO: Needs to add filter mechanism to {@link RepositoryLocationChooser} to restrict shown
	 * types to {@link BlobEntry}s.
	 */
	public static BlobEntry selectBlobEntryForLoading(String mimeType) {
		String locationString = RepositoryLocationChooser.selectEntry(null, null);
		if (locationString != null) {
			RepositoryLocation location;
			try {
				location = new RepositoryLocation(locationString);
				Entry entry = location.locateEntry();
				if (entry instanceof BlobEntry) {
					BlobEntry blobEntry = (BlobEntry) entry;
					if (mimeType.equals(blobEntry.getMimeType())) {
						return blobEntry;
					} else {
						SwingTools.showSimpleErrorMessage("entry_must_be_blob", blobEntry.getName());
						return selectBlobEntryForLoading(mimeType);
					}
				} else {
					SwingTools.showSimpleErrorMessage("entry_must_be_blob", entry.getName());
					return selectBlobEntryForLoading(mimeType);
				}
			} catch (MalformedRepositoryLocationException e) {
				SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
			} catch (RepositoryException e) {
				SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
			}
		}
		return null;
	}

	/**
	 * This method will check if the given location is either empty or is an BlobEntry of the given
	 * mimeType. If neither is the case, null will be returned. Otherwise the BlobEntry denoting
	 * this location will be returned.
	 */
	public static BlobEntry isBlobEntryForStoring(String repositoryLocation, String mimeType) {
		RepositoryLocation location;
		try {
			location = new RepositoryLocation(repositoryLocation);
			Entry entry = location.locateEntry();
			if (entry instanceof BlobEntry) {
				BlobEntry blobEntry = (BlobEntry) entry;
				if (mimeType.equals(blobEntry.getMimeType())) {
					return blobEntry;
				} else {
					SwingTools.showSimpleErrorMessage("entry_must_be_blob", blobEntry.getName());
					return null;
				}
			} else if (entry == null) {
				return createBlobEntry(repositoryLocation);
			} else {
				SwingTools.showSimpleErrorMessage("entry_must_be_blob", entry.getName());
			}
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
		} catch (MalformedRepositoryLocationException e) {
			SwingTools.showSimpleErrorMessage("cannot_access_repository", e);
		}
		return null;
	}

	/**
	 * This method determinate if the given pChild is a successor of pParent. It compares the paths
	 * of botch by splitting them at every {@link RepositoryLocation.SEPARATOR} into chunks and
	 * match these against each other.
	 *
	 * @param pParent
	 *            The absolute path {@link RepositoryLocation} of the parent as {@link String}
	 * @param pChild
	 *            The absolute path {@link RepositoryLocation} of the child as {@link String}
	 * @return {@code true} iff pChild is a successor of pParent
	 **/
	public static boolean isSuccessor(String pParent, String pChild) {
		// Remove the prefix of the repository to avoid unnecessary chunks and then split the path
		// by the separator
		String[] parentStringChunks = pParent.replaceAll(String.valueOf(RepositoryLocation.REPOSITORY_PREFIX), "")
				.split(String.valueOf(RepositoryLocation.SEPARATOR));
		String[] childStringChunks = pChild.replaceAll(String.valueOf(RepositoryLocation.REPOSITORY_PREFIX), "")
				.split(String.valueOf(RepositoryLocation.SEPARATOR));
		// If the pParent path has a deeper depth than the pChild path than pParent can't be pChilds
		// parent.
		if (parentStringChunks.length > childStringChunks.length) {
			return false;
		}
		// Comparing the chunks if they are not identical the child and parent are not in the same
		// branch which means they're not related.
		for (int i = 0; i < parentStringChunks.length; i++) {
			if (!parentStringChunks[i].equals(childStringChunks[i])) {
				return false;
			}
		}
		// If the pChild path has a deeper depth and all chunks of the pParent path are a subset of
		// the pChild path then pChild is a successor of pParent.
		return true;
	}
}
