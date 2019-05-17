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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * class is not thread safe
 */
class OrderedProperties extends Properties {
    private final LinkedHashSet<Object> orderedKeys = new LinkedHashSet<>();

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(orderedKeys);
    }

    @Override
    public synchronized void putAll(Map<?, ?> t) {
        // enforce different JDK impls to delegate to put
        for (Map.Entry<?, ?> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        // workaround LinkedHashSet reinserts not changing order
        orderedKeys.remove(key);
        orderedKeys.add(key);
        return super.put(key, value);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        orderedKeys.remove(key);
        return super.remove(key, value);
    }
}
