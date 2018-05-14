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

package org.wildfly.plugins.componentmatrix.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.TreeSet;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.wildfly.plugins.componentmatrix.DependencyId;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadPomModelUtil {

    public static void main(String[] args) throws Exception {
        //String fileName = args[0];
        String[] fileNames = {
            "/Users/kabir/temp/core-component-matrix.xml",
            "/Users/kabir/temp/full-component-matrix.xml"
        };

        TreeSet<DependencyId> sorted = new TreeSet<>();

        for (String fileName : fileNames) {
            File file = new File(fileName).getAbsoluteFile();

            try (Reader reader = new BufferedReader(new FileReader(file))) {
                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                Model model = mavenReader.read(reader);
                for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
                    if (dependency.getGroupId().equals("${project.groupId}")) {
                        dependency.setGroupId(model.getGroupId());
                    }
                    sorted.add(new DependencyId(dependency));
                }
            }
        }

        System.out.println("Comparison of " + Arrays.toString(fileNames));
        for (DependencyId dependencyId : sorted) {
            System.out.println(dependencyId);
        }

    }
}
