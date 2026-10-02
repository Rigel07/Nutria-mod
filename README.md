# Nutrias — mod de Minecraft (Forge 1.20.1)

Añade la **nutria**: una criatura adorable que juega en el agua y puede ser tu mascota.

## Cómo se juega
- **Aparece** en ríos, pantanos y manglares (y con su huevo generador en la pestaña de huevos del creativo).
- **Domesticar**: clic derecho con **salmón crudo** (1 de cada 3 veces funciona, se gasta un salmón cada intento).
- Domesticada es **inmortal** (solo la afectan /kill, el creativo y similares) y **ataca a los mobs que te atacan**
  (o a los que atacas tú). No ataca a creepers ni ghasts.
- **Seguir / quedarse quieta**: **Shift + clic derecho** alterna entre seguirte y quedarse quieta.
- **Amistad (nivel 1 a 10)**:
  - Darle comida (salmón crudo +10, salmón cocinado +15, bacalao +5, bacalao cocinado +8). Un bocado cada 5 segundos.
  - Caricias: **clic derecho con la mano vacía** (+4 cada 2 segundos).
  - Cada nivel sube su daño. Nivel 1 = 1 de daño, **nivel 10 = 7 de daño (como una espada de diamante)**.
- Juega en el agua: nada, da vueltas y salta chapoteando.

## Compilar
1. Sube todo el contenido de esta carpeta a un repositorio de GitHub.
2. Ve a la pestaña **Actions** → se ejecuta sola con cada subida (o pulsa *Run workflow*).
3. Cuando termine, baja el `.jar` desde **Artifacts → nutriamod-jar** y ponlo en la carpeta `mods` (Forge 1.20.1).

## Ajustes fáciles (OtterEntity.java)
- `LEVEL_THRESHOLDS`: puntos necesarios por nivel.
- `MIN_DAMAGE` / `MAX_DAMAGE`: daño en nivel 1 y nivel 10.
- `PET_POINTS` y `foodPoints()`: puntos por caricia y por comida.
