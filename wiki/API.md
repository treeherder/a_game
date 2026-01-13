# API Reference

High-level APIs

- `create_stage(label, agent_pool, item_pool, mapfile, coordgrids, rules)` — returns an `atom` of the stage map.
- `add-entity(world-atom, entity)` — add an entity with `:id` into `:entities`.
- `add-agent(world-atom, agent)` — add an agent into `:agent_pool`.
- `apply-event(world, event)` — pure function returning a new world map.
- `commit-event(world-atom, event)` — apply and swap into an atom.

See source code for details and examples.
