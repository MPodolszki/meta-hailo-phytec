SUMMARY = "Install prebuilt Hailo-10/15 Python wheels"
DESCRIPTION = "Installs the hailort 5.3.0 wheel into target site-packages, matching the \
               Hailo-10H/15 libhailort10 build (libhailort10_5.3.0.bb). This is the Hailo-10/15 \
               counterpart of hailo8-python-wheels_1.0.bb, which installs the hailort 4.23.0 \
               wheel for Hailo-8/8L/8R. Both packages install into the same 'hailo'/ \
               'hailo_platform' Python import namespace, so they RCONFLICT - only one of the \
               two Python bindings can be installed in a given rootfs at a time. Chip-agnostic \
               wheels (hailo-model-zoo, tappas core python binding) live in \
               hailo-python-common-wheels and can be installed alongside either one."
LICENSE = "CLOSED"

SRC_URI = " \
    file://hailort-5.3.0-cp312-cp312-linux_aarch64.whl \
"

S = "${WORKDIR}"

inherit python3-dir python3native

DEPENDS += "python3-installer-native"

RDEPENDS:${PN} += " \
    libhailort10 (>= 5.3.0) \
    python3-argcomplete \
    python3-contextlib2 \
    python3-core \
    python3-future \
    python3-netaddr \
    python3-netifaces \
    python3-numpy \
"

RCONFLICTS:${PN} += "pyhailort hailo8-python-wheels"

INSANE_SKIP:${PN} += "already-stripped"

do_install() {
    nativepython3 -m installer --destdir=${D} --prefix=${prefix} ${WORKDIR}/hailort-5.3.0-cp312-cp312-linux_aarch64.whl

    # Convert native build-time shebangs to target runtime Python.
    if [ -f ${D}${bindir}/hailo ]; then
        sed -i '1s|^#!.*|#!/usr/bin/env python3|' ${D}${bindir}/hailo
    fi
}

FILES:${PN} += "${bindir} ${PYTHON_SITEPACKAGES_DIR}"
