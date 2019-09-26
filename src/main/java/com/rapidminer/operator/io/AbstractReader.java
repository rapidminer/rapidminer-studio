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
package com.rapidminer.operator.io;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rapidminer.Process;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.MetaDataUpdateQueue;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.DebugMode;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.UserSetupError;
import com.rapidminer.operator.features.weighting.ForestBasedWeighting.RandomForestModelMetaData;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.ConfigurableRandomForestModel;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.ProcessNotInRepositoryMetaDataError;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.SaveProcessQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.io.Encoding;


/**
 * Superclass of all operators that have no input and generate a single output. This class is mainly
 * a tribute to the e-LICO DMO.
 * 
 * @author Simon Fischer, Jan Czogalla
 */
public abstract class AbstractReader<T extends IOObject> extends Operator {

	protected static final String TRANSFORMER_THREAD_KEY = "AbstractReader.transform_metadata";

	private final OutputPort outputPort = getOutputPorts().createPort("output");
	private final Class<? extends IOObject> generatedClass;

	private boolean cacheDirty = true;
	private AtomicBoolean transformationScheduled = new AtomicBoolean();
	private MetaData cachedMetaData;
	private ProcessSetupError cachedError;


	public AbstractReader(OperatorDescription description, Class<? extends IOObject> generatedClass) {
		super(description);
		this.generatedClass = generatedClass;
		ProgressThread mdTransformationThread = createTransformationProgressThread();
		getTransformer().addRule(() -> {
			if (!isDirty() && getProcess() != null && getProcess().getDebugMode() == DebugMode.COLLECT_METADATA_AFTER_EXECUTION
					&& outputPort.getMetaData() != null) {
				return;
			}
			if (!isMetaDataCacheable()) {
				setCachedMetadataAndError();
			} else if (cacheDirty) {
				cachedMetaData = getDefaultMetaData();
				cachedMetaData.addToHistory(outputPort);
				cachedError = null;
				mdTransformationThread.start();
			}
			updateOutputPort();
		});
		observeParameters(mdTransformationThread);
	}

	/**
	 * Creates a {@link ProgressThread} that takes care of transforming the meta data in the background to prevent UI freezes.
	 * This is intended for long running versions of {@link #getGeneratedMetaData()}.
	 *
	 * @see #getGeneratedMetaData()
	 * @see #isMetaDataCacheable()
	 * @since 9.2.0
	 */
	private ProgressThread createTransformationProgressThread() {
		ProgressThread progressThread = new ProgressThread(TRANSFORMER_THREAD_KEY, false, getName()) {

			@Override
			public void start() {
				if (transformationScheduled.compareAndSet(false, true)) {
					MetaDataUpdateQueue.registerMDGeneration(getProcess(), this);
					super.start();
				}
			}

			@Override
			public void run() {
				setCachedMetadataAndError();
				cacheDirty = false;
				updateOutputPort();
				transformationScheduled.set(false);
			}
		};
		progressThread.setDependencyPopups(false);
		progressThread.setIndeterminate(true);
		return progressThread;
	}

	/**
	 * Delivers cached {@link MetaData} and updates {@link #outputPort} with cached {@link MetaDataError} if necessary.
	 *
	 * @since 9.3.1
	 */
	private void updateOutputPort() {
		outputPort.deliverMD(cachedMetaData);
		if (cachedError != null) {
			MetaDataError portError;
			if (cachedError instanceof MetaDataError) {
				portError = (MetaDataError) cachedError;
			} else {
				portError = new SimpleMetaDataError(cachedError.getSeverity(), outputPort, cachedError.getQuickFixes(),
						"cannot_create_exampleset_metadata", cachedError.getMessage());
			}
			outputPort.addError(portError);
		}
	}

