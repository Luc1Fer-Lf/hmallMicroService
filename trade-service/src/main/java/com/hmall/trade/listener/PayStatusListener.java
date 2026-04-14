package com.hmall.trade.listener;

import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayStatusListener {
    private final IOrderService orderService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "trade.pay.success.queue",durable = "true"),
            exchange = @Exchange(name = "pay.direct", type = "direct"),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId){
        //1、查询订单，判断订单状态是否为待支付
        Order order = orderService.getById(orderId);
        //2、判断订单状态是否为未支付
        if(order == null || order.getStatus() != 1){
            //订单不存在或者订单状态不是待支付则不处理
            return;
        }
        orderService.markOrderPaySuccess(orderId);// 修改订单状态
    }

}
