SUMMARY = "Install prebuilt Hailo Python wheels"
DESCRIPTION = "Installs hailort, hailo-model-zoo and hailo-tappas-core-python-binding wheels into target site-packages."
LICENSE = "CLOSED"

SRC_URI = " \
    file://hailort-4.23.0-cp312-cp312-linux_aarch64.whl \
    file://hailo_model_zoo-2.18.0-py3-none-any.whl \
    file://hailo_tappas_core_python_binding-5.3.0-py3-none-any.whl \
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
    python3-imageio \
    python3-matplotlib \
    python3-netaddr \
    python3-netifaces \
    python3-numpy \
    python3-opencv \
    python3-pillow \
    python3-pycocotools \
    python3-termcolor \
    python3-tqdm \
"

RCONFLICTS:${PN} += "pyhailort"

INSANE_SKIP:${PN} += "already-stripped"

do_install() {
    nativepython3 -m installer --destdir=${D} --prefix=${prefix} ${WORKDIR}/hailort-4.23.0-cp312-cp312-linux_aarch64.whl
    nativepython3 -m installer --destdir=${D} --prefix=${prefix} ${WORKDIR}/hailo_model_zoo-2.18.0-py3-none-any.whl
    nativepython3 -m installer --destdir=${D} --prefix=${prefix} ${WORKDIR}/hailo_tappas_core_python_binding-5.3.0-py3-none-any.whl

    # Keep only target-compatible artifacts for this image.
    find ${D}${PYTHON_SITEPACKAGES_DIR} -type f -name "*x86_64-linux-gnu.so" -delete
    find ${D}${PYTHON_SITEPACKAGES_DIR} -type f -name "hailo.cpython-310-aarch64-linux-gnu.so" -delete
    find ${D}${PYTHON_SITEPACKAGES_DIR} -type f -name "hailo.cpython-311-aarch64-linux-gnu.so" -delete
    find ${D}${PYTHON_SITEPACKAGES_DIR} -type f -name "hailo.cpython-313-aarch64-linux-gnu.so" -delete

    # Drop tappas core binary module for now; it links against libs that are not packaged in this stack.
    find ${D}${PYTHON_SITEPACKAGES_DIR} -type f -name "hailo.cpython-312-aarch64-linux-gnu.so" -delete

    # Convert native build-time shebangs to target runtime Python.
    if [ -f ${D}${bindir}/hailo ]; then
        sed -i '1s|^#!.*|#!/usr/bin/env python3|' ${D}${bindir}/hailo
    fi
    if [ -f ${D}${bindir}/hailomz ]; then
        sed -i '1s|^#!.*|#!/usr/bin/env python3|' ${D}${bindir}/hailomz
    fi
}

FILES:${PN} += "${bindir} ${PYTHON_SITEPACKAGES_DIR}"
