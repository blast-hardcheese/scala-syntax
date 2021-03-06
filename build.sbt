// use fork of paiges with custom line combinators.
// If these custom line combinators turn out to be useful, then we can try
// to merge them upstream.
lazy val Paiges = RootProject(
  uri(
    "git://github.com/olafurpg/paiges.git#114ec05b4a3099906c9159ccd1357f3b772b4f1d"
  )
)
lazy val paiges = ProjectRef(Paiges.build, "coreJVM")

lazy val metaV = "3.2.0"

lazy val format = project
  .settings(
    moduleName := "scala-format",
    assemblyJarName in assembly := "scalafmt.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("org", "typelevel", "paiges", xs @ _*) =>
        MergeStrategy.first
      case x =>
        assemblyMergeStrategy.in(assembly).value(x)
    },
    mainClass.in(assembly) := Some("org.scalafmt.Format"),
    libraryDependencies ++= List(
      "com.lihaoyi" %% "pprint" % "0.5.2", // for debugging
      "org.scalameta" %% "scalameta" % metaV,
      "org.scalameta" %% "contrib" % metaV
    )
  )
  .dependsOn(paiges)

// IntegrationTest configuration is not worth the complexity, reusing code across
// configuration is annoying. Easier to create more projects.
lazy val testsShared = project
  .in(file("tests/shared"))
  .settings(
    libraryDependencies ++= List(
      "ch.epfl.scala" %% "scalafix-diff" % "0.5.1",
      "org.scalameta" %% "testkit" % metaV,
      "com.lihaoyi" %% "utest" % "0.6.3",
      "org.scalatest" %% "scalatest" % "3.0.4",
      "com.lihaoyi" %% "fansi" % "0.2.5" % Test
    )
  )
  .dependsOn(format)

lazy val unit = project
  .in(file("tests/unit"))
  .dependsOn(testsShared)

lazy val slow = project
  .in(file("tests/slow"))
  .settings(
    libraryDependencies += "me.tongfei" % "progressbar" % "0.5.5",
    fork in (Test, test) := true,
    fork in (Test, testOnly) := true,
    fork in (Test, testQuick) := true,
    cancelable in Global := true,
    javaOptions in (Test, test) ++= {
      val mem =
        if (sys.env.get("CI").isDefined) "4"
        else sys.env.get("SLOWMEM").getOrElse("20")

      Seq(
        "-Xss20m",
        "-Xms4G",
        s"-Xmx${mem}G",
        "-XX:ReservedCodeCacheSize=1024m",
        "-XX:+TieredCompilation",
        "-XX:+CMSClassUnloadingEnabled"
      )
    },
    javaOptions in (Test, testOnly) ++= (javaOptions in (Test, test)).value,
    javaOptions in (Test, testQuick) ++= (javaOptions in (Test, test)).value
  )
  .dependsOn(testsShared)
