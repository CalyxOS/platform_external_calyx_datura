/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

package org.calyxos.datura.models

data class Header(
    val name: String = String(),
    override val type: Type = Type.HEADER
) : DaturaItem
