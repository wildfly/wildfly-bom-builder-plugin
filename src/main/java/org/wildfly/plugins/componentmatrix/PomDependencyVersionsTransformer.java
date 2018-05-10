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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

class PomDependencyVersionsTransformer {


    public Model transformPomModel(Model model) {
        Model pomModel = model.clone();
        DependencyManagement depMgmt = pomModel.getDependencyManagement();
        Map<String, String> groupIdArtifactIdVersions = new TreeMap<>();
        Map<String, String> groupIdArtifactIdPropertyNames = new TreeMap<>();
        Map<String, String> groupIdVersions = new TreeMap<>();
        Map<String, Set<String>> groupIdArtifactIds = new TreeMap<>();
        for (Dependency dependency : depMgmt.getDependencies()) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String groupIdArtifactId = groupId + ":" + artifactId;
            groupIdArtifactIdVersions.put(groupIdArtifactId, dependency.getVersion());
            groupIdVersions.put(groupId, dependency.getVersion());

            Set<String> artifactIds = groupIdArtifactIds.get(groupId);
            if (artifactIds == null) {
                artifactIds = new HashSet<>();
                groupIdArtifactIds.put(groupId, artifactIds);
            }
            artifactIds.add(artifactId);
        }

        Properties properties = pomModel.getProperties();
        for (Map.Entry<String, String> groupVersion : groupIdVersions.entrySet()) {
            String groupId = groupVersion.getKey();
            Set<String> artifactIds = groupIdArtifactIds.get(groupId);
            if (artifactIds.size() == 1 || allArtifactsInGroupHaveSameVersion(groupId, groupIdArtifactIdVersions, artifactIds)) {
                String propertyName = "version." + groupId;
                properties.setProperty(propertyName, groupVersion.getValue());
                for (String artifactId : artifactIds) {
                    String groupIdArtifactId = groupId + ":" + artifactId;
                    groupIdArtifactIdPropertyNames.put(groupIdArtifactId, propertyName);
                }
            } else {
                for (String artifactId : artifactIds) {
                    String groupIdArtifactId = groupId + ":" + artifactId;
                    String propertyName = "version." + groupId + "." + artifactId;
                    groupIdArtifactIdPropertyNames.put(groupIdArtifactId, propertyName);
                    properties.setProperty(propertyName, groupIdArtifactIdVersions.get(groupIdArtifactId));
                }
            }
        }
        for (Dependency dependency : depMgmt.getDependencies()) {
            String groupId = dependency.getGroupId();
            String artifactId = dependency.getArtifactId();
            String groupIdArtifactId = groupId + ":" + artifactId;
            String propertyName = groupIdArtifactIdPropertyNames.get(groupIdArtifactId);
            dependency.setVersion("${" + propertyName + "}");
        }
        return pomModel;
    }

    private boolean allArtifactsInGroupHaveSameVersion(String groupId, Map<String, String> groupIdArtifactIdVersions, Set<String> artifactIds) {
        String version = null;
        for (String artifactId : artifactIds) {
            String groupIdArtifactId = groupId + ":" + artifactId;
            if (version == null) {
                version = groupIdArtifactIdVersions.get(groupIdArtifactId);
            } else {
                if (!version.equals(groupIdArtifactIdVersions.get(groupIdArtifactId))) {
                    return false;
                }
            }
        }
        return true;
    }

}
