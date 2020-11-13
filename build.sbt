import Settings._
import sbt.Keys.scalaVersion

lazy val commonSettings = Seq(
  organization in ThisBuild := "wetekio",
  version := "1.0",
  scalaVersion := Dependencies.scalaVersion
)

lazy val global = project
  .in(file("."))
  .settings(
    name := "Copernico"
  )
  .settings(commonSettings)
  .settings(modulesSettings)
  .settings(mainSettings)
  .settings(testSettings)
  .settings(scalaFmtSettings)
  .settings(testCoverageSettings)
  .settings(CommandAliases.aliases)
  .aggregate(
    writeside
  )

lazy val writeside = project
  .settings(commonSettings)
  .settings(modulesSettings)
  .settings(
    name := "writeside"
  )
