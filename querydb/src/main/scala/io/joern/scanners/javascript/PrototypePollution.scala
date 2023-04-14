package io.joern.scanners.javascript

import io.joern.scanners._
import io.shiftleft.semanticcpg.language._
import io.shiftleft.codepropertygraph.generated._
import io.joern.console._
import io.joern.macros.QueryMacros._
import io.joern.dataflowengineoss.language._
import io.joern.dataflowengineoss.queryengine.EngineContext
import io.joern.dataflowengineoss.semanticsloader.Semantics
import scala.util.control.Breaks._

object PrototypePollution extends QueryBundle {

  implicit val engineContext: EngineContext = EngineContext(Semantics.empty)
  implicit val resolver: ICallResolver = NoResolve

  @q
  def prototypePollution(): Query =
    Query.make(
      name = "prototype-pollution",
      author = "Tobiasfro",
      title = "This assignment may alter Object.prototype if a malicious '__proto__' string is injected",
      description = """
        |Function can be used to change the prototype of an object, allowing for prototype pollution.
        |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        
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

            def recursiveMethodCalls = cpg.method.fullName(methodNames).ast.isReturn.ast.isCall.methodFullName.filterNot(_.matches(".*operator.*|.*unknownFullName.*")).dedup
            for(recursiveMethodCall <- recursiveMethodCalls){
                methodNames += "|"
                methodNames += recursiveMethodCall
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
        cpg.call.filter(node => idList.contains(node.id)).dedup.l
      }),
      tags = List(QueryTags.pp, QueryTags.default),
      multiFileCodeExamples = MultiFileCodeExamples(
        positive = List(
          List(
            CodeSnippet("""
              |
              |function pp(arg1, arg2, arg3){
              |  var obj = {}
              |  obj[arg1][arg2]=arg3
              |}
              |
              |""".stripMargin,
              "testPositive1.js"
            )
          ),
          List(
            CodeSnippet("""
              |
              |function test(arg){
              |  var t = arg
              |  return t
              |}
              |
              |function pp(arg1, arg2, arg3) {
              |  if (arg3) {
              |    var o = {};
              |    var o1 = o[test(arg1)];
              |    o1[arg2] = arg3;
              |  } else {
              |    var op = {};
              |    var o1 = op['foo1'];
              |    o1['foo2'] = v;
              |  }
              |}
              |
              |""".stripMargin,
              "testPositive2.js"
            )
          )
        ),
        negative = List(
          List(
            CodeSnippet("""
              |
              |function test(arg1, arg2, arg3){
              |  var obj = {}
              |  obj[arg][arg]=arg
              |}
              |
              |""".stripMargin,
              "testNegative1.js"
            )
          ),
          List(
            CodeSnippet("""
              |
              |function test(arg){
              |  var t = arg
              |  return t
              |}
              |
              |function pp(arg1, arg2, arg3) {
              |  if (arg3) {
              |    var o = {10 : {}};
              |    var arg = 10
              |    var o1 = o[test(arg)];
              |    o1[arg2] = arg3;
              |  } else {
              |    var op = {};
              |    var o1 = op['foo1'];
              |    o1['foo2'] = v;
              |  }
              |}
              |
              |""".stripMargin,
              "testNegative2.js"
            )
          )
        )
      )
    )
}