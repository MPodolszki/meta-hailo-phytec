FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://0001-cmake-deps.txt-Fix-eigen-hash-value.patch \
"

# onnxruntime's mlas kernels (core/mlas/lib/aarch64/*) compile some source
# files with their own hardcoded -march=armv8.2-a+{dotprod,i8mm,fp16,bf16}
# flags for their NEON micro-kernel variants. GCC rejects combining that
# with the board's -mcpu=cortex-a53+crc+crypto (injected globally via
# TUNE_CCARGS): "switch ... conflicts with ... switch". Clearing
# TUNE_CCARGS for just this recipe drops the conflicting -mcpu and lets
# onnxruntime's own -march flags apply uncontested; this only affects how
# onnxruntime itself is compiled, not the rest of the image (kernel,
# u-boot, ...), which keep using the board's real tune.
TUNE_CCARGS = ""
