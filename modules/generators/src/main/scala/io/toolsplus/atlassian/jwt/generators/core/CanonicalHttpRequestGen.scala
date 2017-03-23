package io.toolsplus.atlassian.jwt.generators.core

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait CanonicalHttpRequestGen {

  def httpMethodGen: Gen[String] = oneOf("GET", "POST", "PUT", "DELETE")

  def relativePathGen: Gen[String] =
    listOf(alphaNumStr).map(s => s"/${s.mkString("/")}")

  def parameterMapGen: Gen[Map[String, Seq[String]]] =
    mapOf(requestParameterGen)

  def canonicalHttpRequestGen: Gen[CanonicalHttpRequest] =
    for {
      aMethod <- httpMethodGen
      aRelativePath <- relativePathGen
      aParameterMap <- parameterMapGen
    } yield
      new CanonicalHttpRequest {
        override val method = aMethod
        override val relativePath = aRelativePath
        override val parameterMap = aParameterMap
      }

  private def requestParameterGen =
    for {
      key <- alphaStr
      value <- listOf(alphaNumStr)
    } yield (key, value)

}
