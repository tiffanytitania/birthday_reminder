@file:Suppress("DEPRECATION")

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}


tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
