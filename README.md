# a_game

a_game is an extensible, scaling component-entity system for a multiplayer hack-and-slash roguelike arena game.


##### Installation

Grab a binary from the releases page!

## Usage

Roll up a character, join a party, and fight for glory!

    $ java -jar a_game-0.0.1-standalone.jar [obvious args here]

## Options

There are many options.

## Examples
In order to create an entity:
  ```a-game.core=> (def foo (actors.agent/_entity))
     #'a-game.core/foo
     a-game.core=> foo
     ["a91cd14b-a686-48eb-945f-7d2f8b4aef85" #{}]
     a-game.core=> (def a_po #{foo, bar})
     #'a-game.core/a_po
     a-game.core=> a_po
     #{["a91cd14b-a686-48eb-945f-7d2f8b4aef85" #{}] ["6852888c-7c28-471c-b1ea-537502106aa7" #{}]}
```

...

### Bugs
- it doesn't work.
...

## License

Copyright © 2017 Brendan Reddy-Best

Distributed under the Eclipse Public License either version 1.0 or any later version.
