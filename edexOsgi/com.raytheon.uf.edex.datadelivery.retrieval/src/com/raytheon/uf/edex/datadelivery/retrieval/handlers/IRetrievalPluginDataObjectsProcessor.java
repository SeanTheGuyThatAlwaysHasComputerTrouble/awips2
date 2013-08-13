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
package com.raytheon.uf.edex.datadelivery.retrieval.handlers;

import java.util.Date;

import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.datadelivery.retrieval.adapters.RetrievalAdapter.TranslationException;

/**
 * Processes {@link RetrievalResponseXml} that were generated from a retrieval.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 01, 2013 1543       djohnson     Initial creation
 * Aug 09, 2013 1822       bgonzale     Added parameters to processRetrievedPluginDataObjects.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface IRetrievalPluginDataObjectsProcessor {

    /**
     * Process plugin data objects that were created as a result of a data
     * delivery retrieval.
     * 
     * @param dataProvider
     *            NOMADS, MADIS, or PDA
     * @param dataFormat
     *            Binary Grid(OPENDAP), POINT(MADIS), NetCDF4
     * @param sourceType
     *            Model, Observation, Satellite
     * @param date
     *            the date
     * @param retrievalPluginDataObjects
     *            the retrieval plugin data objects
     * @throws SerializationException
     *             on error with serialization
     * @throws TranslationException
     */
    void processRetrievedPluginDataObjects(String dataProvider,
            String dataFormat, String sourceType, Date date,
            RetrievalResponseXml retrievalPluginDataObjects)
            throws SerializationException, TranslationException;
}
