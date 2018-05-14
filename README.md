# wildfly-component-matrix-plugin
Generates the component matrix BOM

Inspired by https://github.com/jboss/bom-builder-maven-plugin this plugin inspects the available dependency management entries and outputs them to a BOM file.

Example usage:
```
    <build>
        <plugins>
            <plugin>
                <groupId>org.wildfly.plugins</groupId>
                <artifactId>wildfly-component-matrix-plugin</artifactId>
                <version>1.0.0.Alpha1-SNAPSHOT</version>
                <executions>
                    <execution>
                        <id>build-bom</id>
                        <goals>
                            <goal>build-bom</goal>
                        </goals>
                        <configuration>
                            <!-- Information about the parent to use. If not present no parent will be used -->
                            <parent>
                                <groupId>org.jboss</groupId>
                                <artifactId>jboss-parent</artifactId>
                                <version>26</version>
                                <relativePath/>
                            </parent>
                            <!-- The groupId of the generated bom, in this case we use the same groupId as the caller -->
                            <bomGroupId>${project.groupId}</bomGroupId>
                            <!-- The artifactId of the generated bom -->
                            <bomArtifactId>wildfly-core-component-matrix</bomArtifactId>
                            <!-- The version of the generated bom, in this case we use the same version as the caller -->
                            <bomVersion>${project.version}</bomVersion>
                            <!-- The maven name of the bom -->
                            <bomName>WildFly Core: Component Matrix</bomName>
                            <!-- The maven description of the bom -->
                            <bomDescription>WildFly Core: Component Matrix</bomDescription>
                            <!-- Whether to inherit the exclusions for each dependency management entry -->
                            <inheritExclusions>true</inheritExclusions>
                            <!-- Whether to copy the licenses from the caller into the generated bom -->
                            <licenses>true</licenses>
                            <!-- A list of profiles to include in the generated bom -->
                            <includeProfiles>
                                <profile>JDK9</profile>
                            </includeProfiles>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```    

In addition it supports the `exlusions` and `dependencyExclusions` documented in https://github.com/jboss/bom-builder-maven-plugin.
