package io.apicurio.registry.compatibility.model;

/**
 * Records the outcome of a single compatibility test comparison.
 */
public class TestOutcome {

    private final String testName;
    private final String endpoint;
    private final String method;
    private final CompatibilityTestResult result;
    private final String confluentStatus;
    private final String apicurioStatus;
    private final String details;

    private TestOutcome(Builder builder) {
        this.testName = builder.testName;
        this.endpoint = builder.endpoint;
        this.method = builder.method;
        this.result = builder.result;
        this.confluentStatus = builder.confluentStatus;
        this.apicurioStatus = builder.apicurioStatus;
        this.details = builder.details;
    }

    public String getTestName() { return testName; }
    public String getEndpoint() { return endpoint; }
    public String getMethod() { return method; }
    public CompatibilityTestResult getResult() { return result; }
    public String getConfluentStatus() { return confluentStatus; }
    public String getApicurioStatus() { return apicurioStatus; }
    public String getDetails() { return details; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String testName;
        private String endpoint;
        private String method;
        private CompatibilityTestResult result;
        private String confluentStatus;
        private String apicurioStatus;
        private String details;

        public Builder testName(String testName) { this.testName = testName; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder result(CompatibilityTestResult result) { this.result = result; return this; }
        public Builder confluentStatus(String status) { this.confluentStatus = status; return this; }
        public Builder apicurioStatus(String status) { this.apicurioStatus = status; return this; }
        public Builder details(String details) { this.details = details; return this; }

        public TestOutcome build() { return new TestOutcome(this); }
    }
}
