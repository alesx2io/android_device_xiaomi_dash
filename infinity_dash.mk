# --- Infinity X ---
INFINITY_MAINTAINER := 'alesx2io'
WITH_GAPPS := true
TARGET_HAS_UDFPS := true
PRODUCT_SYSTEM_PARTITIONS_FILE_SYSTEM_TYPE := erofs

#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from dash device
$(call inherit-product, device/xiaomi/dash/device.mk)

# Inherit some common LineageOS stuff.
$(call inherit-product, vendor/infinity/config/common_full_phone.mk)

ifeq ($(PRODUCT_BRAND),EvolutionX)
# Inherit some common Evolution-X stuff.
$(call inherit-product, device/xiaomi/dash/evolution.mk)
endif

# Device identifier.
PRODUCT_DEVICE := dash
PRODUCT_NAME := infinity_dash
PRODUCT_BRAND := POCO
PRODUCT_MODEL := 2602BPC18G
PRODUCT_MANUFACTURER := Xiaomi

# Device attestation
PRODUCT_BRAND_FOR_ATTESTATION := $(PRODUCT_BRAND)
PRODUCT_DEVICE_FOR_ATTESTATION := $(PRODUCT_DEVICE)
PRODUCT_MODEL_FOR_ATTESTATION := $(PRODUCT_MODEL)
PRODUCT_NAME_FOR_ATTESTATION := dash_global
PRODUCT_MANUFACTURER_FOR_ATTESTATION := $(PRODUCT_MANUFACTURER)

# GMS
PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

# Properties
PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="missi-user 16 BP2A.250605.031.A3 16OS3.1.260922.145821649.MTPEGL.S release-keys" \
    BuildFingerprint=POCO/dash_global/dash:16/BP2A.250605.031.A3/OS3.0.304.0.WPLMIXM:user/release-keys \
    DeviceName=dash \
    DeviceProduct=dash_global \
    SystemDevice=dash \
    SystemName=dash_global

PRODUCT_SYSTEM_PROPERTIES += \
    ro.product.marketname=POCO X8 Pro Max \
    ro.infinity.soc=Dimensity 9500s \
    ro.infinity.camera=50MP + 8MP
