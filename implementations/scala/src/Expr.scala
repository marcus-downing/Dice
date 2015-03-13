abstract class Expr {
  def eval: Result
  def min: List[Int]
  def max: List[Int]
}

case class Die(num: Int, scale: Int) extends Expr {
  def eval = {
    val dice: List[DieResult] = (1 to num).toList.map(n => DieResult(scale, Dice2.random.nextInt(scale) + 1))
    val naturals: List[Int] = dice.map(_.natural)
    val value: Int = naturals.foldRight(0)(_ + _)
    Result(List(value), dice)
  }

  def min = List(num)
  def max = List(num * scale)
}

case class Constant(value: Int) extends Expr {
  def eval = Result(List(value), Nil)
  def min = List(value)
  def max = List(value)
}

case class Neg(expr: Expr) extends Expr {
  def eval = expr.eval.neg
  def min = expr.min.map(-_)
  def max = expr.max.map(-_)
}

case class ExprSeq(expr: List[Expr]) extends Expr {
  def eval = {
    val results = expr.map(_.eval)
    Result(results.flatMap(_.values), results.flatMap(_.dice))
  }

  def min = expr.flatMap(_.min)

  def max = expr.flatMap(_.max)
}

case class Repeat(factor: Int, expr: Expr) extends Expr {
  def eval = {
    val results: List[Result] = (1 to factor).toList.map(n => expr.eval)
    // val resultValues: List[List[Int]] = results.map(_.values)
    val squished: List[Int] = results.map(_.values).reduceLeft {
      (first, second) =>
        val zipped: List[(Int, Int)] = first.zip(second)
        zipped map {case (a, b) => a + b}
    }
    val dice = results.flatMap(_.dice)
    Result(squished, dice)
  }

  def min = {
    val results = (1 to factor).toList.map(n => expr.min)
    results.reduceLeft {
      (first, second) =>
        val zipped = first.zip(second)
        zipped map {case (a, b) => a + b}
    }
  }

  def max = {
    val results = (1 to factor).toList.map(n => expr.max)
    results.reduceLeft {
      (first, second) =>
        val zipped = first.zip(second)
        zipped map {case (a, b) => a + b}
    }
  }
}

case class Sum(expr: List[Expr]) extends Expr {
  def eval = {
    val results = expr.map(_.eval)
    val values = results.map(_.values).reduceLeft((left, right) =>
      for (l <- left; r <- right) yield l + r)
    val dice = results.flatMap(_.dice)
    Result(values, dice)
  }

  def min = {
    val results = expr.map(_.min)
    results reduceLeft {
      (left, right) => for (l <- left; r <- right) yield l + r
    }
  }

  def max = {
    val results = expr.map(_.max)
    results.reduceLeft {
      (left, right) => for (l <- left; r <- right) yield l + r
    }
  }
}

case class Multiply(left: Expr, right: Expr) extends Expr {
  def eval = {
    val lresult = left.eval
    val rresult = right.eval
    val values = for (l <- lresult.values; r <- rresult.values) yield l * r
    Result(values, lresult.dice ::: rresult.dice)
  }

  def min =
    for (l <- left.min; r <- right.min) yield l * r

  def max =
    for (l <- left.max; r <- right.max) yield l * r
}

case class Divide(left: Expr, right: Expr) extends Expr {
  def eval = {
    val lresult = left.eval
    val rresult = right.eval
    val values = for (l <- lresult.values; r <- rresult.values; if (r != 0)) yield l / r
    Result(values, lresult.dice ::: rresult.dice)
  }

  def min =
    for (l <- left.min; r <- right.min; if (r != 0)) yield l / r

  def max =
    for (l <- left.max; r <- right.max; if (r != 0)) yield l / r
}

case class StoreVariable(id: String, expr: Expr) extends Expr {
  def eval = {
    val result = expr.eval
    Data.variables(id) = result.values
    result
  }
  def min = expr.min
  def max = expr.max
}

case class LoadVariable(id: String) extends Expr {
  def eval = {
    val results = if (Data.variables.contains(id))
      Data.variables(id)
    else
      Nil
    Result(results, VariableResult(id, results) :: Nil)
  }
  def min = eval.values
  def max = eval.values
}

case class StoreExpression(id: String, expr: Expr) extends Expr {
  def eval = {
    Data.expressions(id) = expr
    expr.eval
  }
  def min = expr.min
  def max = expr.max
}

case class LoadExpression(id: String) extends Expr {
  def load = {
    if (Data.expressions.contains(id))
      Data.expressions(id)
    else
      FailureExpr
  }
  def eval = {
    if (Data.expressions.contains(id)) {
      val expr = Data.expressions(id)
      val result = expr.eval
      Result(result.values, ExpressionResult(id) :: result.dice)
    } else {
      Result(Nil, ExpressionResult(id) :: Nil)
    }
  }
  def min = load.min
  def max = load.max
}

case class ExecExpression(expr: LoadExpression) extends Expr {
  val result = expr.eval

  def eval = result
  def min = expr.min
  def max = expr.max
}

case class ProfileExpression(expr: Expr, count: Int) extends Expr {
  def eval = {
    /* 
      do this procedural-style for efficiency
        x = the index of the result 1 .. 1,000,000 (or thereabouts)
        i = the index within the result: 0 .. size
        n = the result value: eg 3 .. 18
        freq(i)(n - lowestValue) = frequency of result n for calculation i
     */
    import scala.collection.mutable.ListBuffer

    //  prepare
    val highestValue = expr.max reduceLeft { (a, b) => Math.max(a, b) }
    val lowestValue = expr.min reduceLeft { (a, b) => Math.min(a, b) }
    val size = expr.eval.values.length
    val resultRange = (0 until size).toList
    val valueRange = (lowestValue to highestValue).toList

    //  evaluate
    val freq: List[ListBuffer[Int]] = for (i <- resultRange) yield {
      val lb = new ListBuffer[Int]
      for (n <- valueRange) lb += 0
      lb
    }

    (1 to count).projection foreach { x =>
        val result = expr.eval
        for (i <- resultRange; n <- valueRange) {
          val value = result.values(i)
          if (value == n)
            freq(i)(n - lowestValue) = freq(i)(n - lowestValue) + 1
        }
    }

    //  print
    println(if (count > 1) "Value, Frequencies" else "Value, Frequency")
    for (n <- valueRange) {
      val fs = resultRange.map(i => freq(i)(n - lowestValue))
      println(n + ", " + fs.mkString(", "))
    }

    //  return nothing
    Result(Nil, Nil)
  }
  def min = expr.min
  def max = expr.max
}

case object FailureExpr extends Expr {
  def eval = Result(Nil, Nil)
  def min = Nil
  def max = Nil
}