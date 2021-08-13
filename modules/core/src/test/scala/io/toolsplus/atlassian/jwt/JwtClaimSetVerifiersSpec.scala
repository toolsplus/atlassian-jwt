package io.toolsplus.atlassian.jwt

import com.nimbusds.jwt.JWTClaimsSet
import org.scalatest.EitherValues

import java.time.ZonedDateTime
import java.util.Date

class JwtClaimSetVerifiersSpec extends TestSpec with EitherValues {

  "Using a JwtClaimSetVerifiers" when {

    "hasIssueTimeAndExpirationTime" should {

      "succeed if claims have issue time (iat) and expiry (exp)" in forAll(
        jwtClaimsSetGen()) { claims =>
        JwtClaimSetVerifiers
          .hasIssueTimeAndExpirationTime(claims)
          .right
          .value mustBe a[JWTClaimsSet]
      }

      "fail if issue time (iat) is missing" in forAll(
        jwtClaimsSetGen(Seq("iat" -> null))) { claims =>
        JwtClaimSetVerifiers
          .hasIssueTimeAndExpirationTime(claims)
          .left
          .value mustBe a[JwtInvalidClaimError]
      }

      "fail if issue time (exp) is missing" in forAll(
        jwtClaimsSetGen(Seq("exp" -> null))) { claims =>
        JwtClaimSetVerifiers
          .hasIssueTimeAndExpirationTime(claims)
          .left
          .value mustBe a[JwtInvalidClaimError]
      }
    }

    "expirationTimeIsAfterNotBefore" should {

      val now = ZonedDateTime.now
      val nowDate = Date.from(now.toInstant)
      val expDate = Date.from(now.plusMinutes(5).toInstant)

      "succeed if expiry (exp) is after not before (nbf) time" in forAll(
        jwtClaimsSetGen(Seq("nbf" -> nowDate, "exp" -> expDate))) { claims =>
        JwtClaimSetVerifiers
          .expirationTimeIsAfterNotBefore(claims)
          .right
          .value mustBe a[JWTClaimsSet]
      }

      "succeed if not before (nbf) is missing" in forAll(
        jwtClaimsSetGen(Seq("nbf" -> null))) { claims =>
        JwtClaimSetVerifiers
          .expirationTimeIsAfterNotBefore(claims)
          .right
          .value mustBe a[JWTClaimsSet]
      }

      "fail if expiry (exp) is missing" in forAll(
        jwtClaimsSetGen(Seq("exp" -> null, "nbf" -> nowDate))) { claims =>
        JwtClaimSetVerifiers
          .expirationTimeIsAfterNotBefore(claims)
          .left
          .value mustBe a[JwtInvalidClaimError]
      }

      "fail if expiry (exp) is earlier than not before (nbf) time" in forAll(
        jwtClaimsSetGen(Seq("exp" -> nowDate, "nbf" -> expDate))) { claims =>
        JwtClaimSetVerifiers
          .expirationTimeIsAfterNotBefore(claims)
          .left
          .value mustBe a[JwtInvalidClaimError]
      }
    }

    "nowIsAfterNotBefore" should {

      val now = ZonedDateTime.now
      val fiveMinutesAgo = Date.from(now.minusMinutes(5).toInstant)
      val inFiveMinutes = Date.from(now.plusMinutes(5).toInstant)

      val validator =
        JwtClaimSetVerifiers.nowIsAfterNotBefore(now.toInstant, 30)

      "succeed if current time is after not before (nbf) time" in forAll(
        jwtClaimsSetGen(Seq("nbf" -> fiveMinutesAgo))) { claims =>
        validator(claims).right.value mustBe a[JWTClaimsSet]
      }

      "succeed if not before (nbf) is missing" in forAll(
        jwtClaimsSetGen(Seq("nbf" -> null))) { claims =>
        validator(claims).right.value mustBe a[JWTClaimsSet]
      }

      "fail if expiry (exp) is earlier than not before (nbf) time" in forAll(
        jwtClaimsSetGen(Seq("nbf" -> inFiveMinutes))) { claims =>
        validator(claims).left.value mustBe a[JwtTooEarlyError]
      }
    }

    "nowIsBeforeExpirationTime" should {

      val now = ZonedDateTime.now
      val fiveMinutesAgo = Date.from(now.minusMinutes(5).toInstant)
      val inFiveMinutes = Date.from(now.plusMinutes(5).toInstant)

      val validator =
        JwtClaimSetVerifiers.nowIsBeforeExpirationTime(now.toInstant, 30)

      "succeed if current time is before expiry (exp) time" in forAll(
        jwtClaimsSetGen(Seq("exp" -> inFiveMinutes))) { claims =>
        validator(claims).right.value mustBe a[JWTClaimsSet]
      }

      "fail if expiry (exp) time is missing" in forAll(
        jwtClaimsSetGen(Seq("exp" -> null))) { claims =>
        validator(claims).left.value mustBe a[JwtInvalidClaimError]
      }

      "fail if current time is after expiry (exp) time" in forAll(
        jwtClaimsSetGen(Seq("exp" -> fiveMinutesAgo))) { claims =>
        validator(claims).left.value mustBe a[JwtExpiredError]
      }
    }

    "queryStringHash" should {

      val qsh = "test-query-string-hash"

      val validator =
        JwtClaimSetVerifiers.queryStringHash(qsh)

      "succeed if qsh claim matches expected qsh" in forAll(
        jwtClaimsSetGen(Seq("qsh" -> qsh))) { claims =>
        validator(claims).right.value mustBe a[JWTClaimsSet]
      }

      "succeed if qsh claim is missing" in forAll(
        jwtClaimsSetGen(Seq("qsh" -> null))) { claims =>
        validator(claims).right.value mustBe a[JWTClaimsSet]
      }

      "fail if qsh claim does not match expected qsh" in forAll(
        jwtClaimsSetGen(Seq("qsh" -> "non-matching-qsh"))) { claims =>
        validator(claims).left.value mustBe a[JwtInvalidClaimError]
      }

    }

  }

}
