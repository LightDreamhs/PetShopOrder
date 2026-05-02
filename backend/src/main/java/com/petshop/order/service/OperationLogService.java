package com.petshop.order.service;

import com.petshop.order.common.PageResult;
import com.petshop.order.entity.OperationLog;

public interface OperationLogService {

    PageResult<OperationLog> getList(int page, int size, Long userId, String action, String startTime, String endTime);

    void log(Long userId, String username, String action, String target, String beforeVal, String afterVal);
}
