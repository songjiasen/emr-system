# Windows 一键启动说明

这个目录提供 Windows 运行脚本，适合别人从 GitHub 拉取项目后快速检测环境并启动整套系统。

## 前置环境

请先安装并加入 `PATH`：

- JDK 17 或更高版本
- Maven
- Node.js 18 或更高版本
- MySQL 5.7 或兼容版本，并启动 MySQL 服务
- MySQL Client，也就是命令行里能执行 `mysql`

## 一键启动

在项目根目录打开 PowerShell 或 CMD，执行：

```powershell
.\scripts\start-windows.bat
```

脚本会自动完成：

- 检查 Java、Maven、Node、npm、mysql 是否可用。
- 检查 `8080`、`8101-8109`、`5173`、`5174` 是否被占用。
- 检查并连接本机 MySQL。
- 执行 `deploy/mysql/init/*.sql` 初始化数据库。
- 自动安装两个前端项目依赖。
- 启动 10 个后端微服务和 2 个前端项目。
- 等待健康检查通过后打印访问地址。

默认 MySQL 参数：

- 主机：`127.0.0.1`
- 端口：`3306`
- 用户：`root`
- 密码：`root`

如果本机 MySQL 密码不是 `root`，可以这样执行：

```powershell
.\scripts\start-windows.bat -MySqlUser root -MySqlPassword 你的密码
```

如果数据库已经初始化过，可以跳过 SQL 初始化：

```powershell
.\scripts\start-windows.bat -SkipDbInit
```

## 访问地址

- 患者端：`http://localhost:5173`
- 后台端：`http://localhost:5174`
- 网关：`http://localhost:8080`

## 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 患者 | `patient_demo` | `123456` |
| 管理员 | `admin` | `admin123` |
| 医生 | `doctor` | `123456` |
| 护士 | `nurse` | `123456` |
| 主任 | `director` | `123456` |

## 停止服务

脚本启动的进程 PID 会写入：

```text
tmp/windows-logs/*.pid
```

需要停止所有服务时，在项目根目录执行：

```powershell
Get-Content .\tmp\windows-logs\*.pid | ForEach-Object { Stop-Process -Id $_ -Force }
```

## 常见问题

- 如果提示端口被占用，先关闭占用 `8080`、`8101-8109`、`5173`、`5174` 的程序。
- 如果提示找不到 `mysql`，说明只装了 MySQL 服务但没有把 MySQL Client 加到 `PATH`。
- 如果 Maven 第一次启动很慢，是在下载依赖，等它完成即可。
- 如果 PowerShell 提示脚本执行策略受限，直接运行 `start-windows.bat`，它已经带了 `ExecutionPolicy Bypass`。
