# 授权范围与第三方鸣谢

项目自有程序、脚本和文档按根目录 MIT 许可证发布。以下数据、素材和依赖保留其原有许可；根目录许可证不替代它们。二进制 APK 中也保留了相应署名和许可。

| 内容 | 来源与作者 | 许可与修改 |
| --- | --- | --- |
| HYG v4.1 恒星数据库 | David Nash / Astronomy Nexus，[HYG Database](https://github.com/astronexus/HYG-Database) | CC BY-SA 4.0。选取 V≤11、排除太阳，坐标/星等/色指数经量化、排序、gzip 压缩，115,570 条；这些派生数据仍按 CC BY-SA 4.0 提供。见 `LICENSES/CC-BY-SA-4.0.txt`、`data/HYG-LICENSE.txt`。 |
| OpenNGC 星云目录 | Mattia Verga 与 OpenNGC contributors，[OpenNGC](https://github.com/mattiaverga/OpenNGC) | CC BY-SA 4.0。选取 12 处星云/遗迹坐标与角尺寸，派生子集继续使用同一许可。见 `data/OpenNGC-LICENSE.txt`。纹理由本项目程序生成。 |
| Astronomy Engine 2.1.19 | Don Cross，[Astronomy Engine](https://github.com/cosinekitty/astronomy) | MIT。上游代码未修改，JavaScript 浏览器/CommonJS 版本随源码附带；月球星历表由该库计算。许可见 `LICENSES/MIT-Astronomy-Engine.txt`，也保留在上游文件头。 |
| 月面贴图 | NASA SVS / Ernie Wright；LROC 与 LOLA 团队，[CGI Moon Kit](https://svs.gsfc.nasa.gov/4720/) | NASA SVS 将其内容作为公共领域材料提供，另有标注的除外，参见[使用说明](https://svs.gsfc.nasa.gov/help/#frequently-asked-questions)。本项目使用页面的 `lroc_color_2k.jpg`，缩为 256×128 灰度贴图，再压缩内嵌。保留来源与科学团队鸣谢，不表示 NASA 背书。 |

数据范围包括 `data/catalogue.b64`、`data/catalogue-meta.json`、`data/celestials.json` 中的目录字段及 Android 对应的 `stars.bin.gz`、`catalogue.json`、`nebulae.json`。生成的自包含预览也内嵌这些数据，分发时须同时保留本说明与数据许可证。

仓库中的手机效果图由本项目生成，属于界面/渲染示例；不代表恒星观测照片。它们含上述数据和素材，应保留本文件的归属信息。

Android SDK、JDK、Gradle、Python/Pillow、Node.js、GitHub Actions 等属于构建环境，不随源码包分发，遵循各自供应方许可。旧实验的 AT-HYG 原始下载不是当前 HYG 数据库的构建依赖，不包含在发布包中。
