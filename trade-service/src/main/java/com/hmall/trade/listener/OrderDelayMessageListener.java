package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constants.MQConstants;
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
public class OrderDelayMessageListener {
    private final IOrderService orderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE_NAME,durable = "true"),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE_NAME, type = "direct",delayed = "true"),
            key = MQConstants.DELAY_ORDER_KEY
    ))
    public void listenOrderDelayMessage(Long orderId) {
        System.out.println("订单延迟队列监听到消息：" + orderId);
        // 1.查询订单
        Order order = orderService.getById(orderId);
        // 2.判断是否已支付
        if(order == null || order.getStatus() != 1){
            return;
        }
        // 3.未支付，需要查询支付流水
        PayOrderDTO payOrder = payClient.queryPayOrderByBizOrderNo(orderId);
        //4、判断是否已支付
        if(payOrder != null && payOrder.getStatus() == 3){
            // 4.2、已支付，标记订单为已支付
            orderService.markOrderPaySuccess(orderId);
        }
        else {
            // 4.1、未支付，需要取消订单，恢复库存

            orderService.cancelOrder(orderId);

            orderService.restoreStock(orderId);
        }

    }
}
