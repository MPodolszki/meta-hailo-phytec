require recipes-images/images/phytec-facematch-image.bb

SUMMARY =  "PHYTEC's AiKit Hailo Demo image"
LICENSE = "MIT"

IMAGE_INSTALL += "\
    packagegroup-imx-ml \
    python3-pip \
    git \
    python3-netifaces \
"

#adding Hailo Packages to the Phytecs AI Image
IMAGE_INSTALL:append = " hailo-firmware libhailort hailortcli hailo-pci libgsthailo "
# IMAGE_INSTALL:append = " libgsthailotools hailo-post-processes "
IMAGE_INSTALL:append = " hailo-python-wheels"
# IMAGE_INSTALL:append = " tappas-apps tappas-tracers"


#Adding Dependencies - In a PHYTEC BSP these are already met
IMAGE_INSTALL:append = " gstreamer1.0 gstreamer1.0-plugins-base "
IMAGE_INSTALL:append = " gstreamer1.0-plugins-good gstreamer1.0-plugins-bad "

# Whisper speech recognition benchmark, i.MX8MP NPU vs Hailo-8.
# Pulls in ~280 MB of models via demo-whisper-benchmark-data, plus ~230 MB
# of cached NPU graph binaries under /var/cache at first run.
IMAGE_INSTALL:append = " demo-whisper-benchmark"

HAILO_SOC_NAME = "hailo8"
