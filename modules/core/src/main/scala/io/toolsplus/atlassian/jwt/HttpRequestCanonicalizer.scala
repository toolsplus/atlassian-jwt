package io.toolsplus.atlassian.jwt

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.security.{MessageDigest, NoSuchAlgorithmException}

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest
import org.bouncycastle.util.encoders.Hex

/**
  * Instructions for computing the query hash parameter ("qsh") from a HTTP request.
  * -------------------------------------------------------------------------------------
  *
  * Overview:       query hash = hash(canonical-request)
  *
  * canonical-request = canonical-method + '&amp;' + canonical-URI + '&amp;' + canonical-query-string
  *
  * 1. Compute canonical method.
  * Simply the upper-case of the method name (e.g. "GET", "PUT").
  *
  * 2. Append the character '&amp;'
  *
  * 3. Compute canonical URI.
  * Discard the protocol, server, port, context path and query parameters from the full URL.
  * For requests targeting add-ons discard the `baseUrl` in the add-on descriptor.
  * (Removing the context path allows a reverse proxy to redirect incoming requests for "jira.example.com/getsomething"
  * to "example.com/jira/getsomething" without breaking authentication. The requester cannot know that the reverse proxy
  * will prepend the context path "/jira" to the originally requested path "/getsomething".)
  * Empty-string is not permitted; use "/" instead.
  * Do not suffix with a '/' character unless it is the only character.
  * Url-encode any '&amp;' characters in the path.
  *       E.g. in "http://server:80/some/path/?param=value" the canonical URI is "/some/path"
  * and in "http://server:80" the canonical URI is "/".
  *
  * 4. Append the character '&amp;'.
  *
  * 5. Compute the canonical query string.
  * Sort the query parameters primarily by their percent-encoded names and secondarily by their percent-encoded values.
  * Sorting is by codepoint: sort(["a", "A", "b", "B"]) =&gt; ["A", "B", "a", "b"].
  * For each parameter append its percent-encoded name, the '=' character and then its percent-encoded value.
  * In the case of repeated parameters append the ',' character and subsequent percent-encoded values.
  * Ignore the JWT query string parameter, if present.
  * Some particular values to be aware of: "+" is encoded as "%20",
  * "*" as "%2A" and
  * "~" as "~".
  * (These values used for consistency with OAuth1.)
  * An example: for a GET request to the not-yet-percent-encoded URL
  * "http://localhost:2990/path/to/service?zee_last=param&amp;repeated=parameter 1&amp;first=param&amp;
  * repeated=parameter 2"
  * the canonical request is "GET&amp;/path/to/service&amp;first=param&amp;repeated=parameter%201,parameter%202&amp;
  * zee_last=param".
  *
  * 6. Convert the canonical request string to bytes.
  * The encoding used to represent characters as bytes is UTF-8.
  *
  * 7. Hash the canonical request bytes using the SHA-256 algorithm.
  *    E.g. The SHA-256 hash of "foo" is "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae".
  */
object HttpRequestCanonicalizer {

  val QUERY_STRING_HASH_CLAIM_NAME: String = "qsh"

  /**
    * When the JWT message is specified in the query string of a URL then this is the parameter name.
    *
    * E.g. "jwt" in:
    * <pre>
    * http://server:80/some/path?otherparam=value&amp;jwt=eyJhbGciOiJIUzI1NiIsI.eyJleHAiOjEzNzg5NCI6MTM3ODk1MjQ4OH0
    * .cDihfcsKW_We_EY21tIs55dVwjU
    * </pre>
    */
  private val JWT_PARAM_NAME: String = "jwt"

  /**
    * Query parameter separator as it appears between "value1" and "param2" in the URL
    * "http://server/path?param1=value1&amp;param2=value2".
    */
  private val QUERY_PARAMS_SEPARATOR: Char = '&'

  /**
    * The character between "a" and "b%20c" in "some_param=a,b%20c"
    */
  private val ENCODED_PARAM_VALUE_SEPARATOR: String = ","

  /**
    * For separating the method, URI etc in a canonical request string.
    */
  private[jwt] val CANONICAL_REQUEST_PART_SEPARATOR: Char = '&'

