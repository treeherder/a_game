# Events & Networking

We use an event-log style model for serverless multiplayer.

- Events are plain maps with a `:type` key. Example: `{:type :add-entity :entity <entity-map>}`.
- Serialize events with `stage.environment/event->json` and deserialize with `json->event`.
- Use `commit-event` to apply deserialized events to the local world atom.

Networking: exchange events (not full state). For joining peers, send a snapshot via `snapshot`.
