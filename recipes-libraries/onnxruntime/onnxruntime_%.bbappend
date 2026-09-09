# onnxruntime's mlas kernels (core/mlas/lib/aarch64/*) compile some source
# files with their own hardcoded -march=armv8.2-a+{dotprod,i8mm,fp16,bf16}
# flags for their NEON micro-kernel variants. GCC rejects combining that
# with the board's -mcpu=cortex-a53+crc+crypto (injected globally via
# TUNE_CCARGS): "switch ... conflicts with ... switch". Clearing
# TUNE_CCARGS for just this recipe drops the conflicting -mcpu and lets
# onnxruntime's own -march flags apply uncontested; this only affects how
# onnxruntime itself is compiled, not the rest of the image (kernel,
# u-boot, ...), which keep using the board's real tune.
#
# Do NOT "fix" this with a global DEFAULTTUNE in conf/layer.conf -- that
# silently retunes every package on the board including u-boot and ATF,
# and broke boot completely (no UART output at all).
#
# Was onnxruntime_1.17.1.bbappend on the scarthgap branch, together with an
# eigen-hash patch for that exact version. walnascar's BSP carries 1.22.0
# (meta-imx/meta-imx-ml), whose cmake/deps.txt no longer needs the patch, so
# the append is now version-agnostic and carries only the tune fix.
TUNE_CCARGS = ""
