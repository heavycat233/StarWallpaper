# 授权范围与第三方鸣谢

项目自有程序、脚本和文档按根目录 MIT 许可证发布。以下数据、素材和依赖保留其原有许可；根目录许可证不替代它们。二进制 APK 中也保留了相应署名和许可。

| 内容 | 来源与作者 | 许可与修改 |
| --- | --- | --- |
| HYG v4.1 恒星数据库 | David Nash / Astronomy Nexus，[HYG Database](https://github.com/astronexus/HYG-Database) | CC BY-SA 4.0。选取 V≤11、排除太阳，坐标/星等/色指数经量化、排序、gzip 压缩，115,570 条；这些派生数据仍按 CC BY-SA 4.0 提供。见 `LICENSES/CC-BY-SA-4.0.txt`、`data/HYG-LICENSE.txt`。 |
| AT-HYG v3.2 深星表 | David Nash / Astronomy Nexus 与贡献者，[AT-HYG Database](https://github.com/astronexus/ATHYG-Database) | CC BY-SA 4.0。从两份 v3.2 CSV 中筛选无 HYG 交叉编号、近似 Johnson V≤11 的恒星；对 Tycho VT 与 BT−VT 按 ESA 转换公式换算，随后量化、按星等排序、分字节压缩，得 822,917 条。派生文件 `preview/deep-catalog.js`、`data/athyg-preview-meta.json`、Android 的 `stars-deep.bin.gz` 与 `deep-catalogue.json` 仍按 CC BY-SA 4.0 提供。见 `LICENSES/CC-BY-SA-4.0.txt`、`android/app/src/main/assets/ATHYG-LICENSE.txt`。原始大 CSV 不纳入仓库。 |
| OpenNGC 星云与河外星系目录 | Mattia Verga 与 OpenNGC contributors，[OpenNGC](https://github.com/mattiaverga/OpenNGC) | CC BY-SA 4.0。选取 24 处星云/遗迹，以及 M31、M33 的坐标与角尺寸；派生子集继续使用同一许可。见 `data/OpenNGC-LICENSE.txt`。 |
| 全天 Hα 星云图 | Douglas P. Finkbeiner，[Full-Sky H-alpha Composite v1.1](https://faun.rc.fas.harvard.edu/dfink/skymaps/halpha/data/v1_1/index.html)，通过 [NASA SkyView](https://skyview.gsfc.nasa.gov/current/docs/batchpage.html) 裁图 | 21 处星云的 Hα 强度裁图经分位数归一化、艺术着色、水平坐标翻转与边缘透明渐隐，存于 `data/halpha-nebula-sprites.json`，并内嵌于预览及 APK。源 FITS 的查询地址和 SHA-256 附在该数据文件；源 FITS 不随仓库分发。非自然色，亮度不可用于科学测光。其余 3 处星云纹理由程序生成。 |
| 流星雨年度参考资料 | [International Meteor Organization，2026 Meteor Shower Calendar](https://www.imo.net/files/meteor-shower/cal2026.pdf) | Table 5 的活动期、太阳黄经峰值、辐射点、ZHR 和亮度指数整理为 `data/meteor-showers.json`，来源与视觉加速限制均写入数据文件；按 IMO 原文条件及许可引用。 |
| 星座连线和名称 | Olaf Frohn，[d3-celestial](https://github.com/ofrohn/d3-celestial) | BSD 3-Clause。以 J2000 GeoJSON 的 88 个星座、89 组连线转换为供 Android 和预览共用的紧凑坐标数据，保留原始文件及 `LICENSES/BSD-3-Clause-d3-celestial.txt`。连线是约定的示意图形，不是 IAU 星座边界。 |
| Astronomy Engine 2.1.19 | Don Cross，[Astronomy Engine](https://github.com/cosinekitty/astronomy) | MIT。上游代码未修改，JavaScript 浏览器/CommonJS 版本随源码附带；月球星历表由该库计算。许可见 `LICENSES/MIT-Astronomy-Engine.txt`，也保留在上游文件头。 |
| 月面贴图 | NASA SVS / Ernie Wright；LROC 与 LOLA 团队，[CGI Moon Kit](https://svs.gsfc.nasa.gov/4720/) | NASA SVS 将其内容作为公共领域材料提供，另有标注的除外，参见[使用说明](https://svs.gsfc.nasa.gov/help/#frequently-asked-questions)。本项目使用页面的 `lroc_color_2k.jpg`，缩为 256×128 灰度贴图，再压缩内嵌。保留来源与科学团队鸣谢，不表示 NASA 背书。 |

数据范围包括 `data/catalogue.b64`、`data/catalogue-meta.json`、`data/athyg-preview-meta.json`、`preview/deep-catalog.js`、`data/celestials.json`、`data/galaxies.json`、`data/meteor-showers.json` 中的目录字段及 Android 对应资产。生成的自包含预览也内嵌部分数据，分发时须同时保留本说明与适用的数据许可证及来源说明。

仓库中的手机效果图由本项目生成，属于界面/渲染示例；不代表恒星观测照片。它们含上述数据和素材，应保留本文件的归属信息。

Android SDK、JDK、Gradle、Python/Pillow、Node.js、GitHub Actions 等属于构建环境，不随源码包分发，遵循各自供应方许可。AT-HYG 原始下载仅用于重建深星表派生数据；普通 Android 构建直接使用已附资产，不包含原始大 CSV。
