# meta-hailo-phytec

Dieser Layer erweitert das bestehende **phyCORE-i.MX 8M Plus AI Kit** (phyBOARD-Pollux, Artikelnummer L-1015e.A1) um Unterstützung für die **Hailo-8 M.2 KI-Beschleunigerkarte**. Die Basis-BSP-Dokumentation von PHYTEC (siehe `https://www.phytec.de/cdocuments/?doc=9oB5Hg`) beschreibt das Board mit seiner *internen* NPU (VeriSilicon Vivante VIP8000) und der darauf aufbauenden „Celebrity Face Match"-Demo (Image `phytec-facematch-image`). `meta-hailo-phytec` baut auf genau diesem Image auf (`phytec-hailo-image.bb` requires `phytec-facematch-image.bb`) und fügt eine **zweite, deutlich leistungsfähigere Inferenz-Engine** hinzu, ohne die vorhandene NPU-Demo zu verändern.

Dieses Dokument beschreibt, was der Layer an neuer Funktionalität mitbringt, wie die enthaltenen Demos benutzt werden und welches Paket welches Feature liefert – als Ergänzung zur offiziellen PHYTEC-Dokumentation.

## Kurzüberblick: was ist neu

| Bereich | Vorher (phytec-facematch-image) | Neu durch meta-hailo-phytec |
|---|---|---|
| Beschleuniger | interne NPU (VIP8000, TFLite/XNNPACK) | zusätzlich: Hailo-8 M.2-Karte (26 TOPS, PCIe) |
| Runtime | TensorFlow Lite | + HailoRT (libhailort, hailortcli, Python-Bindings) |
| GStreamer | Standard-Plugins | + `hailonet`, TAPPAS-Post-Processing-Plugins, Tracer |
| Demo-Apps | Celebrity Face Match | + Objekterkennung, Kennzeichenerkennung, Multistream-Erkennung, ResNet-Demo, Whisper-Benchmark |
| Image | `phytec-facematch-image` | `phytec-hailo-image` (erweitert das Face-Match-Image) |

## Layer-Struktur

`meta-hailo-phytec` ist selbst ein Dach über vier BitBake-Layern (alle `LAYERSERIES_COMPAT = "scarthgap"`, Priorität 8):

```
sources/meta-hailo-phytec/
├── conf/layer.conf                  Collection "hailo-phytec" (recipes-examples, -images, -devtools)
├── meta-hailo-accelerator/          Kernel-Treiber + Firmware für die Hailo-8-Karte
├── meta-hailo-libhailort/           HailoRT-Laufzeitumgebung, CLI, GStreamer-Plugin, Python-Bindings
├── meta-hailo-tappas/               TAPPAS: Post-Processing, GStreamer-Tools, Beispiel-Apps
├── recipes-examples/whisper-benchmark/   Hailo-8- vs. CPU-Benchmark-Demo (Spracherkennung)
├── recipes-images/images/           phytec-hailo-image.bb
└── recipes-devtools/pseudo/         Pseudo-Fix für neuere Host-glibc
```

`meta-hailo-tappas` hängt zusätzlich von `meta-hailo-libhailort` ab (`LAYERDEPENDS`), da die TAPPAS-Bibliotheken gegen `libgsthailo`/`libhailort` linken. Keiner der vier `layer.conf` deklariert eine explizite Abhängigkeit zu `meta-phytec`/`meta-freescale` – die Kompatibilität zur PHYTEC-BSP läuft über `MACHINE`-String-Prüfungen (`imx8`/`hailo15`) und darüber, dass `phytec-hailo-image.bb` das PHYTEC-eigene `phytec-facematch-image.bb` requires.

---

## 1. meta-hailo-accelerator — Kernel-Treiber & Firmware

Bringt die Hailo-8-Karte überhaupt erst zum Laufen (PCIe-Ebene, unterhalb von HailoRT).

| Rezept | Version | Liefert |
|---|---|---|
| `hailo-firmware_4.23.0.bb` | 4.23.0 | Firmware-Blob `hailo8_fw.bin` unter `/usr/lib/firmware/hailo/`, wird von HailoRT beim ersten Zugriff auf den Chip hochgeladen. Proprietäre Lizenz, Bezug direkt von Hailos S3-Bucket. |
| `hailo-pci_4.23.0.bb` | 4.23.0 | PCIe-Kernelmodul `hailo_pci.ko` (aus `hailort-drivers`, Branch `hailo8`), erkennt die M.2-Karte über die PCIe-Lane des SoC und stellt das `/dev/hailo*`-Device bereit. GPLv2. |

