/*
 * SPDX-FileCopyrightText: 2023 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.20" apply false
    id("org.jetbrains.kotlin.kapt") version "1.8.20" apply false
    id("com.google.dagger.hilt.android") version "2.45" apply false
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1" apply false
}
