package org.xhy.infrastructure.rag.processor;

import org.xhy.domain.rag.model.ProcessedSegment;
import org.xhy.domain.rag.model.SpecialNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 标题节点 - 表示Markdown文档中的标题层次结构
 * 
 * 用于递归分割算法，维护标题的层级关系和内容聚合
 * 
 * @author claude */
public class HeadingNode {

    /** 标题级别 (1-6，对应H1-H6) */
    private int level;
    
    /** 标题文本内容 */
    private String title;
    
    /** 标题下的直接内容（非子标题内容） */
    private StringBuilder directContent;
    
    /** 子标题节点列表 */
    private List<HeadingNode> children;
    
    /** 父节点引用 */
    private HeadingNode parent;
    
    /** 标题下收集的特殊节点 */
    private List<SpecialNode> specialNodes;
    
    /** 内容长度缓存（避免重复计算） */
    private Integer cachedContentLength;
    
    public HeadingNode(int level, String title) {
        this.level = level;
        this.title = title;
        this.directContent = new StringBuilder();
        this.children = new ArrayList<>();
        this.specialNodes = new ArrayList<>();
        this.parent = null;
        this.cachedContentLength = null;
    }

    /** 添加直接内容到当前标题下 */
    public void addDirectContent(String content) {
        if (content != null && !content.trim().isEmpty()) {
            if (directContent.length() > 0) {
                directContent.append("\n\n");
            }
            directContent.append(content.trim());
            // 清空缓存，因为内容发生变化
            cachedContentLength = null;
        }
    }

    /** 添加特殊节点 */
    public void addSpecialNode(SpecialNode node) {
        if (node != null) {
            specialNodes.add(node);
        }
    }

    /** 添加子标题节点 */
    public void addChild(HeadingNode child) {
        if (child != null) {
            children.add(child);
            child.parent = this;
            // 清空缓存，因为子节点发生变化
            cachedContentLength = null;
        }
    }

    /** 获取完整标题路径（从根到当前节点） */
    public String getFullTitlePath() {
        if (parent == null) {
            return getFormattedTitle();
        }
        return parent.getFullTitlePath() + "\n\n" + getFormattedTitle();
    }

    /** 获取格式化的标题文本（带#前缀） */
    public String getFormattedTitle() {
        return "#".repeat(level) + " " + title;
    }

    /** 计算当前节点及其所有子节点的总内容长度 */
    public int getTotalContentLength() {
        if (cachedContentLength != null) {
            return cachedContentLength;
        }

        int totalLength = 0;
        
        // 标题长度
        totalLength += getFormattedTitle().length() + 2; // +2 for "\n\n"
        
        // 直接内容长度
        if (directContent.length() > 0) {
            totalLength += directContent.length() + 2; // +2 for "\n\n"
        }
        
        // 子节点内容长度
        for (HeadingNode child : children) {
            totalLength += child.getTotalContentLength();
        }
        
        cachedContentLength = totalLength;
        return totalLength;
    }

    /** 获取当前节点的直接内容长度（不包含子节点） */
    public int getDirectContentLength() {
        int length = getFormattedTitle().length() + 2; // +2 for "\n\n"
        if (directContent.length() > 0) {
            length += directContent.length() + 2; // +2 for "\n\n"
        }
        return length;
    }

    /** 生成完整内容（包含当前标题、直接内容和所有子节点） */
    public String generateFullContent() {
        StringBuilder result = new StringBuilder();
        
        // 添加当前标题
        result.append(getFormattedTitle()).append("\n\n");
        
        // 添加直接内容
        if (directContent.length() > 0) {
            result.append(directContent.toString()).append("\n\n");
        }
        
        // 添加子节点内容
        for (HeadingNode child : children) {
            result.append(child.generateFullContent()).append("\n\n");
        }
        
        return result.toString().trim();
    }

    /** 生成仅包含直接内容的ProcessedSegment */
    public ProcessedSegment generateDirectContentSegment() {
        StringBuilder content = new StringBuilder();
        
        // 添加完整标题路径
        content.append(getFullTitlePath()).append("\n\n");
        
        // 添加直接内容
        if (directContent.length() > 0) {
            content.append(directContent.toString());
        }
        
        ProcessedSegment segment = new ProcessedSegment(
            content.toString().trim(), 
            org.xhy.domain.rag.model.enums.SegmentType.SECTION, 
            null
        );
        
        // 添加特殊节点
        for (SpecialNode specialNode : specialNodes) {
            segment.addSpecialNode(specialNode);
        }
        
        return segment;
    }

    /** 检查是否为叶子节点（无子节点） */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** 检查是否有直接内容 */
    public boolean hasDirectContent() {
        return directContent.length() > 0;
    }

    /** 获取指定级别的子节点 */
    public List<HeadingNode> getChildrenAtLevel(int targetLevel) {
        List<HeadingNode> result = new ArrayList<>();
        for (HeadingNode child : children) {
            if (child.level == targetLevel) {
                result.add(child);
            }
        }
        return result;
    }

    /** 获取最深的子节点级别 */
    public int getMaxChildLevel() {
        int maxLevel = level;
        for (HeadingNode child : children) {
            maxLevel = Math.max(maxLevel, child.getMaxChildLevel());
        }
        return maxLevel;
    }

    // Getters
    public int getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getDirectContent() {
        return directContent.toString();
    }

    public List<HeadingNode> getChildren() {
        return new ArrayList<>(children);
    }

    public HeadingNode getParent() {
        return parent;
    }

    public List<SpecialNode> getSpecialNodes() {
        return new ArrayList<>(specialNodes);
    }

    @Override
    public String toString() {
        return "HeadingNode{" +
                "level=" + level +
                ", title='" + title + '\'' +
                ", directContentLength=" + directContent.length() +
                ", childrenCount=" + children.size() +
                ", totalLength=" + getTotalContentLength() +
                '}';
    }
}