# Morral — mochilas vivas para Paper

Plugin de mochilas para **Paper 26.3** (Java 25). Cada mochila guarda su contenido dentro del propio objeto,
así que puedes tirarla, guardarla en un cofre o regalarla y sus cosas van con ella. Sin resource pack:
las mochilas usan los modelos de los *bundles* vanilla.

## Qué la hace distinta

- **6 niveles con mejora sin perder nada**: Cuero (9) → Cobre (18) → Hierro (27) → Oro (36) → Diamante (45) → Netherita (54).
  La de Netherita brilla y resiste el fuego y la lava.
- **Parches cosibles**: módulos que se cosen arrastrando el parche sobre la mochila en tu inventario.
  Cada nivel tiene más huecos para parches (1, 1, 2, 2, 3, 4). Se descosen con clic derecho usando unas tijeras.
- **Peso**: si entre todas tus mochilas llevas demasiadas casillas ocupadas, caminas más lento (configurable).
- **Tintes**: combina la mochila con cualquier tinte para cambiar su color.
- **Vista previa en el lore**: barra de llenado, parches cosidos y los objetos más abundantes.

### Parches

| Parche | Efecto | Receta (sin forma) |
|---|---|---|
| Imán | Atrae objetos cercanos (agáchate para pausarlo). No atrae lo que tú tiras. | Cuero + Cuerda + Lingote de hierro + Redstone |
| Recolector | Lo que recoges va directo a la mochila si ya hay de ese objeto dentro. | Cuero + Cuerda + Tolva |
| Reabastecer | Cuando gastas el último bloque o comida de la mano, la rellena desde la mochila. | Cuero + Cuerda + Dispensador |
| Alma | La mochila no se pierde al morir. | Cuero + Cuerda + Fragmento de eco + Ojo de Ender |
| Compactador | Al cerrar, convierte cada 9 objetos en su bloque (lingotes, pepitas, trigo...). | Cuero + Cuerda + Pistón + Mesa de crafteo |
| Candado | Solo quien lo cosió puede abrir la mochila (o quitar el candado). | Cuero + Cuerda + Gancho de cuerda + Lingote de hierro |

## Recetas

- **Mochila de Cuero**: `Cuero Cuerda Cuero / Cuero Cofre Cuero / Cuero Cuero Cuero`
- **Mejora** (Cobre, Hierro, Oro, Diamante): la mochila en el centro rodeada de 8 lingotes de cobre / hierro / oro / diamantes.
- **Netherita**: plantilla de mejora de netherita arriba, mochila de Diamante en el centro y lingote de netherita abajo.
- **Tinte**: mochila + cualquier tinte.

Todas las recetas aparecen en el libro de recetas.

## Uso

- **Abrir**: clic derecho con la mochila en la mano, o clic derecho sobre ella dentro de tu inventario.
- **Coser un parche**: coge el parche con el ratón y haz clic sobre la mochila en tu inventario.
- **Descoser**: coge unas tijeras con el ratón y haz clic derecho sobre la mochila (quita el último parche).

No se pueden meter mochilas dentro de mochilas (ni shulkers/bundles, configurable), y la mochila abierta no se
puede mover ni tirar mientras está abierta, para evitar duplicaciones.

## Comandos y permisos

| Comando | Permiso | Descripción |
|---|---|---|
| `/morral` (alias `/mochila`) | — | Ayuda |
| `/morral abrir` | `morral.use` | Abre la mochila de la mano |
| `/morral dar <jugadores> <nivel>` | `morral.admin` | Entrega una mochila |
| `/morral parche <jugadores> <tipo>` | `morral.admin` | Entrega un parche |
| `/morral recargar` | `morral.admin` | Recarga `config.yml` |

`morral.bypass.candado` (op) permite abrir mochilas con candado ajenas.

## Configuración

`plugins/Morral/config.yml` permite ajustar el peso, el radio/fuerza del imán, materiales que el compactador
ignora, si se permiten shulkers/bundles dentro y todos los mensajes (formato MiniMessage).

## Compilar

Requiere JDK 25.

```bash
./gradlew build
```

El jar queda en `build/libs/Morral-<versión>.jar`. Cópialo a la carpeta `plugins/` de tu servidor Paper 26.3.
