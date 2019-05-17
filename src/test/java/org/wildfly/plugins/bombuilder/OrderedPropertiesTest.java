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

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

public class OrderedPropertiesTest {

    @Test
    public void testOrderedPropertiesAreSortedInOrderOfAdding() throws Exception {
        OrderedProperties properties = new OrderedProperties();
        properties.put("project.build.sourceEncoding", "utf-8");
        properties.put("version.org.codehaus.plexus", "1.2.3");
        assertEquals("project.build.sourceEncoding", properties.keySet().iterator().next());
    }

    @Test
    public void testPutAll() throws Exception {
        Properties properties = new Properties();
        properties.put("project.build.sourceEncoding", "utf-8");
        properties.put("version.org.codehaus.plexus", "1.2.3");
        OrderedProperties orderedProperties = new OrderedProperties();
        orderedProperties.putAll(properties);
        assertEquals(properties.size(), orderedProperties.size());
        assertEquals(properties.keySet().size(), orderedProperties.keySet().size());
    }
}