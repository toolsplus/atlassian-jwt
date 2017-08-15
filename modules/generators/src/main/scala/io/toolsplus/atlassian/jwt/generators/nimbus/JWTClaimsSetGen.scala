package io.toolsplus.atlassian.jwt.generators.nimbus

import java.time.{Duration, ZonedDateTime}
import java.util.Date

import com.fortysevendeg.scalacheck.datetime.GenDateTime._
import com.fortysevendeg.scalacheck.datetime.instances.jdk8._
import com.fortysevendeg.scalacheck.datetime.jdk8.granularity.minutes
import com.nimbusds.jwt.JWTClaimsSet
import org.scalacheck.Gen

trait JWTClaimsSetGen {

  def jwtClaimsSetGen(
      customClaims: Seq[(String, Any)] = Seq.empty): Gen[JWTClaimsSet] =
    for {
      issuer <- Gen.alphaStr suchThat (!_.isEmpty)
      subject <- Gen.alphaStr suchThat (!_.isEmpty)
      now = ZonedDateTime.now
      issuedAt <- Gen.const(now).map(t => Date.from(t.toInstant))
      expiration <- genDateTimeWithinRange(now.plusMinutes(5),
                                           Duration
                                             .ofMinutes(25))
        .map(t => Date.from(t.toInstant))
      builder = new JWTClaimsSet.Builder()
        .issuer(issuer)
        .subject(subject)
        .issueTime(issuedAt)
        .expirationTime(expiration)
    } yield
      customClaims
        .foldLeft(builder)((b, claim) => b.claim(claim._1, claim._2))
        .build()
}
