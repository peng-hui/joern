// dsl
package io.joern.dsl
import io.shiftleft.semanticcpg.language._
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes}
import io.shiftleft.semanticcpg.codedumper.CodeDumper
import io.shiftleft.codepropertygraph.generated.help.{Doc, Traversal}
import io.shiftleft.semanticcpg.language.operatorextension.OpNodes.Assignment
import io.shiftleft.semanticcpg.language.operatorextension.allAssignmentTypes

/**
* FindNodeByType
* The nodes should be useable by reachableBy
*/

class DSL(val traversal: Iterator[AstNode]) extends AnyVal {

  /**
   * Finds assignment calls within any traversal.
   *
   * @return Iterator[Assignment]
   */
  def findAssignments(): Iterator[Assignment] = {
    traversal.flatMap(_.assignment)

    //  {
    //   case call: Call if allAssignmentTypes.contains(call.methodFullName) =>
    //     Some(call.asInstanceOf[Assignment])
    //   case _ => None
    // }
  }
}
