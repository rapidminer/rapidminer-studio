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
package com.rapidminer.connection.gui.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.internal.remote.RemoteRepository;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/**
 * ConnectionModel backing connection UIs. It contains all relevant pieces of a {@link
 * com.rapidminer.connection.ConnectionInformation ConnectionInformation} object.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ConnectionModel {

	private final RepositoryLocation location;
	private final boolean editable;
	private final StringProperty description = new SimpleStringProperty();
	private final ObservableList<String> tags = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<ConnectionParameterGroupModel> parameterGroups = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<ValueProviderModel> valueProviders = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<PlaceholderParameterModel> placeholders = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<Path> libraryFiles = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ObservableList<Path> otherFiles = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
	private final ConnectionInformation information;


	/**
	 * Copy constructor
	 *
	 * <p>This copy constructor does only copy the data, not the listeners!</p>
	 *
	 * @param model
	 * 		the connection model to copy
	 */
	ConnectionModel(ConnectionModel model) {
		this.information = model.information;
		this.location = model.location;
		this.editable = model.editable;
		this.description.setValue(model.description.getValue());
		this.tags.addAll(model.tags);

		for (ConnectionParameterGroupModel group : model.parameterGroups) {
			this.parameterGroups.add(new ConnectionParameterGroupModel(this, group));
		}

		for (ValueProviderModel vp : model.valueProviders) {
			this.valueProviders.add(new ValueProviderModel(vp));
		}

		for (PlaceholderParameterModel placeholder : model.placeholders) {
			this.placeholders.add(new PlaceholderParameterModel(this, placeholder));
		}

		this.libraryFiles.addAll(model.libraryFiles);
		this.otherFiles.addAll(model.otherFiles);
	}

	ConnectionModel(ConnectionInformation information, RepositoryLocation location, boolean editable, List<ValueProviderModel> valueProviders) {
		this.information = information;
		this.location = location;
		this.editable = editable;
		this.valueProviders.setAll(valueProviders);
	}

	public RepositoryLocation getLocation() {
		return location;
	}

	public String getType() {
		return information.getConfiguration().getType();
	}

	public String getName() {
		return information.getConfiguration().getName();
	}

	public String getDescription() {
		return description.get();
	}

	public StringProperty descriptionProperty() {
		return description;
	}

	public void setDescription(String description) {
		this.description.setValue(description);
	}

	public ObservableList<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags.setAll(tags);
	}

	/**
	 * @return {@code true} if the connection is editable
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Use {@link #removeParameterGroup(String)} or {@link #addOrSetParameter} and {@link #removeParameter} to add and
	 * remove placeholders
	 *
	 * @return unmodifiable observable list
	 */
	public ObservableList<ConnectionParameterGroupModel> getParameterGroups() {
		return FXCollections.unmodifiableObservableList(parameterGroups);
	}

	/**
	 * Gets a parameter group by its name
	 *
	 * @param groupName
	 * 		the parameter group name
	 * @return the parameter group for this name, or {@code null}
	 */
	public ConnectionParameterGroupModel getParameterGroup(String groupName) {
		for (ConnectionParameterGroupModel group : parameterGroups) {
			if (group.getName().equals(groupName)) {
				return group;
			}
		}
		return null;
	}

	/**
	 * Removes a parameter group
	 *
	 * @param groupName
	 * 		the parameter group name
	 * @return {@code true} if a group with this name was removed
	 */
	public boolean removeParameterGroup(String groupName) {
		ConnectionParameterGroupModel group = getParameterGroup(groupName);
		if (group == null) {
			return false;
		}
		synchronized (parameterGroups) {
			return parameterGroups.remove(group);
		}
	}

	/**
	 * Gets or creates the parameter group
	 *
	 * @param groupName
	 * 		the group name
	 * @return the existing group of this name or a new one
	 */
	public ConnectionParameterGroupModel getOrCreateParameterGroup(String groupName) {
		ConnectionParameterGroupModel model = getParameterGroup(groupName);

		if (model != null) {
			return model;
		}

		synchronized (parameterGroups) {
			model = getParameterGroup(groupName);
			if (model == null) {
				model = new ConnectionParameterGroupModel(this, groupName);
				parameterGroups.add(model);
			}
		}

		return model;
	}

	/**
	 * Gets a parameter
	 *
	 * @param groupName
	 * 		the group name of the parameter
	 * @param parameterName
	 * 		the name of the parameter
	 * @return the parameter, or {@code null}
	 */
	public ConnectionParameterModel getParameter(String groupName, String parameterName) {
		return Optional.ofNullable(getParameterGroup(groupName)).map(m -> m.getParameter(parameterName)).orElse(null);
	}

	/**
	 * Use {@link #addOrSetPlaceholder} and {@link #removePlaceholder} to add and remove placeholders
	 *
	 * @return unmodifiable observable list
	 */
	public ObservableList<PlaceholderParameterModel> getPlaceholders() {
		return FXCollections.unmodifiableObservableList(placeholders);
	}

	/**
	 * Gets a placeholder
	 *
	 * @param groupName
	 * 		the group name of the parameter
	 * @param parameterName
	 * 		the name of the parameter
	 * @return the placeholder, or {@code null}
	 */
	public PlaceholderParameterModel getPlaceholder(String groupName, String parameterName) {
		for (PlaceholderParameterModel placeholder : placeholders) {
			if (placeholder.getGroupName().equals(groupName) && placeholder.getName().equals(parameterName)) {
				return placeholder;
			}
		}
		return null;
	}

	/**
	 * Adds a placeholder
	 *
	 * @param groupName
	 * 		the group name
	 * @param name
	 * 		the parameter name
	 * @param isEncrypted
	 * 		if the parameter is encrypted
	 * @param injectorName
	 * 		the name of the value provider
	 * @param isEnabled
	 * 		if the parameter is enabled
	 * @return {@code true} if the parameter was added
	 */
	public boolean addOrSetPlaceholder(String groupName, String name, String value, boolean isEncrypted, String injectorName, boolean isEnabled) {
		PlaceholderParameterModel parameter = getPlaceholder(groupName, name);
		if (parameter != null) {
			parameter.setValue(value);
			parameter.setEncrypted(isEncrypted);
			parameter.setInjectorName(injectorName);
			parameter.setEnabled(isEnabled);
			return false;
		}
		return placeholders.add(new PlaceholderParameterModel(this, groupName, name, value, isEncrypted, injectorName, isEnabled));
	}

	/**
	 * Removes placeholder
	 *
	 * @param groupName
	 * 		the group name
	 * @param parameterName
	 * 		the parameter name
	 * @return {@code true} if the parameter was removed
	 */
	public boolean removePlaceholder(String groupName, String parameterName) {
		return placeholders.removeIf(param -> param.getGroupName().equals(groupName) && param.getName().equals(parameterName));
	}

	/**
	 * Adds a parameter
	 *
	 * @param groupName
	 * 		the group name
	 * @param name
	 * 		the parameter name
	 * @param isEncrypted
	 * 		if the parameter is encrypted
	 * @param injectorName
	 * 		the name of the value provider
	 * @param isEnabled
	 * 		if the parameter is enabled
	 * @return {@code true} if the parameter was added
	 */
	public boolean addOrSetParameter(String groupName, String name, String value, boolean isEncrypted, String injectorName, boolean isEnabled) {
		return getOrCreateParameterGroup(groupName).addOrSetParameter(name, value, isEncrypted, injectorName, isEnabled);
	}

	/**
	 * Removes a parameter
	 *
	 * @param groupName
	 * 		the group name
	 * @param parameterName
	 * 		the parameter name
	 * @return {@code true} if the parameter was removed
	 */
	public boolean removeParameter(String groupName, String parameterName) {
		return Optional.ofNullable(getParameterGroup(groupName))
				.map(g -> g.removeParameter(parameterName)).orElse(false);
	}

	/**
	 * @return a shallow copy of {@link #valueProvidersProperty()}
	 */
	public List<ValueProvider> getValueProviders() {
		return new ArrayList<>(valueProvidersProperty());
	}

	/**
	 * If not editing the list of used value providers will be shown. If editing all possible value providers need to be
	 * shown to be able to configure them.
	 *
	 * @return mutable observable list
	 */
	public ObservableList<ValueProviderModel> valueProvidersProperty() {
		if (editable) {
			setupOtherValueProviders();
		}
		return valueProviders;
	}

	/**
	 * Replaces the current value providers with the given ones
	 *
	 * @param valueProviders
	 * 		the new value providers
	 */
	public void setValueProviders(List<ValueProviderModel> valueProviders) {
		this.valueProviders.setAll(valueProviders);
	}

	/**
	 * @return mutable observable list of library files
	 */
	public ObservableList<Path> getLibraryFiles() {
		return libraryFiles;
	}

	public void setLibraryFiles(List<Path> libraryFiles) {
		this.libraryFiles.setAll(libraryFiles);
	}

	/**
	 * @return mutable observable list of other files
	 */
	public ObservableList<Path> getOtherFiles() {
		return otherFiles;
	}

	public void setOtherFiles(List<Path> otherFiles) {
		this.otherFiles.setAll(otherFiles);
	}

	/**
	 * Creates a copy of the data <strong>without the listeners</strong>
	 *
	 * @return a copy without the listeners
	 */
	public ConnectionModel copyDataOnly() {
		return new ConnectionModel(this);
	}

	/**
	 * Add those {@link ValueProvider ValueProviders} to the valueprovider list that were not used so far by this
	 * connection. These need to be configurable in editmode and therefore exist in the list. They will be added in
	 * alphabetical order.
	 */
	private void setupOtherValueProviders() {
		String filter;
		try {
			filter = location.getRepository() instanceof RemoteRepository ? ValueProviderHandlerRegistry.REMOTE : null;
		} catch (RepositoryException e) {
			filter = ValueProviderHandlerRegistry.REMOTE;
		}
		final List<ValueProviderModel> newValueProviders = ValueProviderHandlerRegistry.getInstance().getVisibleTypes(filter)
				.stream().filter(aType -> this.valueProviders.stream().noneMatch(vp -> aType.equals(vp.getType())))
				.map(type -> ValueProviderHandlerRegistry.getInstance().getHandler(type).createNewProvider(ConnectionI18N.getValueProviderTypeName(type)))
				.map(ValueProviderModelConverter::toModel).collect(Collectors.toList());
		if (newValueProviders.isEmpty()) {
			return;
		}
		newValueProviders.sort(Comparator.comparing(ValueProviderModel::getName));
		valueProviders.addAll(newValueProviders);
	}

	/**
	 * Converts the current configuration into an ConnectionInformation object
	 *
	 * @return a ConnectionInformation object which contains the current information
	 */
	ConnectionInformation asConnectionInformation() {
		return ConnectionModelConverter.applyConnectionModel(information, this);
	}
}
