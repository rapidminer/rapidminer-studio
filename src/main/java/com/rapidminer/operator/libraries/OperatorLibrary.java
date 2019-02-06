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
package com.rapidminer.operator.libraries;

import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.documentation.OperatorDocBundle;
import com.rapidminer.tools.documentation.OperatorDocumentation;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;


/**
 * This is the interface for all OperatorLibraries. These are objects that contain the description
 * of an arbitrary number of Operators, together with their documentation.
 * 
 * @author Sebastian Land
 */
public interface OperatorLibrary extends Serializable {

	/**
	 * This is the repository location where this library is stored.
	 */
	public String getRepositoryLocation();

	/**
	 * This method returns the keys of all defined operators.
	 */
	public Set<String> getOperatorKeys();

	/**
	 * This method returns the {@link OperatorDescription} associated with the given key.
	 */
	public LibraryOperatorDescription getDescription(String key);

	/**
	 * This method must return the {@link OperatorDescription}'s of all defined operators.
	 */
	public List<LibraryOperatorDescription> getOperatorDescriptions();

	/**
	 * This method will register this {@link OperatorLibrary}s operators to the
	 * {@link OperatorService}.
	 * 
	 * @throws OperatorCreationException
	 */
	public void registerOperators() throws OperatorCreationException;

	/**
	 * This method will unregister this {@link OperatorLibrary}s operators from the
	 * {@link OperatorService}.
	 */
	public void unregisterOperators();

	/**
	 * This method has to return a {@link OperatorDocumentation} for the given key. The result must
	 * be not null! If no documentation exists, use a default one, with just the key as name.
	 */
	public OperatorDocumentation getOperatorDocumentation(String operatorKey);

	/**
	 * This returns a {@link OperatorDocBundle} that contains information about each single
	 * delivered {@link Operator} of this library.
	 */
	public OperatorDocBundle getDocumentationBundle();

	/**
	 * This returns the name of the library. This name will be displayed as Subgroup name in the
	 * NewOperatorTree below the top group for libraries.
	 */
	public String getName();

	/**
	 * This returns a short textual description of the functionality provided by this
	 * {@link OperatorLibrary}.
	 */
	public String getSynopsis();

	/**
	 * This returns a HTML fragment that in detail describes the inner working of this
	 * {@link OperatorLibrary}.
	 */
	public String getDocumentation();

	/**
	 * This method returns a {@link VersionNumber} that is the version of this
	 * {@link OperatorLibrary}.
	 */
	public VersionNumber getVersion();

	/**
	 * This method returns the namespace of this OperatorLibrary. These namespaces are used to
	 * distinguish between different libraries. The namespace should be unique and must be of the
	 * form: rmol_"Qualifier".
	 */
	public String getNamespace();

	/**
	 * This method returns the speaking name of the namespace of this OperatorLibrary. These
	 * namespaces are used to distinguish between different libraries. The namespace should be
	 * unique and must be of the form: rmol_"Qualifier". In order to make it unique a random suffix
	 * is added. This returns the original namespace name given by user.
	 */
	public String getNamespaceName();

	/**
	 * This sets the name of the library. This name will be displayed as Subgroup name in the
	 * NewOperatorTree below the top group for libraries.
	 */
	public void setName(String newName);

	/**
	 * This sets a short textual description of the functionality provided by this
	 * {@link OperatorLibrary}.
	 */
	public void setSynopsis(String newSyopsis);

	/**
	 * This sets a HTML fragment that in detail describes the inner working of this
	 * {@link OperatorLibrary}.
	 */
	public void setDocumentation(String newDocumentation);

	/**
	 * This method sets a {@link VersionNumber} that is the version of this {@link OperatorLibrary}.
	 */
	public void setVersion(VersionNumber newVersion);

	/**
	 * This method will save the current state of the library to the given repository entry
	 */
	public void save(BlobEntry entry);

	/**
	 * This method will save the current state of the library into the given stream.
	 */
	public void save(OutputStream out);
}
