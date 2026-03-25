# 1. 使用官方的 Nginx 轻量级镜像（这里选用和你原来版本一致的 1.20.2）
FROM nginx:1.20.2-alpine

# 2. 覆盖 Nginx 的主配置文件（这步很关键，里面有你的反向代理配置）
COPY ./nginx-1.20.2/conf/nginx.conf /etc/nginx/nginx.conf

# 3. 把原有的静态文件（所有的前端代码）复制到容器内的 Nginx 默认目录
COPY ./nginx-1.20.2/html /usr/share/nginx/html

# 4. 暴露 80 端口
EXPOSE 80