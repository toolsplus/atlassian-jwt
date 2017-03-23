package io.toolsplus.atlassian.jwt.api

/**
  * Trait to identify components in a request required to create a string that includes information
  * about the request in a standardized (canonical) format.
  */
trait CanonicalHttpRequest {

  /**
    * HTTP method (e.g. "GET", "POST" etc).
    *
    * @return the HTTP method in upper-case.
    */
  def method: String

  /**
    * The part of an absolute URL that is after the protocol, server, port and context (i.e. base) path.
    * E.g. "/the_path" in "http://server:80/context/the_path?param=value" where "/context" is the context path.
    *
    * @return the relative path with no case manipulation.
    */
  def relativePath: String

  /**
    * The Map of parameter-name to parameter-values.
    *
    * @return Map representing { parameter-name-1 to { parameter-value-1, parameter-value-2 ... }, parameter-name-2
    *         to { ... }, ... }
    */
  def parameterMap: Map[String, Seq[String]]

}
