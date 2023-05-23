import sbt.*

object Dependencies {
  val catsEffectVersion = "3.4.11"
  val circeVersion = "0.14.5"

  val h2DatabaseVersion = "1.4.+"
  val hikariConnectionPoolVersion = "5.0.1"
  val scalikeJdbcVersion = "4.0.+"

  val scalaTestVersion = "3.2.15"
  val scalaTestPlusMockitoVersion = "3.2.10.0"
  val scalaTestPlusVersion = "3.2.11.0"

  private val catsEffect = Seq(
    "org.typelevel" %% "cats-effect" % catsEffectVersion
  )

  private val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
  )

  private val scalikeJdbc: Seq[ModuleID] = Seq(
    "com.h2database"  %  "h2" % h2DatabaseVersion,
    "com.zaxxer" % "HikariCP" % hikariConnectionPoolVersion,
    "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
    "org.scalikejdbc" %% "scalikejdbc-config" % scalikeJdbcVersion,
  )

  private val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "org.scalatestplus" %% "mockito-3-4" % scalaTestPlusMockitoVersion % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % scalaTestPlusVersion % Test,
  )

  val commonDependencies: Seq[ModuleID] =
    catsEffect ++
      circe ++
      scalaTest ++
      scalikeJdbc

}
