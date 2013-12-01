# Dice specification

This section specifies the syntax of the dice-rolling language. This readme outlines the language in human terms. The accompanying syntax documents specify it in a way that can be used by compilers, interpreters and other tools.



# Expressions

An *expression* is a single string (assumed to be UTF-8) that describes either:

- a value literal, or
- a set of dice rolls, values and instructions that can be executed to produce a value

An expression can be *executed*, which means rolling dice, collapsing control structures and ultimately producing a result. Executing an expression is *idempotent*: executing the result again will only produce the same result.

The specification is divided into three profiles, each a subset of the next:

- Profile 1: Value expressions
- Profile 2: Expressions with dice
- Profile 3: Expressions with programming constructs

For most purposes, profile 2 is what's needed


# Profile 1: Value Expressions

A *value* can be a number, an array or some other structure of numbers. It contains no random or undetermined elements.

A *value literal* is an expression which describes a single value unambigously. Often there can be more than one way of describing a value, one of which is considered the *simplest form*. Executing an expression always attempts to reduce a value to its simplest form - which is not necessarily the same as the shortest form.

This result of executing any expression is a value, and values are also expressions, so the value literal syntax is a subset of the full expression syntax.

All values reduce to integers or the special "unit" value. There are no strings, characters, floating point numbers or blobs. Booleans are either `1` for true or `0` for false.



## Value literal syntax

## Constants

A *constant* is a single number. Numbers are all integers.

```
6
```

The number `6`.


## Unit

```
_
```

The underscore `_` represents a special entity called the *unit value*. This is a placeholder which can mean anything or nothing depending on where it's used. It has parallels with values like `null` in some languages.

For uses of the unit value, see "Labelled values" and "Maths with the unit" below.



## Labelled values

Depending on the rules of the game, not all numbers refer to the same type of thing. You can attach labels to numbers, which can then be combined and calculated while remembering their component parts.

```
3 fire damage
```

A number `3` qualified with two labels, `fire` and `damage`.

```
3 damage fire
```

Exactly the same thing. The order of labels makes no difference

```
3 piercing damage + 3 bludgeoning damage
```

A single value consisting of two parts.

```
(3 piercing + 3 bludgeoning) damage
```

An alternative way of saying the same thing.

Given a composite value like this you can find:

- The *absolute value* = `6`
- The *`damage` part* = `6 damage`
- The *`piercing` part* = `3 piercing`

```
6 fire damage - 3 fire
```

Equivalent to `3 fire damage`. This can be used to model resistance to various energy types etc.

### Labelled unit values

```
_ deafened
```

Attaching a label to the unit value is a pattern for conveying labels that have no numeric value.


## Arrays

An *array* is a sequence of values. 

```
[2, 3]
```

An array of two values.

```
[]
```

An array with no elements. This is equal to `_`.


### Rich arrays

Arrays can contain any value, including more arrays. The parts aren't required to be of the same type or have the same qualification.

```
[[23 ranged touch attack, 14 ranged touch attack], 18 dc will, [9 cold damage, 2 str penalty, _ fatigued], nil]
```

In this example, the first value is an array of attacks; the second value describes a Will Save with a difficulty class of 18; the third describes a set of effects (including damage, attribute penalty and a status effect) and the fourth is an empty array.

There's no fixed format for the values in an array, which can get confusing. Named parts allow you to create an associative array or object:

```
[attack: [23, 14] ranged touch attack, save: 18 dc will, fail: [9 cold damage, 2 str penalty, _ fatigued], pass: nil]
```

In this version the various parts are labelled for clarity. Note that the labels for parts are not the same as the labels attached to the values.





# Profile 2: Expressions

Expressions cover any syntax which _produces_ a value but is not itself a value.


## Comments

Any line beginning with `//`, optionally preceded by a number of spaces, is omitted from the expression. `//` elsewhere in a line does not start a comment

```
// calculate the strength penalty
```


## Dice expressions

```
d8
```

A *die* is the analogy of a physical die. It generally produces a number between 1 and its cap value, so a `d8` produces a value between `1` and `8`. You can have odd numbers of dice: `d87` is as valid as `d2`.

```
3d6
```

Independently rolls three separate `d6` dice.

A *dice expression* is a combination of dice with other values.

```
d8 + 3d6 + 4
```

Indepently rolls one `d8`, three `d6` and adds them together with the bonus `4` to produce a result between `8` and `30`.

Dice expressions may also be labelled:

```
(d8 + 4) piercing + 3d6 sneak
```

Produces a combined result such as `7 piercing + 9 sneak`.



## Maths

```
3 + 3
```

Equals `6`.

```
2 * 3
```

Equals `6`.

```
2 * (3 + 2)
```

Equals `10`.

```
9 / 2
```

Equals `4`. Normally division always rounds down to the nearest integer - but see the section on 

```
9 // 2
```

Equals `5`. Division using `//` rounds up.

