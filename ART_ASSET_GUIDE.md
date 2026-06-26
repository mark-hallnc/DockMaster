# DockMaster Art Asset Guide

This guide explains how to add custom graphics to DockMaster. The game uses a fallback system: if a PNG file is missing, it will automatically render using arcade-style shapes.

## Folder Structure
All images must be placed in `core/assets/images/` (or `assets/images/` depending on your IDE setup).

```
assets/images/
  ├── boats/
  │   ├── skiff.png
  │   ├── runabout.png
  │   └── pontoon.png
  ├── docks/
  │   └── dock_plank.png
  ├── effects/
  │   └── buoy.png
  └── backgrounds/
      └── water_tile.png
```

## Boat Sprites
* **Recommended Size:** 256x128 pixels (or similar aspect ratio).
* **Orientation:** The **BOW (front)** of the boat must point **RIGHT** in the PNG file.
* **Background:** Must be transparent.
* **Filenames:** Must match the boat IDs (`skiff.png`, `runabout.png`, `pontoon.png`).

## Dock Sprites
* **dock_plank.png:** 128x128 or 256x256. This texture will be stretched or tiled over the dock rectangles.

## Backgrounds & Decor
* **water_tile.png:** 512x512 pixels. Should be a seamless/loopable water texture. It will be tiled across the water area.
* **buoy.png:** 64x64 transparent PNG. Used for decorative markers in the water.

## Technical Details
* Format: 32-bit PNG (with Alpha channel).
* Power-of-two dimensions (64, 128, 256, 512) are recommended for better compatibility with older Android devices.
* The game uses linear filtering for textures to keep them looking smooth when scaled.
