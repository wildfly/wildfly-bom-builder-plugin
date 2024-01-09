package org.wildfly.plugins.bombuilder;

import org.apache.maven.model.Dependency;

/**
 * @author emmartins
 */
public class IncludeDependency extends Dependency {

    private Boolean transitive;

    private InheritExclusions inheritExclusions;

    public Boolean getTransitive() {
        return transitive;
    }

    public void setTransitive(Boolean transitive) {
        this.transitive = transitive;
    }

    public InheritExclusions getInheritExclusions() {
        return inheritExclusions;
    }

    public void setInheritExclusions(final InheritExclusions inheritExclusions) {
        this.inheritExclusions = inheritExclusions;
    }
}

