package io.blume.update;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionCompareTest {

    @Test
    void stableNewerThanBeta() {
        assertThat(VersionCompare.isOlder("0.5.0-beta.1", "0.5.0")).isTrue();
        assertThat(VersionCompare.isOlder("0.5.0", "0.5.0-beta.1")).isFalse();
    }

    @Test
    void betaOrdering() {
        assertThat(VersionCompare.isOlder("0.5.0-beta.1", "0.5.0-beta.2")).isTrue();
        assertThat(VersionCompare.isOlder("0.5.0-beta.2", "0.5.0-beta.1")).isFalse();
    }

    @Test
    void majorMinorPatchOrdering() {
        assertThat(VersionCompare.isOlder("0.4.7", "0.5.0")).isTrue();
        assertThat(VersionCompare.isOlder("0.5.0", "0.4.7")).isFalse();
        assertThat(VersionCompare.isOlder("0.5.0", "0.5.1")).isTrue();
    }

    @Test
    void betaAgainstOlderStable() {
        assertThat(VersionCompare.isOlder("0.4.7", "0.5.0-beta.1")).isTrue();
    }

    @Test
    void dirtyBuildIsOlderThanRelease() {
        assertThat(VersionCompare.isOlder("0.5.0-2-gabc", "0.5.0")).isTrue();
    }

    @Test
    void stripPrefix() {
        assertThat(VersionCompare.stripPrefix("v0.5.0")).isEqualTo("0.5.0");
        assertThat(VersionCompare.stripPrefix("0.5.0")).isEqualTo("0.5.0");
    }

    @Test
    void isBeta() {
        assertThat(VersionCompare.isBeta("0.5.0-beta.1")).isTrue();
        assertThat(VersionCompare.isBeta("v0.5.0-beta.1")).isTrue();
        assertThat(VersionCompare.isBeta("0.5.0-beta.1-2-gabc")).isTrue();
        assertThat(VersionCompare.isBeta("0.5.0")).isFalse();
    }
}
