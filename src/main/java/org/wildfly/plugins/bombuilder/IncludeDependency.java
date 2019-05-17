package org.wildfly.plugins.bombuilder;

import org.apache.maven.model.Dependency;

/**
 * @author emmartins
 */
public class IncludeDependency extends Dependency {

    private Boolean transitive;

    public Boolean getTransitive() {
        return transitive;
    }

    public void setTransitive(Boolean transitive) {
        this.transitive = transitive;
    }
}

