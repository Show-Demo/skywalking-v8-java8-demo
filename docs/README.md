# SkyWalking 8.9.1 + Java8 Demo（docker-compose）

目录：`/Users/RunwangGuo/Documents/Playground/skywalking8-java8-demo`

## 1) 启动（统一用 compose）

```bash
cd /Users/RunwangGuo/Documents/Playground/skywalking8-java8-demo
docker compose up -d --build
```

访问地址：`http://localhost:8088`

默认会同时启动 MySQL：
- 地址：`127.0.0.1:3306`
- 库名：`skywalking_demo`
- 用户：`skywalking`
- 密码：`skywalking123`

## 2) （可选）开启企微告警转发

先设置环境变量再启动：

```bash
export QYWX_WEBHOOK_URL='https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=你的key'
docker compose up -d --build
```

## 3) 打测试流量并验证

```bash
for i in $(seq 1 80); do
  curl --noproxy '*' --max-time 3 -s -X POST "http://127.0.0.1:8081/api/orders/create?orderNo=NO-${i}&slowMs=300&failRate=0.2" >/dev/null || true
done

for i in $(seq 1 30); do
  curl --noproxy '*' --max-time 5 -s -X POST "http://127.0.0.1:8081/api/orders/create?orderNo=ALARM-${i}&slowMs=1200&failRate=0.8" >/dev/null || true
done

docker compose logs --no-color demo-handler | grep -E 'Received SkyWalking alarm|Forwarded alarm to WeCom|Failed to forward alarm' | tail -n 20

# 可选：确认业务数据已写入 MySQL
docker exec -it skywalking8-java8-demo-mysql-1 mysql -uskywalking -pskywalking123 -D skywalking_demo -e "SELECT COUNT(*) AS order_count FROM orders; SELECT COUNT(*) AS action_count FROM lab_actions;"
```

成功标志：
- UI 中出现 `demo-handler`、`demo-order`
- 拓扑出现 `demo-order -> demo-handler`
- 日志输出 `Received SkyWalking alarm`
- 配置了企微时输出 `Forwarded alarm to WeCom, status=200`

## 4) 停止并清理

```bash
docker compose down -v --remove-orphans
```

## 5) 性能剖析无数据排查

这是正常现象，`性能剖析` 必须满足 3 个条件才会出数据：

- Task 任务时间窗口内有请求
- 请求命中你配置的精确端点名（建议用 `POST:/api/orders/create`）
- 请求要“足够慢”（否则不会产出 thread dump）

如果页面出现 `Sampled Traces = No Data`，通常是任务建好了但没有命中以上条件。

直接这样测（可复现）：

1. 在 UI 新建任务
- 服务：`demo-order`
- 端点：`POST:/api/orders/create`（注意带 `POST:`）
- 持续时间：10~20 分钟
- Min Duration 设小一点（如 `100ms`）

2. 立刻打慢请求流量（持续 2~5 分钟）

```bash
for i in $(seq 1 300); do
  curl --noproxy '*' -s -X POST "http://127.0.0.1:8081/api/orders/create?orderNo=PF-${i}&slowMs=1500&failRate=0" >/dev/null
  sleep 0.2
done
```
