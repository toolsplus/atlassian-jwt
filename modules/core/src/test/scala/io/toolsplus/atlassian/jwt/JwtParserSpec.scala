package io.toolsplus.atlassian.jwt

import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jwt.JWTClaimsSet
import org.scalacheck.Gen._

import scala.jdk.CollectionConverters._

class JwtParserSpec extends TestSpec {

  "Using a JwtParser" when {

    "given a valid JWT string" should {

      "successfully parse a JWT" in forAll(signedSymmetricJwtStringGen()) { token =>
        JwtParser.parse(token) match {
          case Right(jwt) => jwt mustBe a[Jwt]
          case Left(_) => fail()
        }
      }

      "successfully parse a JWSObject" in forAll(signedSymmetricJwtStringGen()) {
        token =>
          JwtParser.parseJWSObject(token) match {
            case Right(jwsObject) =>
              jwsObject mustBe a[JWSObject]
              jwsObject.serialize() mustBe token
            case Left(_) => fail()
          }
      }

      "successfully parse JWTClaimsSet" in forAll(jwtClaimsSetGen()) {
        claims =>
          JwtParser.parseJWTClaimsSet(claims.toJSONObject) match {
            case Right(parsedClaims) =>
              parsedClaims mustBe a[JWTClaimsSet]
              JSONObjectUtils.toJSONString(parsedClaims.toJSONObject) mustBe JSONObjectUtils.toJSONString(claims.toJSONObject)
            case Left(_) => fail()
          }
      }

    }

    "given an invalid JWT string" should {

      "fail to parse a JWT from an unsigned token string" in forAll(
        unsignedJwtStringGen()) { token =>
        JwtParser.parse(token) match {
          case Left(failure) => failure mustBe a[ParsingFailure]
          case Right(jwt) => fail(s"Expected parsing of JWT ($jwt) to fail")
        }
      }

      "fail to parse a JWSObject from an unsigned token string" in forAll(
        unsignedJwtStringGen()) { token =>
        JwtParser.parseJWSObject(token) match {
          case Left(failure) => failure mustBe a[ParsingFailure]
          case Right(jws) =>
            fail(s"Expected parsing of JWS object ($jws) to fail")
        }
      }

      "fail to parse a JWTClaimSet from a random string" in forAll(alphaStr) {
        claimValue =>
          val invalidClaims = Map[String, Object]("exp" -> claimValue).asJava
          JwtParser.parseJWTClaimsSet(invalidClaims) match {
            case Left(failure) => failure mustBe a[ParsingFailure]
            case Right(c) =>
              fail(s"Expected parsing of claims (${c.toString}) to fail")
          }
      }

    }

  }

}
