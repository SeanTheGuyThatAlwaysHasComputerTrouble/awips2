package com.raytheon.uf.common.archive.config;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import com.raytheon.uf.common.util.SizeUtil;

/**
 * This class contains the information on directories that are associated with a
 * display label. Allows a GUI to maintain the state of the display instead of
 * the manager.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 7, 2013  1966       rferrel     Initial creation
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */
public class DisplayData implements Comparable<DisplayData> {

    /** Comparator ordering by size. */
    public static final Comparator<DisplayData> SIZE_ORDER = new Comparator<DisplayData>() {
        @Override
        public int compare(DisplayData o1, DisplayData o2) {
            int result = 0;
            long diff = o1.size - o2.size;
            if (diff < 0L) {
                result = -1;
            } else if (diff > 0L) {
                result = 1;
            }
            return result;
        }
    };

    /** Comparator ordering by label. */
    public static final Comparator<DisplayData> LABEL_ORDER = new Comparator<DisplayData>() {
        @Override
        public int compare(DisplayData o1, DisplayData o2) {
            return o1.displayLabel.compareToIgnoreCase(o2.displayLabel);
        }
    };

    /** Label to use when size not yet known. */
    public static final String UNKNOWN_SIZE_LABEL = "????";

    /** A negative value to indicate unknown size. */
    public static final long UNKNOWN_SIZE = -1L;

    /** The data's archive configuration. */
    protected final ArchiveConfig archiveConfig;

    /** The data's category configuration. */
    protected final CategoryConfig categoryConfig;

    /** The display label for this data. */
    protected final String displayLabel;

    /**
     * List of directories for the display label matching the category's
     * directory pattern and found under the archive's root directory.
     */
    protected final List<File> dirs;

    /**
     * For use by GUI to indicate. Use to indicate selected for retention or for
     * placing in a case.
     */
    private boolean selected = false;

    /** For use by GUI for indicating the size of the directories' contents. */
    private long size = UNKNOWN_SIZE;

    /**
     * Constructor.
     * 
     * @param archiveConfig
     * @param categoryConfig
     * @param displayLabel
     * @param dirs
     */
    public DisplayData(ArchiveConfig archiveConfig,
            CategoryConfig categoryConfig, String displayLabel, List<File> dirs) {
        this.archiveConfig = archiveConfig;
        this.categoryConfig = categoryConfig;
        this.displayLabel = displayLabel;
        this.dirs = dirs;
    }

    /**
     * Is instance selected.
     * 
     * @return selected
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Set selected state.
     * 
     * @param selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * 
     * @return displayLabel.
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    /**
     * The size of the directories' contents.
     * 
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * The string label for the size.
     * 
     * @return sizeLabel
     */
    public String getSizeLabel() {
        String label = UNKNOWN_SIZE_LABEL;
        if (size >= 0L) {
            label = SizeUtil.prettyByteSize(size);
        }
        return label;
    }

    /**
     * Set the size of the directories' contents.
     * 
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * The archive's root directory name.
     * 
     * @return rootDir
     */
    public String getRootDir() {
        return archiveConfig.getRootDir();
    }

    /**
     * Determine if this is the name of the archive.
     * 
     * @param archiveName
     * @return
     */
    public boolean isArchive(String archiveName) {
        return archiveConfig.getName().equals(archiveName);
    }

    /**
     * Determine if this is the name of the category.
     * 
     * @param categoryName
     * @return
     */
    public boolean isCategory(String categoryName) {
        return categoryConfig.getName().equals(categoryName);
    }

    /**
     * Update the information for the category.
     */
    public void updateCategory() {
        if (isSelected()) {
            categoryConfig.addSelectedDisplayName(displayLabel);
        } else {
            categoryConfig.removeSelectedDisplayName(displayLabel);
        }
    }

    /**
     * Determine if the object contains the same data as the instance.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof DisplayData) {
            DisplayData displayData = (DisplayData) object;
            return compareTo(displayData) == 0;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(DisplayData o) {
        int result = archiveConfig.getName().compareTo(
                o.archiveConfig.getName());
        if (result == 0) {
            result = categoryConfig.getName().compareTo(
                    o.categoryConfig.getName());
            if (result == 0) {
                result = displayLabel.compareTo(o.displayLabel);
            }
        }
        return result;
    }

    public String getArchiveName() {
        return archiveConfig.getName();
    }

    public String getCategoryName() {
        return categoryConfig.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DisplayData[");
        sb.append("displayLabel: ").append(displayLabel);
        sb.append(", isSlected: ").append(isSelected());
        sb.append(", size: ").append(size);
        sb.append(", category.name: ").append(categoryConfig.getName());
        sb.append(", archive.name: ").append(archiveConfig.getName())
                .append("]");
        return sb.toString();
    }
}