# 开发环境手册（本机从零到跑通）

> 本机已完成的一次性安装见 §1；日常启停见 §2；新环境重建见文末。

## 1. 已就绪的一次性安装（2026-11-04，本机 E 盘）

| 组件 | 位置 | 说明 |
|---|---|---|
| Miniforge (conda) | `E:\miniforge3` | 用于免管理员安装 PostgreSQL |
| PostgreSQL 16.15 + pgvector 0.8.6 | `E:\PostgreSQL`（conda 环境） | 数据目录 `E:\PostgreSQL\data`，端口 5432，postgres/123456（仅本地） |
| 业务库 agenthub | — | 6 张表已建（`E:\jjchat\setup\agenthub_tables.sql`） |
| Java 21 / Maven 3.9.11 / Node 24 | 系统 PATH | — |
| API Keys | `E:\jjchat\新建 文本文档.txt` | 智谱 + DeepSeek，均已验证；**严禁入库**，经环境变量注入 |

pgvector 注意：conda 版二进制在 `E:\PostgreSQL\Library\bin`（不是 bin/）。

## 2. 日常启停

```bash
# 数据库（重启电脑后需要）
E:\PostgreSQL\start-pg.bat     # 启动；停止用 stop-pg.bat；日志 E:\PostgreSQL\pg.log

# 验证
"E:/PostgreSQL/Library/bin/pg_isready.exe" -h localhost -p 5432
```

## 3. 环境变量（后端启动前）

Git Bash：
```bash
export ZHIPU_API_KEY=<智谱key>      # chat + embedding 共用
export DEEPSEEK_API_KEY=<ds key>
# PG 密码默认 123456，如改过：export PG_PASSWORD=xxx
```
PowerShell：`$env:ZHIPU_API_KEY="..."`。IDEA 里配在 Run Configuration 的 Environment variables。
M1 会在仓库放 `.env.example` 模板。

## 4. 数据库初始化脚本（纳入仓库后由脚本执行）

- `db/schema.sql` —— agenthub 6 张表（与已建库一致，幂等 IF NOT EXISTS）
- `db/eshop.sql` —— eshop 演示库建表 + 种子数据（M2 编写）
- `db/init-test-db.sql` —— 创建测试库 `agenthub_test` / `eshop_test` 并套用 schema
- 一键初始化：`db/init-all.bat`（调用 psql 依次执行以上脚本）

## 5. 后端 / 前端启动

```bash
cd agentforge && mvn spring-boot:run     # 8080
cd ui && npm install && npm run dev      # 5173，CORS 已开后端直连
```

## 6. 新环境重建速查（换机器时）

1. Miniforge 装到 `E:\miniforge3`（JustMe 静默）→ `conda create -p E:/PostgreSQL -c conda-forge postgresql pgvector -y`
2. `initdb -D E:/PostgreSQL/data -U postgres -A password -E UTF8 --locale=C`（密码文件方式）
3. `pg_ctl start` → 执行 §4 脚本 → 设 §3 环境变量
4. Docker Desktop 安装包已备而未装：`E:\jjchat\setup\downloads\DockerDesktopInstaller.exe`（装到 E:\docker，需要 UAC + 可能重启；仅未来部署用）
