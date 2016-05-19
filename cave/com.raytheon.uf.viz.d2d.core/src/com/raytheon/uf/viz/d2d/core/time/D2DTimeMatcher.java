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
package com.raytheon.uf.viz.d2d.core.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import org.apache.commons.lang.Validate;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.DataTimeComparator;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.comm.PerspectiveSpecificLoadProperties;
import com.raytheon.uf.viz.core.drawables.AbstractDescriptor;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.globals.VizGlobalsManager;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IDisposeListener;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.d2d.core.D2DLoadProperties;

/**
 * Performs D2D-style time matching
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 10, 2009           chammack    Initial creation
 * Jul 03, 2013  2159     bsteffen    Synchronize TimeCache access.
 * Aug  9, 2013  16448    D. Friedman Validate time match basis in
 *                                    redoTimeMatching
 * May  5, 2014  17201    D. Friedman Make same-radar time matching work more
 *                                    like A1.
 * May  5, 2014  3265     bsteffen    Better handling of resources returning
 *                                    null dataTimes.
 * May 13, 2015  4461     bsteffen    Move the logic to change frames into the
 *                                    FrameCoordinator.
 * Jul 14, 2015  DR 13900 D. Friedman Validate descriptor of time match basis
 *                                    before time matching it.
 * Jul 30, 2015  17761    D. Friedman Allow resources to return data times based
 *                                    on base frame times.
 * 
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
public class D2DTimeMatcher extends AbstractTimeMatcher {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(D2DTimeMatcher.class);

    /**
     * Always synchronize on an instance of TimeCache before using it to avoid
     * getting mixed state from modification on other threads.
     */
    private static class TimeCache {
        /**
         * The last set of times that the resource with these properties was
         * matched against. As long as we are matching against these same times
         * then lastFrameTimes is valid.
         */
        private DataTime[] lastBaseTimes;

        /** The result of the last time matching. */
        private DataTime[] lastFrameTimes;

        /** The number of frames time matched against */
        private int lastFrameCount;

        public DataTime[] getLastBaseTimes() {
            return lastBaseTimes;
        }

        public DataTime[] getLastFrameTimes() {
            return lastFrameTimes;
        }

        public int getLastFrameCount() {
            return lastFrameCount;
        }

        public void setTimes(DataTime[] baseTimes, DataTime[] frameTimes) {
            setTimes(baseTimes, frameTimes, -1);
        }

        public void setTimes(DataTime[] baseTimes, DataTime[] frameTimes,
                int frameCount) {
            this.lastBaseTimes = baseTimes;
            this.lastFrameTimes = frameTimes;
            this.lastFrameCount = frameCount;
        }

    }

    protected transient AbstractVizResource<?, ?> timeMatchBasis;

    private final IDisposeListener timeMatchBasisDisposeListener = new IDisposeListener() {

        @Override
        public void disposed(AbstractVizResource<?, ?> resource) {
            if ((resource == timeMatchBasis)) {
                synchronized (D2DTimeMatcher.this) {
                    timeMatchBasis = null;
                }
            }
        }

    };

    /** A clock time limit for loading data (optional) */
    @XmlAttribute
    protected Date clockFilter;

    /** A forecast time filter (optional) */
    @XmlAttribute
    protected long forecastFilter;

    /** A delta time filter (optional) */
    @XmlAttribute
    protected long deltaFilter;

    /** user time options interaction is selected. */
    private boolean isTimeOptionsSelected;

    /** The load mode */
    @XmlAttribute
    protected LoadMode loadMode = (LoadMode) VizGlobalsManager
            .getCurrentInstance().getPropery(VizConstants.LOADMODE_ID);

    private AbstractTimeMatchingConfigurationFactory configFactory;

    private final Map<AbstractVizResource<?, ?>, TimeCache> timeCacheMap = new IdentityHashMap<AbstractVizResource<?, ?>, D2DTimeMatcher.TimeCache>();

    /**
     * Default Constructor.
     */
    public D2DTimeMatcher() {
        super();
        try {
            configFactory = AbstractTimeMatchingConfigurationFactory
                    .constructConfigurationFactory();
        } catch (VizException e) {
            statusHandler.handle(Priority.SIGNIFICANT,
                    "Error Initializing Time Matcher", e);
        }
    }

    @Override
    public void redoTimeMatching(AbstractVizResource<?, ?> resource) {
        TimeCache cache = null;
        synchronized (timeCacheMap) {
            cache = timeCacheMap.get(resource);
        }
        if (cache != null) {
            synchronized (cache) {
                cache.setTimes(null, null);
            }
        }
    }

    /**
     * Checks if a resource is contained in the {@link IResourceGroup}
     * recursively checking for {@link IResourceGroup}s in the group's list
     * 
     * @param group
     * @param resource
     * @return
     */
    private boolean contained(IResourceGroup group,
            AbstractVizResource<?, ?> resource) {
        ResourceList list = group.getResourceList();
        if (list.containsRsc(resource)) {
            return true;
        } else {
            for (ResourcePair rp : list) {
                if (rp.getResourceData() instanceof IResourceGroup) {
                    if (contained((IResourceGroup) rp.getResourceData(),
                            resource)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.ITimeMatcher#redoTimeMatching()
     */
    @Override
    public void redoTimeMatching(IDescriptor descriptor) throws VizException {
        synchronized (this) {
            if ((timeMatchBasis != null)
                    && (timeMatchBasis.getDescriptor() == descriptor)
                    && !validateTimeMatchBasis(descriptor)) {
                changeTimeMatchBasis(null);
            }
            if (timeMatchBasis != null) {
                IDescriptor tmDescriptor = timeMatchBasis.getDescriptor();
                if (tmDescriptor != null) {
                    if (tmDescriptor != descriptor
                            && tmDescriptor.getTimeMatcher() == this
                            && hasContainer(descriptor)) {
                        if (validateDescriptor(tmDescriptor)) {
                            redoTimeMatching(tmDescriptor);
                        } else {
                            changeTimeMatchBasis(null);
                        }
                    } else if (contained(tmDescriptor, timeMatchBasis) == false) {
                        // Checks to ensure the timeMatchBasis is not "orphaned"
                        timeMatchBasis = null;
                    }
                }
            }
            // Find the times for the time match basis.
            DataTime[] timeSteps = findBasisTimes(descriptor.getResourceList(),
                    descriptor.getNumberOfFrames());
            if (timeSteps == null) {
                descriptor.setFramesInfo(new FramesInfo(null, -1));
                return;
            }

            Map<AbstractVizResource<?, ?>, DataTime[]> resourceTimeMap = new HashMap<AbstractVizResource<?, ?>, DataTime[]>();
            resourceTimeMap.put(timeMatchBasis, timeSteps);

            // Find times for every other resource
            Iterator<ResourcePair> pairIterator = descriptor.getResourceList()
                    .listIterator();
            while (pairIterator.hasNext()) {
                AbstractVizResource<?, ?> rsc = pairIterator.next()
                        .getResource();
                recursiveOverlay(descriptor, new FramesInfo(timeSteps, -1,
                        resourceTimeMap), rsc, resourceTimeMap);
            }

            // Update the descriptor to the new times.
            if ((timeMatchBasis.getDescriptor() != null)
                    && (timeMatchBasis.getDescriptor() != descriptor)) {
                int idx = timeMatchBasis.getDescriptor().getFramesInfo()
                        .getFrameIndex();
                if ((idx >= 0) && (idx < timeSteps.length)) {
                    descriptor.setFramesInfo(new FramesInfo(timeSteps, idx,
                            resourceTimeMap));
                } else {
                    descriptor.setFramesInfo(new FramesInfo(timeSteps,
                            resourceTimeMap));
                }
            } else {
                descriptor.setFramesInfo(new FramesInfo(timeSteps,
                        resourceTimeMap));
            }

            // Add Remove data for all the resources.
            for (Entry<AbstractVizResource<?, ?>, DataTime[]> entry : resourceTimeMap
                    .entrySet()) {
                if (entry.getKey().getDescriptor() == descriptor) {
                    timeMatchUpdate(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Recursively determine times for all resource, or if it is an
     * IResourceGroup then for all resources in it.
     * 
     * @param descriptor
     *            the descriptor that is being updated
     * @param rsc
     *            the resource being updated.
     * @param frameTimesSoure
     *            map of all previously time matched resources that may be used
     *            to determine the frame times
     * @throws VizException
     */
    private void recursiveOverlay(IDescriptor descriptor,
            FramesInfo framesInfo, AbstractVizResource<?, ?> rsc,
            Map<AbstractVizResource<?, ?>, DataTime[]> frameTimesSoure)
            throws VizException {
        if (rsc == null) {
            return;
        }
        if (rsc instanceof IResourceGroup) {
            Map<AbstractVizResource<?, ?>, DataTime[]> completed = new HashMap<AbstractVizResource<?, ?>, DataTime[]>(
                    frameTimesSoure);
            for (ResourcePair rp : ((IResourceGroup) rsc).getResourceList()) {
                AbstractVizResource<?, ?> rsc1 = rp.getResource();
                recursiveOverlay(descriptor, framesInfo, rsc1, completed);
            }
        }

        if (rsc != timeMatchBasis) {
            TimeMatchingConfiguration config = getConfiguration(rsc
                    .getLoadProperties());
            TimeCache timeCache = getTimeCache(rsc);
            synchronized (timeCache) {
                DataTime[] timeSteps = getFrameTimes(descriptor, framesInfo,
                        frameTimesSoure);
                if (Arrays.equals(timeSteps, timeCache.getLastBaseTimes())) {
                    framesInfo.getTimeMap().put(rsc,
                            timeCache.getLastFrameTimes());
                } else {
                    config = config.clone();
                    if ((config.getDataTimes() == null)
                            || (config.getDataTimes().length < 1)) {
                        config.setDataTimes(getLatestTimes(rsc, timeSteps));
                    }
                    populateConfiguration(config);
                    TimeMatcher tm = new TimeMatcher();
                    if (rsc instanceof ID2DTimeMatchingExtension) {
                        ((ID2DTimeMatchingExtension) rsc).modifyTimeMatching(
                                this, rsc, tm);
                    }
                    DataTime[] overlayDates = tm.makeOverlayList(
                            config.getDataTimes(), config.getClock(),
                            timeSteps, config.getLoadMode(),
                            config.getForecast(), config.getDelta(),
                            config.getTolerance());
                    timeCache.setTimes(timeSteps, overlayDates);
                    framesInfo.getTimeMap().put(rsc, overlayDates);
                }
            }
        }
    }

    /**
     * Determine the times to match this resource against, for single panel this
     * is the timeMatchBasisTimes, for four panel it is a bit more complex.
     * 
     * @param descriptor
     * @param frameInfo
     * @param frameTimesSoure
     * @return
     */
    private DataTime[] getFrameTimes(IDescriptor descriptor,
            FramesInfo frameInfo,
            Map<AbstractVizResource<?, ?>, DataTime[]> frameTimesSource) {
        DataTime[] descTimes = frameInfo.getFrameTimes();
        if ((timeMatchBasis != null)
                && (timeMatchBasis.getDescriptor() == descriptor)) {
            return descTimes;
        }

        // If the time match basis is not in this descriptor we should instead
        // fall back on the first resource in this descriptor which has times
        // for this frame. This concept was adopted from Frame::dataTime() in
        // the A1 source.
        DataTime[] times = new DataTime[frameInfo.getFrameCount()];

        for (ResourcePair rp : descriptor.getResourceList()) {
            DataTime[] rscTimes = frameTimesSource.get(rp.getResource());
            if ((rscTimes == null) || (rscTimes.length != times.length)) {
                if (rp.getResource() instanceof IResourceGroup) {
                    // Descend into resource groups.
                    for (ResourcePair rp1 : ((IResourceGroup) rp.getResource())
                            .getResourceList()) {
                        rscTimes = frameTimesSource.get(rp1.getResource());
                        if ((rscTimes != null)
                                && (rscTimes.length == times.length)) {
                            for (int i = 0; i < times.length; i++) {
                                if ((times[i] == null) && (rscTimes[i] != null)) {
                                    times[i] = rscTimes[i];
                                }
                            }
                        }
                    }
                }
                continue;
            }
            for (int i = 0; i < times.length; i++) {
                if ((times[i] == null) && (rscTimes[i] != null)) {
                    times[i] = rscTimes[i];
                }
            }
        }
        for (int i = 0; i < times.length; i++) {
            if ((times[i] == null) && (descTimes[i] != null)) {
                times[i] = descTimes[i];
            }
        }
        return times;
    }

    /**
     * Attempts to find the frame times for a time match basis, if time match
     * basis is not defined attempts to find a new time match basis. Returns
     * null if no Time Match basis can be found.
     * 
     * @param descriptor
     * @param resourceList
     * @return
     * @throws VizException
     */
    private DataTime[] findBasisTimes(ResourceList resourceList,
            int numberOfFrames) throws VizException {
        if (timeMatchBasis != null) {
            TimeCache timeCache = getTimeCache(timeMatchBasis);
            DataTime[] times = null;
            synchronized (timeCache) {
                times = timeCache.getLastFrameTimes();
                if ((times == null) || (timeCache.getLastBaseTimes() != null)
                        || (timeCache.getLastFrameCount() != numberOfFrames)) {
                    times = makeEmptyLoadList(numberOfFrames, timeMatchBasis);
                    timeCache.setTimes(null, times, numberOfFrames);
                }
            }
            if (times != null) {
                return times;
            } else {
                timeMatchBasis = null;
            }
        }
        Iterator<ResourcePair> pairIterator = resourceList.iterator();
        while (pairIterator.hasNext()) {
            ResourcePair pair = pairIterator.next();
            AbstractVizResource<?, ?> rsc = pair.getResource();
            if (rsc == null) {
                continue;
            }
            if (pair.getProperties().isMapLayer()
                    || pair.getProperties().isSystemResource()) {
                continue;
            }

            if (rsc.getResourceData() instanceof IResourceGroup) {
                DataTime[] times = findBasisTimes(
                        ((IResourceGroup) rsc.getResourceData())
                                .getResourceList(),
                        numberOfFrames);

                if (times != null) {
                    return times;
                }
            } else {
                DataTime[] times = makeEmptyLoadList(numberOfFrames, rsc);
                if (times != null) {
                    TimeCache cache = getTimeCache(rsc);
                    synchronized (cache) {
                        cache.setTimes(null, times, numberOfFrames);
                    }
                    return times;
                }
            }
        }
        return null;
    }

    /**
     * Make an empty load list for a resource, if this is successful the
     * resource will be defined as the time match basis
     * 
     * @param descriptor
     * @param rsc
     * @return
     * @throws VizException
     */
    private DataTime[] makeEmptyLoadList(int numberOfFrames,
            AbstractVizResource<?, ?> rsc) throws VizException {
        if ((timeMatchBasis != null) && (rsc != timeMatchBasis)) {
            throw new IllegalArgumentException(
                    "Cannot make Empty Load List for a resource which is not the Time Match Basis.");
        }

        TimeMatchingConfiguration config = getConfiguration(
                rsc.getLoadProperties()).clone();
        if ((config.getDataTimes() == null)
                || (config.getDataTimes().length < 1)) {
            config.setDataTimes(getLatestTimes(rsc, null));
            if ((config.getDataTimes() == null)
                    || (config.getDataTimes().length < 1)) {
                return null;
            }
        }
        populateConfiguration(config);
        DataTime[] timeSteps = TimeMatcher.makeEmptyLoadList(
                config.getDataTimes(), config.getClock(), numberOfFrames,
                config.getLoadMode(), config.getForecast(), config.getDelta());
        if ((timeSteps == null) || (timeSteps.length == 0)) {
            return null;
        }
        changeTimeMatchBasis(rsc);
        return timeSteps;
    }

    /**
     * Remove unused times from a resource and add in any new times.
     * 
     * @param rsc
     * @param timeSteps
     * @throws VizException
     */
    private void timeMatchUpdate(AbstractVizResource<?, ?> rsc,
            DataTime[] timeSteps) throws VizException {

        if (rsc.getResourceData() instanceof IResourceGroup) {
            for (ResourcePair tmp : ((IResourceGroup) rsc.getResourceData())
                    .getResourceList()) {
                AbstractVizResource<?, ?> tmpr = tmp.getResource();
                if (tmpr != null) {
                    timeMatchUpdate(tmpr, null);
                }
            }
        } else {
            pruneUnusedData(rsc, timeSteps);

            updateResourceWithLatest(timeSteps, rsc);

        }

        if (timeSteps == null) {
            timeSteps = rsc.getDataTimes();
            Arrays.sort(timeSteps);
        }
    }

    /**
     * Find the Time Matching Configuration for this resource or if one can't be
     * found return an empty configuration
     * 
     * @param properties
     * @return
     */
    private TimeMatchingConfiguration getConfiguration(LoadProperties properties) {
        if (properties == null) {
            return new TimeMatchingConfiguration();
        }
        PerspectiveSpecificLoadProperties perspProps = properties
                .getPerspectiveProperty();
        D2DLoadProperties d2dProps = null;
        if (perspProps instanceof D2DLoadProperties) {
            d2dProps = (D2DLoadProperties) perspProps;
            if (d2dProps.getTimeConfig() != null) {
                return d2dProps.getTimeConfig();
            }
        }
        return new TimeMatchingConfiguration();
    }

    private TimeCache getTimeCache(AbstractVizResource<?, ?> resource) {
        synchronized (timeCacheMap) {
            TimeCache cache = timeCacheMap.get(resource);
            if (cache == null) {
                cache = new TimeCache();
                timeCacheMap.put(resource, cache);
            }
            return cache;
        }
    }

    /**
     * populates all unset fields of the configuration with defaults except
     * dataTimes, dataTimes should be set before calling this function.
     * 
     * @param config
     */
    private void populateConfiguration(TimeMatchingConfiguration config) {
        Arrays.sort(config.getDataTimes());
        if (config.getClock() == null) {
            if (SimulatedTime.getSystemTime().isRealTime()
                    && (config.getDataTimes() != null)
                    && (config.getDataTimes().length != 0)) {
                config.setClock(config.getDataTimes()[config.getDataTimes().length - 1]
                        .getValidTime().getTime());
            } else {
                config.setClock(SimulatedTime.getSystemTime().getTime());
            }
        }
        if (config.getLoadMode() == null) {
            config.setLoadMode(loadMode);
        }
        if (config.getForecast() == null) {
            config.setForecast(getForecastFilter());
        }
        if (config.getDelta() == null) {
            config.setDelta(getDeltaFilter());
        }
        if (config.getTolerance() == null) {
            config.setTolerance(TimeMatcher.DEFAULT_TOLERANCE_FACTOR);
        }
    }

    /**
     * Retrieves the latest times from a time sequence resource
     * 
     * If the resource is also requestable, we check catalog for the latest
     * product times.
     * 
     * @param rsc
     * @param timeSteps
     * @return
     * @throws VizException
     */
    protected DataTime[] getLatestTimes(AbstractVizResource<?, ?> rsc, DataTime[] timeSteps)
            throws VizException {
        DataTime[] availableTimes = null;

        // If resource is handling requests itself, just return the datatimes
        // listed in the resource
        AbstractResourceData resourceData = rsc.getResourceData();
        if (resourceData instanceof AbstractRequestableResourceData) {
            AbstractRequestableResourceData req = (AbstractRequestableResourceData) resourceData;
            if (req.isRequeryNecessaryOnTimeMatch()
                    || (rsc.getDataTimes() == null)
                    || (rsc.getDataTimes().length == 0)) {
                availableTimes = req.getAvailableTimes();
            }
        }

        if (availableTimes == null) {
            availableTimes = rsc.getMatchedDataTimes(timeSteps);
            Arrays.sort(availableTimes);
        }

        return availableTimes;
    }

    protected void pruneUnusedData(
            AbstractVizResource<? extends AbstractResourceData, ? extends IDescriptor> rsc,
            DataTime[] times) {

        DataTime[] rscTimes = rsc.getDataTimes();
        Arrays.sort(rscTimes);

        // If resource is handling requests itself, quit
        AbstractResourceData resourceData = rsc.getResourceData();

        if (times == null) {
            return;
        }
        for (DataTime dt : rscTimes) {

            if (dt == null) {
                continue;
            }
            boolean found = false;
            for (DataTime t2 : times) {
                if (dt.equals(t2)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // If resource has resource data (SHOULD) fire listener
                if (resourceData != null) {
                    resourceData
                            .fireChangeListeners(ChangeType.DATA_REMOVE, dt);
                } else {
                    // otherwise just remove it
                    rsc.remove(dt);
                }

            }

        }
    }

    /**
     * Takes a list of data times that are available for a resource and requests
     * the products that are missing
     * 
     * @param dataTimes
     *            available dataTimes
     * @param resource
     *            the resource to update
     * @throws VizException
     */
    private void updateResourceWithLatest(DataTime[] dataTimes,
            AbstractVizResource<?, ?> resource) throws VizException {
        Validate.notNull(resource, "Resource must not be null");
        if (resource.getResourceData() == null) {
            return;
        }

        if (resource.getResourceData() instanceof AbstractRequestableResourceData) {
            AbstractRequestableResourceData arrd = (AbstractRequestableResourceData) resource
                    .getResourceData();

            DataTime[] dt = resource.getDataTimes();
            /*
             * Passing in the comparator allows it to handle null times. Ideally
             * there should be no null times but if there is this is not the
             * place to break things.
             */
            Arrays.sort(dt, new DataTimeComparator());
            PluginDataObject[] pdo = arrd.getLatestPluginDataObjects(dataTimes,
                    dt);
            if (pdo.length > 0) {
                resource.getResourceData().update(pdo);
            }
        }

    }

    @Override
    public void handleRemove(AbstractVizResource<?, ?> resource,
            IDescriptor descriptor) {
        if ((resource == timeMatchBasis)
                && (descriptor instanceof AbstractDescriptor)) {
            synchronized (this) {
                timeMatchBasis = null;
            }
        }
        synchronized (timeCacheMap) {
            timeCacheMap.remove(resource);
        }
    }

    /**
     * @return the loadMode
     */
    public LoadMode getLoadMode() {
        return loadMode;
    }

    /**
     * @param loadMode
     *            the loadMode to set
     */
    public void setLoadMode(LoadMode loadMode) {
        this.loadMode = loadMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.AbstractTimeMatcher#initialLoad(com.raytheon
     * .uf.viz.core.comm.LoadProperties,
     * com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData,
     * com.raytheon.uf.viz.core.drawables.IDescriptor)
     */
    @Override
    public DataTime[] initialLoad(LoadProperties loadProps,
            DataTime[] availableTimes, IDescriptor descriptor)
            throws VizException {

        DataTime[] dataTimesToLoad = null;

        TimeMatchingConfiguration config = null;
        if (timeMatchBasis == null) {
            config = configFactory.getConfiguration(loadProps, this,
                    availableTimes, descriptor);
            if ((config == null) || config.isCancel()) {
                return dataTimesToLoad;
            }
            config = config.clone();
            if ((config.getDataTimes() == null)
                    || (config.getDataTimes().length < 1)) {
                config.setDataTimes(availableTimes);
            }
            populateConfiguration(config);
            dataTimesToLoad = TimeMatcher.makeEmptyLoadList(
                    config.getDataTimes(), config.getClock(),
                    descriptor.getNumberOfFrames(), config.getLoadMode(),
                    config.getForecast(), config.getDelta());
        } else {
            config = configFactory.getOverlayConfiguration(loadProps, this,
                    availableTimes, descriptor);
            if ((config == null) || config.isCancel()) {
                return dataTimesToLoad;
            }
            config = config.clone();
            if ((config.getDataTimes() == null)
                    || (config.getDataTimes().length < 1)) {
                config.setDataTimes(availableTimes);
            }
            populateConfiguration(config);
            DataTime[] existingDataTimes = getFrameTimes(descriptor,
                    descriptor.getFramesInfo(), descriptor.getFramesInfo()
                            .getTimeMap());

            TimeMatcher tm = new TimeMatcher();
            dataTimesToLoad = tm.makeOverlayList(config.getDataTimes(),
                    config.getClock(), existingDataTimes, config.getLoadMode(),
                    config.getForecast(), config.getDelta(),
                    config.getTolerance());

            if ((timeMatchBasis.getDescriptor() != null)
                    && (timeMatchBasis.getDescriptor() != descriptor)) {
                // Still use my times, but the index from the time match basis
                FramesInfo myFi = descriptor.getFramesInfo();
                FramesInfo tmFi = timeMatchBasis.getDescriptor()
                        .getFramesInfo();
                descriptor.setFramesInfo(new FramesInfo(myFi.getFrameTimes(),
                        tmFi.getFrameIndex()));
            }
        }

        return dataTimesToLoad;
    }

    /**
     * Changes the time match basis for the time matcher to be the specified
     * resource
     * 
     * @param resource
     */
    public void changeTimeMatchBasis(AbstractVizResource<?, ?> resource) {
        if (timeMatchBasis != resource) {
            if (timeMatchBasis != null) {
                TimeMatchingConfiguration config = getConfiguration(timeMatchBasis
                        .getLoadProperties());
                config.setTimeMatchBasis(false);
                TimeCache timeCache = getTimeCache(timeMatchBasis);
                synchronized (timeCache) {
                    timeCache.setTimes(null, null);
                }
                timeMatchBasis
                        .unregisterListener(timeMatchBasisDisposeListener);
            }

            timeMatchBasis = resource;

            if (timeMatchBasis != null) {
                TimeMatchingConfiguration config = getConfiguration(timeMatchBasis
                        .getLoadProperties());
                config.setTimeMatchBasis(true);
                TimeCache timeCache = getTimeCache(timeMatchBasis);
                synchronized (timeCache) {
                    timeCache.setTimes(null, null);
                }
                timeMatchBasis.registerListener(timeMatchBasisDisposeListener);
            }
        }
    }

    /**
     * Returns the time match basis for the D2DTimeMatcher
     * 
     * @return
     */
    public AbstractVizResource<?, ?> getTimeMatchBasis() {
        return timeMatchBasis;
    }

    public boolean hasTimeMatchBasis() {
        return (timeMatchBasis != null);
    }

    /**
     * @return the clockFilter
     */
    public Date getClockFilter() {
        return clockFilter;
    }

    /**
     * @return the forecastFilter
     */
    public long getForecastFilter() {
        return forecastFilter;
    }

    /**
     * @return the deltaFilter
     */
    public long getDeltaFilter() {
        return deltaFilter;
    }

    /**
     * @return the isTimeOptionsSelected
     */
    public boolean isTimeOptionsSelected() {
        return isTimeOptionsSelected;
    }

    /**
     * @param isTimeOptionsSelected
     *            the isTimeOptionsSelected to set
     */
    public void setTimeOptionsSelected(boolean isTimeOptionsSelected) {
        this.isTimeOptionsSelected = isTimeOptionsSelected;
    }

    /**
     * @return the dataTimeDialogManager
     */
    public AbstractTimeMatchingConfigurationFactory getTimeMatchingConfigurationFactory() {
        return configFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.AbstractTimeMatcher#getDisplayLoadOrder(java
     * .util.List)
     */
    @Override
    public List<AbstractRenderableDisplay> getDisplayLoadOrder(
            List<AbstractRenderableDisplay> displays) {
        // if any of the displays have a set time match basis then load it first
        AbstractRenderableDisplay basisDisplay = null;
        for (AbstractRenderableDisplay display : displays) {
            if (getBasisResourcePair(display.getDescriptor().getResourceList()) != null) {
                basisDisplay = display;
                break;
            }
        }
        if (basisDisplay != null) {
            List<AbstractRenderableDisplay> results = new ArrayList<AbstractRenderableDisplay>(
                    displays);
            results.remove(basisDisplay);
            results.add(0, basisDisplay);
            return results;
        }
        return super.getDisplayLoadOrder(displays);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.AbstractTimeMatcher#getResourceLoadOrder(java
     * .util.List)
     */
    @Override
    public List<ResourcePair> getResourceLoadOrder(List<ResourcePair> resources) {
        // if any of the resources are set as the time match basis then load it
        // first
        ResourcePair basisPair = getBasisResourcePair(resources);
        if (basisPair != null) {
            List<ResourcePair> results = new ArrayList<ResourcePair>(resources);
            results.remove(basisPair);
            results.add(0, basisPair);
            return results;
        }
        return super.getResourceLoadOrder(resources);
    }

    private ResourcePair getBasisResourcePair(List<ResourcePair> resources) {
        for (ResourcePair pair : resources) {
            if (getConfiguration(pair.getLoadProperties()).isTimeMatchBasis()) {
                return pair;
            }
            if (pair.getResourceData() instanceof IResourceGroup) {
                ResourcePair testPair = getBasisResourcePair(((IResourceGroup) pair
                        .getResourceData()).getResourceList());
                if (testPair != null) {
                    return pair;
                }
            }
        }
        return null;
    }

    @Override
    public void copyFrom(AbstractTimeMatcher timeMatcher) {
        if (timeMatcher instanceof D2DTimeMatcher) {
            D2DTimeMatcher d2d = (D2DTimeMatcher) timeMatcher;
            if (timeMatchBasis == null) {
                this.clockFilter = d2d.clockFilter;
                this.forecastFilter = d2d.forecastFilter;
                this.deltaFilter = d2d.deltaFilter;
                this.loadMode = d2d.loadMode;
            }
        }
        resetMultiload();
    }

    @Override
    public void resetMultiload() {
        configFactory.resetMultiload();
    }

    private boolean validateTimeMatchBasis(IDescriptor descriptor) {
        /*
         * If a resource is shared by multiple panels (this can be the case with
         * tools, at least), then it is necessary to search all of them as
         * resource.descriptor() may not contain resource. TODO: Don't allow
         * this condition to occur?
         */
        IRenderableDisplay display = descriptor.getRenderableDisplay();
        IDisplayPaneContainer container = display != null ? display
                .getContainer() : null;
        if (container != null) {
            for (IDisplayPane pane : container.getDisplayPanes()) {
                IRenderableDisplay paneDisplay = pane.getRenderableDisplay();
                IDescriptor paneDescriptor = paneDisplay != null ? paneDisplay
                        .getDescriptor() : null;
                if ((paneDescriptor != null)
                        && validateTimeMatchBasis(paneDescriptor
                                .getResourceList())) {
                    return true;
                }
            }
        } else {
            return validateTimeMatchBasis(descriptor.getResourceList());
        }
        return false;
    }

    private boolean validateTimeMatchBasis(ResourceList list) {
        for (ResourcePair rp : list) {
            AbstractVizResource<?, ?> rsc = rp.getResource();
            if (rsc == timeMatchBasis) {
                return true;
            } else if (rp.getProperties().isMapLayer()
                    || rp.getProperties().isSystemResource()) {
                continue;
            } else if ((rsc != null)
                    && (rsc.getResourceData() instanceof IResourceGroup)) {
                if (validateTimeMatchBasis(((IResourceGroup) rsc
                        .getResourceData()).getResourceList())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasContainer(IDescriptor descriptor) {
        IRenderableDisplay display = descriptor.getRenderableDisplay();
        return display.getContainer() != null;
    }

    private boolean validateDescriptor(IDescriptor descriptor) {
        IRenderableDisplay display = descriptor.getRenderableDisplay();
        IDisplayPaneContainer container = display != null ? display
                .getContainer() : null;
        if (container != null) {
            for (IDisplayPane pane : container.getDisplayPanes()) {
                IRenderableDisplay paneDisplay = pane.getRenderableDisplay();
                IDescriptor paneDescriptor = paneDisplay != null ? paneDisplay
                        .getDescriptor() : null;
                if (paneDescriptor == descriptor)
                    return true;
            }
        }
        return false;
    }

}
