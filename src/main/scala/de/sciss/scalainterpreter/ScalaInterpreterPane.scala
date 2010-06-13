/*
 *  ScalaInterpreterPane.scala
 *  (ScalaInterpreterPane)
 *
 *  Copyright (c) 2010 Hanns Holger Rutz. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *	  Below is a copy of the GNU Lesser General Public License
 *
 *	  For further information, please contact Hanns Holger Rutz at
 *	  contact@sciss.de
 */

package de.sciss.scalainterpreter

import java.awt.{ BorderLayout, Dimension, Font, GraphicsEnvironment, Toolkit }
import javax.swing.{ AbstractAction, Box, JComponent, JEditorPane, JLabel, JPanel, JProgressBar, JScrollPane,
   KeyStroke, OverlayLayout, ScrollPaneConstants, SwingWorker }
import ScrollPaneConstants._

import jsyntaxpane.{ DefaultSyntaxKit, SyntaxDocument }
import tools.nsc.{ ConsoleWriter, Interpreter, InterpreterResults => IR, NewLinePrintWriter, Settings }
import java.io.{ File, PrintWriter, Writer }
import java.awt.event.{InputEvent, ActionEvent, KeyEvent, KeyListener}

object ScalaInterpreterPane {
   val version = 0.15
}

/**
 *    @version 0.14, 11-Jun-10
 */
class ScalaInterpreterPane
extends JPanel with CustomizableFont {
   pane =>

   @volatile private var interpreterVar: Option[ Interpreter ] = None
   private var docVar: Option[ SyntaxDocument ] = None

   // subclasses may override this
   var executeKeyStroke = {
      val ms = Toolkit.getDefaultToolkit.getMenuShortcutKeyMask
      KeyStroke.getKeyStroke( KeyEvent.VK_E, if( ms == InputEvent.CTRL_MASK ) ms | InputEvent.SHIFT_MASK else ms )
   }

   // subclasses may override this
   var initialCode: Option[ String ] = None

   // subclasses may override this
   var out: Option[ Writer ] = None

   var customKeyMapActions:    Map[ KeyStroke, Function0[ Unit ]] = Map.empty
   var customKeyProcessAction: Option[ Function1[ KeyEvent, KeyEvent ]] = None
   var initialText = """// Type Scala code here.
// Press '""" + KeyEvent.getKeyModifiersText( executeKeyStroke.getModifiers() ) + " + " +
      KeyEvent.getKeyText( executeKeyStroke.getKeyCode() ) + """' to execute selected text
// or current line.
"""

   private val ggStatus = new JLabel( "Initializing..." )

   protected val editorPane      = new JEditorPane() {
      override protected def processKeyEvent( e: KeyEvent ) {
         super.processKeyEvent( customKeyProcessAction.map( fun => {
            fun.apply( e )
         }) getOrElse e )
      }
   }
   private val progressPane      = new JPanel()
   private val ggProgress        = new JProgressBar()
   private val ggProgressInvis   = new JComponent {
      override def getMinimumSize   = ggProgress.getMinimumSize
      override def getPreferredSize = ggProgress.getPreferredSize
      override def getMaximumSize   = ggProgress.getMaximumSize
   }

   def interpreter: Option[ Interpreter ] = interpreterVar
   def doc: Option[ SyntaxDocument ] = docVar

   def init {
      // spawn interpreter creation
      (new SwingWorker[ Unit, Unit ] {
         override def doInBackground {
            val settings = {
               val set = new Settings()
               set.classpath.value += File.pathSeparator + System.getProperty( "java.class.path" )
               set
            }

            DefaultSyntaxKit.initKit()
            val in = new Interpreter( settings, new NewLinePrintWriter( out getOrElse (new ConsoleWriter), true )) {
               override protected def parentClassLoader = pane.getClass.getClassLoader
            }
            in.setContextClassLoader()
            bindingsCreator.foreach( _.apply( in ))
            initialCode.foreach( code => in.interpret( code ))
            interpreterVar = Some( in )
         }

         override protected def done {
            ggProgressInvis.setVisible( true )
            ggProgress.setVisible( false )
            editorPane.setContentType( "text/scala" )
            editorPane.setText( initialText )
            docVar = editorPane.getDocument() match {
               case sdoc: SyntaxDocument => Some( sdoc )
               case _ => None
            }

            editorPane.setFont( createFont )
            editorPane.setEnabled( true )
            editorPane.requestFocus
            status( "Ready." )
         }
      }).execute()

      val ggScroll   = new JScrollPane( editorPane, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS  )
//      ggScroll.putClientProperty( "JComponent.sizeVariant", "small" )

      ggProgress.putClientProperty( "JProgressBar.style", "circular" )
      ggProgress.setIndeterminate( true )
      ggProgressInvis.setVisible( false )
      editorPane.setEnabled( false )

      val imap = editorPane.getInputMap( JComponent.WHEN_FOCUSED )
      val amap = editorPane.getActionMap()
      imap.put( executeKeyStroke, "de.sciss.exec" )
      amap.put( "de.sciss.exec", new AbstractAction {
         def actionPerformed( e: ActionEvent ) {
            var txt = editorPane.getSelectedText
            if( txt == null ) {
               docVar.foreach( d => txt = d.getLineAt( editorPane.getCaretPosition ))
            }
            if( txt != null ) interpret( txt )
         }
      })
      customKeyMapActions.iterator.zipWithIndex.foreach( tup => {
         val (spec, idx) = tup
         val name = "de.sciss.user" + idx
         imap.put( spec._1, name )
         amap.put( name, new AbstractAction {
            def actionPerformed( e: ActionEvent ) {
               spec._2.apply()
            }
         })
      })

      progressPane.setLayout( new OverlayLayout( progressPane ))
      progressPane.add( ggProgress )
      progressPane.add( ggProgressInvis )
      ggStatus.putClientProperty( "JComponent.sizeVariant", "small" )
      val statusPane = Box.createHorizontalBox()
      statusPane.add( Box.createHorizontalStrut( 4 ))
      statusPane.add( progressPane )
      statusPane.add( Box.createHorizontalStrut( 4 ))
      statusPane.add( ggStatus )

//      setLayout( new BorderLayout )
      setLayout( new BorderLayout() )
      add( ggScroll, BorderLayout.CENTER )
      add( statusPane, BorderLayout.SOUTH )
   }

   /**
    *    Subclasses may override this to
    *    create initial bindings for the interpreter.
    *    Note that this is not necessarily executed
    *    on the event thread.
    */
   var bindingsCreator: Option[ Function1[ Interpreter, Unit ]] = None

   protected def status( s: String ) {
      ggStatus.setText( s )
   }

   def interpret( code: String ) {
      interpreterVar.foreach( in => {
         status( null )
         try { in.interpret( code ) match {
            case IR.Error       => status( "! Error !" )
            case IR.Success     => status( "Ok. <" + in.mostRecentVar + ">" )
            case IR.Incomplete  => status( "! Code incomplete !" )
            case _ =>
         }}
         catch { case e => e.printStackTrace() }
      })
   }
}
