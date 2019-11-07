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
package com.rapidminer.repository.internal.remote;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.model.InstanceData;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.cipher.CipherException;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.jwt.JwtClaim;


/**
 * Base implementation containing methods for all services exposed by Server.
 * May throw {@link com.rapidminer.repository.internal.remote.exception.NotYetSupportedServiceException NotYetSupportedServiceException}
 * or {@link com.rapidminer.repository.internal.remote.exception.DeprecatedServiceException DeprecatedServiceException}
 * which clarify if a service was not supported in the Server version or was deprecated and cannot be used any longer.
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public interface BaseServerClient {

    /**
     * Check if the given filename was blacklisted on the Server
     *
     * @param originalFilename to be checked
     * @return {@code true} if the server would not accept uploading this file
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    boolean isFileExtensionBlacklisted(String originalFilename) throws IOException, RepositoryException;

    /**
     * Load the instance data containing information like server version and product edition.
     *
     * @return the {@link InstanceData} containing all the information from server
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    InstanceData loadInstanceData() throws IOException, RepositoryException;

    /**
     * Read and return the server version.
     *
     * @return the servers {@link VersionNumber}
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    VersionNumber getServerVersion() throws IOException, RepositoryException;

    /**
     * Check if the process with the given repository location would be compatible with the {@link RemoteRepository} this
     * check is called upon.
     *
     * @param location the path of an entry in the repository
     * @return an empty list if there are no errors or a list of errors
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    List<String> checkProcessCompatibility(String location) throws IOException, RepositoryException;

    /**
     * Returns the URI to which a browser can be pointed to access the RA web interface.
     *
     * @return {@link URI} to the servers webinterface
     */
    URI getWebInterfaceURI();

    /**
     * Returns the URI to which a browser can be pointed to browse a given entry.
     *
     * @return {@link URI} for a resource
     */
    URI getURIForResource(String path);

    /**
     * Get the {@link URI} to access a log for a process
     *
     * @param id of the process
     * @return Server {@link URI} of the process log
     */
    URI getProcessLogURI(int id);

    /**
     * Load JDBC connections from Server
     *
     * @return a Collection of Objects which are FieldConnectionEntry
     * @throws XMLException    if the returned XML structure is incorrect
     * @throws CipherException if there was a problem during decryption
     * @throws SAXException    if there was a problem parsing the servers response to XML
     * @throws IOException     in case accessing the server failed technically
     */
    Collection fetchJDBCEntries() throws XMLException, CipherException, SAXException, IOException, RepositoryException;

    /**
     * Load the global search details for the global search item
     *
     * @param path to the remote folder
     * @return ResponseContainer with the response details
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer getGlobalSearchItemDetails(String path) throws RepositoryException, IOException;

    /**
     * Load the global search summary for the remote folder
     *
     * @param path to the remote folder
     * @return ResponseContainer with the response details
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer getGlobalSearchItemSummary(String path) throws RepositoryException, IOException;

    /**
     * Read the claim from the remote token service without verifying the signature
     *
     * <p>
     * Warning: Don't use the result of this method to give access to sensitive information!
     * </p>
     *
     * @return JwtClaim or null
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    JwtClaim getJwtClaim() throws RepositoryException, IOException;

    /**
     * Tries to connect to the server which will test the current username and password.
     * <p>
     * The {@link com.rapidminer.tools.WebServiceTools#WEB_SERVICE_TIMEOUT} settings value will be used as the timeout value.
     *
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    void checkPasswordOnce() throws IOException, RepositoryException;

    /**
     * Retrieve the {@link URL} to access the {@link RemoteInfoService}
     *
     * @return the {@link URL} to connect to the Server's Info Service
     */
    URL getRAInfoServiceWSDLUrl();

    /**
     * Retrieve the {@link URL} to access the {@link RemoteContentManager}
     *
     * @return the {@link URL} to connect to the Server's Repository Service
     */
    URL getRepositoryServiceWSDLUrl();

    /**
     * Load all server vault entries for a repository location
     *
     * @param repoLocation the location to read the vault information for
     * @return the entries for that repository location
     */
    RemoteVaultEntry[] loadVaultInfo(String repoLocation) throws RepositoryException;

    /**
     * Update the instance data from the outside, like after loading it to choose the best matching client implementation.
     *
     * @param instanceData the {@link InstanceData} of the server
     */
    void setInstanceData(InstanceData instanceData);

    /**
     * Get the currently known {@link InstanceData}, will not read from server
     *
     * @return the current {@link InstanceData}, may be null
     */
    InstanceData getInstanceData();

    /**
     * Create new entry in the vault
     *
     * @param path    of the entry in the repository
     * @param entries a list of entries to add to the vault
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    void createVaultEntry(String path, List<RemoteCreateVaultInformation> entries) throws IOException, RepositoryException;

    /**
     * Load the history of a repository entry, all the available versions
     *
     * @param path of the entry in the repository
     * @return a generic container with the original content
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer loadRepositoryEntryHistory(String path) throws IOException, RepositoryException;

    /**
     * Load an entry from the repository
     *
     * @param path        of the entry
     * @param streamType  to be loaded
     * @param readChunked if the reading should happen chunked
     * @return a generic container with all the information of the connection
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer loadRepositoryEntry(String path, RemoteRepository.EntryStreamType streamType, boolean readChunked) throws IOException, RepositoryException;

    /**
     * Get write access to an entry in the repository, where writing is left to the caller to write what's necessary into the {@link java.io.OutputStream}
     *
     * @param path         of the entry
     * @param type         of the entry
     * @param mimeType     for sending
     * @param writeChunked if writing should happen chunked, telling the server about it upfront
     * @return a generic container with all the information of the connection
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer openEntryOutputstream(String path, RemoteRepository.EntryStreamType type, String mimeType, boolean writeChunked) throws IOException, RepositoryException;

    /**
     * Check if a connection exists remote for the given path. Can be used to check the successful consumption of an
     * uploaded connection information by Server.
     *
     * @param path The path of the connection that was stored.
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    void checkConnectionAvailable(String path) throws IOException, RepositoryException;

    /**
     * Load all the available configuration type IDs from Server.
     *
     * @return a list of configuration type IDs
     * @throws IOException            in case accessing the server failed technically
     * @throws RepositoryException    in case the repository could not fulfill the request
     * @throws SAXException           in case parsing the response fails
     * @throws ConfigurationException in case the response structure is invalid
     */
    List<String> loadConfigurationTypes() throws IOException, RepositoryException, SAXException, ConfigurationException;

    /**
     * Load the configurations for a specific type ID retrieved from {@link BaseServerClient#loadConfigurationTypes()}
     *
     * @param typeId of the configurations to be retrieved
     * @return generic {@link ResponseContainer} leaves parsing the returned data to the implementation below.
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer loadConfigurationType(String typeId) throws IOException, RepositoryException;

    /**
     * Store a configuration on Server
     *
     * @param typeId of the configuration to be stored
     * @param xml    to be written
     * @return generic {@link ResponseContainer} with information if storing was possible
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     * @throws XMLException        in case writing the XML did not work properly
     */
    ResponseContainer storeConfigurationType(String typeId, Document xml) throws IOException, RepositoryException, XMLException;

    /**
     * Load the {@link com.rapidminer.operator.ports.metadata.MetaData} of an {@link com.rapidminer.repository.Entry}
     *
     * @param path              of the MetaData
     * @param mdEntryStreamType expected type of MetaData
     * @return generic ResponseContainer leaves parsing the result to the caller
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer loadMetaData(String path, RemoteRepository.EntryStreamType mdEntryStreamType) throws RepositoryException, IOException;

    /**
     * Load an {@link com.rapidminer.operator.IOObject} with a specific type
     *
     * @param path            of the object
     * @param entryStreamType of the object
     * @return generic ResponseContainer to read the {@link com.rapidminer.operator.IOObject} properly
     * @throws IOException         in case accessing the server failed technically
     * @throws RepositoryException in case the repository could not fulfill the request
     */
    ResponseContainer loadIOObject(String path, RemoteRepository.EntryStreamType entryStreamType) throws IOException, RepositoryException;

    /**
     * Check if the Server supports connections.
     *
     * @return true iff storing connections is possible on Server
     */
    boolean supportsConnections();
}
