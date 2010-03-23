package de.sciss.scalainterpreter

import java.awt.{GraphicsEnvironment, EventQueue}
import javax.swing.{WindowConstants, SwingConstants, JSplitPane, JFrame}

object Main extends Runnable {
   def main( args: Array[ String ]) {
      EventQueue.invokeLater( this )
   }

   def run {
      val ip = new ScalaInterpreterPane
      val lp = new LogPane
      lp.init
      ip.out = Some( lp.writer )
      ip.init

      val frame = new JFrame( "Scala Interpreter" )
      val sp = new JSplitPane( SwingConstants.HORIZONTAL )
      sp.setTopComponent( ip )
      sp.setBottomComponent( lp )
      val cp = frame.getContentPane
      cp.add( sp )
      val b = GraphicsEnvironment.getLocalGraphicsEnvironment.getMaximumWindowBounds
      frame.setSize( b.width / 2, b.height * 5 / 6 )
      sp.setDividerLocation( b.height * 2 / 3 )
      frame.setLocationRelativeTo( null )
      frame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE )
      frame.setVisible( true )
   }
}