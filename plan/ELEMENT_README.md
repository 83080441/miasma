# Element

Una **capa de identidad** del mod.
No es un bloque ni una entidad. Es la etiqueta con la que el contenido —bloques, ítems, criaturas, efectos— dice *de qué está hecho el mundo*.

Cada elemento tiene un **id** estable (`fire`, …) y un **número** (1–8). Las cantidades son enteros: *1 de fire, 2 de water*.

Sin combate ni matriz de debilidades todavía.

---

## Catálogo

| # | Id | Nombre | Sensación |
|---|----|--------|-----------|
| **1** | fire | Fuego | Calor, combustión, agresión |
| **2** | water | Agua | Fluidez, erosión, adaptación |
| **3** | earth | Tierra | Masa, anclaje, resistencia |
| **4** | wind | Viento | Movimiento, dispersión, alcance |
| **5** | light | Luz | Revelación, pureza visible |
| **6** | darkness | Oscuridad | Ocultación, vacío perceptivo |
| **7** | miasma | Miasma | Corrupción / contaminación del mundo |
| **8** | aether | Aether | Rareza trascendente; contrapunto etéreo al miasma |

---

## En una frase

| Elemento | En una frase |
|----------|--------------|
| **Fire** | Quema y empuja |
| **Water** | Fluye y desgasta |
| **Earth** | Sostiene y pesa |
| **Wind** | Lleva y dispersa |
| **Light** | Muestra lo que estaba oculto |
| **Darkness** | Esconde y vacía |
| **Miasma** | Contamina el mundo |
| **Aether** | Trasciende lo material |

---

## Cantidades (`ElementAmounts`)

Un contenido puede llevar **varios** elementos a la vez. Cada uno tiene una cantidad **1–100** (0 = ausente).

Ejemplo: `fire 40, water 12` → código `1:40,2:12`.

El **primario** es el de mayor cantidad (empate → número más bajo).

---

## Warps

Cada nodo Warp tiene su propia mezcla elemental.

Al generar el mundo (misma seed → mismos valores): **1–3** elementos distintos, cada uno con cantidad **1–100**.

## Casco revelador

Con el casco puesto **y agachado (shift)**:

1. **Elementos** (bajo el crosshair): icono 16×16 y cantidad debajo; solo si > 0.
2. **Debug** (texto a la derecha).

Sin agacharse, el casco no muestra nada (juego normal).

Origen de los elementos:

| Mirás | Fuente |
|-------|--------|
| Warp | mezcla del nodo |
| Mob | datapack `element/entities/...` |
| Ítem en el suelo | datapack `element/items/...` |
| Bloque bajo el crosshair | datapack del ítem de ese bloque |

No revela el ítem de la mano. Sin agacharse, no muestra nada.

---

## Datapack (ítems y mobs)

Defaults editables sin recompilar. Sin archivo = vacío.

### Rutas

- Ítems: `data/sdfg/element/items/<namespace>/<path>.json`  
  → id `namespace:path` (ej. `items/minecraft/torch.json` → `minecraft:torch`)
- Entidades: `data/sdfg/element/entities/<namespace>/<path>.json`  
  → ej. `entities/minecraft/zombie.json` → `minecraft:zombie`

### Formato

```json
{
  "elements": {
    "fire": 40,
    "light": 15
  }
}
```

Solo keys con cantidad **1–100**. Ids de elemento desconocidos se ignoran (warning en log).

### Ejemplos / cobertura

Hay definiciones generadas para **casi todos los bloques** (ítem de bloque) y **mobs** vanilla:

- `element/items/minecraft/*.json` (~725)
- `element/entities/minecraft/*.json` (~90 mobs; sin botes, minecarts ni proyectiles)

Cantidades heurísticas (tierra en piedra, fuego/luz en antorchas, water en acuáticos, etc.). Para regenerar tras un update de MC:

```bash
node tools/generate_element_datapack.js
```

(requiere fuentes extraídas en `net/minecraft/...` desde el jar `-sources`, ver el script).

Podés editar a mano cualquier JSON; el generador sobrescribe al correrlo.

### API

```java
ElementLookup.of(stack);           // ItemStack
ElementLookup.of(Items.TORCH);     // Item
ElementLookup.of(zombie);          // Entity
ElementLookup.of(EntityType.ZOMBIE);
```

Los Warps llevan `ElementAmounts` en la entidad. Al absorber un ítem o matar un mob ganan **+1** de cada elemento que tenía la presa (vía `ElementLookup`), cap 100.

---

## Cómo se usa (API)

- `Element.number()` / `Element.byNumber(int)`
- `ElementAmounts` — get/set/add, encode/decode, `CODEC`, `randomForWarp`
- `ElementHolder` — `getElement()`, `getElementAmounts()`, `of(...)`
- `ElementLookup` / `ElementDefinitions` — defaults datapack

---

## Fuera de alcance (por ahora)

- Matriz de afinidades u opuestos
- Daño elemental, armaduras, pociones
- Tags `#sdfg:element/...`
- DataComponent / Attachment por instancia
- Ítems que no son bloque (espadas, comida, etc.) — se pueden agregar igual bajo `element/items/`
