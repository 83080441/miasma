# Plan de port: Warp Entity (NeoForge 26.2)

Documento vivo para replicar este prototipo (`sdfg` / `dev.sdfg.mod`) en el **proyecto original**.
Cada feature nueva pedida en el chat debe actualizar este plan (sección Changelog + pasos afectados).

**Stack de referencia:** NeoForge 26.2 / Minecraft 26.2 / Java 25 / EntityRenderState + `SubmitNodeCollector`.

**Identidad del repo:** `mod_id=sdfg`, `mod_group_id` / paquete Java `dev.sdfg.mod` (el modId de Minecraft no admite puntos).

**Nombre oficial:** la entidad y todo el sistema se llaman **Warp** (no Lens).

---

## 1. Objetivo del feature

Entidad invisible al jugador (pero pickable para F3+B) que:

- Ocupa **0.5×0.5** y se **snapea al centro del bloque** `(x+0.5, y+0.5, z+0.5)`.
- Deforma el espacio detrás (warp UV radial) **sin PNG**.
- Tiene **Distortion 1–100** (NBT + synched data).
- Tiene **Subtype**: `static` | `vortex` | `binary` | `amoeba` | `virus`.
- Emite partículas `warp_mote` violeta pálido (`#4D4D6B`) desde el borde del pozo (**12 bloques**); cantidad y velocidad escalan con **`Force`** (no Distortion).
- **Efecto Eco:** cerca del Warp (~16 bloques), volumen de categorías (incluye música) + pitch hacia abajo según Distortion × proximidad. No afecta UI.
- **Gravedad:** todos los subtypes **excepto `static`** atraen ítems, jugadores y mobs en **12 bloques**. **`Force` 1–100**. Al tocar: ítems se destruyen; living reciben daño mágico **1 vez/seg** = **`Force/10` HP** (½ corazón por cada 10 de Force).

Summon de prueba:

```mcfunction
/summon <modid>:entity1 ~ ~ ~ {Distortion:80,Subtype:"vortex",Force:25}
```

Nombre mostrado: **Warp** (`entity.<modid>.entity1`).

---

## 2. Mapa de archivos (copiar / adaptar)

Paquete ya alineado: `dev.sdfg.mod` / namespace `sdfg`. Si el destino usa otro id, renombrar desde ahí.

### Common (servidor + cliente)

| Origen en este repo | Rol |
|---|---|
| `entity/WarpEntity.java` | Entidad, Distortion, Subtype, snap, pickable; item pull/consume (no-static) |
| `entity/WarpSubtype.java` | Enum de subtipos |
| `entity/ModEntities.java` | DeferredRegister: solo `entity1` → `WarpEntity` |
| `particle/ModParticles.java` | DeferredRegister: `warp_mote` (`SimpleParticleType`) |
| Registro en clase principal del mod | `ENTITY_TYPES.register(bus)` + `PARTICLE_TYPES.register(bus)` |

### Client-only

| Origen | Rol |
|---|---|
| `client/WarpSceneCapture.java` | Copia el framebuffer a textura `textures/misc/warp_capture` |
| `client/WarpRenderHelper.java` | Mesh UV warp + lógica por subtype |
| `client/WarpRenderer.java` | EntityRenderer + binary orbit 3D |
| `client/WarpRenderState.java` | Estado de render (distortion, subtype, UV center, `warpScale`, offsets) |
| `client/WarpClientParticles.java` | Tick: motes desde PULL_RANGE; count/speed × Force |
| `client/WarpMoteParticle.java` | Homing mote color `#4D4D6B` |
| `client/WarpEchoState.java` | Intensidad Eco + `updateCategoryVolume` (rango 16; incluye MUSIC) |
| `client/WarpEchoSoundInstance.java` | Wrapper pitch (tickable-aware); volumen vía categorías |
| `client/WarpEchoSounds.java` | `PlaySoundEvent` + `PlayLevelSoundEvent` (pitch); skip UI |
| `WarpCommands.java` | `/warpkill` y `/killwarps` — elimina todos los Warp |
| Cliente del mod | Register renderer, particle provider, captura AfterSky / AfterOpaque |

