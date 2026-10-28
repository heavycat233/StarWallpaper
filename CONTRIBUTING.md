# 参与贡献

欢迎通过 GitHub Issues 报告问题，或从分支提交 Pull Request。提交前先阅读 README、NOTICE 和 `docs/BUILDING.md`。

## 开发约定

- 自有新增代码使用 MIT；引入第三方代码或数据时保留来源、版本、许可及修改说明。
- Android 端与预览端保持同一份 HYG 数据，修改星等、投影、光核、光晕时应同时对照两端实现。
- 保留 Java 17 / Android API 37 和离线运行能力。不添加广告、分析上传或互联网权限。
- 精简模式仅显示时间与日期。定位由用户主动开启，拒绝授权时应继续支持手动位置。
- 不提交 `.signing/`、`.toolchains/`、SDK、原始大星表、设备导出、定位记录或生成的 APK。

## 提交前验证

```text
node --test tests/sky-math.test.cjs tests/celestial-math.test.cjs
python tools/verify_android_catalog.py
python tools/verify_open_source.py
```

涉及 Android 行为或着色器时，按构建文档编译，再在 API 37+ 设备或模拟器验证。PR 说明问题、最终行为、运行过的检查，以及新功能在数字/指针、精简/标准模式下的截图。设备差异请写明型号和系统版本。

贡献者保留各自贡献的著作权；提交贡献表示同意按本项目相应许可分发该贡献。目录数据的派生修改仍遵守 CC BY-SA 4.0。
