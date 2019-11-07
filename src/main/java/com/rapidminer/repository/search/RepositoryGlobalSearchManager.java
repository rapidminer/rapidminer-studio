/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.search;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.ConnectionListener;
import com.rapidminer.repository.ConnectionRepository;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.DateEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryListener;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryManagerListener;
import com.rapidminer.repository.internal.db.DBRepository;
import com.rapidminer.repository.internal.remote.RESTRepository;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.internal.remote.ResponseContainer;
import com.rapidminer.repository.internal.remote.exception.NotYetSupportedServiceException;
import com.rapidminer.repository.resource.ResourceRepository;
import com.rapidminer.search.AbstractGlobalSearchManager;
import com.rapidminer.search.GlobalSearchDefaultField;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;
import com.rapidminer.search.GlobalSearchResultBuilder;
import com.rapidminer.search.GlobalSearchUtilities;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.WebServiceTools;


/**
 * Manages repository Global Search.
 *
 * @author Marco Boeck
 * @since 8.1
 */
class RepositoryGlobalSearchManager extends AbstractGlobalSearchManager implements RepositoryListener, RepositoryManagerListener, ConnectionListener {

	private static final String API_REST_REMOTE_REPO_DETAILS = "api/rest/globalsearch/repo/details";
	private static final String API_REST_REMOTE_REPO_SUMMARY = "api/rest/globalsearch/repo/summary";

	private static final float FIELD_BOOST_CONNECTION_TAG = 0.5f;
	private static final float FIELD_BOOST_CONNECTION_TYPE_NAME = 0.25f;

	private static final Map<String, String> ADDITIONAL_FIELDS;
	private static final String FIELD_TYPE = "type";
	private static final String FIELD_PARENT = "parent";
	private static final String FIELD_LOCATION = "location";
	private static final String FIELD_MODIFIED = "modified";
	private static final String FIELD_USER = "user";
	private static final String FIELD_ATTRIBUTE = "attribute";
	private static final String FIELD_CONNECTION_NAME = "connection_name";
	private static final String FIELD_CONNECTION_TYPE = RepositoryGlobalSearch.FIELD_CONNECTION_TYPE;
	private static final String FIELD_CONNECTION_TYPE_NAME = "connection_type_name";
	private static final String FIELD_CONNECTION_TAGS = "connection_tags";

	static {
		ADDITIONAL_FIELDS = new HashMap<>();
		ADDITIONAL_FIELDS.put(FIELD_TYPE, "The type of the data, e.g. 'process' or 'data'");
		ADDITIONAL_FIELDS.put(FIELD_PARENT, "The name of the parent folder of the data");
		ADDITIONAL_FIELDS.put(FIELD_MODIFIED, "The timestamp of the last modification of the data, if available. Format: 'YYYY-MM-DD'");
		ADDITIONAL_FIELDS.put(FIELD_USER, "The user who last edited the data");
		ADDITIONAL_FIELDS.put(FIELD_ATTRIBUTE, "The attributes for ExampleSets and the training set attributes in case of Models");
		ADDITIONAL_FIELDS.put(FIELD_CONNECTION_NAME, "The name of a connection");
		ADDITIONAL_FIELDS.put(FIELD_CONNECTION_TYPE, "The type key of a connection");
		ADDITIONAL_FIELDS.put(FIELD_CONNECTION_TYPE_NAME, "The type name of a connection");
		ADDITIONAL_FIELDS.put(FIELD_CONNECTION_TAGS, "The tags of a connection");
	}


	protected RepositoryGlobalSearchManager() {
		super(RepositoryGlobalSearch.CATEGORY_ID, ADDITIONAL_FIELDS, new GlobalSearchDefaultField(FIELD_CONNECTION_TAGS, FIELD_BOOST_CONNECTION_TAG),
				new GlobalSearchDefaultField(FIELD_CONNECTION_TYPE_NAME, FIELD_BOOST_CONNECTION_TYPE_NAME));
	}

