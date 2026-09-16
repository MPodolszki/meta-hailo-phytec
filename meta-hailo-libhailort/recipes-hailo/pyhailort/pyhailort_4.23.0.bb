DESCRIPTION = "pyhailort - Hailo's Python API for the Hailo-8/8L/8R, built from source \
               against the target's own Python. This is the Hailo-8 counterpart of \
               pyhailort10_5.3.0.bb (HailoRT master, Hailo-10H/15) and replaces the \
               prebuilt wheel in hailo8-python-wheels_1.0.bb: that wheel ships a cp312 \
               extension module (_pyhailort.cpython-312-*.so), which cannot be imported \
               by the Python 3.13 in this BSP -- it needs _PyThreadState_UncheckedGet, \
               removed in CPython 3.13. Building the binding here makes it follow \
               PYTHON_BASEVERSION instead of being frozen to whatever interpreter Hailo \
               happened to build the wheel with."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://../../../../LICENSE;md5=ed57bbf10be0c74ecf2c80710208b2b3 \
                    file://../../../../LICENSE-3RD-PARTY.md;md5=87f8edc8e3d5342f8b0614df5bae3b58"

# Same repo, branch and revision as libhailort_4.23.0.bb -- the binding links against
# that exact libhailort, so the two must not drift apart.
SRC_URI = "git://git@github.com/hailo-ai/hailort.git;protocol=https;branch=hailo8"
SRCREV = "08f088d3b443c7846af067269ce998c6d5d91449"

S = "${UNPACKDIR}/git/hailort/libhailort/bindings/python/platform"

inherit pkgconfig hailort-base python3native setuptools3

DEPENDS += "python3-wheel-native libhailort python3-pybind11 git-native"
RDEPENDS:${PN} += "libhailort python3-future python3-importlib-metadata python3-netifaces \
                   python3-appdirs python3-contextlib2 python3-netaddr \
                   python3-argcomplete python3-numpy python3-setuptools"

# Only one Hailo Python binding can own the hailo_platform import namespace.
RCONFLICTS:${PN} += "pyhailort10 hailo8-python-wheels hailo-python-wheels"

# hailort's cmake/external/pybind11.cmake FetchContent-pulls pybind11 2.10.1. That version
# predates NumPy 2, and its py::array accessors read the numpy ABI through offsets that moved
# in NumPy 2 -- buffer.nbytes() then comes back as 0, so every inference dies in libhailort with
# "Input buffer size 0 is different than expected N" (both the InferModel and the InferVStreams
# API). The target carries NumPy 2.2, so the binding has to be built against a NumPy-2-aware
# pybind11; meta-python provides 2.13.6, which also is the first line to support CPython 3.13.
# Redirect the FetchContent block at the sysroot package instead. This is done here rather than
# as a SRC_URI patch because the file sits outside S (S points deep into bindings/python/platform).
do_configure:prepend() {
    cat > ${UNPACKDIR}/git/hailort/cmake/external/pybind11.cmake <<'EOFCMAKE'
cmake_minimum_required(VERSION 3.11.0)

# Patched by pyhailort_4.23.0.bb: use the pybind11 from the Yocto sysroot (2.13.6, NumPy 2 and
# CPython 3.13 aware) instead of FetchContent'ing 2.10.1 from github.
find_package(pybind11 REQUIRED CONFIG)
EOFCMAKE
}

do_compile:prepend() {
    # setup.py drives cmake itself (we inherit setuptools3, not cmake's do_compile), so the
    # cross-build settings have to reach it through the environment.

    # allow linkage against HailoRT
    export HailoRT_DIR=${STAGING_LIBDIR}/cmake/HailoRT
    # allow linkage against pybind11
    export PYTHON_INCLUDE_DIRS=${STAGING_INCDIR}/python${PYTHON_BASEVERSION}
    # cmake.bbclass generates this in WORKDIR, not in UNPACKDIR
    export CMAKE_TOOLCHAIN_FILE=${WORKDIR}/toolchain.cmake
}

# The extension comes out of cmake already stripped.
INSANE_SKIP:${PN} += "already-stripped"

COMPATIBLE_MACHINE = "(mx8mp-nxp-bsp)"
