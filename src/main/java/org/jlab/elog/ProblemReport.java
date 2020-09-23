package org.jlab.elog;

import org.jlab.elog.exception.LogRuntimeException;

/**
 * <p>An immutable problem report associated with a log entry.</p>
 * 
 * <ul>
 * <li>System IDs can be obtained from  <a href="https://accweb/hco/data/systems">https://accweb/hco/data/systems</a>.</li>
 * <li>Group IDs can be obtained from  <a href="https://accweb/hco/data/groups">https://accweb/hco/data/groups</a>.</li>
 * <li>Component IDs can be obtained from <a href="https://accweb/hco/data/components">https://accweb/hco/data/components</a>.</li>
 * </ul>
 * 
 * @author ryans
 */
public final class ProblemReport {
    private final ProblemReportType type;
    private final boolean needsAttention;
    private final int systemId;
    private final int groupId;
    private final Integer componentId;

    /**
     * Create a new Problem Report with the specified values.
     * 
     * @param type The problem report type
     * @param needsAttention true if the report needs attention, false if resolved
     * @param systemId The system ID 
     * @param groupId The group ID
     * @param componentId The optional component ID (may be null)
     * @throws LogRuntimeException if type is null
     */
    public ProblemReport(ProblemReportType type, boolean needsAttention, int systemId, int groupId,
            Integer componentId) throws LogRuntimeException {
        
        // Fail early instead of waiting until they try to use the ProblemReport later
        if(type == null) {
            throw new LogRuntimeException("type cannot be null");
        }
        
        this.type = type;
        this.needsAttention = needsAttention;
        this.systemId = systemId;
        this.groupId = groupId;
        this.componentId = componentId;
    }

    /**
     * Return the problem report type.
     * 
     * @return The problem report type 
     */
    public ProblemReportType getType() {
        return type;
    }

    /**
     * Return true if the problem report needs attention, false if it has been resolved.
     * 
     * @return true for attention, false otherwise
     */
    public boolean isNeedsAttention() {
        return needsAttention;
    }

    /**
     * Return the system ID.
     * 
     * @return The system ID
     */
    public int getSystemId() {
        return systemId;
    }

    /**
     * Return the group ID.
     * 
     * @return The group ID
     */
    public int getGroupId() {
        return groupId;
    }

    /**
     * Return the component ID or null if none.
     * 
     * @return The component ID or null
     */
    public Integer getComponentId() {
        return componentId;
    }
}
