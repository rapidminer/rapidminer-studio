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

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.RMUrlHandler;


/**
 * Meta data about an {@link IOObject}. Includes the specific class of the IOObject plus a map of
 * key-value pairs specifying more detailed properties. Additionally, may contain information about
 * which {@link OutputPort} originally generated this meta data. <br/>
 * 
 * Subclasses representing the meta data for a class T (in particular those defined by plugins),
 * should implement a constructor accepting a T and a boolean and be registered with
 * {@link MetaDataFactory#registerIOObjectMetaData(Class, Class)}.
 * 
 * @author Simon Fischer
 */
public class MetaData implements Serializable {

	private static final long serialVersionUID = 1L;

	/** A list of ports that have generated or modified this meta data. */
	private transient LinkedList<OutputPort> generationHistory = new LinkedList<OutputPort>();

	/** Maps keys (MD_KEY_...) to values. */
	private final Map<String, Object> keyValueMap = new HashMap<String, Object>();

	private Class<? extends IOObject> dataClass;

	private Annotations annotations = new Annotations();

	public MetaData() {
		this(IOObject.class);
	}

	/**
	 * Restores an empty history.
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public Object readResolve() throws ObjectStreamException {
		// in.defaultReadObject();
		if (generationHistory == null) {
			generationHistory = new LinkedList<OutputPort>();
		}
		if (annotations == null) {
			annotations = new Annotations();
		}
		return this;
	}

	public MetaData(Class<? extends IOObject> dataClass) {
		this(dataClass, Collections.<String, Object> emptyMap());
	}

	public MetaData(Class<? extends IOObject> dataClass, String key, Object value) {
		this(dataClass, Collections.singletonMap(key, value));
	}

	public MetaData(Class<? extends IOObject> dataClass, Map<String, Object> keyValueMap) {
		this.dataClass = dataClass;
		this.keyValueMap.putAll(keyValueMap);
	}

	public void addToHistory(OutputPort generator) {
		this.generationHistory.addFirst(generator);
	}

	public List<OutputPort> getGenerationHistory() {
		return Collections.unmodifiableList(generationHistory);
	}

	public String getGenerationHistoryAsHTML() {
		boolean first = true;
		StringBuilder b = new StringBuilder();
		if (generationHistory != null) {
			for (OutputPort port : generationHistory) {
				if (!first) {
					b.append(" &#8592; ");
				}
				b.append("<a href=\"" + RMUrlHandler.URL_PREFIX + "operator/");
				b.append(port.getPorts().getOwner().getOperator().getName());
				b.append("\">");
				b.append(port.getSpec());
				b.append("</a>");
				first = false;
			}
		}
		return b.toString();
	}

	public Class<? extends IOObject> getObjectClass() {
		return dataClass;
	}

	public Object getMetaData(String key) {
		return keyValueMap.get(key);
	}

	public Object putMetaData(String key, Object value) {
		return keyValueMap.put(key, value);
	}

	@Override
	public MetaData clone() {
		MetaData clone;
		try {
			clone = this.getClass().newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot clone " + this, e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot clone " + this, e);
		}
		if (generationHistory == null) {
			clone.generationHistory = new LinkedList<OutputPort>();
		} else {
			clone.generationHistory = new LinkedList<OutputPort>(this.generationHistory);
		}
		clone.dataClass = this.getObjectClass();
		clone.keyValueMap.putAll(this.keyValueMap);
		if (this.annotations != null) {
			clone.annotations.putAll(this.annotations);
		}
		return clone;
	}

	@Override
	public String toString() {
		return getObjectClass().getSimpleName() + (keyValueMap.isEmpty() ? "" : (" hints: " + keyValueMap.toString()));
	}

	public String getDescription() {
		String name = getTitleForDescription();
		if (name == null) {
			name = dataClass.getSimpleName();
		}
		StringBuilder desc = new StringBuilder(name);
		if (!keyValueMap.isEmpty()) {
			desc.append("; ");
			desc.append(keyValueMap);
		}
		if ((annotations != null) && !annotations.isEmpty()) {
			desc.append("<ul>");
			for (String key : annotations.getKeys()) {
				desc.append("<li><em>").append(key).append(":</em> ").append(annotations.get(key));
			}
			desc.append("</ul>");
		}
		return desc.toString();
	}

	/**
	 * Returns the title that is used in the {@link #getDescription()} method
	 * <p>The default implementation checks {@link RendererService#getName}</p>
	 * <p>If this method returns {@code null}, the {@link Class#getSimpleName()} of the data class is used.</p>
	 *
	 * @return the description title, might contain html
	 */
	protected String getTitleForDescription() {
		return RendererService.getName(dataClass);
	}

	/**
	 * Returns true if isData is compatible with this meta data, where <code>this</code> represents
	 * desired meta data and isData represents meta data that was actually delivered.
	 */
	public boolean isCompatible(MetaData isData, CompatibilityLevel level) {
		return getErrorsForInput(null, isData, level).isEmpty();
	}

	/**
	 * Returns a (possibly empty) list of errors specifying in what regard <code>isData</code>
	 * differs from <code>this</code> meta data specification.
	 * 
	 * @param inputPort
	 *            required for generating errors
	 * @param isData
	 *            the data received by the port
	 */
	public Collection<MetaDataError> getErrorsForInput(InputPort inputPort, MetaData isData, CompatibilityLevel level) {
		if (!this.dataClass.isAssignableFrom(isData.dataClass) &&
				!AtPortConverter.isConvertible(this.dataClass, isData.dataClass)) {
			return Collections.<MetaDataError> singletonList(new InputMissingMetaDataError(inputPort, this.getObjectClass(),
					isData.getObjectClass()));
		}
		Collection<MetaDataError> errors = new LinkedList<MetaDataError>();
		if (level == CompatibilityLevel.VERSION_5) {
			for (Map.Entry<String, Object> entry : this.keyValueMap.entrySet()) {
				Object isValue = isData.keyValueMap.get(entry.getKey());
				if (!entry.getValue().equals(isValue)) {
					errors.add(new SimpleMetaDataError(Severity.ERROR, inputPort, "general_property_mismatch", new Object[] {
							entry.getKey(), entry.getValue() }));
				}
			}
		}
		return errors;
	}

	/**
	 * This will return the meta data description of the given IOObject. If the shortened flag is
	 * true, the meta data will be incomplete to avoid to generate too much data if this is
	 * supported by the actual meta data implementation.
	 */
	public static MetaData forIOObject(IOObject ioo, boolean shortened) {
		return MetaDataFactory.getInstance().createMetaDataforIOObject(ioo, shortened);
	}

	public static MetaData forIOObject(IOObject ioo) {
		return forIOObject(ioo, false);
	}

	public Annotations getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	/**
	 * Shrinks the values of the meta data, if possible. For now, this is only done for number of nominal values in
	 * {@link ExampleSetMetaData}.
	 *
	 * @param metaData
	 * 		the meta data to try to shrink
	 * @since 9.3.2
	 */
	public static void shrinkValues(MetaData metaData) {
		if (metaData instanceof ExampleSetMetaData) {
			for (AttributeMetaData amd : ((ExampleSetMetaData) metaData).getAllAttributes()) {
				if (amd.isNominal()) {
					amd.shrinkValueSet();
				}
			}
		}
	}
}
