# Pergaminos

El pergamino dice qué hechizo se canaliza y qué elementos pide.
Por ahora hay uno, Bolt. Los que vengan se agregan igual, cada uno con su lista.

Se ve como la página en blanco de Minecraft, a propósito, hasta que haya un modelo propio. No tiene receta. Está en la pestaña del mod.

---

## En una frase

El primer pergamino de las casillas 1, 2 y 3 del acceso rápido es el que usa la varita.

---

## Bolt

| | |
|--|--|
| Id | `sdfg:scroll_bolt` |
| Nombre | Scroll: Rayo / Scroll: Bolt |
| Subtítulo | Scroll de los elementos, tierra, fuego, agua |
| Elementos, en ese orden | tierra, fuego, agua |

El subtítulo es gris y en cursiva. Los nombres salen de `element.sdfg.<id>`, no se escriben a mano en el ítem.

---

## Con la varita

La varita en la mano, clic derecho, y hay que mantenerlo **3 segundos**. Sin un pergamino en las casillas 1, 2 o 3, el clic no hace nada. Si hay más de uno, vale el de la casilla más baja.

Mientras dura, el jugador no camina, no salta y no lo empujan. Solo puede mirar. Soltar el clic, cambiar de ítem o soltar la varita corta el conjuro. Lo ya sacado de los bloques no vuelve, y no salen flechas.

El cubo es el 3×3×3 centrado en el bloque donde está parado.

Cada **segundo** resta **10** de cada elemento del pergamino. El orden de esos elementos sale al azar, y los 10 salen de bloques al azar del cubo. Un bloque suelta como mucho esos 10 y, si todavía le queda, sigue. Si se queda sin ningún elemento, pasa a aire y suelta motas de ese color hacia la varita. Lo gastado queda en el chunk y la fila de la varita muestra lo que queda.

Al cumplir los 3 segundos la absorción se detiene. Después salen las flechas, **una por elemento** y en el orden en que está escrito el pergamino, cada **medio segundo**, en un anillo alrededor del jugador, hacia donde miraba al terminar. En Bolt: tierra, luego fuego, luego agua. Cada una deja motas de su elemento. No se pueden recoger.

Si el clic sigue apretado, no empieza otra canalización. Hay que soltarlo y volver a apretar.

---

## Cómo se agrega otro

1. Un `Kind` nuevo en `ScrollItem`, con id estable y los elementos en el orden en que se muestran y se disparan.
2. `ScrollItem.register` en `ExampleMod`, y el ítem en la pestaña creativa.
3. `assets/sdfg/items/scroll_<id>.json`, por ahora `minecraft:item/paper`.
4. El nombre en `en_us` y `es_es`: `item.sdfg.scroll_<id>`. El subtítulo `item.sdfg.scroll.elements` ya se comparte.

El id del ítem no lleva puntos: `scroll_bolt`, no `scroll.bolt`.

---

## Archivos

| Pieza | Código |
|-------|--------|
| Pergamino | `item/ScrollItem.java` |
| Registro | `ExampleMod`, `sdfg:scroll_bolt` |
| Canalización | `item/WandChannel.java` |
| Varita | `item/WandItem.java` |
| Lo que queda en el bloque | `element/BlockResidue.java` |
| Flecha | `entity/ElementArrow.java` |
| Modelo | `assets/sdfg/items/scroll_bolt.json` |
