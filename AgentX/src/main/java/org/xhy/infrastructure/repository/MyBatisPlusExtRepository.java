package org.xhy.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.xhy.infrastructure.exception.BusinessException;

public interface MyBatisPlusExtRepository<T> extends BaseMapper<T> {

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

    default void checkedDelete(Wrapper<T> deleteWrapper) {
        int affected = delete(deleteWrapper);
        if (affected == 0) {
            throw new BusinessException("数据更新失败");
        }
    }

    default void checkInsert(T t) {
        int affected = insert(t);
        if (affected == 0) {
            throw new BusinessException("数据更新失败");
        }
    }
}
