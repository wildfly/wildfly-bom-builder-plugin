# wildfly-bom-builder-plugin
Maven plugin that builds Wildfly BOMs

Inspired by https://github.com/jboss/bom-builder-maven-plugin this plugin inspects the available dependency management entries and outputs them to a BOM file.

Example usage:
```
    <build>
        <plugins>
            <plugin>
                <groupId>org.wildfly.plugins</groupId>
                <artifactId>wildfly-bom-builder-plugin</artifactId>
                <version>2.0.0.Beta1-SNAPSHOT</version>
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
                                <!--
                                    The version is optional in some cases. If a parent with the specified groupId and
                                    artifactId can be found in the consuming pom's parents, and the version is not
                                    specified, then the version of the consuming pom's parent will be used. If a parent
                                    is specified that is not in the consuming pom's parents, an error will be thrown.
                                -->
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
                            <inheritExclusions>ALL</inheritExclusions>
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
