# 构建与测试

## Windows 原生 APK（已验证）

需要 Python 3.11+。在仓库根目录执行：

```powershell
python tools/bootstrap_android.py
python tools/bootstrap_android16.py
python tools/verify_android_catalog.py
python tools/build_android.py
python tools/build_android_qa.py
python tools/build_android16.py
python tools/verify_android16.py
python tools/build_android16_qa.py
```

bootstrap 从官方来源获取 JDK 21、SDK 37 / 36、Build Tools 37 / 36 和 platform-tools，并按供应方摘要校验。需要网络与相应磁盘空间。构建本身使用 aapt2、javac、D8、zipalign 和 apksigner，无需 Gradle 或重新生成天文数据。Android 17 包最低/目标/编译 API 为 37；Android 16 包三者均为 36。

APK、摘要和签名信息输出到 `dist/`。`.signing/` 自动生成私钥及本地构建密码，必须自行保管，不提交或上传。未附维护者私钥，其他人的构建不能覆盖维护者签名的安装。开发中更换签名时，需在测试设备卸载旧安装或使用独立 applicationId。

Android Studio 工程在 `android/`，默认声明 AGP 9.1.1、Gradle 9.3.1、Java 17、SDK 37；仓库不附 Gradle Wrapper。Android 16 独立包使用 `tools/build_android16.py`，不会改动源清单或 Android 17 包。普通发布以已验证的 Windows 流水线为准。

## 离线检查

```text
node --test tests/sky-math.test.cjs tests/celestial-math.test.cjs
python tools/verify_android_catalog.py
python tools/verify_android16.py
python tools/verify_open_source.py
```

Node.js 20+，Python 3.11+；上述检查无第三方包依赖。数学测试覆盖恒星时、地平坐标、正逆投影、星等筛选及独立日/月食日期的月相。星表检查逐字节比较 Android 与预览数据库。`verify_android16.py` 须在两个 APK 均构建后运行，检查 API 36、应用名、摘要与签名一致性。开源检查仅扫描发布文件清单，不读取私钥目录。

## Android 设备测试

先安装主 APK 和 QA APK。仅在可清理的测试设备/模拟器运行，夹具会更改并恢复测试偏好；不要在日常设备运行历史夹具。

```powershell
.toolchains/sdk/platform-tools/adb.exe install -r dist/starwallpaper-1.2.4-android17.apk
.toolchains/sdk/platform-tools/adb.exe install -r build/android/qa/starfield-qa.apk
.toolchains/sdk/platform-tools/adb.exe shell am instrument -w app.starfield.wallpaper.test/.CelestialInstrumentation
.toolchains/sdk/platform-tools/adb.exe shell am instrument -w app.starfield.wallpaper.test/.AnnotationInstrumentation
```

`AnnotationInstrumentation` 验证亮星名称与星座开关在精简版可用、自动切换标准版，以及星名、星座和闪烁强度带来实际 GLES 像素变化。`CelestialInstrumentation` 验证数据库、预览参数、GLES 帧、月球和星云像素、亮星面积、指针模式、地平线下月球隐藏。其他夹具记录初期版本的设置或系统集成场景；`BrightnessInstrumentation` 和 `UpgradeSeedInstrumentation` 是早期升级夹具，预设不适用于当前版本。`SmokeInstrumentation` 的旧大星表断言也不作为本版回归入口。

Android 16 安装包可使用 `python tools/bootstrap_emulator16.py` 准备官方 API 36 模拟器；系统镜像约 844 MB，测试环境须另有足够空间。安装 `dist/starwallpaper-1.2.4-android16.apk` 和 `build/android16/qa/starfield-qa.apk` 后，运行同一组 `AnnotationInstrumentation` 与 `CelestialInstrumentation`。Android 17 模拟器准备脚本仍为 `tools/bootstrap_emulator.py`。

## 预览

```text
python tools/build_preview.py
python tools/serve_preview.py
```

脚本只监听 `127.0.0.1`，使用支持 Canvas 与 DecompressionStream 的现代浏览器。Ctrl+C 结束服务。片段作为独立浏览器预览时不使用 Codex 的会话偏好保存；调整仍会立即改变画面。

## 重新生成天文数据（可选）

普通构建已经有全部离线资产。只有修改数据时才需要以下步骤：

| 操作 | 命令 / 输入 |
| --- | --- |
| 将预览星表复制到 Android | `python tools/prepare_android_catalog.py`；输入为随仓库附带的 `data/catalogue.b64` |
| 从原始 HYG CSV 重新量化 | `python tools/prepare_catalog.py`；需从 NOTICE 的 HYG 来源另取 v4.1 CSV，放在 `data/hygdata_v41.csv`。原始大 CSV 不纳入仓库。 |
| 重建星云与月面派生数据 | `python tools/prepare_celestials.py`，再 `python tools/prepare_android_celestials.py`；需要 Pillow，输入为已附 OpenNGC CSV 与 NASA 月面图。 |
| 重建 1900–2101 月球星历 | `node tools/prepare_moon.cjs`；使用已附 Astronomy Engine 2.1.19，无 npm 安装。 |
| 重建星座连线与名称 | `python tools/build_constellations.py`；输入为已附的 d3-celestial 原始 GeoJSON，输出 Android 与预览共享的 J2000 坐标。 |

修改目录时继续保留 CC BY-SA 许可、来源、修改说明，并更新两端资产和一致性测试。`tools/download_deep_catalog.py` 是旧 AT-HYG 实验脚本，当前构建不运行它。

## 打包开源源码

```text
python tools/verify_open_source.py
python tools/package_open_source.py
```

只按明确清单打包代码、文档、许可证和必要数据。`.git/`、`.signing/`、`.toolchains/`、设备导出、原始大 HYG/AT-HYG 和旧 APK 均排除。`dist/starwallpaper-open-source-1.2.4.zip` 可以直接解压后构建。
