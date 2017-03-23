package io.toolsplus.atlassian.jwt

import io.toolsplus.atlassian.jwt.generators.core.JwtGen
import io.toolsplus.atlassian.jwt.generators.nimbus.NimbusGen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}

abstract class TestSpec
    extends WordSpec
    with MustMatchers
    with GeneratorDrivenPropertyChecks
    with JwtGen
    with NimbusGen
