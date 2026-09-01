DESCRIPTION = "gsthailo GStreamer plugin for Hailo-10H/15/Mars \
               compiles the hailo gstreamer plugin including hailonet against the master-branch \
               HailoRT. Recipe/package name is libgsthailo10 purely so bitbake can carry this \
               alongside the Hailo-8 build (libgsthailo_4.23.0.bb, PN libgsthailo) as two \
               separate recipes; phytec-hailo-image.bb's HAILO_CHIP flag ever only pulls in ONE \
               of the two into a given image, so the installed plugin is plain \
               gstreamer-1.0/libgsthailo.so either way."

LICENSE = "LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://hailort/LICENSE;md5=800c77403398cedcbbbcd86d37f5e0ff \
                    file://hailort/LICENSE-3RD-PARTY.md;md5=eb78bffb175a3f2be317bb4c45fedecf \
                    file://hailort/libhailort/bindings/gstreamer/LICENSE;md5=4b54a1fd55a448865a0b32d41598759d"

SRC_URI = "git://git@github.com/hailo-ai/hailort.git;protocol=https;branch=master"
SRCREV = "d503417f2a0db186a838390fb08690c4ea0f415e"

S = "${WORKDIR}/git"

inherit pkgconfig
inherit hailort-base

DEPENDS = "glib-2.0 gstreamer1.0 gstreamer1.0-plugins-base glib-2.0-native"
RDEPENDS:${PN} += "libhailort10"

EXTRA_OECMAKE:append = " -DHAILO_BUILD_GSTREAMER=1"
OECMAKE_TARGET_COMPILE = "gsthailo"

GST_HAILO_SOURCES_DIR = "${S}/hailort/libhailort/bindings/gstreamer/gst-hailo"
GST_HAILO_INCLUDE_STAGING_DIR = "${D}${includedir}/gst-hailo"
GST_HAILO_INCLUDE_STAGING_INCLUDE_DIR = "${GST_HAILO_INCLUDE_STAGING_DIR}/metadata"

do_install() {
    install -d ${D}${libdir}/gstreamer-1.0
    install -m 0755 ${LIB_SRC_DIR}libgsthailo.so ${D}${libdir}/gstreamer-1.0/libgsthailo.so

    install -d ${GST_HAILO_INCLUDE_STAGING_DIR}
    install -d ${GST_HAILO_INCLUDE_STAGING_INCLUDE_DIR}
    cd ${GST_HAILO_SOURCES_DIR}
    find . -type f -name \*.hpp -exec install -D {} ${GST_HAILO_INCLUDE_STAGING_DIR}/{} \;
    find . -type f -name \*.h -exec install -D {} ${GST_HAILO_INCLUDE_STAGING_INCLUDE_DIR}/{} \;
}

FILES:${PN} += "${libdir}/gstreamer-1.0/libgsthailo.so"
FILES:${PN}-dev += "${includedir}/gst-hailo ${includedir}/gst-hailo/*"
