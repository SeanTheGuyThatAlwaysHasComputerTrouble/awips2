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
package com.raytheon.uf.edex.registry.ebxml.dao;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Database validation strategy.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 15, 2013 1693       djohnson     Initial creation
 * 10/16/2014   3454       bphillip    Upgrading to Hibernate 4
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface IEbxmlDatabaseValidationStrategy {

    /**
     * Check whether the database is valid compared to the annotation
     * configuration.
     * 
     * @param aConfig
     *            the configuration
     * @param sessionFactory
     *            the session factory
     * @return true if valid
     */
    boolean isDbValid(Configuration aConfig,
            SessionFactory sessionFactory);

}
