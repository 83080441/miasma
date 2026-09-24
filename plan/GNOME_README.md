# Gnome

Una criatura **pacífica** del mod.
Pequeña, frágil, con modelo propio exportado desde Blockbench (base husk) y skin 64×64.

---

## En una frase

Un gnomo tímido que no ataca, aguanta poco y no deja botín.

---

## Stats

| Atributo | Valor |
|----------|--------|
| Vida | **3** HP (1½ corazones) |
| Temperamento | Pacífico (Creature) |
| Drops | **Ninguno** |
| Nombre | Siempre visible: **Gnome** |

---

## Visual

- Modelo: `GnomeModel` (mesh Blockbench / husk convertido)
- Textura: `assets/sdfg/textures/entity/gnome.png` (64×64)
- Renderer: `GnomeRenderer` → `MobRenderer` + layer `sdfg:gnome`

---

## Esencia (Element)

Definición datapack: `data/sdfg/element/entities/sdfg/gnome.json`

| Elemento | Cantidad |
|----------|----------|
| earth | 35 |
| aether | 25 |
| wind | 15 |

El casco revelador (shift) la muestra como cualquier otro mob.

---

## Cómo aparece

Por ahora **solo summon**:

```
/summon sdfg:gnome
```

Sin spawn natural ni huevo creativo todavía.

---

## Código

- `GnomeEntity` — PathfinderMob pacífico
- `ModEntities.GNOME` — registro
- `GnomeModel` — mesh Blockbench
- `GnomeRenderer` — textura + animación básica de caminar
