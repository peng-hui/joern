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
      name = "prototype-pollution-assignment",
      author = "Tobiasfro",
      title = "Allows user to change the prototype of an object",
      description = """
        |User can use the function to change the prototype of an object, allowing for prototype pollution
        |""".stripMargin,
      score = 8,
      withStrRep({ cpg =>
        def possiblePollution = cpg.call.where(_.name(Operators.assignment).reachableBy(cpg.method.parameter.nameNot("this")))
        var idList : List[Long] = List()
        for (pp <- possiblePollution) {
          breakable {
            def methodName = pp.location.methodShortName
            def valReachable = pp.argument(2).reachableBy(cpg.method.name(methodName++"|anonymous.*").parameter.nameNot("this"))
            def indexAccessInAssignment = pp.astChildren.order(1).isCall.name(Operators.indexAccess)
            def indexArgument = indexAccessInAssignment.argument(2)
            def indexArgumentTainted = indexArgument.where(_.reachableBy(cpg.method.name(methodName++"|anonymous.*").parameter.nameNot("this")))
            def identifierOrCall = indexAccessInAssignment.argument(1)
            if (identifierOrCall.isIdentifier.nonEmpty) {
                if (identifierOrCall.evalType("(ANY|.*(O|o)bject|.*\\{.*\\}.*)").size == 0){
                    break()
                }
            }
            def lastArgumentTainted = identifierOrCall.reachableBy(cpg.call(Operators.indexAccess)).where(_.argument(2).reachableBy(cpg.method.name(methodName++"|anonymous.*").parameter.nameNot("this")))
            if (indexAccessInAssignment.size > 0 && indexArgument.size > 0 && indexArgumentTainted.size > 0 && identifierOrCall.size > 0 && lastArgumentTainted.size > 0) {
              idList = idList :+ pp.id
            }
          }
        }
        cpg.call.filter(node => idList.contains(node.id)).l
      }),
      tags = List(QueryTags.ppAssign, QueryTags.default),
    )

    @q
  def general(): Query =
    Query.make(
      name = "prototype-pollution-general",
      author = "Tobiasfro",
      title = "Allows user to change the prototype of an object",
      description = """
        |User can use the function to change the prototype of an object, allowing for prototype pollution
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
            def indexArgumentTainted = indexArgument.where(_.reachableBy(cpg.method.name(methodName++"|anonymous.*").parameter.nameNot("this")))
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
            def lastArgumentTainted = identifierOrCall.reachableBy(cpg.call(Operators.indexAccess)).where(_.argument.reachableBy(cpg.method.name(methodName++"|anonymous.*").parameter.nameNot("this")))
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