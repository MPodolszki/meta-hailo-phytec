SUMMARY = "ANTLR 4.9 runtime for Python 3"
DESCRIPTION = "Pinned to 4.9.3 because omegaconf's pre-generated ANTLR grammar \
               (used by hailo-model-zoo, see hailo-python-common-wheels) embeds \
               a serialized ATN at version 4, which the 4.13.x runtime already \
               present in meta-openembedded refuses to deserialize."
HOMEPAGE = "http://www.antlr.org"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://PKG-INFO;md5=86db285bb0a3d95f91912f736520a6bf"

PYPI_PACKAGE = "antlr4-python3-runtime"

inherit pypi python_setuptools_build_meta

SRC_URI[sha256sum] = "f224469b4168294902bb1efa80a8bf7855f24c99aef99cbefc1bcd3cce77881b"

BBCLASSEXTEND = "nativesdk native"
