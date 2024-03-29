// SPDX-FileCopyrightText: Copyright 2018 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

#pragma once

#include "core/hle/service/cmif_types.h"
#include "core/hle/service/service.h"

namespace Service {

namespace Nvnflinger {
class Nvnflinger;
}

namespace AM {

struct Applet;
class IApplicationProxy;

class IApplicationProxyService final : public ServiceFramework<IApplicationProxyService> {
public:
    explicit IApplicationProxyService(Core::System& system_, Nvnflinger::Nvnflinger& nvnflinger);
    ~IApplicationProxyService() override;

private:
    Result OpenApplicationProxy(Out<SharedPointer<IApplicationProxy>> out_application_proxy,
                                ClientProcessId pid, InCopyHandle<Kernel::KProcess> process_handle);

private:
    std::shared_ptr<Applet> GetAppletFromProcessId(ProcessId pid);
    Nvnflinger::Nvnflinger& m_nvnflinger;
};

} // namespace AM
} // namespace Service
