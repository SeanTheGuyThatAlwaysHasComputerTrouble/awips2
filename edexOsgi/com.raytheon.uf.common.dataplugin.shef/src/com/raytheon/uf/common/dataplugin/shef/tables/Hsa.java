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
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Hsa generated by hbm2java
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
@Table(name = "hsa")
@javax.xml.bind.annotation.XmlRootElement
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.NONE)
@com.raytheon.uf.common.serialization.annotations.DynamicSerialize
public class Hsa extends com.raytheon.uf.common.dataplugin.persist.PersistableDataObject implements java.io.Serializable, com.raytheon.uf.common.serialization.ISerializableObject {

    private static final long serialVersionUID = 1L;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private String hsa;

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Set<Rpffcstpoint> rpffcstpointsForSecondaryBack = new HashSet<Rpffcstpoint>(
            0);

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Set<Rpffcstpoint> rpffcstpointsForPrimaryBack = new HashSet<Rpffcstpoint>(
            0);

    @javax.xml.bind.annotation.XmlElement
    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Set<Location> locations = new HashSet<Location>(0);

    public Hsa() {
    }

    public Hsa(String hsa) {
        this.hsa = hsa;
    }

    public Hsa(String hsa, Set<Rpffcstpoint> rpffcstpointsForSecondaryBack,
            Set<Rpffcstpoint> rpffcstpointsForPrimaryBack,
            Set<Location> locations) {
        this.hsa = hsa;
        this.rpffcstpointsForSecondaryBack = rpffcstpointsForSecondaryBack;
        this.rpffcstpointsForPrimaryBack = rpffcstpointsForPrimaryBack;
        this.locations = locations;
    }

    @Id
    @Column(name = "hsa", unique = true, nullable = false, length = 3)
    public String getHsa() {
        return this.hsa;
    }

    public void setHsa(String hsa) {
        this.hsa = hsa;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hsaBySecondaryBack")
    public Set<Rpffcstpoint> getRpffcstpointsForSecondaryBack() {
        return this.rpffcstpointsForSecondaryBack;
    }

    public void setRpffcstpointsForSecondaryBack(
            Set<Rpffcstpoint> rpffcstpointsForSecondaryBack) {
        this.rpffcstpointsForSecondaryBack = rpffcstpointsForSecondaryBack;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hsaByPrimaryBack")
    public Set<Rpffcstpoint> getRpffcstpointsForPrimaryBack() {
        return this.rpffcstpointsForPrimaryBack;
    }

    public void setRpffcstpointsForPrimaryBack(
            Set<Rpffcstpoint> rpffcstpointsForPrimaryBack) {
        this.rpffcstpointsForPrimaryBack = rpffcstpointsForPrimaryBack;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hsa")
    public Set<Location> getLocations() {
        return this.locations;
    }

    public void setLocations(Set<Location> locations) {
        this.locations = locations;
    }

}
