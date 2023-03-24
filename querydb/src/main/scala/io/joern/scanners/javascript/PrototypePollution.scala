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
  def assignment(): Query =
    Query.make(
      name = "prototype-pollution",
      author = "Tobiasfro",
      title = "This assignment may alter Object.prototype if a malicious '__proto__' string is injected",
      description = """
        |Function can be used to change the prototype of an object, allowing for prototype pollution
        |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        def assignmentWithIndexAccess = cpg.call.where(
          _.name(Operators.assignment).argument(1).isCall.name(Operators.indexAccess)
        )
    
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

            def indexAccessInAssignment = pp.astChildren.order(1).isCall.name(Operators.indexAccess)
            def identifierOrCall = indexAccessInAssignment.argument(1)
            if (identifierOrCall.isIdentifier.nonEmpty) {
                if (identifierOrCall.evalType("(ANY|.*(O|o)bject|.*\\{.*\\}.*)").size == 0){
                    break()
                }
            }
            
            var nameArr = pp.location.methodFullName
            var methodNames = nameArr
            while(nameArr.splitAt(nameArr.lastIndexOf(":"))._2 != ":program"){
              methodNames += "|"
              nameArr = nameArr.splitAt(nameArr.lastIndexOf(":"))._1
              methodNames += nameArr
            }
            def recursiveMethodCalls = cpg.method.fullName(methodNames).ast.isReturn.ast.isCall.methodFullName.filterNot(_.matches(".*operator.*|.*unknownFullName.*")).dedup
            for(recursiveMethodCall <- recursiveMethodCalls){
                methodNames += "|"
                methodNames += recursiveMethodCall
            }
            def valReachable = pp.argument(2).ast.where(
              _.isIdentifier.reachableBy(cpg.method.fullName(methodNames).parameter.nameNot("this"))
            ) ++ (pp.argument(2).ast.isCall ++ pp.argument(2).ast.isIdentifier).reachableBy(
              cpg.identifier.evalType("IArguments").astParent.isCall
            ) ++ pp.argument(2).where(_.code("\\{\\}"))
            
            def indexArgument = indexAccessInAssignment.argument(2)
            def indexArgumentTainted = indexArgument.where(
                _.reachableBy(cpg.method.fullName(methodNames).parameter.nameNot("this"))
              ) ++ indexArgument.where(
                _.reachableBy(cpg.identifier.evalType("IArguments").astParent.isCall)
              )
            def lastArgumentTainted = identifierOrCall.reachableBy(
                cpg.call(Operators.indexAccess)
              ).filterNot(
                node => node.id == indexAccessInAssignment.id.l.head || (node.lineNumber.get == identifierOrCall.lineNumber.l.head && pp.argument(2).id == node.id)
              ).where(
                _.argument(2).reachableBy(cpg.method.fullName(methodNames).parameter.nameNot("this"))
              ) ++ identifierOrCall.reachableBy(
                cpg.call(Operators.indexAccess)
              ).filterNot(
                node => node.id == indexAccessInAssignment.id.l.head || (node.lineNumber.get == identifierOrCall.lineNumber.l.head && pp.argument(2).id == node.id)
              ).where(
                _.argument(2).reachableBy(
                  cpg.identifier.evalType("IArguments").astParent.isCall
                )
              )
            if (valReachable.size > 0 && indexAccessInAssignment.size > 0 && indexArgument.size > 0 && indexArgumentTainted.size > 0 && identifierOrCall.size > 0 && lastArgumentTainted.size > 0) {
              idList = idList :+ pp.id
            }
          }
        }
        cpg.call.filter(node => idList.contains(node.id)).dedup.l
      }),
      tags = List(QueryTags.pp, QueryTags.default),
    )

    @q
  def general(): Query =
    Query.make(
      name = "prototype-pollution-general",
      author = "Tobiasfro",
      title = "This assignment may alter Object.prototype if a malicious '__proto__' string is injected",
      description = """
        |Function can be used to change the prototype of an object, allowing for prototype pollution
        |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        def possiblePollution = cpg.call.where(_.name(Operators.assignment).reachableBy(cpg.method.parameter.nameNot("this")))
        var idList : List[Long] = List()
        for (pp <- possiblePollution) {
          breakable {
            def methodName = pp.location.methodShortName
            def indexAccessInAssignment = pp.astChildren.order(1).isCall.name(Operators.indexAccess)
            def indexArgument = indexAccessInAssignment.astChildren.order(2).isIdentifier
            def indexArgumentTainted = indexArgument.where(_.reachableBy(cpg.method.parameter.nameNot("this")))
            def identifierOrCall = if(indexAccessInAssignment.astChildren.order(1).isIdentifier.isEmpty){
                indexAccessInAssignment.astChildren.order(1).isCall
            } else {
                indexAccessInAssignment.astChildren.order(1).isIdentifier
            }
            if (identifierOrCall.isIdentifier.nonEmpty) {
                  if (identifierOrCall.evalType("(ANY|.*(O|o)bject|.*\\{.*\\}.*)").size == 0){
                      break()
                  }
            }
            def lastArgumentTainted = identifierOrCall.reachableBy(cpg.call(Operators.indexAccess)).where(_.argument.reachableBy(cpg.method.parameter.nameNot("this")))
            if (indexAccessInAssignment.size > 0 && indexArgument.size > 0 && indexArgumentTainted.size > 0 && identifierOrCall.size > 0 && lastArgumentTainted.size > 0) {
              idList = idList :+ pp.id
            }
          }
        }
        cpg.call.filter(node => idList.contains(node.id)).l
      }),
      tags = List(QueryTags.ppGeneral, QueryTags.default),
    )
}