#!/bin/bash

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 获取项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_DIR="$PROJECT_ROOT/docs/sql"

echo -e "${YELLOW}AgentX SQL 文件检测测试${NC}"
echo "==============================="
echo "项目根目录: $PROJECT_ROOT"
echo "SQL目录: $SQL_DIR"
echo

# 测试 SQL 目录是否存在
if [ ! -d "$SQL_DIR" ]; then
    echo -e "${RED}❌ SQL 目录不存在: $SQL_DIR${NC}"
    exit 1
else
    echo -e "${GREEN}✅ SQL 目录存在: $SQL_DIR${NC}"
fi

# 测试 SQL 文件是否存在
SQL_FILES=$(find "$SQL_DIR" -name "*.sql" -type f)
if [ -z "$SQL_FILES" ]; then
    echo -e "${RED}❌ 没有找到 .sql 文件${NC}"
    exit 1
else
    echo -e "${GREEN}✅ 找到以下 SQL 文件:${NC}"
    echo "$SQL_FILES" | while read -r file; do
        filename=$(basename "$file")
        echo "   - $filename"
    done
fi

echo
echo -e "${GREEN}✅ 所有检查通过，脚本可以正常执行${NC}" 