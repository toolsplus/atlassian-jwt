package io.toolsplus.atlassian.jwt.generators.nimbus

import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.{PlainJWT, SignedJWT}
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.generators.util.JwtTestHelper
import org.scalacheck.Gen

trait NimbusGen extends JWSHeaderGen with JWTClaimsSetGen {

  def signedJwtStringGen(
      secret: String = JwtTestHelper.defaultSigningSecret,
      customClaims: Seq[(String, Any)] = Seq.empty): Gen[RawJwt] =
    for {
      header <- jwsHeaderGen(secret)
      claimsSet <- jwtClaimsSetGen(customClaims)
      token = new SignedJWT(header, claimsSet)
      _ = token.sign(new MACSigner(secret))
    } yield token.serialize

  def unsignedJwtStringGen(
      customClaims: Seq[(String, Any)] = Seq.empty): Gen[RawJwt] =
    for {
      claims <- jwtClaimsSetGen(customClaims)
      token = new PlainJWT(claims)
    } yield token.serialize

}
