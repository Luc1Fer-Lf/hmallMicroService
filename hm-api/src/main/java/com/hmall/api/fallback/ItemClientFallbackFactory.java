package com.hmall.api.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.utils.CollUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;

@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {

    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemsByIds(Collection<Long> ids) {
                log.error("item-service查询商品服务调用失败", cause);
                return CollUtils.emptyList();// 返回空列表
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("item-service扣减库存服务调用失败", cause);
                throw new RuntimeException(cause);
            }
        };
    }
}
