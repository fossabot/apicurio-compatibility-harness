package io.apicurio.registry.compatibility.model;

/**
 * Possible outcomes when comparing Apicurio's response against Confluent's.
 */
public enum CompatibilityTestResult {
    PASS,
    FAIL,
    SKIPPED_GAP,
    KNOWN_INCOMPATIBILITY,
    ERROR
}
