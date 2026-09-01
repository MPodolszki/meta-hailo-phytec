SUMMARY = "Install prebuilt Hailo-8 Python wheels"
DESCRIPTION = "Installs the hailort 4.23.0 wheel (the last HailoRT line that still supports \
               Hailo-8/8L/8R) into target site-packages. This is the Hailo-8 counterpart of \
               hailo-python-wheels_1.0.bb, which installs the hailort 5.3.0 wheel for \
               Hailo-10H/15. Both packages install into the same 'hailo'/'hailo_platform' \
               Python import namespace, so they RCONFLICT - only one of the two Python \
               bindings can be installed in a given rootfs at a time. If you need both chips \
               reachable from Python in the same image, use separate virtualenvs/sysroots per \
               chip instead of installing both wheel packages together."
LICENSE = "CLOSED"

SRC_URI = " \
    file://hailort-4.23.0-cp312-cp312-linux_aarch64.whl \
"

S = "${WORKDIR}"

inherit python3-dir python3native

DEPENDS += "python3-installer-native"

RDEPENDS:${PN} += " \
    libhailort (>= 4.23.0) \
    python3-argcomplete \
    python3-contextlib2 \
    python3-core \
    python3-future \
    python3-netaddr \
    python3-netifaces \
    python3-numpy \
"

RCONFLICTS:${PN} += "pyhailort hailo-python-wheels"

INSANE_SKIP:${PN} += "already-stripped"

do_install() {
    nativepython3 -m installer --destdir=${D} --prefix=${prefix} ${WORKDIR}/hailort-4.23.0-cp312-cp312-linux_aarch64.whl

    if [ -f ${D}${bindir}/hailo ]; then
        sed -i '1s|^#!.*|#!/usr/bin/env python3|' ${D}${bindir}/hailo
    fi
}

FILES:${PN} += "${bindir} ${PYTHON_SITEPACKAGES_DIR}"
