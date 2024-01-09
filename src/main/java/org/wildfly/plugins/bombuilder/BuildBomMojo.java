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

package org.wildfly.plugins.bombuilder;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.wildfly.channel.Channel;
import org.wildfly.channel.ChannelSession;
import org.wildfly.channel.UnresolvedMavenArtifactException;
import org.wildfly.channel.maven.VersionResolverFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import static org.codehaus.plexus.util.StringUtils.defaultString;
import static org.codehaus.plexus.util.StringUtils.trim;
import static org.wildfly.channel.maven.VersionResolverFactory.DEFAULT_REPOSITORY_MAPPER;

/**
 * Build a BOM based on the dependencies in a GAV
 */
@Mojo(name = "build-bom", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class BuildBomMojo
        extends AbstractMojo {

    private static final String WILDCARD = "*";

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
     * List of exclusions to add to the BOM
     */
    @Parameter
    private List<AddExclusion> addExclusions;

    /**
     * List of source's managed dependencies which should not be added to BOM
     */
    @Parameter
    private List<Dependency> excludeDependencies;

    /**
     * List of source's managed dependencies which should be added to BOM.
     */
    @Parameter
    private List<IncludeDependency> includeDependencies;

    /**
     * List of source's managed dependencies which (only) its transitives should be added to BOM, regardless of scope.
     */
    @Parameter
    private List<IncludeDependency> includeDependenciesTransitives;

    /**
     * List of dependencies which should be imported to BOM
     */
    @Parameter
    private List<Dependency> importDependencies;

    /**
     * List of dependencies which should be added to BOM, with a version that references to an existent dependency version.
     * The version reference should use {@link org.apache.maven.model.Dependency#getManagementKey()} format.
     */
    @Parameter
    private List<Dependency> versionRefDependencies;

    /**
     * Set to {@code false} to, when including a dependency, do not include its transitives.
     */
    @Parameter
    private boolean includeTransitives = true;

    /**
     * Set to {@code true} to build a BOM with dependencies. If the includeDependencies param is specified then only the included dependencies are added to the BOM's dependencies, otherwise all managed dependencies are added.
     */
    @Parameter
    private boolean bomWithDependencies = false;

    /**
     * A list of exclusions which should be ignored when defining the exclusions for included dependencies.
     */
    @Parameter(alias = "ignored-exclusions")
    private List<Exclusion> ignoredExclusions;

    /**
     * Which dependency exclusions, present in the source POM, should be included in the BOM:
     *
     *  NONE - no exclusion should be inherited
     *  ALL - inherits all exclusions
     *  UNMANAGED - only inherits exclusions which targets dependencies not in the BOM's dependency management.
     *
     * The default value is NONE.
     */
    @Parameter(defaultValue = "NONE")
    private InheritExclusions inheritExclusions;

    @Parameter
    private Set<String> includeProfiles;

    @Parameter
    private Set<String> includePlugins;

    @Parameter
    private Set<String> includeRepositories;

    /**
     * The current project
     */
    @Component
    MavenProject mavenProject;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    @Parameter( defaultValue = "${repositorySystemSession}", readonly = true, required = true )
    private RepositorySystemSession repositorySystemSession;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter( defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true )
    private List<RemoteRepository> repositories;

    @Component
    private ProjectDependenciesResolver projectDependenciesResolver;

    private final PomDependencyVersionsTransformer versionsTransformer;
    private final ModelWriter modelWriter;


    @Parameter(alias = "channels", required = false)
    private List<ChannelConfiguration> channels;

    public BuildBomMojo() {
        this(new ModelWriter(), new PomDependencyVersionsTransformer());
    }

    public BuildBomMojo(ModelWriter modelWriter, PomDependencyVersionsTransformer versionsTransformer) {
        this.versionsTransformer = versionsTransformer;
        this.modelWriter = modelWriter;
    }

    public void execute()
            throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Generating BOM");
        }
        Model model = initializeModel();
        addDependencyManagement(model);
        finalizeModel(model);
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
            if (parent.getGroupId() == null) {
                throw new IllegalArgumentException("No groupId was set for the parent");
            }
            if (parent.getArtifactId() == null) {
                throw new IllegalArgumentException("No artifactId was set for the parent");
            }
            if (parent.getVersion() == null) {
                MavenProject current = mavenProject;
                while (current != null) {
                    if (current.getGroupId().equals(parent.getGroupId()) && current.getArtifactId().equals(parent.getArtifactId())) {
                        parent.setVersion(current.getVersion());
                        break;
                    }
                    current = current.getParent();
                }
                if (parent.getVersion() == null) {
                    throw new IllegalArgumentException("No version was set for the parent " + parent.getGroupId() + ":" + parent.getArtifactId() +
                            " and it cannot be determined from the parents of the consuming pom");
                }
            }
            pomModel.setParent(parent);
        }

        if (includeProfiles != null && !includeProfiles.isEmpty()) {
            Set<String> addedProfiles = new HashSet<>();
            List<Profile> profiles = new ArrayList<>();
            MavenProject current = mavenProject;
            while (current != null) {
                Model currModel = current.getModel();
                if (currModel != null && currModel.getProfiles() != null) {
                    for (Profile profile : currModel.getProfiles()) {
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
        }

        if (includeRepositories != null) {
            Set<String> addedRepositories = new HashSet<>();
            Set<String> addedPluginRepositories = new HashSet<>();
            List<Repository> repositories = new ArrayList<>();
            List<Repository> pluginRepositories = new ArrayList<>();
            MavenProject current = mavenProject;
            while (current != null) {
                Model currModel = current.getModel();
                if (currModel != null) {
                    if (currModel.getRepositories() != null) {
                        for (Repository repository : currModel.getRepositories()) {
                            if ((includeRepositories.isEmpty() || includeRepositories.contains(repository.getId())) && !addedRepositories.contains(repository.getId())) {
                                repositories.add(repository);
                                addedRepositories.add(repository.getId());
                            }
                        }
                    }
                    if (currModel.getPluginRepositories() != null) {
                        for (Repository pluginRepository : currModel.getPluginRepositories()) {
                            if ((includeRepositories.isEmpty() || includeRepositories.contains(pluginRepository.getId())) && !addedPluginRepositories.contains(pluginRepository.getId())) {
                                pluginRepositories.add(pluginRepository);
                                addedPluginRepositories.add(pluginRepository.getId());
                            }
                        }
                    }
                }
                current = current.getParent();
            }
            if (repositories.size() > 0) {
                pomModel.setRepositories(repositories);
            }
            if (pluginRepositories.size() > 0) {
                pomModel.setPluginRepositories(pluginRepositories);
            }
        }

        if (includePlugins != null && !includePlugins.isEmpty()) {
            Set<String> addedPlugins = new HashSet<>();
            List<Plugin> plugins = new ArrayList<>();
            MavenProject current = mavenProject;
            while (current != null) {
                Model currModel = current.getModel();
                if (currModel != null) {
                    final Build build = currModel.getBuild();
                    if (build != null) {
                        final PluginManagement pluginManagement = build.getPluginManagement();
                        if (pluginManagement != null && pluginManagement.getPlugins() != null) {
                            for (Plugin plugin : pluginManagement.getPlugins()) {
                                if (includePlugins.contains(plugin.getArtifactId()) && addedPlugins.add(plugin.getArtifactId())) {
                                    plugins.add(plugin);
                                } else {
                                    final String ga = plugin.getGroupId()+":"+plugin.getArtifactId();
                                    if (includePlugins.contains(ga) && addedPlugins.add(ga)) {
                                        plugins.add(plugin);
                                    }
                                }
                            }
                        }
                    }
                }
                current = current.getParent();
            }
            if (plugins.size() > 0) {
                Build build = pomModel.getBuild();
                if (build == null) {
                    build = new Build();
                    pomModel.setBuild(build);
                }
                PluginManagement pluginManagement = build.getPluginManagement();
                if (pluginManagement == null) {
                    pluginManagement = new PluginManagement();
                    build.setPluginManagement(pluginManagement);
                }
                pluginManagement.setPlugins(plugins);
            }
        }

        return pomModel;
    }

    private void finalizeModel(Model model) throws MojoExecutionException {
        // if dep management was generated replace versions with properties
        if (model.getDependencyManagement() != null){
            model = versionsTransformer.transformPomModel(model);
            if (getLog().isDebugEnabled()) {
                getLog().debug("Dependencies versions converted to properties");
            }
        }
        // write pom
        final File file = new File(mavenProject.getBuild().getDirectory(), outputFilename);
        modelWriter.writeModel(model, file);
        // attach the artifact
        final Artifact pomArtifact =
                new DefaultArtifact(
                        bomGroupId, bomArtifactId, bomVersion,
                        null, "pom", null, artifactHandlerManager.getArtifactHandler("pom"));
        pomArtifact.setFile(file);
        mavenProject.addAttachedArtifact( pomArtifact );
    }

    private void addDependencyManagement(Model pomModel) throws MojoExecutionException {
        if (mavenProject.getDependencyManagement() == null || mavenProject.getDependencyManagement().getDependencies() == null) {
            return;
        }
        pomModel.setDependencyManagement(new DependencyManagement());
        // gather initial managed deps from source
        final Map<String, Dependency> managedDependenciesMap = new TreeMap<>();
        final List<String> orderedManagedDependencies = new ArrayList<>();
        final Set<String> managedExclusions = new HashSet<>();
        final List<String> includedManagedDependencies = new ArrayList<>();
        final List<String> includedManagedDependenciesWithTransitives = new ArrayList<>();
        ChannelSession channelSession = null;
        if (this.channels != null) {
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            final List<Channel> channels = new ArrayList<>();
            session.setLocalRepositoryManager(repositorySystemSession.getLocalRepositoryManager());
            session.setOffline(repositorySystemSession.isOffline());
            Map<String, RemoteRepository> mapping = new HashMap<>();
            for (RemoteRepository r : repositories) {
                mapping.put(r.getId(), r);
            }
            for (ChannelConfiguration channelConfiguration : this.channels) {
                channels.add(channelConfiguration.toChannel(repositories));
            }
            Function<org.wildfly.channel.Repository, RemoteRepository> mapper = r -> {
                RemoteRepository rep = mapping.get(r.getId());
                if (rep == null) {
                    rep = DEFAULT_REPOSITORY_MAPPER.apply(r);
                }
                return rep;
            };
            VersionResolverFactory factory = new VersionResolverFactory(repositorySystem, session, mapper);
            channelSession = new ChannelSession(channels, factory);
        }
        for (Dependency dependency : mavenProject.getDependencyManagement().getDependencies()) {
            if (isExcludedDependency(dependency) && getIncludedTransitiveDependency(dependency) == null) {
                getLog().info("Skipping dependency excluded by config: "+dependency.getManagementKey());
                continue;
            }
            addBuilderManagedDependency(dependency, orderedManagedDependencies, managedDependenciesMap, includedManagedDependencies, includedManagedDependenciesWithTransitives, managedExclusions, channelSession);
        }
        // build a dep management clone with no exclusions, this just enforces versions and should be used when resolving transitives for a single dependency
        final List<Dependency> dependencyManagementWithoutExclusions = new ArrayList<>();
        for (Dependency managedDependency : managedDependenciesMap.values()) {
            Dependency managedDependencyClone = managedDependency.clone();
            managedDependencyClone.setExclusions(null);
            dependencyManagementWithoutExclusions.add(managedDependencyClone);
        }
        // build the standard dep exclusions when resolving transitives, which prevents resolving the excluded dependencies
        final List<Exclusion> dependenciesExcludedFromResolving = new ArrayList<>();
        if (excludeDependencies != null && !excludeDependencies.isEmpty()) {
            for (Dependency excludedDependency : excludeDependencies) {
                Exclusion exclusion = new Exclusion();
                exclusion.setGroupId(excludedDependency.getGroupId());
                exclusion.setArtifactId(excludedDependency.getArtifactId());
                if (!isIgnoredExclusion(exclusion)) {
                    dependenciesExcludedFromResolving.add(exclusion);
                }
            }
        }
        // include transitives of dependencies
        if (includeDependenciesTransitives != null) {
            for (IncludeDependency includeDependencyTransitives : includeDependenciesTransitives) {
                // in case transitives of the included transitives should be included too
                final boolean transitive = includeDependencyTransitives.getTransitive() == null ? includeTransitives : includeDependencyTransitives.getTransitive();
                final Dependency dependency = managedDependenciesMap.get(includeDependencyTransitives.getManagementKey());
                if (isExcludedDependency(dependency)) {
                    // an excluded dependency which was mapped only due to later include its transitives, unmap it now
                    getLog().info("Skipping dependency excluded by config: "+dependency.getManagementKey());
                    managedDependenciesMap.remove(dependency.getManagementKey());
                }
                // retrieve and include the dependency transitives
                for (Dependency dependencyTransitive : getDependencyFirstLevelTransitives(dependency, dependenciesExcludedFromResolving, dependencyManagementWithoutExclusions)) {
                    if (!managedDependenciesMap.containsKey(dependencyTransitive.getManagementKey()) || isExcludedDependency(dependencyTransitive)) {
                        // skip unmanaged or excluded
                        continue;
                    }
                    final String managementKey = dependencyTransitive.getManagementKey();
                    includedManagedDependencies.add(managementKey);
                    if (transitive) {
                        includedManagedDependenciesWithTransitives.add(managementKey);
                        getLog().debug("Dependency transitive included (with transitives) by config: "+managementKey);
                    } else {
                        getLog().debug("Dependency transitive included (without transitives) by config: "+managementKey);
                    }
                }
            }
        }
        // add version refs
        if (versionRefDependencies != null) {
            for (Dependency dependency : versionRefDependencies) {
                Dependency versionRef = managedDependenciesMap.get(dependency.getVersion());
                if (versionRef == null) {
                    throw new MojoExecutionException("Dependency "+dependency.getManagementKey()+" version ref "+dependency.getVersion()+" not found");
                }
                dependency.setVersion(versionRef.getVersion());
                addBuilderManagedDependency(dependency, orderedManagedDependencies, managedDependenciesMap, includedManagedDependencies, includedManagedDependenciesWithTransitives, managedExclusions, channelSession);
            }
        }
        // verify all included dependencies were found
        if (includeDependencies != null) {
            for  (IncludeDependency includeDependency : includeDependencies) {
                if (WILDCARD.equals(includeDependency.getGroupId()) || WILDCARD.equals(includeDependency.getArtifactId()) || WILDCARD.equals(includeDependency.getType()) || WILDCARD.equals(includeDependency.getClassifier())) {
                    // skip wildcards
                    continue;
                }
                final String dependencyKey = includeDependency.getManagementKey();
                if (!includedManagedDependencies.contains(dependencyKey) && !includedManagedDependenciesWithTransitives.contains(dependencyKey)) {
                    throw new MojoExecutionException("Dependency to include "+dependencyKey+" not found in builder's dependency management");
                }
            }
        }
        // process managed dep exclusions
        for (Dependency dependency : managedDependenciesMap.values()) {
            switch (getInheritExclusions(dependency)) {
                case ALL:
                    break;
                case NONE:
                    dependency.setExclusions(null);
                    break;
                case UNMANAGED:
                    if (!dependency.getExclusions().isEmpty()) {
                        // resolve wildcards
                        final Map<String, Exclusion> resolvedExclusions = new HashMap<>();
                        for (Exclusion exclusion : dependency.getExclusions()) {
                            if (exclusion.getGroupId().trim().equals(WILDCARD)) {
                                if (exclusion.getArtifactId().trim().equals(WILDCARD)) {
                                    // groupId + artifactId wildcard
                                    for (Dependency transitive : getDependencyFirstLevelTransitives(dependency, dependenciesExcludedFromResolving, dependencyManagementWithoutExclusions)) {
                                        final Exclusion resolvedExclusion = new Exclusion();
                                        resolvedExclusion.setGroupId(transitive.getGroupId());
                                        resolvedExclusion.setArtifactId(transitive.getArtifactId());
                                        final String resolvedExclusionKey = resolvedExclusion.getGroupId()+":"+resolvedExclusion.getArtifactId();
                                        getLog().debug("Exclusion "+resolvedExclusionKey+" resolved for *:* exclusion on dependency "+dependency.getManagementKey());
                                        resolvedExclusions.putIfAbsent(resolvedExclusionKey, resolvedExclusion);
                                    }
                                } else {
                                    // groupId wildcard
                                    final String exclusionArtifactId = exclusion.getArtifactId().trim();
                                    final DependencyFilter dependencyFilter = new DependencyFilter() {
                                        @Override
                                        public boolean accept(DependencyNode node, List<DependencyNode> parents) {
                                            final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
                                            return artifact != null && artifact.getArtifactId().trim().equals(exclusionArtifactId);
                                        }
                                    };
                                    for (Dependency transitive : getDependencyTransitives(dependency, dependencyFilter, dependenciesExcludedFromResolving, dependencyManagementWithoutExclusions)) {
                                        final Exclusion resolvedExclusion = new Exclusion();
                                        resolvedExclusion.setGroupId(transitive.getGroupId());
                                        resolvedExclusion.setArtifactId(transitive.getArtifactId());
                                        final String resolvedExclusionKey = resolvedExclusion.getGroupId()+":"+resolvedExclusion.getArtifactId();
                                        getLog().debug("Exclusion "+resolvedExclusionKey+" resolved for *:"+exclusionArtifactId+" exclusion on dependency "+dependency.getManagementKey());
                                        resolvedExclusions.putIfAbsent(resolvedExclusionKey, resolvedExclusion);
                                    }
                                }
                            } else {
                                if (exclusion.getArtifactId().trim().equals(WILDCARD)) {
                                    // artifactId wildcard
                                    final String exclusionGroupId = exclusion.getGroupId().trim();
                                    final DependencyFilter dependencyFilter = new DependencyFilter() {
                                        @Override
                                        public boolean accept(DependencyNode node, List<DependencyNode> parents) {
                                            final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
                                            return artifact != null && artifact.getGroupId().trim().equals(exclusionGroupId);
                                        }
                                    };
                                    for (Dependency transitive : getDependencyTransitives(dependency, dependencyFilter, dependenciesExcludedFromResolving, dependencyManagementWithoutExclusions)) {
                                        final Exclusion resolvedExclusion = new Exclusion();
                                        resolvedExclusion.setGroupId(transitive.getGroupId());
                                        resolvedExclusion.setArtifactId(transitive.getArtifactId());
                                        final String resolvedExclusionKey = resolvedExclusion.getGroupId()+":"+resolvedExclusion.getArtifactId();
                                        getLog().debug("Exclusion "+resolvedExclusionKey+" resolved for "+exclusionGroupId+":* exclusion on dependency "+dependency.getManagementKey());
                                        resolvedExclusions.putIfAbsent(resolvedExclusionKey, resolvedExclusion);
                                    }
                                } else {
                                    // no wildcard
                                    final String exclusionKey = exclusion.getGroupId()+":"+exclusion.getArtifactId();
                                    resolvedExclusions.putIfAbsent(exclusionKey, exclusion);
                                }
                            }
                        }
                        // remove managed artifacts from resolved exclusions
                        final List<Exclusion> filteredExclusions = new ArrayList<>();
                        for (Map.Entry<String, Exclusion> resolvedExclusionEntry : resolvedExclusions.entrySet()) {
                            final String resolvedExclusionKey = resolvedExclusionEntry.getKey();
                            if (managedExclusions.contains(resolvedExclusionKey) || isIgnoredExclusion(resolvedExclusionEntry.getValue())) {
                                getLog().debug("Removing exclusion "+resolvedExclusionKey+" from dependency "+dependency.getManagementKey());
                                continue;
                            }
                            getLog().debug("Keeping exclusion "+resolvedExclusionKey+" from dependency "+dependency.getManagementKey());
                            filteredExclusions.add(resolvedExclusionEntry.getValue());
                        }
                        // update the dependency exclusions with the filtered ones
                        dependency.setExclusions(filteredExclusions);
                    }
                    break;
            }
            addExclusions(dependency);
        }
        final List<Dependency> bomManagedDependencies = new ArrayList<>();
        final List<Dependency> bomDependencies = new ArrayList<>();
        if (includeDependencies != null || includeDependenciesTransitives != null) {
            // if includeDependencies is defined... filter the builder's dep management
            if (!includedManagedDependenciesWithTransitives.isEmpty()) {
                // need to resolve transitives
                final MavenProject clone = mavenProject.clone();
                clone.setDependencyArtifacts(null);
                clone.getDependencyManagement().setDependencies(new ArrayList<>());
                for (String orderedManagedDependencyKey : orderedManagedDependencies) {
                    clone.getDependencyManagement().getDependencies().add(managedDependenciesMap.get(orderedManagedDependencyKey));
                }
                clone.setDependencies(new ArrayList<>());
                for (String managementKey : includedManagedDependencies) {
                    final Dependency managedDependencyClone = managedDependenciesMap.get(managementKey).clone();
                    // replace any import scopes with compile
                    if ("import".equals(managedDependencyClone.getScope())) {
                        managedDependencyClone.setScope("compile");
                    }
                    if (!includedManagedDependenciesWithTransitives.contains(managementKey)) {
                        // add wildcard exclusion to prevent resolving transitives
                        Exclusion exclusion = new Exclusion();
                        exclusion.setGroupId(WILDCARD);
                        exclusion.setArtifactId(WILDCARD);
                        managedDependencyClone.getExclusions().add(exclusion);
                    } else {
                        // add just the deps excluded from the bom
                        managedDependencyClone.setExclusions(dependenciesExcludedFromResolving);
                    }
                    clone.getDependencies().add(managedDependencyClone);
                }
                try {
                    for (org.eclipse.aether.graph.Dependency aDependency : projectDependenciesResolver.resolve(new DefaultDependencyResolutionRequest(clone, repositorySystemSession)).getDependencies()) {
                        final Dependency resolvedDependency = new Dependency();
                        resolvedDependency.setGroupId(trim(aDependency.getArtifact().getGroupId()));
                        resolvedDependency.setArtifactId(trim(aDependency.getArtifact().getArtifactId()));
                        resolvedDependency.setType(trim(aDependency.getArtifact().getExtension()));
                        String resolvedClassifier = trim(aDependency.getArtifact().getClassifier());
                        if (resolvedClassifier != null && !resolvedClassifier.isEmpty()) {
                            resolvedDependency.setClassifier(resolvedClassifier);
                        }
                        resolvedDependency.setVersion(aDependency.getArtifact().getVersion());
                        final Dependency managedDependency = managedDependenciesMap.get(resolvedDependency.getManagementKey());
                        addBomManagedDependency(managedDependency, bomManagedDependencies);
                        if (bomWithDependencies) {
                            addBomDependency(managedDependency, bomDependencies);
                        }
                    }
                } catch (Throwable e) {
                    throw new MojoExecutionException(e.getMessage(),e);
                }
            } else {
                // no need to resolve transitives
                for (String managementKey : includedManagedDependencies) {
                    final Dependency managedDependency = managedDependenciesMap.get(managementKey);
                    addBomManagedDependency(managedDependency, bomManagedDependencies);
                    if (bomWithDependencies) {
                        addBomDependency(managedDependency, bomDependencies);
                    }
                }
            }
        } else {
            // if includeDependencies is not defined... add all managed deps to BOM
            for (String managedDependencyKey : orderedManagedDependencies) {
                final Dependency managedDependency = managedDependenciesMap.get(managedDependencyKey);
                addBomManagedDependency(managedDependency, bomManagedDependencies);
                if (bomWithDependencies) {
                    addBomDependency(managedDependency, bomDependencies);
                }
            }
        }
        pomModel.getDependencyManagement().setDependencies(bomManagedDependencies);
        getLog().info("Added " + pomModel.getDependencyManagement().getDependencies().size() + " managed dependencies to the BOM.");
        pomModel.setDependencies(bomDependencies);
        getLog().info("Added " + pomModel.getDependencies().size() + " dependencies to the BOM.");
    }

    private void addBuilderManagedDependency(Dependency dependency, List<String> orderedManagedDependencies, Map<String, Dependency> managedDependenciesMap, List<String> includedManagedDependencies, List<String> includedManagedDependenciesWithTransitives, Set<String> managedExclusions, ChannelSession channelSession) {
        dependency = dependency.clone();
        final String managementKey = dependency.getManagementKey();
        // if channels are configured then use them to resolve the dependency, to obtain any version update
        if (channelSession != null) {
            try {
                getLog().debug("Resolving dependency "+managementKey+"'s latest version on channels... (current = "+dependency.getVersion()+")");
                final String latestVersion = channelSession.findLatestMavenArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getClassifier(), dependency.getVersion());
                getLog().debug("Resolved dependency "+managementKey+"'s latest version on channels: "+latestVersion);
                dependency.setVersion(latestVersion);
            } catch (UnresolvedMavenArtifactException e) {
                getLog().debug("Failed to resolve dependency "+managementKey+" on channels", e);
            }
        }
        managedDependenciesMap.put(managementKey, resolveDependency(dependency));
        orderedManagedDependencies.add(managementKey);
        final IncludeDependency includedDependency = getIncludedDependency(dependency);
        if (includedDependency != null) {
            final boolean transitive = includedDependency.getTransitive() == null ? includeTransitives : includedDependency.getTransitive();
            includedManagedDependencies.add(managementKey);
            if (transitive) {
                includedManagedDependenciesWithTransitives.add(managementKey);
                getLog().debug("Dependency included (with transitives) by config: "+managementKey);
            } else {
                getLog().debug("Dependency included (without transitives) by config: "+managementKey);
            }
        }
        if (isImportedDependency(dependency)) {
            dependency.setScope("import");
            includedManagedDependencies.add(managementKey);
            getLog().debug("Dependency imported by config: "+managementKey);
        }
        if (getInheritExclusions(dependency) == InheritExclusions.UNMANAGED) {
            managedExclusions.add(dependency.getGroupId() + ":" + dependency.getArtifactId());
        }
    }

    private void addBomManagedDependency(Dependency managedDependency, List<Dependency> bomManagedDependencies) {
        if (managedDependency != null) {
            managedDependency = managedDependency.clone();
            bomManagedDependencies.add(managedDependency);
            getLog().info("Managed dependency "+managedDependency.getManagementKey()+":"+managedDependency.getVersion()+" added to the BOM.");
        }
    }

    private void addBomDependency(Dependency dependency, List<Dependency> bomDependencies) {
        if (dependency == null) {
            return;
        }
        if ("import".equals(dependency.getScope())) {
            // do not add imports
            return;
        }
        final Dependency bomDependency = dependency.clone();
        bomDependency.setExclusions(null);
        bomDependency.setVersion(null);
        if ("compile".equals(bomDependency.getScope())) {
            bomDependency.setScope(null);
        }
        bomDependencies.add(bomDependency);
        getLog().info("Dependency "+bomDependency.getManagementKey()+":"+bomDependency.getVersion()+" added to the BOM.");
    }

    private boolean isExcludedDependency(Dependency dependency) {
        final Dependency dependencyMatch = getDependencyMatch(dependency, excludeDependencies);
        if (dependencyMatch != null) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Managed dependency " + dependency.getManagementKey() + " matches dependency exclude " + dependencyMatch.getManagementKey());
            }
            return true;
        }
        return false;
    }

    private InheritExclusions getInheritExclusions(final Dependency dependency) {
        if (dependency instanceof IncludeDependency) {
            final InheritExclusions depInheritExclusion = ((IncludeDependency) dependency).getInheritExclusions();
            if (depInheritExclusion != null) {
                return depInheritExclusion;
            }
        }
        return inheritExclusions;
    }

    private IncludeDependency getIncludedTransitiveDependency(Dependency dependency) {
        return getDependencyMatch(dependency, includeDependenciesTransitives);
    }

    private IncludeDependency getIncludedDependency(Dependency dependency) {
        return getDependencyMatch(dependency, includeDependencies);
    }

    private Dependency resolveDependency(final Dependency dependency) {
        if (dependency instanceof IncludeDependency) {
            return dependency;
        }
        final IncludeDependency includeDependency = getIncludedDependency(dependency);
        if (includeDependency == null) {
            return dependency;
        }
        if (includeDependency.getClassifier() == null) {
            includeDependency.setClassifier(dependency.getClassifier());
        }
        if (includeDependency.getOptional() == null) {
            includeDependency.setOptional(dependency.getOptional());
        }
        if (includeDependency.getScope() == null) {
            includeDependency.setScope(dependency.getScope());
        }
        if (includeDependency.getSystemPath() == null) {
            includeDependency.setSystemPath(dependency.getSystemPath());
        }
        if (includeDependency.getType() == null) {
            includeDependency.setType(dependency.getType());
        }
        if (includeDependency.getVersion() == null) {
            includeDependency.setVersion(dependency.getVersion());
        }
        return includeDependency;
    }

    private Collection<? extends Dependency> getDependencyFirstLevelTransitives(final Dependency dependency, List<Exclusion> dependenciesExcludedFromResolving, List<Dependency> dependencyManagementWithoutExclusions) throws MojoExecutionException {
        final DependencyFilter dependencyFilter = new DependencyFilter() {
            @Override
            public boolean accept(DependencyNode node, List<DependencyNode> parents) {
                // only 1st level dependencies accepted
                return parents.size() == 2;
            }
        };
        return getDependencyTransitives(dependency, dependencyFilter, dependenciesExcludedFromResolving, dependencyManagementWithoutExclusions);
    }

    private Collection<? extends Dependency> getDependencyTransitives(final Dependency dependency, final DependencyFilter filter, List<Exclusion> dependenciesExcludedFromResolving, List<Dependency> dependencyManagementWithoutExclusions) throws MojoExecutionException {
        final MavenProject projectClone = mavenProject.clone();
        projectClone.setDependencyArtifacts(null);
        projectClone.getDependencyManagement().setDependencies(dependencyManagementWithoutExclusions);
        projectClone.setDependencies(new ArrayList<>());
        final Dependency managedDependencyClone = dependency.clone();
        managedDependencyClone.setExclusions(dependenciesExcludedFromResolving);
        // replace any import scopes with compile
        if ("import".equals(managedDependencyClone.getScope())) {
            managedDependencyClone.setScope("compile");
        }
        projectClone.getDependencies().add(managedDependencyClone);
        final List<Dependency> resolvedDependencies = new ArrayList<>();
        try {
            for (org.eclipse.aether.graph.Dependency aDependency : projectDependenciesResolver.resolve(new DefaultDependencyResolutionRequest(projectClone, repositorySystemSession).setResolutionFilter(filter)).getDependencies()) {
                final Dependency resolvedDependency = new Dependency();
                resolvedDependency.setGroupId(trim(aDependency.getArtifact().getGroupId()));
                resolvedDependency.setArtifactId(trim(aDependency.getArtifact().getArtifactId()));
                resolvedDependency.setType(trim(aDependency.getArtifact().getExtension()));
                String resolvedClassifier = trim(aDependency.getArtifact().getClassifier());
                if (resolvedClassifier != null && !resolvedClassifier.isEmpty()) {
                    resolvedDependency.setClassifier(resolvedClassifier);
                }
                resolvedDependency.setVersion(aDependency.getArtifact().getVersion());
                resolvedDependencies.add(resolvedDependency);
            }
        } catch (Throwable e) {
            throw new MojoExecutionException(e.getMessage(),e);
        }
        return resolvedDependencies;
    }

    private boolean isImportedDependency(Dependency dependency) {
        if (!"pom".equals(dependency.getType())) {
            return false;
        }
        final Dependency dependencyMatch = getDependencyMatch(dependency, importDependencies);
        if (dependencyMatch != null) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Managed dependency " + dependency.getManagementKey() + " matches dependency import " + dependencyMatch.getManagementKey());
            }
            return true;
        }
        return false;
    }

    private boolean isIgnoredExclusion(final Exclusion exclusion) {
        for (Exclusion ignoredExclusion : ignoredExclusions) {
            if (ignoredExclusion.getArtifactId().equals(exclusion.getArtifactId()) &&
                    ignoredExclusion.getGroupId().equals(exclusion.getGroupId())) {
                return true;
            }
        }
        return false;
    }

    private <T extends Dependency> T getDependencyMatch(Dependency dependency, Collection<? extends T> dependencyMatches) {
        if (dependencyMatches != null && !dependencyMatches.isEmpty()) {
            for (T dependencyMatch : dependencyMatches) {
                if (matchesDependency(dependency, dependencyMatch)) {
                    return dependencyMatch;
                }
            }
        }
        return null;
    }

    private boolean matchesDependency(Dependency dependency, Dependency match) {
        if (!"*".equals(match.getGroupId())) {
            String groupId = defaultString(trim(dependency.getGroupId()), "");
            if(!groupId.equals(match.getGroupId())) {
                return false;
            }
        }
        if (!"*".equals(match.getArtifactId())) {
            String artifactId = defaultString(trim(dependency.getArtifactId()), "");
            if (!artifactId.equals(match.getArtifactId())) {
                return false;
            }
        }
        if (!"*".equals(match.getType())) {
            String type = defaultString(trim(dependency.getType()), "jar");
            if(!type.equals(match.getType())) {
                return false;
            }
        }
        if (!"*".equals(match.getClassifier())) {
            String classifier = trim(dependency.getClassifier());
            if (classifier == null) {
                if (match.getClassifier() != null) {
                    return false;
                }
            } else {
                if (match.getClassifier() == null || !classifier.equals(match.getClassifier())) {
                    return false;
                }
            }
        }
        return true;
    }


    private void addExclusions(Dependency dependency) {
        if (addExclusions != null) {
            for (AddExclusion exclusion : addExclusions) {
                if (exclusion.getDependencyGroupId().equals(dependency.getGroupId()) &&
                        exclusion.getDependencyArtifactId().equals(dependency.getArtifactId())) {
                    Exclusion ex = new Exclusion();
                    ex.setGroupId(exclusion.getExclusionGroupId());
                    ex.setArtifactId(exclusion.getExclusionArtifactId());
                    dependency.addExclusion(ex);
                }
            }
        }
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
