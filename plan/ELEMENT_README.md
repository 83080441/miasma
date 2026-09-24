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

Dos capas separadas al mirar un Warp:

1. **Elementos** (bajo el crosshair / nodo): icono 16×16 y debajo la cantidad; solo si > 0.
2. **Debug** (texto a la derecha): subtype, distortion, force, damage, pos, distance.

Iconos en `assets/sdfg/textures/gui/element/<id>.png`.

---

## Cómo se usa (API)

- `Element.number()` / `Element.byNumber(int)`
- `ElementAmounts` — get/set/add, encode/decode, `randomForWarp`
- `ElementHolder` — `getElement()`, `getElementAmounts()`, `of(...)`

Los Warps implementan `ElementHolder`. Otro contenido del mod puede hacer lo mismo.

---

## Fuera de alcance (por ahora)

- Matriz de afinidades u opuestos
- Daño elemental, armaduras, pociones
- Tags `#sdfg:element/...`

Las cantidades ya viven en los nodos; el resto del combate elemental vendrá después.
