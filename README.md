# what's different from the original sky-take-out(proposal)?

## 1.使用go独立做websocket服务器 
+ 骑手位置实施上上报？



## 2.微服务：分布式订单系统

Spring Boot 微服务化（服务拆分）

Kafka / RabbitMQ 异步下单流程

分布式事务思路（最终一致性、重试、补偿）

Redis 缓存 + 限流

API 网关（Nginx 或 Spring Cloud Gateway）

## 3.注重devops生产环境自动构建部署上线和监控告警
自动化测试（Java JUnit、Go test）

Docker 多阶段构建

镜像推送到 registry（Docker Hub / GHCR）

Kubernetes 部署（用 minikube）

自动回滚（可选）

### Kubernetes 微服务部署
Deployment/Service/Ingress 基础

ConfigMap/Secret

Pod 日志收集（简单版本：ELK 或 Loki）

自动水平扩容（HPA）