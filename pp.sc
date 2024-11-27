import io.joern.scanners._
import io.shiftleft.semanticcpg.language._
import io.shiftleft.codepropertygraph.generated._
import io.joern.console._
import io.joern.macros.QueryMacros._
import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.joern.dataflowengineoss.semanticsloader.Semantics
import scala.util.control.Breaks._

@main def main(inputPath: String="/Users/phli/llm4pa/joern/benchmark/changeset_lib/node_modules/changeset/cpg.bin", outputPath: String = "") = {
    importCpg(inputPath)

    /*
    * Finds all assignment calls where the left hand side is an index access.
    * Pos example: val1[val2] = val3
    * Neg example: val1 = val2 
    */
    def assignmentWithIndexAccess = cpg.call.where(
        _.name(Operators.assignment).argument(1).isCall.name(Operators.indexAccess)
    )

    /*
    * Finds all assignmentWithIndexAccess that is reachable by some function parameter.
    */
    def possiblePollution = assignmentWithIndexAccess.where(
        _.reachableBy(
        cpg.method.parameter.nameNot("this")
        )
    ) ++ assignmentWithIndexAccess.where(
        _.reachableBy(
        cpg.identifier.evalType("IArguments").astParent.isCall
        )
    )

    var idList : List[Long] = List()

    for (pp <- possiblePollution) {
        breakable {
        
            /*
            * Given a possible pollution sink, only select the index access call on the left hand side of the assignment.
            */
            def indexAccessInAssignment = pp.astChildren.order(1).isCall.name(Operators.indexAccess)
            
            /*
            * If the identifier idf in 'idf[arg]' has any other type than an object or ANY, discard the sink as a possible TP.
            */
            def identifierOrCall = indexAccessInAssignment.argument(1)
            if (identifierOrCall.isIdentifier.nonEmpty) {
                if (identifierOrCall.evalType("(ANY|.*(O|o)bject|.*\\{.*\\}.*)").size == 0){
                break()
                }
            }
            
            /*
            * Creates a list containing all methodNames to be considered as possible sources.
            * Makes sure that not all method parameters are analyzed, only those parameters that are reachable by the analyzed method should be used
            */
            var nameList = pp.location.methodFullName
            var methodNames = nameList
            while(nameList.splitAt(nameList.lastIndexOf(":"))._2 != ":program"){
                methodNames += "|"
                nameList = nameList.splitAt(nameList.lastIndexOf(":"))._1
                methodNames += nameList
            }

            def callerMethods = cpg.method.fullName(methodNames).caller.fullName
            def recursiveMethodCalls = cpg.method.fullName(methodNames).ast.isReturn.ast.isCall.methodFullName.filterNot(
                _.matches(".*operator.*|.*unknownFullName.*")
            ).dedup

            def additionalMethods = (callerMethods ++ recursiveMethodCalls).dedup

            for(additionalMethod <- additionalMethods){
                methodNames += "|"
                methodNames += additionalMethod
            }

            /*
            * Checks that the right hand side of the assignment call can be reached by a method parameter.
            * For example if val in 'obj[idx] = val' can be reached. 
            */
            def valReachable = pp.argument(2).ast.where(
                _.isIdentifier.reachableBy(
                cpg.method.fullName(methodNames).parameter.nameNot("this")
                )
            ) ++ (pp.argument(2).ast.isCall ++ pp.argument(2).ast.isIdentifier).reachableBy(
                cpg.identifier.evalType("IArguments").astParent.isCall
            )
            
            /*
            * Given an index access call of obj[idx] the query will give idx as the result.
            */
            def indexArgument = indexAccessInAssignment.argument(2)

            /*
            * Checks that indexArgument can be reached by a method parameter.
            */ 
            def indexArgumentTainted = indexArgument.where(
                _.reachableBy(
                cpg.method.fullName(methodNames).parameter.nameNot("this")
                )
            ) ++ indexArgument.where(
                _.reachableBy(
                cpg.identifier.evalType("IArguments").astParent.isCall
                )
            )
            
            /*
            * obj = obj[arg1]
            * obj[arg2] = arg3
            * Query checks that obj[arg2] can be reached by obj[arg1] in the above example
            */
            def lastArgument = identifierOrCall.reachableBy(
                cpg.call(Operators.indexAccess)
            ).filterNot(
                node => node.id == indexAccessInAssignment.id.l.head || (node.lineNumber.get == identifierOrCall.lineNumber.l.head && pp.argument(2).id == node.id)
            )
            
            /*
            * If the identifier idf in 'idf[arg]' has any other type than an object or ANY, discard the sink as a possible TP.
            */
            if (lastArgument.argument(1).isIdentifier.nonEmpty) {
                if (lastArgument.argument(1).evalType("(ANY|.*(O|o)bject|.*\\{.*\\}.*)").size == 0){
                break()
                }
            }

            /*
            * Check that idx in 'obj[idx]' can be reached by a method parameter.
            */
            def lastArgumentTainted = lastArgument.where(
                _.argument(2).reachableBy(
                cpg.method.fullName(methodNames).parameter.nameNot("this")
                )
            ) ++ lastArgument.where(
                _.argument(2).reachableBy(
                cpg.identifier.evalType("IArguments").astParent.isCall
                )
            )

            /*
            * If all of the subqueries have had atleast one found match each, mark the original assignment call as a TP.
            */
            if (valReachable.size > 0 && indexAccessInAssignment.size > 0 && indexArgument.size > 0 && indexArgumentTainted.size > 0 && identifierOrCall.size > 0 && lastArgumentTainted.size > 0) {
                idList = idList :+ pp.id
            }
        }
    }
    /*
    * Mark all calls of the analyzed cpg as TP if that specific call has an id that can also be found in the idList
    */
    var result = cpg.call.filter(node => idList.contains(node.id)).dedup.l


    val outputFile = if (outputPath == "") {
    val outPath = os.Path(inputPath, base = os.pwd) / os.up
    outPath / "output.txt"
    } else os.Path(outputPath, base = os.pwd)
    println()
    println(s"Writing output to file: ${outputFile}")

    os.write.over(outputFile, result.mkString("\n"))
}