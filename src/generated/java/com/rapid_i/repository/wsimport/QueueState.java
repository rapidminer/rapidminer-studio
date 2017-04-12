/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for queueState complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="queueState">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="backlog" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="numberOfRunningProcesses" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "queueState", propOrder = {
    "backlog",
    "numberOfRunningProcesses"
})
public class QueueState {

    protected int backlog;
    protected int numberOfRunningProcesses;

    /**
     * Gets the value of the backlog property.
     * 
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Sets the value of the backlog property.
     * 
     */
    public void setBacklog(int value) {
        this.backlog = value;
    }

    /**
     * Gets the value of the numberOfRunningProcesses property.
     * 
     */
    public int getNumberOfRunningProcesses() {
        return numberOfRunningProcesses;
    }

    /**
     * Sets the value of the numberOfRunningProcesses property.
     * 
     */
    public void setNumberOfRunningProcesses(int value) {
        this.numberOfRunningProcesses = value;
    }

}
