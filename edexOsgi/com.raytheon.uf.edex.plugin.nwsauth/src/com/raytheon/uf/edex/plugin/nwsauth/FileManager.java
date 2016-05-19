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
package com.raytheon.uf.edex.plugin.nwsauth;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.plugin.nwsauth.xml.NwsRoleData;
import com.raytheon.uf.common.plugin.nwsauth.xml.PermissionXML;
import com.raytheon.uf.common.plugin.nwsauth.xml.RoleXML;
import com.raytheon.uf.common.plugin.nwsauth.xml.UserXML;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Uses localization data to determine role/permissions. Intentionally
 * package-private as all access should remain localized to the NWS plugin.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 09, 2013 1412       djohnson     Moved file writing from viz plugin to server-side.
 * Jan 17, 2013 1412       djohnson     Check files for having been modified each time data is requested, 
 *                                      in case they were written by another member of the cluster.
 * Mar 12, 2013 1646       mpduff       Format the output.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */
class FileManager {
    /** Status handler */
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FileManager.class);

    private static final FileManager instance = new FileManager();

    private static JAXBManager jaxbManager;

    private final String ROLE_DIR = "roles";

    private final AtomicLong lastReadTime = new AtomicLong(-1L);

    /**
     * Application name -> Role Data.
     */
    private final ConcurrentMap<String, NwsRoleData> roleDataMap = new ConcurrentHashMap<String, NwsRoleData>();

    /**
     * Application name -> LocalizationFile map.
     */
    private final ConcurrentMap<String, LocalizationFile> roleFileMap = new ConcurrentHashMap<String, LocalizationFile>();

    /**
     * Package-level visibility so tests can create new instances.
     */
    FileManager() {
        readXML();
    }

    /**
     * Get an instance.
     * 
     * @return an instance
     */
    public static FileManager getInstance() {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public void save(String application) {
        NwsRoleData roleData = roleDataMap.get(application);
        LocalizationFile lf = roleFileMap.get(application);

        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext context = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.SITE);
        LocalizationFile locFile = pm
                .getLocalizationFile(context, lf.getName());
        try {
            JAXBManager jaxbManager = getJaxbManager();
            Marshaller marshaller = jaxbManager.getJaxbContext()
                    .createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(roleData, locFile.getFile());
            locFile.save();

        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        } catch (LocalizationOpFailedException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }

    }

    private void readXML() {
        try {
            LocalizationFile[] roleFiles = getUserRoleLocalizationFiles();
            boolean needToReadFiles = false;
            for (LocalizationFile lf : roleFiles) {
                final long fileLastModified = lf.getFile().lastModified();
                final long lastTimeFilesWereRead = lastReadTime.get();

                if (fileLastModified > lastTimeFilesWereRead) {
                    needToReadFiles = true;
                    break;
                }
            }

            if (needToReadFiles) {
                for (LocalizationFile lf : roleFiles) {
                    final long fileLastModified = lf.getFile().lastModified();
                    final long lastTimeFilesWereRead = lastReadTime.get();

                    if (fileLastModified < lastTimeFilesWereRead) {
                        continue;
                    }
                    NwsRoleData roleData = lf.jaxbUnmarshal(NwsRoleData.class,
                            getJaxbManager());

                    if (roleData != null) {
                        final String application = roleData.getApplication();
                        this.roleDataMap.put(application, roleData);
                        this.roleFileMap.put(application, lf);
                    }
                }
            }
            lastReadTime.set(TimeUtil.currentTimeMillis());
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }

    private LocalizationFile[] getUserRoleLocalizationFiles() {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext[] contexts = new LocalizationContext[2];
        contexts[0] = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.BASE);
        contexts[1] = pm.getContext(LocalizationType.COMMON_STATIC,
                LocalizationLevel.SITE);
        LocalizationFile[] roleFiles = pm.listFiles(contexts, ROLE_DIR,
                new String[] { ".xml" }, false, true);
        return roleFiles;
    }

    private JAXBManager getJaxbManager() throws JAXBException {
        if (jaxbManager == null) {
            jaxbManager = new JAXBManager(NwsRoleData.class,
                    PermissionXML.class, RoleXML.class, UserXML.class);
        }
        return jaxbManager;
    }

    /**
     * @return
     */
    public Map<String, NwsRoleData> getRoleDataMap() {
        readXML();
        return roleDataMap;
    }

    /**
     * @param roleDataWithChanges
     */
    public void writeApplicationRoleData(
            Map<String, NwsRoleData> roleDataWithChanges) {
        for (Entry<String, NwsRoleData> entry : roleDataWithChanges.entrySet()) {
            final String application = entry.getKey();
            roleDataMap.put(application, entry.getValue());

            save(application);
        }
    }
}