### Assets

| Origen | Rol |
|---|---|
| `assets/<modid>/particles/warp_mote.json` | Sprites `minecraft:generic_0`…`generic_7` |
| `assets/<modid>/lang/*.json` | `entity.<modid>.entity1` = `"Warp"` |

**No hace falta** textura PNG propia del warp (se eliminó a propósito).

---

## 3. Checklist de port (orden recomendado)

### Fase A — Entidad

1. Crear `WarpSubtype` con los 5 ids de string.
2. Crear `WarpEntity`:
   - `sized(0.5F, 0.5F)`, `eyeHeight(0.25F)`, `MISC`, fireImmune.
   - Synched: `Distortion` int 1–100 (default 50), `Subtype` ordinal.
   - NBT: `Distortion` (int), `Subtype` (string id).
   - `snapToBlockCenter()` en `onAddedToLevel` / primer `tick`.
   - `isPickable() = true` (hitbox F3+B).
   - `noPhysics`, `setNoGravity(true)`, no hurt, ignore pistons.
3. Registrar `entity1` → `WarpEntity` en DeferredRegister y en el bus del mod.
4. Lang: `entity.<modid>.entity1` = `"Warp"`.

### Fase B — Captura de escena + warp

5. Portar `WarpSceneCapture` (AbstractTexture + copy del MainTarget).
6. En client setup: `ensureRegistered()`.
7. Eventos:
   - `RenderLevelStageEvent.AfterSky` → `beginFrame()`
   - `RenderLevelStageEvent.AfterOpaqueBlocks` → `captureThisFrame()`
8. Portar `WarpRenderState` + `WarpRenderer` + `WarpRenderHelper`.
9. Registrar renderer en `EntityRenderersEvent.RegisterRenderers`.
10. Verificar: warp visible, sin PNG, fuerza escala con Distortion.

### Fase C — Subtipos visuales

Implementados en renderer/helper (misma entidad):

| Subtype | Comportamiento esperado |
|---|---|
| `static` | Disco convexo estático (**sin** gravedad de ítems) |
| `vortex` | Giro + pull al centro + atracción de ítems |
| `binary` | 2 nodos al 20% del bloque, órbita 3D + separación pulsante + ítems |
| `amoeba` | Borde irregular mutante + ítems |
| `virus` | Spikes que salen/entran + pull + ítems |

### Fase D — Partículas

11. Registrar `warp_mote` + JSON de sprites.
12. `RegisterParticleProvidersEvent` → `WarpMoteParticle.Provider`.
13. `WarpClientParticles` en `ClientTickEvent.Post`:
    - Solo warps cerca del jugador (~48 bloques).
    - Spawn en esfera = `PULL_RANGE` (12).
    - Count/speed × **Force** (no Distortion); color `#4D4D6B`.
14. **Importante:** no usar `DustParticle` / `ParticleTypes.DUST` — randomizan velocidad. Usar partícula custom con homing al target.

### Fase E — UX de desarrollo (opcional)

15. En `build.gradle` run client: `--quickPlaySingleplayer`, `'test'` si el mundo se llama así.

### Fase F — Efecto Eco (audio)

16. Portar `WarpEchoState`: `getEntitiesOfClass` en AABB, `MAX_RANGE = 16`, `intensity = (distortion/100) * proximity` (lineal).
17. Cada tick: `SoundManager.updateCategoryVolume` en todas las categorías salvo `MASTER`/`UI` → gain `lerp(1, 0.05, intensity)` (al lado del Warp se oye ~5%).
18. Portar `WarpEchoSoundInstance` (solo pitch: `lerp(1, 0.55, intensity)`).
19. `PlaySoundEvent` (wrap, skip UI) + `PlayLevelSoundEvent` client (pitch para path de `Level.playSound`).
20. **Nota 26.2:** no hay low-pass OpenAL/EFX; volumen = category gain; pitch = wrapper / level event.

