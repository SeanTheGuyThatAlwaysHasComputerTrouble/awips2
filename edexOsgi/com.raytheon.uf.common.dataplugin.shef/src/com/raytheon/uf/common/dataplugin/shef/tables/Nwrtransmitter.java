/**
* This software was developed and / or modified by Raytheon Company,
* pursuant to Contract DG133W-05-CQ-1067 with the US Government.
* 
* U.S. EXPORT CONTROLLED TECHNICAL DATA
* This software product contains export-restricted data whose
* export/transfer/disclosure is restricted by U.S. law. Dissemination
* to non-U.S. persons whether in the United States or abroad requires
* an export license or other authorization.
* 
* Contractor Name:        Raytheon Company
* Contractor Address:     6825 Pine Street, Suite 340
*                         Mail Stop B8
*                         Omaha, NE 68106
*                         402.291.0100
* 
* See the AWIPS II Master Rights File ("Master Rights File.pdf") for
* further licensing information.
**/
package com.raytheon.uf.common.dataplugin.shef.tables;
// default package
// Generated Oct 17, 2008 2:22:17 PM by Hibernate Tools 3.2.2.GA

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Nwrtransmitter generated by hbm2java
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 17, 2008                        Initial generation by hbm2java
 * Aug 19, 2011      10672     jkorman Move refactor to new project
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.1
 */
@Entity
@Table(name = "nwrtransmitter")
@javax.xml.bind.annotation.XmlRootElement
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.NONE)
@com.raytheon.uf.common.serialization.annotations.DynamicSerialize
public class Nwrtransmitter extends com.raytheon.uf.common.dataplugin.persist.PersistableDataObject implements java.io.Serializable, com.raytheon.uf.common.serialization.ISerializableObject {

    private static final long serialVersionUID = 1L;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String callSign;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private State state;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Wfo wfo;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String city;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String county;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String coverageArea;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Double lat;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Double lon;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Double transmitFreq;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Integer transmitPower;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String transmitProdCode;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String transmitCountynum;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String useTransmitter;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Set<Counties> countieses = new HashSet<Counties>(0);

    public Nwrtransmitter() {
    }

    public Nwrtransmitter(String callSign) {
        this.callSign = callSign;
    }

    public Nwrtransmitter(String callSign, State state, Wfo wfo, String city,
            String county, String coverageArea, Double lat, Double lon,
            Double transmitFreq, Integer transmitPower,
            String transmitProdCode, String transmitCountynum,
            String useTransmitter, Set<Counties> countieses) {
        this.callSign = callSign;
        this.state = state;
        this.wfo = wfo;
        this.city = city;
        this.county = county;
        this.coverageArea = coverageArea;
        this.lat = lat;
        this.lon = lon;
        this.transmitFreq = transmitFreq;
        this.transmitPower = transmitPower;
        this.transmitProdCode = transmitProdCode;
        this.transmitCountynum = transmitCountynum;
        this.useTransmitter = useTransmitter;
        this.countieses = countieses;
    }

    @Id
    @Column(name = "call_sign", unique = true, nullable = false, length = 6)
    public String getCallSign() {
        return this.callSign;
    }

    public void setCallSign(String callSign) {
        this.callSign = callSign;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "state")
    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "wfo")
    public Wfo getWfo() {
        return this.wfo;
    }

    public void setWfo(Wfo wfo) {
        this.wfo = wfo;
    }

    @Column(name = "city", length = 20)
    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Column(name = "county", length = 20)
    public String getCounty() {
        return this.county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    @Column(name = "coverage_area", length = 25)
    public String getCoverageArea() {
        return this.coverageArea;
    }

    public void setCoverageArea(String coverageArea) {
        this.coverageArea = coverageArea;
    }

    @Column(name = "lat", precision = 17, scale = 17)
    public Double getLat() {
        return this.lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    @Column(name = "lon", precision = 17, scale = 17)
    public Double getLon() {
        return this.lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    @Column(name = "transmit_freq", precision = 17, scale = 17)
    public Double getTransmitFreq() {
        return this.transmitFreq;
    }

    public void setTransmitFreq(Double transmitFreq) {
        this.transmitFreq = transmitFreq;
    }

    @Column(name = "transmit_power")
    public Integer getTransmitPower() {
        return this.transmitPower;
    }

    public void setTransmitPower(Integer transmitPower) {
        this.transmitPower = transmitPower;
    }

    @Column(name = "transmit_prod_code", length = 3)
    public String getTransmitProdCode() {
        return this.transmitProdCode;
    }

    public void setTransmitProdCode(String transmitProdCode) {
        this.transmitProdCode = transmitProdCode;
    }

    @Column(name = "transmit_countynum", length = 4)
    public String getTransmitCountynum() {
        return this.transmitCountynum;
    }

    public void setTransmitCountynum(String transmitCountynum) {
        this.transmitCountynum = transmitCountynum;
    }

    @Column(name = "use_transmitter", length = 1)
    public String getUseTransmitter() {
        return this.useTransmitter;
    }

    public void setUseTransmitter(String useTransmitter) {
        this.useTransmitter = useTransmitter;
    }

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "countytransmit", joinColumns = { @JoinColumn(name = "call_sign", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "county", nullable = false, updatable = false),@JoinColumn(name = "state", nullable = false, updatable = false) })
    public Set<Counties> getCountieses() {
        return this.countieses;
    }

    public void setCountieses(Set<Counties> countieses) {
        this.countieses = countieses;
    }

}
