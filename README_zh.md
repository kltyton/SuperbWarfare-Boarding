<div align="center">

<img src="docs/publishing/artwork/superb-warfare-boarding-icon-256.png" width="128" alt="卓越前线：登乘演出 图标" />

# 卓越前线：登乘演出 · Superb Warfare: Boarding

**基于 [合拍（AnimCue）](https://github.com/kltyton/AnimCue) 打造的卓越前线（Superb Warfare）载具实景上下车演出附属模组。**

[English](README.md) | [简体中文](README_zh.md)

</div>

---

## 简介

**卓越前线：登乘演出（Superb Warfare: Boarding）**（模组 ID：`superbwarfare_boarding`）为 **卓越前线（Superb Warfare）** 的可乘坐载具增加贴合原车模型的上下车演出动画，底层由 **合拍（AnimCue / `interactive_animation`）** 运行库驱动。

保持原本的操作习惯，直接右击载具即可触发：
- **智能近走远跑与自动避障**：距离较近时自然步行、距离较远时切换奔跑接近载具，并自动绕开机翼、车体与邻近载具；当入口路径完全受阻时自动平滑回退至卓越前线原生乘坐，不额外卡死操作。
- **贴合原车几何的上下车动作**：按原车模型的真实车门、金属脚踏板、斜钢梯、起落架轮胎、后舱门、炮塔舱盖、翼根与座舱盖位置设计完整路径与骨骼动画。
- **下车物理关门与关盖**：载具停稳下车时，角色踩踏台阶/门槛稳步落地后转身面对敞开的车门、后门或舱盖，伸手将其物理推合再恢复操控；行驶中、空中弹射或无安全落脚点时保持原生即时离座。
- **丝滑镜头与动画期双面渲染**：第一人称自动平滑拉出至载具外侧跟拍，结束后无跳变归位；播放上下车动画及镜头过渡期间自动关闭载具模型的背面剔除，开门与舱内视角不再透明穿帮。
- **双击快跳**：上车过程中再次右击载具、或下车过程中再次按下离座键，即可立即跳过演出直接落座或落地。
- **关键帧音效同步**：自动按脚下方块材质播放脚步声，并在服务端按关键帧同步触发开关门、舱盖启闭、金属踏步与落座音效。

完整保留卓越前线原有的载具上锁、工具、菜单、多座位逻辑、行驶离座与弹射体验。

---

## 特色载具演出一览

- **民用与卡车**：Sodayo 皮卡系列（`sodayo_pick_up*`）、重型卡车（`truck`，踩踏实车金属脚踏板与宽角车门）、FH-77BW 自行火炮（`fh_77bw`，逐级攀爬 4 级侧钢梯并拉合前铰链驾驶室门）。
- **步战车与装甲运兵车**：布拉德利（`bradley`，后装甲内嵌小人员门开合与转身推门）、LAV-25 / LAV-AD（`lav_25`、`lav_ad`，双开后门与双手推关）、BMP-2（`bmp_2`）、LAV-150（`lav_150`）。
- **主战坦克与自行火炮**：T-90A（`t_90a`）、光棱坦克（`prism_tank`）、M1A2（`m_1a_2`）、ZTZ-99A（`ztz_99a`）、PLZ-05（`plz_05`）、YX-100（`yx_100`），包含贴合履带/侧裙登车、炮塔/车体舱盖启闭与高台缓冲落地。
- **直升机与固定翼战机**：米-28（`mi_28`，踩踏主起落架轮胎与短翼台阶拉合装甲侧门）、A-10A（`a_10a`）、歼-16（`j_16`）、斯图卡 Ju 87（`ju_87`）、AH-6（`ah_6`）、AC-130H（`ac_130h`）。
- **船艇、飞艇与固定武器**：快乐恶魂（`happiest_ghast`）、基洛夫空艇（`kirov`）、快艇、轮椅以及各类炮台/防空阵地。

---

## 安装要求

| 平台 | Minecraft | 加载器 | 必需前置 | 可选肢体/躯干弯曲增强 |
| --- | --- | --- | --- | --- |
| **NeoForge** | `1.21.1` | NeoForge `21.1.248+` | [合拍 AnimCue](https://github.com/kltyton/AnimCue)（`interactive_animation >= 0.1.0`）、Superb Warfare（`>= 0.8.9.2`）、GeckoLib、Kotlin for Forge | Player Animation Library（PAL）+ PAL More Rotation |
| **Forge** | `1.20.1` | Forge `47.4.22+` | [合拍 AnimCue](https://github.com/kltyton/AnimCue)（`interactive_animation >= 0.1.0`）、Superb Warfare（`>= 0.8.9.2`）、GeckoLib、Kotlin for Forge、MixinExtras（`>= 0.3.6`，随 Superb Warfare 提供） | playerAnimator + bendy-lib + PAL More Rotation（Forge 版） |

所有前置均并排安装，模组本体不内嵌第三方依赖包。

---

## 从源码构建

进入对应目标子目录运行 Gradle Wrapper：

```bash
# NeoForge 1.21.1
cd targets/neoforge-1.21.1
../../gradlew build verifyDistribution

# Forge 1.20.1
cd targets/forge-1.20.1
../../gradlew build verifyDistribution
```

可选本地构建参数：
- `-PinteractiveAnimationJar=<路径>`：指定对应加载器与版本的本地 AnimCue JAR。
- `-PsuperbWarfareJar=<路径>`：指定本地 Superb Warfare 发行 JAR。
- `-PpalMoreJar=<路径>`：编译 Forge 1.20.1 目标时指定本地 PAL More Rotation Forge JAR。

---

## 开源协议

本项目基于 **GNU Lesser General Public License v3.0 or later**（[LGPL-3.0-or-later](LICENSE)）发布。载具模型与贴图资源均由用户已安装的 Superb Warfare 本体提供。
