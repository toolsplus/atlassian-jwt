package io.toolsplus.atlassian.connect.jwt.scala

import io.toolsplus.atlassian.connect.jwt.scala.generators.core.JwtGen
import io.toolsplus.atlassian.connect.jwt.scala.generators.nimbus.NimbusGen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}

abstract class TestSpec
    extends WordSpec
    with MustMatchers
    with GeneratorDrivenPropertyChecks
    with JwtGen
    with NimbusGen
