import java.io.{BufferedReader, InputStreamReader}
import scala.util.Random
import scala.io.Source


object Dice2 {
  val random = new Random()

  val help = """
DICE

Roll a single die                                      d6
  - any size die                                       d 349
Roll four separate dice and add the results            4d6
  - combine any number of dice and numbers             2d20 + 35d6 - 32
Brackets multiply                                      3(d6+4)
  - so does *                                          3*d6+12
  - division rounds down                               5(2*d6+4d12-3)/2

MULTIPLE RESULTS

Roll two separate dice and return two results          d4, d12
  - any combination of expressions                     d4+10, d8-12, 6d35-2d9
Apply the same calculation to multiple results         4(d6,d6,d6) + 5
  - combine dice and numbers                           d20 + (17,12,7)
Do a calculation several times, add the results        3 [d20+4d6-3]
Apply the same multiplier to different dice            12[d6,d6,d8]
  - combine multipliers                                4[d6,d8],3[d10,d12],d20

VARIABLES

Store a value as a variable $foo                       $foo = d20 + 2d6
Use a variable in later expressions                    d20 - $foo

STORED EXPRESSIONS

Store a calculation as a variable &bar                 &bar = d6+3
Use the calculation in later expressions               d20 - &bar
Force the expression to calculate immediately          $&bar
  - useful when the expression would be duplicated     3[d8 + $&bar]"""


  def main(args: Array[String]) {
    val br = new BufferedReader(new InputStreamReader(System.in))
    println("\nDICE\n\nExamples:\nd6       (2d10+2)/2      d20+(17,12,7)      help      exit")
    while (true) {
      print("\ndice> ")
      val cmd = br.readLine.trim.toLowerCase
      if (cmd == "exit")
        return 0;
      else if (cmd == "help") {
        println(help)
      } else {
        val expr = DiceParser.parse(cmd)
        val result = expr.eval

        println(result.dice.map(die => die.toString).mkString("   "))
        expr match {
          case StoreVariable(id, _) => print("$" + id + " = ")
          // case StoreExpression(id, _) => print("&" + id + " = ")
          case _ => print(" = ")
        }
        result.values match {
          case Nil => println("-")
          case _ => println(result.values.map(_.toString).mkString(", "))
        }
      }
    }
    return 0
  }
}