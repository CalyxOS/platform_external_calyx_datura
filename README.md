# Datura

Datura is a firewall app that allows users to restrict internet access of both system and user apps, across
different types of network connections such as Wi-Fi, cellular, VPN, etc.

## Development

Datura is compatible with both AOSP and Gradle build systems and seamlessly integrates with CalyxOS.

To build in AOSP, add the following lines to an included Makefile:

```makefile
# Apps
PRODUCT_PACKAGES += \
    Datura \
```

To build in Android Studio, clone this repo to get started. Ensure that the testing device targets
a supported API level. The `debug` build type is additionally signed with AOSP signing keys (test keys) to
allow installation over the existing app in the system.

Various patches might be required across the OS to support all the features Datura offers, depending
upon the use case. Please check [our Gerrit instance](https://review.calyxos.org/) for a complete list of patches.

## Copyright and License

Datura is licensed and distributed under the [Apache 2.0 License](LICENSE/Apache-2.0.txt). See files for individual
copyright holder's information.
