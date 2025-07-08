#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

# 获取当前时间戳
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="database_backups"
BACKUP_FILE="agentx_backup_${TIMESTAMP}.sql"

echo -e "${BLUE}================================"
echo -e "      数据库备份脚本"
echo -e "================================${NC}"
echo

# 创建备份目录
mkdir -p $BACKUP_DIR

echo -e "${YELLOW}正在检查数据库连接...${NC}"

# 检查容器是否运行
if ! docker ps | grep -q agentx-postgres; then
    echo -e "${RED}错误: 找不到运行中的 agentx-postgres 容器${NC}"
    echo -e "${YELLOW}请确保数据库容器正在运行${NC}"
    exit 1
fi

# 测试数据库连接
if ! docker exec agentx-postgres pg_isready -U postgres -d agentx > /dev/null 2>&1; then
    echo -e "${RED}错误: 无法连接到数据库${NC}"
    exit 1
fi

echo -e "${GREEN}数据库连接正常${NC}"
echo

echo -e "${YELLOW}开始备份数据库...${NC}"
echo -e "备份文件: ${BACKUP_DIR}/${BACKUP_FILE}"

# 执行备份
docker exec agentx-postgres pg_dump -U postgres -d agentx --clean --if-exists > "${BACKUP_DIR}/${BACKUP_FILE}"

# 检查备份是否成功
if [ $? -eq 0 ] && [ -s "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
    BACKUP_SIZE=$(du -h "${BACKUP_DIR}/${BACKUP_FILE}" | cut -f1)
    echo -e "${GREEN}✅ 数据库备份成功！${NC}"
    echo -e "备份文件大小: ${BACKUP_SIZE}"
    echo -e "备份路径: $(pwd)/${BACKUP_DIR}/${BACKUP_FILE}"
    echo
    
    # 显示备份文件信息
    echo -e "${BLUE}备份文件信息:${NC}"
    ls -lh "${BACKUP_DIR}/${BACKUP_FILE}"
    echo
    
    # 显示恢复命令
    echo -e "${YELLOW}恢复命令:${NC}"
    echo "如需恢复此备份，请使用以下命令："
    echo "docker exec -i agentx-postgres psql -U postgres -d agentx < ${BACKUP_DIR}/${BACKUP_FILE}"
    echo
    
    # 显示最近的备份文件
    echo -e "${BLUE}最近的备份文件:${NC}"
    ls -lt "${BACKUP_DIR}"/ | head -6
    
else
    echo -e "${RED}❌ 数据库备份失败！${NC}"
    if [ -f "${BACKUP_DIR}/${BACKUP_FILE}" ]; then
        rm "${BACKUP_DIR}/${BACKUP_FILE}"
    fi
    exit 1
fi

echo
echo -e "${GREEN}🎉 备份完成！现在可以安全地进行测试了${NC}" 