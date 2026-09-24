# Warp

Un punto en el espacio que **dobla lo que hay detrás**.
Sin texturas decorativas. Solo distorsión pura.

Cada Warp ocupa medio bloque, anclado al centro. Su fuerza se controla con **Distortion** (1–100).

---

## Tipos

### Static
El clásico. Una lente convexa quieta que curva la escena hacia el centro. Ideal cuando querés presencia sin movimiento. **No** atrae objetos.

### Vortex
Gira y **jala** el espacio hacia adentro. El remolino se siente más fuerte cuanto más alta sea la Distortion. Los ítems del suelo **caen hacia él**.

### Binary
Dos núcleos pequeños orbitan dentro del mismo bloque. Bailan en 3D, se acercan y se separan — un sistema dual vivo, no un disco plano. También **absorbe** ítems cercanos.

### Amoeba
Una mancha irregular que muta. El borde respira, se deforma y nunca se repite igual. Distorsión orgánica, casi biológica. **Traga** lo que se acerque.

### Virus
Núcleo compacto con **picos** que salen y se retraen. Cada spike late a su ritmo; el espacio se tensa hacia el centro. Los objetos **desaparecen** al tocarlo.

---

## En una frase

| Tipo | Sensación |
|------|-----------|
| **Static** | Lente quieta (sin atracción) |
| **Vortex** | Remolino que absorbe |
| **Binary** | Dos ojos bailando |
| **Amoeba** | Blob vivo |
| **Virus** | Corona de picos |

---

## Gravedad

Todos excepto Static: pozo de **12 bloques** que tira de **ítems, jugadores y mobs**. **Force** (1–100) controla tirón y daño. El tirón sobre vos siempre es **más débil que caminar**, así que podés salir a pie. Al tocar el Warp, los **ítems desaparecen**; vos y los mobs recibís daño **cada segundo**: **medio corazón por cada 10 de Force** (Force 50 → 2½♥/s; Force 100 → 5♥/s). Si el Warp **mata un mob**, emite una **onda oscura** que cae y se esparce lento por el piso (~14 bloques); los mobs que toca reciben **7 corazones**. Si mueren por la onda, sale **otra onda** en cadena.

Al **absorber un ítem** o **matar un mob**, el Warp gana **+1** de cada elemento que tenía la presa (según datapack), hasta **100** por elemento. Al matar un mob también suma Force = vida máxima de la víctima.

---

## Efecto Eco

Acercate y el mundo **casi se apaga** (~5% de volumen). Pasos, bloques, viento, animales y música: amortiguados y afinados hacia abajo, como bajo el agua. Cuanto más alta la Distortion y más cerca estés, más denso el silencio.

La UI del menú queda intacta.

---

## Cómo se siente

- **Distortion baja** → susurro visual, apenas ahí.
- **Distortion alta** → el mundo se rompe hacia el Warp; el audio se ahoga.
- **Force alta** → más motes violeta pálido caen hacia el centro, mismo ritmo que el pozo.

El Warp no es un modelo. Es un **agujero en la percepción**.

---

## En el mundo

Aparecen **solo al generar terreno** (Overworld), **2–3 bloques sobre el piso** (si hay aire), y son **escasos**.

La posición XZ (y el tipo / Distortion / Force) sale de una **fórmula fija con la seed** del mundo: misma seed → mismos sitios. Si un Warp se destruye, **no vuelve a spawnear** en esa ubicación.
