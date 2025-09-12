package io.github.iamrenny.ruleflow.vo;

import java.util.Map;
import java.util.Objects;

public class Action {
    private String name;
    private Map<String, String> params;

    public Action(String name, Map<String, String> params) {
        this.name = name;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action action)) return false;
        return Objects.equals(name, action.name) && Objects.equals(params, action.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, params);
    }

    @Override
    public String toString() {
        return "Action{" +
            "name='" + name + '\'' +
            ", params=" + params +
            '}';
    }
}