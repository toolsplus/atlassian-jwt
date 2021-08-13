package io.toolsplus.atlassian.jwt.symmetric

import com.nimbusds.jose.{JWSAlgorithm, JWSVerifier}
import com.nimbusds.jose.crypto.MACVerifier
import io.toolsplus.atlassian.jwt.JwtReader

/**
  * JWT Reader to read and verify symmetrically signed JWT strings.
  *
  * Atlassian Connect symmetrically signed JWTs should always be signed using
  * the HS256 algorithm. This reader will only accept JWTs with alg HS256.
  *
  * Each reader instance has to be configured with the shared secret that it will use
  * to verify JWT signatures.
  */
case class SymmetricJwtReader(sharedSecret: String) extends JwtReader {
  override protected val verifier: JWSVerifier = new MACVerifier(sharedSecret)
  override protected val allowedAlgorithm: JWSAlgorithm = JWSAlgorithm.HS256
}
