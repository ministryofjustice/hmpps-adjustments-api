package uk.gov.justice.digital.hmpps.adjustments.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "feature-toggles")
data class FeatureToggles(
  var displayPotentialAdas: Boolean = false,
  var checkForPreviousPeriodsOfUal: Boolean = false,
)
