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

  // def sinks = cpg.call
  //  .where(_.name(Operators.assignment))
  //  .argument(1)
  //  .isCall
  //  .name(Operators.indexAccess)
  //  .argument(2)
  //  .toList


  // def sources = cpg.assignment
  //   .where(_.target.code(".*(module\\.exports|exports).*")) // Match exports
  //   .source
  //   .flatMap {
  //     case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
  //     // Handle named functions
  //     cpg.method.fullNameExact(id.typeFullName).parameter
  //     case call: io.shiftleft.codepropertygraph.generated.nodes.Call =>
  //     // Handle inline/anonymous functions
  //     call.method.parameter
  //     case _ =>
  //     List.empty
  //   }
  //   .distinct
  //   .toList

// def sources = cpg.assignment
//   .where(_.target.code(".*(module\\.exports|exports).*")) // Match exports
//   .source
//   .flatMap {
//     case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
//       // Handle named functions
//       cpg.method.fullNameExact(id.typeFullName).parameter
//     case call: io.shiftleft.codepropertygraph.generated.nodes.Call =>
//       // Handle inline/anonymous functions via calls
//       call.method.parameter
//     case methodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef =>
//       // Resolve MethodRef to Method and get parameters
//       cpg.method.fullNameExact(methodRef.methodFullName).parameter
//         .filterNot(_.name == "this") // Exclude implicit parameters like `this`
//     case _ =>
//       List.empty
//   }
//   .distinct
//   .toList

// def sources = cpg.assignment
//   .where(_.target.code(".*(module\\.exports|exports).*")) // Match exports
//   .source
//   .flatMap {
//     case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
//       // Handle named functions using their `name`
//       cpg.method.where(_.nameExact(id.name)).parameter
//     case call: io.shiftleft.codepropertygraph.generated.nodes.Call =>
//       // Handle inline/anonymous functions via calls
//       call.method.parameter
//     case methodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef =>
//       // Resolve MethodRef to Method and get parameters
//       cpg.method.where(_.fullNameExact(methodRef.methodFullName)).parameter
//         .filterNot(_.name == "this") // Exclude implicit parameters like `this`
//     case block: io.shiftleft.codepropertygraph.generated.nodes.Block =>
//       // Handle object literal (e.g., { flatten, unflatten })
//       block.astChildren
//         .isCall
//         .where(_.name(Operators.assignment)) // Match assignment calls
//         .argument(2) // Get right-hand side (Identifiers like "flatten" or "unflatten")
//         .collect { case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier => id }
//         .flatMap(id => cpg.method.where(_.nameExact(id.name)).parameter)
//     case _ =>
//       List.empty
//   }
//   .distinct
//   .toList


// def sources = cpg.assignment
//   .where(_.target.code(".*(module\\.exports|exports).*")) // Match exports
//   .source
//   .flatMap {
//     case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
//       // Handle parameters of the exported constructor/function
//       val constructorParams = cpg.method.where(_.nameExact(id.name)).parameter

//       // Handle prototype method parameters
//       val prototypeParams = cpg.assignment
//         .where(_.target.code(s"${id.name}\\.prototype\\..*")) // Match prototype assignments
//         .argument(2) // Get the right-hand side (MethodRef)
//         .collect { case methodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef => methodRef }
//         .flatMap(methodRef => cpg.method.where(_.fullNameExact(methodRef.methodFullName)).parameter)
//         .filterNot(_.name == "this") // Exclude `this`

//       constructorParams ++ prototypeParams

//     case call: io.shiftleft.codepropertygraph.generated.nodes.Call =>
//       // Handle inline/anonymous functions via calls
//       call.method.parameter

//     case methodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef =>
//       // Resolve MethodRef to Method and get parameters
//       cpg.method.where(_.fullNameExact(methodRef.methodFullName)).parameter
//         .filterNot(_.name == "this") // Exclude implicit parameters like `this`

//     case block: io.shiftleft.codepropertygraph.generated.nodes.Block =>
//       // Handle object literal (e.g., { flatten, unflatten })
//       block.astChildren
//         .isCall
//         .where(_.name(Operators.assignment)) // Match assignment calls
//         .argument(2) // Get right-hand side (Identifiers like "flatten" or "unflatten")
//         .collect { case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier => id }
//         .flatMap(id => cpg.method.where(_.nameExact(id.name)).parameter)

//     case _ =>
//       List.empty
//   }
//   .distinct
//   .toList


// //working finale 2024/11/27
// def sources = cpg.assignment
//   .where(_.target.code(".*(module\\.exports|exports).*")) // Match exports
//   .source
//   .flatMap {
//     case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
//       // Handle parameters of the exported constructor/function
//       val constructorParams = cpg.method.where(_.nameExact(id.name)).parameter

//       // Handle prototype method parameters
//       val prototypeParams = cpg.assignment
//         .where(_.target.code(s"${id.name}\\.prototype\\..*")) // Match prototype assignments
//         .argument(2) // Get the right-hand side (MethodRef)
//         .collect { case methodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef => methodRef }
//         .flatMap(methodRef => cpg.method.where(_.fullNameExact(methodRef.methodFullName)).parameter)
//         .filterNot(_.name == "this") // Exclude `this`

//       // Handle properties assigned to the exported object
//       val objectPropertyParams = cpg.assignment
//         .where(_.target.code(s"${id.name}\\..*")) // Match property assignments (e.g., predefine.merge)
//         .argument(2) // Get the right-hand side (Identifier or MethodRef)
//         .flatMap {
//           case propId: io.shiftleft.codepropertygraph.generated.nodes.Identifier =>
//             cpg.method.where(_.nameExact(propId.name)).parameter
//           case propMethodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef =>
//             cpg.method.where(_.fullNameExact(propMethodRef.methodFullName)).parameter
//           case _ =>
//             List.empty
//         }
//         .filterNot(_.name == "this") // Exclude `this`

//       constructorParams ++ prototypeParams ++ objectPropertyParams

//     case call: io.shiftleft.codepropertygraph.generated.nodes.Call =>
//       // Handle inline/anonymous functions via calls
//       call.method.parameter

//     case methodRef: io.shiftleft.codepropertygraph.generated.nodes.MethodRef =>
//       // Resolve MethodRef to Method and get parameters
//       cpg.method.where(_.fullNameExact(methodRef.methodFullName)).parameter
//         .filterNot(_.name == "this") // Exclude implicit parameters like `this`

//     case block: io.shiftleft.codepropertygraph.generated.nodes.Block =>
//       // Handle object literal (e.g., { flatten, unflatten })
//       block.astChildren
//         .isCall
//         .where(_.name(Operators.assignment)) // Match assignment calls
//         .argument(2) // Get right-hand side (Identifiers like "flatten" or "unflatten")
//         .collect { case id: io.shiftleft.codepropertygraph.generated.nodes.Identifier => id }
//         .flatMap(id => cpg.method.where(_.nameExact(id.name)).parameter)

//     case _ =>
//       List.empty
//   }
//   .distinct
//   .toList

def sources = cpg.method.parameter.distinct.toList

  def sinks = cpg.call
   .where(_.name(Operators.assignment))
   .argument(1)
   .isCall
   .name(Operators.indexAccess)
   .not(_.argument(1).code("exports"))
   .argument(2)
   .toList
   
    def vuls = sinks.filter { sink =>
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
