# Bedrock Finder

一个简单的Minecraft客户端Mod，用于查找并导出指定数量的基岩坐标到文件中。

## 功能

- 通过客户端命令 `/findbedrock <数量>` 扫描周围区域的基岩
- 自动保存找到的基岩坐标到 `bedrock_coordinates.txt` 文件中
- 支持多线程搜索，避免游戏卡顿
- 自动去重，避免重复坐标

## 安装

1. 确保已安装 [Fabric Loader](https://fabricmc.net/use/) 和 [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. 将mod文件放入Minecraft的`mods`文件夹
3. 启动游戏

## 使用说明

1. 在游戏中打开聊天窗口
2. 输入命令 `/findbedrock <数量>` (例如 `/findbedrock 50`)
3. Mod会在后台扫描周围区域的基岩
4. 扫描完成后，坐标会保存到游戏目录下的`bedrock_coordinates.txt`文件中
5. 游戏内会收到完成通知

## 技术细节

- 扫描Y坐标：4 (主世界底部) 和 123 (主世界顶部)
- 以玩家为中心向外扩展搜索
- 自动检测无更多基岩的区域并停止搜索

## 兼容性

- Minecraft版本: 1.21.7
- 需要Java 21或更高版本
- 需要Fabric Loader 0.16.14或更高版本

## 许可证

本项目采用GPLv3许可证开源。
