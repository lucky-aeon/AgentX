#!/bin/bash

# 清理Git历史中的敏感信息脚本

echo "⚠️  警告：此操作将重写Git历史记录！"
echo "📋 这将清理以下敏感信息："
echo "   - SSO_COMMUNITY_APP_KEY"
echo "   - SSO_COMMUNITY_APP_SECRET"
echo ""
read -p "确定要继续吗？(y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "操作已取消"
    exit 1
fi

echo "🔄 开始清理敏感信息..."

# 使用git filter-repo清理敏感数据
git filter-repo --replace-text <(cat <<EOF
0a20f78c4cd922b8bf36843dd8123ba9=***REMOVED***
a16d7e161d75de63d9e9530c90581b915541ec00a4e8d365486c798e37db3533=***REMOVED***
EOF
) --force

echo "✅ 清理完成！"
echo ""
echo "📝 接下来需要："
echo "1. 检查清理结果"
echo "2. 强制推送到远程仓库："
echo "   git push --force-with-lease origin feature/sso-integration"
echo ""
echo "⚠️  注意：协作者需要重新克隆仓库！"