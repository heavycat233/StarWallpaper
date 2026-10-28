# Android 原生工程

当前版本 **1.2.1**；Java 17 / OpenGL ES 3.0 / Android API 37。Android Studio 打开本目录；仓库没有 Gradle Wrapper。已验证的 Windows 命令行构建从仓库根目录运行：

```text
python tools/bootstrap_android.py
python tools/build_android.py
```

完整功能与数据许可见根目录 [README](../README.md)、[NOTICE](../NOTICE.md)，测试与源码包构建见 [BUILDING](../docs/BUILDING.md)。APK 使用与预览逐字节相同的 HYG v4.1 数据，115,570 颗恒星。原始天文数据不必重新下载。

`.signing/` 下的密钥只属于本机构建者；不会进入 Git 或发布包。CI 测试 APK 使用临时签名，无法覆盖维护者正式签名的安装。升级既有安装必须使用相同 applicationId 和签名。
