package io.github.samzhu.topplecat.junit;

/** Whether a declared expected key was used by an executed ToppleCat case. */
public enum ExpectedConsumption {
  ASSERTED,
  READ,
  UNTOUCHED
}
