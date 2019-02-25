name := "bantu"

version := "0.1"

scalaVersion := "2.12.6"

val akkaVersion      = "2.5.19"
val akkaHttpVersion  = "10.1.7"
val scalaTestVersion = "3.0.5"


lazy val bantu = (project in file("."))
  .aggregate(core,service,web)
lazy val core = (project in file("core")).
  settings(
    libraryDependencies ++= Seq (
      "com.typesafe.akka"   %% "akka-actor"      % akkaVersion,
      "com.typesafe.akka"   %% "akka-slf4j"      % akkaVersion,
      "com.typesafe.akka"   %% "akka-http"       % akkaHttpVersion,
      "com.typesafe.akka"   %% "akka-stream"     % akkaVersion,
      "ch.qos.logback"      %  "logback-classic" % "1.2.3",
      "commons-daemon"      %  "commons-daemon"  % "1.1.0",
      "com.github.mauricio" %% "mysql-async"     % "0.2.21",
      "com.typesafe.akka"   %% "akka-testkit"    % akkaVersion      % Test,
      "org.scalatest"       %% "scalatest"       % scalaTestVersion % Test
    )
  )

lazy val service = (project in file("service")).
  settings(libraryDependencies ++= Seq(
    "com.typesafe.akka"    %% "akka-testkit"     % akkaVersion      % Test,
    "org.scalatest"        %% "scalatest"        % scalaTestVersion % Test
    )
  ).dependsOn(core)

lazy val web = (project in file("web")).
  settings(libraryDependencies ++= Seq(
    "com.typesafe.akka"   %% "akka-testkit"      % akkaVersion      % Test,
    "org.scalatest"       %%  "scalatest"        % scalaTestVersion % Test,
    "com.typesafe.akka"   %% "akka-http-testkit" % akkaHttpVersion  % Test
    )
  ).dependsOn(core, service)
