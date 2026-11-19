/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

// The MediaTek AIDL audio HAL (android.hardware.audio.core-impl-mediatek) takes
// legacy "key=value;key=value" strings in VendorParameter::id and hands them to the
// legacy setParameters() of the HAL. Without a vendor extension the parameter parser
// rejects every vendor parameter, so AudioSystem.setParameters() never reaches the
// HAL (for example the MediaTek in-call volume keys volumeIndex/volumeDevice).

#define LOG_TAG "MtkHalAdapterVendorExt"

#include <aidl/android/media/audio/BnHalAdapterVendorExtension.h>
#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <media/AudioParameter.h>

#include <string>
#include <vector>

namespace {

using ::aidl::android::hardware::audio::core::VendorParameter;
using ::aidl::android::media::audio::BnHalAdapterVendorExtension;
using ::aidl::android::media::audio::IHalAdapterVendorExtension;
using ParameterScope = IHalAdapterVendorExtension::ParameterScope;
using ScopeType = IHalAdapterVendorExtension::ScopeType;

class MtkHalAdapterVendorExtension : public BnHalAdapterVendorExtension {
  public:
    ::ndk::ScopedAStatus parseVendorParameterIds(const ParameterScope& in_scope,
                                                 const std::string& in_rawKeys,
                                                 std::vector<std::string>* _aidl_return) override {
        // Reading vendor parameters back needs the HAL's private result parcelable;
        // only keep the generic key that AOSP understands.
        if (in_scope.scope == ScopeType::MODULE) {
            ::android::AudioParameter params(::android::String8(in_rawKeys.c_str()));
            if (params.containsKey(
                        ::android::String8(::android::AudioParameter::keyClipTransitionSupport))) {
                _aidl_return->emplace_back(::android::AudioParameter::keyClipTransitionSupport);
            }
        }
        return ::ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus parseVendorParameters(const ParameterScope& in_scope,
                                               const std::string& in_rawKeysAndValues,
                                               std::vector<VendorParameter>* out_syncParameters,
                                               std::vector<VendorParameter>*) override {
        if (!in_rawKeysAndValues.empty()) {
            LOG(VERBOSE) << __func__ << ": scope: " << in_scope.toString()
                         << ", keys/values: " << in_rawKeysAndValues;
            VendorParameter param;
            param.id = in_rawKeysAndValues;
            out_syncParameters->push_back(std::move(param));
        }
        return ::ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus parseBluetoothA2dpReconfigureOffload(
            const std::string&, std::vector<VendorParameter>*) override {
        return ::ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus parseBluetoothLeReconfigureOffload(
            const std::string&, std::vector<VendorParameter>*) override {
        return ::ndk::ScopedAStatus::ok();
    }

    ::ndk::ScopedAStatus processVendorParameters(const ParameterScope& in_scope,
                                                 const std::vector<VendorParameter>& in_parameters,
                                                 std::string* _aidl_return) override {
        if (in_scope.scope == ScopeType::MODULE) {
            ::android::AudioParameter result;
            for (const auto& param : in_parameters) {
                if (param.id == ::android::AudioParameter::keyClipTransitionSupport) {
                    result.addInt(
                            ::android::String8(::android::AudioParameter::keyClipTransitionSupport),
                            true);
                }
            }
            *_aidl_return = result.toString().c_str();
        }
        return ::ndk::ScopedAStatus::ok();
    }
};

}  // namespace

extern "C" __attribute__((visibility("default"))) int createIHalAdapterVendorExtension() {
    auto extension = ::ndk::SharedRefBase::make<MtkHalAdapterVendorExtension>();
    const std::string name = std::string(IHalAdapterVendorExtension::descriptor) + "/default";
    binder_status_t status = AServiceManager_addService(extension->asBinder().get(), name.c_str());
    if (status != STATUS_OK) {
        LOG(ERROR) << "failed to register " << name << ": " << status;
    } else {
        LOG(INFO) << "registered " << name;
    }
    return status;
}
