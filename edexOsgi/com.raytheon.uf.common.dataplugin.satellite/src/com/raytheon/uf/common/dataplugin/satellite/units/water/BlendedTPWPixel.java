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
package com.raytheon.uf.common.dataplugin.satellite.units.water;

import javax.measure.converter.AddConverter;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.unit.DerivedUnit;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents a pixel value on a satellite blended total precipitable water(TPW)
 * image.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Mar 18, 2010           jsanchez    Initial creation
 * Nov 20, 2013  2492     bsteffen    Make conversion unbounded.
 * 
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */

public class BlendedTPWPixel extends DerivedUnit<Length> {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object anObject) {
        return (anObject instanceof PrecipPixel);

    }

    @Override
    public Unit<Length> getStandardUnit() {
        return SI.METER;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public UnitConverter toStandardUnit() {
        return SI.MILLI(SI.METRE).getConverterTo(SI.METRE)
                .concatenate(new AddConverter(-176));
    }
}