### Fase G — Comandos

21. Registrar `/warpkill` y `/killwarps` (permiso gamemaster): `entity.kill` en todos los `WarpEntity` de mundos cargados.
22. Alternativa vanilla: `/kill @e[type=<modid>:entity1]`.

### Fase H — Gravedad (ítems + living)

23. En `WarpEntity.tick` (servidor), si subtype ≠ `static`:
    - Synched/NBT **`Force` 1–100** (default 25); factor = Force/100.
    - Rango `PULL_RANGE = 12`.
    - **Ítems:** accel `(0.025 + 0.55 * proximity²) * force`; consume + log al tocar.
    - **Jugadores/mobs:** pull sin damp de input; accel ≤ `0.10` (&lt; walk ≈0.216) para poder escapar caminando; daño al tocar igual.
24. Por ahora **no** hay inventario/loot — solo destroy + log en ítems. `Force` lista para mecánicas (pull + daño).

---

## 4. Decisiones técnicas a respetar

- **Nombre oficial Warp** en clases, ids de partícula/textura, lang y comentarios. No reintroducir “Lens”.
- **Una sola entity type** (`entity1`); los estilos son NBT `Subtype`, no entidades distintas.
- Warp visual = sample del framebuffer + distorsión UV; **sin glow PNG**.
- Capture puede fallar mid-pass: conservar último frame válido (`hasCapture`).
- Binary: escala nodo `0.2`, radio órbita `~0.25`, tumble 3D, no random por frame (usar seed de posición/`entityId`).
- Partículas: atracción cada tick + fade in/out; remove cerca del centro (`dist < 0.12`).
- Campo de escala en render state: `warpScale` (no `lensScale`).
- Textura de captura: `textures/misc/warp_capture`.
- Eco: category gains (incluye MUSIC) + pitch; no tocar UI; sin EFX low-pass en 26.2.
- Comando: `/warpkill` / `/killwarps`.
- Gravedad: pull 12 bloques; living **escapable a pie** (pull ≤0.10, sin damp); ítems se consumen; touch damage `Force/10` HP/s; static no.
- APIs 26.2: `ValueInput`/`ValueOutput`, `SubmitNodeCollector`, `EntityRenderState`, `SingleQuadParticle.Layer`, `PlaySoundEvent`, `PlayLevelSoundEvent`, `updateCategoryVolume`.

---

## 5. Pruebas mínimas al portar

```mcfunction
/summon <modid>:entity1 ~ ~ ~ {Distortion:10,Subtype:"static"}
/summon <modid>:entity1 ~ ~ ~ {Distortion:100,Subtype:"vortex",Force:25}
/summon <modid>:entity1 ~ ~ ~ {Distortion:70,Subtype:"binary",Force:100}
/summon <modid>:entity1 ~ ~ ~ {Distortion:60,Subtype:"amoeba",Force:40}
/summon <modid>:entity1 ~ ~ ~ {Distortion:80,Subtype:"virus",Force:15}
```

Checklist visual:

- [ ] Hitbox 0.5 en centro de bloque (F3+B)
- [ ] Nombre de entidad **Warp**
- [ ] Distorsión más fuerte con Distortion alto
- [ ] Cada subtype se ve distinto
- [ ] Binary baila en 3D dentro del bloque
- [ ] Motes violeta `#4D4D6B` desde ~12 bloques al centro
- [ ] Más Force → más/más rápidas partículas
- [ ] Distortion 100 cerca → música/bloques/pasos amortiguados y graves
- [ ] >16 bloques del Warp → audio normal
- [ ] UI sin cambio
- [ ] `/warpkill` elimina todos los Warp
- [ ] Vortex/binary/amoeba/virus atraen ítems, jugadores y mobs en 12 bloques
- [ ] Static **no** atrae
- [ ] Al tocar: ítem desaparece; living reciben daño cada 1s (Force 50 → 2.5♥/s; Force 100 → 5♥/s)

