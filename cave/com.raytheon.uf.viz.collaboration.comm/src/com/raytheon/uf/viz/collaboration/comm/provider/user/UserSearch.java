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
package com.raytheon.uf.viz.collaboration.comm.provider.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.ReportedData.Row;
import org.jivesoftware.smackx.search.UserSearchManager;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Search utility for XMPP connections
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 22, 2013 2561       bclement     Initial creation
 * Jan 24, 2014 2701       bclement     distinction between userid and username
 * Feb  3, 2014 2699       bclement     fixed assumption that username search was exact
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class UserSearch {

    private final XMPPConnection connection;

    private final UserSearchManager searchManager;

    private static final String SEARCH_SERVICE_PREFIX = "search.";

    private static final String SEARCH_ACTION = "search";

    public static final String USERNAME_FIELD = "Username";

    public static final String JABBER_ID_COLUMN = "jid";

    public static final String NAME_COLUMN = "name";

    private static final String FORM_TYPE = "FORM_TYPE";

    private final IUFStatusHandler log = UFStatus.getHandler(this.getClass());

    /**
     * @param conn
     */
    public UserSearch(XMPPConnection conn) {
        this.connection = conn;
        this.searchManager = new UserSearchManager(conn);
    }

    /**
     * Search by username (the part of the user id before the @). This is case
     * sensitive and matches any part of the string (^.*username.*$).
     * 
     * @param name
     * @return list of user ids that match that name
     * @throws XMPPException
     */
    public List<UserId> byUsername(String username) throws XMPPException {
        return byCriteria(USERNAME_FIELD, username);
    }

    /**
     * Search by username (the part of the user id before the @). This only
     * returns a match if the entire username matches the entire entry.
     * 
     * @param username
     * @return null if none found
     * @throws XMPPException
     */
    public UserId byExactUsername(String username) throws XMPPException {
        List<UserId> results = byUsername(username);
        UserId rval = null;
        for (UserId result : results) {
            if (result.getName().equals(username)) {
                rval = result;
                break;
            }
        }
        return rval;
    }

    /**
     * Search for users by single criteria. This is case sensitive and matches
     * any part of the string (^.*value.*$).
     * 
     * @param field
     * @param value
     * @return list of user ids that match that criteria
     * @throws XMPPException
     */
    public List<UserId> byCriteria(String field, String value)
            throws XMPPException {
        String serviceName = getSearchServiceName();
        Form searchForm = searchManager.getSearchForm(serviceName);
        Form answerForm = searchForm.createAnswerForm();
        answerForm.setAnswer(field, true);
        answerForm.setAnswer(SEARCH_ACTION, value);
        ReportedData results = searchManager.getSearchResults(answerForm,
                serviceName);
        return unwrap(results);
    }

    /**
     * Extract user ids from search results
     * 
     * @param results
     * @return
     */
    protected List<UserId> unwrap(ReportedData results) {
        List<UserId> rval = new ArrayList<UserId>();
        Iterator<Row> rows = results.getRows();
        while (rows.hasNext()) {
            Row row = (Row) rows.next();
            Iterator<?> jids = row.getValues(JABBER_ID_COLUMN);
            Iterator<?> names = row.getValues(NAME_COLUMN);
            while (jids.hasNext() && names.hasNext()) {
                String jid = (String) jids.next();
                UserId id = IDConverter.convertFrom(jid);
                id.setAlias((String) names.next());
                rval.add(id);
            }
            if (jids.hasNext() || names.hasNext()) {
                log.warn("Search results had unmatched number of names and ids");
            }
        }
        return rval;
    }

    /**
     * @return fully qualified service name
     */
    private String getSearchServiceName() {
        return SEARCH_SERVICE_PREFIX + connection.getServiceName();
    }

    /**
     * @return fields for user search
     * @throws XMPPException
     */
    public String[] getUserPropertiesFields() throws XMPPException {
            String serviceName = getSearchServiceName();
            Form searchForm = searchManager.getSearchForm(serviceName);

            Set<String> fields = new HashSet<String>();
            Iterator<FormField> userProperties = searchForm.getFields();
            while (userProperties.hasNext()) {
                FormField field = (FormField) userProperties.next();
                String variable = field.getVariable();
                // ignore these fields
                if (!variable.equalsIgnoreCase(FORM_TYPE)
                        && !variable.equalsIgnoreCase(SEARCH_ACTION))
                    fields.add(variable);
            }
            return (String[]) fields.toArray(new String[0]);
    }
}