	@Override
	protected void init() {
		RepositoryManager.getInstance(null).addRepositoryManagerListener(this);
	}

	@Override
	protected List<Document> createInitialIndex(ProgressThread progressThread) {
		// the listener is triggered for each repository while Studio starts up, loading each one in a separate ProgressThread
		// so no need to perform initial indexing
		return Collections.emptyList();
	}

	@Override
	public void entryAdded(Entry newEntry, Folder parent) {
		if (newEntry instanceof Folder) {
			// can be ignored because all entries under the folder will also show up as entryAdded events
			return;
		}
		addDocumentToIndex(createDocument(createItem(newEntry, true)));
	}

	@Override
	public void entryChanged(Entry entry) {
		if (entry instanceof Folder) {
			// can be ignored. Renaming events are entryMoved events.
			return;
		}
		addDocumentToIndex(createDocument(createItem(entry, true)));
	}

	@Override
	public void entryRemoved(Entry removedEntry, Folder parent, int oldIndex) {
		if (removedEntry instanceof Folder) {
			deleteEntriesUnderLocationFromIndex(removedEntry.getLocation().getAbsoluteLocation());
		} else {
			removeDocumentFromIndex(createDocument(createItem(removedEntry, false)));
		}
	}

	@Override
	public void entryMoved(Entry newEntry, Folder formerParent, String formerName) {
		if (newEntry instanceof Folder) {
			String parentLocation;
			if (formerParent == null) {
				// a repository was renamed
				parentLocation = RepositoryLocation.REPOSITORY_PREFIX + formerName;
			} else {
				parentLocation = formerParent.getLocation().getAbsoluteLocation() + RepositoryLocation.SEPARATOR + formerName;
			}

			// delete all entries under the former parent folder
			deleteEntriesUnderLocationFromIndex(parentLocation);

			// add all entries under the new folder to the index again
			addEntriesUnderFolderToIndex((Folder) newEntry);
		} else {
			// delete old entry
			removeDocumentFromIndex(createDocumentForDeletion(formerParent.getLocation().getAbsoluteLocation() + RepositoryLocation.SEPARATOR + formerName, formerName));

			// add new entry
			addDocumentToIndex(createDocument(createItem(newEntry, true)));
		}
	}

	@Override
	public void repositoryWasAdded(Repository repository) {
		if (repository instanceof DBRepository) {
			// skip DB special repository
			return;
		}
		if (repository instanceof ResourceRepository && !RepositoryManager.SAMPLE_REPOSITORY_NAME.equals(repository.getName())) {
			// skip resource repositories from tutorials/templates/etc - except for the Samples repository, that one can be indexed
			return;
		}

		// listen for add/delete/change events
		repository.addRepositoryListener(this);

		if (repository instanceof ConnectionRepository) {
			// cannot index directly, add listener to index once it is connected
			((ConnectionRepository) repository).addConnectionListener(this);
		} else {
			// all other repositories, index now!
			addEntriesUnderFolderToIndex(repository);
		}
	}

	@Override
	public void repositoryWasRemoved(Repository repository) {
		// No longer listen for add/delete/change events
		repository.removeRepositoryListener(this);

		if (repository instanceof ConnectionRepository) {
			((ConnectionRepository) repository).removeConnectionListener(this);
		}

		// always delete entries for repo that is going to be removed
		deleteEntriesUnderLocationFromIndex(RepositoryLocation.REPOSITORY_PREFIX + repository.getName());
	}

	@Override
	public void folderRefreshed(Folder folder) {
		// delete all entries under the folder
		deleteEntriesUnderLocationFromIndex(folder.getLocation().getAbsoluteLocation());

		if (folder instanceof ConnectionRepository && !((ConnectionRepository) folder).isConnected()) {
			// do not trigger authentication if offline
			return;
		}
		// add all entries under the new folder to the index again
		addEntriesUnderFolderToIndex(folder);
	}

