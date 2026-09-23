package com.atec.autoshot

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionOutcomeTest {

    @Test
    fun grantedIgnoresRationale() {
        assertEquals(PermissionOutcome.GRANTED, PermissionOutcome.of(granted = true, shouldShowRationale = false))
        assertEquals(PermissionOutcome.GRANTED, PermissionOutcome.of(granted = true, shouldShowRationale = true))
    }

    @Test
    fun deniedWithRationaleCanAskAgain() {
        assertEquals(PermissionOutcome.DENIED, PermissionOutcome.of(granted = false, shouldShowRationale = true))
    }

    @Test
    fun deniedWithoutRationaleIsPermanent() {
        assertEquals(
            PermissionOutcome.PERMANENTLY_DENIED,
            PermissionOutcome.of(granted = false, shouldShowRationale = false),
        )
    }
}
