package io.toolsplus.atlassian.connect.jwt.scala.api

object Predef {

  /** Type indicating a raw JWT string.
    *
    * Prefer this type anywhere there is a raw JWT string over a plain
    * string type.
    */
  type RawJwt = String

}
