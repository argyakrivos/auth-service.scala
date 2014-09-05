// This is needed due to a bug in the scala reflection that makes tests intermittently fail.
// See: https://issues.scala-lang.org/browse/SI-6240
val testSettings = Seq(
  parallelExecution in Test := false
)

val buildSettings = Seq(
  name := "auth-service-public",
  organization := "com.blinkbox.books.zuul",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion := "2.11.2",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val akkaV = "2.3.5"
    val sprayV = "1.3.1"
    val json4sV = "3.2.10"
    Seq(
      "io.spray"                  %%  "spray-client"          % sprayV,
      "org.json4s"                %%  "json4s-native"         % json4sV, // for swagger :-/
      "com.typesafe.akka"         %%  "akka-slf4j"            % akkaV,
      "com.zaxxer"                %   "HikariCP-java6"        % "2.0.1",
      "commons-lang"              %   "commons-lang"          % "2.6",
      "com.lambdaworks"           %   "scrypt"                % "1.4.0",
      "com.blinkbox.books"        %%  "common-config"         % "1.1.0",
      "com.blinkbox.books"        %%  "common-slick"          % "0.1.1",
      "com.blinkbox.books"        %%  "common-spray"          % "0.16.0",
      "com.blinkbox.books"        %%  "common-spray-auth"     % "0.5.1",
      "com.blinkbox.books.hermes" %%  "rabbitmq-ha"           % "6.0.4",
      "com.blinkbox.books.hermes" %%  "message-schemas"       % "0.6.2",
      "com.blinkbox.books"        %%  "common-scala-test"     % "0.3.0"   % "test",
      "io.spray"                  %%  "spray-testkit"         % sprayV    % "test",
      "xmlunit"                   %   "xmlunit"               % "1.5"     % "test"
    )
  }
)

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*).
  settings(testSettings: _*)
