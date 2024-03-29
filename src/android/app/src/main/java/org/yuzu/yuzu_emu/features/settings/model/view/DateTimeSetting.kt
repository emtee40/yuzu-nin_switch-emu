// SPDX-FileCopyrightText: 2023 yuzu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

package org.yuzu.yuzu_emu.features.settings.model.view

import org.yuzu.yuzu_emu.features.settings.model.AbstractLongSetting

class DateTimeSetting(
    private val longSetting: AbstractLongSetting,
    titleId: Int,
    descriptionId: Int
) : SettingsItem(longSetting, titleId, descriptionId) {
    override val type = TYPE_DATETIME_SETTING

    fun getValue(needsGlobal: Boolean = false): Long = longSetting.getLong(needsGlobal)
    fun setValue(value: Long) = (setting as AbstractLongSetting).setLong(value)
}
