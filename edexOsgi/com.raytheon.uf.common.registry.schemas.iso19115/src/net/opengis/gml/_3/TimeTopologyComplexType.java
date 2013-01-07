//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.01.10 at 11:00:08 AM CST 
//


package net.opengis.gml._3;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for TimeTopologyComplexType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TimeTopologyComplexType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.opengis.net/gml/3.2}AbstractTimeComplexType">
 *       &lt;sequence>
 *         &lt;element name="primitive" type="{http://www.opengis.net/gml/3.2}TimeTopologyPrimitivePropertyType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TimeTopologyComplexType", propOrder = {
    "primitive"
})
public abstract class TimeTopologyComplexType
    extends AbstractTimeComplexType
{

    @XmlElement(required = true)
    protected List<TimeTopologyPrimitivePropertyType> primitive;

    /**
     * Gets the value of the primitive property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the primitive property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPrimitive().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TimeTopologyPrimitivePropertyType }
     * 
     * 
     */
    public List<TimeTopologyPrimitivePropertyType> getPrimitive() {
        if (primitive == null) {
            primitive = new ArrayList<TimeTopologyPrimitivePropertyType>();
        }
        return this.primitive;
    }

}
