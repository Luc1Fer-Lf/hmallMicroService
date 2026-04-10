package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {
    private final NacosConfigManager nacosConfigManager;// Nacos 配置管理器
    private final String dataId = "gateway-routes.json";// 路由配置文件
    private final String groupId = "DEFAULT_GROUP";
    private final RouteDefinitionWriter routeDefinitionWriter;// 路由定义写入器
    private final Set<String> routeIds = new HashSet<>();// 记录路由信息，方便下次更新时删除

    /**
     * 初始化路由配置监听
     */
    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        log.info("初始化路由配置监听");
        //项目启动，先拉取一次配置，并且添加配置监听器
        String config = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, groupId, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String config) {
                        //2、监听到配置文件更新，重新加载路由
                        updateRoute(config);
                    }
                });
        //3.第一次读取到配置，也需要更新到路由表
        updateRoute(config);
    }

    //4.更新路由表方法
    private void updateRoute(String config) {
        log.info("见听到路由配置信息：{}", config);
        //1、解析配置文件，得到路由表
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(config, RouteDefinition.class);
        //2、删除旧的路由信息
        routeIds.forEach(routeId -> routeDefinitionWriter.delete(Mono.just(routeId)).subscribe());//删除旧的路由信息
        routeIds.clear();//清空路由信息表
        //3、更新路由表
        for(RouteDefinition routeDefinition: routeDefinitions){
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();//保存
            //记录路由信息 方便下次更新时删除
            routeIds.add(routeDefinition.getId());
        }
    }
}
