 def sinks = cpg.call.where(_.name(Operators.assignment)).argument(1).isCall.name(Operators.indexAccess).argument(2)

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

  def flows = sinks.reachableByFlows(sources).dedup

