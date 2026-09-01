DESCRIPTION = "hailortcli - command line utility wrapper for libhailort operations on \
               Hailo-10H/15/Mars, including inference, fw controls, measurements and more. \
               Recipe/package name is hailort10cli purely so bitbake can carry this alongside \
               the Hailo-8 build (hailortcli_4.23.0.bb, PN hailortcli) as two separate recipes; \
               phytec-hailo-image.bb's HAILO_CHIP flag ever only pulls in ONE of the two into a \
               given image, so the installed binary is plain bindir/hailortcli either way, \
               matching upstream Hailo documentation and scripts."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://hailort/LICENSE;md5=800c77403398cedcbbbcd86d37f5e0ff \
                    file://hailort/LICENSE-3RD-PARTY.md;md5=eb78bffb175a3f2be317bb4c45fedecf"

SRC_URI = "git://git@github.com/hailo-ai/hailort.git;protocol=https;branch=master"
SRCREV = "d503417f2a0db186a838390fb08690c4ea0f415e"

S = "${WORKDIR}/git"

inherit hailort-base

RDEPENDS:${PN} += "libhailort10"
OECMAKE_TARGET_COMPILE = "hailortcli"

do_install:append() {
  install -d ${D}${bindir}
  install -m 0755 ${BIN_SRC_DIR}/hailortcli ${D}${bindir}
}

FILES:${PN} += "${bindir}/hailortcli"
