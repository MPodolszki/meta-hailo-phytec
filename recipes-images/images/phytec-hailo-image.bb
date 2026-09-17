require recipes-images/images/phytec-vision-image.bb

SUMMARY = "PHYTEC's AiKit Hailo image: the complete accelerator stack, no demos"
LICENSE = "MIT"

# Which Hailo accelerator chip this image is built for: "hailo8" (Hailo-8/8L/8R) or "hailo10"
# (Hailo-10H/15/Mars). Only ONE full stack (firmware, kernel driver, hailortcli, libhailort,
# libgsthailo, Python bindings) ever goes into a given image - not both. That's a real
# constraint, not just a style choice: HailoRT's master branch (used for Hailo-10/15) dropped
# Hailo-8 support entirely (see the hailo-ai/hailort README), so Hailo-8 needs a separate,
# older HailoRT line (branch 'hailo8'/4.23.0) with its own driver, firmware and Python wheel.
# Because each build only ever installs one of the two stacks, hailortcli/libhailort.so/
# libgsthailo.so keep their normal, doc-matching names no matter which chip is selected - the
# Hailo-10 packages (PN suffixed "10": hailort10cli, libhailort10, libgsthailo10) still install
# those exact same plain filenames, the "10" only exists so bitbake can carry both recipes
# side by side; whichever one HAILO_CHIP does NOT select is simply never installed.
#
# Hailo-8 boards release first, so that's the default. To build an image for a Hailo-10 board
# instead, set HAILO_CHIP = "hailo10" (e.g. in local.conf, on the bitbake command line, or by
# editing the line below) and rebuild.
HAILO_CHIP ?= "hailo8"

def hailo_chip_packages(d):
    chip = d.getVar('HAILO_CHIP')
    if chip == "hailo8":
        # HailoRT 'hailo8' branch/4.23.0 - the last HailoRT line that still supports Hailo-8.
        # pyhailort (not hailo8-python-wheels): the prebuilt wheel's cp312 extension module
        # cannot be imported by this BSP's Python 3.13, so the binding is built from source
        # against the target interpreter instead. See pyhailort_4.23.0.bb.
        packages = "hailo8-firmware hailo8-pci libhailort hailortcli libgsthailo pyhailort"
    elif chip == "hailo10":
        # HailoRT master branch/5.3.0 - Hailo-10H/15/Mars.
        packages = "hailo-firmware hailo-pci libhailort10 hailort10cli libgsthailo10 hailo-python-wheels"
    else:
        bb.fatal("HAILO_CHIP must be 'hailo8' or 'hailo10', got '%s'" % chip)
    return packages

# The complete Hailo stack and nothing that only a demo needs: this image has to
# stand on its own without meta-hailo-examples-phytec. The demos, the kiosk loop
# and what they pull in are added on top by phytec-hailo-demo-image over there.
IMAGE_INSTALL += "\
    packagegroup-imx-ml \
    python3-pip \
    git \
    python3-netifaces \
"

#adding Hailo Packages to the Phytecs AI Image
IMAGE_INSTALL:append = " ${@hailo_chip_packages(d)} hailo-python-common-wheels"
# IMAGE_INSTALL:append = " libgsthailotools hailo-post-processes "
# IMAGE_INSTALL:append = " tappas-apps tappas-tracers"


#Adding Dependencies - In a PHYTEC BSP these are already met
IMAGE_INSTALL:append = " gstreamer1.0 gstreamer1.0-plugins-base "
IMAGE_INSTALL:append = " gstreamer1.0-plugins-good gstreamer1.0-plugins-bad "
