package io.apicurio.registry.compatibility.collector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.apicurio.registry.compatibility.model.CompatibilityTestResult;
import io.apicurio.registry.compatibility.model.TestOutcome;

public class TestResultCollector {

    private static final TestResultCollector INSTANCE = new TestResultCollector();

    private final List<TestOutcome> outcomes = new CopyOnWriteArrayList<>();

    private TestResultCollector() {
    }

    public static TestResultCollector getInstance() {
        return INSTANCE;
    }

    public void record(TestOutcome outcome) {
        outcomes.add(outcome);
    }

    public List<TestOutcome> getOutcomes() {
        return Collections.unmodifiableList(new ArrayList<>(outcomes));
    }

    public long getPassCount() {
        return count(CompatibilityTestResult.PASS);
    }

    public long getFailCount() {
        return count(CompatibilityTestResult.FAIL);
    }

    public long getSkippedGapCount() {
        return count(CompatibilityTestResult.SKIPPED_GAP);
    }

    public long getKnownIncompatibilityCount() {
        return count(CompatibilityTestResult.KNOWN_INCOMPATIBILITY);
    }

    public long getErrorCount() {
        return count(CompatibilityTestResult.ERROR);
    }

    public int getTotalCount() {
        return outcomes.size();
    }

    private long count(CompatibilityTestResult result) {
        return outcomes.stream().filter(o -> o.getResult() == result).count();
    }
}