  /**
    * Assemble the components of the HTTP request into the correct format so that they can be signed or hashed.
    *
    * @param request [[CanonicalHttpRequest]] that provides the necessary components
    * @return String encoding the canonical form of this request as required for constructing query string hash values
    * @throws UnsupportedEncodingException [[UnsupportedEncodingException]] if the [[java.net.URLEncoder]] cannot encode the request's field's characters
    */
  def canonicalize(request: CanonicalHttpRequest): String =
    s"${canonicalizeMethod(request)}$CANONICAL_REQUEST_PART_SEPARATOR" +
      s"${canonicalizeUri(request)}$CANONICAL_REQUEST_PART_SEPARATOR" +
      s"${canonicalizeQueryParameters(request)}"

  /**
    * Canonicalize the given [[CanonicalHttpRequest]] and hash it.
    * This request hash can be included as a JWT claim to verify that request components are genuine.
    *
    * @param request CanonicalHttpRequest to be canonicalized and hashed
    * @return String hash suitable for use as a JWT claim value
    * @throws UnsupportedEncodingException if the [[java.net.URLEncoder]] cannot encode the request's field's characters
    * @throws NoSuchAlgorithmException     if the hashing algorithm does not exist at runtime
    */
  def computeCanonicalRequestHash(request: CanonicalHttpRequest): String = {
    // prevent the code in this method being repeated in every call site that needs a request hash,
    // encapsulate the knowledge of the type of hash that we are using
    computeSha256Hash(canonicalize(request))
  }

  /**
    * Compute the SHA-256 hash of hashInput.
    * E.g. The SHA-256 hash of "foo" is "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae".
    *
    * @param hashInput String to be hashed.
    * @return String hash
    * @throws NoSuchAlgorithmException if the hashing algorithm does not exist at runtime
    */
  private def computeSha256Hash(hashInput: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashInputBytes = hashInput.getBytes
    digest.update(hashInputBytes, 0, hashInputBytes.length)
    new String(Hex.encode(digest.digest()))
  }

  private[jwt] def canonicalizeMethod(request: CanonicalHttpRequest): String =
    request.method.toUpperCase

  private[jwt] def canonicalizeUri(request: CanonicalHttpRequest): String = {
    val relativeRequestPath = request.relativePath
    val pathWithoutTrailingSlash =
      if (relativeRequestPath.endsWith("/")) relativeRequestPath.dropRight(1)
      else
        relativeRequestPath
    val path =
      if (pathWithoutTrailingSlash.isEmpty) "/" else pathWithoutTrailingSlash

    val separatorAsString = CANONICAL_REQUEST_PART_SEPARATOR.toString

    // If the separator is not URL encoded then the following URLs have the same query-string-hash:
    //   https://djtest9.jira-dev.com/rest/api/2/project&a=b?x=y
    //   https://djtest9.jira-dev.com/rest/api/2/project?a=b&x=y
    val encodedPath =
      path.replaceAll(separatorAsString, percentEncode(separatorAsString))

    if (encodedPath.startsWith("/")) encodedPath else s"/$encodedPath"
  }

  private[jwt] def canonicalizeQueryParameters(
      request: CanonicalHttpRequest): String = {
    (request.parameterMap - JWT_PARAM_NAME).toSeq
      .sortBy {
        case (key, values) =>
          s"${percentEncode(key)} ${percentEncode(values.mkString(","))}"
      }
      .map((percentEncodePair _).tupled)
      .mkString(QUERY_PARAMS_SEPARATOR.toString)
  }

  /**
    * Construct a form-urlencoded document from the given name/parameter pair.
    */
  private def percentEncodePair(key: String, values: Seq[String]): String = {
    val encKey = percentEncode(key)
    val encVal =
      values.map(percentEncode).mkString(ENCODED_PARAM_VALUE_SEPARATOR)
    s"$encKey=$encVal"
  }

  /**
    * Encode value using URLEncoder.encode() but encode some characters differently to URLEncoder, to match OAuth1
    * and VisualVault.
    *
    * @param value String to be percent-encoded
    * @return encoded Encoded result string
    * @throws UnsupportedEncodingException if URLEncoder does not support UTF-8
    */
  private def percentEncode(value: String): String =
    URLEncoder
      .encode(value, "UTF-8")
      .replace("+", "%20")
      .replace("*", "%2A")
      .replace("%7E", "~")

}
