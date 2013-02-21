//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.01.10 at 11:00:08 AM CST 
//


package net.opengis.gml._3;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PassThroughOperationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PassThroughOperationType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/gml/3.2}AbstractCoordinateOperationType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.opengis.net/gml/3.2}modifiedCoordinate" maxOccurs="unbounded"/>
 *         &lt;element ref="{http://www.opengis.net/gml/3.2}coordOperation"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.opengis.net/gml/3.2}AggregationAttributeGroup"/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PassThroughOperationType", propOrder = {
    "modifiedCoordinate",
    "coordOperation"
})
public class PassThroughOperationType
    extends AbstractCoordinateOperationType
{

    @XmlElement(required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected List<BigInteger> modifiedCoordinate;
    @XmlElementRef(name = "coordOperation", namespace = "http://www.opengis.net/gml/3.2", type = JAXBElement.class)
    protected JAXBElement<CoordinateOperationPropertyType> coordOperation;
    @XmlAttribute
    protected AggregationType aggregationType;

    /**
     * Gets the value of the modifiedCoordinate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the modifiedCoordinate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getModifiedCoordinate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BigInteger }
     * 
     * 
     */
    public List<BigInteger> getModifiedCoordinate() {
        if (modifiedCoordinate == null) {
            modifiedCoordinate = new ArrayList<BigInteger>();
        }
        return this.modifiedCoordinate;
    }

    /**
     * Gets the value of the coordOperation property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link CoordinateOperationPropertyType }{@code >}
     *     {@link JAXBElement }{@code <}{@link CoordinateOperationPropertyType }{@code >}
     *     {@link JAXBElement }{@code <}{@link CoordinateOperationPropertyType }{@code >}
     *     
     */
    public JAXBElement<CoordinateOperationPropertyType> getCoordOperation() {
        return coordOperation;
    }

    /**
     * Sets the value of the coordOperation property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link CoordinateOperationPropertyType }{@code >}
     *     {@link JAXBElement }{@code <}{@link CoordinateOperationPropertyType }{@code >}
     *     {@link JAXBElement }{@code <}{@link CoordinateOperationPropertyType }{@code >}
     *     
     */
    public void setCoordOperation(JAXBElement<CoordinateOperationPropertyType> value) {
        this.coordOperation = ((JAXBElement<CoordinateOperationPropertyType> ) value);
    }

    /**
     * Gets the value of the aggregationType property.
     * 
     * @return
     *     possible object is
     *     {@link AggregationType }
     *     
     */
    public AggregationType getAggregationType() {
        return aggregationType;
    }

    /**
     * Sets the value of the aggregationType property.
     * 
     * @param value
     *     allowed object is
     *     {@link AggregationType }
     *     
     */
    public void setAggregationType(AggregationType value) {
        this.aggregationType = value;
    }

}
