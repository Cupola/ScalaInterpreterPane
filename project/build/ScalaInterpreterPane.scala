import sbt._

class ScalaInterpreterPaneProject( info: ProjectInfo ) extends DefaultProject( info ) { 
   val dep = "jsyntaxpane" % "jsyntaxpane" % "0.9.5-b29" from "http://jsyntaxpane.googlecode.com/files/jsyntaxpane-0.9.5-b29.jar"
}
