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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.Model;
import com.rapidminer.operator.ports.InputPort;


/**
 * This class is holds the informations for all models. It is the parent class of each more model
 * specific implementation, which should be able to simulate all changes on the data of a model
 * application during meta data transformation. This super class remembers the meta data of the
 * trainings set and already checks compatibility of the application meta data with the trainings
 * meta data. This is done in an equal way to the PredictionModel.
 * 
 * TODO: This model needs to become abstract in order to force all operators to implement a proper
 * and problem specific ModelMetaData object.
 * 
 * @author Simon Fischer, Sebastian Land
 */
public class ModelMetaData extends MetaData {

	private static final long serialVersionUID = 1L;

	private ExampleSetMetaData trainingSetMetaData;

	/** Clone constructor */
	protected ModelMetaData() {}

	public ModelMetaData(ExampleSetMetaData trainingSetMetaData) {
		this(Model.class, trainingSetMetaData);
	}

	public ModelMetaData(Class<? extends Model> mclass, ExampleSetMetaData trainingSetMetaData) {
		super(mclass);
		this.trainingSetMetaData = trainingSetMetaData;
	}

	public ModelMetaData(Model model, boolean shortened) {
		super(model.getClass());
		this.trainingSetMetaData = (ExampleSetMetaData) MetaData.forIOObject(model.getTrainingHeader(), shortened);
	}

	@Override
	public String getDescription() {
		return super.getDescription();
	}

	/**
	 * This method simulates the application of a model. First the compatibility of the model with
	 * the current example set is checked and then the effects are applied.
	 */
	public final ExampleSetMetaData apply(ExampleSetMetaData emd, InputPort inputPort) {
		checkCompatibility(emd, inputPort);
		return applyEffects(emd, inputPort);
	}

	private void checkCompatibility(ExampleSetMetaData emd, InputPort inputPort) {

	}

	/**
	 * This method must be implemented by subclasses in order to apply any changes on the meta data,
	 * that would occur on application of the real model. TODO: This method should be abstract.
	 */
	protected ExampleSetMetaData applyEffects(ExampleSetMetaData emd, InputPort inputPort) {
		return emd;
	}

	@Override
	public ModelMetaData clone() {
		ModelMetaData md = (ModelMetaData) super.clone();
		if (trainingSetMetaData != null) {
			md.trainingSetMetaData = trainingSetMetaData.clone();
		}
		return md;
	}

	public ExampleSetMetaData getTrainingSetMetaData() {
		return trainingSetMetaData;
	}

}
