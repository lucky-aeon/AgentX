# 多阶段构建：第一阶段 - 构建应用
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /build

# 复制package文件
COPY package*.json ./

# 安装依赖（使用 legacy-peer-deps 解决依赖冲突）
RUN npm install --legacy-peer-deps

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 多阶段构建：第二阶段 - 运行时镜像
FROM node:18-alpine

# 设置工作目录
WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装生产依赖
RUN npm install --production --legacy-peer-deps

# 从构建阶段复制构建结果
COPY --from=builder /build/.next ./.next
COPY --from=builder /build/public ./public
COPY --from=builder /build/next.config.mjs ./
COPY --from=builder /build/components.json ./

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:3000 || exit 1

# 启动应用
CMD ["npm", "start"] 