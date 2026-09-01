DESCRIPTION = "hailo8 chip firmware (hailo8_fw.bin) \
               the recipe copies the file to ${nonarch_base_libdir}/firmware/hailo on the target \
               device's root file system - flat, not under a hailo8/ subdirectory, because the \
               hailo_pci kernel driver (hailo8 branch) requests firmware via request_firmware() \
               with the literal path \"hailo/hailo8_fw.bin\" (confirmed on real Hailo-8 hardware: \
               a hailo8/ subdirectory makes the driver fail firmware load with -ENOENT and the \
               board never comes up, dmesg shows 'Failed with error -2 to write file \
               hailo/hailo8_fw.bin'). This does not collide with the Hailo-10H firmware installed \
               by hailo-firmware_5.3.0.bb under .../firmware/hailo/hailo10h/, and the two are \
               never installed into the same image anyway (see HAILO_CHIP in \
               phytec-hailo-image.bb)."

BASE_URI = "https://hailo-hailort.s3.eu-west-2.amazonaws.com"
FW_AWS_DIR = "Hailo8/${PV}/FW"
FW = "hailo8_fw.${PV}.bin"
LICENSE_FILE = "LICENSE"
SRC_URI = "${BASE_URI}/${FW_AWS_DIR}/${FW};unpack=0;md5sum=a9a80585a6733674e2ca158ec15971c4 \
		${BASE_URI}/${FW_AWS_DIR}/${LICENSE_FILE};md5sum=263ee034adc02556d59ab1ebdaea2cda"

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://${WORKDIR}/${LICENSE_FILE};md5=263ee034adc02556d59ab1ebdaea2cda"

FW_PATH = "${WORKDIR}/${FW}"

do_install() {
	install -d ${D}${nonarch_base_libdir}/firmware/hailo
	install -m 0755 ${FW_PATH} ${D}${nonarch_base_libdir}/firmware/hailo/hailo8_fw.bin
}

FILES:${PN} += "${nonarch_base_libdir}/firmware/hailo/hailo8_fw.bin"
