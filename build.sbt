val commonSettings = Seq(
  organization := "io.toolsplus",
  scalaVersion := "2.11.8",
  resolvers ++= Seq(
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
  )
)

lazy val publishSettings = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/toolsplus/atlassian-jwt")),
  licenses := Seq(
    "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/toolsplus/atlassian-jwt"),
      "scm:git:git@github.com:toolsplus/atlassian-jwt.git"
    )
  ),
  developers := List(
    Developer("tbinna",
              "Tobias Binna",
              "tobias.binna@toolsplus.ch",
              url("https://twitter.com/tbinna"))
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false,
  publishTo := Some(
    Resolver.file("Unused transient repository", file("target/dummyrepo")))
)

def moduleSettings(projectName: String, moduleName: String) = {
  def toSnakeCase(s: String) = s.toLowerCase.replace(" ", "-")
  Seq(
    description := s"${toSnakeCase(projectName)}-${toSnakeCase(moduleName)}",
    name := s"$projectName $moduleName"
  )
}

val projectName = "Atlassian JWT"

lazy val `atlassian-jwt-root` = (project in file("."))
  .aggregate(
    `atlassian-jwt-api`,
    `atlassian-jwt-generators`,
    `atlassian-jwt-core`
  )
  .settings(commonSettings: _*)
  .settings(noPublishSettings)

lazy val `atlassian-jwt-api` = project
  .in(file("src/api"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(projectName, "API"))

lazy val `atlassian-jwt-generators` = project
  .in(file("src/generators"))
  .settings(libraryDependencies ++= Dependencies.generators)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(projectName, "Generators"))
  .dependsOn(`atlassian-jwt-api`)

lazy val `atlassian-jwt-core` = project
  .in(file("src/core"))
  .settings(libraryDependencies ++= Dependencies.core)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(projectName, "Core"))
  .dependsOn(`atlassian-jwt-api`)
  .dependsOn(`atlassian-jwt-generators` % "test")
