# Líquido

El **elemento puro**, en forma de fluido.
Hay uno por cada id del catálogo: fire, water, earth, wind, light, darkness, miasma, aether.

Cada uno lleva **100** de su elemento y nada más. No es una mezcla.

El cuerpo es agua. Se ven iguales entre sí. Lo que los separa son las **partículas** del color de ese elemento.

---

## Catálogo

| # | Id | Cubo | Líquido |
|---|----|------|---------|
| **1** | fire | `sdfg:fire_bucket` | Fuego puro |
| **2** | water | `sdfg:water_bucket` | Agua pura |
| **3** | earth | `sdfg:earth_bucket` | Tierra pura |
| **4** | wind | `sdfg:wind_bucket` | Viento puro |
| **5** | light | `sdfg:light_bucket` | Luz pura |
| **6** | darkness | `sdfg:darkness_bucket` | Oscuridad pura |
| **7** | miasma | `sdfg:miasma_bucket` | Miasma puro |
| **8** | aether | `sdfg:aether_bucket` | Aether puro |

El fluido del elemento water **no** es el agua vanilla. Solo se parece.

---

## En una frase

Agua de Minecraft, con motes del color del elemento mientras el bloque existe.

---

## Cuerpo

Misma textura, forma y flujo que el agua. Sin tinte en el líquido.

- Fluye como el agua.
- Es **finito**: un source no genera océanos infinitos.
- Source y flowing se ven igual de transparentes y azules que el agua.

---

## Partículas

Mientras el bloque de líquido está colocado (source **y** flowing), emite motes del color del elemento.

Eso es lo único que los distingue. Sin las partículas, los ocho son agua.

El color vive en el enum `Element`, alineado con el icono 16×16 de `assets/sdfg/textures/gui/element/<id>.png`.

Oscuridad usa un violeta un poco más claro que el centro del icono, para que el mote se lea sobre el agua.

| Id | Color | Notas |
|----|-------|-------|
| fire | `#FF8C1A` | naranja de la llama |
| water | `#3D9BE8` | azul del icono |
| earth | `#8B5E3C` | marrón |
| wind | `#E6D3C4` | beige claro |
| light | `#FFF4A3` | amarillo pálido |
| darkness | `#6B3A9A` | violeta legible sobre agua |
| miasma | `#7A6488` | púrpura apagado |
| aether | `#22E7FF` | cian eléctrico |

---

## Cubo

Cada líquido se coloca y se recoge con **su** cubo. Un cubo vacío contra un source lo recoge; el cubo lleno lo vuelve a colocar.

Los ocho cubos están en el tab de ítems del mod (`itemGroup.sdfg`, pestaña `example_tab`).

| Locale | Ejemplo |
|--------|---------|
| es | Fuego puro, Agua pura, Tierra pura, Viento puro, Luz pura, Oscuridad pura, Miasma puro, Aether puro |
| en | Pure Fire, Pure Water, Pure Earth, Pure Wind, Pure Light, Pure Darkness, Pure Miasma, Pure Aether |

Esencia del cubo (y del líquido colocado): **100** de ese elemento, para el casco revelador.

---

## Fuera de alcance (por ahora)

- Recetas
- Generación en el mundo
- Calderos y botellas
- Mezcla entre elementos
- Daño elemental
