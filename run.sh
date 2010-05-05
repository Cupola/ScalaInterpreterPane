#!/bin/sh
java -cp target/scala_2.8.0.RC1/scalainterpreterpane_2.8.0.RC1-0.13.jar:lib/jsyntaxpane.jar:${SCALA_HOME}/lib/scala-library.jar:${SCALA_HOME}/lib/scala-compiler.jar de.sciss.scalainterpreter.Main
