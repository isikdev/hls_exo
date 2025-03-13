// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

// Указываем путь к SDK явно
ext {
    set("sdk.dir", System.getProperty("user.home") + "/AppData/Local/Android/Sdk")
}