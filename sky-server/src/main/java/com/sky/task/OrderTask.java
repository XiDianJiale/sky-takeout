package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;


    /**     * 每分钟扫描一次，处理超时未支付订单
     */
    @Scheduled(cron = "0 * * * * ?") //每分钟

    public void processTimeoutOrders() {
        log.info("Processing timeout orders:{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time );

        if(ordersList != null && ordersList.size()>0){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，系统自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
                log.info("Order ID:{} has been cancelled due to 支付 timeout.", orders.getId());
            }
        }

    }




    /**
     * 每天凌晨1点处理配送中的订单为已完成
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrders() {
        log.info("定时处理派送中的订单:{}", LocalDateTime.now());



        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now().plusMinutes(-60));
        if(ordersList != null && ordersList.size()>0){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.COMPLETED);
                orders.setCancelReason("订单超时，系统自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
                log.info("Order ID:{} has been cancelled due to 派送 timeout.", orders.getId());
            }
        }

    }



}
