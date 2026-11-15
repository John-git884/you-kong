# 部署手册

从一台空服务器到能公网访问，按顺序做完即可。两种方式选一种，Docker 更快，裸机更好排查。

## 0. 准备

- 服务器：1 核 2G 起（2 核 2G 更稳），镜像选 **Ubuntu 22.04**
- 安全组放行：**8080**（应用）、22（SSH）。不要开 3306
- **不需要域名，也不需要备案**——直接用 `http://IP:8080` 访问。用域名走 80/443 才需要 ICP 备案，个人备案要 7–20 天

## 0.1 先加 1G swap（2G 内存的机器必做）

MySQL 加 JVM 一起跑，2G 很容易被 OOM Killer 杀掉。加 swap 能兜住：

```bash
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h   # 确认 Swap 那行不是 0
```

## 方案 A：Docker

### A1. 装 Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
docker --version
```

国内机器如果拉镜像慢，先配镜像加速：`/etc/docker/daemon.json` 加 `{"registry-mirrors": ["https://docker.mirrors.ustc.edu.cn"]}`，然后 `sudo systemctl restart docker`。

### A2. 拉代码

```bash
git clone https://github.com/John-git884/you-kong.git
cd you-kong
```

### A3. 改密码（两处，必须改）

编辑 `docker-compose.yml`：

```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: 你的数据库密码     # 改这里
app:
  environment:
    DB_PASSWORD: 你的数据库密码             # 和上面保持一致
```

### A4. 启动

```bash
export JWT_SECRET=$(openssl rand -base64 48)
export ADMIN_PASSWORD=你的后台密码
docker compose up -d --build
```

首次构建要 5–10 分钟（拉 Node、Maven、JRE 镜像 + 编译）。看进度：

```bash
docker compose logs -f app
```

看到 `Started YouKongApplication` 就好了。

### A5. 验证

```bash
curl http://localhost:8080/api/shops/1
# 期望：{"code":0,...,"name":"拾光美甲 · 示例店"}

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"owner","password":"你设的后台密码"}'
# 期望：{"code":0,"data":{"token":"eyJ..."}}
```

浏览器打开 `http://服务器IP:8080`，首页能出服务列表即成功。

## 方案 B：裸机 jar

适合想看清每一步、或者机器内存只有 1G 的情况。

### B1. 装环境

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless mysql-server git
java -version   # 应为 17.x
```

### B2. 建库

```bash
sudo mysql
```

```sql
CREATE USER 'youkong'@'localhost' IDENTIFIED BY '你的密码';
SOURCE /path/to/you-kong/sql/init-mysql.sql;
GRANT ALL ON youkong.* TO 'youkong'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### B3. 限制 MySQL 内存

```bash
sudo tee /etc/mysql/mysql.conf.d/youkong.cnf > /dev/null <<'EOF'
[mysqld]
innodb_buffer_pool_size = 128M
EOF
sudo systemctl restart mysql
```

### B4. 编译并上传

本地打包，只传 jar，服务器上不用装 Maven：

```bash
# 本机
cd web && npm ci && npm run build && cd ..
./mvnw -DskipTests package
scp target/youkong-0.1.0.jar user@服务器IP:/opt/youkong/
```

### B5. 配成系统服务

```bash
sudo tee /etc/systemd/system/youkong.service > /dev/null <<'EOF'
[Unit]
Description=YouKong booking service
After=network.target mysql.service

[Service]
User=www-data
WorkingDirectory=/opt/youkong
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=DB_HOST=127.0.0.1
Environment=DB_USER=youkong
Environment=DB_PASSWORD=你的密码
Environment=JWT_SECRET=用openssl生成的串
Environment=ADMIN_PASSWORD=你的后台密码
ExecStart=/usr/bin/java -Xmx512m -jar /opt/youkong/youkong-0.1.0.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now youkong
sudo systemctl status youkong
```

看日志：`sudo journalctl -u youkong -f`

## 上线后立刻做的三件事

| 事项 | 为什么 |
| --- | --- |
| 换掉 `JWT_SECRET` | 不换等于签名密钥公开，任何人都能伪造 token |
| 换掉 `ADMIN_PASSWORD` | 默认的 `youkong123` 写在 README 里，全世界都知道 |
| 换掉数据库密码 | 同上 |

## 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| `docker compose up` 后 app 立刻退出 | `docker compose logs app` 看报错，多半是数据库没起来就连接了；等 mysql healthcheck 通过会自动重试 |
| MySQL 容器一直 unhealthy | 首次初始化要 30–60 秒，`docker inspect --format='{{.State.Health.Status}}'` 观察 |
| 浏览器访问超时 | 安全组没放行 8080；或 `ufw` 挡了：`sudo ufw allow 8080` |
| 进程莫名消失 | 被 OOM Killer 杀了：`dmesg | grep -i kill` 确认，加 swap 或降 `-Xmx` |
| 前端页面 404 但接口正常 | 构建产物没进 jar：确认 `src/main/resources/static/index.html` 存在后再打包 |

## 回滚

```bash
# Docker
git checkout <上一个提交>
docker compose up -d --build

# 裸机
sudo systemctl stop youkong
scp 上一个版本的 jar 上去
sudo systemctl start youkong
```

## 部署完成后回来更新 README

把在线地址加到 README 顶部，徽章旁边：

```markdown
[在线演示](http://你的IP:8080) · [后台](http://你的IP:8080/admin)
```

这是整个项目对面试官最有说服力的一行字。
