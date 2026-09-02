SUMMARY = "Pre-compile and cache the Celebrity Face Match NPU graph at boot"
DESCRIPTION = "Compiling the facematch model's graph for the i.MX8MP NPU \
               (Vivante VX delegate) the first time takes ~40s and \
               reliably segfaults if that compile runs concurrently with \
               demo-celebrity-face-match's own GTK/Wayland/camera startup. \
               This oneshot service compiles and caches the graph alone, \
               before the demo starts, so the demo only ever needs to load \
               an already-compiled graph."
HOMEPAGE = "https://www.phytec.de"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://npu-cache-warmup \
    file://npu-cache-warmup.service \
    file://npu-warmup.conf \
"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "npu-cache-warmup.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -Dm 0755 ${WORKDIR}/npu-cache-warmup ${D}${bindir}/npu-cache-warmup
    install -Dm 0644 ${WORKDIR}/npu-cache-warmup.service ${D}${systemd_system_unitdir}/npu-cache-warmup.service
    install -Dm 0644 ${WORKDIR}/npu-warmup.conf \
        ${D}${systemd_system_unitdir}/demo-celebrity-face-match.service.d/npu-warmup.conf
}

FILES:${PN} = " \
    ${bindir}/npu-cache-warmup \
    ${systemd_system_unitdir}/npu-cache-warmup.service \
    ${systemd_system_unitdir}/demo-celebrity-face-match.service.d/npu-warmup.conf \
"

RDEPENDS:${PN} = " \
    demo-celebrity-face-match-data \
    tensorflow-lite \
    tensorflow-lite-vx-delegate \
    python3-numpy \
"
