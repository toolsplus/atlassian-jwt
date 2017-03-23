package io.toolsplus.atlassian.jwt

import io.toolsplus.atlassian.jwt.generators.core.JwtGen
import io.toolsplus.atlassian.jwt.generators.nimbus.NimbusGen
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class HttpRequestCanonicalizerSpec
    extends TestSpec
    with GeneratorDrivenPropertyChecks
    with JwtGen
    with NimbusGen {

  "Using a HttpRequestCanonicalizer" when {

    "given a valid CanonicalHttpRequest" should {

      "compute the correct canonical request string" in forAll(
        canonicalHttpRequestGen) { request =>
        def partsOf(r: String) =
          r.split(HttpRequestCanonicalizer.CANONICAL_REQUEST_PART_SEPARATOR)
        val canonicalizedRequest =
          HttpRequestCanonicalizer.canonicalize(request)
        if (canonicalizedRequest.endsWith("&"))
          partsOf(canonicalizedRequest).length mustBe 2
      }

      "compute the correct canonical method string" in forAll(
        canonicalHttpRequestGen) { request =>
        val canonicalizedMethod =
          HttpRequestCanonicalizer.canonicalizeMethod(request)
        canonicalizedMethod mustBe canonicalizedMethod.toUpperCase
      }

      "compute the correct canonical uri string" in forAll(
        canonicalHttpRequestGen) { request =>
        val canonicalizedUri =
          HttpRequestCanonicalizer.canonicalizeUri(request)
        canonicalizedUri must startWith("/")
        if (canonicalizedUri.length > 1) canonicalizedUri must not endWith "/"
        canonicalizedUri must not contain HttpRequestCanonicalizer.CANONICAL_REQUEST_PART_SEPARATOR
      }

      "successfully compute canonical request hash" in forAll(
        canonicalHttpRequestGen) { request =>
        val canonicalRequestHash =
          HttpRequestCanonicalizer.computeCanonicalRequestHash(request)

      }

    }

  }

}
