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
package com.rapidminer.operator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.learner.meta.MetaModel;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.Tools;


/**
 * This model is a container for all models which should be applied in a sequence.
 *
 * @author Ingo Mierswa
 */
public class GroupedModel extends AbstractModel implements Iterable<Model>, MetaModel {

	private static final long serialVersionUID = -4954107247345450771L;

	/** Contains all models. */
	private List<Model> models = new ArrayList<Model>();

	/**
	 * @deprecated Using this constructor results in a GroupedModel without a training header
	 *             example set. This can cause NPE in places where the {@link Model} is assumed to
	 *             have a training header set (for example when the model is connected to the
	 *             process result port). Use {@link GroupedModel#GroupedModel(ExampleSet)} instead.
	 */
	@Deprecated
	public GroupedModel() {
		super(null);
	}

	/**
	 * Creates a GroupedModel with a training header example set. The training header example set
	 * should correspond to the last (in the order in which the models are added and will be applied
	 * to the group model) model.
	 *
	 * @param exampleSet
	 *            The {@link ExampleSet} from which to create the training header example set
	 * @since 7.4.0
	 */
	public GroupedModel(ExampleSet exampleSet) {
		super(exampleSet);
	}

	/** Applies all models. */
	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		exampleSet = (ExampleSet) exampleSet.clone();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(100);
		}
		int modelCounter = 0;

		for (Model model : models) {
			// add observer to observe the progress of the model
			Operator dummy = null;
			if (progress != null) {
				try {
					dummy = OperatorService.createOperator("dummy");
				} catch (OperatorCreationException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.operator.GroupedModel.couldnt_create_operator");
				}
				if (dummy != null && model instanceof AbstractModel) {
					final OperatorProgress finalProgress = progress;
					final int finalModelCounter = modelCounter;
					((AbstractModel) model).setOperator(dummy);
					((AbstractModel) model).setShowProgress(true);
					OperatorProgress internalProgress = dummy.getProgress();
					internalProgress.setCheckForStop(false);
					internalProgress.addObserver(new Observer<OperatorProgress>() {

						@Override
						public void update(Observable<OperatorProgress> observable, OperatorProgress arg) {
							try {
								finalProgress.setCompleted((int) ((double) arg.getProgress() / getNumberOfModels()
										+ 100.0 * finalModelCounter / getNumberOfModels()));
							} catch (ProcessStoppedException e) {
								throw new ProcessStoppedRuntimeException();
							}
						}
					}, false);
				}
			}

			exampleSet = model.apply(exampleSet);

			if (progress != null) {
				if (dummy != null && model instanceof AbstractModel) {
					((AbstractModel) model).setShowProgress(false);
					((AbstractModel) model).setOperator(null);
				}
				progress.setCompleted((int) (100.0 * ++modelCounter / getNumberOfModels()));
			}
		}
		return exampleSet;
	}

	@Override
	public Iterator<Model> iterator() {
		return models.iterator();
	}

	/** Returns true if all inner models return true. */
	@Override
	public boolean isUpdatable() {
		Iterator<Model> i = models.iterator();
		while (i.hasNext()) {
			if (!i.next().isUpdatable()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Updates the model if the classifier is updatable. Otherwise, an {@link UserError} is thrown.
	 */
	@Override
	public void updateModel(ExampleSet updateExampleSet) throws OperatorException {
		Iterator<Model> i = models.iterator();
		while (i.hasNext()) {
			i.next().updateModel(updateExampleSet);
		}
	}

	@Override
	public String getName() {
		return "GroupedModel";
	}

	/** Adds the given model to the container. */
	public void prependModel(Model model) {
		models.add(0, model);
	}

	/** Adds the given model to the container. */
	public void addModel(Model model) {
		models.add(model);
	}

	/** Removes the given model from the container. */
	public void removeModel(Model model) {
		models.remove(model);
	}

	/** Returns the total number of models. */
	public int getNumberOfModels() {
		return models.size();
	}

	/** Returns the i-th model. */
	public Model getModel(int index) {
		return models.get(index);
	}

	/**
	 * Returns the first model in this container with the desired class. A cast is not necessary.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Model> T getModel(Class<T> desiredClass) {
		Iterator<Model> i = models.iterator();
		while (i.hasNext()) {
			Model model = i.next();
			if (desiredClass.isAssignableFrom(model.getClass())) {
				return (T) model;
			}
		}
		return null;
	}

	/**
	 * Invokes the method for all models. Please note that this method will only throw an exception
	 * if no model was able to handle the given parameter.
	 */
	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		boolean ok = false;
		Iterator<Model> i = models.iterator();
		while (i.hasNext()) {
			try {
				i.next().setParameter(key, value);
				ok = true;
			} catch (OperatorException e) {
			}
		}
		if (!ok) {
			throw new UnsupportedApplicationParameterError(null, getName(), key);
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("Model [");
		for (int i = 0; i < getNumberOfModels(); i++) {
			if (i != 0) {
				result.append(", ");
			}
			result.append(getModel(i).toString());
		}
		result.append("]");
		return result.toString();
	}

	@Override
	public String toResultString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < getNumberOfModels(); i++) {
			result.append(i + 1 + ". " + getModel(i).toResultString() + Tools.getLineSeparator());
		}
		return result.toString();
	}

	@Override
	public List<String> getModelNames() {
		LinkedList<String> modelNames = new LinkedList<String>();
		for (Model model : models) {
			modelNames.add(model.getName());
		}
		return modelNames;
	}

	@Override
	public List<Model> getModels() {
		return models;
	}
}
