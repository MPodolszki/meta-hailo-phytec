SUMMARY = "A hierarchical configuration system for Python"
DESCRIPTION = "Required by hailo-model-zoo (hailomz), which is packaged in \
               hailo-python-common-wheels but does not declare this dependency \
               there. Pins python3-antlr4-runtime to 4.9.3 -- see that recipe."
HOMEPAGE = "https://github.com/omry/omegaconf"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=7404c7fb5d66cfa18ddcd7b007a3e157"

PYPI_PACKAGE = "omegaconf"

inherit pypi python_setuptools_build_meta

# Upstream's build_py always re-runs ANTLR via a bundled antlr-4.9.3-complete.jar
# before building, even though the sdist already ships the generated grammar --
# see the patch. This build has no Java, so skip the redundant regeneration.
SRC_URI += "file://0001-Don-t-regenerate-the-ANTLR-grammar-at-build-time.patch"

SRC_URI[sha256sum] = "e5e7de64aeebeddaf8e6d3f7a783b32ac2a01c0fbd9c878012caecb891a1f42a"

RDEPENDS:${PN} += " \
    python3-antlr4-runtime (= 4.9.3) \
    python3-pyyaml \
"

BBCLASSEXTEND = "nativesdk native"
