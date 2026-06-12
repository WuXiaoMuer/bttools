# 蓝牙工具箱 (bttools)

Android 蓝牙串口 (SPP) 调试工具，基于 Kotlin + Jetpack Compose + Material 3 构建。

![主界面](res/interface.png)

## 功能

- **蓝牙设备扫描** — 搜索并发现附近的经典蓝牙设备（非 BLE）
- **已配对设备** — 显示已配对的蓝牙设备列表，一键连接
- **串口通信** — 通过 RFCOMM 协议进行数据收发，支持文本命令
- **日志查看** — 实时显示发送/接收消息，带时间戳和颜色标识
- **摇杆控制** — 8 方向模拟摇杆 + A/B/X/Y 按键，支持自定义指令和周期发送
- **主题定制** — 深色/浅色模式切换，主题色自定义

## 要求

- 最低 Android 7.0 (API 24)
- 目标 Android 15 (API 36)
- 蓝牙硬件（非必需，无蓝牙设备也可安装）

## 权限

| 权限 | 用途 |
|------|------|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Android 11 以下蓝牙连接与扫描 |
| `BLUETOOTH_SCAN` | Android 12+ 蓝牙扫描 |
| `BLUETOOTH_CONNECT` | Android 12+ 蓝牙连接 |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Android 11 以下蓝牙扫描必需 |

> Android 10+ 搜索蓝牙设备需要开启位置服务 (GPS)。

## 构建

```bash
./gradlew assembleDebug
```

APK 生成路径：`app/build/outputs/apk/debug/`

## 项目结构

```
app/                          # 主应用模块
├── src/main/java/com/bttools/app/
│   ├── MainActivity.kt        # 主 Activity，权限管理、设备搜索、指令发送
│   ├── BluetoothService.kt    # 蓝牙 SPP 通信服务 (Accept/Connect/Connected Thread)
│   ├── BluetoothDiscoveryReceiver.kt  # 蓝牙发现广播接收器
│   ├── SettingsManager.kt     # 设置持久化管理
│   ├── ui/                    # Compose UI 组件
│   │   ├── BluetoothAppNav.kt  # 底部导航
│   │   ├── StatusScreen.kt     # 状态页
│   │   ├── ScanScreen.kt       # 扫描与通信页
│   │   ├── DevicesScreen.kt    # 设备列表页
│   │   ├── JoystickScreen.kt   # 摇杆控制页
│   │   ├── LogsScreen.kt       # 日志页
│   │   ├── SettingsScreen.kt   # 设置页
│   │   ├── AboutScreen.kt      # 关于页
│   │   └── theme/              # Material 3 主题
│   └── res/                    # 资源文件
res/                          # 截图资源
```

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **蓝牙**: Android Bluetooth SDK (RFCOMM/SPP)
- **构建**: Gradle + Kotlin DSL + Version Catalog
