# SkyWalking 8.9.1 + Java8 Demo（docker-compose）

目录：`/Users/RunwangGuo/Documents/Playground/skywalking8-java8-demo`

## 1) 启动（统一用 compose）

```bash
cd /Users/RunwangGuo/Documents/Playground/skywalking8-java8-demo
docker compose up -d --build
```

访问地址：`http://localhost:8088`

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
