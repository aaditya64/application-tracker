package com.gradapp.tracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageSnapshotServiceTest {

    private static final String REAL_POSTING = """
            Graduate Software Engineer

            We are looking for a talented graduate to join our engineering team and help us build
            reliable, scalable systems used by millions of people every day. You'll work alongside
            experienced engineers on real production code from day one, with mentorship and support
            to help you grow quickly.

            Requirements: a degree in Computer Science or a related field, proficiency in at least
            one modern programming language, and a genuine interest in software engineering.
            """;

    @Test
    void doesNotFlagARealJobPosting() {
        assertThat(PageSnapshotService.looksFaulty(REAL_POSTING)).isFalse();
    }

    @Test
    void flagsARobotCheckPage() {
        assertThat(PageSnapshotService.looksFaulty("Quick Check Needed\nI'm not a robot\nContinue")).isTrue();
    }

    @Test
    void flagsACloudflareStyleInterstitial() {
        assertThat(PageSnapshotService.looksFaulty("Just a moment...\nPlease wait while we verify your browser.")).isTrue();
    }

    @Test
    void flagsSuspiciouslyThinContent() {
        assertThat(PageSnapshotService.looksFaulty("Loading...")).isTrue();
    }

    @Test
    void flagsEmptyOrNullText() {
        assertThat(PageSnapshotService.looksFaulty("")).isTrue();
        assertThat(PageSnapshotService.looksFaulty(null)).isTrue();
    }

    @Test
    void matchingIsCaseInsensitive() {
        assertThat(PageSnapshotService.looksFaulty(REAL_POSTING + "\nVERIFY YOU ARE HUMAN")).isTrue();
    }
}
