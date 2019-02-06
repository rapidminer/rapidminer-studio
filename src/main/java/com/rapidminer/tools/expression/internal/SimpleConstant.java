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
package com.rapidminer.tools.expression.internal;

import java.util.Date;

import com.rapidminer.tools.expression.Constant;
import com.rapidminer.tools.expression.ExpressionType;


/**
 * A {@link Constant} that supplies constructors for all admissible combinations of its fields.
 *
 * @author Gisa Schaefer
 *
 */
public class SimpleConstant implements Constant {

	private final ExpressionType type;
	private final String name;
	private final String stringValue;
	private final double doubleValue;
	private final boolean booleanValue;
	private final Date dateValue;
	private String annotation;
	private boolean invisible = false;

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringValue
	 *            the string value
	 */
	public SimpleConstant(String name, String stringValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.STRING;
		this.name = name;
		this.stringValue = stringValue;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.dateValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringValue
	 *            the string value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, String stringValue, String annotation) {
		this(name, stringValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringValue
	 *            the string value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, String stringValue, String annotation, boolean invisible) {
		this(name, stringValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param doubleValue
	 *            the double value
	 */
	public SimpleConstant(String name, double doubleValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.DOUBLE;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = doubleValue;
		this.booleanValue = false;
		this.dateValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param doubleValue
	 *            the double value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, double doubleValue, String annotation) {
		this(name, doubleValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param doubleValue
	 *            the double value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, double doubleValue, String annotation, boolean invisible) {
		this(name, doubleValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param booleanValue
	 *            the boolean value
	 */
	public SimpleConstant(String name, boolean booleanValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.BOOLEAN;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = booleanValue;
		this.dateValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param booleanValue
	 *            the boolean value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, boolean booleanValue, String annotation) {
		this(name, booleanValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param booleanValue
	 *            the boolean value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, boolean booleanValue, String annotation, boolean invisible) {
		this(name, booleanValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param dateValue
	 *            the date value
	 */
	public SimpleConstant(String name, Date dateValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.DATE;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.dateValue = dateValue;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param dateValue
	 *            the date value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, Date dateValue, String annotation) {
		this(name, dateValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param dateValue
	 *            the date value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, Date dateValue, String annotation, boolean invisible) {
		this(name, dateValue, annotation);
		this.invisible = invisible;
	}

	@Override
	public ExpressionType getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getStringValue() {
		if (type == ExpressionType.STRING) {
			return stringValue;
		} else {
			throw new IllegalStateException("element is not of type String");
		}
	}

	@Override
	public double getDoubleValue() {
		if (type == ExpressionType.DOUBLE) {
			return doubleValue;
		} else {
			throw new IllegalStateException("element is not of type double");
		}
	}

	@Override
	public boolean getBooleanValue() {
		if (type == ExpressionType.BOOLEAN) {
			return booleanValue;
		} else {
			throw new IllegalStateException("element is not of type boolean");
		}
	}

	@Override
	public Date getDateValue() {
		if (type == ExpressionType.DATE) {
			return dateValue;
		} else {
			throw new IllegalStateException("element is not of type Date");
		}
	}

	@Override
	public String getAnnotation() {
		return annotation;
	}

	/**
	 * Sets the annotation of this constant.
	 *
	 * @param annotation
	 *            the annotation to set
	 */
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	@Override
	public boolean isInvisible() {
		return invisible;
	}

}
