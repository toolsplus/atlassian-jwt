package io.toolsplus.atlassian.connect.jwt.scala

import java.time.Instant

import io.circe.Json
import io.circe.parser._
import io.circe.optics.JsonPath._
import org.scalatest.Assertion
import java.time.Duration
import java.time.temporal.ChronoUnit
import org.scalacheck.Gen._

class JwtJsonBuilderSpec extends TestSpec {

  private val leewaySeconds = 30

  private val issuedAt = root.iat.long
  private val expiry = root.exp.long
  private val audience = root.aud.as[Seq[String]]
  private val issuer = root.iss.string
  private val jwtId = root.jti.string
  private val notBefore = root.nbf.long
  private val subject = root.sub.string
  private val `type` = root.typ.string
  private val queryStringHash = root.qsh.string
  private def customClaim(name: String) = root.selectDynamic(name).string

  "A JwtJsonBuilder" when {

    "used to build a JWT" should {

      "create a JWT claims with default lifetime if no arguments are given" in {
        val now = Instant.now
        val result = JwtJsonBuilder().build

        def assertion(json: Json) = {
          val defaultLifetime = JwtJsonBuilder.DEFAULT_JWT_LIFETIME.getSeconds
          val expectedExpiry = Instant.now plusSeconds defaultLifetime
          issuedAt.getOption(json).getOrElse(fail) mustBe now.getEpochSecond
          expiry
            .getOption(json)
            .getOrElse(fail) mustBe expectedExpiry.getEpochSecond +- leewaySeconds
        }

        validate(assertion)(result)
      }

      "create JWT claims with specific expiration time" in {
        val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
        val result = new JwtJsonBuilder(expireAfter).build

        def assertion(json: Json) = {
          val expectedExpiry = Instant.now plus expireAfter
          expiry
            .getOption(json)
            .getOrElse(fail) mustBe expectedExpiry.getEpochSecond +- 1
        }

        validate(assertion)(result)
      }

      "create JWT claims with overridden expiration time" in {
        val expireAfter = Duration.of(10, ChronoUnit.SECONDS)
        val expectedExpiry = Instant.now plus expireAfter
        val result =
          JwtJsonBuilder()
            .withExpirationTime(expectedExpiry.getEpochSecond)
            .build

        def assertion(json: Json) = {
          expiry
            .getOption(json)
            .getOrElse(fail) mustBe expectedExpiry.getEpochSecond +- 1
        }

        validate(assertion)(result)
      }

      "create JWT claims with specific issue time" in {
        val expectedIssueTime = Instant.now.getEpochSecond
        val result =
          JwtJsonBuilder()
            .withIssuedAt(expectedIssueTime)
            .build

        def assertion(json: Json) = {
          issuedAt
            .getOption(json)
            .getOrElse(fail) mustBe expectedIssueTime +- 1
        }

        validate(assertion)(result)
      }

      "create JWT claims with specific audience" in {
        forAll(listOf(alphaStr)) { expectedAudience =>
          val result = JwtJsonBuilder().withAudience(expectedAudience).build

          def assertion(json: Json) = {
            audience.getOption(json).getOrElse(fail) mustBe expectedAudience
          }

          validate(assertion)(result)
        }
      }

      "create JWT claims with specific issuer" in {
        forAll(alphaStr) { expectedIssuer =>
          val result = JwtJsonBuilder().withIssuer(expectedIssuer).build

          def assertion(json: Json) = {
            issuer.getOption(json).getOrElse(fail) mustBe expectedIssuer
          }

          validate(assertion)(result)
        }
      }

      "create JWT claims with specific jwt id" in {
        forAll(alphaStr) { expectedJwtId =>
          val result = JwtJsonBuilder().withJwtId(expectedJwtId).build

          def assertion(json: Json) = {
            jwtId.getOption(json).getOrElse(fail) mustBe expectedJwtId
          }

          validate(assertion)(result)
        }
      }

      "create JWT claims with specific not before timestamp" in {
        val expectedNotBefore = Instant.now minus Duration.of(
          10,
          ChronoUnit.SECONDS)
        val result = JwtJsonBuilder()
          .withNotBefore(expectedNotBefore.getEpochSecond)
          .build

        def assertion(json: Json) = {
          notBefore
            .getOption(json)
            .getOrElse(fail) mustBe expectedNotBefore.getEpochSecond +- 1
        }

        validate(assertion)(result)
      }

      "create JWT claims with specific subject" in {
        forAll(alphaStr) { expectedSubject =>
          val result = JwtJsonBuilder().withSubject(expectedSubject).build

          def assertion(json: Json) = {
            subject.getOption(json).getOrElse(fail) mustBe expectedSubject
          }

          validate(assertion)(result)
        }
      }

      "create JWT claims with specific type" in {
        forAll(alphaStr) { expectedType =>
          val result = JwtJsonBuilder().withType(expectedType).build

          def assertion(json: Json) = {
            `type`.getOption(json).getOrElse(fail) mustBe expectedType
          }

          validate(assertion)(result)
        }
      }

      "create JWT claims with specific query string hash" in {
        forAll(alphaStr) { expectedQsh =>
          val result = JwtJsonBuilder().withQueryHash(expectedQsh).build

          def assertion(json: Json) = {
            queryStringHash.getOption(json).getOrElse(fail) mustBe expectedQsh
          }

          validate(assertion)(result)
        }
      }

      "create JWT claims with specific custom claim" in {
        forAll(alphaStr, alphaStr) { (claimName, claimValue) =>
          val result = JwtJsonBuilder().withClaim(claimName, claimValue).build

          def assertion(json: Json) = {
            customClaim(claimName)
              .getOption(json)
              .getOrElse(fail) mustBe claimValue
          }

          validate(assertion)(result)
        }
      }

      "successfully check if a specific claim has been set" in {
        forAll(alphaStr, alphaStr) { (claimName, claimValue) =>
          val builder = JwtJsonBuilder().withClaim(claimName, claimValue)
          builder.containsClaim(claimName) mustBe true
        }
      }

    }

  }

  private def validate(assertion: Json => Assertion)(result: String) = {
    parse(result) match {
      case Right(json) => assertion(json)
      case Left(e) => fail(e.message, e)
    }
  }

}
