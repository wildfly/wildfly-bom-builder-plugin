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

import static org.codehaus.plexus.util.StringUtils.defaultString;
import static org.codehaus.plexus.util.StringUtils.trim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

/**
 * Build a BOM based on the dependencies in a GAV
 */
@Mojo(name = "build-bom", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildBomMojo
        extends AbstractMojo {

    private static final String VERSION_PROPERTY_PREFIX = "version.";


    /**
     * Set the parent of the generated bom
     */
    @Parameter
    private Parent parent;

    /**
     * BOM groupId
     */
    @Parameter(required = true)
    private String bomGroupId;

    /**
     * BOM artifactId
     */
    @Parameter(required = true)
    private String bomArtifactId;

    /**
     * BOM version
     */
    @Parameter(required = true)
    private String bomVersion;

    /**
     * BOM name
     */
    @Parameter(defaultValue = "")
    private String bomName;

    /**
     * BOM description
     */
    @Parameter(defaultValue = "")
    private String bomDescription;

    /**
     * Set to {@code true} to generate the licenses
     */
    @Parameter
    private boolean licenses = false;

    /**
     * BOM output file
     */
    @Parameter(defaultValue = "bom-pom.xml")
    String outputFilename;

    /**
     * Whether the BOM should include the dependency exclusions that
     * are present in the source POM.  By default the exclusions
     * will not be copied to the new BOM.
     */
    @Parameter
    private List<BomExclusion> exclusions;

    /**
     * List of dependencies which should not be added to BOM
     */
    @Parameter
    private List<DependencyExclusion> dependencyExclusions;

    /**
     * Whether or not to inherit exclusions
     */
    @Parameter
    private boolean inheritExclusions;

    @Parameter
    private Set<String> includeProfiles;

    /**
     * The current project
     */
    @Component
    MavenProject mavenProject;

    /**
     *
     */
    @Component
    private ModelBuilder modelBuilder;

    /**
     *
     */
    @Component
    private ProjectBuilder projectBuilder;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;


    private final PomDependencyVersionsTransformer versionsTransformer;
    private final ModelWriter modelWriter;

    public BuildBomMojo() {
        this(new ModelWriter(), new PomDependencyVersionsTransformer());
    }

    public BuildBomMojo(ModelWriter modelWriter, PomDependencyVersionsTransformer versionsTransformer) {
        this.versionsTransformer = versionsTransformer;
        this.modelWriter = modelWriter;
    }

    public void execute()
            throws MojoExecutionException {
        getLog().debug("Generating BOM");
        Model model = initializeModel();
        addDependencyManagement(model);

        model = versionsTransformer.transformPomModel(model);
        getLog().debug("Dependencies versions converted to properties");

        final File file = new File(mavenProject.getBuild().getDirectory(), outputFilename);
        modelWriter.writeModel(model, file);

        // Add the artifact
        Artifact projectArtifact = mavenProject.getArtifact();
        final Artifact pomArtifact =
                new DefaultArtifact(
                        bomGroupId, bomArtifactId, bomVersion,
                        null, "pom", null, artifactHandlerManager.getArtifactHandler("pom"));
        pomArtifact.setFile(file);
        mavenProject.addAttachedArtifact( pomArtifact );
    }

    private Model initializeModel() {
        Model pomModel = new Model();
        pomModel.setModelVersion("4.0.0");

        pomModel.setGroupId(bomGroupId);
        pomModel.setArtifactId(bomArtifactId);
        pomModel.setVersion(bomVersion);
        pomModel.setPackaging("pom");

        pomModel.setName(bomName);
        pomModel.setDescription(bomDescription);

        pomModel.setProperties(new OrderedProperties());
        pomModel.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");

        if (licenses) {
            pomModel.setLicenses(mavenProject.getLicenses());
        }
        if (parent != null) {
            pomModel.setParent(parent);
        }

        Set<String> addedProfiles = new HashSet<>();
        List<Profile> profiles = new ArrayList<>();
        MavenProject current = mavenProject;
        while (current != null) {
            Model currModel = current.getModel();
            if (currModel != null) {
                for ( Profile profile : currModel.getProfiles() ) {
                    if (includeProfiles.contains(profile.getId()) && !addedProfiles.contains(profile.getId())) {
                        profiles.add(profile);
                        addedProfiles.add(profile.getId());
                    }
                }
            }
            current = current.getParent();
        }
        if (profiles.size() > 0) {
            pomModel.setProfiles(profiles);
        }

        return pomModel;
    }

    private void addDependencyManagement(Model pomModel) {
        Map<DependencyId, Dependency> originalDeps = createDependencyMap(mavenProject.getDependencyManagement());

        Properties versionProperties = new Properties();
        DependencyManagement depMgmt = new DependencyManagement();
        for (Dependency originalDependency : mavenProject.getDependencyManagement().getDependencies()) {
            if (isExcludedDependency(originalDependency)) {
                continue;
            }

            String versionPropertyName = VERSION_PROPERTY_PREFIX + originalDependency.getGroupId();
            if (versionProperties.getProperty(versionPropertyName) != null
                    && !versionProperties.getProperty(versionPropertyName).equals(originalDependency.getVersion())) {
                versionPropertyName = VERSION_PROPERTY_PREFIX + originalDependency.getGroupId() + "." + originalDependency.getArtifactId();
            }
            versionProperties.setProperty(versionPropertyName, originalDependency.getVersion());

            Dependency dep = new Dependency();
            dep.setGroupId(originalDependency.getGroupId());
            dep.setArtifactId(originalDependency.getArtifactId());
            dep.setVersion(originalDependency.getVersion());
            if (!StringUtils.isEmpty(originalDependency.getClassifier())) {
                dep.setClassifier(originalDependency.getClassifier());
            }
            if (!StringUtils.isEmpty(originalDependency.getType())) {
                dep.setType(originalDependency.getType());
            }
            if (!StringUtils.isEmpty(originalDependency.getScope())) {
                dep.setScope(originalDependency.getScope());
            }
            if (exclusions != null) {
                applyExclusions(originalDependency, dep);
            }
            if (inheritExclusions) {
                inheritExclusions(originalDeps, originalDependency, dep);
            }
            depMgmt.addDependency(dep);
        }
        pomModel.setDependencyManagement(depMgmt);
        getLog().debug("Added " + depMgmt.getDependencies().size() + " dependencies to dependency management.");
    }

    private void inheritExclusions(Map<DependencyId, Dependency> originalDeps, Dependency artifact, Dependency dep) {
        Dependency originalDependency = originalDeps.get(new DependencyId(artifact));
        if (originalDependency == null) {
            getLog().warn("Could not find dependency for " + artifact);
            return;
        }
        for (Exclusion originalExclusion : originalDependency.getExclusions()) {
            dep.addExclusion(originalExclusion.clone());
        }
    }

    boolean isExcludedDependency(Dependency dependency) {
        if (dependencyExclusions == null || dependencyExclusions.size() == 0) {
            return false;
        }
        for (DependencyExclusion exclusion : dependencyExclusions) {
            if (matchesExcludedDependency(dependency, exclusion)) {
                getLog().debug("Artifact " + dependency.getGroupId() + ":" + dependency.getArtifactId() + " matches excluded dependency " + exclusion.getGroupId() + ":" + exclusion.getArtifactId());
                return true;
            }
        }
        return false;
    }

    boolean matchesExcludedDependency(Dependency artifact, DependencyExclusion exclusion) {
        String groupId = defaultAndTrim(artifact.getGroupId());
        String artifactId = defaultAndTrim(artifact.getArtifactId());
        String exclusionGroupId = defaultAndTrim(exclusion.getGroupId());
        String exclusionArtifactId = defaultAndTrim(exclusion.getArtifactId());
        boolean groupIdMatched = ("*".equals(exclusionGroupId) || groupId.equals(exclusionGroupId));
        boolean artifactIdMatched = ("*".equals(exclusionArtifactId) || artifactId.equals(exclusionArtifactId));
        return groupIdMatched && artifactIdMatched;
    }

    private String defaultAndTrim(String string) {
        return defaultString(trim(string), "");
    }

    private void applyExclusions(Dependency artifact, Dependency dep) {
        for (BomExclusion exclusion : exclusions) {
            if (exclusion.getDependencyGroupId().equals(artifact.getGroupId()) &&
                    exclusion.getDependencyArtifactId().equals(artifact.getArtifactId())) {
                Exclusion ex = new Exclusion();
                ex.setGroupId(exclusion.getExclusionGroupId());
                ex.setArtifactId(exclusion.getExclusionArtifactId());
                dep.addExclusion(ex);
            }
        }
    }

    private Map<DependencyId, Dependency> createDependencyMap(DependencyManagement dependencyManagement) {
        if (dependencyManagement == null) {
            return Collections.emptyMap();
        }
        Map<DependencyId, Dependency> dependencyMap = new HashMap<>();
        for (Dependency dep : dependencyManagement.getDependencies()) {
            dependencyMap.put(new DependencyId(dep), dep);
        }
        return dependencyMap;
    }

    static class ModelWriter {

        void writeModel(Model pomModel, File outputFile)
                throws MojoExecutionException {
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(outputFile)) {
                MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
                mavenWriter.write(writer, pomModel);
            } catch (IOException e) {
                e.printStackTrace();
                throw new MojoExecutionException("Unable to write pom file.", e);
            }

        }
    }

}