---

## 6. Referencia rápida de clases en este repo

```
src/main/java/dev/sdfg/mod/
  ExampleMod.java
  ExampleModClient.java
  WarpCommands.java            # /warpkill /killwarps
  entity/
    WarpEntity.java
    WarpSubtype.java
    ModEntities.java
  particle/
    ModParticles.java
  client/
    WarpSceneCapture.java
    WarpRenderHelper.java
    WarpRenderer.java
    WarpRenderState.java
    WarpClientParticles.java
    WarpMoteParticle.java
    WarpEchoState.java
    WarpEchoSoundInstance.java
    WarpEchoSounds.java
src/main/resources/assets/sdfg/particles/warp_mote.json
```

---

## Documentación comercial

- `plan/WARP_README.md` — tipos de Warp y qué hacen (tono comercial, sin detalle técnico de port).
- `plan/archify/warp-architecture.html` — mapa Archify interactivo (nodos SRC → archivos del mod).
- Skill Archify: `.agents/skills/archify` (instalado desde [tt-a1i/archify](https://github.com/tt-a1i/archify)).

---

## Changelog del plan

| Fecha | Cambio pedido | Impacto en el plan |
|---|---|---|
| 2026-09-22 | Primer commit en GitHub `83080441/miasma` (historial limpio) | remote + identidad `dev.sdfg.mod`/`sdfg` |
| 2026-09-22 | Compilar como `dev.sdfg.mod` | `mod_group_id`/`package`=`dev.sdfg.mod`; `mod_id`=`sdfg`; assets `assets/sdfg/` |
| 2026-09-22 | Prep GitHub: `.cursor/` y `net/` solo locales | `.gitignore` + untrack `.cursor`; `net/` ya ignorado |
| 2026-09-21 | Extraer shape/sprites del caldero vanilla a `plan/` | `plan/cauldron/` (JSON + PNG/JPG) |
| 2026-09-20 | Implementar Archify para ver arquitectura → archivos | Skill en `.agents/skills/archify`; `plan/archify/warp-architecture.{json,html}` |
| 2026-09-20 | Partículas más lentas; gravedad living &lt; walk speed | mote cap 0.045; living pull ≤0.10 sin damp |
| 2026-09-20 | Partículas × Force + color `#4D4D6B`; spawn = 12 | `WarpClientParticles` / `WarpMoteParticle` |
| 2026-09-20 | Daño al tocar: 1/s, Force/10 HP (½♥ por 10 Force) | `tryTouchDamage`; magic damage |
| 2026-09-20 | Gravedad también en jugadores y mobs | `pullLivingEntities`; ×0.65; sin consume |
| 2026-09-20 | Force 1–100 para gravedad de ítems (default 25, lento) | NBT `Force`; 100 = tirón original |
| 2026-09-20 | Gravedad de ítems (12 bloques, no-static); destroy + log | Fase H; `WarpEntity.pullAndConsumeItems` |
| 2026-09-20 | Eco al 5% de volumen al lado del Warp | `MIN_VOLUME_GAIN = 0.05` |
| 2026-09-20 | Eco no se oía + comando eliminar Warps | Eco vía category gains (MUSIC sí); rango 16; `/warpkill`; Fase G |
| 2026-09-20 | Efecto Eco (audio amortiguado según Distortion) | Fase F; `WarpEchoState` / `WarpEchoSoundInstance` / `WarpEchoSounds` |
| 2026-09-20 | README comercial de los Warp (tipos y efecto) | Añadido `plan/WARP_README.md` |
| 2026-09-20 | Renombrar Lens → **Warp** en todo el proyecto | Nombre oficial Warp; clases, `warp_mote`, `warp_capture`, `warpScale`, lang |
| 2026-09-20 | Crear carpeta `plan/` y documento de port | Documento inicial con estado del prototipo |

<!-- Las filas nuevas van arriba de la tabla al actualizar -->
