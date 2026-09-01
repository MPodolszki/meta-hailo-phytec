DESCRIPTION = "hailo pcie driver for hailo8 \
               compiles the kernel driver for pci communication with hailo8/hailo8l/hailo8r \
               from the hailort-drivers 'hailo8' branch. Builds hailo_pci.ko, which is a \
               separate kernel module from hailo1x_pci.ko (built by hailo-pci_5.3.0.bb for \
               Hailo-10H/15/Mars) - the two use different module names, driver names \
               ('hailo' vs 'hailo1x') and PCI device IDs, so both can be loaded at the same time."

LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://../../LICENSE;md5=39bba7d2cf0ba1036f2a6e2be52fe3f0"

SRC_URI = "git://git@github.com/hailo-ai/hailort-drivers.git;protocol=https;branch=hailo8"
SRCREV = "ce1087bfe8132c99b41374e3128fc78612a3f492"

inherit module

S = "${WORKDIR}/git/linux/pcie"

EXTRA_OEMAKE += "KERNEL_DIR=${STAGING_KERNEL_DIR}"
MAKE_TARGETS = "all"
MODULES_INSTALL_TARGET = "install"