	/**
	 * Sets the {@link #cachedMetaData} to the result of {@link #getGeneratedMetaData()} if possible. If an error occurs,
	 * sets the {@link #cachedError}.
	 *
	 * @since 9.2.0
	 */
	private void setCachedMetadataAndError() {
		try {
			cachedMetaData = getGeneratedMetaData();
			cachedError = null;
			if (cachedMetaData == null) {
				cachedMetaData = getDefaultMetaData();
				cachedError = new SimpleMetaDataError(Severity.WARNING, outputPort,
						"cannot_create_exampleset_metadata", new NullPointerException().getLocalizedMessage());
			}
		} catch (OperatorException e) {
			cachedMetaData = getDefaultMetaData();
			String msg = e.getMessage();
			if ((msg == null) || (msg.length() == 0)) {
				msg = e.toString();
			}

			// will be added below
			if (e instanceof UserSetupError) {
				cachedError = ((UserSetupError) e).getSetupError();
			} else if (e instanceof UserError && ((UserError) e).getCode() == 317 && getProcess() != null) {
				cachedError = new ProcessNotInRepositoryMetaDataError(Severity.WARNING, outputPort,
						Collections.singletonList(new SaveProcessQuickFix(getProcess())),
						"save_process", msg);
			} else {
				cachedError = new SimpleMetaDataError(Severity.WARNING, outputPort,
						"cannot_create_exampleset_metadata", msg);
			}
		}
		cachedMetaData.addToHistory(outputPort);
	}

	private void observeParameters(ProgressThread mdTransformationThread) {
		// we add this as the first observer. otherwise, this change is not seen
		// by the resulting meta data transformation
		getParameters().addObserverAsFirst((observable, arg) -> {
			cacheDirty = true;
			if (isMetaDataCacheable() && transformationScheduled.compareAndSet(true, false)) {
				// IMPORTANT NOTE: this only works properly as long as there are no dependencies involved here
				// If any other pg depends on this one, this will trigger a popup
				mdTransformationThread.cancel();
			}
		}, false);
	}

	/**
	 * Returns the generated {@link MetaData} of this reader. This can be a long running operation
	 * iff {@link #isMetaDataCacheable()} returns {@code true}.
	 *
	 * @return the result of {@link #getDefaultMetaData()} by default.
	 * @throws OperatorException
	 * 		if an error occurs
	 * @see #getDefaultMetaData()
	 */
	public MetaData getGeneratedMetaData() throws OperatorException {
		return getDefaultMetaData();
	}

	/**
	 * Returns a basic {@link MetaData} object that can be used as a stand-in even if invalid parameters are chosen.
	 * This method should return immediately and is not suitable for long running operations.
	 * By default this can return any of the core meta data implementations; these are example sets, collections,
	 * models (also specific for {@link PredictionModel} and {@link ConfigurableRandomForestModel}).
	 * For all other {@link IOObject IOObjects} it will return generic meta data.
	 *
	 * @return a basic {@link MetaData} object
	 * @see #getGeneratedMetaData()
	 * @since 9.2.0
	 */
	@SuppressWarnings("unchecked")
	protected MetaData getDefaultMetaData() {
		if (ExampleSet.class.isAssignableFrom(generatedClass)) {
			return new ExampleSetMetaData();
		}
		if (IOObjectCollection.class.isAssignableFrom(generatedClass)) {
			return new CollectionMetaData();
		}
		if (Model.class.isAssignableFrom(generatedClass)) {
			if (PredictionModel.class.isAssignableFrom(generatedClass)) {
				return new PredictionModelMetaData((Class<? extends PredictionModel>) generatedClass);
			}
			if (ConfigurableRandomForestModel.class.isAssignableFrom(generatedClass)) {
				return new RandomForestModelMetaData();
			}
			return new ModelMetaData((Class<? extends Model>) generatedClass, new ExampleSetMetaData());
		}
		return new MetaData(generatedClass);
	}

	/**
	 * Returns whether this reader's {@link MetaData} is cacheable or not. Meta data should be cacheable
	 * where the generation of real meta data is expected to be a long running operation.
	 * As long as the meta data only depends strictly on the parameter values, this should return {@code false}.
	 * An example for long running meta data
	 * @return  {@code false} by default.
	 *
	 * @see #getGeneratedMetaData()
	 */
	protected boolean isMetaDataCacheable() {
		return false;
	}

	/** Creates (or reads) the ExampleSet that will be returned by {@link #apply()}. */
	public abstract T read() throws OperatorException;

