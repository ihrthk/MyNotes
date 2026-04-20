# fastfetch 与 btop 终端工具




    
    
    
    
    
> 日期：2026-04-05
> 分类：tech
> 标签：cli, terminal, macos, system-monitor, neofetch

## 概述

fastfetch 是 neofetch 的高性能替代品，用于在终端展示系统信息。btop 是功能丰富的系统资源监控工具，类似 htop 但更现代。

## fastfetch

### 基本信息
- **版本**: 2.61.0 (aarch64)
- **安装路径**: `/opt/homebrew/bin/fastfetch`
- **安装方式**: `brew install fastfetch`

### 功能
- 终端启动时展示系统概览（OS、内核、Shell、显示器、主题等）
- neofetch 的 C 语言重写版，速度快得多
- 支持自定义 logo 和输出模块

### 常用配置
配置文件路径: `~/.config/fastfetch/config.jsonc`

### 使用
```bash
# 直接运行显示系统信息
fastfetch

# 查看版本
fastfetch --version
```

## btop

### 基本信息
- **版本**: 1.4.6
- **编译器**: clang++ 17.0.0
- **安装路径**: `/opt/homebrew/bin/btop`
- **安装方式**: `brew install btop`

### 功能
- 实时 CPU、内存、磁盘、网络、进程监控
- 支持 mouse 交互
- 比 htop 更美观的 UI
- 支持查看 GPU 信息

### 快捷键
| 快捷键 | 功能 |
|--------|------|
| `q` | 退出 |
| `/` | 搜索进程 |
| `m` | 切换视图模式 |
| `k` | 杀死进程 |
| `e` | 切换进程树视图 |
| `Tab` | 切换侧边栏 |

### 使用
```bash
# 启动 btop
btop

# 查看版本
btop --version
```

## 参考
- fastfetch GitHub: https://github.com/fastfetch-cli/fastfetch
- btop GitHub: https://github.com/aristocratos/btop



