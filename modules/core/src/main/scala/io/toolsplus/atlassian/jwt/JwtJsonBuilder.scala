package io.toolsplus.atlassian.jwt

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}

import net.minidev.json.JSONObject

import scala.collection.JavaConverters._

object JwtJsonBuilder {
  private[jwt] val DEFAULT_JWT_LIFETIME =
    Duration.of(180L, ChronoUnit.SECONDS)

  def apply() = new JwtJsonBuilder(DEFAULT_JWT_LIFETIME)
}

class JwtJsonBuilder(val expireAfter: Duration) {

  private final val json = new JSONObject()
  private val issuedAt: Long = Instant.now.getEpochSecond

  withIssuedAt(issuedAt)
  withExpirationTime(issuedAt + expireAfter.getSeconds)

  def withAudience(aud: Seq[String]): JwtJsonBuilder =
    withProperty("aud", aud.asJava)

  def withExpirationTime(exp: Long): JwtJsonBuilder =
    withProperty("exp", exp.asInstanceOf[java.lang.Long])

  def withIssuedAt(iat: Long): JwtJsonBuilder =
    withProperty("iat", iat.asInstanceOf[java.lang.Long])

  def withIssuer(iss: String): JwtJsonBuilder = withProperty("iss", iss)

  def withJwtId(jti: String): JwtJsonBuilder = withProperty("jti", jti)

  def withNotBefore(nbf: Long): JwtJsonBuilder =
    withProperty("nbf", nbf.asInstanceOf[java.lang.Long])

  def withSubject(sub: String): JwtJsonBuilder = withProperty("sub", sub)

  def withType(typ: String): JwtJsonBuilder = withProperty("typ", typ)

  def withQueryHash(qsh: String): JwtJsonBuilder = withProperty("qsh", qsh)

  def withClaim(name: String, value: AnyRef): JwtJsonBuilder =
    withProperty(name, value)

  def build: String = json.toString

  def containsClaim(name: String): Boolean = json.containsKey(name)

  private def withProperty(key: String, value: AnyRef): JwtJsonBuilder = {
    json.put(key, value)
    this
  }

}
