#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # 无颜色

BACKUP_DIR="database_backups"

echo -e "${BLUE}================================"
echo -e "      数据库恢复脚本"
echo -e "================================${NC}"
echo

# 检查备份目录是否存在
if [ ! -d "$BACKUP_DIR" ]; then
    echo -e "${RED}错误: 备份目录 '$BACKUP_DIR' 不存在${NC}"
    exit 1
fi

# 显示可用的备份文件
echo -e "${YELLOW}可用的备份文件:${NC}"
echo
BACKUP_FILES=($(ls -t "$BACKUP_DIR"/*.sql 2>/dev/null))

if [ ${#BACKUP_FILES[@]} -eq 0 ]; then
    echo -e "${RED}错误: 没有找到备份文件${NC}"
    exit 1
fi

# 显示备份文件列表
for i in "${!BACKUP_FILES[@]}"; do
    FILE="${BACKUP_FILES[$i]}"
    SIZE=$(du -h "$FILE" | cut -f1)
    DATE=$(date -r "$FILE" "+%Y-%m-%d %H:%M:%S")
    echo "[$((i+1))] $(basename "$FILE") (${SIZE}, ${DATE})"
done

echo
echo -e "${YELLOW}请选择要恢复的备份文件 (输入序号):${NC}"
read -p "请输入选择 [1-${#BACKUP_FILES[@]}]: " CHOICE

# 验证选择
if ! [[ "$CHOICE" =~ ^[0-9]+$ ]] || [ "$CHOICE" -lt 1 ] || [ "$CHOICE" -gt ${#BACKUP_FILES[@]} ]; then
    echo -e "${RED}错误: 无效的选择${NC}"
    exit 1
fi

SELECTED_FILE="${BACKUP_FILES[$((CHOICE-1))]}"
echo -e "${GREEN}已选择: $(basename "$SELECTED_FILE")${NC}"
echo

# 确认恢复操作
echo -e "${RED}⚠️  警告: 恢复操作将清空当前数据库并替换为备份数据！${NC}"
read -p "确认继续? [y/N]: " -n 1 -r
echo

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}操作已取消${NC}"
    exit 0
fi

# 检查容器是否运行
echo -e "${YELLOW}正在检查数据库连接...${NC}"
if ! docker ps | grep -q agentx-postgres; then
    echo -e "${RED}错误: 找不到运行中的 agentx-postgres 容器${NC}"
    exit 1
fi

# 测试数据库连接
if ! docker exec agentx-postgres pg_isready -U postgres -d agentx > /dev/null 2>&1; then
    echo -e "${RED}错误: 无法连接到数据库${NC}"
    exit 1
fi

echo -e "${GREEN}数据库连接正常${NC}"
echo

# 执行恢复
echo -e "${YELLOW}开始恢复数据库...${NC}"
echo -e "恢复文件: $SELECTED_FILE"

docker exec -i agentx-postgres psql -U postgres -d agentx < "$SELECTED_FILE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 数据库恢复成功！${NC}"
    echo -e "已从备份文件恢复: $(basename "$SELECTED_FILE")"
else
    echo -e "${RED}❌ 数据库恢复失败！${NC}"
    exit 1
fi

echo
echo -e "${GREEN}🎉 恢复完成！${NC}" 