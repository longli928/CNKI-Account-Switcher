# CNKI 账号切换器 v1

Kotlin + Jetpack Compose，面向 Android 10（API 29）及以上。界面采用 MIUI 风格的圆角卡片、渐变、半透明磨砂视觉和触摸反馈；没有依赖小米专有 MIUIX SDK，以保证普通设备可编译运行。

## 环境
- Android Studio、JDK 17、Android SDK 35
- Magisk / 可用的 `su`
- 知网包名：`com.cnki.android.cnkimobile`

## 固定目录约定
- 备份根目录：`/data/adb/cnki-accounts/`
- 每个账号：`/data/adb/cnki-accounts/<用户名>/cnki_mobile.xml`
- 可选备注文件：同目录 `display_name`
- 目标文件：`/data/data/com.cnki.android.cnkimobile/shared_prefs/cnki_mobile.xml`

## 已实现
- Root 授权检测、账号目录扫描、手动刷新
- SHA-256 比对活动 XML 与快照，识别当前账号
- 强制停止知网、`cp` 覆盖 XML、使用 monkey 启动知网
- 删除账号快照、修改显示备注

## LSPosed 模块接口约定
模块登录成功后创建以用户名命名的目录，并写入 `cnki_mobile.xml`。切换器读取目录名作为原始用户名；若存在 `display_name` 则用它显示，否则显示目录名。

## 注意
1. 首次运行需在 Magisk 弹窗中授予切换器 Root 权限。
2. 当前工程是源码，不含 APK；Android Studio 打开根目录编译。
3. 当前账号识别依赖 XML SHA-256 完全一致；若 APP 改写 XML，可能显示未匹配。
4. 第一版直接 `cp` 覆盖既有目标文件，不擅自修改权限/SELinux 标签；若目标文件不存在或系统拒绝写入会报错。
5. XML 是登录凭据，请勿分享或上传。
