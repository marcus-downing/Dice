case class Result(val values: List[Int], val dice: List[InnerResult]) {
  def neg = Result(values.map(-_), dice)

  def *(factor: Int) = Result(values.map(_ * factor), dice)
}

abstract class InnerResult {
  def toString: String
}

case class DieResult(val scale: Int, val natural: Int) extends InnerResult {override def toString = "d" + scale + ": " + natural}

case class VariableResult(val varname: String, val values: List[Int]) extends InnerResult {
  override def toString = "$" + varname + ": " + (values match {
    case Nil => "-"
    case _ => values.map(_.toString).reduceLeft(_ + "," + _)
  })
}

case class ExpressionResult(val exprname: String) extends InnerResult {
  override def toString = "&" + exprname
}
