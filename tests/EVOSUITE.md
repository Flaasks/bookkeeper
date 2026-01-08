# EvoSuite Attempt (Jan 8, 2026)

## Summary
- Goal: Integrate EvoSuite to auto-generate additional tests for `bookkeeper-common` without breaking the build.
- Approach tried: Add a Maven profile using `org.evosuite.plugins:evosuite-maven-plugin` to generate tests, then include generated sources in the test phase.
- Result: Maven failed to resolve the plugin from configured repositories:
  - Error included: "No plugin found for prefix 'evosuite'" and missing artifact `org.evosuite.plugins:evosuite-maven-plugin:jar:1.2.0`.
- Outcome: Reverted the EvoSuite profile and any related config to keep the build stable. The repository remains unchanged by the attempt.

## Current State
- Unit and integration tests pass (`mvn -pl bookkeeper-common -B verify`).
- Failsafe integration tests for `BoundedScheduledExecutorService` are active and green.
- No EvoSuite-specific configuration is present in the POMs.

## CI Considerations
- Do not run EvoSuite generation in CI by default; it is slow and can be non-deterministic.
- If you want CI validation, commit the generated tests and treat them like normal tests.

## Notes
- Java 21: Validate EvoSuite version compatibility with your JDK.
- Revert strategy: If plugin resolution fails or generation proves flaky, remove the profile and re-run `mvn verify` to confirm build health.
