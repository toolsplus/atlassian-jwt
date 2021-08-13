package io.toolsplus.atlassian.jwt.asymmetric

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jose.crypto.RSASSAVerifier
import io.toolsplus.atlassian.jwt.JwtClaimSetVerifiers.ClaimSetVerifier
import io.toolsplus.atlassian.jwt.{JwtInvalidClaimError, JwtReader}

import java.net.URI
import java.security.interfaces.RSAPublicKey
import scala.util.Left
import scala.jdk.CollectionConverters._

/**
  * JWT Reader to read and verify asymmetrically signed JWT strings.
  *
  * Atlassian Connect asymmetrically signed JWTs should always be signed using
  * the RS256 algorithm. This reader will only accept JWTs with alg RS256.
  *
  * Each reader instance has to be configured with the public key that it will use
  * to verify JWT signatures. Additionally, it requires the app's base URL to validate
  * the audience claim.
  */
case class AsymmetricJwtReader(publicKey: RSAPublicKey, appBaseUrl: String) extends JwtReader {
  override protected val verifier: JWSVerifier = new RSASSAVerifier(publicKey)
  override protected val allowedAlgorithm: JWSAlgorithm = JWSAlgorithm.RS256

  override protected def extraJwtClaimSetVerifiers: Seq[ClaimSetVerifier] = Seq(
    audienceMatchesAppBaseUrl(appBaseUrl)
  )

  /**
    * Validate that claim set contains issue time and expiration time.
    *
    * @return Either given claim set if successful or JwtInvalidClaimError
    */
  def audienceMatchesAppBaseUrl(appBaseUrl: String): ClaimSetVerifier = {
    claims =>
      claims.getAudience.asScala.headOption match {
        case Some(audienceClaim) =>
          if (URI.create(appBaseUrl).equals(URI.create(audienceClaim)))
            Right(claims)
          else
            Left(JwtInvalidClaimError(
              s"Audience claim '$audienceClaim' in JWT does not match app baseURL '$appBaseUrl' from the app descriptor"))

        case None =>
          Left(JwtInvalidClaimError(s"Missing audience claim in JWT"))

      }
  }
}
