# Tuberías

La red que saca elementos del caldero.
La tapa arranca el camino, la tubería lo lleva y el contenedor lo recibe.

Las texturas y formas son de Minecraft, a propósito, hasta que haya modelos propios.

---

## En una frase

Una tolva sobre el caldero elige un elemento al azar, lo saca y lo empuja por tubos de hierro hasta un bloque de vidrio.

---

## Piezas

| Bloque | Id | Se ve como | Qué hace |
|--------|----|------------|----------|
| Tapa del caldero | `sdfg:cauldron_lid` | Tolva, pico hacia abajo | Se coloca solo encima del caldero. Es el inicio de la red. |
| Tubería | `sdfg:element_pipe` | Tubo de hierro de 4 píxeles | Une la tapa con el contenedor. También se une a otras tuberías. Siempre deja pasar. |
| Tubería de paso | `sdfg:element_valve` | Tubo con núcleo de cobre o de redstone | Sección con estados **abierta** y **cerrada**. Cerrada, el elemento no cruza. |
| Contenedor | `sdfg:element_container` | Vidrio con el líquido dentro | Guarda lo que llega. Hasta **100** de cada elemento. El nivel y el color se ven en el bloque. |

Están en la pestaña del mod.

| Locale | Tapa | Tubería | Tubería de paso | Contenedor |
|--------|------|---------|----------------|------------|
| es | Tapa del caldero | Tubería | Tubería de paso | Contenedor |
| en | Cauldron Lid | Pipe | Valve Pipe | Container |

---

## Cómo se arma

1. Caldero con elementos dentro (agua, ítems absorbidos, o ambos).
2. Tapa en el bloque de encima. Si el caldero desaparece, la tapa se cae.
3. Tuberías en cualquier dirección, pegadas a la tapa, entre sí o al contenedor.
4. Un contenedor de vidrio en algún punto de esa red.

La tubería muestra un brazo hacia cada vecino que sea tapa, otra tubería, una tubería de paso o contenedor. Sola, se ve solo el cubo del centro.

---

## Tubería de paso

Se coloca en la línea como cualquier tubería. Empieza **cerrada**.

Clic derecho la alterna:

| Estado | Núcleo | Paso |
|--------|--------|------|
| Cerrada | Cubo de redstone, más grande | Esa sección no deja pasar. El resto de la red sigue, pero el camino se corta ahí. |
| Abierta | Cubo de cobre, del tamaño del tubo | Deja pasar igual que la tubería de hierro. |

Cerrada o abierta, los brazos siguen conectados a los vecinos. Lo que cambia es si el elemento puede cruzar ese bloque. Al cerrar suena una trampilla de hierro.

El contenedor puede estar pegado a la tapa, sin tubos en el medio.

Si hay varios contenedores, llega al más cercano. El camino no atraviesa otro caldero ni otra tapa. Como máximo recorre **64** bloques de red.

---

## Qué mueve

Cada **segundo** (20 ticks), si el camino llega a un contenedor y el caldero no está vacío:

- Elige **un** elemento al azar entre los que hay. Misma chance para cada tipo, sin importar la cantidad.
- Mueve **solo ese**. El resto se queda en el caldero.
- Si el contenedor tiene sitio, pasa la cantidad entera. Si no cabe (tope 100), pasa lo que entra y el sobrante se queda.
- Si ese elemento ya está a 100 en el contenedor, ese segundo no mueve nada.

Cuando el caldero se queda sin elementos, se desellla y el agua visual baja a vacío.

Al mover, suena el soporte de pociones y salen motes del color del elemento en la tapa y en el contenedor.

---

## Contenedor

Vacío es solo vidrio. Cuando entra un elemento, aparece agua dentro, teñida con el color de la mezcla (igual que el caldero).

| Cantidad total | Altura |
|----------------|--------|
| 1–25 | Baja |
| 26–50 | Media |
| 51–75 | Alta |
| 76 o más | Lleno |

**100** de un solo elemento ya lo llena. Si luego llega otro, se queda lleno y el color pasa a ser la mezcla de los dos.

Clic derecho abre un menú con **un** espacio. Ahí entra el **vial** (`sdfg:vial`), una botella de vidrio con tapa de corcho. No tiene receta.

El vial saca como máximo **100** en total:

| En el contenedor | En el vial |
|------------------|------------|
| Solo un elemento, 100 o más | 100 de ese elemento |
| 100 agua, 100 tierra, 50 luz (100:100:50) | 40 agua, 40 tierra, 20 luz |
| Menos de 100 en total | Se lleva todo, en la misma proporción |

El vial vacío se queda vacío si el contenedor no tiene nada. El líquido del icono se tiñe con el color de la mezcla, pesado por la cantidad de cada elemento, igual que el cubo de un líquido puro. El texto del ítem lista lo que llevó. Al romper el vidrio, el vial que estaba en el menú cae.

---

## Casco revelador

Con el casco y agachado:

- Mirar el caldero, o la tapa encima, muestra lo que sigue en el cuenco.
- Mirar el vidrio muestra lo que ya llegó.

---

## Archivos

| Pieza | Código |
|-------|--------|
| Tapa | `block/CauldronLidBlock.java`, `block/CauldronLidBlockEntity.java` |
| Tubería | `block/ElementPipeBlock.java` |
| Tubería de paso | `block/ValvePipeBlock.java` |
| Contenedor | `block/ElementContainerBlock.java`, `block/ElementContainerBlockEntity.java` |
| Vial | `item/VialItem.java`; menú `inventory/VialMenu.java` |
| Camino | `block/PipeNetwork.java` |
| Sacar del caldero | `HeatedCauldronBlockEntity.drain` |
| Modelos | `assets/sdfg/models/block/element_pipe_core.json`, `element_pipe_arm.json`, `element_pipe_inventory.json` |

La tapa usa el modelo `minecraft:block/hopper`. El contenedor es vidrio con `water_still` teñido por `ElementContainerTint`. La tubería usa la textura `minecraft:block/iron_block`.
