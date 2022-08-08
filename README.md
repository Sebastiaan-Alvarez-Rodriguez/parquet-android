# parquet-android
[![<Sebastiaan-Alvarez-Rodriguez>](https://circleci.com/gh/Sebastiaan-Alvarez-Rodriguez/parquet-android.svg?style=svg)](https://app.circleci.com/pipelines/github/Sebastiaan-Alvarez-Rodriguez/parquet-android)
[![](https://jitpack.io/v/Sebastiaan-Alvarez-Rodriguez/parquet-android.svg)](https://jitpack.io/#Sebastiaan-Alvarez-Rodriguez/parquet-android)

This project satisfies 2 goals:
 1. Read and write snappy-compressed parquet on Android devices.
 2. use minimal dependencies, minimal size.

Current package is under 3MB.

## Versioning
Parquet-android versioning scheme follows the versions of the parquet library.
E.g. Parquet-android `1.12.3` provides parquet library version `1.12.3`.  
Our Java bindings are compiled for Java 11.  
Our Android bindings are compiled for `minSdk=29, targetSdk=32, compileSdk=32`.


## Depending on this project
We publish [releases](https://github.com/Sebastiaan-Alvarez-Rodriguez/parquet-android/releases)
on jitpack.

In your project root `build.gradle`:
```groovy
allprojects {
    repositories {
        // Other repositories...
        maven { url 'https://jitpack.io' }
    }
}
```

In your module `build.gradle`:
```groovy
dependencies {
    implementation 'com.github.Sebastiaan-Alvarez-Rodriguez:parquet-android:1.12.3'
}
```

## Using this project
See [example](parquet-android/src/test/java/org/sebastiaan/parquet/android/ParquetTest.java)
for reading and writing parquet files.

## FAQ
Q: Which compression algorithms are supported?
A: We currently support `uncompressed` out of the box for writing and reading.
Add [snappy-android](https://github.com/Sebastiaan-Alvarez-Rodriguez/snappy-android)
as a dependency to add `snappy` compression support.

Q: Can I use this project for non-Android (e.g. normal Java, iOS) projects?  
A: No, this project produces an Android Archive (AAR) binary, which only works for android.

Q: Why do you produce an AAR instead of a Java Archive (JAR) binary?  
A: TL;DR We are shipping native libraries so we must do that.
The building process for Android PacKages (APK) and Android Bundles
requires that native libraries are present in AAR files in `/jni/<ABI>/libmylibrary.so`.
If we pass an AAR with stuff in `/jni/<ABI>/`, the build process copies the shared libraries
to `/jni/<ABI>/` of the output  APK/Bundle.
The Android system can then load these libraries at runtime using `System.loadLibrary("mylibrary")`.  
If we present a JAR file to the build process instead, with `/jni/<ABI>/libmylibrary.so` inside the JAR,
it places the entire JAR file in `/libs/myjar.jar`.
The Android system cannot find the libraries at `/jni/<ABI>`, and native libraries are not loaded.
So, we need an AAR.
For more info about the workings of AARs, see [here](https://developer.android.com/studio/projects/android-library.html#aar-contents).


## Thanks
Many thanks to the developers of [parquet-floor](https://github.com/strategicblue/parquet-floor):  
We use their project as a basis to have minimal dependencies.

Many thanks to the developers of Apache, specifically those who made [parquet-mr](https://github.com/apache/parquet-mr):  
We use (parts of) their project to read and write to the parquet format.
