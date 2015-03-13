import scala.util.parsing.combinator.lexical._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.syntax.StdTokens

class DiceLexical extends StdLexical {
  // exclude digits from being considered identifiers/keywords
  override def token: Parser[Token] =
    (letter ~ rep(letter) ^^ {case first ~ rest => processIdent(first :: rest mkString "")}
            | digit ~ rep(digit) ^^ {case first ~ rest => NumericLit(first :: rest mkString "")}
            | delim
            | failure("illegal character")
            )

  reserved ++= List("d", "profile")
  delimiters ++= List("+", "-", "(", ")", ",", "[", "]", "*", "/", "$", "&", "=")
}

object DiceParser extends StdTokenParsers {
  type Tokens = StdTokens
  val lexical = new DiceLexical

  def die = "d" ~> numericLit ^^ {case scale => Die(1, scale.toInt)}
  def ndie = (numericLit <~ "d") ~ numericLit ^^ {case num ~ scale => Die(num.toInt, scale.toInt)}

  def nseq: Parser[Repeat] = numericLit ~ ("[" ~> expr <~ "]") ^^ {case num ~ x => Repeat(num.toInt, x)}
  def multiplyb: Parser[Multiply] = numericLit ~ group ^^ {case factor ~ x => Multiply(Constant(factor.toInt), x)}
  def multiply: Parser[Multiply] = (group | multiplyb | nseq | single) ~ "*" ~ (group | multiplyb | nseq | single) ^^ {case left ~ "*" ~ right => Multiply(left, right)}
  def divide: Parser[Divide] = (multiply | group | multiplyb | nseq | single) ~ "/" ~ (multiply | group | multiplyb | nseq | single) ^^ {case left ~ "/" ~ right => Divide(left, right)}

  def variable = "$" ~> ident ^^ {case id => LoadVariable(id)}
  def storedexpr = "&" ~> ident ^^ {case id => LoadExpression(id)}
  def execexpr = "$" ~> "&" ~> ident ^^ {case id => ExecExpression(LoadExpression(id))}
  def assignvariable = "$" ~> ident ~ ("=" ~> expr) ^^ {case id ~ expr => StoreVariable(id, expr)}
  def assignexpr = "&" ~> ident ~ ("=" ~> expr) ^^ {case id ~ expr => StoreExpression(id, expr)}
  def profileexpr = "profile" ~> expr ^^ {case expr => ProfileExpression(expr, 1000000)}

  def constant = numericLit ^^ {case num => Constant(num.toInt)}
  def single = die | ndie | constant | variable | storedexpr | execexpr
  def neg = "-" ~> (divide | multiply | group | multiplyb | nseq | single) ^^ {case x => Neg(x)}
  def group: Parser[Expr] = "(" ~> expr <~ ")" ^^ {case x => x}
  def value = divide | multiply | group | multiplyb | nseq | single

  def sum: Parser[Sum] = psum | nsum
  def psum: Parser[Sum] = value ~ sumcont ^^ {case x ~ z => Sum(x :: z.expr)}
  def nsum: Parser[Sum] = neg ~ sumcont ^^ {case x ~ z => Sum(x :: z.expr)}
  def sumcont: Parser[Sum] = psumcont | nsumcont | psumtail | nsumtail
  def psumcont = "+" ~> value ~ sumcont ^^ {case x ~ cont => Sum(List(x, cont))}
  def nsumcont = "-" ~> value ~ sumcont ^^ {case x ~ cont => Sum(List(Neg(x), cont))}
  def sumstail: Parser[Sum] = psumtail | nsumtail
  def psumtail = "+" ~> value ^^ {case x => Sum(List(x))}
  def nsumtail = "-" ~> value ^^ {case x => Sum(List(Neg(x)))}

  def iexpr = sum | neg | value

  def seq: Parser[ExprSeq] = iexpr ~ "," ~ expr ^^ {
    case x ~ "," ~ y => y match {
      case ExprSeq(z) => ExprSeq(x :: z)
      case _ => ExprSeq(List(x, y))
    }
  }

  def expr = seq | iexpr
  def cmd = profileexpr | assignvariable | assignexpr | expr

  def parse(text: String): Expr = {
    val tokens = new lexical.Scanner(text)
    val result = phrase(cmd)(tokens)
    result match {
      case Success(expr: Expr, _) => {
        expr
      }
      case Failure(msg, reader) => {
        println(msg + " @ " + reader.pos.column)
        println(reader.pos.longString)
        FailureExpr
      }
      case _ => println("?"); FailureExpr
    }
  }
}