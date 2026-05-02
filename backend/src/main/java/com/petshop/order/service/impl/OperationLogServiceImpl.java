package com.petshop.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.petshop.order.common.PageResult;
import com.petshop.order.entity.OperationLog;
import com.petshop.order.mapper.OperationLogMapper;
import com.petshop.order.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLog> getList(int page, int size, Long userId, String action, String startTime, String endTime) {
        PageHelper.startPage(page, size);
        List<OperationLog> list = operationLogMapper.selectPageList(userId, action, startTime, endTime);
        PageInfo<OperationLog> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), page, size);
    }

    @Override
    public void log(Long userId, String username, String action, String target, String beforeVal, String afterVal) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setTarget(target);
        log.setBeforeVal(beforeVal);
        log.setAfterVal(afterVal);
        operationLogMapper.insert(log);
    }
}
