package com.hmall.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;// 注入 配置类
    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();// 创建匹配器
    @Override
    public int getOrder() {
        return 0;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 1.获取请求头
        ServerHttpRequest request = exchange.getRequest();
        // 2.判断是否需要过滤
        if (isExcludePath(request.getPath().toString())) {
            return chain.filter(exchange);
        }
        // 3.获取token
        String token = null;
        List<String> headers = request.getHeaders().get("Authorization");
        if(headers != null && !headers.isEmpty()){
            token = headers.get(0);
        }
        // 4.解析 token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            // 解析失败 拦截
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        //  5.传递用户信息
        String userInfo = userId.toString();
        ServerWebExchange swe = exchange.mutate()// 创建新的请求对象
                .request(builder -> builder.header("user-info", userInfo))// 设置请求头
                .build();
        // 6.放行
        return chain.filter(swe);
    }


    /**
     * 判断请求路径是否在白名单中
     * @param string
     * @return
     */
    private boolean isExcludePath(String string) {
        for(String path:authProperties.getExcludePaths()){
            if(antPathMatcher.match(path,string)){
                return true;
            }
        }
        return false;
    }

}
