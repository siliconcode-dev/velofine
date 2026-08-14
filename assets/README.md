# assets/

Drop your real Velofine logo source file(s) here (e.g. `velofine-logo.svg` or a high-res PNG).

This folder is just the drop point for source art — it isn't consumed directly by the build.
The installer actually reads derived, pre-sized files from `installer/branding/`:

| File | Size | Purpose |
|---|---|---|
| `installer/branding/wizard-image.bmp` | 192×386, 24-bit BMP | Inno Setup wizard page side image |
| `installer/branding/wizard-small.bmp` | ~55×55, 24-bit BMP | Inno Setup wizard top-right logo |
| `installer/branding/velofine.ico` | multi-size (16/32/48/256) `.ico` | App icon (installer, jpackage app-image, shortcuts) |

Those three files currently hold placeholder art (a generated black/white/red brutalist mark,
programmatically drawn — no design tool was available in this environment). Once you drop a real
logo here, export/convert it to the three files above (any image editor, or ImageMagick:
`magick logo.png -resize 192x386 wizard-image.bmp`, etc.) and they'll flow straight into the next
installer build — no code changes needed.
