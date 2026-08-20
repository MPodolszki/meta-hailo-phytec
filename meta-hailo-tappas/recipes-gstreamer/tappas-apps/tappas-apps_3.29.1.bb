DESCRIPTION = "TAPPAS ARM applications recipe, \
               the recipe copies the app script, hef files and media to ${ROOT_HOME}/apps \
               the apps hefs and media urls are taken from files/download_reqs.txt"

PV_PARSED = "${@ '${PV}'.replace('.0', '')}"
SRC_URI = "git://git@github.com/hailo-ai/tappas.git;protocol=https;branch=master"

S = "${WORKDIR}/git/core/hailo"

SRCREV = "4327923422ababaf3a9395f86bf39f5b34dcfd83"
LICENSE = "LGPL-2.1-only"
LIC_FILES_CHKSUM += "file://../../LICENSE;md5=4fbd65380cdd255951079008b364516c"

inherit hailotools-base

# Setting meson build target as 'apps'
TAPPAS_BUILD_TARGET = "apps"

DEPENDS += " gstreamer1.0 gstreamer1.0-plugins-base cxxopts rapidjson"
RDEPENDS:${PN} += " bash libgsthailotools"

LPR_APP_NAME = "license_plate_recognition"

OPENCV_UTIL = "libhailo_cv_singleton.so"
GST_IMAGES_UTIL = "libhailo_gst_image.so"

ROOTFS_APPS_DIR = "${D}${ROOT_HOME}/apps"

APPS_DIR_PREFIX = "${WORKDIR}/git/apps/"
IMX8_DIR = "${APPS_DIR_PREFIX}/h8/gstreamer/imx8/"
HAILO15_DIR = "${APPS_DIR_PREFIX}/h15/gstreamer/"

REQS_PATH = "${FILE_DIRNAME}/files/"
REQS_IMX8_FILE = "${REQS_PATH}download_reqs_imx8.txt"
REQS_HAILO15_FILE = "${REQS_PATH}download_reqs_hailo15.txt"

REQS_FILE = ""
ARM_APPS_DIR = ""
python () {
    if 'imx8' in d.getVar('MACHINE'):
        d.setVar('REQS_FILE', d.getVar('REQS_IMX8_FILE'))
        d.setVar('ARM_APPS_DIR', d.getVar('IMX8_DIR'))
    else:
        d.setVar('REQS_FILE', d.getVar('REQS_HAILO15_FILE'))
        d.setVar('ARM_APPS_DIR', d.getVar('HAILO15_DIR'))
        d.appendVar('DEPENDS', " libmedialib-api xtensor")
}

IS_H15 = "${@ 'true' if 'hailo15' in d.getVar('MACHINE') else 'false'}"
INSTALL_LPR = "true"

CURRENT_APP_NAME = ""
CURRENT_REQ_FILE = ""

# meson configuration
EXTRA_OEMESON += " \
        -Dapps_install_dir='${ROOT_HOME}/apps' \
        -Dinstall_lpr='${INSTALL_LPR}' \
        -Dlibcxxopts='${STAGING_INCDIR}/cxxopts' \
        -Dlibrapidjson='${STAGING_INCDIR}/rapidjson' \
        "
addtask install_requirements after do_install before do_package

do_fetch[prefuncs] += "do_set_requirements_src_uris"
do_unpack[prefuncs] += "do_set_requirements_src_uris"
do_cleanstate[prefuncs] += "do_set_requirements_src_uris"
do_cleanall[prefuncs] += "do_set_requirements_src_uris"
do_clean[prefuncs] += "do_set_requirements_src_uris"

do_install_requirements[depends]+=" virtual/fakeroot-native:do_populate_sysroot"

