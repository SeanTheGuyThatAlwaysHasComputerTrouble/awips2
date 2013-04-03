//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.06.28 at 09:07:34 AM EDT 
//


package gov.noaa.nws.ncep.ui.pgen.file;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Color" maxOccurs="unbounded"/>
 *         &lt;element ref="{}Point" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="otlkCondsEndg" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="otlkCondsDvlpg" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="condsContg" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fromCondsEndg" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fromCondsDvlpg" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="airmetTag" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="outlookEndTime" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="untilTime" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="issueTime" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="textVor" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="otherValues" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="contour" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="gr" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="frequency" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tsCategory" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fzlRange" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="level" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="intensity" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="speed" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="dueTo" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lyr" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="coverage" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fzlTopBottom" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="bottom" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="top" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="states" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ending" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="beginning" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="area" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="cycleHour" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="cycleDay" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="isOutlook" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="issueType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="desk" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tag" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="fcstHr" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="hazard" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lonText" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="latText" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="fillPattern" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="filled" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="closed" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="smoothFactor" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="sizeScale" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="lineWidth" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="pgenType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="pgenCategory" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "color",
    "point"
})
@XmlRootElement(name = "Gfa")
public class Gfa {

    @XmlElement(name = "Color", required = true)
    protected List<Color> color;
    @XmlElement(name = "Point", required = true)
    protected List<Point> point;
    @XmlAttribute
    protected String otlkCondsEndg;
    @XmlAttribute
    protected String otlkCondsDvlpg;
    @XmlAttribute
    protected String condsContg;
    @XmlAttribute
    protected String fromCondsEndg;
    @XmlAttribute
    protected String fromCondsDvlpg;
    @XmlAttribute
    protected String airmetTag;
    @XmlAttribute
    protected String outlookEndTime;
    @XmlAttribute
    protected String untilTime;
    @XmlAttribute
    protected String issueTime;
    @XmlAttribute
    protected String textVor;
    @XmlAttribute
    protected String otherValues;
    @XmlAttribute
    protected String contour;
    @XmlAttribute
    protected String gr;
    @XmlAttribute
    protected String frequency;
    @XmlAttribute
    protected String tsCategory;
    @XmlAttribute
    protected String fzlRange;
    @XmlAttribute
    protected String level;
    @XmlAttribute
    protected String intensity;
    @XmlAttribute
    protected String speed;
    @XmlAttribute
    protected String dueTo;
    @XmlAttribute
    protected String lyr;
    @XmlAttribute
    protected String coverage;
    @XmlAttribute
    protected String fzlTopBottom;
    @XmlAttribute
    protected String cig;
    @XmlAttribute
    protected String vis;
    @XmlAttribute
    protected String bottom;
    @XmlAttribute
    protected String top;
    @XmlAttribute
    protected String states;
    @XmlAttribute
    protected String ending;
    @XmlAttribute
    protected String beginning;
    @XmlAttribute
    protected String area;
    @XmlAttribute
    protected String type;
    @XmlAttribute
    protected Integer cycleHour;
    @XmlAttribute
    protected Integer cycleDay;
    @XmlAttribute
    protected Boolean isOutlook;
    @XmlAttribute
    protected String issueType;
    @XmlAttribute
    protected String desk;
    @XmlAttribute
    protected String tag;
    @XmlAttribute
    protected String fcstHr;
    @XmlAttribute
    protected String hazard;
    @XmlAttribute
    protected Double lonText;
    @XmlAttribute
    protected Double latText;
    @XmlAttribute
    protected String fillPattern;
    @XmlAttribute
    protected Boolean filled;
    @XmlAttribute
    protected Boolean closed;
    @XmlAttribute
    protected Integer smoothFactor;
    @XmlAttribute
    protected Double sizeScale;
    @XmlAttribute
    protected Float lineWidth;
    @XmlAttribute
    protected String pgenType;
    @XmlAttribute
    protected String pgenCategory;

    /**
     * Gets the value of the color property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the color property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getColor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Color }
     * 
     * 
     */
    public List<Color> getColor() {
        if (color == null) {
            color = new ArrayList<Color>();
        }
        return this.color;
    }

    /**
     * Gets the value of the point property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the point property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Point }
     * 
     * 
     */
    public List<Point> getPoint() {
        if (point == null) {
            point = new ArrayList<Point>();
        }
        return this.point;
    }

