#!/usr/bin/env python3
"""Remove the optional OpenCL GPU backend from the Android llama.kt build.

MIMO currently uses CPU inference as its supported baseline. Android NDK
does not ship libOpenCL.so, and this device/runtime build must not require it.
Vulkan sources remain available; the model itself is never touched.
"""
from pathlib import Path

cmake = Path("libs/llama.kt/llama-kt/src/main/cpp/CMakeLists.txt")
source = cmake.read_text()

replacements = [
    ("    ${SRC_DIR}/ggml-opencl/ggml-opencl.cpp\n", ""),
    ("    ${SRC_DIR}/ggml-opencl/cl-program-cache.cpp\n", ""),
    ("    ${GGML_OPENCL_KERNEL_HEADERS}\n", ""),
    ("    ${SRC_DIR}/../../../../third_party/OpenCL-Headers\n", ""),
    ("    -DLM_GGML_USE_OPENCL\n", ""),
    ("    -DLM_GGML_OPENCL_USE_ADRENO_KERNELS\n", ""),
    ("    -DLM_GGML_OPENCL_EMBED_KERNELS\n", ""),
    ("    -DLM_GGML_OPENCL_SOA_Q\n", ""),
    ("    OpenCL\n", ""),
]
for old, new in replacements:
    if old not in source:
        raise SystemExit(f"Expected OpenCL CMake entry not found: {old.strip()}")
    source = source.replace(old, new)

# The unused OpenCL kernel embedding setup is harmless, but remove it too so
# the Android CPU build doesn't spend time generating unused OpenCL headers.
start = source.find("# ── OpenCL kernel embedding")
end = source.find("# ── Library target", start)
if start < 0 or end < 0:
    raise SystemExit("Could not locate OpenCL kernel embedding CMake block")
source = source[:start] + source[end:]

# Remove stale OpenCL link-directory setup. Keep Vulkan and Android system libs.
old = """# OpenCL stub (runtime link via jniLibs stub .so)
set(OPENCL_STUB ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libOpenCL.so)

target_link_directories(llamakt PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}
)

"""
if old not in source:
    raise SystemExit("Expected OpenCL stub/link-directory block not found")
source = source.replace(old, "")

if "OpenCL" in source or "ggml-opencl" in source:
    raise SystemExit("OpenCL references remain in Android CMake after patch")

cmake.write_text(source)
print("OpenCL backend disabled; CPU + Vulkan build retained.")