	@Override
	public void connectionLost(ConnectionRepository repository) {
		// drop all items in that repository because they will get re-added once the connection is re-established
		// if we did not drop them, we may store out-of-sync information because the content may have changed
		// this is especially important if the user of the repository were to change. Then contents are very different.
		deleteEntriesUnderLocationFromIndex(repository.getLocation().getAbsoluteLocation());
	}

	@Override
	public void connectionEstablished(ConnectionRepository repository) {
		// if connection to a connection repository (e.g. RM Server or Cloud) is established, try to query information for all available entries
		addEntriesUnderFolderToIndex(repository);
	}

	/**
	 * Add all entries under the given folder to the index. The full index is potentially costly, so both fast and full index happen async in ProgressThreads.
	 *
	 * @param folder
	 * 		the folder under which all elements will be added to the index
	 */
	private void addEntriesUnderFolderToIndex(final Folder folder) {
		// add stuff to index fast so user can search basic stuff asap
		ProgressThread pgFast = createIndexingThread(folder, false);
		pgFast.start();

		// if enabled, add full metadata to index afterwards, so user can search advanced things
		// don't do it for RestRepositories, the full index is too expensive here
		boolean isRestRepository = folder instanceof RESTRepository;
		if (Boolean.parseBoolean(ParameterService.getParameterValue(RepositoryGlobalSearch.PROPERTY_FULL_REPOSITORY_INDEXING)) && !isRestRepository) {
			ProgressThread pgFull = createIndexingThread(folder, true);
			pgFull.addDependency(pgFast.getID());
			pgFull.start();
		}
	}

	/**
	 * Creates an indexing {@link ProgressThread} for the given {@link Folder}. Can either be a fast or full index.
	 *
	 * @param folder
	 * 		the folder to index
	 * @param fullIndex
	 * 		whether the indexing should be fast or detailed
	 * @return the indexing thread
	 * @since 9.0.0
	 */
	private ProgressThread createIndexingThread(Folder folder, boolean fullIndex) {
		ProgressThread pgIndexing = new ProgressThread("global_search.repo.search_index_" + (fullIndex ? "full" : "fast"), false, folder.getName()) {

			@Override
			public void run() {
				List<Document> indexedEntries = new ArrayList<>();
				try {
					Repository repo = folder.getLocation().getRepository();
					// special indexing for Remote Repo to avoid potentially thousands of queries
					if (repo instanceof RemoteRepository) {
						indexRemoteFolder(indexedEntries, folder, (RemoteRepository) repo, fullIndex);
					} else if (repo instanceof RESTRepository) {
						indexRESTFolder(indexedEntries, folder, (RESTRepository) repo, fullIndex, this);
					} else {
						indexFolder(indexedEntries, folder, fullIndex, this);
					}
					addDocumentsToIndex(indexedEntries);
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_folder", folder.getName());
				}
			}
		};
		pgIndexing.setIndeterminate(true);
		return pgIndexing;
	}

	/**
	 * Recursively indexes the given folder and creates documents for all entries.
	 *
	 * @param list
	 * 		the list in which to store the documents, must not be {@code null}
	 * @param folder
	 * 		the folder for which its entries should be indexed, must not be {@code null}
	 * @param indexMetaData
	 * 		if {@code true}, meta data will be indexed as well, i.e. the attributes will be stored. This is slow!
	 * @param pg
	 * 		the {@link ProgressThread} in which the operation takes place
	 * @throws RepositoryException
	 * 		if something goes wrong during repository access
	 */
	private void indexFolder(final List<Document> list, final Folder folder, final boolean indexMetaData, final ProgressThread pg) throws RepositoryException {
		for (Folder subfolder : folder.getSubfolders()) {
			if (pg.isCancelled()) {
				return;
			}
			indexFolder(list, subfolder, indexMetaData, pg);
		}

		for (DataEntry entry : folder.getDataEntries()) {
			if (pg.isCancelled()) {
				return;
			}
			list.add(createDocument(createItem(entry, indexMetaData)));
		}
	}

