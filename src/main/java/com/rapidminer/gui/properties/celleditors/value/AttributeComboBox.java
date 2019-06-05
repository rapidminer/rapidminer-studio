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
package com.rapidminer.gui.properties.celleditors.value;

import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.autocomplete.AutoCompleteComboBoxAddition;
import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.tools.container.Pair;


/**
 * Autocompletion combo box that observes an input port so it can update itself whenever the meta
 * data changes.
 *
 * @author Simon Fischer, Sebastian Land
 *
 */
public class AttributeComboBox extends JComboBox<String> {

	private static final long serialVersionUID = 1L;

	static class AttributeComboBoxModel extends DefaultComboBoxModel<String> implements MetaDataChangeListener {

		private static final long serialVersionUID = 1L;

		private ParameterTypeAttribute attributeType;
		private List<Pair<String, Integer>> attributes = new ArrayList<>();

		AttributeComboBoxModel(ParameterTypeAttribute attributeType) {
			this.attributeType = attributeType;
			MetaData metaData = attributeType.getMetaData();
			if (metaData != null) {
				informMetaDataChanged(metaData);
			}
		}

		@Override
		public int getSize() {
			return attributes.size();
		}

		@Override
		public String getElementAt(int index) {
			return attributes.get(index).getFirst();
		}

		/**
		 * This method will cause this model to register as a MetaDataChangeListener on the given
		 * input port. Attention! Make sure, it will be proper unregistered to avoid a memory leak!
		 */
		void registerListener() {
			MetaDataProvider mdp = attributeType.getMetaDataProvider();
			if (mdp != null) {
				mdp.addMetaDataChangeListener(this);
			}
		}

		/**
		 * This method will unregister this model from the InputPort.
		 */
		void unregisterListener() {
			MetaDataProvider mdp = attributeType.getMetaDataProvider();
			if (mdp != null) {
				mdp.removeMetaDataChangeListener(this);
			}
		}

		@Override
		public void informMetaDataChanged(MetaData newMetadata) {
			SwingUtilities.invokeLater(() -> {
				attributes = attributeType.getAttributeNamesAndTypes(true);
				fireContentsChanged(0, 0, attributes.size());
			});
		}

		/**
		 * @return the attribute <> value type pairs
		 */
		List<Pair<String, Integer>> getAttributePairs() {
			return attributes;
		}
	}

	private AttributeComboBoxModel model;

	public AttributeComboBox(ParameterTypeAttribute type) {
		super(new AttributeComboBoxModel(type));
		model = (AttributeComboBoxModel) getModel();
		AutoCompleteComboBoxAddition autoCompleteCBA = new AutoCompleteComboBoxAddition(this);
		autoCompleteCBA.setCaseSensitive(false);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		model.registerListener();
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		model.unregisterListener();
	}
}
