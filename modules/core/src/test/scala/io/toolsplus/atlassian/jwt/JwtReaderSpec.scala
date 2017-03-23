package io.toolsplus.atlassian.jwt

import java.time.{Duration, Instant}
import java.util.Date

import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import org.scalacheck.{Gen, Shrink}

class JwtReaderSpec extends TestSpec {

  private val signingSecret: String = "EVqrJGaYbf=EGVwg)aYAxXJqm2zjfab6"

  "Using a JwtReader" when {

    "given a valid JWT string" should {

      "successfully read and verify a JWT" in forAll(canonicalHttpRequestGen) {
        request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          forAll(signedJwtStringGen(signingSecret, Seq("qsh" -> queryHash))) {
            token =>
              JwtReader(signingSecret).readAndVerify(token, queryHash) match {
                case Right(jwt) => jwt mustBe a[Jwt]
                case Left(e) => fail(e)
              }
          }
      }

      "successfully read and verify a JWT even if qsh is not present (self-authenticated)" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        forAll(signedJwtStringGen(signingSecret)) { token =>
          JwtReader(signingSecret).readAndVerify(token, queryHash) match {
            case Right(jwt) => jwt mustBe a[Jwt]
            case Left(e) => fail(e)
          }
        }
      }

      "fail if not before claim ('nbf') is after expiry time" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val notBefore = Instant.now plus Duration.ofMinutes(45)
          val customClaims = Seq("nbf" -> Date.from(notBefore))
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedJwtStringGen(signingSecret, customClaims)) { token =>
            JwtReader(signingSecret).readAndVerify(token, queryHash) match {
              case Left(e) => e mustBe a[JwtInvalidClaimError]
              case Right(jwt) =>
                fail(
                  s"Expected validation for JWT ($jwt) with 'nbf' claim to fail")
            }
          }
        }
      }

      "fail if not before claim ('nbf') is after current time" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val notBefore = Instant.now plus Duration.ofMinutes(3)
          val customClaims = Seq("nbf" -> Date.from(notBefore))
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedJwtStringGen(signingSecret, customClaims)) { token =>
            JwtReader(signingSecret).readAndVerify(token, queryHash) match {
              case Left(e) => e mustBe a[JwtTooEarlyError]
              case Right(jwt) =>
                fail(
                  s"Expected validation for JWT ($jwt) with 'nbf' claim to fail")
            }
          }
        }
      }

      "fail if expiry ('exp') is before current time" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val expiry = Instant.now minus Duration.ofMinutes(3)
          val customClaims = Seq("exp" -> Date.from(expiry))
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedJwtStringGen(signingSecret, customClaims)) { token =>
            JwtReader(signingSecret).readAndVerify(token, queryHash) match {
              case Left(e) => e mustBe a[JwtExpiredError]
              case Right(jwt) =>
                fail(s"Expected validation for expired JWT ($jwt) to fail")
            }
          }
        }
      }

      "fail if there is no issue time ('iat')" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val customClaims = Seq("iat" -> null)
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedJwtStringGen(signingSecret, customClaims)) { token =>
            JwtReader(signingSecret).readAndVerify(token, queryHash) match {
              case Left(e) => e mustBe a[JwtInvalidClaimError]
              case Right(jwt) =>
                fail(
                  s"Expected validation for JWT ($jwt) without issue time to fail")
            }
          }
        }
      }

      "fail if there is no expiration time ('exp')" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val customClaims = Seq("exp" -> null)
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedJwtStringGen(signingSecret, customClaims)) { token =>
            JwtReader(signingSecret).readAndVerify(token, queryHash) match {
              case Left(e) => e mustBe a[JwtInvalidClaimError]
              case Right(jwt) =>
                fail(
                  s"Expected validation for JWT ($jwt) without expiration time to fail")
            }
          }
        }
      }

    }

    "given an invalid JWT string" should {

      "fail to read a JWT with query string hash mismatch" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        forAll(signedJwtStringGen(signingSecret, Seq("qsh" -> queryHash)),
               Gen.alphaNumStr) { (token, invalidQsh) =>
          JwtReader(signingSecret).readAndVerify(token, invalidQsh) match {
            case Left(failure) => failure mustBe a[JwtVerificationError]
            case Right(_) => fail
          }
        }
      }

      "fail to read a JWT from an unsigned token string" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        forAll(unsignedJwtStringGen(Seq("qsh" -> queryHash))) { token =>
          JwtReader(signingSecret).readAndVerify(token, queryHash) match {
            case Left(failure) => failure mustBe a[ParsingFailure]
            case Right(_) => fail
          }
        }
      }

      "fail to read a JWT from a token string with invalid signature" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        val customClaims = Seq("qsh" -> queryHash)
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedJwtStringGen(signingSecret, customClaims)) { token =>
          JwtReader(signingSecret)
            .readAndVerify(token.dropRight(5), queryHash) match {
            case Left(failure) => failure mustBe a[JwtSignatureMismatchError]
            case Right(_) => fail
          }
        }
      }

    }

  }

}
