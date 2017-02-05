name := "Akka-Streamy"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.4.16"

  Seq(
    akka                  %%  "akka-stream"      % akkaV,
    akka                  %%  "akka-slf4j"       % akkaV,
    "com.typesafe"        %   "config"           % "1.3.1",
    "ch.qos.logback"      %   "logback-classic"  % "1.1.9",
    "org.codehaus.groovy" %   "groovy"           % "2.4.8"
  )
}

