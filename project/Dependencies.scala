import sbt._

object Dependencies {
  val core = Seq(
    Library.cats,
    Library.nimbusJoseJwt,
    Library.bouncyCastle,
    Library.scalaCheck % "test",
    Library.scalaCheckDateTime % "test",
    Library.scalaTest % "test",
    Library.circeCore % "test",
    Library.circeParser % "test",
    Library.circeOptics % "test"
  )

  val generators = Seq(
    Library.nimbusJoseJwt,
    Library.scalaCheck,
    Library.scalaCheckDateTime
  )
}

object Version {
  val cats = "1.0.1"
  val nimbusJoseJwt = "5.4"
  val bouncyCastle = "1.59"
  val circe = "0.9.1"
  val scalaTest = "3.0.5"
  val scalaCheck = "1.13.5"
  val scalaCheckDateTime = "0.2.4"
}

object Library {
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % Version.nimbusJoseJwt
  val bouncyCastle = "org.bouncycastle" % "bcpg-jdk15on" % Version.bouncyCastle
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circeOptics = "io.circe" %% "circe-optics" % Version.circe
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
  val scalaCheckDateTime = "com.47deg" %% "scalacheck-toolbox-datetime" % Version.scalaCheckDateTime
}
