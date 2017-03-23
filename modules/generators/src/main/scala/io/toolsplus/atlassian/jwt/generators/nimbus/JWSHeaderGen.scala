package io.toolsplus.atlassian.jwt.generators.nimbus

import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.util.ByteUtils
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import org.scalacheck.Gen
import org.scalacheck.Gen._

import scala.collection.JavaConversions._

trait JWSHeaderGen {

  def jwsHMACAlgorithmGen: Gen[JWSAlgorithm] =
    oneOf(JWSAlgorithm.HS256, JWSAlgorithm.HS384, JWSAlgorithm.HS512)

  def jwsHeaderGen: Gen[JWSHeader] =
    for {
      alg <- jwsHMACAlgorithmGen
    } yield jwsHeader(alg)

  /** Returns a JWS header generator compatible with the given secret.
    *
    * Based on secret's length only certain signing algorithms can be chosen. This variant only generates headers
    * which are valid with the given secret.
    *
    * @param secret Reference secret
    * @return JWS header generator compatible with the given secret
    */
  def jwsHeaderGen(secret: String): Gen[JWSHeader] =
    for {
      alg <- oneOf(compatibleAlgorithms(secret).toSeq)
    } yield jwsHeader(alg)

  private def jwsHeader(alg: JWSAlgorithm): JWSHeader =
    new JWSHeader.Builder(alg).build()

  private def compatibleAlgorithms(secret: String): Set[JWSAlgorithm] =
    MACSigner.getCompatibleAlgorithms(ByteUtils.bitLength(secret.length)).toSet

}
