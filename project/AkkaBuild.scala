/**
 *  Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka

import sbt._
import sbt.Keys._


object AkkaBuild extends Build {
  System.setProperty("akka.mode", "test") // Is there better place for this?


  val requestedScalaVersion = System.getProperty("akka.scalaVersion", "2.10.2")

  lazy val buildSettings = Seq(
    organization := "com.typesafe.akka",
    version      := "2.3-SNAPSHOT",
    // Also change ScalaVersion in akka-sbt-plugin/sample/project/Build.scala
    scalaVersion := requestedScalaVersion,
    scalaBinaryVersion <<= (scalaVersion, scalaBinaryVersion)((v, bv) => System.getProperty("akka.scalaBinaryVersion", if (v contains "-") v else bv))
  )

  lazy val akka = Project(
    id = "akka",
    base = file("."),
    settings = parentSettings ++ Seq(
      
    ),
    aggregate = Seq(actor, testkit, actorTests)
  )



  lazy val actor = Project(
    id = "akka-actor",
    base = file("akka-actor"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.actor
    )
  )


  lazy val testkit = Project(
    id = "akka-testkit",
    base = file("akka-testkit"),
    dependencies = Seq(actor),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.testkit
    )
  )

  lazy val actorTests = Project(
    id = "akka-actor-tests",
    base = file("akka-actor-tests"),
    dependencies = Seq(testkit % "compile;test->test"),
    settings = defaultSettings ++ Seq(
      libraryDependencies ++= Dependencies.actorTests
    )
  )



  // Settings

  override lazy val settings =
    super.settings ++
    buildSettings ++
    Seq(
      shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
    )

  lazy val baseSettings = Defaults.defaultSettings

  lazy val parentSettings = baseSettings ++ Seq(
    publishArtifact := false
  )


  lazy val defaultSettings = baseSettings ++ Seq(
    // compile options
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6", "-Xlint:unchecked", "-Xlint:deprecation"),

    // if changing this between binary and full, also change at the bottom of akka-sbt-plugin/sample/project/Build.scala
    crossVersion := CrossVersion.binary,

    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet


  )

}

// Dependencies

object Dependencies {

  object Compile {
    // Compile
    val camelCore     = "org.apache.camel"            % "camel-core"                   % "2.10.3" exclude("org.slf4j", "slf4j-api") // ApacheV2

    val config        = "com.typesafe"                % "config"                       % "1.0.2"       // ApacheV2
    val netty         = "io.netty"                    % "netty"                        % "3.6.6.Final" // ApacheV2
    val protobuf      = "com.google.protobuf"         % "protobuf-java"                % "2.5.0"       // New BSD
    val scalaStm      = "org.scala-stm"              %% "scala-stm"                    % "0.7"         // Modified BSD (Scala)
    val scalaBuffRuntime = "net.sandrogrzicic"       %% "scalabuff-runtime"            % "1.2.0"       // ApacheV2

    val slf4jApi      = "org.slf4j"                   % "slf4j-api"                    % "1.7.2"       // MIT
    val zeroMQClient  = "org.zeromq"                 %% "zeromq-scala-binding"         % "0.0.7"       // ApacheV2
    val uncommonsMath = "org.uncommons.maths"         % "uncommons-maths"              % "1.2.2a" exclude("jfree", "jcommon") exclude("jfree", "jfreechart")      // ApacheV2
    val ariesBlueprint = "org.apache.aries.blueprint" % "org.apache.aries.blueprint"   % "1.1.0"       // ApacheV2
    val osgiCore      = "org.osgi"                    % "org.osgi.core"                % "4.2.0"       // ApacheV2
    val osgiCompendium= "org.osgi"                    % "org.osgi.compendium"          % "4.2.0"       // ApacheV2
    val levelDB      = "org.iq80.leveldb"            % "leveldb"                      % "0.5"         // ApacheV2

    // Camel Sample
    val camelJetty  = "org.apache.camel"              % "camel-jetty"                  % camelCore.revision // ApacheV2

    // Cluster Sample
    val sigar       = "org.fusesource"                   % "sigar"                        % "1.6.4"            // ApacheV2

    // Compiler plugins
    val genjavadoc    = compilerPlugin("com.typesafe.genjavadoc" %% "genjavadoc-plugin" % "0.5" cross CrossVersion.full) // ApacheV2

    // Test

    object Test {
      val commonsMath  = "org.apache.commons"          % "commons-math"                 % "2.1"              % "test" // ApacheV2
      val commonsIo    = "commons-io"                  % "commons-io"                   % "2.0.1"            % "test" // ApacheV2
      val commonsCodec = "commons-codec"               % "commons-codec"                % "1.7"              % "test" // ApacheV2
      val junit        = "junit"                       % "junit"                        % "4.10"             % "test" // Common Public License 1.0
      val logback      = "ch.qos.logback"              % "logback-classic"              % "1.0.7"            % "test" // EPL 1.0 / LGPL 2.1
      val mockito      = "org.mockito"                 % "mockito-all"                  % "1.8.1"            % "test" // MIT
      // changing the scalatest dependency must be reflected in akka-docs/rst/dev/multi-jvm-testing.rst
      val scalatest    = "org.scalatest"              %% "scalatest"                    % "1.9.2-SNAP2"      % "test" // ApacheV2
      val scalacheck   = "org.scalacheck"             %% "scalacheck"                   % "1.10.0"           % "test" // New BSD
      val ariesProxy   = "org.apache.aries.proxy"      % "org.apache.aries.proxy.impl"  % "1.0.1"              % "test" // ApacheV2
      val pojosr       = "com.googlecode.pojosr"       % "de.kalpatec.pojosr.framework" % "0.1.4"            % "test" // ApacheV2
      val tinybundles  = "org.ops4j.pax.tinybundles"   % "tinybundles"                  % "1.0.0"            % "test" // ApacheV2
      val log4j        = "log4j"                       % "log4j"                        % "1.2.14"           % "test" // ApacheV2
      val junitIntf    = "com.novocode"                % "junit-interface"              % "0.8"              % "test" // MIT
    }
  }

  import Compile._

  val actor = Seq(config)

  val testkit = Seq(Test.junit, Test.scalatest)

  val actorTests = Seq(Test.junit, Test.scalatest, Test.commonsCodec, Test.commonsMath, Test.mockito, Test.scalacheck, protobuf, Test.junitIntf)

  val remote = Seq(netty, protobuf, uncommonsMath, Test.junit, Test.scalatest)

  val remoteTests = Seq(Test.junit, Test.scalatest)

  val cluster = Seq(Test.junit, Test.scalatest)

  val slf4j = Seq(slf4jApi, Test.logback)

  val agent = Seq(scalaStm, Test.scalatest, Test.junit)

  val transactor = Seq(scalaStm, Test.scalatest, Test.junit)

  val persistence = Seq(levelDB, Test.scalatest, Test.junit, Test.commonsIo)

  val mailboxes = Seq(Test.scalatest, Test.junit)

  val fileMailbox = Seq(Test.commonsIo, Test.scalatest, Test.junit)

  val kernel = Seq(Test.scalatest, Test.junit)

  val camel = Seq(camelCore, Test.scalatest, Test.junit, Test.mockito, Test.logback, Test.commonsIo, Test.junitIntf)

  val camelSample = Seq(camelJetty)

  val osgi = Seq(osgiCore, osgiCompendium, Test.logback, Test.commonsIo, Test.pojosr, Test.tinybundles, Test.scalatest, Test.junit)

  val osgiDiningHakkerSampleCore = Seq(config, osgiCore, osgiCompendium)

  val osgiDiningHakkerSampleCommand = Seq(osgiCore, osgiCompendium)

  val uncommons = Seq(uncommonsMath)

  val scalaBuff = Seq(scalaBuffRuntime)

  val osgiAries = Seq(osgiCore, osgiCompendium, ariesBlueprint, Test.ariesProxy)

  val docs = Seq(Test.scalatest, Test.junit, Test.junitIntf)

  val zeroMQ = Seq(protobuf, zeroMQClient, Test.scalatest, Test.junit)

  val clusterSample = Seq(Test.scalatest, sigar)

  val contrib = Seq(Test.junitIntf)

  val multiNodeSample = Seq(Test.scalatest)
}
