package didim.inquiry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    //개발자모드를 킨 브라우저에서 로그아웃 후 로그인시 에러가발생하여 처리
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }
}
