package io.toolsplus.atlassian.jwt

import java.time.Instant

/**
  * Indicates a JWT was well-formed, but failed to validate.
  */
trait JwtVerificationError

/**
  * Indicates that the JWT was not well-formed, e.g. the JWT JSON is invalid.
  */
trait JwtParsingError

/**
  * The base exception type for both decoding and parsing errors.
  */
sealed abstract class Error extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

final case class JwtTooEarlyError(notBefore: Instant,
                                  now: Instant,
                                  leewaySeconds: Int)
    extends Error
    with JwtVerificationError {
  override def getMessage: String =
    s"Not-before time is $notBefore and time is now $now ($leewaySeconds " +
      s"leeway seconds is allowed)"
}

/**
  * Indicates that the JWT's signature does not match its contents or the
  * shared secret for the specified issuer.
  */
final case class JwtSignatureMismatchError(message: String)
    extends Error
    with JwtVerificationError {
  override def getMessage: String = message
}

/**
  * If an expected claim is missing or the value of a reserved claim did not
  * match its expected format.
  */
final case class JwtInvalidClaimError(message: String)
    extends Error
    with JwtVerificationError {
  override def getMessage: String = message
}

/**
  * If the JWT's timestamps show that it has expired.
  */
final case class JwtExpiredError(expiredAt: Instant,
                                 now: Instant,
                                 leewaySeconds: Int)
    extends Error
    with JwtVerificationError {
  override def getMessage: String =
    s"Expired at $expiredAt and time is now $now ($leewaySeconds seconds " +
      s"leeway is allowed)"
}

/**
  * If a problem was encountered while signing a JWT.
  */
final case class JwtSigningError(message: String, underlying: Throwable)
    extends Exception {
  override def getMessage: String = message
}

final case class ParsingFailure(message: String, underlying: Throwable)
    extends Error
    with JwtParsingError {
  override def getMessage: String = message
}
