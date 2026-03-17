# SkyWalking 8.9.1 + Java8 Demo（docker-compose）

目录：`/Users/RunwangGuo/Documents/Playground/skywalking8-java8-demo`

## 1) 启动（统一用 compose）

```bash
cd /Users/RunwangGuo/Documents/Playground/skywalking8-java8-demo
docker compose up -d --build
```

访问地址：`http://localhost:8088`

默认会同时启动 Nacos（SkyWalking 告警动态配置中心）：
- 地址：`http://127.0.0.1:8848/nacos`
- 用户名：`nacos`
- 密码：`nacos`
- 启动时会自动把 `skywalking8/config/alarm-settings.yml` 发布到 Nacos（`dataId=alarm.default.alarm-settings`, `group=skywalking`）
- 已开启鉴权：`NACOS_AUTH_ENABLE=true`
- `NACOS_AUTH_TOKEN` 是 Nacos JWT 签名密钥（不是登录密码），登录接口返回的 `accessToken` 由它签发并校验

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

可选：查看 Nacos 中已发布的告警配置

```bash
TOKEN=$(curl --noproxy '*' -s -X POST "http://127.0.0.1:8848/nacos/v1/auth/login" \
  --data-urlencode "username=nacos" \
  --data-urlencode "password=nacos" \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

curl --noproxy '*' -s "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=alarm.default.alarm-settings&group=skywalking&accessToken=${TOKEN}"
```

## 4) 停止并清理

```bash
docker compose down -v --remove-orphans
```

## 5) 如何测试性能剖析功能

`性能剖析` 功能的标准测试步骤如下。

1. 在 UI 新建任务
- 服务：`demo-order`
- 端点：`POST:/api/orders/create`（注意带 `POST:`）
- 持续时间：10~20 分钟
- Min Duration：`100ms`（建议）

2. 在任务生效后，立即打慢请求流量（持续 2~5 分钟）

```bash
for i in $(seq 1 300); do
  curl --noproxy '*' -s -X POST "http://127.0.0.1:8081/api/orders/create?orderNo=PF-${i}&slowMs=1500&failRate=0" >/dev/null
  sleep 0.2
done
```

3. 回到 UI 的 `性能剖析` 页面，选择刚创建的任务并点击 `分析`
- 左下 `Sampled Traces` 应出现采样链路
- 中间 Span 列表应展示调用明细
- 右下 `Thread Stack` 可能为空；这是正常现象，只有采到线程栈 dump 才会显示