	@Override
	public void doWork() throws OperatorException {
		final T result = read();
		addAnnotations(result);
		outputPort.deliver(result);
	}

	protected void addAnnotations(T result) {
		for (ReaderDescription rd : READER_DESCRIPTIONS.values()) {
			if (rd.readerClass.equals(this.getClass())) {
				if (result.getAnnotations().getAnnotation(Annotations.KEY_SOURCE) == null) {
					try {
						String source = getParameter(rd.fileParameterKey);
						if (source != null) {
							result.getAnnotations().setAnnotation(Annotations.KEY_SOURCE, source);
						}
					} catch (UndefinedParameterError e) {
					}
				}
				return;
			}
		}
	}

	/** Describes an operator that can read certain file types. */
	public static class ReaderDescription {

		private final String fileExtension;
		private final Class<? extends AbstractReader<?>> readerClass;
		/** This parameter must be set to the file name. */
		private final String fileParameterKey;

		public ReaderDescription(String fileExtension, Class<? extends AbstractReader<?>> readerClass,
				String fileParameterKey) {
			super();
			this.fileExtension = fileExtension;
			this.readerClass = readerClass;
			this.fileParameterKey = fileParameterKey;
		}
	}

	private static final Map<String, ReaderDescription> READER_DESCRIPTIONS = new HashMap<>();

	/** Registers an operator that can read files with a given extension. */
	protected static void registerReaderDescription(ReaderDescription rd) {
		READER_DESCRIPTIONS.put(rd.fileExtension.toLowerCase(), rd);
	}

	/**
	 * @depreacated call {@link #createReader(URI)}
	 */
	@Deprecated
	public static AbstractReader<?> createReader(URL url) throws OperatorCreationException {
		try {
			return createReader(url.toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException("Failed to convert URI to URL: " + e, e);
		}
	}

	/**
	 * Returns a reader that can read the given file or URL. The type is determined by looking at
	 * the file extension. Only Operators registered via
	 * {@link #registerReaderDescription(ReaderDescription)} will be checked.
	 */
	public static AbstractReader<?> createReader(URI uri) throws OperatorCreationException {
		String fileName = uri.toString();
		int dot = fileName.lastIndexOf('.');
		if (dot == -1) {
			return null;
		} else {
			String extension = fileName.substring(dot + 1).toLowerCase();
			ReaderDescription rd = READER_DESCRIPTIONS.get(extension);
			if (rd == null) {
				return null;
			}

			AbstractReader<?> reader = OperatorService.createOperator(rd.readerClass);
			if (uri.getScheme().equals("file")) {
				// local file
				File file = new File(uri);
				reader.setParameter(rd.fileParameterKey, file.getAbsolutePath());
			} else {
				// remote url
				reader.setParameter(rd.fileParameterKey, uri.toString());
			}

			return reader;
		}
	}

	public static boolean canMakeReaderFor(URL url) {
		String file = url.getFile();
		int dot = file.lastIndexOf('.');
		if (dot == -1) {
			return false;
		} else {
			String extension = file.substring(dot + 1).toLowerCase();
			return READER_DESCRIPTIONS.containsKey(extension);
		}
	}

	/** Returns the key of the parameter that specifies the file to be read. */
	public static String getFileParameterForOperator(Operator operator) {
		for (ReaderDescription rd : READER_DESCRIPTIONS.values()) {
			if (rd.readerClass.equals(operator.getClass())) {
				return rd.fileParameterKey;
			}
		}
		return null;
	}

	@Override
	protected void registerOperator(Process process) {
		super.registerOperator(process);
		cacheDirty = true;
	}

	/**
	 * Returns the cached {@link MetaData}, if any
	 *
	 * @return the cached meta data; might be {@code null}
	 * @see #isMetaDataCacheable()
	 * @since 9.3
	 */
	protected MetaData getCachedMetaData() {
		return cachedMetaData;
	}

	protected boolean supportsEncoding() {
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		if (supportsEncoding()) {
			types.addAll(Encoding.getParameterTypes(this));
		}
		return types;
	}
}
