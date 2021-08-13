package io.toolsplus.atlassian.jwt.generators.util

import java.security.interfaces.RSAPublicKey
import java.security.{KeyPair, KeyPairGenerator}
import java.util.Base64

trait RSAKeyPairGenerator {

  def defaultKeySize = 2048

  def kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")

  /**
    * Generates an RSA key pair of the given key size.
    *
    * @param keySize Required key size
    * @return RSA key pair with the given key size
    */
  def generateKeyPair(keySize: Int = defaultKeySize): KeyPair = {
    kpg.initialize(keySize)
    kpg.generateKeyPair
  }

  /**
    * Generates the PEM formatted public key string.
    *
    * @param key Public key to print in PEM format
    * @return PEM formatted public key
    */
  def toPemString(key: RSAPublicKey): String = {
    val startTag = "-----BEGIN PUBLIC KEY-----"
    val endTag = "-----END PUBLIC KEY-----"
    val base64encodedKey = new String(Base64.getEncoder.encode(key.getEncoded))
    s"$startTag\n$base64encodedKey\n$endTag"
  }

}