```
9 % 2
```

Equals `1`, the remainder from `9 / 2`.


### Maths on labelled values

Labelled values may also take part in calculations.

```
2 * 3 electricity damage
```

Equivalent to `6 electricity damage`.

```
2 * (3 fire + 3 bludgeoning) damage
```

Equivalent to `6 fire damage + 6 bludgeoning damage`.

There is a special operator `|` for labels:

```
2 (fire | bludgeoning)
```

Equivalent to `1 fire + 1 bludgeoning`. The number is broken into two parts, one for each label.

```
6 (fire | bludgeoning) damage
```

Equivalent to `3 fire damage + 3 bludgeoning damage`. Splits the value into two parts with different qualifications. In cases that don't divide equally, the first number gets favour:

```
11 (fire | electricity | piercing) damage
```

Equivalent to `4 fire + 4 electricity + 3 piercing`.


### Maths with the unit value

```
3 + _
```

Equals `3`. Adding the unit has no effect.

```
3 * _
```

Equals `3`. Multiplying by the unit has no effect.

```
_ / 2
```

Equals `_`.

```
3 / _
3 // _
```

Both equal `3`. Dividing by the unit has no effect

```
3 % _
```

Equals `0`. There is no remainder from dividing by the unit.


### Maths on arrays

Arrays may take part in calculations.

```
1 + [0, 1]
```

Equals `[1, 2]`.

```
2 * [3, 4]
```

Equals `[6, 8]`.

```
[2, 2] + [3, 5]
```

Equals `[5, 6]`. Each of the parts of the array are added together.

```
[2, 2] * [3, 5]
```

Equals `[6, 10]`. Again, the values are multiplied separately. d20expr does not do vector or matrix multiplication.

```
[2, 2, 2] + [3, 5]
```

Equals `[5, 6, 2]`. Missing parts are considered to be the unit value, `_`.


## Array operations

```
[2, 3] . [4, 5]
```

Equals `[2, 3, 4, 5]`. The `.` operator appends arrays.

```
#[9, 5, 3]
```

Equals `3`. The `#` operator gets the length of an array. See below for applying this to variables.

```
#[[1, 2], [3, 4]]
```

Equals `2`. The `#` operator does nothing special with nested arrays.

```
3 ^ 7
```

Equals `[7, 7, 7]`. Produces three sets of the value `7` in an array. The value to be repeated can be any value.

```
2 ^ 5 (slashing | piercing)
```

Equals `[3 slashing + 2 piercing, 3 slashing + 2 piercing]`.


## Blocks

```
3 { d4 + 1 }
```

Produces a value such as `8`. Rolls three sets of `d4 + 1`, and adds together the three results.

```
3 ^ { d4 + 1}
```

Produces a value such as `[2, 5, 3]`. Rolls three sets of `d4 + 1` and puts the three results in an array.




# Profile 3: Programming

The third profile introduces variables, control structures, functions and higher-order pgoramming, allowing for much more complicated programs to be written.

While not strictly enforced, the language encourages immutable values and a functional programming style. This is of course only a very small subset of a real programming language.


## Variables

```
$str = 14
```

Allocates a value to a variable. That variable may now be used as if it were a value.

```
d20 + $str
```


### Variables and arrays

```
$arr = [1, 2, 3]
$foo = $arr[2]
```

Equals `3`. Use the `$arr[...]` structure to get indexed fields from an array.

```
$arr = [4, 5, 6]
$slice = $arr[1;2]
```

Equals `[5, 6]`. Use the `$arr[...;...]` structure to get a slice from an array. Either side of the slice divider may be omitted

```
$arr = [2, 3]
[$x, $y] = $arr
```

Assigns `2` to `$x` and `3` to `$y`.

```
$array = [1, 2, 3, 4, 5, 6]
[$a, _, _, $b] = $array
```

Assigns `1` to `$a` and `4` to `$b`. A placeholder `_` can be used to discard a value, and any value off the end of the target array will also be discarded.

```
[$first, $second, $third] = [7, 3]
```

Assigns `7` to `$first`, `3` to `$second` and the unit value `_` to `$third`.

The array length operator works on variables as well, provided the value of the variable is an array:

```
$stats = [3, 9, 1]
#$stats
```

In this case `#stats` equals `3`, the length of the array `[3, 9, 1]`



## Control structures

### If

A conditional allows decisions to be made based on values. Like any other expression, it produces one value as its result

```
if 2 < 3 ? 1 ; 0
```

Equals `1`. The `if` take a boolean value and two possible values. In this case, because `2 < 3`, it produces the value `1`. When the condition is part of a larger sequence, surround it with brackets:

```
$x = d6 + d3; (if $x > 6 ? 20 ; 5)
```

By default, the results of an `if` condition are `1` and `0`, and these are equivalent to  so this has the effect of converting an expression into a boolean.

```
$nat20 = d20 == 20 ?
```


### Switch