fakeroot install_app_dir() {
    # install app path on the rootfs
    install -d ${ROOTFS_APPS_DIR}/${CURRENT_APP_NAME}
    install -d ${ROOTFS_APPS_DIR}/${CURRENT_APP_NAME}/resources

    # copy the required file into the app path under resources directory
    install -m 0755 ${WORKDIR}/${CURRENT_REQ_FILE} ${ROOTFS_APPS_DIR}/${CURRENT_APP_NAME}/resources
    # copy the app shell script into the app path
    if ls ${ARM_APPS_DIR}/${CURRENT_APP_NAME}/*.sh >/dev/null 2>&1; then
    	install -m 0755 ${ARM_APPS_DIR}/${CURRENT_APP_NAME}/*.sh ${ROOTFS_APPS_DIR}/${CURRENT_APP_NAME}
    else
        bbnote ".sh file not found, skipping install"
    fi
    if [ -d "${ARM_APPS_DIR}/${CURRENT_APP_NAME}/configs" ]; then
        install -d ${ROOTFS_APPS_DIR}/${CURRENT_APP_NAME}/resources/configs
        install -m 0755 ${ARM_APPS_DIR}/${CURRENT_APP_NAME}/configs/* ${ROOTFS_APPS_DIR}/${CURRENT_APP_NAME}/resources/configs
    fi
}

do_install:append() {
    # Meson installs shared objects in apps target,
    # we remove it from the rootfs to prevent duplication with libgsthailotools
    rm -rf ${D}/usr/lib/libgsthailometa*
    rm -rf ${D}/usr/include/gsthailometa
    rm -rf ${D}/usr/lib/pkgconfig/gsthailometa.pc
    # rm -rf ${D}/usr/lib/libhailo_tracker*

    if [ '${IS_H15}' = 'true' ]; then
        install -d ${ROOTFS_APPS_DIR}/encoder_pipelines_new_api/configs/
        install -m 0755 ${S}/apps/hailo15/encoder_pipelines_new_api/*.json ${ROOTFS_APPS_DIR}/encoder_pipelines_new_api/configs/
    fi
}

python do_set_requirements_src_uris() {
    req_file = d.getVar('REQS_FILE')

    with open(req_file, "r") as req_file:
        for line in req_file:
            # iterate over download_reqs.txt, parse each line
            stripped_line = line.strip().split(' -> ')
            url = stripped_line[0]
            md5sum = stripped_line[2]
            # set src_uri from app url + md5sum, do_fetch task will use it
            src_uri = ' {};md5sum={}'.format(url, md5sum)
            d.appendVar('SRC_URI', src_uri)
}

fakeroot python do_install_requirements() {
    import glob
    import os

    req_file = d.getVar('REQS_FILE')
    workdir = d.getVar('WORKDIR')

    with open(req_file, "r") as req_file:
        for line in req_file:
            # iterate over download_reqs.txt, parse each line
            stripped_line = line.strip().split(' -> ')
            req_file = stripped_line[0].split('/')[-1]
            app_path = stripped_line[1]
            app_name = app_path.split('/')[-1]

            # Keep detection app resilient if a mirrored HEF filename differs.
            if app_name == 'detection':
                req_path = os.path.join(workdir, req_file)
                if not os.path.exists(req_path):
                    for candidate in ['yolov5m_wo_spp.hef', 'yolov8m.hef']:
                        candidate_path = os.path.join(workdir, candidate)
                        if os.path.exists(candidate_path):
                            req_file = candidate
                            break
                    else:
                        hef_candidates = sorted(glob.glob(os.path.join(workdir, '*.hef')))
                        if hef_candidates:
                            req_file = os.path.basename(hef_candidates[0])
                        else:
                            bb.fatal('No HEF artifact found in WORKDIR for detection app')

            # set app name and file variables and call install_app_dir
            d.setVar('CURRENT_APP_NAME', app_name)
            d.setVar('CURRENT_REQ_FILE', req_file)
            bb.build.exec_func('install_app_dir', d)
}

fakeroot python do_install_requirements:append() {
    # Keep detection app compatible with current HailoRT and ensure LVDS
    # output under Weston by installing a known-good launcher.
    import os
    import stat

    rootfs_apps_dir = d.getVar('ROOTFS_APPS_DIR')
    detection_dir = os.path.join(rootfs_apps_dir, 'detection')
    resources_dir = os.path.join(detection_dir, 'resources')
    detection_script = os.path.join(detection_dir, 'detection.sh')
    multistream_dir = os.path.join(rootfs_apps_dir, 'multistream_detection')
    multistream_script = os.path.join(multistream_dir, 'multi_stream_detection.sh')
    resnet_demo_script = os.path.join(rootfs_apps_dir, 'resnet_inference_demo.sh')

    preferred_hef = 'yolov5m_wo_spp.hef'
    fallback_hef = 'yolov8m.hef'

    target_hef = preferred_hef
    if not os.path.exists(os.path.join(resources_dir, preferred_hef)) and \
       os.path.exists(os.path.join(resources_dir, fallback_hef)):
        target_hef = fallback_hef

    detection_script_content = f'''#!/bin/bash
set -e

CURRENT_DIR="$(dirname "$(realpath "${{BASH_SOURCE[0]}}")")"

function init_variables() {{
    readonly RESOURCES_DIR="${{CURRENT_DIR}}/resources"
    readonly POSTPROCESS_DIR="/usr/lib/hailo-post-processes"
    readonly DEFAULT_POSTPROCESS_SO="$POSTPROCESS_DIR/libyolo_hailortpp_post.so"
    readonly DEFAULT_NETWORK_NAME="yolov5"
    readonly DEFAULT_VIDEO_SOURCE="/dev/video0"
    readonly DEFAULT_HEF_PATH="${{RESOURCES_DIR}}/{target_hef}"
    readonly DEFAULT_JSON_CONFIG_PATH="$RESOURCES_DIR/configs/yolov5.json"

    postprocess_so=$DEFAULT_POSTPROCESS_SO
    network_name=$DEFAULT_NETWORK_NAME
    input_source=$DEFAULT_VIDEO_SOURCE
    hef_path=$DEFAULT_HEF_PATH
    json_config_path=$DEFAULT_JSON_CONFIG_PATH

    if [ -S /run/user/1000/wayland-1 ]; then
        export XDG_RUNTIME_DIR=/run/user/1000
        export WAYLAND_DISPLAY=wayland-1
        video_sink="waylandsink"
    else
        video_sink="autovideosink"
    fi

    print_gst_launch_only=false
    additional_parameters=""
}}

function print_usage() {{
    echo "IMX8 Detection pipeline usage:"
    echo ""
    echo "Options:"
    echo "  --help              Show this help"
    echo "  -i INPUT --input INPUT          Set the video source (default $input_source)"
    echo "  --show-fps          Print fps"
    echo "  --print-gst-launch  Print the ready gst-launch command without running it"
    exit 0
}}

function parse_args() {{
    while test $# -gt 0; do
        if [ "$1" = "--help" ] || [ "$1" == "-h" ]; then
            print_usage
            exit 0
        elif [ "$1" = "--print-gst-launch" ]; then
            print_gst_launch_only=true
        elif [ "$1" = "--show-fps" ]; then
            echo "Printing fps"
            additional_parameters="-v | grep hailo_display"
        elif [ "$1" = "--input" ] || [ "$1" = "-i" ]; then
            input_source="$2"
            shift
        else
            echo "Received invalid argument: $1. See expected arguments below:"
            print_usage
            exit 1
        fi

        shift
    done
}}

init_variables $@
parse_args $@

PIPELINE="gst-launch-1.0 \
    v4l2src device=$input_source ! video/x-raw,format=YUY2,width=1280,height=720,framerate=30/1 ! \
    videoconvert ! videoscale ! video/x-raw,format=RGB,width=640,height=640,pixel-aspect-ratio=1/1 ! \
    queue leaky=downstream max-size-buffers=5 max-size-bytes=0 max-size-time=0 ! \
    hailonet hef-path=$hef_path ! \
    queue leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
    hailofilter config-path=$json_config_path so-path=$postprocess_so qos=false ! \
    queue leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
    hailooverlay ! \
    queue leaky=downstream max-size-buffers=5 max-size-bytes=0 max-size-time=0 ! \
    videoconvert ! \
    fpsdisplaysink video-sink=$video_sink name=hailo_display sync=false text-overlay=false ${{additional_parameters}}"

echo "Running $network_name"
echo "Video sink: $video_sink"
echo ${{PIPELINE}}

if [ "$print_gst_launch_only" = true ]; then
    exit 0
fi

eval ${{PIPELINE}}
'''

    if os.path.isdir(detection_dir):
        with open(detection_script, 'w', encoding='utf-8') as fh:
            fh.write(detection_script_content)
        os.chmod(detection_script, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR |
                                  stat.S_IRGRP | stat.S_IXGRP |
                                  stat.S_IROTH | stat.S_IXOTH)

    multistream_script_content = '''#!/bin/bash
set -e

CURRENT_DIR="$(dirname "$(realpath "${BASH_SOURCE[0]}")")"

function init_variables() {
    readonly RESOURCES_DIR="${CURRENT_DIR}/resources"
    readonly POSTPROCESS_DIR="/usr/lib/hailo-post-processes"
    readonly DEFAULT_POSTPROCESS_SO="$POSTPROCESS_DIR/libyolo_hailortpp_post.so"
    readonly DEFAULT_HEF_PATH="$RESOURCES_DIR/yolov5s_personface_nv12.hef"
    readonly DEFAULT_JSON_CONFIG_PATH="$RESOURCES_DIR/configs/yolov5_personface.json"

    postprocess_so=$DEFAULT_POSTPROCESS_SO
    hef_path=$DEFAULT_HEF_PATH
    json_config_path=$DEFAULT_JSON_CONFIG_PATH
    sync_pipeline=false
    num_of_src=6
    compositor_locations="sink_0::xpos=0 sink_0::ypos=0 sink_1::xpos=640 sink_1::ypos=0 sink_2::xpos=1280 sink_2::ypos=0 sink_3::xpos=1920 sink_3::ypos=0 sink_4::xpos=0 sink_4::ypos=640 sink_5::xpos=640 sink_5::ypos=640 sink_6::xpos=1280 sink_6::ypos=640 sink_7::xpos=1920 sink_7::ypos=640 sink_8::xpos=0 sink_8::ypos=1280"
    print_gst_launch_only=false

    if [ -S /run/user/1000/wayland-1 ]; then
        export XDG_RUNTIME_DIR=/run/user/1000
        export WAYLAND_DISPLAY=wayland-1
        videosink="waylandsink"
    else
        videosink="autovideosink"
    fi

    additional_parameters=""
}

function print_usage() {
    echo "Multistream Detection hailo - pipeline usage:"
    echo ""
    echo "Options:"
    echo "  --help                          Show this help"
    echo "  --show-fps                      Printing fps"
    echo "  --fakesink                      Run the application without display"
    echo "  --num-of-sources NUM            Setting number of sources to given input (default and maximum value is 6)"
    echo "  --print-gst-launch              Print the ready gst-launch command without running it"
    exit 0
}

function parse_args() {
    while test $# -gt 0; do
        if [ "$1" = "--help" ] || [ "$1" == "-h" ]; then
            print_usage
            exit 0
        elif [ "$1" = "--print-gst-launch" ]; then
            print_gst_launch_only=true
        elif [ "$1" = "--show-fps" ]; then
            echo "Printing fps"
            additional_parameters="-v | grep hailo_display"
        elif [ "$1" = "--fakesink" ]; then
            echo "Running without display"
            videosink="fakesink"
            sync_pipeline=true
        elif [ "$1" = "--num-of-sources" ]; then
            shift
            if [ $1 -gt 6 ]; then
                echo "Received number of sources: $1, but maximum number of sources is 6"
                exit 1
            fi
            echo "Setting number of sources to $1"
            num_of_src=$1
        else
            echo "Received invalid argument: $1. See expected arguments below:"
            print_usage
            exit 1
        fi
        shift
    done
}

function create_sources() {
    sources=""
    for ((n = 0; n < $num_of_src; n++)); do
        sources+="uridecodebin3 uri=file://$RESOURCES_DIR/detection$n.mp4 \
                name=source_$n ! videorate ! video/x-raw,framerate=30/1 ! \
                queue name=hailo_preprocess_q_$n leaky=no max-size-buffers=5 max-size-bytes=0 max-size-time=0 ! videoconvert ! \
                queue name=video_scale_q_$n leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
                videoscale ! video/x-raw,width=640,height=640,pixel-aspect-ratio=1/1 ! \
                fun.sink_$n sid.src_$n ! queue name=comp_q_$n leaky=downstream max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! comp.sink_$n "
    done
}

function main() {
    init_variables "$@"
    parse_args "$@"
    create_sources

    pipeline="gst-launch-1.0 \
           funnel name=fun ! \
           queue name=net max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
           synchailonet hef-path=$hef_path ! \
           queue name=filter leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
           hailofilter so-path=$postprocess_so config-path=$json_config_path qos=false ! \
           queue name=overlay leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
           hailooverlay ! \
           streamiddemux name=sid compositor name=comp start-time-selection=0 $compositor_locations ! \
           queue name=hailo_video_q_0 leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
           videoconvert ! queue name=hailo_display_q_0 leaky=no max-size-buffers=30 max-size-bytes=0 max-size-time=0 ! \
           fpsdisplaysink video-sink=$videosink name=hailo_display sync=false text-overlay=false \
           $sources ${additional_parameters}"

    echo "$pipeline"
    if [ "$print_gst_launch_only" = true ]; then
        exit 0
    fi

    echo "Running Pipeline..."
    eval "$pipeline"
}

main "$@"
'''

    if os.path.isdir(multistream_dir):
        with open(multistream_script, 'w', encoding='utf-8') as fh:
            fh.write(multistream_script_content)
        os.chmod(multistream_script, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR |
                                    stat.S_IRGRP | stat.S_IXGRP |
                                    stat.S_IROTH | stat.S_IXOTH)

    resnet_demo_script_content = '''#!/bin/bash
set -e

HEF_PATH="/usr/lib/python3.12/site-packages/hailo_tutorials/hefs/resnet_v1_18.hef"
print_gst_launch_only=false
show_fps=false
source_mode="test"
camera_dev="/dev/video0"

if [ -S /run/user/1000/wayland-1 ]; then
    export XDG_RUNTIME_DIR=/run/user/1000
    export WAYLAND_DISPLAY=wayland-1
    video_sink="waylandsink"
else
    video_sink="autovideosink"
fi

while test $# -gt 0; do
    case "$1" in
        --help|-h)
            echo "ResNet inference demo"
            echo "Options:"
            echo "  --camera [DEV]       Use v4l2 camera source (default /dev/video0)"
            echo "  --fakesink           Run headless"
            echo "  --show-fps           Print gst verbose output"
            echo "  --print-gst-launch   Print pipeline and exit"
            exit 0
            ;;
        --camera)
            source_mode="camera"
            if [ -n "$2" ] && [ "${2#-}" = "$2" ]; then
                camera_dev="$2"
                shift
            fi
            ;;
        --fakesink)
            video_sink="fakesink"
            ;;
        --show-fps)
            show_fps=true
            ;;
        --print-gst-launch)
            print_gst_launch_only=true
            ;;
        *)
            echo "Unknown argument: $1"
            exit 1
            ;;
    esac
    shift
done

if [ "$source_mode" = "camera" ]; then
    SRC="v4l2src device=$camera_dev ! video/x-raw,format=YUY2,width=1280,height=720,framerate=30/1 ! videoconvert ! videoscale ! video/x-raw,format=RGB,width=224,height=224,pixel-aspect-ratio=1/1"
else
    SRC="videotestsrc is-live=true pattern=ball ! video/x-raw,format=RGB,width=224,height=224,framerate=30/1"
fi

if [ "$video_sink" = "fakesink" ]; then
    SINK_CHAIN="fakesink sync=false"
else
    SINK_CHAIN="videoconvert ! videoscale ! video/x-raw,width=1280,height=720 ! $video_sink sync=false"
fi

EXTRA=""
if [ "$show_fps" = true ]; then
    EXTRA="-v"
fi

PIPELINE="gst-launch-1.0 $EXTRA \
    $SRC ! \
    queue leaky=downstream max-size-buffers=5 max-size-bytes=0 max-size-time=0 ! \
    hailonet hef-path=$HEF_PATH ! \
    queue leaky=downstream max-size-buffers=5 max-size-bytes=0 max-size-time=0 ! \
    $SINK_CHAIN"

echo "Running ResNet inference demo"
echo "$PIPELINE"

if [ "$print_gst_launch_only" = true ]; then
    exit 0
fi

eval "$PIPELINE"
'''

    with open(resnet_demo_script, 'w', encoding='utf-8') as fh:
        fh.write(resnet_demo_script_content)
    os.chmod(resnet_demo_script, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR |
                                 stat.S_IRGRP | stat.S_IXGRP |
                                 stat.S_IROTH | stat.S_IXOTH)
}


FILES:${PN} += " ${ROOT_HOME}/apps/* ${ROOT_HOME}/apps/${LPR_APP_NAME}/* ${ROOT_HOME}/apps/${LPR_APP_NAME}/resources/* /usr/lib/${OPENCV_UTIL}.${PV} /usr/lib/${GST_IMAGES_UTIL}.${PV}"
FILES:${PN}-lib += "/usr/lib/${OPENCV_UTIL}.${PV} /usr/lib/${GST_IMAGES_UTIL}.${PV}"
RDEPENDS:${PN}-staticdev = ""
RDEPENDS:${PN}-dev = ""
RDEPENDS:${PN}-dbg = ""
