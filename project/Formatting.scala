import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

object Formatting {

  /**
   * Scalariform settings. These are the settings required to make compatible with the Scala Style guide and ADE.
   */
  lazy val formattingSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(CompactControlReadability, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  )

}