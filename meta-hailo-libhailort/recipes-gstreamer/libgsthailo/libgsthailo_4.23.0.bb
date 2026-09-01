DESCRIPTION = "gsthailo GStreamer plugin for Hailo-8/8L/8R \
               compiles the hailo gstreamer plugin including hailonet against the hailo8-branch HailoRT \
               Kept as libgsthailo (not libgsthailo8) so it matches existing Hailo-8 setups - the \
               Hailo-10/15 build is the one renamed to libgsthailo10 instead \
               (libgsthailo10_5.3.0.bb). Note: both plugins still register a GStreamer element \
               named 'hailonet' internally, so only one of the two can be the active hailonet \
               implementation in a given running GStreamer process - do not expect to mix \
               Hailo-8 and Hailo-10 pipelines in the same process."

LICENSE = "LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://hailort/LICENSE;md5=ed57bbf10be0c74ecf2c80710208b2b3 \
                    file://hailort/LICENSE-3RD-PARTY.md;md5=87f8edc8e3d5342f8b0614df5bae3b58 \
                    file://hailort/libhailort/bindings/gstreamer/LICENSE;md5=4b54a1fd55a448865a0b32d41598759d"

SRC_URI = "git://git@github.com/hailo-ai/hailort.git;protocol=https;branch=hailo8"
SRCREV = "08f088d3b443c7846af067269ce998c6d5d91449"

S = "${WORKDIR}/git"

inherit pkgconfig
inherit hailort-base

DEPENDS = "glib-2.0 gstreamer1.0 gstreamer1.0-plugins-base glib-2.0-native"
RDEPENDS:${PN} += "libhailort"

EXTRA_OECMAKE:append = " -DHAILO_BUILD_GSTREAMER=1"
OECMAKE_TARGET_COMPILE = "gsthailo"

GST_HAILO_SOURCES_DIR = "${S}/hailort/libhailort/bindings/gstreamer/gst-hailo"
GST_HAILO_INCLUDE_STAGING_DIR = "${D}${includedir}/gst-hailo"

do_install() {
    install -d ${D}${libdir}/gstreamer-1.0
    install -m 0755 ${LIB_SRC_DIR}libgsthailo.so ${D}${libdir}/gstreamer-1.0/libgsthailo.so

    install -d ${GST_HAILO_INCLUDE_STAGING_DIR}
    cd ${GST_HAILO_SOURCES_DIR}
    find . -type f -name \*.hpp -exec install -D {} ${GST_HAILO_INCLUDE_STAGING_DIR}/{} \;
    find . -type f -name \*.h   -exec install -D {} ${GST_HAILO_INCLUDE_STAGING_DIR}/{} \;
    for subdir in metadata hailo_events; do
        install -D ${GST_HAILO_SOURCES_DIR}/include/hailo_gst.h \
            ${GST_HAILO_INCLUDE_STAGING_DIR}/${subdir}/include/hailo_gst.h
    done
}

FILES:${PN} += "${libdir}/gstreamer-1.0/libgsthailo.so"
FILES:${PN}-dev += "${includedir}/gst-hailo ${includedir}/gst-hailo/*"
