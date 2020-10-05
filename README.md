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
                <version>2.0.0.Final-SNAPSHOT</version>
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
                                <relativePath/>
                            </parent>
                            <!-- The groupId of the generated bom -->
                            <bomGroupId>org.wildfly</bomGroupId>
                            <!-- The artifactId of the generated bom -->
                            <bomArtifactId>wildfly-jaxws-client-bom</bomArtifactId>
                            <!-- The version of the generated bom, in this case we use the same version as the caller -->
                            <bomVersion>${project.version}</bomVersion>
                            <!-- The maven project name of the bom -->
                            <bomName>WildFly BOMs: JAXWS Client</bomName>
                            <!-- The maven project description of the bom -->
                            <bomDescription>This artifact provides a bill of materials (BOM) for JAXWS client usage.</bomDescription>
                            <!-- aftifacts in the bom's dependency management are also added in its dependencies, so users may just dependend on the bom to dependend on all artifacts -->
                            <bomWithDependencies>true</bomWithDependencies>
                            <!-- The builder's maven project licenses are added to the bom -->
                            <licenses>true</licenses>
                            <!-- All exclusions in the builder managed dependencies are inherited by the bom, other options are NONE and UNMANAGED -->
                            <inheritExclusions>ALL</inheritExclusions>
                            <!-- A list of profiles to include in the generated bom -->
                            <includeProfiles>
                                <profile>JDK9</profile>
                            </includeProfiles>
                            <!-- IDs from maven repositories from builder, to add to the bom -->
                            <includeRepositories>
                                <id>jboss-public-repository-group</id>
                                <id>jboss-enterprise-maven-repository</id>
                            </includeRepositories>
                            <!-- Managed dependencies to exclude and not add to the bom -->
                            <excludeDependencies>
                                <dependency>
                                    <groupId>log4j</groupId>
                                    <artifactId>log4j</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.slf4j</groupId>
                                    <artifactId>jcl-over-slf4j</artifactId>
                                </dependency>
                            </excludeDependencies>
                            <!-- Managed dependencies to include and add to the bom -->
                            <includeDependencies>
                                <dependency>
                                    <groupId>org.jboss.ws.cxf</groupId>
                                    <artifactId>jbossws-cxf-client</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.jboss.spec.javax.annotation</groupId>
                                    <artifactId>jboss-annotations-api_1.3_spec</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.jboss.slf4j</groupId>
                                    <artifactId>slf4j-jboss-logmanager</artifactId>
                                </dependency>
                                <dependency>
                                    <groupId>org.jboss.logmanager</groupId>
                                    <artifactId>jboss-logmanager</artifactId>
                                </dependency>
                            </includeDependencies>
                            <!-- Unmanaged dependencies to add to the BOM, with a version obtained from an existing managed dependency -->
                            <versionRefDependencies>
                                <dependency>
                                    <groupId>org.hibernate.validator</groupId>
                                    <artifactId>hibernate-validator-annotation-processor</artifactId>
                                    <version>org.hibernate.validator:hibernate-validator:jar</version>
                                </dependency>
                                <dependency>
                                    <groupId>org.hibernate</groupId>
                                    <artifactId>hibernate-jpamodelgen</artifactId>
                                    <version>org.hibernate:hibernate-core:jar</version>
                                </dependency>
                            </versionRefDependencies>
                            <!-- Extra exclusions to add to specific dependencies in the bom -->
                            <addExclusions>
                                <exclusion>
                                    <dependencyGroupId>org.hibernate</dependencyGroupId>
                                    <dependencyArtifactId>hibernate-jpamodelgen</dependencyArtifactId>
                                    <exclusionGroupId>javax.xml.bind</exclusionGroupId>
                                    <exclusionArtifactId>jaxb-api</exclusionArtifactId>
                                </exclusion>
                            </addExclusions>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```    
