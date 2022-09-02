# parquet-android
[![<Sebastiaan-Alvarez-Rodriguez>](https://circleci.com/gh/Sebastiaan-Alvarez-Rodriguez/parquet-android.svg?style=svg)](https://app.circleci.com/pipelines/github/Sebastiaan-Alvarez-Rodriguez/parquet-android)
[![](https://jitpack.io/v/Sebastiaan-Alvarez-Rodriguez/parquet-android.svg)](https://jitpack.io/#Sebastiaan-Alvarez-Rodriguez/parquet-android)

This project provides means to read from/write to parquet files.
It allows to read/write uncompressed and snappy-compressed parquet.
It uses minimal size.

## Why
We created this project because we wanted to export/import data between a database and a file efficiently in Android.
On the internet, we found many sources stating one should write to CSV, or JSON, or even XML on Android,
because that is what is supported.
These storage schemas feature human-readability and simple manual editing of data.
We care not for these traits.
Instead, we focus on low latency, high throughput, efficient compression. 

Parquet is a binary columnar storage format that supports nested data.
For more information about Parquet, see [here](https://github.com/apache/parquet-format).

## Performance
After implementing this framework, we executed performance experiments.
One of the results is visualised below.

![Read+Write performance on a simple dataset](/benchmark-results/plots/Speedup_factor_of_uncompressed_pq_vs_csv.png)

Here we proof that our parquet (pq) implementation is faster than the csv implementation.
We plotted the 99%-confidence interval for the speedup factor of our implementation versus csv.

In layman's terms, the plot shows '*how many times pq is faster than csv*' for a number of group sizes.
E.g. by looking at this picture, at x=10,000, we can state that:
 - 'we are 99% confident that pq is between 12 and 12.5 times faster than csv for 10,000 rows'.
 - 'in our measurements, pq was on average 12.25 times faster than csv for 10,0000 rows'.

Overall, we conclude that:
 - For the measurements, pq is between 10-16 times faster than csv, with certainty level $\alpha=99$%.
 - pq gets relatively faster than csv when increasing the dataset size. 
 The predicted speedup factor of pq versus csv is given by formula $0.00000001x^2+0.0001x+10.367$, where $x$ is the amount of rows.
 - pq is faster in aspect of both reading and writing.

For more plots and more statistics, see [stats.md](/stats.md).
If you believe we made a statistical computation error, please create a new issue.

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
