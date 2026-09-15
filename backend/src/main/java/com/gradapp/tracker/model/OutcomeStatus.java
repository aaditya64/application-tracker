package com.gradapp.tracker.model;

/**
 * Distinguishes, within a single stage (e.g. OA_TEST), whether the applicant still has to act
 * (complete the OA) or has acted and is waiting to hear back. Only meaningful for stages that
 * represent a task-then-wait cycle: OA_TEST, HIREVUE, INTERVIEW, FINAL_STAGE.
 */
public enum OutcomeStatus {
    ACTION_NEEDED,
    AWAITING_OUTCOME
}
