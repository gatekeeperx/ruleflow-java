package com.gatekeeperx.ruleflow.vo;

import java.util.Objects;

public class WorkflowInfo {
    private final String country;
    private final String version;
    private final String workflowName;

    public WorkflowInfo(String country, String workflowName) {
        this(country, null, workflowName);
    }

    public WorkflowInfo(String country, String version, String workflowName) {
        this.country = country;
        this.version = version;
        this.workflowName = workflowName;
    }

    public String getCountry() {
        return country;
    }

    public String getVersion() {
        return version;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        WorkflowInfo that = (WorkflowInfo) object;
        return Objects.equals(country, that.country) && Objects.equals(version, that.version) && Objects.equals(workflowName, that.workflowName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, version, workflowName);
    }

}