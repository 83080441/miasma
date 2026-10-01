# Descubrimiento

Un jugador nuevo **no conoce** los elementos.
Los aprende mirando un nodo Warp con el **catalejo** de Minecraft (`minecraft:spyglass`), clic derecho sostenido.

Cada mirada desbloquea **un** elemento: el más alto del nodo. El siguiente sale de otro nodo.

---

## En una frase

El catalejo enseña el primario del Warp, y en el núcleo aparece un cuadrado de ese color.

---

## Qué se desbloquea

Los básicos son los 8 del catálogo (`fire` … `aether`).

El **más alto** es el primario del nodo: mayor cantidad. Si empatan, gana el número más bajo.

| El nodo tiene | Se descubre |
|---------------|-------------|
| `fire 80, water 20` | fire |
| `earth 40, wind 40` | earth (número más bajo) |
| un primario que ya conocés | nada nuevo |

El set es del jugador. Empieza vacío en un mundo nuevo, se guarda, y **no se pierde al morir**.

Conocer uno no enseña el resto de la mezcla de ese nodo.

---

## Visual

La forma del Warp no cambia. Si el catalejo apunta a **ese** nodo (hasta 48 bloques):

- Un cuadrado de **0.20** bloques en el centro.
- Color del primario.
- Gira y se mueve un poco alrededor del núcleo.
- En Binary queda entre los dos núcleos.

Si el elemento ya estaba descubierto, el cuadrado igual se ve.

---

## Aviso

La primera vez, la barra de acción dice el nombre. Una sola vez por elemento.

---

## Comando

`/element unlock <nivel>` (permiso de comandos).

| Nivel | Qué desbloquea |
|-------|----------------|
| **1** | Las partículas de ahora: los 8 del catálogo |
| 2, 3, … | Reservados. Hoy responden que ese nivel todavía no existe |

El nivel solo incluye los elementos de ese catálogo. Cuando exista el 2, `/element unlock 1` sigue siendo únicamente los de ahora.

Marca esos elementos como conocidos (el casco los muestra con icono y número) y otorga su logro de la pestaña de progreso. Es del jugador que ejecuta el comando. Persiste igual que un descubrimiento con el catalejo.

---

## Fuera de alcance

- El casco revelador muestra icono y número solo de los elementos ya conocidos. El resto sale ofuscado.
- No se desbloquea el segundo o tercero del mismo nodo.
- No hay libro ni menú de elementos descubiertos.
