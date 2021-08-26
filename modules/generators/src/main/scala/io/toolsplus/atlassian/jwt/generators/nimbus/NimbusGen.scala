package io.toolsplus.atlassian.jwt.generators.nimbus

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.{MACSigner, RSASSASigner}
import com.nimbusds.jwt.{PlainJWT, SignedJWT}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen

import java.security.PrivateKey

trait NimbusGen extends JWSHeaderGen with JWTClaimsSetGen {

  def signedSymmetricJwtStringGen(
      secret: String = JwtTestHelper.defaultSigningSecret,
      customClaims: Seq[(String, Any)] = Seq.empty,
      algorithm: JWSAlgorithm = JWSAlgorithm.HS256): Gen[RawJwt] =
    for {
      header <- jwsHeaderGen(algorithm, None)
      claimsSet <- jwtClaimsSetGen(customClaims)
      token = new SignedJWT(header, claimsSet)
      _ = token.sign(new MACSigner(secret))
    } yield token.serialize

  def signedAsymmetricJwtStringGen(
      keyId: String,
      privateKey: PrivateKey,
      customClaims: Seq[(String, Any)] = Seq.empty,
      algorithm: JWSAlgorithm = JWSAlgorithm.RS256
  ): Gen[RawJwt] =
    for {
      header <- jwsHeaderGen(algorithm, Some(keyId))
      claimsSet <- jwtClaimsSetGen(customClaims)
      token = new SignedJWT(header, claimsSet)
      _ = token.sign(new RSASSASigner(privateKey))
    } yield token.serialize

  def unsignedJwtStringGen(
      customClaims: Seq[(String, Any)] = Seq.empty): Gen[RawJwt] =
    for {
      claims <- jwtClaimsSetGen(customClaims)
      token = new PlainJWT(claims)
    } yield token.serialize

}
