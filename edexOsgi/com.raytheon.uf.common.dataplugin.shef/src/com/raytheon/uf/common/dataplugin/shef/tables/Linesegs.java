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

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Linesegs generated by hbm2java
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 17, 2008                        Initial generation by hbm2java
 * Aug 19, 2011      10672     jkorman Move refactor to new project
 * Oct 07, 2013       2361     njensen Removed XML annotations
 * 
 * </pre>
 * 
 * @author jkorman
 * @version 1.1
 */
@Entity
@Table(name = "linesegs")
@com.raytheon.uf.common.serialization.annotations.DynamicSerialize
public class Linesegs extends com.raytheon.uf.common.dataplugin.persist.PersistableDataObject implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private LinesegsId id;

    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Geoarea geoarea;

    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Integer hrapEndCol;

    @com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement
    private Double area;

    public Linesegs() {
    }

    public Linesegs(LinesegsId id, Geoarea geoarea) {
        this.id = id;
        this.geoarea = geoarea;
    }

    public Linesegs(LinesegsId id, Geoarea geoarea, Integer hrapEndCol,
            Double area) {
        this.id = id;
        this.geoarea = geoarea;
        this.hrapEndCol = hrapEndCol;
        this.area = area;
    }

    @EmbeddedId
    @AttributeOverrides( {
            @AttributeOverride(name = "areaId", column = @Column(name = "area_id", nullable = false, length = 8)),
            @AttributeOverride(name = "hrapRow", column = @Column(name = "hrap_row", nullable = false)),
            @AttributeOverride(name = "hrapBegCol", column = @Column(name = "hrap_beg_col", nullable = false)) })
    public LinesegsId getId() {
        return this.id;
    }

    public void setId(LinesegsId id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "area_id", nullable = false, insertable = false, updatable = false)
    public Geoarea getGeoarea() {
        return this.geoarea;
    }

    public void setGeoarea(Geoarea geoarea) {
        this.geoarea = geoarea;
    }

    @Column(name = "hrap_end_col")
    public Integer getHrapEndCol() {
        return this.hrapEndCol;
    }

    public void setHrapEndCol(Integer hrapEndCol) {
        this.hrapEndCol = hrapEndCol;
    }

    @Column(name = "area", precision = 17, scale = 17)
    public Double getArea() {
        return this.area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

}
