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
package com.raytheon.uf.edex.bufrtools;

import java.nio.ByteBuffer;

/**
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 20071127            382 jkorman     Initial Coding.
 * 9/16/2014    #3628      mapeters    Moved from uf.edex.decodertools plugin.
 * </pre>
 * 
 * @author jkorman
 * @version 1.0
 */
public class BUFRSection2 extends BUFRSection {

    BUFRSection2(ByteBuffer dataBuffer) {
        super(dataBuffer);
        setValidity(true);
    }

    public StringBuilder getStringData(StringBuilder buffer) {
        buffer = super.getStringData(buffer);
        buffer.append(String.format("Section 2: Data Length = %6d\n",
                getSectionSize()));

        return buffer;
    }

}
