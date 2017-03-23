package io.toolsplus.atlassian.jwt

import java.time.Instant

import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jose.{JWSObject, JWSVerifier}
import com.nimbusds.jwt.JWTClaimsSet

import scala.util.{Failure, Left, Success, Try}

/**
  * JWT Reader to read and verify JWT strings.
  *
  * Each reader has to be configured with the shared secret that it will use
  * to verify JWT signatures.
  *
  * NOTE: If the JWT does not include the qsh claim, verification will still succeed.
  * This is because self-authenticated tokens do not contain the qsh claim.
  *
  */
case class JwtReader(sharedSecret: String) {

  private final val verifier: JWSVerifier = new MACVerifier(sharedSecret)

  def readAndVerify(jwt: String, queryStringHash: String): Either[Error, Jwt] =
    read(jwt, queryStringHash, shouldVerifySignature = true)

  private def read(jwt: String,
                   queryStringHash: String,
                   shouldVerifySignature: Boolean): Either[Error, Jwt] = {
    JwtParser.parseJWSObject(jwt) match {
      case Right(jwsObject) =>
        if (shouldVerifySignature) {
          verifySignature(jwsObject) match {
            case Right(_) => verifyRest(jwsObject, queryStringHash)
            case l @ Left(_) => l.asInstanceOf[Either[Error, Jwt]]
          }
        } else {
          verifyRest(jwsObject, queryStringHash)
        }
      case l @ Left(_) => l.asInstanceOf[Either[Error, Jwt]]
    }
  }

  private def verifyRest(jwsObject: JWSObject,
                         queryStringHash: String): Either[Error, Jwt] =
    JwtParser.parseJWTClaimsSet(jwsObject.getPayload.toJSONObject) match {
      case Right(claims) =>
        verifyStandardClaims(claims) match {
          case Right(_) =>
            verifyQueryStringHash(claims, queryStringHash) match {
              case Right(_) =>
                Right(Jwt(jwsObject, claims))
              case l @ Left(_) => l.asInstanceOf[Either[Error, Jwt]]
            }
          case l @ Left(_) => l.asInstanceOf[Either[Error, Jwt]]
        }
      case l @ Left(_) => l.asInstanceOf[Either[Error, Jwt]]
    }

  private def verifyStandardClaims(
      claims: JWTClaimsSet): Either[Error, JWTClaimsSet] = {
    if (claims.getIssueTime == null || claims.getExpirationTime == null) {
      return Left(JwtInvalidClaimError(
        "'exp' and 'iat' are required claims. Atlassian JWT does not allow JWTs with unlimited lifetimes."))
    }

    val now = Instant.now()
    val nowMinusLeeway =
      now.minusSeconds(JwtReader.TIME_CLAIM_LEEWAY_SECONDS)
    val nowPlusLeeway =
      now.plusSeconds(JwtReader.TIME_CLAIM_LEEWAY_SECONDS)

    if (null != claims.getNotBeforeTime) {
      if (!claims.getExpirationTime.after(claims.getNotBeforeTime)) {
        return Left(JwtInvalidClaimError(
          s"The expiration time must be after the not-before time but exp=${claims.getExpirationTime} and nbf=${claims.getNotBeforeTime}"))
      }

      if (claims.getNotBeforeTime.toInstant.isAfter(nowPlusLeeway)) {
        return Left(
          JwtTooEarlyError(claims.getNotBeforeTime.toInstant,
                           now,
                           JwtReader.TIME_CLAIM_LEEWAY_SECONDS))
      }
    }

    if (claims.getExpirationTime.toInstant.isBefore(nowMinusLeeway)) {
      return Left(
        JwtExpiredError(claims.getExpirationTime.toInstant,
                        now,
                        JwtReader.TIME_CLAIM_LEEWAY_SECONDS))
    }

    Right(claims)

  }

  /** Verify query string hash claim if it is present, otherwise assume
    * successful verification.
    *
    * @param claims Claim set to verify.
    * @param queryStringHash Expected query string hash.
    * @return Either claim set if verification succeeded or Error otherwise.
    */
  private def verifyQueryStringHash(
      claims: JWTClaimsSet,
      queryStringHash: String): Either[Error, JWTClaimsSet] = {
    val maybeExtractedQueryStringHash =
      Option(
        claims.getClaim(HttpRequestCanonicalizer.QUERY_STRING_HASH_CLAIM_NAME))
    maybeExtractedQueryStringHash match {
      case Some(extractedQueryStringHash) =>
        if (queryStringHash != extractedQueryStringHash) {
          Left(JwtInvalidClaimError(
            s"Expecting claim '${HttpRequestCanonicalizer.QUERY_STRING_HASH_CLAIM_NAME}' to have value '$queryStringHash' but instead it has the value '$maybeExtractedQueryStringHash'"))
        } else Right(claims)
      case None => Right(claims)
    }
  }

  private def verifySignature(jwsObject: JWSObject): Either[Error, JWSObject] = {
    Try(jwsObject.verify(verifier)) match {
      case Success(isValid) =>
        if (isValid)
          Right(jwsObject)
        else
          Left(JwtSignatureMismatchError(jwsObject.getParsedString))
      case Failure(exception) =>
        Left(JwtSignatureMismatchError(exception.getMessage))
    }
  }

}

object JwtReader {

  /** The JWT spec says that implementers "MAY provide for some small leeway,
    * usually no more than a few minutes, to account for clock skew".
    * Calculations of the current time for the purposes of accepting or
    * rejecting time-based claims (e.g. "exp" and "nbf") will allow for the
    * current time being plus or minus this leeway, resulting in some
    * time-based claims that are marginally before or after the current time
    * being accepted instead of rejected.
    */
  private val TIME_CLAIM_LEEWAY_SECONDS: Int = 30

}
