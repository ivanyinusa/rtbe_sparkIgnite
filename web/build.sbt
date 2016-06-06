//import akka.{ AkkaBuild, Formatting, OSGi, Dependencies, Version }
import nb_rtbe.{ AkkaBuild, Dependencies}

AkkaBuild.defaultSettings
//Formatting.formatSettings
//OSGi.actor
Dependencies.web
//Version.versionSettings

//enablePlugins(spray.boilerplate.BoilerplatePlugin)

fork in run := true
