package io.toolsplus.atlassian.connect.jwt.scala

import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt.JWTClaimsSet

/**
  * JWT representation used to hold information extracted from parsed JWT.
  */
case class Jwt(jwsObject: JWSObject, claims: JWTClaimsSet) {

  def iss: String = claims.getIssuer

  def sub: String = claims.getSubject

  /**
    * String representation of JWT JSON payload. Can be used to parse to JSON
    * type in different libraries or decoded to an instance of a case class.
    * @return Stringified JWT JSON payload
    */
  def json: String = jwsObject.getPayload.toString
}
