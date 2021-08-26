package io.toolsplus.atlassian.jwt

import cats.implicits._
import com.nimbusds.jose.{Algorithm, JWSObject, JWSVerifier}
import com.nimbusds.jwt.JWTClaimsSet
import io.toolsplus.atlassian.jwt.JwtClaimSetVerifiers.ClaimSetVerifier

import java.time.Instant
import scala.util.{Failure, Left, Success, Try}

trait JwtReader {

  protected def verifier: JWSVerifier
  protected def allowedAlgorithm: Algorithm

  /**
    * Additional JWT claim set verifiers to evaluate during to token verifications, defaults
    * to none.
    *
    * This allows implementing classes to inject extra validations that they may need.
    *
    * @return Additional claim set verifiers, none by default.
    */
  protected def extraJwtClaimSetVerifiers: Seq[ClaimSetVerifier] = Seq.empty

  /**
    * Parses the JWT, then validates the token header (signature and algorithm)
    * and finally validates standard claims, QSH and evaluates any extra claim
    * set validators.
    *
    * @param jwt JWT to read and validate
    * @param queryStringHash QSH compiled from the request the JWT was attached to
    * @return Parsed and validated JWT, or a validation error
    */
  def readAndVerify(jwt: String, queryStringHash: String): Either[Error, Jwt] =
    for {
      jwsObject <- JwtParser.parseJWSObject(jwt)
      _ <- verifySignature(jwsObject)
      _ <- verifyAlgorithm(jwsObject)
      result <- verifyPayload(jwsObject, queryStringHash)
    } yield result

  /**
    * Verifies the payload of the JWS object including the query string hash.
    *
    * @param jwsObject JWS object for which to verify the JWT payload
    * @param queryStringHash Query string hash associated with the request
    * @return Verified JWT if verification is successful.
    */
  private def verifyPayload(jwsObject: JWSObject,
                            queryStringHash: String): Either[Error, Jwt] =
    for {
      claims <- JwtParser.parseJWTClaimsSet(jwsObject.getPayload.toJSONObject)
      _ <- verifyStandardClaims(claims)
      _ <- JwtClaimSetVerifiers.queryStringHash(queryStringHash)(claims)
      _ <- verifyExtraClaims(claims)
    } yield Jwt(jwsObject, claims)

  /**
    * Verifies standard JWT claims.
    *
    * Note that this will short circuit, i.e. if any validation fails it stops
    * and returns the first validation error encountered.
    *
    * @param claims Claims to be validated
    * @return Validated claim set if success, the first validation error otherwise
    */
  private def verifyStandardClaims(
      claims: JWTClaimsSet): Either[Error, JWTClaimsSet] = {
    JwtClaimSetVerifiers
      .standardClaimVerifiers(Instant.now(), JwtReader.TimeClaimLeewaySeconds)
      .toList
      .foldM(claims)((_, verifier) => verifier(claims))
  }

  /**
    * Verifies any extra claims that may have been injected by the implementing class.
    *
    * Note that this will short circuit, i.e. if any validation fails it stops
    * and returns the first validation error encountered.
    *
    * @param claims Claims to be validated
    * @return Validated claim set if success, the first validation error otherwise
    */
  private def verifyExtraClaims(
      claims: JWTClaimsSet): Either[Error, JWTClaimsSet] = {
    extraJwtClaimSetVerifiers.toList.foldM(claims)((_, verifier) =>
      verifier(claims))
  }

  /**
    * Verifies that the alg field in the JMS object header matches the allowed algorithm.
    *
    * @param jwsObject JMS object for which to verify the algorithm field
    * @return Verified JMS object or an error if verification failed
    */
  private def verifyAlgorithm(
      jwsObject: JWSObject): Either[Error, JWSObject] = {
    val algorithm = jwsObject.getHeader.getAlgorithm
    if (allowedAlgorithm.equals(algorithm)) {
      Right(jwsObject)
    } else {
      Left(JwtInvalidSigningAlgorithmError(
        s"Expected JWT to be signed with $allowedAlgorithm but it was signed with $algorithm instead"))
    }
  }

  private def verifySignature(
      jwsObject: JWSObject): Either[Error, JWSObject] = {
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
  private val TimeClaimLeewaySeconds: Int = 30

}
