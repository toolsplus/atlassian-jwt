package io.toolsplus.atlassian.connect.jwt.scala.util

import org.scalacheck.Gen
import org.scalacheck.Gen._

trait CommonGen {

  def alphaStr(n: Int): Gen[String] = listOf(n, alphaChar).map(_.mkString)

}

object CommonGen extends CommonGen
