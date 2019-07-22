package com.franklinharper.inactivitymonitor

/**
 * Force a compile-time check that the "when" covers all cases.
 * This is done by creating a dummy usage of the "when" as an expression.
 */
val <T> T.exhaustiveWhen: T
  get() = this