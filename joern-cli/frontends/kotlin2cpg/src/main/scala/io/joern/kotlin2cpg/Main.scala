package io.joern.kotlin2cpg

import io.joern.kotlin2cpg.Frontend._
import io.joern.x2cpg.{X2CpgConfig, X2CpgMain}
import scopt.OParser

case class InferenceJarPath(path: String, isResource: Boolean)

final case class Config(
  inputPaths: Set[String] = Set.empty,
  outputPath: String = X2CpgConfig.defaultOutputPath,
  classpath: Set[String] = Set.empty,
  withStdlibJarsInClassPath: Boolean = true,
  withAndroidJarsInClassPath: Boolean = true,
  withMiscJarsInClassPath: Boolean = true // TODO: remove
) extends X2CpgConfig[Config] {

  override def withAdditionalInputPath(inputPath: String): Config =
    copy(inputPaths = inputPaths + inputPath)

  override def withOutputPath(x: String): Config = copy(outputPath = x)
}

private object Frontend {
  implicit val defaultConfig: Config = Config()

  val cmdLineParser: OParser[Unit, Config] = {
    val builder = OParser.builder[Config]
    import builder.programName
    import builder.opt
    OParser.sequence(
      programName("kotlin2cpg"),
      opt[String]("classpath")
        .unbounded()
        .text("directories to be searched for type resolution jars")
        .action((incl, c) => c.copy(classpath = c.classpath + incl)),
      opt[Unit]("with-stdlib-jars")
        .text("adds local versions of Kotlin stdlib jars to classpath")
        .action((_, c) => c.copy(withStdlibJarsInClassPath = true)),
      opt[Unit]("with-android-jars")
        .text("adds local versions of Android jars to classpath")
        .action((_, c) => c.copy(withStdlibJarsInClassPath = true)),
      opt[Unit]("with-misc-jars")
        .text("adds local versions of various common library jars to classpath")
        .action((_, c) => c.copy(withStdlibJarsInClassPath = true))
    )
  }
}

object Main extends X2CpgMain(cmdLineParser, new Kotlin2Cpg()) {
  def run(config: Config, kotlin2cpg: Kotlin2Cpg): Unit = {
    kotlin2cpg.run(config)
  }
}
