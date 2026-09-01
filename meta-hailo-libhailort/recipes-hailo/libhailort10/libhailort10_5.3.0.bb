DESCRIPTION = "libhailort - hailo's API for running inference on the Hailo-10H/15/Mars chips \
               (HailoRT master branch, see hailo-ai/hailort README: master no longer supports \
               Hailo-8). Recipe/package name is libhailort10 purely so bitbake can carry this \
               alongside the Hailo-8 build (libhailort_4.23.0.bb, PN libhailort) as two separate \
               recipes; phytec-hailo-image.bb's HAILO_CHIP flag ever only pulls in ONE of the two \
               into a given image, so the installed files use the same canonical libhailort.so / \
               ${includedir}/hailort / cmake/HailoRT names either way, matching upstream Hailo \
               documentation and example apps. \
               the recipe compiles libhailort and copies it on the target device's root file system"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://hailort/LICENSE;md5=800c77403398cedcbbbcd86d37f5e0ff \
                    file://hailort/LICENSE-3RD-PARTY.md;md5=eb78bffb175a3f2be317bb4c45fedecf"

SRC_URI = "git://git@github.com/hailo-ai/hailort.git;protocol=https;branch=master"
SRCREV = "d503417f2a0db186a838390fb08690c4ea0f415e"

S = "${WORKDIR}/git"

inherit hailort-base
OECMAKE_TARGET_COMPILE = "libhailort"
HAILORT_INCLUDE_STAGING_DIR = "${D}${includedir}"
HAILORT_EXPORT_DIR = "${D}${libdir}/cmake/HailoRT"
RDEPENDS:${PN} += "libatomic"

do_install:append() {
  install -d ${D}${libdir}
  install -m 0755 ${LIB_SRC_DIR}/libhailort.so.${PV} ${D}${libdir}
  ln -s -r ${D}${libdir}/libhailort.so.${PV} ${D}${libdir}/libhailort.so

  install -d ${HAILORT_INCLUDE_STAGING_DIR}
  cp -r ${S}/hailort/libhailort/include/* ${HAILORT_INCLUDE_STAGING_DIR}/

  install -d ${HAILORT_EXPORT_DIR}
  install -m 0644 ${WORKDIR}/build/hailort/libhailort/src/*.cmake ${HAILORT_EXPORT_DIR}
  install -m 0644 ${WORKDIR}/build/hailort/libhailort/src/CMakeFiles/Export/**/*.cmake ${HAILORT_EXPORT_DIR}
}

FILES:${PN} += "${libdir}/libhailort.so.${PV}"
FILES:${PN}-dev += "${includedir}/hailort ${includedir}/hailort/* ${libdir}/libhailort.so"
