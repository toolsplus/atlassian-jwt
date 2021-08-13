package io.toolsplus.atlassian.jwt

import com.nimbusds.jwt.JWTClaimsSet

import java.time.Instant
import scala.util.Left

object JwtClaimSetVerifiers {

  type ClaimSetVerifier = JWTClaimsSet => Either[Error, JWTClaimsSet]

  /**
    * Validate that claim set contains issue time and expiration time.
    *
    * @return Either given claim set if successful or JwtInvalidClaimError
    */
  def hasIssueTimeAndExpirationTime: ClaimSetVerifier = { claims =>
    if (Option(claims.getIssueTime).isEmpty || Option(claims.getExpirationTime).isEmpty) {
      Left(JwtInvalidClaimError(
        "'exp' and 'iat' are required claims. Atlassian JWT does not allow JWTs with unlimited lifetime"))
    } else {
      Right(claims)
    }
  }

  /**
    * Validate that claim set expiration time is after 'not before' (nbf) time.
    *
    * Note that if 'not before' claim is not defined validation is successful.
    *
    * @return Either given claim set if successful or JwtInvalidClaimError
    */
  def expirationTimeIsAfterNotBefore: ClaimSetVerifier = { claims =>
    if (Option(claims.getNotBeforeTime).isDefined) {
      if (Option(claims.getExpirationTime).isDefined && claims.getExpirationTime
            .after(claims.getNotBeforeTime)) {
        Right(claims)
      } else {
        Left(JwtInvalidClaimError(
          s"The expiration time must be after the not-before time but exp=${claims.getExpirationTime} and nbf=${claims.getNotBeforeTime}"))
      }
    } else {
      Right(claims)
    }
  }

  /** Validate that claim set 'not before' time is before current time.
    *
    * Note that if 'not before' claim is not defined validation is successful.
    *
    * @param now Current time
    * @param leewaySeconds Leeway in seconds that timing is allowed to be off
    * @return Either given claim set if successful or JwtTooEarlyError
    */
  def nowIsAfterNotBefore(now: Instant,
                          leewaySeconds: Int): ClaimSetVerifier = { claims =>
    val nowPlusLeeway =
      now.plusSeconds(leewaySeconds)
    if (Option(claims.getNotBeforeTime).isDefined && claims.getNotBeforeTime.toInstant
          .isAfter(nowPlusLeeway)) {
      Left(
        JwtTooEarlyError(claims.getNotBeforeTime.toInstant, now, leewaySeconds))
    } else {
      Right(claims)
    }
  }

  /** Validate that claim set expiration time is after current time.
    *
    * @param now Current time
    * @param leewaySeconds Leeway in seconds that timing is allowed to be off
    * @return Either given claim set if successful or JwtExpiredError
    */
  def nowIsBeforeExpirationTime(now: Instant,
                                leewaySeconds: Int): ClaimSetVerifier = {
    claims =>
      val nowMinusLeeway = now.minusSeconds(leewaySeconds)
      Option(claims.getExpirationTime) match {
        case Some(expiry) =>
          if (expiry.toInstant.isBefore(nowMinusLeeway)) {
            Left(
              JwtExpiredError(claims.getExpirationTime.toInstant,
                              now,
                              leewaySeconds))
          } else {
            Right(claims)
          }
        case None =>
          Left(JwtInvalidClaimError(
            s"The expiration time must be defined (exp=${claims.getExpirationTime})"))

      }
  }

  /** Verify query string hash claim if it is present, otherwise assume
    * successful verification.
    *
    * @param queryStringHash Expected query string hash.
    * @return Either given claim set if successful or JwtInvalidClaimError
    */
  def queryStringHash(queryStringHash: String): ClaimSetVerifier = { claims =>
    val maybeExtractedQueryStringHash =
      Option(claims.getClaim(HttpRequestCanonicalizer.QueryStringHashClaimName))
    maybeExtractedQueryStringHash match {
      case Some(extractedQueryStringHash) =>
        if (queryStringHash != extractedQueryStringHash) {
          Left(JwtInvalidClaimError(
            s"Expecting claim '${HttpRequestCanonicalizer.QueryStringHashClaimName}' to have value '$queryStringHash' but instead it has the value '$maybeExtractedQueryStringHash'"))
        } else Right(claims)
      case None => Right(claims)
    }
  }

  /**
    * Standard JWT claims. These claims should be verified for every JWT.
    *
    * @param now Current time
    * @param secondsLeeway Leeway in seconds the JWT time may be off relative to the computed time
    * @return Sequence of standard claim verifiers
    */
  def standardClaimVerifiers(now: Instant,
                             secondsLeeway: Int): Seq[ClaimSetVerifier] = Seq(
    hasIssueTimeAndExpirationTime,
    expirationTimeIsAfterNotBefore,
    nowIsAfterNotBefore(now, secondsLeeway),
    nowIsBeforeExpirationTime(now, secondsLeeway)
  )

}
