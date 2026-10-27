package app.possessory.services

import app.possessory.PossessoryApp
import kotlinx.coroutines.flow.MutableStateFlow

/** Standalone builds have no store SDK and cannot grant or simulate purchases. */
class AmazonBilling(@Suppress("UNUSED_PARAMETER") app: PossessoryApp) {
  val enabled = false
  val state = MutableStateFlow(BillingState())

  fun start() = Unit

  fun purchase() = Unit

  fun restore() = Unit
}
