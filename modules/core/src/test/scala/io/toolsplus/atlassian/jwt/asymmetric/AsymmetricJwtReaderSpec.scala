package io.toolsplus.atlassian.jwt.asymmetric

import com.nimbusds.jose.JWSAlgorithm
import io.toolsplus.atlassian.jwt._
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.RSAKeyPairGenerator
import org.scalacheck.{Gen, Shrink}

import java.security.interfaces.RSAPublicKey
import java.time.{Duration, Instant}
import java.util.Date

class AsymmetricJwtReaderSpec extends TestSpec with RSAKeyPairGenerator {

  private val appBaseUrl: String = "https://test.atlassian.net"
  private val keyId: String = "0e50fccb-239d-4991-a5db-dc850ba3f236"
  private val keyPair = generateKeyPair()
  private val publicKey = keyPair.getPublic.asInstanceOf[RSAPublicKey]
  private val privateKey = keyPair.getPrivate

  private val jwtReader = AsymmetricJwtReader(publicKey, appBaseUrl)

  private def jwtGen(qsh: String): Gen[RawJwt] =
    signedAsymmetricJwtStringGen(keyId,
                                 privateKey,
                                 Seq("qsh" -> qsh, "aud" -> appBaseUrl))

  "Using a AsymmetricJwtReader" when {

    "given a valid JWT string" should {

      "successfully read and verify a JWT" in forAll(canonicalHttpRequestGen) {
        request =>
          val queryHash =
            HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
          forAll(jwtGen(queryHash)) { token =>
            jwtReader
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
        forAll(
          signedAsymmetricJwtStringGen(keyId,
                                       privateKey,
                                       Seq("aud" -> appBaseUrl))) { token =>
          jwtReader
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
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(
            signedAsymmetricJwtStringGen(keyId,
                                         privateKey,
                                         Seq.empty,
                                         JWSAlgorithm.RS512)) { token =>
            jwtReader
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
          val customClaims = Seq("qsh" -> queryHash,
                                 "aud" -> appBaseUrl,
                                 "nbf" -> Date.from(notBefore))
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)) {
            token =>
              jwtReader
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
          val customClaims = Seq("qsh" -> queryHash,
                                 "aud" -> appBaseUrl,
                                 "nbf" -> Date.from(notBefore))
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)) {
            token =>
              jwtReader
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
          val customClaims = Seq("qsh" -> queryHash,
                                 "aud" -> appBaseUrl,
                                 "exp" -> Date.from(expiry))
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)) {
            token =>
              jwtReader
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
          val customClaims =
            Seq("qsh" -> queryHash, "aud" -> appBaseUrl, "iat" -> null)
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)) {
            token =>
              jwtReader
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
          val customClaims =
            Seq("qsh" -> queryHash, "aud" -> appBaseUrl, "exp" -> null)
          implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
          forAll(signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)) {
            token =>
              jwtReader
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
        forAll(signedAsymmetricJwtStringGen(keyId,
                                            privateKey,
                                            Seq("qsh" -> queryHash,
                                                "aud" -> appBaseUrl)),
               Gen.alphaNumStr) { (token, invalidQsh) =>
          jwtReader
            .readAndVerify(token, invalidQsh) match {
            case Left(failure) => failure mustBe a[JwtVerificationError]
            case Right(_)      => fail
          }
        }
      }

      "fail to read a JWT from an unsigned token string" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        forAll(unsignedJwtStringGen(Seq("qsh" -> queryHash))) { token =>
          jwtReader
            .readAndVerify(token, queryHash) match {
            case Left(failure) => failure mustBe a[ParsingFailure]
            case Right(_)      => fail
          }
        }
      }

      "fail to read a JWT from a token string with invalid signature" in forAll(
        canonicalHttpRequestGen) { request =>
        val queryHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)
        val customClaims = Seq("qsh" -> queryHash, "aud" -> appBaseUrl)
        implicit val rawJwtNoShrink = Shrink[RawJwt](_ => Stream.empty)
        forAll(signedAsymmetricJwtStringGen(keyId, privateKey, customClaims)) {
          token =>
            jwtReader
              .readAndVerify(token.dropRight(5), queryHash) match {
              case Left(failure) => failure mustBe a[JwtSignatureMismatchError]
              case Right(_)      => fail
            }
        }
      }

    }

  }

}
