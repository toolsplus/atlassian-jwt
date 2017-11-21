package io.toolsplus.atlassian.jwt

import cats.syntax.either._
import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt._
import net.minidev.json.JSONObject

import scala.util.{Failure, Success, Try}

object JwtParser {

  final def parse(input: String): Either[ParsingFailure, Jwt] =
    for {
      jwsObject <- parseJWSObject(input)
      claims <- parseJWTClaimsSet(jwsObject.getPayload.toJSONObject)
    } yield Jwt(jwsObject, claims)

  final def parseJWSObject(input: String): Either[ParsingFailure, JWSObject] = {
    Try(JWSObject.parse(input)) match {
      case Success(jwsObject) => Right(jwsObject)
      case Failure(exception) =>
        Left(ParsingFailure(exception.getMessage, exception))
    }
  }

  final def parseJWTClaimsSet(
      json: JSONObject): Either[ParsingFailure, JWTClaimsSet] = {
    Try(JWTClaimsSet.parse(json)) match {
      case Success(claims) => Right(claims)
      case Failure(exception) =>
        Left(ParsingFailure(claimParsingFailureDetails(exception), exception))
    }
  }

  final def claimParsingFailureDetails(exception: Throwable): String = {
    def toClaimName(message: String): String = message
      .replace(JwtParser.UnexpectedTypeMessagePrefix, "")
      .replaceAll("\"", "")

    def failureDetails(claimName: String): String = {
      if (JwtParser.NumericClaimNames.contains(claimName))
        s"Expecting claim '$claimName' to be numeric but it is a string"
      else
        s"Perhaps a claim is of the wrong type (e.g. expecting integer but found string): ${exception.getMessage}"
    }

    Option(exception.getMessage)
      .filter(_.startsWith(JwtParser.UnexpectedTypeMessagePrefix))
      .map(message => failureDetails(toClaimName(message)))
      .getOrElse("Parsing claims failed")
  }

  private val UnexpectedTypeMessagePrefix: String =
    "Unexpected type of JSON object member with key "

  private val NumericClaimNames: Set[String] = Set("exp", "iat", "nbf")

}
