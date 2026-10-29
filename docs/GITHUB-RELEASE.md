# GitHub 发布说明

公开仓库：[`heavycat233/StarWallpaper`](https://github.com/heavycat233/StarWallpaper)。建议描述为“Offline real-sky live wallpaper for Android 17+, with HYG stars, nebulae and lunar phases”。

首次上传前运行 `python tools/verify_open_source.py` 并检查 `git diff --cached --stat`。只提交发布清单内的文件；私钥、构建缓存和历史大数据保留在本机忽略目录。

本地已建立仓库时，可在自己的 GitHub 账号创建不带 README/许可证的空仓库，然后使用 Git 推送 `main`。如果仓库预先存在内容，先读取远端，避免强制覆盖。也可以使用已登录的 GitHub CLI：

```text
git remote add origin https://github.com/heavycat233/StarWallpaper.git
git push -u origin main
```

只在 origin 未配置且确认目标是自己的账号时使用。不要把访问令牌写进文件或远端 URL。

## Releases

维护者应在自己的可信本机，用保留的发布密钥构建 APK。确认许可证、检查数据一致性、安装测试与签名后，再为对应提交打版本标签并创建 Release，附 APK、SHA256SUMS 和更新说明。

GitHub CI 会在 Windows 生成临时签名的测试 APK，仅供验证和下载，不自动发布正式 Release、不使用维护者私钥、不需要写入仓库的 token。CI APK 每次签名可能不同，不能当作同签名更新渠道。不要把 CI artifact 标为维护者正式签名版本。

公开仓库后，维护者可在 GitHub Security 设置中开启私密漏洞报告。GitHub Actions 的配置已提供，首轮远端执行结果需要在 Actions 页面核实。
