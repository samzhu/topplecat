package io.github.samzhu.topplecat.core;

/** Final machine verdict for one delegation verification run. */
public enum EvidenceVerdict {
    PASS,
    FAIL,
    INCOMPLETE,
    /** The reviewer explicitly disabled this adversarial safeguard for the run. */
    DISABLED
}
