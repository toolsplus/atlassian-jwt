package io.toolsplus.atlassian.jwt.symmetric

import com.nimbusds.jose.JWSAlgorithm
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt._
import org.scalacheck.{Gen, Shrink}

import java.time.{Duration, Instant}
import java.util.Date

class SymmetricJwtReaderSpec extends TestSpec {

  private val signingSecret: String = "EVqrJGaYbf=EGVwg)aYAxXJqm2zjfab6"

  "Using a SymmetricJwtReader" when {

    "given a valid JWT string" should {

      "successfully read and verify a JWT" in forAll(canonicalHttpRequestGen) {
        request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          forAll(signedSymmetricJwtStringGen(signingSecret,
                                             Seq("qsh" -> queryHash))) {
            token =>
              SymmetricJwtReader(signingSecret)
                .readAndVerify(token, queryHash) match {
                case Right(jwt) => jwt mustBe a[Jwt]
                case Left(e)    => fail(e)
              }
          }
      }

      "successfully read and verify a JWT even if qsh is not present (self-authenticated)" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        forAll(signedSymmetricJwtStringGen(signingSecret)) { token =>
          SymmetricJwtReader(signingSecret)
            .readAndVerify(token, queryHash) match {
            case Right(jwt) => jwt mustBe a[Jwt]
            case Left(e)    => fail(e)
          }
        }
      }

      "fail if algorithm ('alg') is is not HS256" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
          val signingSecret64Token =
            "EVqrJGaYbf=EGVwg)aYAxXJqm2zjfab6EVqrJGaYbf=EGVwg)aYAxXJqm2zjfab6"
          forAll(
            signedSymmetricJwtStringGen(signingSecret64Token,
                                        Seq.empty,
                                        JWSAlgorithm.HS512)) { token =>
            SymmetricJwtReader(signingSecret64Token)
              .readAndVerify(token, queryHash) match {
              case Left(e) => e mustBe a[JwtInvalidSigningAlgorithmError]
              case Right(jwt) =>
                fail(
                  s"Expected validation for JWT ($jwt) with 'alg' claim to fail")
            }
          }
        }
      }

      "fail if not before claim ('nbf') is after expiry time" in {
        forAll(canonicalHttpRequestGen) { request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          val notBefore = Instant.now plus Duration.ofMinutes(45)
          val customClaims = Seq("nbf" -> Date.from(notBefore))
          implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
          forAll(signedSymmetricJwtStringGen(signingSecret, customClaims)) {
            token =>
              SymmetricJwtReader(signingSecret)
                .readAndVerify(token, queryHash) match {
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
          implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
          forAll(signedSymmetricJwtStringGen(signingSecret, customClaims)) {
            token =>
              SymmetricJwtReader(signingSecret)
                .readAndVerify(token, queryHash) match {
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
          implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
          forAll(signedSymmetricJwtStringGen(signingSecret, customClaims)) {
            token =>
              SymmetricJwtReader(signingSecret)
                .readAndVerify(token, queryHash) match {
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
          implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
          forAll(signedSymmetricJwtStringGen(signingSecret, customClaims)) {
            token =>
              SymmetricJwtReader(signingSecret)
                .readAndVerify(token, queryHash) match {
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
          implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
          forAll(signedSymmetricJwtStringGen(signingSecret, customClaims)) {
            token =>
              SymmetricJwtReader(signingSecret)
                .readAndVerify(token, queryHash) match {
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
        forAll(signedSymmetricJwtStringGen(signingSecret,
                                           Seq("qsh" -> queryHash)),
               Gen.alphaNumStr) { (token, invalidQsh) =>
          SymmetricJwtReader(signingSecret)
            .readAndVerify(token, invalidQsh) match {
            case Left(failure) => failure mustBe a[JwtVerificationError]
            case Right(_)      => fail()
          }
        }
      }

      "fail to read a JWT from an unsigned token string" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        forAll(unsignedJwtStringGen(Seq("qsh" -> queryHash))) { token =>
          SymmetricJwtReader(signingSecret)
            .readAndVerify(token, queryHash) match {
            case Left(failure) => failure mustBe a[ParsingFailure]
            case Right(_)      => fail()
          }
        }
      }

      "fail to read a JWT from a token string with invalid signature" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        val customClaims = Seq("qsh" -> queryHash)
        implicit val rawJwtNoShrink: Shrink[RawJwt] = Shrink.shrinkAny
        forAll(signedSymmetricJwtStringGen(signingSecret, customClaims)) {
          token =>
            SymmetricJwtReader(signingSecret)
              .readAndVerify(token.dropRight(5), queryHash) match {
              case Left(failure) => failure mustBe a[JwtSignatureMismatchError]
              case Right(_)      => fail()
            }
        }
      }

    }

  }

}
