package org.xhy.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.xhy.infrastructure.exception.BusinessException;

public interface MyBatisPlusExtRepository<T> extends BaseMapper<T> {

    /**
     * 带检查的更新
     */
    default void checkedUpdate(T entity, Wrapper<T> updateWrapper) {
        int affected = update(entity, updateWrapper);
        if (affected == 0) {
            throw new BusinessException("数据更新失败");
        }
    }

    default void checkedUpdate(Wrapper<T> updateWrapper) {
        int affected = update(updateWrapper);
        if (affected == 0) {
            throw new BusinessException("数据更新失败");
        }
    }

    default void checkedUpdateById(T t) {
        int affected = updateById(t);
        if (affected == 0) {
            throw new BusinessException("数据更新失败");
        }
    }


    /**
     * 带检查的删除
     */
    default void checkedDelete(Wrapper<T> deleteWrapper) {
        int affected = delete(deleteWrapper);
        if (affected == 0) {
            throw new BusinessException("数据更新失败");
        }
    }
}
