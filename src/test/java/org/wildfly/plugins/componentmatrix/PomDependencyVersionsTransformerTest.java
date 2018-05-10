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

import static org.junit.Assert.assertEquals;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.junit.Test;

public class PomDependencyVersionsTransformerTest {
    @Test
    public void testDependencyVersionsAreSpecifiedByProperties() throws Exception {
        PomDependencyVersionsTransformer transformer = new PomDependencyVersionsTransformer();
        Model pomModel = createPomModel();
        Dependency dependency = createDependency("groupId", "artifactId", "version");
        addDependency(pomModel, dependency);

        Model transformedModel = transformer.transformPomModel(pomModel);

        assertEquals(1, transformedModel.getProperties().size());
        String versionKey = createKey(dependency);
        assertEquals(dependency.getVersion(), transformedModel.getProperties().get(versionKey));
        assertEquals("${" + versionKey + "}", transformedModel.getDependencyManagement().getDependencies().get(0).getVersion());
    }

    @Test
    public void testDependencyVersionIsSpecifiedByPropertyWithKeyIncludingArtifactId() throws Exception {
        PomDependencyVersionsTransformer transformer = new PomDependencyVersionsTransformer();
        Model pomModel = createPomModel();
        Dependency dependency1 = createDependency("groupId", "artifactId1", "version1");
        addDependency(pomModel, dependency1);
        Dependency dependency2 = createDependency("groupId", "artifactId2", "version2");
        addDependency(pomModel, dependency2);

        Model transformedModel = transformer.transformPomModel(pomModel);

        assertEquals(2, transformedModel.getProperties().size());
        String versionKey1 = createKeyIncludingArtifactId(dependency1);
        assertEquals(dependency1.getVersion(), transformedModel.getProperties().get(versionKey1));
        assertEquals("${" + versionKey1 + "}", transformedModel.getDependencyManagement().getDependencies().get(0).getVersion());

        String versionKey2 = createKeyIncludingArtifactId(dependency2);
        assertEquals(dependency2.getVersion(), transformedModel.getProperties().get(versionKey2));
        assertEquals("${" + versionKey2 + "}", transformedModel.getDependencyManagement().getDependencies().get(1).getVersion());
    }

    // FIXME one groupId with same versions, but configuration requires property for given artifactId

    private String createKey(Dependency dependency) {
        return "version." + dependency.getGroupId();
    }

    private String createKeyIncludingArtifactId(Dependency dependency) {
        return "version." + dependency.getGroupId() + "." + dependency.getArtifactId();
    }

    private Dependency createDependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }


    private void addDependency(Model pomModel, Dependency dependency) {
        pomModel.getDependencyManagement().addDependency(dependency);
    }

    private Model createPomModel() {
        Model model = new Model();
        DependencyManagement dependencyManagement = new DependencyManagement();
        model.setDependencyManagement(dependencyManagement);
        model.setProperties(new OrderedProperties());
        return model;
    }

}