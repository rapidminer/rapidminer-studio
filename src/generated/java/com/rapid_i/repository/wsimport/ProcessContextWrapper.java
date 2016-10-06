/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapid_i.repository.wsimport;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for processContextWrapper complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="processContextWrapper">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="inputRepositoryLocations" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="macros" type="{http://service.web.rapidanalytics.de/}macroDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="outputRepositoryLocations" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processContextWrapper", propOrder = {
    "inputRepositoryLocations",
    "macros",
    "outputRepositoryLocations"
})
public class ProcessContextWrapper {

    @XmlElement(nillable = true)
    protected List<String> inputRepositoryLocations;
    @XmlElement(nillable = true)
    protected List<MacroDefinition> macros;
    @XmlElement(nillable = true)
    protected List<String> outputRepositoryLocations;

    /**
     * Gets the value of the inputRepositoryLocations property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputRepositoryLocations property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputRepositoryLocations().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getInputRepositoryLocations() {
        if (inputRepositoryLocations == null) {
            inputRepositoryLocations = new ArrayList<String>();
        }
        return this.inputRepositoryLocations;
    }

    /**
     * Gets the value of the macros property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the macros property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMacros().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MacroDefinition }
     * 
     * 
     */
    public List<MacroDefinition> getMacros() {
        if (macros == null) {
            macros = new ArrayList<MacroDefinition>();
        }
        return this.macros;
    }

    /**
     * Gets the value of the outputRepositoryLocations property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the outputRepositoryLocations property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOutputRepositoryLocations().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getOutputRepositoryLocations() {
        if (outputRepositoryLocations == null) {
            outputRepositoryLocations = new ArrayList<String>();
        }
        return this.outputRepositoryLocations;
    }

}
