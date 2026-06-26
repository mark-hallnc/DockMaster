# DockMaster Art Asset Guide

This guide explains how to add custom graphics to DockMaster. The game uses a fallback system: if a PNG file is missing, it will automatically render using generated placeholder textures (via Pixmap) or arcade-style shapes.

## Starter Assets
The project now includes a `PlaceholderTextureFactory` that generates basic textures at runtime if real files are missing. You can replace these by adding your own PNGs to the folders listed below.

## Folder Structure
All images must be placed in `core/assets/images/` (or `assets/images/` depending on your IDE setup).

```
assets/images/
  ├── boats/
  │   ├── skiff.png        (Small white hull)
  │   ├── runabout.png     (Cyan sleeker hull)
  │   ├── pontoon.png      (Grey wide deck)
  │   └── cruiser.png      (Premium gold cruiser)
  ├── docks/
  │   ├── dock_plank.png   (Brown planks)
  │   ├── piling.png       (Corner posts)
  │   ├── tire_bumper.png  (Side protection)
  │   └── cleat.png        (Tie-down points)
  ├── effects/
  │   ├── buoy.png         (Red/White marker)
  │   ├── marker_buoy.png
  │   ├── channel_marker_green.png
  │   ├── channel_marker_red.png
  │   ├── wake_foam.png
  │   └── impact_splash.png
  ├── decor/
  │   ├── fuel_pump.png
  │   └── umbrella.png
  ├── backgrounds/
  │   ├── water_tile.png   (Blue tiled water)
  │   ├── rocks_tile.png
  │   └── sand_tile.png
  └── ui/
      ├── star_filled.png
      ├── star_empty.png
      ├── cash.png
      ├── wrench.png
      ├── damage.png
      └── lock.png
```

## Technical Details
* Format: 32-bit PNG (with Alpha channel).
* Orientation: Boat bow/front must point **RIGHT** in the PNG.
* Power-of-two dimensions (64, 128, 256, 512) are recommended.
* Missing optional images are skipped; missing core images use generated placeholders.
