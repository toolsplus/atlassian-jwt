package io.toolsplus.atlassian.jwt

import com.nimbusds.jose._

import scala.util.{Failure, Success, Try}

/** JWT Writer to write valid Atlassian compatible JWTs.
  *
  * Each writer has to be configured with the [[JWSAlgorithm]] and [[JWSSigner]]
  * that will be used sign the token.
  */
case class JwtWriter(algorithm: JWSAlgorithm, signer: JWSSigner) {

  def jsonToJwt(json: String): Either[JwtSigningError, String] =
    generateJwsObject(json).map(_.serialize)

  def generateJwsObject(payload: String): Either[JwtSigningError, JWSObject] = {
    val header = new JWSHeader.Builder(algorithm)
      .`type`(new JOSEObjectType(JwtWriter.JWT))
      .build

    val jwsObject = new JWSObject(header, new Payload(payload))
    Try(jwsObject.sign(signer)) match {
      case Success(_)         => Right(jwsObject)
      case Failure(exception) =>
        Left(JwtSigningError(exception.getMessage, exception))
    }
  }

}

object JwtWriter {

  private val JWT: String = "JWT"

}
