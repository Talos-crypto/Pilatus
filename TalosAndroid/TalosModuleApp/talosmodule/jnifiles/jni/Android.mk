LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := crypto
LOCAL_SRC_FILES := libs/libcrypto.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ssl
LOCAL_SRC_FILES := libs/libssl.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := relic
LOCAL_SRC_FILES := libs/relic_256_end/librelic_s.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := relic_80
LOCAL_SRC_FILES := libs/relic_158_end_gmp/librelic_s.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := relic_gmp
LOCAL_SRC_FILES := libs/relic_256_end_gmp/librelic_s.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := gmp
LOCAL_SRC_FILES := libs/libgmp.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := ecelgamal
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES := gamalcryptofinal.c
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES := crypto ssl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := eccrypto
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES := eccryptofinal.c
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES := crypto ssl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := pailliercrypto
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES := paillier.c
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES := crypto ssl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := relic-proxy-re-enc
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_SRC_FILES := proxy-re-wrapper.c pre-hom.c pre-hom.h
LOCAL_LDLIBS := -llog -Wl,--no-warn-shared-textrel
#LOCAL_STATIC_LIBRARIES := relic_gmp gmp
#LOCAL_STATIC_LIBRARIES := relic_80 gmp
LOCAL_STATIC_LIBRARIES := relic
include $(BUILD_SHARED_LIBRARY)