    /**
     * Gets the value of the otlkCondsEndg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtlkCondsEndg() {
        return otlkCondsEndg;
    }

    /**
     * Sets the value of the otlkCondsEndg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtlkCondsEndg(String value) {
        this.otlkCondsEndg = value;
    }

    /**
     * Gets the value of the otlkCondsDvlpg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtlkCondsDvlpg() {
        return otlkCondsDvlpg;
    }

    /**
     * Sets the value of the otlkCondsDvlpg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtlkCondsDvlpg(String value) {
        this.otlkCondsDvlpg = value;
    }

    /**
     * Gets the value of the condsContg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCondsContg() {
        return condsContg;
    }

    /**
     * Sets the value of the condsContg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCondsContg(String value) {
        this.condsContg = value;
    }

    /**
     * Gets the value of the fromCondsEndg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFromCondsEndg() {
        return fromCondsEndg;
    }

    /**
     * Sets the value of the fromCondsEndg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFromCondsEndg(String value) {
        this.fromCondsEndg = value;
    }

    /**
     * Gets the value of the fromCondsDvlpg property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFromCondsDvlpg() {
        return fromCondsDvlpg;
    }

    /**
     * Sets the value of the fromCondsDvlpg property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFromCondsDvlpg(String value) {
        this.fromCondsDvlpg = value;
    }

    /**
     * Gets the value of the airmetTag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAirmetTag() {
        return airmetTag;
    }

    /**
     * Sets the value of the airmetTag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAirmetTag(String value) {
        this.airmetTag = value;
    }
    /**
     * Gets the value of the outlookEndTime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOutlookEndTime() {
        return outlookEndTime;
    }

    /**
     * Sets the value of the outlookEndTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOutlookEndTime(String value) {
        this.outlookEndTime = value;
    }
    /**
     * Gets the value of the untilTime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUntilTime() {
        return untilTime;
    }

    /**
     * Sets the value of the untilTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUntilTime(String value) {
        this.untilTime = value;
    }

    /**
     * Gets the value of the issueTime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIssueTime() {
        return issueTime;
    }

    /**
     * Sets the value of the issueTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIssueTime(String value) {
        this.issueTime = value;
    }

    /**
     * Gets the value of the textVor property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTextVor() {
        return textVor;
    }

    /**
     * Sets the value of the textVor property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTextVor(String value) {
        this.textVor = value;
    }

    /**
     * Gets the value of the otherValues property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOtherValues() {
        return otherValues;
    }

    /**
     * Sets the value of the otherValues property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOtherValues(String value) {
        this.otherValues = value;
    }

    /**
     * Gets the value of the contour property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContour() {
        return contour;
    }

    /**
     * Sets the value of the contour property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContour(String value) {
        this.contour = value;
    }

    /**
     * Gets the value of the gr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGr() {
        return gr;
    }

    /**
     * Sets the value of the gr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGr(String value) {
        this.gr = value;
    }

    /**
     * Gets the value of the frequency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     * Sets the value of the frequency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFrequency(String value) {
        this.frequency = value;
    }

    /**
     * Gets the value of the tsCategory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTsCategory() {
        return tsCategory;
    }

    /**
     * Sets the value of the tsCategory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTsCategory(String value) {
        this.tsCategory = value;
    }

    /**
     * Gets the value of the fzlRange property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFzlRange() {
        return fzlRange;
    }

    /**
     * Sets the value of the fzlRange property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFzlRange(String value) {
        this.fzlRange = value;
    }

    /**
     * Gets the value of the level property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the value of the level property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLevel(String value) {
        this.level = value;
    }

    /**
     * Gets the value of the intensity property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIntensity() {
        return intensity;
    }

    /**
     * Sets the value of the intensity property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIntensity(String value) {
        this.intensity = value;
    }

    /**
     * Gets the value of the speed property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSpeed() {
        return speed;
    }

    /**
     * Sets the value of the speed property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSpeed(String value) {
        this.speed = value;
    }

    /**
     * Gets the value of the dueTo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDueTo() {
        return dueTo;
    }

    /**
     * Sets the value of the dueTo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDueTo(String value) {
        this.dueTo = value;
    }

    /**
     * Gets the value of the lyr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLyr() {
        return lyr;
    }

    /**
     * Sets the value of the lyr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLyr(String value) {
        this.lyr = value;
    }

    /**
     * Gets the value of the coverage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCoverage() {
        return coverage;
    }

    /**
     * Sets the value of the coverage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCoverage(String value) {
        this.coverage = value;
    }

    /**
     * Gets the value of the fzlTopBottom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFzlTopBottom() {
        return fzlTopBottom;
    }

    /**
     * Sets the value of the fzlTopBottom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFzlTopBottom(String value) {
        this.fzlTopBottom = value;
    }

    /**
     * Gets the value of the CIG property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCig() {
        return cig;
    }

    /**
     * Sets the value of the CIG property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCig(String value) {
        this.cig = value;
    }
    /**
     * Gets the value of the VIS property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVis() {
        return vis;
    }

    /**
     * Sets the value of the VIS property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVis(String value) {
        this.vis = value;
    }
    /**
     * Gets the value of the bottom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBottom() {
        return bottom;
    }

    /**
     * Sets the value of the bottom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBottom(String value) {
        this.bottom = value;
    }

    /**
     * Gets the value of the top property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTop() {
        return top;
    }

    /**
     * Sets the value of the top property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTop(String value) {
        this.top = value;
    }

    /**
     * Gets the value of the states property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStates() {
        return states;
    }

    /**
     * Sets the value of the states property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStates(String value) {
        this.states = value;
    }

    /**
     * Gets the value of the ending property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEnding() {
        return ending;
    }

    /**
     * Sets the value of the ending property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEnding(String value) {
        this.ending = value;
    }

    /**
     * Gets the value of the beginning property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBeginning() {
        return beginning;
    }

    /**
     * Sets the value of the beginning property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBeginning(String value) {
        this.beginning = value;
    }

    /**
     * Gets the value of the area property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArea() {
        return area;
    }

    /**
     * Sets the value of the area property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArea(String value) {
        this.area = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the cycleHour property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCycleHour() {
        return cycleHour;
    }

    /**
     * Sets the value of the cycleHour property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCycleHour(Integer value) {
        this.cycleHour = value;
    }

    /**
     * Gets the value of the cycleDay property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCycleDay() {
        return cycleDay;
    }

    /**
     * Sets the value of the cycleDay property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCycleDay(Integer value) {
        this.cycleDay = value;
    }

    /**
     * Gets the value of the isOutlook property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsOutlook() {
        return isOutlook;
    }

    /**
     * Sets the value of the isOutlook property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsOutlook(Boolean value) {
        this.isOutlook = value;
    }

    /**
     * Gets the value of the issueType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIssueType() {
        return issueType;
    }

    /**
     * Sets the value of the issueType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIssueType(String value) {
        this.issueType = value;
    }

    /**
     * Gets the value of the desk property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDesk() {
        return desk;
    }

    /**
     * Sets the value of the desk property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDesk(String value) {
        this.desk = value;
    }

    /**
     * Gets the value of the tag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the value of the tag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTag(String value) {
        this.tag = value;
    }

    /**
     * Gets the value of the fcstHr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFcstHr() {
        return fcstHr;
    }

    /**
     * Sets the value of the fcstHr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFcstHr(String value) {
        this.fcstHr = value;
    }

    /**
     * Gets the value of the hazard property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHazard() {
        return hazard;
    }

    /**
     * Sets the value of the hazard property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHazard(String value) {
        this.hazard = value;
    }

    /**
     * Gets the value of the lonText property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getLonText() {
        return lonText;
    }

    /**
     * Sets the value of the lonText property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setLonText(Double value) {
        this.lonText = value;
    }

    /**
     * Gets the value of the latText property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getLatText() {
        return latText;
    }

    /**
     * Sets the value of the latText property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setLatText(Double value) {
        this.latText = value;
    }

    /**
     * Gets the value of the fillPattern property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFillPattern() {
        return fillPattern;
    }

    /**
     * Sets the value of the fillPattern property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFillPattern(String value) {
        this.fillPattern = value;
    }

    /**
     * Gets the value of the filled property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isFilled() {
        return filled;
    }

    /**
     * Sets the value of the filled property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFilled(Boolean value) {
        this.filled = value;
    }

    /**
     * Gets the value of the closed property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isClosed() {
        return closed;
    }

    /**
     * Sets the value of the closed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setClosed(Boolean value) {
        this.closed = value;
    }

    /**
     * Gets the value of the smoothFactor property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getSmoothFactor() {
        return smoothFactor;
    }

    /**
     * Sets the value of the smoothFactor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSmoothFactor(Integer value) {
        this.smoothFactor = value;
    }

    /**
     * Gets the value of the sizeScale property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getSizeScale() {
        return sizeScale;
    }

    /**
     * Sets the value of the sizeScale property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setSizeScale(Double value) {
        this.sizeScale = value;
    }

    /**
     * Gets the value of the lineWidth property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getLineWidth() {
        return lineWidth;
    }

    /**
     * Sets the value of the lineWidth property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setLineWidth(Float value) {
        this.lineWidth = value;
    }

    /**
     * Gets the value of the pgenType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPgenType() {
        return pgenType;
    }

    /**
     * Sets the value of the pgenType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPgenType(String value) {
        this.pgenType = value;
    }

    /**
     * Gets the value of the pgenCategory property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPgenCategory() {
        return pgenCategory;
    }

    /**
     * Sets the value of the pgenCategory property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPgenCategory(String value) {
        this.pgenCategory = value;
    }

}
