#### To Do:
------
1. Make actionable plan for achievable goals.
2. Maps
    1. Parse
    2. Generate
    3. Represent

  3. Re-Document codebase
  4. ????
  5. Profit. 

- consider doing this entirely with types?


##### Code:
1. A single tile in a stage:
`{:coordinates (XYZ), :kind TILE_SET_TYPE  :exists bool, :passable bool, :destructable bool :contains TILE_CONTENTS_TYPE :occupied/by bool/list }`
   1. After ascertaining that the tile exists,  the engine must reference the type, contents, and occupants before attempting to render it.
     1. The tile type, contents, and occupants must all be collections of the appropriate entites, which in turn must all obey the rules set in the stage.  The game engine would thus be responsible for building the individual elements and negotiating their inter-relationships.
       1.  TILE_SET_TYPE -> {tileset_theme, tile_id}   
