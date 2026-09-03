# meta-hailo-phytec

PHYTEC's Yocto layer for the **PHYTEC Hailo AI Kit**: a phyBOARD-Pollux
(i.MX8MP) carrying a Hailo-8 M.2 AI accelerator, running an unattended demo
kiosk out of the box.

This layer is a fork of [hailo-ai/meta-hailo](https://github.com/hailo-ai/meta-hailo).
Hailo's own layer description — HailoRT, drivers, firmware, TAPPAS, links to
the developer zone — is preserved unchanged in
[README.hailo-upstream.md](README.hailo-upstream.md). This file documents what
PHYTEC added on top and how to build and flash the kit.

---

## Scope of the first release

**Hailo-8 only.** The image recipe also carries a complete Hailo-10H/15 stack
(`HAILO_CHIP = "hailo10"`, see [Configuration](#configuration)), but that path
is neither shipped nor tested for this kit. Build it only if you know what you
are doing.

**Camera is fixed:** a **VM-016 on CSI1**. The device tree overlays, the
`-c vm016` demo arguments and `setup-pipeline-csi1` are wired for that one
camera; there is no configuration path for a different module.

**Display:** HDMI or LVDS, whichever is connected at boot. HDMI wins if both
are. There is no hotplug — connect the display before powering on.

---

## Hardware

| | |
|---|---|
| Board | phyBOARD-Pollux i.MX8MP (`phyboard-pollux-imx8mp-3`) |
| Accelerator | Hailo-8 (M.2) |
| Camera | phyCAM VM-016 on CSI1 |
| Display | HDMI or LVDS |
| Audio | optional USB speaker — the Whisper demo plays back its sample; the SoC's built-in `audiohdmi` card alone gives you no sound and the demo says so on screen |

---

## Getting the sources

The kit is defined by a single manifest. Everything else — which layers are
active, which revisions — follows from it; `bblayers.conf` is **generated**
from the manifest by `meta-phytec/scripts/init_bblayers.py`, so there is no
`bblayers.conf` to edit or ship.

```sh
mkdir hailo-ai-kit && cd hailo-ai-kit
repo init -u git://git.phytec.de/phy2octo -b topic \
          -m BSP-Yocto-HailoAIKit-i.MX8MP-v0.1.xml
repo sync
```

> **Not yet published.** As of this writing the manifest exists locally only,
> and both Hailo layers live under `github.com/MPodolszki` rather than
> `github.com/phytec`. Before the kit ships, the repositories have to move and
> the manifest's `remote=`/`revision=` entries have to be re-pinned — the spot
> is marked with a comment in the manifest.

---

## Building

```sh
./tools/init
. sources/poky/oe-init-build-env
bitbake phytec-hailo-image
```

`./tools/init` runs once per checkout. It generates `build/conf/bblayers.conf`
from the manifest and writes `MACHINE` and `DISTRO` into `build/conf/local.conf`
from the manifest's `supported_builds` field, so you should not have to set
either by hand:

```
MACHINE ?= "phyboard-pollux-imx8mp-3"
DISTRO  ?= "ampliphy-vendor-xwayland"
```

Every later shell only needs the `oe-init-build-env` line.

A container image for a reproducible build host is named in the manifest
(`build_container`); `./tools/init` prints the `docker run` invocation for it
at the end.

---

## Flashing

The build deploys to
`build/deploy-ampliphy-vendor-xwayland/images/phyboard-pollux-imx8mp-3/`:

| File | Use |
|---|---|
| `phytec-hailo-image-*.rootfs.wic.xz` + `.wic.bmap` | full SD-card image |
| `phytec-hailo-image-*.rootfs.partup` | PHYTEC [partup](https://github.com/phytec/partup) package |
| `phytec-hailo-image-*.rootfs.tar.gz` | plain rootfs tarball |

**SD card, from a Linux host** (replace `/dev/sdX` with the card, *not* a
partition — check with `lsblk` first, this overwrites the whole device):

```sh
cd build/deploy-ampliphy-vendor-xwayland/images/phyboard-pollux-imx8mp-3
bmaptool copy --bmap phytec-hailo-image-*.rootfs.wic.bmap \
              phytec-hailo-image-*.rootfs.wic.xz /dev/sdX
```

**partup**, run on the target against the target's own storage:

```sh
partup install phytec-hailo-image-<version>.rootfs.partup /dev/mmcblk2
```

Consult the PHYTEC BSP manual for your board revision before writing to
internal eMMC; boot source selection on phyBOARD-Pollux is a hardware setting.

---

## What the board does after boot

`demo-loop.service` is the only demo that autostarts. It rotates four slots,
30 seconds each, forever, on whichever display was detected:

1. **Celebrity Face Match** — live camera, matches the visitor's face against a
   celebrity database. Face detection and embeddings run on the i.MX8MP **NPU**.
2. **Object Detection** — YOLOv8n, **Hailo-8 and CPU side by side** with live
   FPS counters, so the accelerator's contribution is visible rather than
   claimed.
3. **Whisper** — speech-to-text on the Hailo-8, transcribing a bundled sample
   and playing it back.
4. **Info screen** — static slide listing the other demos that stay runnable by
   hand.

Two supporting units run before it:

- `weston-output-config.service` → `detect-display`, before Weston: picks
  HDMI or LVDS, rewrites the `[output]` stanzas in
  `/etc/xdg/weston/weston.ini`, and records the choice in
  `/run/demo-display-kind`.
- `npu-cache-warmup.service`: compiles and caches the Vivante NPU graph
  **alone**, before the face-match demo starts. Without it the ~40 s cold
  compile races the demo's own GTK/Wayland/camera startup and the closed-source
  Vivante driver stack segfaults on a NULL dereference. This service is the
  fix; do not disable it.

Stop the kiosk to get the screen back:

```sh
systemctl stop demo-loop.service
```

`demo-celebrity-face-match.service` ships **disabled on purpose**
(`SYSTEMD_AUTO_ENABLE:${PN} = "disable"` in the examples layer). Enabling it
alongside the loop makes it hold `/dev/video0` forever, and every camera slot
the loop starts then dies with `mipi_csis_set_fmt, set sensor format fail` in
dmesg.

### Running demos by hand

Over the debug UART or SSH, after stopping the loop:

```sh
object-detection-image [-b hailo|cpu]      # single image
object-detection-hailo-video <file>        # video file, Hailo-8
object-detection-cpu-video <file>          # video file, CPU
object-detection-compare-cam               # live camera, Hailo-8 vs CPU
object-detection-benchmark                 # text-only speed comparison
whisper-hailo -m                           # microphone, Hailo-8
whisper-cpu [file.wav]                     # CPU instead of Hailo-8
whisper-benchmark                          # text-only speed comparison
hailortcli fw-control identify             # is the Hailo-8 alive?
```

---

## Configuration

`HAILO_CHIP` in `recipes-images/images/phytec-hailo-image.bb` selects which
accelerator stack goes into the image. Exactly one is ever installed:

| Value | Stack |
|---|---|
| `hailo8` (default, **the shipped kit**) | HailoRT 4.23.0 `hailo8` branch — the last line that still supports Hailo-8 — plus firmware, PCIe driver, `hailortcli`, `libhailort`, `libgsthailo`, Python wheels, and the three demos |
| `hailo10` | HailoRT 5.3.0 master — Hailo-10H/15/Mars. **Untested for this kit.** |

They cannot coexist: HailoRT's master branch dropped Hailo-8 support entirely,
so the two lines need separate drivers, firmware and wheels. The `10`-suffixed
recipe names exist only so bitbake can carry both side by side; the installed
binaries keep their normal upstream names either way.

To change it, add the line to `build/conf/local.conf` and rebuild:

```
HAILO_CHIP = "hailo10"
```

---

## Layer layout

```
meta-hailo-phytec/                 this layer   (BBFILE_PRIORITY 8)
├── meta-hailo-accelerator/        Hailo-8/10 firmware + PCIe driver   ACTIVE
├── meta-hailo-libhailort/         HailoRT, hailortcli, GStreamer      ACTIVE
├── meta-hailo-tappas/             TAPPAS pipelines                    not in bblayers
└── meta-hailo-vpu/                Hailo-15 VPU                        not in bblayers
```

Only the two layers marked ACTIVE are listed as `<sublayer>` entries in the kit
manifest, which is what puts them into `bblayers.conf`. TAPPAS and VPU are
Hailo-15 territory and stay out of the Hailo-8 kit.

`LAYERDEPENDS` are `core phytec ampliphy imx-machine-learning`;
`LAYERSERIES_COMPAT` is `scarthgap`.

The demo kiosk itself lives in a separate layer,
[meta-hailo-examples-phytec](https://github.com/MPodolszki/meta-hailo-examples-phytec).

### Note on `meta-celebrity-face-match`

This layer carries its own `demo-celebrity-face-match_0.5.bb` and
`demo-celebrity-face-match-data_1.1.bb`. PHYTEC's standalone
`meta-celebrity-face-match` layer ships recipes of the **same name and
version**, so the two layers must not both be active. The kit manifest
therefore does not include it.

---

## Two things worth knowing before you change something

Both are recorded as comments at the point they matter; repeated here because
both cost real debugging time.

- **Do not set a global `DEFAULTTUNE` in `conf/layer.conf`.** The onnxruntime
  `-mcpu`/`-march` conflict is fixed inside that recipe's own `.bbappend`. A
  machine-wide tune silently retunes every package including U-Boot and ATF,
  and broke boot completely — no UART output, not even from the bootloader.
- **`PREFERRED_VERSION_python3-antlr4-runtime = "4.9.3"` is load-bearing.**
  omegaconf (via hailo-model-zoo) ships a pre-generated ANTLR grammar tied to
  the 4.9.x runtime; meta-openembedded's 4.13.1 fails to deserialize it at
  import time.

---

## License

MIT, as upstream — see [COPYING.MIT](COPYING.MIT).
