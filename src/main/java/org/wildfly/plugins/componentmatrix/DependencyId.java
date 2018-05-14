/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.wildfly.plugins.componentmatrix;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DependencyId implements Comparable<DependencyId>{
    private final String groupId;
    private final String artifactId;
    private final String type;
    private final String classifier;
    private final String scope;

    public DependencyId(Artifact artifact) {
        this.groupId = artifact.getGroupId();
        this.artifactId = artifact.getArtifactId();
        this.type = artifact.getType();
        this.classifier = artifact.getClassifier();
        this.scope = artifact.getScope();
    }

    public DependencyId(Dependency dependency) {
        this.groupId = dependency.getGroupId();
        this.artifactId = dependency.getArtifactId();
        this.type = dependency.getType();
        this.classifier = dependency.getClassifier();
        this.scope = dependency.getScope();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyId that = (DependencyId) o;

        if (!groupId.equals(that.groupId)) return false;
        if (!artifactId.equals(that.artifactId)) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) return false;
        return scope != null ? scope.equals(that.scope) : that.scope == null;
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(DependencyId o) {
        int curr = groupId.compareTo(o.groupId);
        if (curr != 0) {
            return curr;
        }
        curr = artifactId.compareTo(o.artifactId);
        if (curr != 0) {
            return curr;
        }
        curr = compareNullable(type, o.type);
        if (curr != 0) {
            return curr;
        }
        curr = compareNullable(classifier, o.classifier);
        if (curr != 0) {
            return curr;
        }
        curr = compareNullable(scope, o.scope);
        if (curr != 0) {
            return curr;
        }
        return 0;
    }

    private int compareNullable(String a, String b) {
        if (a == null && b == null) {
            return 0;
        } else if (a != null && b != null) {
            return a.compareTo(b);
        } else if (a == null && b != null) {
            return 1;
        } else if (a != null && b == null) {
            return -1;
        }
        return 0;
    }

    public String toString () {
        StringBuilder sb = new StringBuilder(groupId + ":" + artifactId);
        sb.append(":");
        if (type != null) {
            sb.append(type);
        }
        sb.append(":");
        if (classifier != null) {
            sb.append(classifier);
        }
        sb.append(":");
        if (scope != null) {
            sb.append(scope);
        }
        return sb.toString();
    }
}