```
switch d3 ? 1: 2d6 fire damage; 2: 2d6 cold damage; 3: d3 negative levels
```

The `switch` conditional allows you to produce different expressions for different values.

```
(switch d6 ?
	5, 6: 2d8
	_:    1
)
```

In this example the `switch` condition is spread over multiple lines, with newlines taking the place of semicolons. The comma-separated list allow either of the values to be matched, while the placeholder catches any other value. The brackets are needed to make it clear where the switch ends.

### Loops

```
for [1, 2, 3] ; { $a: $a * ($a + 1) }
```

Equals `[2, 6, 12]`. For each of the numbers in the array `[1, 2, 3]`, assign that to the variable `$a` then run the expression `$a * ($a + 1)`.



## Special variables

The system includes a number of variables that are set automatically. If you try to assign a value to any of these variables, the expression will refuse to run.

```
for [1, 2, 3] { $x = d4; $x ^  }
```

In any scope with one argument, that argument is stored in `$_`.

```
[$old, $older, @oldest] = $?
```

The special variable `$?` has a history of the results of recent lines, in reverse order. This example assigns the most recent result to `$old`, the previous one into `$older`, and the one before that into `$oldest`. Note that this variable changes every single line, so assigning to an array is a good way of fetching several of them at once.

```
[_, _, $thing] = $?
```

Assigns the third-most recent result to `$thing`.

Note that there is a separate history for each scope of excution, so when used within a function `$?` only has results from inside the function. Calling a function produces only a single result in the outside scope, so its internals are not exposed.

```
$attack = d20 + 4
$critical = $d20 == 20 ?
```

The special variables `$d4`, `$d6`, `$d8` etc contain the natural result of the most recently rolled die of each type.

```
[$arg1, $arg2, $arg3] = $@
```

If the script or function was called with arguments, they're stored in the special variable `$@`. This is generally not the easiest way to get these values - see below for the normal way to use functions.

You can use the array length operator `#` to get the number of arguments: `#@`.


## Blocks and functions

A *block* is a sequence of instructions that results in one value at the end.

```
$modifier = {
	$attribute = 17
	($attribute - 10) / 2
}
```

Equivalent to `$modifier = 3`. The block is executed straight away, and the last line in it produces its results. In this case, the value `3` is assigned to `$modifier`.

To store a block and re-use it later, assign it using the `&` sigil in place of `$`. This makes it a *function*.

```
&create_attribute_score = {
	$attr = 3d6
	$attr = if $attr < 6 ? 6 ; $attr
	$attr = if $attr > 12 ? 12 ; $attr
	$attr
}
&create_str_score = &create_attribute_score
$str = &create_str_score()
$dex = &create_attribute_score()
```

Assigns a number between 6 and 12 to `$str`. The function `&create_attribute_score` is renamed by assigning it again to `&create_str_score`, but only when called with brackets is it executed.

A block or function may take arguments, by specifying them as the first element in the block followed by a `:`:

```
&calc = { $a, $b :
	($a + 1) * ($b + 1)
}
&calc(3, 2)
```

Equals `12`. The function `&calc` is defined as taking two arguments, then later it's called with the values `3` and `2`.

### Arrays as arguments

Arrays can be passed to functions, as can any structure of nested arrays.

```
&add_two = { $left, $right:
	$left + $right
}
$left = [1, 3, 4]
$right = [7, 5, 4]
&add_two($left, $right)
```

Equals `[8, 8, 8]`.

Arrays can be expanded into a function's arguments with `...`:

```
$values = [5, 1]
&calc($values...)
```


### Higher order programming

Functions can be used as arguments to functions:

```
&roll_damage = { $number, $bonus, &roll:
	$number ^ { $bonus + &roll() }
}
&roll_damage(3, 7, { d8 + 2 } )
```

Passes the expression block `d8 + 2` as an argument into the function `&roll_damage`. Note that the argument uses the `&` sigil instead of `$`. If the expression `d8 + 2` were passed as a value to a normal variable, it would be calculated _before_ the function call.



# API

The following built-in functions are made available.

```
&max(d20 + 2)
```

Equals `22`. The highest possible value of an expression.

```
&min(d20 + 2)
```

Equals `3`. The lowest possible value of an expression.

```
&avg(d20 + 2)
```

Equals `12`. The average value of an expression, rounded down.

```
&profile(2d6)
```

Produces `[[2, 1], [3, 2], [4, 3], [5, 4], [6, 5], [7, 6], [8, 5], [9, 4], [10, 3], [11, 2], [12, 1]]`. This is a statistical profile of the expression.

```
&abs(3 slashing + 5 sneak)
```

Equals `5`. The absolute value add together all parts, discards all labels and flattens all arrays to produce a single number.





# Special commands

These commands are only available in the command line - they aren't part of the proper syntax

## Output

```
echo We're rolling dice!
```

Writes `We're rolling dice!` to the command line.