	/**
	 * Read all contents of the given remote repository/subfolder and store them as {@link Document}s. If that fails, logs it.
	 *
	 * @param list
	 * 		the list to add the search documents to
	 * @param repository
	 * 		the repository to read all contents from
	 * @param folder
	 * 		the subfolder which should be queried
	 * @param fullIndex
	 * 		if {@code true}, RM Server will be asked to create a full index result including metadata (slow); otherwise metadata is omitted
	 */
	private void indexRemoteFolder(final List<Document> list, final Folder folder, final RemoteRepository repository, final boolean fullIndex) {
		// relative path
		String path = folder.getLocation().getPath();
		if (path == null || Character.toString(RepositoryLocation.SEPARATOR).equals(path)) {
			path = "";
		}

		try {
			ResponseContainer globalSearchResult;
			if (fullIndex) {
				globalSearchResult = repository.getClient().getGlobalSearchItemDetails(path);
			} else {
				globalSearchResult = repository.getClient().getGlobalSearchItemSummary(path);
			}

			int responseCode = globalSearchResult.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// query worked, parse JSON to items and add to search index
				String json = Tools.readTextFile(globalSearchResult.getInputStream());
				RepositoryGlobalSearchItem[] repositorySearchItems = WebServiceTools.parseJsonString(json, RepositoryGlobalSearchItem[].class, false);
				for (RepositoryGlobalSearchItem item : repositorySearchItems) {
					// If an item has no parent, it's in the root folder.
					// Because the alias is locally defined, it is not known on RM Server. Set it here.
					if (item.getParent().isEmpty()) {
						item.setParent(repository.getAlias());
					}
					// for the same reason as above, always set the remote repository alias as repository before the absolute location
					item.setLocation(RepositoryLocation.REPOSITORY_PREFIX + repository.getAlias() + item.getLocation());
					list.add(createDocument(item));
				}
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_remote_folder", new Object[]{repository.getName() + path, responseCode});
			}
		} catch (NotYetSupportedServiceException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_remote_folder_old_server", new Object[]{repository.getName() + path});
		}catch (IOException | RepositoryException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_remote_folder", new Object[]{repository.getName() + path, e.getMessage()});
		}
	}

	/**
	 * Read all contents of the given REST repository/subfolder and store them as {@link Document}s. If that fails, logs it.
	 * If the repository can not use the global search REST service,
	 * normal indexing ({@link #indexFolder(List, Folder, boolean, ProgressThread) indexFolder}) will be used!
	 *
	 * @param list
	 * 		the list to add the search documents to
	 * @param folder
	 * 		the subfolder which should be queried
	 * @param repository
	 * 		the repository to read all contents from
	 * @param fullIndex
	 * 		if {@code true}, RM Server will be asked to create a full index result including metadata (slow); otherwise metadata is omitted
	 * @param pg
	 * 		the progress thread this is called from; needed if fall back to normal indexing
	 */
	private void indexRESTFolder(List<Document> list, Folder folder, RESTRepository repository, boolean fullIndex, ProgressThread pg) {
		// relative path
		String path = folder.getLocation().getPath();
		if (path == null || Character.toString(RepositoryLocation.SEPARATOR).equals(path)) {
			path = "";
		}

		try {
			String apiPath = fullIndex ? API_REST_REMOTE_REPO_DETAILS : API_REST_REMOTE_REPO_SUMMARY;
			String repoPrefix = repository.getPrefix();
			if (repoPrefix != null && !repoPrefix.isEmpty()) {
				repoPrefix = RepositoryLocation.SEPARATOR + repoPrefix;
			}
			HttpURLConnection conn = repository.getGlobalSearchConnection(apiPath, URLEncoder.encode(path, StandardCharsets.UTF_8.name()));
			conn.setRequestMethod("GET");
			conn.setUseCaches(false);
			conn.setAllowUserInteraction(false);
			conn.setRequestProperty("Content-Type", "application/json");
			int responseCode = conn.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				throw new IOException(responseCode + " " + conn.getResponseMessage());
			}
			// query worked, parse JSON to items and add to search index
			String json = Tools.readTextFile(conn.getInputStream());
			RepositoryGlobalSearchItem[] repositorySearchItems = WebServiceTools.parseJsonString(json, RepositoryGlobalSearchItem[].class, false);
			for (RepositoryGlobalSearchItem item : repositorySearchItems) {
				// If an item has no parent, it's in the root folder.
				// Because the name is locally defined, it is not known on RM Server. Set it here.
				if (item.getParent().isEmpty()) {
					item.setParent(repository.getName());
				}
				String itemLocation = item.getLocation();
				if (repoPrefix != null) {
					if (!itemLocation.startsWith(repoPrefix)) {
						// skip items that do not come from the correct prefix
						continue;
					}
					// if there is a repo prefix, cut it from the actual location
					itemLocation = itemLocation.substring(repoPrefix.length());
				}
				// for the same reason as above, always set the REST repository name as repository before the absolute location
				item.setLocation(RepositoryLocation.REPOSITORY_PREFIX + repository.getName() + itemLocation);
				list.add(createDocument(item));
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_rest_folder", new Object[]{repository.getName() + path, e.getMessage()});
		}
	}

	/**
	 * Deletes all entries under the given location from the index.
	 *
	 * @param absoluteFolderPath
	 * 		the absolute path of a folder under which all subfolders and entries should be removed from the index
	 */
	private void deleteEntriesUnderLocationFromIndex(final String absoluteFolderPath) {
		// delete all entries under the former parent folder
		// escape our repository path syntax, it does not place nicely with Lucene
		String escapedParentLocation = GlobalSearchUtilities.INSTANCE.encodeRepositoryPath(absoluteFolderPath);
		// now also add a wildcard at end of path, to find all elements starting with that path
		String entriesUnderFormerPath = escapedParentLocation + GlobalSearchUtilities.QUERY_WILDCARD;

		// now actually search for all elements that start with the prepared path
		GlobalSearchResultBuilder builder = new GlobalSearchResultBuilder(FIELD_LOCATION + GlobalSearchUtilities.QUERY_FIELD_SPECIFIER + entriesUnderFormerPath);
		builder.setMaxNumberOfResults(Integer.MAX_VALUE).setSearchCategories(GlobalSearchRegistry.INSTANCE.getSearchCategoryById(getSearchCategoryId()));
		try {
			GlobalSearchResult result = builder.runSearch();
			removeDocumentsFromIndex(result.getResultDocuments());
		} catch (ParseException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.delete_index_error_folder", e);
		}
	}

	/**
	 * Creates a repository search item for the given entry.
	 *
	 * @param entry
	 * 		the repository entry for which to create the search item
	 * @return the item, never {@code null}
	 */
	private RepositoryGlobalSearchItem createItem(final Entry entry, final boolean indexMetaData) {
		RepositoryGlobalSearchItem item = new RepositoryGlobalSearchItem();
		if (entry instanceof DateEntry) {
			long ms = ((DateEntry) entry).getDate();
			if (ms > 0) {
				item.setModified(String.valueOf(ms));
			}
		}

		// See if it's an ExampleSet/Model, then try to get its attributes
		if (entry instanceof ConnectionEntry) {
			// Extract connection information from metadata
			try {
				item.setConnectionName(entry.getName());
				ConnectionEntry conEntry = (ConnectionEntry) entry;
				item.setConnectionType(conEntry.getConnectionType());
				ConnectionConfiguration conf = ((ConnectionInformationMetaData) conEntry.retrieveMetaData()).getConfiguration();
				List<String> tags = conf.getTags();
				item.setConnectionTags(tags != null ? tags.toArray(new String[0]) : null);
			} catch (RepositoryException e) {
				// no metadata available, ignore
			} catch (Exception e) {
				// no metadata, no connection information
				LogService.log(LogService.getRoot(), Level.WARNING, e, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_md_connection_reading", entry.getLocation().getAbsoluteLocation());
			}
		} else if (indexMetaData && entry instanceof IOObjectEntry) {
			// Extract attributes from example sets
			try {
				MetaData md = ((IOObjectEntry) entry).retrieveMetaData();
				ExampleSetMetaData exampleSetMetaData = null;
				if (md instanceof ExampleSetMetaData) {
					exampleSetMetaData = (ExampleSetMetaData) md;
				} else if (md instanceof ModelMetaData) {
					exampleSetMetaData = ((ModelMetaData) md).getTrainingSetMetaData();
				}

				if (exampleSetMetaData != null) {
					int size = exampleSetMetaData.getAllAttributes().size();
					String[] attributes = new String[size];
					int i = 0;
					for (AttributeMetaData amd : exampleSetMetaData.getAllAttributes()) {
						attributes[i++] = amd.getName();
					}

					item.setAttributes(attributes);
				}
			} catch (RepositoryException e) {
				// no metadata available, ignore
			} catch (Exception e) {
				// just to keep going, but log this unexpected error
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.global_search.RepositorySearchManager.error.initial_index_error_md_reading", e);
			}
		}

		// generic fields
		item.setType(entry.getType()).setParent(entry.getContainingFolder().getName()).setOwner(entry.getOwner()).setName(entry.getName()).setLocation(entry.getLocation().getAbsoluteLocation());
		return item;
	}


	/**
	 * Creates a repository search document only for deletion. Does not need to know as many things as documents for searching.
	 *
	 * @param location
	 * 		the absolute repository location of the item to delete
	 * @param name
	 * 		the name of the item to delete
	 * @return the document, never {@code null}
	 */
	private Document createDocumentForDeletion(final String location, final String name) {
		return GlobalSearchUtilities.INSTANCE.createDocument(location, name);
	}

	/**
	 * Creates a repository search document for the given {@link RepositoryGlobalSearchItem}.
	 *
	 * @param item
	 * 		the repository search item for which to create the search document
	 * @return the document, never {@code null}
	 */
	private Document createDocument(final RepositoryGlobalSearchItem item) {
		List<Field> fields = new ArrayList<>();
		String modified = item.getModified();
		if (modified != null && !modified.trim().isEmpty()) {
			long ms = Long.parseLong(modified);
			if (ms > 0) {
				fields.add(GlobalSearchUtilities.INSTANCE.createFieldForDateValues(FIELD_MODIFIED, ms));

				// also sort by last modified, to return last edited things first
				fields.add(GlobalSearchUtilities.INSTANCE.createSortingField(ms));
			}
		}

		// See if it's an ExampleSet/Model, then try to get its attributes
		String[] attributes = item.getAttributes();
		if (attributes != null && attributes.length > 0) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_ATTRIBUTE, String.join(" ", attributes)));
		}

		// Connection fields
		if (item.getConnectionName() != null) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTitles(FIELD_CONNECTION_NAME, item.getConnectionName()));
		}
		if (item.getConnectionTags() != null) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_CONNECTION_TAGS, String.join(" ", item.getConnectionTags())));
		}
		String connectionType = item.getConnectionType();
		if (connectionType != null) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTitles(FIELD_CONNECTION_TYPE, connectionType));
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTitles(FIELD_CONNECTION_TYPE_NAME, ConnectionI18N.getTypeName(connectionType)));
		}

		// generic fields
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(FIELD_TYPE, item.getType()));
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_PARENT, item.getParent()));
		fields.add(GlobalSearchUtilities.INSTANCE.createFieldForTexts(FIELD_LOCATION, GlobalSearchUtilities.INSTANCE.encodeRepositoryPath(item.getLocation())));
		if (item.getOwner() != null) {
			fields.add(GlobalSearchUtilities.INSTANCE.createFieldForIdentifiers(FIELD_USER, item.getOwner()));
		}
		// absolute repository location is the unique ID for the repository category
		return GlobalSearchUtilities.INSTANCE.createDocument(item.getLocation(), item.getName(), fields.toArray(new Field[0]));
	}

}