**Ohne diese beiden Pakete funktioniert nichts anderes im Hailo-Stack** – sie sind die Voraussetzung für alle folgenden Layer.

## 2. meta-hailo-libhailort — HailoRT-Laufzeitumgebung

HailoRT ist Hailos Runtime-API zum Laden kompilierter Netzwerk-Binaries (**HEF**-Dateien, „Hailo Executable Format") auf den Chip und zum Ausführen der Inferenz.

| Rezept | Version | Liefert |
|---|---|---|
| `libhailort_4.23.0.bb` | 4.23.0 | `libhailort.so` – die eigentliche C++-Runtime-Bibliothek. MIT-Lizenz. |
| `hailortcli_4.23.0.bb` | 4.23.0 | Kommandozeilenwerkzeug `hailortcli`: Inferenz auf HEFs ausführen, Firmware-Steuerung, Board-Messungen (Temperatur, Leistung), angeschlossene Geräte scannen. |
| `hailo-python-wheels_1.0.bb` | 1.0 (wrapt HailoRT 4.23.0) | Drei vorgebaute Python-Wheels: `hailort` (Python-Bindings/`hailo_platform`-API + `hailo`/`hailomz`-CLI), `hailo_model_zoo` (2.18.0), `hailo_tappas_core_python_binding` (nur der Pure-Python-Anteil; die kompilierte `.so` wird bewusst entfernt, da sie gegen hier nicht paketierte Libraries linkt). |
| `libgsthailo_4.23.0.bb` | 4.23.0 | GStreamer-Plugin `libgsthailo.so` mit dem Element **`hailonet`** – führt Inferenz direkt in einer GStreamer-Pipeline aus. LGPL-2.1. |

Hinweis für die Doku: `hailort-service` (systemd-Dienst) und ein eigenständiges `pyhailort`-Rezept sind in der `hailort-base.bbclass` zwar vorbereitet, aber in diesem Layer **nicht aktiviert/paketiert** – die Python-Anbindung läuft ausschließlich über die vorgebauten `hailo-python-wheels`.

### hailortcli – Beispielbenutzung

```bash
hailortcli scan                       # angeschlossene Hailo-Geräte auflisten
hailortcli fw-control identify        # Firmware-/Chip-Infos
hailortcli measure-power              # Leistungsmessung
hailortcli run mein_modell.hef        # Inferenz-Benchmark auf einer HEF-Datei
```

## 3. meta-hailo-tappas — Post-Processing, GStreamer-Tools & Demo-Apps

TAPPAS ist Hailos High-Level-Framework auf GStreamer-Basis: Post-Processing-Bibliotheken (Bounding-Box-Zeichnen, Cropping, NMS) + fertige Beispiel-Pipelines.

| Rezept | Version | Liefert |
|---|---|---|
| `hailo-post-processes_5.1.0.bb` | 5.1.0 | Netz-spezifische Post-Processing-`.so`-Dateien unter `/usr/lib/hailo-post-processes/`, u. a. `libyolo_hailortpp_post.so` (YOLO-Decodierung/NMS). |
| `libgsthailotools_5.1.0.bb` | 5.1.0 | GStreamer-Plugin `libgsthailotools.so` mit den Elementen **`hailofilter`**, **`hailooverlay`**, **`synchailonet`** u. a., plus `libgsthailometa.so` (Metadaten) und `libhailo_tracker.so` (Objekt-Tracking). |
| `tappas-tracers_5.1.0.bb` | 5.1.0 | `libgsthailotracers.so` – GStreamer-Tracer für Performance-Debugging (Latenz, FPS, Queue-Level, Buffer-Drops etc., steuerbar über `GST_TRACERS`). |
| `tappas-apps_3.29.1.bb` | 3.29.1 | Vier fertige Demo-Anwendungen unter `${ROOT_HOME}/apps/` (siehe unten). |
| `cxxopts`, `rapidjson`, `xtensor`, `xtl` | – | Header-only-Hilfsbibliotheken (CLI-Parsing, JSON-Configs, Tensor-Mathematik für die Post-Processing-Logik). |

### Demo-Apps aus `tappas-apps`

⚠️ **Pfad-Korrektur:** Bis zu diesem Stand installierte `tappas-apps_3.29.1.bb` hart nach `/home/root/apps`. Auf dieser BSP/Distro (`meta-ampliphy`) ist `ROOT_HOME` aber auf `/root` gesetzt – root loggt sich also in `~` = `/root` ein, und genau dort liegen bereits die vorhandenen `phytec-camera-examples` als Symlink `gstreamer-examples` (`ln -s ${datadir}/phytec-gstreamer-examples ${ROOT_HOME}/gstreamer-examples`). `/home/root/apps` war damit ein eigenes, vom eingeloggten Home-Verzeichnis losgelöstes Verzeichnis. Das Rezept wurde korrigiert, sodass `ROOTFS_APPS_DIR`, der Meson-Parameter `apps_install_dir` und die `FILES`-Liste jetzt `${ROOT_HOME}/apps` verwenden – die Hailo-Demos landen damit im selben Verzeichnis (`~`) wie die `gstreamer-examples`, nämlich unter `/root/apps/<app>/`.

Alle Apps werden per Shell-Skript gestartet. Ressourcen (HEFs, Beispielvideos, JSON-Pipeline-Configs) werden beim Bauen mitgezogen. Insgesamt liefert `tappas-apps` **vier** Demos (keine weiteren sind über `files/download_reqs_imx8.txt` mit Ressourcen verdrahtet – falls zusätzliche TAPPAS-Beispiel-Apps wie Instance-Segmentation oder Pose-Estimation gewünscht sind, müssten sie dort ergänzt werden):

**`detection`** – Objekterkennung (YOLOv5m, `yolov5m_wo_spp.hef`) auf einer Live-Kamera:
```bash
cd ~/apps/detection
./detection.sh -i /dev/video0 --show-fps
./detection.sh --help                 # weitere Optionen, u.a. --print-gst-launch
```
Nutzt automatisch `waylandsink`, falls unter Weston verfügbar, sonst `autovideosink`.

**`multistream_detection`** – Personen-/Gesichtserkennung (`yolov5s_personface_nv12.hef`) auf bis zu 6 gleichzeitigen Quellen (Kacheldarstellung, standardmäßig die mitgelieferten Beispielclips `detection0.mp4` … `detection5.mp4`):
```bash
cd ~/apps/multistream_detection
./multi_stream_detection.sh --num-of-sources 4 --show-fps
```

**`license_plate_recognition`** – Kennzeichenerkennung, dreistufige Pipeline (Fahrzeugerkennung → Kennzeichenerkennung → LPRNet-Texterkennung), Beispielmaterial `lpr.raw` liegt bereits im App-Verzeichnis (`resources/`).

**`resnet_inference_demo.sh`** – einfache ResNet-Bilderkennungs-Demo (Testmuster `videotestsrc` oder Kamera), liegt direkt unter `~/apps/`:
```bash
cd ~/apps
./resnet_inference_demo.sh                 # Testmuster (Ball)
./resnet_inference_demo.sh --camera /dev/video0 --show-fps
```
Setzt voraus, dass ein `hailo_tutorials`-Python-Paket mit dem HEF `resnet_v1_18.hef` installiert ist (nicht Teil dieses Layers).

Zum Debuggen der GStreamer-Pipelines steht (falls das `tappas_bashrc`-Skript eingebunden wird) `gst_set_debug`/`gst_set_graphic` zur Verfügung, um Tracer-Logs bzw. Pipeline-Graphen (`.dot`) zu erzeugen.

⚠️ **Wichtig:** In `phytec-hailo-image.bb` sind `libgsthailotools`, `hailo-post-processes`, `tappas-apps` und `tappas-tracers` derzeit **auskommentiert** – d. h. sie werden gebaut, aber standardmäßig **nicht** ins Image installiert. Wer diese Demos nutzen möchte, muss die entsprechenden Zeilen in `recipes-images/images/phytec-hailo-image.bb` einkommentieren (siehe Abschnitt 5).

## 4. Whisper-Benchmark-Demo (`recipes-examples/whisper-benchmark`)

Eine eigenständige Vergleichsdemo für Spracherkennung (Whisper-tiny), die Hailo-8 gegen die interne CPU des i.MX8MP antreten lässt. **Bewusst ohne NPU-Backend** – die interne VIP8000-NPU ist auf quantisierte CNNs ausgelegt und laut Messung 5,3× langsamer als die reine CPU bei diesem transformerbasierten Modell (BATCH_MATMUL/TRANSPOSE/LayerNorm passen nicht auf die NPU-Architektur).

Pakete:
- `demo-whisper-benchmark_1.0.bb` – die Python-CLI-Tools (`whisper-benchmark`, `whisper-hailo`, `whisper-cpu`), `COMPATIBLE_MACHINE = mx8mp-nxp-bsp`.
- `demo-whisper-benchmark-data_1.0.bb` – Modelle & Assets (~280 MB), aufgeteilt in Unterpakete `-hailo`, `-tflite`, `-common`, damit ein Image auch nur einen Teil installieren kann.

### Benutzung

```bash
whisper-benchmark                      # beide Backends vergleichen (Standard: jfk.wav)
whisper-benchmark -b hailo             # nur Hailo-8
whisper-benchmark -b cpu               # nur CPU (TFLite/XNNPACK)
whisper-benchmark -a /pfad/zu/16k.wav  # eigene Audiodatei (16 kHz WAV)
whisper-benchmark --json ergebnis.json # Ergebnis zusätzlich als JSON

whisper-hailo aufnahme.wav             # nur Transkript, Hailo-8-Backend
whisper-cpu aufnahme.wav               # nur Transkript, CPU-Backend
```

### Mit echtem Mikrofon ausprobieren

`whisper-benchmark`, `whisper-hailo` und `whisper-cpu` unterstützen `-m`/`--mic` als Ersatz für eine WAV-Datei: Aufnahme über `sounddevice`/PortAudio, Enter-Taste beendet die Aufnahme vorzeitig, sonst stoppt sie nach der angegebenen Maximaldauer (Standard: das 10-s-Kompilierfenster) – anschließend wird direkt transkribiert. Das entspricht der Interaktion der offiziellen `hailo-apps`-`speech_recognition`-Referenz-App (Enter zum Start/Stopp) statt einer festen Aufnahmedauer.

```bash
whisper-hailo --mic                              # bis zu 10 s vom Standard-Mikrofon, Enter zum vorzeitigen Stopp
whisper-hailo --mic 5                            # Maximaldauer 5 s
python3 -m sounddevice                            # Eingabegeräte auflisten, falls falsches Default-Device
whisper-hailo --mic -D USB                       # Geräteauswahl per Namens-Teilstring
whisper-benchmark --mic                           # Live-Mikrofon, beide Backends im Vergleich
```

Voraussetzung ist `python3-sounddevice` (zieht `portaudio-v19` für `libportaudio` nach) auf dem Zielsystem – `demo-whisper-benchmark` empfiehlt es per `RRECOMMENDS`, ist aber ohne es weiter benutzbar (nur `--mic` schlägt dann fehl). Ohne TTY auf stdin (z. B. reines `ssh board whisper-hailo --mic` ohne `-t`) kann kein Enter erkannt werden – dann läuft die Aufnahme einfach über die volle Maximaldauer.

**Hintergrund, warum das kein Klon der Hailo-10H-Demo ist:** Auf Hailo-10H-Hardware (z. B. im `meta-emy`-Layer für i.MX95) gibt es die High-Level-API `hailo_platform.genai.Speech2Text` – ein voll auf dem Chip laufendes ASR-Modul, das Encoder, Decoder und Tokenisierung in einem Aufruf kapselt. **Diese GenAI-API existiert ausschließlich für Hailo-10H**, nicht für Hailo-8. Der Hailo-8-Pfad läuft stattdessen über die niedrigere `InferModel`-API mit getrennten Encoder-/Decoder-HEFs – genau das, was `demo-whisper-benchmark` bereits seit der ersten Version so implementiert (siehe Abschnitt 4) und was auch Hailos eigene geräteübergreifenden Tools (`hailocs/hailo-whisper`, `hailo-ai/hailo-apps` `speech_recognition`) für Hailo-8 verwenden. Die Portierung des "Mikrofon → Whisper → Text"-Ablaufs bedeutet für Hailo-8 deshalb nicht das Kopieren der GenAI-Aufrufe, sondern das Hinzufügen einer `sounddevice`-Aufnahme vor die bereits vorhandene Encoder-/Decoder-Pipeline – umgesetzt als neues `whisper_bench/mic.py`-Modul plus `-m/--mic`-Option in allen drei CLI-Tools, und ein neues `python3-sounddevice`-Rezept (`recipes-devtools/python/`).

Gemessene Beispielwerte auf phyBOARD-Pollux i.MX8MP + Hailo-8: Encoder 29,4 Inferenzen/s (Hailo-8) vs. 0,97 Inferenzen/s (CPU) – **ca. 9,9× schneller** auf dem reinen Encoder-Vergleich (die einzige wirklich faire Spalte, da die Decoder-HEF mit anderer Sequenzlänge/Embedding-Strategie kompiliert wurde als das TFLite-Modell).

Es existiert außerdem eine dokumentierte, aber **nicht ins Produkt übernommene** Quantisierungs-Untersuchung (`quantization/QUANTIZATION.md`): eine Post-Training-Int8-Quantisierung des TFLite-Modells lädt zwar technisch, liefert aber unbrauchbare Transkripte (klassischer PTQ-Kollaps bei Transformer-Attention ohne QAT) – explizit **nicht** in `demo-whisper-benchmark-data` verdrahtet.

## 5. Image-Rezept: `phytec-hailo-image`

`recipes-images/images/phytec-hailo-image.bb` erweitert `phytec-facematch-image.bb` (aus der PHYTEC-BSP) um den Hailo-Stack. Standardmäßig installiert werden:

```
hailo-firmware libhailort hailortcli hailo-pci libgsthailo hailo-python-wheels
demo-whisper-benchmark
packagegroup-imx-ml python3-pip git python3-netifaces
gstreamer1.0 gstreamer1.0-plugins-base gstreamer1.0-plugins-good gstreamer1.0-plugins-bad
```

**Nicht** standardmäßig installiert (auskommentiert im Rezept):
```
# libgsthailotools hailo-post-processes
# tappas-apps tappas-tracers
```

Um die TAPPAS-Demo-Apps (Abschnitt 3) im Image zu haben, diese vier Zeilen in `phytec-hailo-image.bb` einkommentieren und neu bauen.

`HAILO_SOC_NAME = "hailo8"` legt den Ziel-Chip fest (Hailo-8, passend zur M.2-Karte des AI-Kits).

### Build-Anleitung (analog zur bestehenden Face-Match-Doku)

```bash
MACHINE=phyboard-pollux-imx8mp-1 DISTRO=yogurt-vendor-xwayland \
  ./phyLinux init -p topic -r <passende BSP-Revision mit meta-hailo-phytec>
source sources/poky/oe-init-build-env
bitbake phytec-hailo-image
```

Wie beim Basis-Image ggf. `ACCEPT_FSL_EULA = "1"` in `conf/local.conf` setzen (NXP-Binärlizenzen für GPU/VPU).

## 6. Sonstiges

- `recipes-devtools/pseudo/pseudo_git.bbappend` – pinnt eine neuere `pseudo`-Git-Revision und ersetzt einen PIE-Flag-Patch durch einen glibc-≥2.34-Kompatibilitätspatch (behebt Symbol-Konflikte, wenn der Build-Host eine neuere glibc als der Ziel-Sysroot hat).
- `meta-hailo-tappas/recipes-core/hailo-base-config/files/tappas_bashrc` liegt im Layer, wird aber von keinem Rezept konsumiert (verwaistes Entwickler-Skript mit Debug-Hilfsfunktionen wie `gst_set_debug`).

## Offene Punkte für zukünftige Layer-Pflege

- `hailort-service`, `pyhailort`, `recipes-core/packagegroups` und `recipes-images/images` unter `meta-hailo-libhailort` sind leere Verzeichnisse ohne Rezept – falls künftig ein systemd-Service oder eine eigene Paketgruppe gewünscht ist, muss das dort ergänzt werden.
- `tappas_bashrc` sollte entweder an ein Rezept angebunden oder entfernt werden.
- Der Kommentar zu „~230 MB NPU-Graph-Cache" in `phytec-hailo-image.bb` bezieht sich auf ein NPU-Backend der Whisper-Demo, das inzwischen entfernt wurde – sollte beim nächsten Edit korrigiert werden.
- ~~`tappas-apps` installierte nach `/home/root/apps` statt in `${ROOT_HOME}` (`/root` auf dieser Distro) – behoben, Apps liegen jetzt unter `${ROOT_HOME}/apps`, im selben Verzeichnis wie `gstreamer-examples`.~~ Erledigt.
- Nur die vier oben beschriebenen Apps sind über `download_reqs_imx8.txt` mit Modell-/Medien-Ressourcen verdrahtet. Weitere TAPPAS-Beispiel-Apps (z. B. Instance-Segmentation, Pose-Estimation, Depth-Estimation) existieren im TAPPAS-Upstream-Repo, werden von diesem Layer aber nicht automatisch gezogen.
