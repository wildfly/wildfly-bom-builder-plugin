package org.wildfly.plugins.bombuilder;

/**
 * Describes the relationship for inheriting dependency exclusions.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum InheritExclusions {
    /**
     * All exclusions are inherited.
     */
    ALL,
    /**
     * No exclusions are inherited.
     */
    NONE,
    /**
     * Exclusions from unmanaged dependencies are inherited.
     */
    UNMANAGED
}
