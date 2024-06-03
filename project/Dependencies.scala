import sbt._

object Dependencies {
  val core = Seq(
    Library.cats,
    Library.nimbusJoseJwt,
    Library.bouncyCastle,
    Library.scalaCheck % "test",
    Library.scalaTestPlusCheck % "test",
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
  val cats = "2.12.0"
  val nimbusJoseJwt = "9.39.3"
  val bouncyCastle = "1.70"
  val circe = "0.14.7"
  val scalaTest = "3.2.18"
  val scalaCheck = "1.18.0"
  val scalaTestPlusScalaCheck = "3.2.18.0"
  val scalaCheckDateTime = "0.7.0"
}

object Library {
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % Version.nimbusJoseJwt
  val bouncyCastle = "org.bouncycastle" % "bcpg-jdk15on" % Version.bouncyCastle
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circeOptics = "io.circe" %% "circe-optics" % "0.14.1"
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
  val scalaTestPlusCheck = "org.scalatestplus" %% "scalacheck-1-17" % Version.scalaTestPlusScalaCheck
  val scalaCheckDateTime = "com.47deg" %% "scalacheck-toolbox-datetime" % Version.scalaCheckDateTime
}
