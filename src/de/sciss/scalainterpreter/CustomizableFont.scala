package de.sciss.scalainterpreter

import java.awt.{ Font, GraphicsEnvironment }

trait CustomizableFont {
   var preferredFonts = List( "DejaVu Sans Mono" -> 12, "Bitstream Vera Sans Mono" -> 12, "Menlo" -> 12,
      "Monaco" -> 12, "Anonymous Pro" -> 12 )

   protected def createFont : Font = {
//      val osName                 = System.getProperty( "os.name" )
//      val isMac                  = osName.startsWith( "Mac OS" )
      val allFontNames           = GraphicsEnvironment.getLocalGraphicsEnvironment.getAvailableFontFamilyNames
      val (fontName, fontSize)   = preferredFonts.find( spec => allFontNames.contains( spec._1 ))
         .getOrElse( "Monospaced" -> 12 )

      new Font( fontName, Font.PLAIN, /*if( isMac )*/ fontSize /*else fontSize * 3/4*/ )
   }
}