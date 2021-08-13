package io.toolsplus.atlassian.jwt.generators.util

object JwtTestHelper extends RSAKeyPairGenerator {

  /**
    * A JWT signing secret compatible with HS256 algorithm.
    *
    * This is intended to be used for testing.
    */
  val defaultSigningSecret: String = "EVqrJGaYbf=EGVwg)aYAxXJqm2zjfab6"

}
