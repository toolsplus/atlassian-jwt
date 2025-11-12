import ReleaseTransformations.*
import xerial.sbt.Sonatype.sonatypeCentralHost

val commonSettings = Seq(
  organization := "io.toolsplus",
  scalaVersion := "3.3.6",
  versionScheme := Some("early-semver"),
  resolvers ++= Seq(
    Resolver.typesafeRepo("releases"),
    Resolver
      .sonatypeCentralRepo("releases")
  )
)

lazy val publishSettings = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/toolsplus/atlassian-jwt")),
  licenses := Seq(
    "Apache 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")
  ),
  publishMavenStyle := true,
  releaseIgnoreUntrackedFiles := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  ThisBuild / publishTo := sonatypePublishToBundle.value,
  ThisBuild / sonatypeCredentialHost := sonatypeCentralHost,
  autoAPIMappings := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/toolsplus/atlassian-jwt"),
      "scm:git:git@github.com:toolsplus/atlassian-jwt.git"
    )
  ),
  developers := List(
    Developer(
      "tbinna",
      "Tobias Binna",
      "tobias.binna@toolsplus.io",
      url("https://twitter.com/tbinna")
    )
  )
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publishTo := Some(
    Resolver.file("Unused transient repository", file("target/dummyrepo"))
  )
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

def moduleSettings(project: Project) = {
  Seq(
    description := project.id.split("-").map(_.capitalize).mkString(" "),
    name := project.id
  )
}

lazy val `atlassian-jwt` = project
  .in(file("."))
  .aggregate(
    `atlassian-jwt-api`,
    `atlassian-jwt-generators`,
    `atlassian-jwt-core`
  )
  .settings(commonSettings)
  .settings(noPublishSettings)

lazy val `atlassian-jwt-api` = project
  .in(file("modules/api"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(project))

lazy val `atlassian-jwt-generators` = project
  .in(file("modules/generators"))
  .settings(libraryDependencies ++= Dependencies.generators)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(project))
  .dependsOn(`atlassian-jwt-api`)

lazy val `atlassian-jwt-core` = project
  .in(file("modules/core"))
  .settings(libraryDependencies ++= Dependencies.core)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(project))
  .dependsOn(`atlassian-jwt-api`)
  .dependsOn(`atlassian-jwt-generators` % "test")
