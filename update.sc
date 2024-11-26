import io.joern.scanners._
import io.shiftleft.semanticcpg.language._
import io.shiftleft.codepropertygraph.generated._
import io.joern.console._
import io.joern.macros.QueryMacros._
import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.joern.dataflowengineoss.semanticsloader.Semantics
import scala.util.control.Breaks._

@main def main(inputPath: String="/Users/phli/llm4pa/joern/benchmark/changeset_lib/node_modules/changeset/", outputPath: String = "") = {
    importCode(inputPath)


  def sinks = cpg.call.where(
      _.name(Operators.assignment)
      )
      .argument(1)
      .isCall
      .name(Operators.indexAccess)
      .argument(2)
      .toList

  def sources = cpg.assignment
    .where(_.target.code(".*(module\\.exports|exports).*")) // Match exports
    .source
    .flatMap {
      case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
      // Handle named functions
      cpg.method.fullNameExact(id.typeFullName).parameter
      case call: io.shiftleft.codepropertygraph.generated.nodes.Call =>
      // Handle inline/anonymous functions
      call.method.parameter
      case _ =>
      List.empty
    }
    .distinct
    .toList



    val vuls = sinks.filter { sink =>
    sink.reachableBy(sources).nonEmpty
    }


    val result = vuls.map { case v =>
        val fileName = v.file.name.headOption.getOrElse("UNKNOWN-FILES")
        val lineNumber = v.lineNumber.getOrElse(-99)
        println(s"$fileName:$lineNumber")
        s"$fileName:$lineNumber"
    }.distinct


    val outputFile = if (outputPath.isEmpty) {
    val outPath = os.Path(inputPath, base = os.pwd) / os.up / "bug.txt"
    outPath
    } else {
    os.Path(outputPath, base = os.pwd)
    }

    println(s"Writing output to file: ${outputFile}")
    
    os.write.over(outputFile, result.mkString("\n"))
}
