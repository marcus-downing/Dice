# Dice

- A language for describing complex sets of dice rolls
- An implementation to roll the dice and produce a result
- An interactive command line
- A web server for rolling dice


## Outline

When playing complex games, such as role-playing or table-top strategy games, you sometimes need to roll a large or complicated set of dice, balanced with bonuses, adding and subtracting results, tracking multiple types, tallying stacking and non-stacking numbers, etc.

This project sets out to create a standard to be used by programs that wish to roll complex sets of dice. The goal of this project is to balance:

* clarity for a human being reading the syntax
* simplicity for a computer to execute
* richness to capture the near-infinite possible uses

While initially inspired by d20-based role-playing games, the uses of a dice description language are vary.


## Examples

```
d6
```

Rolls a single `d6` (a six-sided die) and gives a result between 1 and 6. You can combine different sizes of dice and bonuses for a more complex roll:

```
2d8+3d6+9
```

Rolls two `d8` and three `d6` dice and adds a bonus of `9`, giving a result between 14 and 43.

```
2d6 fire damage + 2 dex penalty
```

Numbers can be qualified with labels to delineate types of number and parts of a number. These labelled parts can be calculated, passed around, separate and recombined.

```

```


## Specification

The `specification` section of this project defines the language for both.

The specification is divided into three profiles, each a subset of the next:

- Profile 1: Value expressions
- Profile 2: Dice expressions
- Profile 3: Programming expressions


## Implementations

Given the standard syntax, interpreters may be implemented in any language. I intend to provide reference interpretations written in:

- Go
- JavaScript


## Tests

...