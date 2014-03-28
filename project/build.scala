import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import de.oakgrove.SbtBrand.{BrandKeys, brandSettings, Val, BrandObject}
import de.oakgrove.SbtHgId.{HgIdKeys, hgIdSettings}

object SiliconBuild extends Build {

  /* Base settings */

  lazy val baseSettings = (
       Defaults.defaultSettings
	  ++ hgIdSettings
	  ++ brandSettings
    ++ Seq(
          organization := "semper",
          version := "0.1-SNAPSHOT",
          scalaVersion := "2.10.3",
          scalacOptions in Compile ++= Seq(
            "-deprecation",
            "-unchecked",
            "-feature"
            /*"-Xfatal-warnings"*/),
          resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"))

  /* Projects */

	lazy val silicon = {
    var p = Project(
      id = "silicon",
      base = file("."),
      settings = (
           baseSettings
			  ++ assemblySettings
        ++ Seq(
              name := "Silicon",
              mainClass in assembly := Some("semper.silicon.Silicon"),
              jarName in assembly := "silicon.jar",
              traceLevel := 10,
              maxErrors := 6,
              fork := true,
                /* Fork Silicon when run and tested. Avoids problems with file
                 * handlers on Windows 7 that remain open until Sbt is closed,
                 * which makes it very annoying to work on test files.
                 *
                 * There have been reports about problems with forking. If you
                 * experience strange problems, disable forking and try again.
                 *
                 * Malte 2013-11-18: Jenkins failed with
                 * "OutOfMemoryError: unable to create new native thread".
                 * Reducing the stack size from 256M to 128M seems to resolve
                 * the problem and Silicon seems to be fine with less stack.
                 * Not sure what to do if Silicon really required so much
                 * stack at some point.
                 */
               javaOptions in run += "-Xss128M",
               javaOptions in Test += "-Xss128M",
                /* Options passed to JVMs forked by test-related Sbt command.
                 * See http://www.scala-sbt.org/0.12.4/docs/Detailed-Topics/Forking.html
                 * In contrast to what the documentation states, it seemed
                 * that neither were the options passed to Sbt's JVM forwarded
                 * to forked JVMs, nor did "javaOptions in (Test,run)"
                 * work for me (Malte, using Sbt 0.12.4).
                 * You can inspect the settings in effect using via
                 * "show javaOptions" on the Sbt console.
                 */
              libraryDependencies ++= externalDep,
              BrandKeys.dataPackage := "semper.silicon",
              BrandKeys.dataObject := "brandingData",
              BrandKeys.data += Val("buildDate", new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date)),
              BrandKeys.data <+= scalaVersion(Val("scalaVersion", _)),
              BrandKeys.data <+= sbtBinaryVersion(Val("sbtBinaryVersion", _)),
              BrandKeys.data <+= sbtVersion(Val("sbtVersion", _)),
              BrandKeys.data <+= name(Val("sbtProjectName", _)),
              BrandKeys.data <+= version(Val("sbtProjectVersion", _)),
              BrandKeys.data <+= HgIdKeys.projectId { hgid =>
                BrandObject("hgid",
                            """val version = "%s"
                               val id = "%s"
                               val branch = "%s"
                               val tags = "%s"
                            """.format(hgid.version, hgid.id, hgid.branch, hgid.tags))
              },
              sourceGenerators in Compile <+= BrandKeys.generateDataFile)
              /* ,addCommandAlias("tn", "test-only -- -n ") // requires sbt 0.13.x */)
    ).dependsOn(common)

    for (dep <- internalDep) {
      p = p.dependsOn(dep)
    }

    p.aggregate(common)
  }

  lazy val common = Project(
    id = "common",
    base = file("common"),
    settings = (
      baseSettings
        ++ Seq(name := "Silicon-Common",
               javacOptions ++= Seq("-source", "1.7", "-target", "1.7"))))

  // On the build-server, we cannot have all project in the same directory, and thus we use the publish-local mechanism for dependencies.
  def isBuildServer = sys.env.contains("BUILD_TAG") // should only be defined on the build server

  def internalDep = if (isBuildServer) Nil else Seq(dependencies.silSrc % "compile->compile;test->test")

  def externalDep = {
    dependencies.logging ++ Seq(dependencies.scallop) ++
    (if (isBuildServer) Seq(dependencies.sil % "compile->compile;test->test") else Nil)
  }

  /* Dependencies */

  object dependencies {
    lazy val logging = Seq(slf4s, slf4j)
    lazy val slf4s = "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7"
    lazy val slf4j = "org.slf4j" % "slf4j-log4j12" %	"1.6.4"

    lazy val scallop = "org.rogach" %% "scallop" % "0.9.4"

    lazy val sil = "semper" %% "sil" %  "0.1-SNAPSHOT"
    lazy val silSrc = RootProject(new java.io.File("../sil"))
  }
}
